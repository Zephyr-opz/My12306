package com.cdvtc.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cdvtc.demo.common.Result;
import com.cdvtc.demo.entity.Passenger;
import com.cdvtc.demo.mapper.PassengerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class PassengerController {
    private final PassengerMapper passengerMapper;

    @GetMapping("/api/user-service/passenger/query")
    public Result<?> queryPassengers(@RequestParam(required = false) String username) {
        String currentUsername = currentUsername();
        String queryUsername = username == null || username.isBlank() ? currentUsername : username;
        List<Passenger> list = passengerMapper.selectList(new LambdaQueryWrapper<Passenger>().eq(Passenger::getUsername, queryUsername));
        return Result.success(list);
    }

    @PostMapping("/api/user-service/passenger/save")
    public Result<?> savePassenger(@RequestBody Passenger passenger) {
        passenger.setUsername(currentUsername());
        passenger.setCreateDate(LocalDateTime.now());
        passenger.setVerifyStatus(1);
        passengerMapper.insert(passenger);
        return Result.success();
    }

    @PostMapping("/api/user-service/passenger/update")
    public Result<?> updatePassenger(@RequestBody Passenger passenger) {
        passengerMapper.updateById(passenger);
        return Result.success();
    }

    @PostMapping("/api/user-service/passenger/remove")
    public Result<?> removePassenger(@RequestBody Passenger passenger) {
        passengerMapper.deleteById(passenger.getId());
        return Result.success();
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }
}
