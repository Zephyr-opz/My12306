package com.cdvtc.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cdvtc.demo.entity.SeatSegmentOccupancy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SeatSegmentOccupancyMapper extends BaseMapper<SeatSegmentOccupancy> {

    @Update("UPDATE seat_segment_occupancies SET occupancy_status = 0, release_time = NOW() "
            + "WHERE order_sn = #{orderSn} AND occupancy_status = 1")
    int releaseByOrder(@Param("orderSn") String orderSn);
}
