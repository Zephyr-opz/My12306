package com.cdvtc.demo.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrainScheduleGenerationTest {

    @Test
    void createsInclusiveDatesAcrossMonth() {
        assertEquals(
                List.of(
                        LocalDate.of(2026, 10, 30),
                        LocalDate.of(2026, 10, 31),
                        LocalDate.of(2026, 11, 1),
                        LocalDate.of(2026, 11, 2)),
                TrainAdminService.buildScheduleDates(
                        LocalDate.of(2026, 10, 30),
                        LocalDate.of(2026, 11, 2)));
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        assertThrows(IllegalArgumentException.class, () ->
                TrainAdminService.buildScheduleDates(
                        LocalDate.of(2026, 11, 2),
                        LocalDate.of(2026, 10, 30)));
    }
}
