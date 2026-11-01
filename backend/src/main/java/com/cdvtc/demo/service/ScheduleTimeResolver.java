package com.cdvtc.demo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 将模板经停站时间换算到某一天的实际运行计划。
 * 模板时间保留模板起始日期，运行计划日期决定真实年月日。
 */
public final class ScheduleTimeResolver {
    private ScheduleTimeResolver() {
    }

    public static LocalDateTime resolve(LocalDate scheduleDate,
                                        LocalDateTime templateStart,
                                        LocalDateTime templateStationTime) {
        if (scheduleDate == null || templateStationTime == null) {
            return templateStationTime;
        }
        LocalDate templateStartDate = templateStart == null
                ? templateStationTime.toLocalDate()
                : templateStart.toLocalDate();
        long dayOffset = ChronoUnit.DAYS.between(templateStartDate, templateStationTime.toLocalDate());
        return LocalDateTime.of(scheduleDate.plusDays(dayOffset), templateStationTime.toLocalTime());
    }
}
