package com.cdvtc.demo.controller;

import com.cdvtc.demo.common.Result;
import com.cdvtc.demo.dto.StationSaveRequest;
import com.cdvtc.demo.service.RegionStationAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/stations")
@RequiredArgsConstructor
public class AdminStationController {

    private final RegionStationAdminService service;

    @GetMapping
    public Result<?> list(@RequestParam(required = false) String keyword) {
        return Result.success(service.listStations(keyword));
    }

    @PostMapping
    public Result<?> create(@Valid @RequestBody StationSaveRequest request) {
        return Result.success(service.createStation(request));
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody StationSaveRequest request) {
        return Result.success(service.updateStation(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.deleteStation(id);
        return Result.success();
    }
}
