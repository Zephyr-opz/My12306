package com.cdvtc.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cdvtc.demo.dto.TrainPreviewResponse;
import com.cdvtc.demo.dto.TrainSaveRequest;
import com.cdvtc.demo.entity.*;
import com.cdvtc.demo.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TrainAdminService extends ServiceImpl<TrainMapper, Train> {

    private final TrainMapper trainMapper;
    private final StationMapper stationMapper;
    private final TrainStationMapper trainStationMapper;
    private final TrainStationPriceMapper trainStationPriceMapper;
    private final TrainStationRelationMapper trainStationRelationMapper;
    private final CarriageMapper carriageMapper;
    private final SeatMapper seatMapper;
    private final ScheduleSeatMapper scheduleSeatMapper;
    private final SeatSegmentOccupancyMapper occupancyMapper;
    private final TrainScheduleMapper trainScheduleMapper;
    private final OrderMapper orderMapper;
    private final TicketMapper ticketMapper;

    public List<Train> list(String keyword) {
        LambdaQueryWrapper<Train> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Train::getTrainNumber, keyword)
                    .or().like(Train::getStartStation, keyword)
                    .or().like(Train::getEndStation, keyword);
        }
        return list(wrapper.orderByAsc(Train::getTrainNumber));
    }

    public Map<String, Object> detail(Long id) {
        Train train = getById(id);
        if (train == null) {
            throw new IllegalArgumentException("车次不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("train", train);
        result.put("stops", trainStationMapper.selectList(new LambdaQueryWrapper<TrainStation>()
                .eq(TrainStation::getTrainId, id).orderByAsc(TrainStation::getSequence)));
        result.put("fares", trainStationPriceMapper.selectList(new LambdaQueryWrapper<TrainStationPrice>()
                .eq(TrainStationPrice::getTrainId, id)));
        result.put("carriages", carriageMapper.selectList(new LambdaQueryWrapper<Carriage>()
                .eq(Carriage::getTrainId, id).orderByAsc(Carriage::getCarriageNumber)));
        result.put("schedules", trainScheduleMapper.selectList(new LambdaQueryWrapper<TrainSchedule>()
                .eq(TrainSchedule::getTrainId, id).orderByAsc(TrainSchedule::getDepartureDate)));
        return result;
    }

    public TrainPreviewResponse preview(TrainSaveRequest request) {
        validateTrainNumber(request, request.getId());
        validateChildConfig(request);
        TrainTemplateCalculator.Calculation calculation = calculate(request);
        return toPreviewResponse(request, calculation);
    }

    @Transactional
    public Train create(TrainSaveRequest request) {
        validateTrainNumber(request, null);
        validateChildConfig(request);
        TrainTemplateCalculator.Calculation calculation = calculate(request);
        applyCalculation(request, calculation);
        Train train = new Train();
        BeanUtils.copyProperties(request, train);
        save(train);
        replaceChildren(train.getId(), request, calculation);
        return train;
    }

    @Transactional
    public Train update(Long id, TrainSaveRequest request) {
        Train train = getById(id);
        if (train == null) {
            throw new IllegalArgumentException("车次不存在");
        }
        ensureNoOrdersOrTickets(id);
        validateTrainNumber(request, id);
        validateChildConfig(request);
        TrainTemplateCalculator.Calculation calculation = calculate(request);
        applyCalculation(request, calculation);
        BeanUtils.copyProperties(request, train);
        train.setId(id);
        updateById(train);
        removeChildren(id);
        replaceChildren(id, request, calculation);
        return train;
    }

    @Transactional
    public void delete(Long id) {
        if (getById(id) == null) {
            throw new IllegalArgumentException("车次不存在");
        }
        ensureNoOrdersOrTickets(id);
        removeRuntimeSeats(id);
        trainScheduleMapper.delete(new LambdaQueryWrapper<TrainSchedule>().eq(TrainSchedule::getTrainId, id));
        trainStationMapper.delete(new LambdaQueryWrapper<TrainStation>().eq(TrainStation::getTrainId, id));
        trainStationPriceMapper.delete(new LambdaQueryWrapper<TrainStationPrice>().eq(TrainStationPrice::getTrainId, id));
        trainStationRelationMapper.delete(new LambdaQueryWrapper<TrainStationRelation>().eq(TrainStationRelation::getTrainId, id));
        carriageMapper.delete(new LambdaQueryWrapper<Carriage>().eq(Carriage::getTrainId, id));
        seatMapper.delete(new LambdaQueryWrapper<Seat>().eq(Seat::getTrainId, id));
        removeById(id);
    }

    public static List<LocalDate> buildScheduleDates(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start)) {
            throw new IllegalArgumentException("运行日期范围无效");
        }
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            dates.add(date);
        }
        return dates;
    }

    private void ensureNoOrdersOrTickets(Long trainId) {
        if (orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getTrainId, trainId).notIn(Order::getStatus, 40, 60)) > 0
                || ticketMapper.selectCount(new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getTrainId, trainId).notIn(Ticket::getTicketStatus, 40, 60)) > 0) {
            throw new IllegalArgumentException("已有订单或车票的车次不能重建或删除");
        }
    }

    private void removeChildren(Long trainId) {
        removeRuntimeSeats(trainId);
        trainScheduleMapper.delete(new LambdaQueryWrapper<TrainSchedule>().eq(TrainSchedule::getTrainId, trainId));
        trainStationMapper.delete(new LambdaQueryWrapper<TrainStation>().eq(TrainStation::getTrainId, trainId));
        trainStationPriceMapper.delete(new LambdaQueryWrapper<TrainStationPrice>().eq(TrainStationPrice::getTrainId, trainId));
        trainStationRelationMapper.delete(new LambdaQueryWrapper<TrainStationRelation>().eq(TrainStationRelation::getTrainId, trainId));
        carriageMapper.delete(new LambdaQueryWrapper<Carriage>().eq(Carriage::getTrainId, trainId));
    }

    private void removeRuntimeSeats(Long trainId) {
        List<TrainSchedule> schedules = trainScheduleMapper.selectList(new LambdaQueryWrapper<TrainSchedule>()
                .eq(TrainSchedule::getTrainId, trainId));
        if (schedules.isEmpty()) {
            return;
        }
        List<Long> scheduleIds = schedules.stream().map(TrainSchedule::getId).toList();
        occupancyMapper.delete(new LambdaQueryWrapper<SeatSegmentOccupancy>().in(SeatSegmentOccupancy::getScheduleId, scheduleIds));
        scheduleSeatMapper.delete(new LambdaQueryWrapper<ScheduleSeat>().in(ScheduleSeat::getScheduleId, scheduleIds));
    }

    private void replaceChildren(Long trainId, TrainSaveRequest request, TrainTemplateCalculator.Calculation calculation) {
        List<TrainSaveRequest.Stop> stops = calculation.getStops();

        for (int i = 0; i < stops.size(); i++) {
            TrainSaveRequest.Stop input = stops.get(i);
            TrainStation stop = new TrainStation();
            BeanUtils.copyProperties(input, stop);
            stop.setTrainId(trainId);
            if (!StringUtils.hasText(stop.getSequence())) {
                stop.setSequence(String.format("%02d", i + 1));
            }
            trainStationMapper.insert(stop);
        }

        for (TrainSaveRequest.Fare input : calculation.getIntervalFares()) {
            TrainStationPrice fare = new TrainStationPrice();
            fare.setTrainId(trainId);
            BeanUtils.copyProperties(input, fare);
            trainStationPriceMapper.insert(fare);
        }

        for (int i = 0; i < stops.size() - 1; i++) {
            for (int j = i + 1; j < stops.size(); j++) {
                TrainSaveRequest.Stop from = stops.get(i);
                TrainSaveRequest.Stop to = stops.get(j);
                TrainStationRelation relation = new TrainStationRelation();
                relation.setTrainId(trainId);
                relation.setDeparture(from.getDeparture());
                relation.setArrival(to.getArrival());
                relation.setStartRegion(from.getStartRegion());
                relation.setEndRegion(to.getEndRegion());
                relation.setDepartureFlag(i == 0 ? 1 : 0);
                relation.setArrivalFlag(j == stops.size() - 1 ? 1 : 0);
                relation.setDepartureTime(from.getDepartureTime());
                relation.setArrivalTime(to.getArrivalTime());
                trainStationRelationMapper.insert(relation);
            }
        }

        if (request.getCarriages() != null) {
            for (TrainSaveRequest.CarriageItem input : request.getCarriages()) {
                Carriage carriage = new Carriage();
                BeanUtils.copyProperties(input, carriage);
                carriage.setTrainId(trainId);
                carriageMapper.insert(carriage);
            }
        }
        generateSchedules(trainId, request, calculation);
    }

    private void generateSchedules(Long trainId, TrainSaveRequest request, TrainTemplateCalculator.Calculation calculation) {
        LocalTime departure = calculation.getDepartureTime().toLocalTime();
        LocalTime arrival = calculation.getArrivalTime().toLocalTime();
        long arrivalDayOffset = java.time.temporal.ChronoUnit.DAYS.between(
                calculation.getDepartureTime().toLocalDate(), calculation.getArrivalTime().toLocalDate());
        for (LocalDate date : buildScheduleDates(request.getRunStartDate(), request.getRunEndDate())) {
            TrainSchedule schedule = new TrainSchedule();
            schedule.setTrainId(trainId);
            schedule.setDepartureDate(date);
            schedule.setDepartureTime(LocalDateTime.of(date, departure));
            schedule.setArrivalTime(LocalDateTime.of(date.plusDays(arrivalDayOffset), arrival));
            schedule.setSaleStatus(0);
            schedule.setPlanStatus(1);
            trainScheduleMapper.insert(schedule);
        }
    }

    private void validateRunRange(TrainSaveRequest request) {
        buildScheduleDates(request.getRunStartDate(), request.getRunEndDate());
    }

    private TrainTemplateCalculator.Calculation calculate(TrainSaveRequest request) {
        validateRunRange(request);
        validateStations(request);
        return TrainTemplateCalculator.calculate(request);
    }

    private void validateStations(TrainSaveRequest request) {
        if (request.getStops() == null) return;
        for (TrainSaveRequest.Stop stop : request.getStops()) {
            Station station = stop.getStationId() == null
                    ? stationMapper.selectOne(new LambdaQueryWrapper<Station>().eq(Station::getName, stop.getDeparture()))
                    : stationMapper.selectById(stop.getStationId());
            if (station == null) {
                throw new IllegalArgumentException("经停站不存在，请先在地区车站维护中创建：" + stop.getDeparture());
            }
            if (StringUtils.hasText(stop.getDeparture()) && !station.getName().equals(stop.getDeparture())) {
                throw new IllegalArgumentException("车站选择与名称不一致，请重新选择：" + stop.getDeparture());
            }
            stop.setStationId(station.getId());
            stop.setDeparture(station.getName());
            stop.setArrival(station.getName());
            stop.setStartRegion(station.getRegion());
            stop.setEndRegion(station.getRegionName());
        }
    }

    private void validateTrainNumber(TrainSaveRequest request, Long currentId) {
        LambdaQueryWrapper<Train> wrapper = new LambdaQueryWrapper<Train>()
                .eq(Train::getTrainNumber, request.getTrainNumber());
        if (currentId != null) {
            wrapper.ne(Train::getId, currentId);
        }
        if (trainMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("车次号已存在");
        }
    }

    private void validateChildConfig(TrainSaveRequest request) {
        if (request.getCarriages() == null || request.getCarriages().isEmpty()
                || request.getCarriages().stream().anyMatch(item -> !StringUtils.hasText(item.getCarriageNumber())
                || item.getSeatCount() == null || item.getSeatCount() < 1)) {
            throw new IllegalArgumentException("至少需要配置一个车厢及座位数");
        }
    }

    private void applyCalculation(TrainSaveRequest request, TrainTemplateCalculator.Calculation calculation) {
        request.setStartStation(calculation.getStartStation());
        request.setEndStation(calculation.getEndStation());
        request.setDepartureTime(calculation.getDepartureTime());
        request.setArrivalTime(calculation.getArrivalTime());
        request.setStops(calculation.getStops());
        request.setFares(calculation.getFullFares());
    }

    private TrainPreviewResponse toPreviewResponse(TrainSaveRequest request, TrainTemplateCalculator.Calculation calculation) {
        TrainPreviewResponse response = new TrainPreviewResponse();
        response.setTrainId(request.getId());
        response.setTrainNumber(request.getTrainNumber());
        response.setStartStation(calculation.getStartStation());
        response.setEndStation(calculation.getEndStation());
        response.setStartRegion(request.getStartRegion());
        response.setEndRegion(request.getEndRegion());
        response.setDepartureTime(calculation.getDepartureTime());
        response.setArrivalTime(calculation.getArrivalTime());
        response.setRunStartDate(request.getRunStartDate());
        response.setRunEndDate(request.getRunEndDate());
        response.setTotalMinutes(calculation.getTotalMinutes());
        response.setStops(calculation.getStops());
        response.setFullFares(calculation.getFullFares());
        response.setIntervalFares(calculation.getIntervalFares());
        response.setCarriages(request.getCarriages());
        return response;
    }
}
