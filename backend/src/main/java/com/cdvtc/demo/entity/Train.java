package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("trains")
public class Train {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String trainNumber;
    private Integer trainType;
    private String trainTag;
    private String trainBrand;
    private String startStation;
    private String endStation;
    private String startRegion;
    private String endRegion;
    private LocalDateTime saleTime;
    private Integer saleStatus;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private LocalDate runStartDate;
    private LocalDate runEndDate;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer delFlag;
}
