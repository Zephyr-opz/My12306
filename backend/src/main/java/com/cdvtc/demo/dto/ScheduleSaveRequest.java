package com.cdvtc.demo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ScheduleSaveRequest {
    @NotNull
    private Long trainId;
    @NotNull
    private LocalDate departureDate;
    @NotNull
    private LocalDateTime departureTime;
    @NotNull
    private LocalDateTime arrivalTime;
    private Integer saleStatus = 0;
    private Integer planStatus = 1;
    private String remark;
}
