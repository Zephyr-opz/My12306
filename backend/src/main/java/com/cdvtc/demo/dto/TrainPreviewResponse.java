package com.cdvtc.demo.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TrainPreviewResponse {
    private Long trainId;
    private String trainNumber;
    private String startStation;
    private String endStation;
    private String startRegion;
    private String endRegion;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private LocalDate runStartDate;
    private LocalDate runEndDate;
    private Long totalMinutes;
    private List<TrainSaveRequest.Stop> stops;
    private List<TrainSaveRequest.Fare> fullFares;
    private List<TrainSaveRequest.Fare> intervalFares;
    private List<TrainSaveRequest.CarriageItem> carriages;
}
