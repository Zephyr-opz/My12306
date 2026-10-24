package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("train_station_prices")
public class TrainStationPrice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long trainId;
    private String departure;
    private String arrival;
    private Integer seatType;
    private Integer price;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer delFlag;
}

