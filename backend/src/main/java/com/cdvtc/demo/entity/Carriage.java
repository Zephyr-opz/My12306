package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("carriages")
public class Carriage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long trainId;
    private String carriageNumber;
    private Integer carriageType;
    private Integer seatCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer delFlag;
}

