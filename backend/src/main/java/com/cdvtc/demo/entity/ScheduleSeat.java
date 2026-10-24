package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("schedule_seats")
public class ScheduleSeat {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long scheduleId;
    private Long trainId;
    private String carriageNumber;
    private String seatNumber;
    private Integer seatType;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
