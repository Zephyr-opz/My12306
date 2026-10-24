package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("tickets")
public class Ticket {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderSn;
    private String username;
    private Long trainId;
    private Long scheduleId;
    private Long scheduleSeatId;
    private LocalDate ridingDate;
    private String departure;
    private String arrival;
    private String carriageNumber;
    private String seatNumber;
    private Integer seatType;
    private Integer price;
    private Long passengerId;
    private Integer ticketStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer delFlag;
}
