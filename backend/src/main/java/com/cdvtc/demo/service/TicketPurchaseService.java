package com.cdvtc.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.cdvtc.demo.dto.PurchaseRequest;
import com.cdvtc.demo.entity.*;
import com.cdvtc.demo.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketPurchaseService {

    private final UserMapper userMapper;
    private final TrainMapper trainMapper;
    private final TrainScheduleMapper scheduleMapper;
    private final TrainStationMapper trainStationMapper;
    private final TrainStationPriceMapper priceMapper;
    private final CarriageMapper carriageMapper;
    private final SeatMapper seatMapper;
    private final ScheduleSeatMapper scheduleSeatMapper;
    private final SeatSegmentOccupancyMapper occupancyMapper;
    private final PassengerMapper passengerMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final TicketMapper ticketMapper;
    private final SeatInventoryMapper inventoryMapper;

    @Transactional
    public Map<String, Object> purchase(PurchaseRequest request, String username) {
        User user = currentUser(username);
        Train train = trainMapper.selectById(Long.valueOf(request.getTrainId()));
        if (train == null) {
            throw new IllegalArgumentException("车次不存在");
        }
        LocalDate date = request.getDepartureDate() != null
                ? request.getDepartureDate()
                : train.getDepartureTime().toLocalDate();
        TrainSchedule schedule = scheduleMapper.selectOne(new LambdaQueryWrapper<TrainSchedule>()
                .eq(TrainSchedule::getTrainId, train.getId())
                .eq(TrainSchedule::getDepartureDate, date)
                .eq(TrainSchedule::getSaleStatus, 0)
                .eq(TrainSchedule::getPlanStatus, 1));
        if (schedule == null) {
            throw new IllegalArgumentException("所选日期没有可售运行计划");
        }

        List<TrainStation> route = trainStationMapper.selectList(new LambdaQueryWrapper<TrainStation>()
                .eq(TrainStation::getTrainId, train.getId())
                .orderByAsc(TrainStation::getSequence));
        TrainStation from = findStop(route, request.getDeparture());
        TrainStation to = findStop(route, request.getArrival());
        int fromIndex = route.indexOf(from);
        int toIndex = route.indexOf(to);
        if (from == null || to == null || fromIndex < 0 || toIndex <= fromIndex) {
            throw new IllegalArgumentException("出发站和到达站区间无效");
        }
        if (request.getPassengers() == null || request.getPassengers().isEmpty()) {
            throw new IllegalArgumentException("至少选择一名乘车人");
        }

        TrainStationPrice price = priceMapper.selectOne(new LambdaQueryWrapper<TrainStationPrice>()
                .eq(TrainStationPrice::getTrainId, train.getId())
                .eq(TrainStationPrice::getDeparture, from.getDeparture())
                .eq(TrainStationPrice::getArrival, to.getArrival())
                .eq(TrainStationPrice::getSeatType, request.getChooseType()));
        if (price == null) {
            throw new IllegalArgumentException("所选区间没有该席别票价");
        }

        int startSequence = sequenceOf(from, fromIndex);
        int endSequence = sequenceOf(to, toIndex);
        int passengerCount = request.getPassengers().size();
        ensureScheduleSeats(schedule, train);

        String orderSn = "O" + System.currentTimeMillis()
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        int totalAmount = price.getPrice() * passengerCount;
        Order order = new Order();
        order.setOrderSn(orderSn);
        order.setUserId(user.getId());
        order.setUsername(username);
        order.setTrainId(train.getId());
        order.setScheduleId(schedule.getId());
        order.setTrainNumber(train.getTrainNumber());
        order.setRidingDate(date);
        order.setDeparture(from.getDeparture());
        order.setArrival(to.getArrival());
        order.setDepartureTime(from.getDepartureTime());
        order.setArrivalTime(to.getArrivalTime());
        order.setTotalAmount(totalAmount);
        order.setExpireTime(LocalDateTime.now().plusMinutes(30));
        order.setSource(0);
        order.setStatus(0);
        order.setOrderTime(LocalDateTime.now());
        orderMapper.insert(order);

        for (PurchaseRequest.PassengerRef ref : request.getPassengers()) {
            Passenger passenger = resolvePassenger(ref, username);
            ensurePassengerNotBooked(schedule, passenger.getId());
            ScheduleSeat scheduleSeat = allocateSeat(schedule, request.getChooseType(), startSequence, endSequence);

            OrderItem item = new OrderItem();
            item.setOrderSn(orderSn);
            item.setUserId(user.getId());
            item.setUsername(username);
            item.setTrainId(train.getId());
            item.setScheduleId(schedule.getId());
            item.setScheduleSeatId(scheduleSeat.getId());
            item.setRidingDate(date);
            item.setDeparture(from.getDeparture());
            item.setArrival(to.getArrival());
            item.setCarriageNumber(scheduleSeat.getCarriageNumber());
            item.setSeatType(request.getChooseType());
            item.setSeatNumber(scheduleSeat.getSeatNumber());
            item.setRealName(passenger.getRealName() != null ? passenger.getRealName() : ref.getRealName());
            item.setIdType(passenger.getIdType() != null ? passenger.getIdType() : ref.getIdType());
            item.setIdCard(passenger.getIdCard() != null ? passenger.getIdCard() : ref.getIdCard());
            item.setPhone(passenger.getPhone() != null ? passenger.getPhone() : ref.getPhone());
            item.setTicketType(ref.getTicketType());
            item.setStatus(0);
            item.setAmount(price.getPrice());
            item.setPrice(price.getPrice());
            orderItemMapper.insert(item);

            Ticket ticket = new Ticket();
            ticket.setOrderSn(orderSn);
            ticket.setUsername(username);
            ticket.setTrainId(train.getId());
            ticket.setScheduleId(schedule.getId());
            ticket.setScheduleSeatId(scheduleSeat.getId());
            ticket.setRidingDate(date);
            ticket.setDeparture(from.getDeparture());
            ticket.setArrival(to.getArrival());
            ticket.setCarriageNumber(scheduleSeat.getCarriageNumber());
            ticket.setSeatNumber(scheduleSeat.getSeatNumber());
            ticket.setSeatType(request.getChooseType());
            ticket.setPrice(price.getPrice());
            ticket.setPassengerId(passenger.getId());
            ticket.setTicketStatus(0);
            ticketMapper.insert(ticket);

            SeatSegmentOccupancy occupancy = new SeatSegmentOccupancy();
            occupancy.setScheduleId(schedule.getId());
            occupancy.setScheduleSeatId(scheduleSeat.getId());
            occupancy.setOrderSn(orderSn);
            occupancy.setTicketId(ticket.getId());
            occupancy.setStartSequence(startSequence);
            occupancy.setEndSequence(endSequence);
            occupancy.setOccupancyStatus(1);
            occupancyMapper.insert(occupancy);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("orderSn", orderSn);
        result.put("status", order.getStatus());
        result.put("totalAmount", totalAmount);
        result.put("expireTime", order.getExpireTime());
        return result;
    }

    void ensurePassengerNotBooked(TrainSchedule schedule, Long passengerId) {
        long existing = ticketMapper.selectCount(new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getScheduleId, schedule.getId())
                .eq(Ticket::getPassengerId, passengerId)
                .notIn(Ticket::getTicketStatus, 40, 60));
        if (existing > 0) {
            throw new IllegalArgumentException("该乘车人已购买同一车次同一日期的车票");
        }
    }

    @Transactional
    public void cancel(String orderSn, String username, boolean refund) {
        Order order = ownedOrder(orderSn, username);
        if (!refund && order.getStatus() != 0) {
            throw new IllegalArgumentException("只有待支付订单可以取消");
        }
        if (refund && order.getStatus() != 10) {
            throw new IllegalArgumentException("只有已支付订单可以退票");
        }
        int released = occupancyMapper.releaseByOrder(orderSn);
        if (released == 0) {
            releaseLegacyInventory(order);
        }
        order.setStatus(refund ? 40 : 60);
        if (refund) {
            order.setRefundTime(LocalDateTime.now());
        } else {
            order.setCancelTime(LocalDateTime.now());
        }
        orderMapper.updateById(order);
        orderItemMapper.update(null, new UpdateWrapper<OrderItem>()
                .eq("order_sn", orderSn).set("status", refund ? 40 : 60));
        ticketMapper.update(null, new UpdateWrapper<Ticket>()
                .eq("order_sn", orderSn).set("ticket_status", refund ? 40 : 60));
    }

    public void ensureScheduleSeats(TrainSchedule schedule, Train train) {
        if (scheduleSeatMapper.selectCount(new LambdaQueryWrapper<ScheduleSeat>()
                .eq(ScheduleSeat::getScheduleId, schedule.getId())) > 0) {
            return;
        }
        List<Seat> templates = seatMapper.selectList(new LambdaQueryWrapper<Seat>()
                .eq(Seat::getTrainId, train.getId()).orderByAsc(Seat::getId));
        if (!templates.isEmpty()) {
            for (Seat template : templates) {
                ScheduleSeat seat = new ScheduleSeat();
                seat.setScheduleId(schedule.getId());
                seat.setTrainId(train.getId());
                seat.setCarriageNumber(template.getCarriageNumber());
                seat.setSeatNumber(template.getSeatNumber());
                seat.setSeatType(template.getSeatType());
                seat.setStatus(1);
                scheduleSeatMapper.insertIgnore(seat);
            }
            return;
        }
        List<Carriage> carriages = carriageMapper.selectList(new LambdaQueryWrapper<Carriage>()
                .eq(Carriage::getTrainId, train.getId()).orderByAsc(Carriage::getCarriageNumber));
        for (Carriage carriage : carriages) {
            int count = carriage.getSeatCount() == null ? 0 : carriage.getSeatCount();
            for (int index = 1; index <= count; index++) {
                ScheduleSeat seat = new ScheduleSeat();
                seat.setScheduleId(schedule.getId());
                seat.setTrainId(train.getId());
                seat.setCarriageNumber(carriage.getCarriageNumber());
                seat.setSeatNumber(String.format("%03d", index));
                seat.setSeatType(carriage.getCarriageType());
                seat.setStatus(1);
                scheduleSeatMapper.insertIgnore(seat);
            }
        }
        if (scheduleSeatMapper.selectCount(new LambdaQueryWrapper<ScheduleSeat>()
                .eq(ScheduleSeat::getScheduleId, schedule.getId())) == 0) {
            throw new IllegalArgumentException("该车次没有配置具体座位");
        }
    }

    private ScheduleSeat allocateSeat(TrainSchedule schedule, Integer seatType,
                                      int startSequence, int endSequence) {
        List<ScheduleSeat> candidates = scheduleSeatMapper.findAvailableSeats(
                schedule.getId(), seatType, startSequence, endSequence, 1);
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("余票不足");
        }
        return candidates.get(0);
    }

    private User currentUser(String username) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) throw new IllegalArgumentException("用户不存在");
        return user;
    }

    private Order ownedOrder(String orderSn, String username) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderSn, orderSn).eq(Order::getUsername, username));
        if (order == null) throw new IllegalArgumentException("订单不存在");
        return order;
    }

    private void releaseLegacyInventory(Order order) {
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderSn, order.getOrderSn()));
        Map<Integer, Long> quantities = items.stream().collect(Collectors.groupingBy(
                OrderItem::getSeatType, Collectors.counting()));
        for (Map.Entry<Integer, Long> entry : quantities.entrySet()) {
            UpdateWrapper<SeatInventory> update = new UpdateWrapper<>();
            update.eq("schedule_id", order.getScheduleId())
                    .eq("departure_station", order.getDeparture())
                    .eq("arrival_station", order.getArrival())
                    .eq("seat_type", entry.getKey())
                    .setSql("available_count = LEAST(total_count, available_count + " + entry.getValue() + ")")
                    .setSql("version = version + 1");
            inventoryMapper.update(null, update);
        }
    }

    private int sequenceOf(TrainStation station, int fallback) {
        try {
            return Integer.parseInt(station.getSequence());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private TrainStation findStop(List<TrainStation> route, String station) {
        return route.stream().filter(stop -> station.equals(stop.getDeparture()) || station.equals(stop.getArrival()))
                .findFirst().orElse(null);
    }

    private Passenger resolvePassenger(PurchaseRequest.PassengerRef ref, String username) {
        if (ref.getPassengerId() != null) {
            Passenger passenger = passengerMapper.selectOne(new LambdaQueryWrapper<Passenger>()
                    .eq(Passenger::getId, ref.getPassengerId()).eq(Passenger::getUsername, username));
            if (passenger == null) throw new IllegalArgumentException("乘车人不存在或不属于当前用户");
            return passenger;
        }
        if (!StringUtils.hasText(ref.getRealName())) throw new IllegalArgumentException("乘车人姓名不能为空");
        Passenger passenger = new Passenger();
        passenger.setRealName(ref.getRealName());
        passenger.setIdType(ref.getIdType());
        passenger.setIdCard(ref.getIdCard());
        passenger.setPhone(ref.getPhone());
        passenger.setUsername(username);
        passengerMapper.insert(passenger);
        return passenger;
    }
}
