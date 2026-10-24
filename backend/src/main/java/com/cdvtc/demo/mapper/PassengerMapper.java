package com.cdvtc.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cdvtc.demo.entity.Passenger;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PassengerMapper extends BaseMapper<Passenger> {
}

