package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("seat_inventories")
public class SeatInventory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long scheduleId;
    private Long trainId;
    private String departureStation;
    private String arrivalStation;
    private Integer seatType;
    private Integer totalCount;
    private Integer availableCount;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
