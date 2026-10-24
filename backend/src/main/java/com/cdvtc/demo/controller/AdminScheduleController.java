package com.cdvtc.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cdvtc.demo.common.Result;
import com.cdvtc.demo.dto.ScheduleSaveRequest;
import com.cdvtc.demo.dto.ScheduleStatusRangeRequest;
import com.cdvtc.demo.entity.Order;
import com.cdvtc.demo.entity.Ticket;
import com.cdvtc.demo.entity.Train;
import com.cdvtc.demo.entity.TrainSchedule;
import com.cdvtc.demo.mapper.OrderMapper;
import com.cdvtc.demo.mapper.TicketMapper;
import com.cdvtc.demo.mapper.TrainMapper;
import com.cdvtc.demo.mapper.TrainScheduleMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/schedules")
@RequiredArgsConstructor
public class AdminScheduleController {

    private final TrainScheduleMapper scheduleMapper;
    private final TrainMapper trainMapper;
    private final OrderMapper orderMapper;
    private final TicketMapper ticketMapper;

    @GetMapping
    public Result<List<TrainSchedule>> list(@RequestParam(required = false) Long trainId,
                                            @RequestParam(required = false) LocalDate departureDate) {
        LambdaQueryWrapper<TrainSchedule> wrapper = new LambdaQueryWrapper<>();
        if (trainId != null) {
            wrapper.eq(TrainSchedule::getTrainId, trainId);
        }
        if (departureDate != null) {
            wrapper.eq(TrainSchedule::getDepartureDate, departureDate);
        }
        return Result.success(scheduleMapper.selectList(wrapper.orderByAsc(TrainSchedule::getDepartureDate)));
    }

    @PostMapping
    @Transactional
    public Result<TrainSchedule> create(@Valid @RequestBody ScheduleSaveRequest request) {
        requireTrain(request.getTrainId());
        ensureUnique(request.getTrainId(), request.getDepartureDate(), null);
        TrainSchedule schedule = new TrainSchedule();
        BeanUtils.copyProperties(request, schedule);
        scheduleMapper.insert(schedule);
        return Result.success(schedule);
    }

    @PutMapping("/{id}")
    @Transactional
    public Result<TrainSchedule> update(@PathVariable Long id,
                                         @Valid @RequestBody ScheduleSaveRequest request) {
        TrainSchedule schedule = scheduleMapper.selectById(id);
        if (schedule == null) {
            throw new IllegalArgumentException("运行计划不存在");
        }
        requireTrain(request.getTrainId());
        ensureUnique(request.getTrainId(), request.getDepartureDate(), id);
        BeanUtils.copyProperties(request, schedule);
        schedule.setId(id);
        scheduleMapper.updateById(schedule);
        return Result.success(schedule);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        TrainSchedule schedule = scheduleMapper.selectById(id);
        if (schedule == null) {
            throw new IllegalArgumentException("运行计划不存在");
        }
        if (hasActiveOrder(id) || hasActiveTicket(id)) {
            throw new IllegalArgumentException("已有订单或车票的运行计划不能删除");
        }
        scheduleMapper.deleteById(id);
        return Result.success();
    }

    @PutMapping("/status-range")
    @Transactional
    public Result<Integer> updateStatusRange(@Valid @RequestBody ScheduleStatusRangeRequest request) {
        requireTrain(request.getTrainId());
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        }
        if (request.getPlanStatus() != 0 && request.getPlanStatus() != 1) {
            throw new IllegalArgumentException("计划状态只能是 0 或 1");
        }
        List<TrainSchedule> schedules = scheduleMapper.selectList(new LambdaQueryWrapper<TrainSchedule>()
                .eq(TrainSchedule::getTrainId, request.getTrainId())
                .ge(TrainSchedule::getDepartureDate, request.getStartDate())
                .le(TrainSchedule::getDepartureDate, request.getEndDate()));
        if (request.getPlanStatus() == 0) {
            for (TrainSchedule schedule : schedules) {
                if (hasActiveOrder(schedule.getId()) || hasActiveTicket(schedule.getId())) {
                    throw new IllegalArgumentException("范围内已有订单或车票的运行计划不能停用");
                }
            }
        }
        if (schedules.isEmpty()) {
            return Result.success(0);
        }
        int changed = scheduleMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<TrainSchedule>()
                .eq("train_id", request.getTrainId())
                .ge("departure_date", request.getStartDate())
                .le("departure_date", request.getEndDate())
                .set("plan_status", request.getPlanStatus()));
        return Result.success(changed);
    }

    private boolean hasActiveOrder(Long scheduleId) {
        return orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getScheduleId, scheduleId)
                .notIn(Order::getStatus, 40, 60)) > 0;
    }

    private boolean hasActiveTicket(Long scheduleId) {
        return ticketMapper.selectCount(new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getScheduleId, scheduleId)
                .notIn(Ticket::getTicketStatus, 40, 60)) > 0;
    }

    private void requireTrain(Long trainId) {
        Train train = trainMapper.selectById(trainId);
        if (train == null) {
            throw new IllegalArgumentException("车次不存在");
        }
    }

    private void ensureUnique(Long trainId, LocalDate date, Long excludeId) {
        LambdaQueryWrapper<TrainSchedule> wrapper = new LambdaQueryWrapper<TrainSchedule>()
                .eq(TrainSchedule::getTrainId, trainId)
                .eq(TrainSchedule::getDepartureDate, date);
        if (excludeId != null) {
            wrapper.ne(TrainSchedule::getId, excludeId);
        }
        if (scheduleMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("同一车次在同一天只能有一条运行计划");
        }
    }
}
