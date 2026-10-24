package com.cdvtc.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdvtc.demo.common.Result;
import com.cdvtc.demo.entity.Order;
import com.cdvtc.demo.entity.OrderItem;
import com.cdvtc.demo.entity.Pay;
import com.cdvtc.demo.entity.Ticket;
import com.cdvtc.demo.mapper.OrderItemMapper;
import com.cdvtc.demo.mapper.OrderMapper;
import com.cdvtc.demo.mapper.PayMapper;
import com.cdvtc.demo.mapper.SeatSegmentOccupancyMapper;
import com.cdvtc.demo.mapper.TicketMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final TicketMapper ticketMapper;
    private final PayMapper payMapper;
    private final SeatSegmentOccupancyMapper occupancyMapper;

    @GetMapping
    public Result<?> page(@RequestParam(required = false) String orderSn,
                          @RequestParam(required = false) String username,
                          @RequestParam(required = false) String trainNumber,
                          @RequestParam(required = false) Integer status,
                          @RequestParam(defaultValue = "1") Integer current,
                          @RequestParam(defaultValue = "10") Integer size) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(orderSn)) wrapper.like(Order::getOrderSn, orderSn);
        if (StringUtils.hasText(username)) wrapper.like(Order::getUsername, username);
        if (StringUtils.hasText(trainNumber)) wrapper.like(Order::getTrainNumber, trainNumber);
        if (status != null) wrapper.eq(Order::getStatus, status);
        wrapper.orderByDesc(Order::getCreateTime);
        Page<Order> page = orderMapper.selectPage(new Page<>(current, size), wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords());
        result.put("total", page.getTotal());
        result.put("current", page.getCurrent());
        result.put("size", page.getSize());
        return Result.success(result);
    }

    @DeleteMapping("/{orderSn}")
    @Transactional
    public Result<Void> delete(@PathVariable String orderSn) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderSn, orderSn));
        if (order == null) throw new IllegalArgumentException("订单不存在");
        occupancyMapper.releaseByOrder(orderSn);
        payMapper.delete(new LambdaQueryWrapper<Pay>().eq(Pay::getOrderSn, orderSn));
        ticketMapper.delete(new LambdaQueryWrapper<Ticket>().eq(Ticket::getOrderSn, orderSn));
        orderItemMapper.delete(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderSn, orderSn));
        orderMapper.deleteById(order.getId());
        return Result.success();
    }
}
