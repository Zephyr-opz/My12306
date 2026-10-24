package com.cdvtc.demo.service;

import com.cdvtc.demo.dto.TrainSaveRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrainTemplateCalculatorTest {

    @Test
    void distributesRemainingRuntimeAcrossStopsAndCalculatesFareByMinutes() {
        TrainSaveRequest request = request(
                LocalDateTime.of(2026, 10, 22, 8, 0),
                LocalDateTime.of(2026, 10, 22, 12, 0),
                List.of(stop("01", "北京", 0), stop("02", "郑州", 20), stop("03", "杭州", 0)),
                10_000);

        TrainTemplateCalculator.Calculation result = TrainTemplateCalculator.calculate(request);

        assertEquals(240, result.getTotalMinutes());
        assertEquals("北京", result.getStartStation());
        assertEquals("杭州", result.getEndStation());
        assertEquals(LocalDateTime.of(2026, 10, 22, 9, 50), result.getStops().get(1).getArrivalTime());
        assertEquals(LocalDateTime.of(2026, 10, 22, 10, 10), result.getStops().get(1).getDepartureTime());
        assertEquals(LocalDateTime.of(2026, 10, 22, 12, 0), result.getStops().get(2).getArrivalTime());
        assertEquals(4_583, result.getIntervalFares().stream()
                .filter(fare -> "北京".equals(fare.getDeparture()) && "郑州".equals(fare.getArrival()))
                .findFirst().orElseThrow().getPrice());
        assertEquals(10_000, result.getIntervalFares().stream()
                .filter(fare -> "北京".equals(fare.getDeparture()) && "杭州".equals(fare.getArrival()))
                .findFirst().orElseThrow().getPrice());
    }

    @Test
    void givesEarlierSegmentsTheRemainderMinute() {
        TrainSaveRequest request = request(
                LocalDateTime.of(2026, 10, 22, 8, 0),
                LocalDateTime.of(2026, 10, 22, 13, 3),
                List.of(stop("01", "甲", 0), stop("02", "乙", 1), stop("03", "丙", 1), stop("04", "丁", 0)),
                12_000);

        TrainTemplateCalculator.Calculation result = TrainTemplateCalculator.calculate(request);

        assertEquals(LocalDateTime.of(2026, 10, 22, 9, 41), result.getStops().get(1).getArrivalTime());
        assertEquals(LocalDateTime.of(2026, 10, 22, 9, 42), result.getStops().get(1).getDepartureTime());
        assertEquals(LocalDateTime.of(2026, 10, 22, 11, 22), result.getStops().get(2).getArrivalTime());
    }

    @Test
    void supportsOvernightTemplate() {
        TrainSaveRequest request = request(
                LocalDateTime.of(2026, 10, 22, 23, 0),
                LocalDateTime.of(2026, 10, 22, 1, 0),
                List.of(stop("01", "上海", 0), stop("02", "北京", 0)),
                5_000);

        TrainTemplateCalculator.Calculation result = TrainTemplateCalculator.calculate(request);

        assertEquals(120, result.getTotalMinutes());
        assertEquals(LocalDate.of(2026, 10, 23), result.getStops().get(1).getArrivalTime().toLocalDate());
    }

    @Test
    void rejectsWhenRuntimeCannotCoverEverySegment() {
        TrainSaveRequest request = request(
                LocalDateTime.of(2026, 10, 22, 8, 0),
                LocalDateTime.of(2026, 10, 22, 8, 1),
                List.of(stop("01", "甲", 0), stop("02", "乙", 0), stop("03", "丙", 0)),
                1_000);

        assertThrows(IllegalArgumentException.class, () -> TrainTemplateCalculator.calculate(request));
    }

    private static TrainSaveRequest request(LocalDateTime departure, LocalDateTime arrival,
                                             List<TrainSaveRequest.Stop> stops, int price) {
        TrainSaveRequest request = new TrainSaveRequest();
        request.setTrainNumber("GTEST");
        request.setDepartureTime(departure);
        request.setArrivalTime(arrival);
        request.setRunStartDate(LocalDate.of(2026, 10, 22));
        request.setRunEndDate(LocalDate.of(2026, 10, 22));
        request.setStops(stops);
        TrainSaveRequest.Fare fare = new TrainSaveRequest.Fare();
        fare.setSeatType(2);
        fare.setPrice(price);
        fare.setDeparture(stops.get(0).getDeparture());
        fare.setArrival(stops.get(stops.size() - 1).getDeparture());
        request.setFares(List.of(fare));
        return request;
    }

    private static TrainSaveRequest.Stop stop(String sequence, String station, int stopover) {
        TrainSaveRequest.Stop stop = new TrainSaveRequest.Stop();
        stop.setSequence(sequence);
        stop.setDeparture(station);
        stop.setArrival(station);
        stop.setStopoverTime(stopover);
        return stop;
    }
}
