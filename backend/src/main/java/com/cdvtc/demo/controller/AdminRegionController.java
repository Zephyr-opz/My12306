package com.cdvtc.demo.controller;

import com.cdvtc.demo.common.Result;
import com.cdvtc.demo.dto.RegionSaveRequest;
import com.cdvtc.demo.service.RegionStationAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/regions")
@RequiredArgsConstructor
public class AdminRegionController {

    private final RegionStationAdminService service;

    @GetMapping
    public Result<?> list(@RequestParam(required = false) String keyword) {
        return Result.success(service.listRegions(keyword));
    }

    @PostMapping
    public Result<?> create(@Valid @RequestBody RegionSaveRequest request) {
        return Result.success(service.createRegion(request));
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody RegionSaveRequest request) {
        return Result.success(service.updateRegion(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.deleteRegion(id);
        return Result.success();
    }
}
