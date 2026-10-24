package com.cdvtc.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cdvtc.demo.entity.ScheduleSeat;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ScheduleSeatMapper extends BaseMapper<ScheduleSeat> {

    @Insert("INSERT IGNORE INTO schedule_seats "
            + "(schedule_id, train_id, carriage_number, seat_number, seat_type, status) "
            + "VALUES (#{scheduleId}, #{trainId}, #{carriageNumber}, #{seatNumber}, #{seatType}, #{status})")
    int insertIgnore(ScheduleSeat seat);

    @Select("SELECT ss.* FROM schedule_seats ss "
            + "WHERE ss.schedule_id = #{scheduleId} AND ss.seat_type = #{seatType} AND ss.status = 1 "
            + "AND NOT EXISTS (SELECT 1 FROM seat_segment_occupancies o "
            + "WHERE o.schedule_seat_id = ss.id AND o.occupancy_status = 1 "
            + "AND o.start_sequence < #{endSequence} AND o.end_sequence > #{startSequence}) "
            + "ORDER BY ss.id LIMIT #{limit} FOR UPDATE")
    List<ScheduleSeat> findAvailableSeats(@Param("scheduleId") Long scheduleId,
                                           @Param("seatType") Integer seatType,
                                           @Param("startSequence") Integer startSequence,
                                           @Param("endSequence") Integer endSequence,
                                           @Param("limit") Integer limit);

    @Select("SELECT COUNT(*) FROM schedule_seats ss "
            + "WHERE ss.schedule_id = #{scheduleId} AND ss.seat_type = #{seatType} AND ss.status = 1 "
            + "AND NOT EXISTS (SELECT 1 FROM seat_segment_occupancies o "
            + "WHERE o.schedule_seat_id = ss.id AND o.occupancy_status = 1 "
            + "AND o.start_sequence < #{endSequence} AND o.end_sequence > #{startSequence})")
    int countAvailableSeats(@Param("scheduleId") Long scheduleId,
                            @Param("seatType") Integer seatType,
                            @Param("startSequence") Integer startSequence,
                            @Param("endSequence") Integer endSequence);
}
