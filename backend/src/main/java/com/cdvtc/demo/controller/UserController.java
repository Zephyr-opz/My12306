package com.cdvtc.demo.controller;

import com.cdvtc.demo.common.Result;
import com.cdvtc.demo.dto.ChangePasswordRequest;
import com.cdvtc.demo.service.UserService;
import jakarta.validation.Valid;
import com.cdvtc.demo.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/current")
    public Result<UserVO> getCurrentUser(Authentication authentication) {
        return Result.success(userService.getCurrentUser(authentication.getName()));
    }

    @PostMapping("/password")
    public Result<Void> changePassword(Authentication authentication,
                                       @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(authentication.getName(), request);
        return Result.success();
    }
}
