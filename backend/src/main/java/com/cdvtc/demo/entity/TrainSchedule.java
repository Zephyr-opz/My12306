package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("train_schedules")
public class TrainSchedule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long trainId;
    private LocalDate departureDate;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private Integer saleStatus;
    private Integer planStatus;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
