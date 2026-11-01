package com.cdvtc.demo.service;

import java.time.Duration;

@FunctionalInterface
public interface PasswordResetCodeSender {

    void send(String username, String code, Duration expiresIn);
}
