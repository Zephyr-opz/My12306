package com.cdvtc.demo.controller;

import com.cdvtc.demo.common.Result;
import com.cdvtc.demo.dto.TrainSaveRequest;
import com.cdvtc.demo.service.TrainAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/trains")
@RequiredArgsConstructor
public class AdminTrainController {

    private final TrainAdminService trainAdminService;

    @GetMapping
    public Result<?> list(@RequestParam(required = false) String keyword) {
        return Result.success(trainAdminService.list(keyword));
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(trainAdminService.detail(id));
    }

    @PostMapping("/preview")
    public Result<?> preview(@Valid @RequestBody TrainSaveRequest request) {
        return Result.success(trainAdminService.preview(request));
    }

    @PostMapping
    public Result<?> create(@Valid @RequestBody TrainSaveRequest request) {
        return Result.success(trainAdminService.create(request));
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody TrainSaveRequest request) {
        return Result.success(trainAdminService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        trainAdminService.delete(id);
        return Result.success();
    }
}
