package com.cdvtc.demo.controller;

import com.cdvtc.demo.common.Result;
import com.cdvtc.demo.service.UserService;
import com.cdvtc.demo.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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
}
