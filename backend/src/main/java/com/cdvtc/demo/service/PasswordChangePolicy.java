package com.cdvtc.demo.service;

import org.springframework.security.crypto.password.PasswordEncoder;

final class PasswordChangePolicy {

    private PasswordChangePolicy() {
    }

    static void validate(String oldPassword, String newPassword, String confirmPassword,
                         PasswordEncoder passwordEncoder, String encodedOldPassword) {
        if (!passwordEncoder.matches(oldPassword, encodedOldPassword)) {
            throw new RuntimeException("旧密码不正确");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("两次输入的新密码不一致");
        }
        if (passwordEncoder.matches(newPassword, encodedOldPassword)) {
            throw new RuntimeException("新密码不能与旧密码相同");
        }
    }
}
