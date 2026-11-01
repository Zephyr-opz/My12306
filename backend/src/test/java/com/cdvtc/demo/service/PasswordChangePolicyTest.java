package com.cdvtc.demo.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordChangePolicyTest {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void rejectsWhenOldPasswordDoesNotMatch() {
        assertThrows(RuntimeException.class, () -> PasswordChangePolicy.validate(
                "wrong-old", "new-password", "new-password",
                passwordEncoder, passwordEncoder.encode("old-password")));
    }

    @Test
    void acceptsMatchingOldAndNewPasswords() {
        assertDoesNotThrow(() -> PasswordChangePolicy.validate(
                "old-password", "new-password", "new-password",
                passwordEncoder, passwordEncoder.encode("old-password")));
    }
}
