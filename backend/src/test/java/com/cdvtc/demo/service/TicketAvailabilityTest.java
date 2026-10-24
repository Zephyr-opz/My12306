package com.cdvtc.demo.service;

import com.cdvtc.demo.entity.ScheduleSeat;
import com.cdvtc.demo.entity.SeatSegmentOccupancy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TicketAvailabilityTest {

    @Test
    void countsOnlySeatsWithoutOverlappingActiveOccupancy() {
        ScheduleSeat first = seat(1L);
        ScheduleSeat second = seat(2L);
        SeatSegmentOccupancy occupied = occupancy(1L, 0, 2);

        assertEquals(1, SeatAvailabilityCalculator.countAvailable(
                List.of(first, second), List.of(occupied), 0, 2));
        assertEquals(2, SeatAvailabilityCalculator.countAvailable(
                List.of(first, second), List.of(occupied), 2, 4));
        assertEquals(0, SeatAvailabilityCalculator.countAvailable(
                List.of(first), List.of(occupied), 1, 4));
    }

    private static ScheduleSeat seat(Long id) {
        ScheduleSeat seat = new ScheduleSeat();
        seat.setId(id);
        seat.setStatus(1);
        return seat;
    }

    private static SeatSegmentOccupancy occupancy(Long seatId, int start, int end) {
        SeatSegmentOccupancy occupancy = new SeatSegmentOccupancy();
        occupancy.setScheduleSeatId(seatId);
        occupancy.setStartSequence(start);
        occupancy.setEndSequence(end);
        occupancy.setOccupancyStatus(1);
        return occupancy;
    }
}
