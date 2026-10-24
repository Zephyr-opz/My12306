package com.cdvtc.demo.service;

import com.cdvtc.demo.dto.TrainSaveRequest;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TrainTemplateCalculator {

    private TrainTemplateCalculator() {
    }

    public static Calculation calculate(TrainSaveRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("车次模板不能为空");
        }
        if (request.getDepartureTime() == null || request.getArrivalTime() == null) {
            throw new IllegalArgumentException("始发时间和终到时间不能为空");
        }
        List<TrainSaveRequest.Stop> stops = normalizeStops(request.getStops());
        LocalDateTime departureTime = request.getDepartureTime();
        LocalDateTime arrivalTime = request.getArrivalTime();
        if (!arrivalTime.isAfter(departureTime)) {
            arrivalTime = arrivalTime.plusDays(1);
        }
        long totalMinutes = Duration.between(departureTime, arrivalTime).toMinutes();
        if (totalMinutes <= 0 || totalMinutes > 24 * 60) {
            throw new IllegalArgumentException("车次运行时长必须大于 0 且不超过 24 小时");
        }

        long stopoverMinutes = 0;
        for (int i = 1; i < stops.size() - 1; i++) {
            int minutes = stops.get(i).getStopoverTime() == null ? 0 : stops.get(i).getStopoverTime();
            if (minutes < 0 || minutes > 999) {
                throw new IllegalArgumentException("中间站停留分钟必须在 0 到 999 之间");
            }
            stops.get(i).setStopoverTime(minutes);
            stopoverMinutes += minutes;
        }
        stops.get(0).setStopoverTime(0);
        stops.get(stops.size() - 1).setStopoverTime(0);

        int segments = stops.size() - 1;
        long remainingRuntime = totalMinutes - stopoverMinutes;
        if (remainingRuntime < segments) {
            throw new IllegalArgumentException("扣除停留时间后，剩余运行时间不足以覆盖所有区间");
        }

        long baseRuntime = remainingRuntime / segments;
        int remainder = (int) (remainingRuntime % segments);
        LocalDateTime previousDeparture = departureTime;
        stops.get(0).setArrivalTime(departureTime);
        stops.get(0).setDepartureTime(departureTime);
        for (int i = 0; i < segments; i++) {
            long segmentMinutes = baseRuntime + (i < remainder ? 1 : 0);
            TrainSaveRequest.Stop from = stops.get(i);
            TrainSaveRequest.Stop to = stops.get(i + 1);
            LocalDateTime arrival = previousDeparture.plusMinutes(segmentMinutes);
            to.setArrivalTime(arrival);
            if (i + 1 < stops.size() - 1) {
                to.setDepartureTime(arrival.plusMinutes(to.getStopoverTime()));
                previousDeparture = to.getDepartureTime();
            } else {
                to.setDepartureTime(null);
            }
        }

        List<TrainSaveRequest.Fare> fullFares = normalizeFullFares(request.getFares(), stops);
        List<TrainSaveRequest.Fare> intervalFares = new ArrayList<>();
        for (TrainSaveRequest.Fare fullFare : fullFares) {
            for (int i = 0; i < stops.size() - 1; i++) {
                for (int j = i + 1; j < stops.size(); j++) {
                    TrainSaveRequest.Fare interval = new TrainSaveRequest.Fare();
                    interval.setDeparture(stops.get(i).getDeparture());
                    interval.setArrival(stops.get(j).getDeparture());
                    interval.setSeatType(fullFare.getSeatType());
                    long intervalMinutes = Duration.between(stops.get(i).getDepartureTime(), stops.get(j).getArrivalTime()).toMinutes();
                    int price = i == 0 && j == stops.size() - 1
                            ? fullFare.getPrice()
                            : (int) Math.round(fullFare.getPrice() * intervalMinutes / (double) totalMinutes);
                    interval.setPrice(price);
                    intervalFares.add(interval);
                }
            }
        }

        Calculation result = new Calculation();
        result.setStartStation(stops.get(0).getDeparture());
        result.setEndStation(stops.get(stops.size() - 1).getDeparture());
        result.setDepartureTime(departureTime);
        result.setArrivalTime(arrivalTime);
        result.setTotalMinutes(totalMinutes);
        result.setStops(stops);
        result.setFullFares(fullFares);
        result.setIntervalFares(intervalFares);
        return result;
    }

    private static List<TrainSaveRequest.Stop> normalizeStops(List<TrainSaveRequest.Stop> input) {
        if (input == null || input.size() < 2) {
            throw new IllegalArgumentException("至少需要配置始发站和终点站");
        }
        List<TrainSaveRequest.Stop> stops = new ArrayList<>(input);
        stops.sort(Comparator.comparing(TrainSaveRequest.Stop::getSequence, Comparator.nullsLast(String::compareTo)));
        Set<String> stations = new HashSet<>();
        for (int i = 0; i < stops.size(); i++) {
            TrainSaveRequest.Stop stop = stops.get(i);
            if (!StringUtils.hasText(stop.getDeparture()) || !stations.add(stop.getDeparture())) {
                throw new IllegalArgumentException("经停站不能重复或为空");
            }
            stop.setArrival(stop.getDeparture());
            stop.setSequence(String.format("%02d", i + 1));
        }
        return stops;
    }

    private static List<TrainSaveRequest.Fare> normalizeFullFares(List<TrainSaveRequest.Fare> input,
                                                                    List<TrainSaveRequest.Stop> stops) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("至少需要配置一条全程票价");
        }
        List<TrainSaveRequest.Fare> result = new ArrayList<>();
        Set<Integer> seatTypes = new HashSet<>();
        String first = stops.get(0).getDeparture();
        String last = stops.get(stops.size() - 1).getDeparture();
        for (TrainSaveRequest.Fare inputFare : input) {
            if (inputFare.getSeatType() == null || inputFare.getPrice() == null || inputFare.getPrice() < 0) {
                throw new IllegalArgumentException("票价和席别不能为空，票价不能为负数");
            }
            if (!seatTypes.add(inputFare.getSeatType())) {
                throw new IllegalArgumentException("同一席别只能配置一条全程票价");
            }
            TrainSaveRequest.Fare fare = new TrainSaveRequest.Fare();
            fare.setDeparture(first);
            fare.setArrival(last);
            fare.setSeatType(inputFare.getSeatType());
            fare.setPrice(inputFare.getPrice());
            result.add(fare);
        }
        return result;
    }

    public static class Calculation {
        private String startStation;
        private String endStation;
        private LocalDateTime departureTime;
        private LocalDateTime arrivalTime;
        private long totalMinutes;
        private List<TrainSaveRequest.Stop> stops;
        private List<TrainSaveRequest.Fare> fullFares;
        private List<TrainSaveRequest.Fare> intervalFares;

        public String getStartStation() { return startStation; }
        public void setStartStation(String startStation) { this.startStation = startStation; }
        public String getEndStation() { return endStation; }
        public void setEndStation(String endStation) { this.endStation = endStation; }
        public LocalDateTime getDepartureTime() { return departureTime; }
        public void setDepartureTime(LocalDateTime departureTime) { this.departureTime = departureTime; }
        public LocalDateTime getArrivalTime() { return arrivalTime; }
        public void setArrivalTime(LocalDateTime arrivalTime) { this.arrivalTime = arrivalTime; }
        public long getTotalMinutes() { return totalMinutes; }
        public void setTotalMinutes(long totalMinutes) { this.totalMinutes = totalMinutes; }
        public List<TrainSaveRequest.Stop> getStops() { return stops; }
        public void setStops(List<TrainSaveRequest.Stop> stops) { this.stops = stops; }
        public List<TrainSaveRequest.Fare> getFullFares() { return fullFares; }
        public void setFullFares(List<TrainSaveRequest.Fare> fullFares) { this.fullFares = fullFares; }
        public List<TrainSaveRequest.Fare> getIntervalFares() { return intervalFares; }
        public void setIntervalFares(List<TrainSaveRequest.Fare> intervalFares) { this.intervalFares = intervalFares; }
    }
}
