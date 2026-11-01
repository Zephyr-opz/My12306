package com.cdvtc.demo.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScheduleTimeResolverTest {

    @Test
    void mapsTemplateStationTimeToSelectedScheduleDate() {
        LocalDateTime templateStart = LocalDateTime.of(2026, 10, 22, 10, 0);
        LocalDateTime templateStation = LocalDateTime.of(2026, 10, 22, 13, 57);

        assertEquals(
                LocalDateTime.of(2026, 10, 29, 13, 57),
                ScheduleTimeResolver.resolve(LocalDate.of(2026, 10, 29), templateStart, templateStation)
        );
    }

    @Test
    void preservesNextDayOffsetForOvernightTrain() {
        LocalDateTime templateStart = LocalDateTime.of(2026, 10, 22, 22, 30);
        LocalDateTime templateStation = LocalDateTime.of(2026, 10, 23, 1, 10);

        assertEquals(
                LocalDateTime.of(2026, 10, 30, 1, 10),
                ScheduleTimeResolver.resolve(LocalDate.of(2026, 10, 29), templateStart, templateStation)
        );
    }
}
