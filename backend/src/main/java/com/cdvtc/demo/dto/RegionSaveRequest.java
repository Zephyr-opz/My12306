package com.cdvtc.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegionSaveRequest {
    @NotBlank
    private String name;
    private String fullName;
    @NotBlank
    private String code;
    private String initial;
    private String spell;
    private Integer popularFlag = 0;
}
