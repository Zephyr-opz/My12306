package com.cdvtc.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cdvtc.demo.common.Result;
import com.cdvtc.demo.entity.Order;
import com.cdvtc.demo.entity.OrderItem;
import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.mapper.OrderItemMapper;
import com.cdvtc.demo.mapper.OrderMapper;
import com.cdvtc.demo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class OrderController {
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;

    @GetMapping("/api/order-service/order/ticket/query")
    public Result<?> queryOrder(@RequestParam String orderSn, Authentication authentication) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>().eq(Order::getOrderSn, orderSn);
        if (!isAdmin(authentication)) {
            wrapper.eq(Order::getUsername, authentication.getName());
        }
        Order order = orderMapper.selectOne(wrapper);
        if (order == null) return Result.error("订单不存在");
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderSn, orderSn));
        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("passengerDetails", items);
        return Result.success(result);
    }

    @GetMapping("/api/order-service/order/ticket/page")
    public Result<?> pageOrders(@RequestParam(required = false) Long userId,
                                @RequestParam(defaultValue = "1") Integer current,
                                @RequestParam(defaultValue = "10") Integer size,
                                Authentication authentication) {
        Long uid = isAdmin(authentication) && userId != null ? userId : currentUserId(authentication);
        return page(uid, current, size);
    }

    @GetMapping("/api/order-service/order/ticket/self/page")
    public Result<?> selfPageOrders(@RequestParam(defaultValue = "1") Integer current,
                                    @RequestParam(defaultValue = "10") Integer size,
                                    Authentication authentication) {
        return page(currentUserId(authentication), current, size);
    }

    private Result<?> page(Long uid, Integer current, Integer size) {
        Page<Order> page = new Page<>(current, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (uid != null) wrapper.eq(Order::getUserId, uid);
        wrapper.orderByDesc(Order::getCreateTime);
        orderMapper.selectPage(page, wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords());
        result.put("total", page.getTotal());
        result.put("current", page.getCurrent());
        result.put("size", page.getSize());
        return Result.success(result);
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, authentication.getName()));
        return user == null ? null : user.getId();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
