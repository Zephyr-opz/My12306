package com.cdvtc.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cdvtc.demo.common.Result;
import com.cdvtc.demo.entity.*;
import com.cdvtc.demo.mapper.*;
import jakarta.validation.Valid;
import com.cdvtc.demo.dto.PurchaseRequest;
import com.cdvtc.demo.service.SeatAvailabilityCalculator;
import com.cdvtc.demo.service.ScheduleTimeResolver;
import com.cdvtc.demo.service.TicketPurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class TicketController {

    private final StationMapper stationMapper;
    private final TrainMapper trainMapper;
    private final TrainStationMapper trainStationMapper;
    private final TrainStationPriceMapper trainStationPriceMapper;
    private final TrainScheduleMapper trainScheduleMapper;
    private final ScheduleSeatMapper scheduleSeatMapper;
    private final SeatSegmentOccupancyMapper seatSegmentOccupancyMapper;
    private final TicketPurchaseService ticketPurchaseService;

    @GetMapping("/api/ticket-service/station/all")
    public Result<?> getAllStations() {
        List<Station> stations = stationMapper.selectList(
                new LambdaQueryWrapper<Station>().orderByAsc(Station::getId));
        return Result.success(stations);
    }

    @GetMapping("/api/ticket-service/ticket/query")
    public Result<?> queryTickets(
            @RequestParam String fromStation,
            @RequestParam String toStation,
            @RequestParam String departureDate) {

        LocalDate queryDate;
        try {
            queryDate = LocalDate.parse(departureDate);
        } catch (DateTimeParseException ex) {
            return Result.error(400, "出发日期格式应为 yyyy-MM-dd");
        }

        // Find station names by code
        Station from = stationMapper.selectOne(
                new LambdaQueryWrapper<Station>().eq(Station::getCode, fromStation));
        Station to = stationMapper.selectOne(
                new LambdaQueryWrapper<Station>().eq(Station::getCode, toStation));
        if (from == null || to == null) {
            Map<String, Object> emptyResult = new HashMap<>();
            emptyResult.put("trainList", Collections.emptyList());
            emptyResult.put("trainBrandList", Collections.emptyList());
            emptyResult.put("seatClassTypeList", Collections.emptyList());
            emptyResult.put("departureStationList", Collections.emptyList());
            emptyResult.put("arrivalStationList", Collections.emptyList());
            return Result.success(emptyResult);
        }

        Map<Long, TrainSchedule> scheduleByTrain = trainScheduleMapper.selectList(
                        new LambdaQueryWrapper<TrainSchedule>()
                                .eq(TrainSchedule::getDepartureDate, queryDate)
                                .eq(TrainSchedule::getSaleStatus, 0)
                                .eq(TrainSchedule::getPlanStatus, 1))
                .stream()
                .collect(Collectors.toMap(TrainSchedule::getTrainId, schedule -> schedule));

        List<Train> trains = trainMapper.selectList(
                new LambdaQueryWrapper<Train>().eq(Train::getSaleStatus, 0));

        // Build train list with seat class info
        List<Map<String, Object>> trainList = new ArrayList<>();
        Set<Integer> brandSet = new HashSet<>();
        Set<Integer> seatTypeSet = new HashSet<>();
        Set<String> departureStationSet = new HashSet<>();
        Set<String> arrivalStationSet = new HashSet<>();
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");

        for (Train train : trains) {
            if (!scheduleByTrain.containsKey(train.getId()))
                continue;

            List<TrainStation> route = trainStationMapper.selectList(
                    new LambdaQueryWrapper<TrainStation>()
                            .eq(TrainStation::getTrainId, train.getId())
                            .orderByAsc(TrainStation::getSequence));
            int fromIndex = findStationIndex(route, from.getName());
            int toIndex = findStationIndex(route, to.getName());
            if (fromIndex < 0 || toIndex < 0 || fromIndex >= toIndex)
                continue;

            TrainStation fromStop = route.get(fromIndex);
            TrainStation toStop = route.get(toIndex);
            TrainSchedule schedule = scheduleByTrain.get(train.getId());
            ticketPurchaseService.ensureScheduleSeats(schedule, train);
            int startSequence = sequenceOf(fromStop, fromIndex);
            int endSequence = sequenceOf(toStop, toIndex);
            List<ScheduleSeat> scheduleSeats = scheduleSeatMapper.selectList(
                    new LambdaQueryWrapper<ScheduleSeat>()
                            .eq(ScheduleSeat::getScheduleId, schedule.getId()));
            List<SeatSegmentOccupancy> activeOccupancies = seatSegmentOccupancyMapper.selectList(
                    new LambdaQueryWrapper<SeatSegmentOccupancy>()
                            .eq(SeatSegmentOccupancy::getScheduleId, schedule.getId())
                            .eq(SeatSegmentOccupancy::getOccupancyStatus, 1));

            Map<String, Object> trainInfo = new HashMap<>();
            trainInfo.put("trainId", String.valueOf(train.getId()));
            trainInfo.put("trainNumber", train.getTrainNumber());
            trainInfo.put("trainType", train.getTrainType());
            trainInfo.put("trainTag", train.getTrainTag());
            trainInfo.put("trainBrand", train.getTrainBrand());
            trainInfo.put("departure", fromStop.getDeparture());
            trainInfo.put("arrival", toStop.getArrival());

            // 经停站时间来自模板，需要换算到当前运行计划日期；跨日车次保留日期偏移。
            LocalDateTime departureTime = ScheduleTimeResolver.resolve(
                    schedule.getDepartureDate(), train.getDepartureTime(), fromStop.getDepartureTime());
            LocalDateTime arrivalTime = ScheduleTimeResolver.resolve(
                    schedule.getDepartureDate(), train.getDepartureTime(), toStop.getArrivalTime());
            trainInfo.put("departureTime", departureTime.format(timeFormat));
            trainInfo.put("arrivalTime", arrivalTime.format(timeFormat));

            // Calculate duration
            Duration duration = Duration.between(departureTime, arrivalTime);
            long hours = duration.toHours();
            long minutes = duration.toMinutesPart();
            trainInfo.put("duration", String.format("%02d:%02d", hours, minutes));
            trainInfo.put("departureFlag", fromIndex == 0 ? 1 : 0);
            trainInfo.put("arrivalFlag", toIndex == route.size() - 1 ? 1 : 0);
            trainInfo.put("saleStatus", train.getSaleStatus());
            trainInfo.put("daysArrived", 0);

            // Get prices for this route
            List<TrainStationPrice> prices = trainStationPriceMapper.selectList(
                    new LambdaQueryWrapper<TrainStationPrice>()
                            .eq(TrainStationPrice::getTrainId, train.getId())
                            .eq(TrainStationPrice::getDeparture, fromStop.getDeparture())
                            .eq(TrainStationPrice::getArrival, toStop.getArrival()));

            // Build seatClassList
            List<Map<String, Object>> seatClassList = new ArrayList<>();
            for (TrainStationPrice price : prices) {
                Map<String, Object> seatClass = new HashMap<>();
                seatClass.put("type", price.getSeatType());
                seatClass.put("price", price.getPrice());

                // Count concrete seats whose occupied intervals do not overlap this query interval
                int quantity = SeatAvailabilityCalculator.countAvailable(
                        scheduleSeats.stream()
                        .filter(c -> Objects.equals(c.getSeatType(), price.getSeatType()))
                        .toList(), activeOccupancies, startSequence, endSequence);
                seatClass.put("quantity", quantity);
                seatClass.put("soldOut", quantity == 0);
                seatClass.put("candidate", false);
                seatClassList.add(seatClass);
                seatTypeSet.add(price.getSeatType());
            }
            trainInfo.put("seatClassList", seatClassList);

            // Collect brands
            if (train.getTrainBrand() != null) {
                Arrays.stream(train.getTrainBrand().split(","))
                        .filter(s -> !s.isEmpty())
                        .forEach(b -> brandSet.add(Integer.parseInt(b.trim())));
            }

            departureStationSet.add(fromStop.getDeparture());
            arrivalStationSet.add(toStop.getArrival());

            trainList.add(trainInfo);
        }

        // Sort by departure time
        trainList.sort((a, b) -> ((String) a.get("departureTime")).compareTo((String) b.get("departureTime")));

        Map<String, Object> result = new HashMap<>();
        result.put("trainList", trainList);
        result.put("trainBrandList", new ArrayList<>(brandSet));
        result.put("seatClassTypeList", new ArrayList<>(seatTypeSet));
        result.put("departureStationList", new ArrayList<>(departureStationSet));
        result.put("arrivalStationList", new ArrayList<>(arrivalStationSet));

        return Result.success(result);
    }

    private int findStationIndex(List<TrainStation> route, String stationName) {
        for (int i = 0; i < route.size(); i++) {
            if (stationName.equals(route.get(i).getDeparture())
                    || stationName.equals(route.get(i).getArrival())) {
                return i;
            }
        }
        return -1;
    }

    private int sequenceOf(TrainStation station, int fallback) {
        try {
            return Integer.parseInt(station.getSequence());
        } catch (NumberFormatException | NullPointerException ex) {
            return fallback;
        }
    }

    @GetMapping("/api/ticket-service/train-station/query")
    public Result<?> queryTrainStation(@RequestParam String trainId) {
        List<TrainStation> stations = trainStationMapper.selectList(
                new LambdaQueryWrapper<TrainStation>()
                        .eq(TrainStation::getTrainId, Long.parseLong(trainId))
                        .orderByAsc(TrainStation::getSequence));

        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");
        List<Map<String, Object>> result = stations.stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("sequence", s.getSequence());
            map.put("departure", s.getDeparture());
            map.put("arrivalTime", s.getArrivalTime() != null ? s.getArrivalTime().format(timeFormat) : "-");
            map.put("departureTime", s.getDepartureTime() != null ? s.getDepartureTime().format(timeFormat) : "-");
            map.put("stopoverTime", s.getStopoverTime() != null ? s.getStopoverTime() + "分" : "-");
            return map;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    @GetMapping("/api/ticket-service/region-station/query")
    public Result<?> queryRegionStation(@RequestParam(required = false) String name) {
        LambdaQueryWrapper<Station> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(Station::getRegionName, name);
        }
        List<Station> stations = stationMapper.selectList(wrapper);
        return Result.success(stations);
    }

    @PostMapping("/api/ticket-service/ticket/purchase/v2")
    public Result<?> purchaseTicket(@Valid @RequestBody PurchaseRequest req,
                                     org.springframework.security.core.Authentication authentication) {
        return Result.success(ticketPurchaseService.purchase(req, authentication.getName()));
    }

    @PostMapping("/api/ticket-service/ticket/cancel")
    public Result<?> cancelTicket(@RequestBody Map<String, Object> req,
                                  org.springframework.security.core.Authentication authentication) {
        ticketPurchaseService.cancel(String.valueOf(req.get("orderSn")), authentication.getName(), false);
        return Result.success();
    }

    @PostMapping("/api/ticket-service/ticket/refund")
    public Result<?> refundTicket(@RequestBody Map<String, Object> req,
                                  org.springframework.security.core.Authentication authentication) {
        ticketPurchaseService.cancel(String.valueOf(req.get("orderSn")), authentication.getName(), true);
        return Result.success();
    }
}

