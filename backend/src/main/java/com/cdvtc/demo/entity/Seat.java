package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("seats")
public class Seat {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long trainId;
    private String carriageNumber;
    private String seatNumber;
    private Integer seatType;
    private String startStation;
    private String endStation;
    private Integer price;
    private Integer seatStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer delFlag;
}
