package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("seat_segment_occupancies")
public class SeatSegmentOccupancy {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long scheduleId;
    private Long scheduleSeatId;
    private String orderSn;
    private Long ticketId;
    private Integer startSequence;
    private Integer endSequence;
    private Integer occupancyStatus;
    private LocalDateTime createTime;
    private LocalDateTime releaseTime;
}
