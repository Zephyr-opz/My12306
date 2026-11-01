package com.cdvtc.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.cdvtc.demo.common.Result;
import com.cdvtc.demo.entity.Order;
import com.cdvtc.demo.entity.OrderItem;
import com.cdvtc.demo.entity.Pay;
import com.cdvtc.demo.entity.Ticket;
import com.cdvtc.demo.mapper.OrderMapper;
import com.cdvtc.demo.mapper.OrderItemMapper;
import com.cdvtc.demo.mapper.PayMapper;
import com.cdvtc.demo.mapper.TicketMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PayController {

    private final PayMapper payMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final TicketMapper ticketMapper;

    @Transactional
    @PostMapping("/api/pay-service/pay/create")
    public Result<?> createPay(@RequestBody PayCreateRequest req, Authentication authentication) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderSn, req.getOrderSn())
                .eq(Order::getUsername, authentication.getName()));
        if (order == null) {
            return Result.error(404, "订单不存在");
        }
        if (order.getStatus() != 0) {
            return Result.error(400, "订单状态不允许支付");
        }

        Pay pay = new Pay();
        pay.setOrderSn(req.getOrderSn());
        pay.setPaySn("PAY" + System.currentTimeMillis());
        pay.setChannel(req.getChannel());
        pay.setTradeType(req.getTradeType());
        pay.setSubject(req.getSubject());
        // 金额以订单为准，不能信任前端传入的金额字段。
        pay.setTotalAmount(order.getTotalAmount());
        pay.setStatus(1);
        pay.setPayTime(LocalDateTime.now());
        payMapper.insert(pay);

        order.setStatus(10);
        order.setPayType(req.getChannel());
        order.setPayTime(pay.getPayTime());
        orderMapper.updateById(order);

        // 支付成功后同步订单明细和车票状态，避免订单显示“已支付”而票明细仍显示“待支付”。
        orderItemMapper.update(null, new UpdateWrapper<OrderItem>()
                .eq("order_sn", order.getOrderSn())
                .set("status", 10));
        ticketMapper.update(null, new UpdateWrapper<Ticket>()
                .eq("order_sn", order.getOrderSn())
                .set("ticket_status", 10));

        Map<String, Object> result = new HashMap<>();
        result.put("paySn", pay.getPaySn());
        result.put("status", 1);
        return Result.success(result);
    }

    @GetMapping("/api/pay-service/pay/query/order-sn")
    public Result<?> queryPayByOrderSn(@RequestParam String orderSn, Authentication authentication) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderSn, orderSn));
        if (order == null || (!isAdmin(authentication) && !order.getUsername().equals(authentication.getName()))) {
            return Result.error(404, "订单不存在");
        }
        Pay pay = payMapper.selectOne(new LambdaQueryWrapper<Pay>().eq(Pay::getOrderSn, orderSn));
        if (pay == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", 0);
            return Result.success(result);
        }
        return Result.success(pay);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    @Data
    public static class PayCreateRequest {
        private String orderSn;
        private Integer channel;
        private Integer tradeType;
        private String subject;
        private Integer totalAmount;
    }
}
