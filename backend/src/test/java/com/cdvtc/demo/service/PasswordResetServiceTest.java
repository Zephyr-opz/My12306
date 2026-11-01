package com.cdvtc.demo.service;

import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetServiceTest {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void resetsPasswordWithTheConsoleCodeAndConsumesIt() {
        UserMapper userMapper = mock(UserMapper.class);
        PasswordResetCodeSender sender = mock(PasswordResetCodeSender.class);
        User user = activeUser();
        when(userMapper.selectOne(any())).thenReturn(user);
        PasswordResetService service = service(userMapper, sender, Clock.systemUTC());

        service.sendCode("admin");
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(sender).send(eq("admin"), codeCaptor.capture(), any());

        service.resetPassword("admin", codeCaptor.getValue(), "newpass1", "newpass1");
        assertTrue(passwordEncoder.matches("newpass1", user.getPassword()));
        assertThrows(RuntimeException.class,
                () -> service.resetPassword("admin", codeCaptor.getValue(), "another1", "another1"));
    }

    @Test
    void rejectsAnIncorrectCode() {
        UserMapper userMapper = mock(UserMapper.class);
        PasswordResetCodeSender sender = mock(PasswordResetCodeSender.class);
        when(userMapper.selectOne(any())).thenReturn(activeUser());
        PasswordResetService service = service(userMapper, sender, Clock.systemUTC());

        service.sendCode("admin");

        assertThrows(RuntimeException.class,
                () -> service.resetPassword("admin", "000000", "newpass1", "newpass1"));
    }

    @Test
    void rejectsACodeOlderThanOneMinute() {
        UserMapper userMapper = mock(UserMapper.class);
        PasswordResetCodeSender sender = mock(PasswordResetCodeSender.class);
        when(userMapper.selectOne(any())).thenReturn(activeUser());
        MutableClock clock = new MutableClock(Instant.parse("2026-10-30T10:00:00Z"));
        PasswordResetService service = service(userMapper, sender, clock);

        service.sendCode("admin");
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(sender).send(eq("admin"), codeCaptor.capture(), any());

        clock.advance(Duration.ofMinutes(1).plusSeconds(1));
        assertThrows(RuntimeException.class,
                () -> service.resetPassword("admin", codeCaptor.getValue(), "newpass1", "newpass1"));
    }

    private PasswordResetService service(UserMapper mapper, PasswordResetCodeSender sender, Clock clock) {
        return new PasswordResetService(mapper, passwordEncoder, sender, clock);
    }

    private User activeUser() {
        User user = new User();
        user.setUsername("admin");
        user.setPassword(passwordEncoder.encode("oldpass1"));
        user.setStatus(1);
        return user;
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
