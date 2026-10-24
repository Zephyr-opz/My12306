package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("train_stations")
public class TrainStation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long trainId;
    private Long stationId;
    private String sequence;
    private String departure;
    private String arrival;
    private String startRegion;
    private String endRegion;
    private LocalDateTime arrivalTime;
    private LocalDateTime departureTime;
    private Integer stopoverTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer delFlag;
}

