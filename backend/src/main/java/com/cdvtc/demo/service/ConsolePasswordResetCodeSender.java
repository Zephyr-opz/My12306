package com.cdvtc.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
public class ConsolePasswordResetCodeSender implements PasswordResetCodeSender {

    @Override
    public void send(String username, String code, Duration expiresIn) {
        log.warn("[TEST ONLY] password reset code: username={}, code={}, expiresInSeconds={}",
                username, code, expiresIn.toSeconds());
    }
}
