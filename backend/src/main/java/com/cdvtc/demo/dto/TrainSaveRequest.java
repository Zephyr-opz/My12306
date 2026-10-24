package com.cdvtc.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TrainSaveRequest {
    private Long id;
    @NotBlank
    private String trainNumber;
    private Integer trainType;
    private String trainTag;
    private String trainBrand;
    private String startStation;
    private String endStation;
    private String startRegion;
    private String endRegion;
    private LocalDateTime saleTime;
    private Integer saleStatus = 0;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private LocalDate runStartDate;
    private LocalDate runEndDate;

    @Valid
    @NotEmpty
    private List<Stop> stops;
    @Valid
    private List<Fare> fares;
    @Valid
    private List<CarriageItem> carriages;

    @Data
    public static class Stop {
        private String sequence;
        private Long stationId;
        @NotBlank
        private String departure;
        private String arrival;
        private String startRegion;
        private String endRegion;
        private LocalDateTime arrivalTime;
        private LocalDateTime departureTime;
        private Integer stopoverTime;
    }

    @Data
    public static class Fare {
        @NotBlank
        private String departure;
        @NotBlank
        private String arrival;
        private Integer seatType;
        private Integer price;
    }

    @Data
    public static class CarriageItem {
        @NotBlank
        private String carriageNumber;
        private Integer carriageType;
        private Integer seatCount;
    }

}
