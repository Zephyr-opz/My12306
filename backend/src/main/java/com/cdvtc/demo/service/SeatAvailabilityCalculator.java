package com.cdvtc.demo.service;

import com.cdvtc.demo.entity.ScheduleSeat;
import com.cdvtc.demo.entity.SeatSegmentOccupancy;

import java.util.List;
import java.util.Objects;

/** Calculates concrete seats available for one station interval. */
public final class SeatAvailabilityCalculator {

    private SeatAvailabilityCalculator() {
    }

    public static int countAvailable(List<ScheduleSeat> seats,
                                     List<SeatSegmentOccupancy> occupancies,
                                     int startSequence,
                                     int endSequence) {
        validateInterval(startSequence, endSequence);
        return (int) seats.stream()
                .filter(seat -> Objects.equals(seat.getStatus(), 1))
                .filter(seat -> occupancies.stream().noneMatch(occupancy ->
                        Objects.equals(occupancy.getScheduleSeatId(), seat.getId())
                                && Objects.equals(occupancy.getOccupancyStatus(), 1)
                                && SeatIntervalAllocator.overlaps(
                                occupancy.getStartSequence(), occupancy.getEndSequence(),
                                startSequence, endSequence)))
                .count();
    }

    private static void validateInterval(int startSequence, int endSequence) {
        if (startSequence < 0 || endSequence <= startSequence) {
            throw new IllegalArgumentException("车站区间无效");
        }
    }
}
