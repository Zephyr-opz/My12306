package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("payments")
public class Pay {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderSn;
    private String paySn;
    private String outOrderSn;
    private Integer channel;
    private Integer tradeType;
    private String subject;
    private String orderRequestId;
    private Integer totalAmount;
    private Integer status;
    private LocalDateTime payTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer delFlag;
}

