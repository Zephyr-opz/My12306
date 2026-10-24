package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("passengers")
public class Passenger {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String realName;
    private Integer idType;
    private String idCard;
    private Integer discountType;
    private String phone;
    private LocalDateTime createDate;
    private Integer verifyStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer delFlag;
}

