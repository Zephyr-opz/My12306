package com.cdvtc.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PurchaseRequest {
    @NotBlank
    private String trainId;
    @NotBlank
    private String departure;
    @NotBlank
    private String arrival;
    private LocalDate departureDate;
    @NotNull
    private Integer chooseType;
    @NotEmpty
    private List<PassengerRef> passengers;

    @Data
    public static class PassengerRef {
        private Long passengerId;
        private String realName;
        private Integer idType;
        private String idCard;
        private String phone;
        private Integer ticketType;
    }
}
