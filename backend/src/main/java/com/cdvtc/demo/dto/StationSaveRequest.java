package com.cdvtc.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StationSaveRequest {
    @NotBlank
    private String code;
    @NotBlank
    private String name;
    private String spell;
    private String region;
    private String regionName;
}
