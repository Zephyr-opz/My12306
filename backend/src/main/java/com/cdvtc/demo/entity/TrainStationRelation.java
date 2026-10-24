package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("train_station_relations")
public class TrainStationRelation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long trainId;
    private String departure;
    private String arrival;
    private String startRegion;
    private String endRegion;
    private Integer departureFlag;
    private Integer arrivalFlag;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer delFlag;
}

