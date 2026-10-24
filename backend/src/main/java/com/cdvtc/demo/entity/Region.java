package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("regions")
public class Region {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String fullName;
    private String code;
    private String initial;
    private String spell;
    private Integer popularFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer delFlag;
}
