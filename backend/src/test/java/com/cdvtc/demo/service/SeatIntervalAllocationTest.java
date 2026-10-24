package com.cdvtc.demo.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeatIntervalAllocationTest {

    @Test
    void adjacentIntervalsCanReuseTheSameSeat() {
        assertFalse(SeatIntervalAllocator.overlaps(0, 2, 2, 4));
    }

    @Test
    void overlappingIntervalsCannotReuseTheSameSeat() {
        assertTrue(SeatIntervalAllocator.overlaps(0, 2, 1, 4));
    }

    @Test
    void invalidIntervalIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                SeatIntervalAllocator.overlaps(2, 2, 2, 4));
    }
}
