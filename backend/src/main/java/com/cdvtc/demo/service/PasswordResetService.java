package com.cdvtc.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordResetService {

    static final Duration CODE_TTL = Duration.ofMinutes(1);
    static final Duration SEND_COOLDOWN = Duration.ofSeconds(60);
    private static final int MAX_ATTEMPTS = 5;

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetCodeSender codeSender;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentHashMap<String, ResetCodeRecord> records = new ConcurrentHashMap<>();

    @Autowired
    public PasswordResetService(UserMapper userMapper,
                                PasswordEncoder passwordEncoder,
                                PasswordResetCodeSender codeSender) {
        this(userMapper, passwordEncoder, codeSender, Clock.systemUTC());
    }

    PasswordResetService(UserMapper userMapper,
                         PasswordEncoder passwordEncoder,
                         PasswordResetCodeSender codeSender,
                         Clock clock) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.codeSender = codeSender;
        this.clock = clock;
    }

    @Transactional
    public void sendCode(String username) {
        String normalizedUsername = normalizeUsername(username);
        User user = findUser(normalizedUsername);
        if (user == null || user.getStatus() == null || user.getStatus() == 0) {
            return;
        }

        long now = Instant.now(clock).toEpochMilli();
        ResetCodeRecord previous = records.get(normalizedUsername);
        if (previous != null && now - previous.sentAt() < SEND_COOLDOWN.toMillis()) {
            throw new RuntimeException("验证码发送过于频繁，请稍后再试");
        }

        String code = String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
        records.put(normalizedUsername,
                new ResetCodeRecord(sha256(code), now + CODE_TTL.toMillis(), now, 0));
        codeSender.send(normalizedUsername, code, CODE_TTL);
    }

    @Transactional
    public void resetPassword(String username, String code, String newPassword, String confirmPassword) {
        String normalizedUsername = normalizeUsername(username);
        validatePassword(code, newPassword, confirmPassword);

        synchronized (records) {
            long now = Instant.now(clock).toEpochMilli();
            ResetCodeRecord record = records.get(normalizedUsername);
            if (record == null || record.expiresAt() <= now || record.attempts() >= MAX_ATTEMPTS) {
                records.remove(normalizedUsername);
                throw new RuntimeException("验证码无效或已过期");
            }

            if (!MessageDigest.isEqual(
                    sha256(code).getBytes(StandardCharsets.US_ASCII),
                    record.codeHash().getBytes(StandardCharsets.US_ASCII))) {
                records.put(normalizedUsername, record.withAttempts(record.attempts() + 1));
                throw new RuntimeException("验证码无效或已过期");
            }

            User user = findUser(normalizedUsername);
            if (user == null || user.getStatus() == null || user.getStatus() == 0) {
                records.remove(normalizedUsername);
                throw new RuntimeException("验证码无效或已过期");
            }

            user.setPassword(passwordEncoder.encode(newPassword));
            userMapper.updateById(user);
            records.remove(normalizedUsername);
        }
    }

    private User findUser(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return userMapper.selectOne(wrapper);
    }

    private static String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new RuntimeException("用户名不能为空");
        }
        return username.trim();
    }

    private static void validatePassword(String code, String newPassword, String confirmPassword) {
        if (code == null || !code.matches("^\\d{6}$")) {
            throw new RuntimeException("验证码必须为6位数字");
        }
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6 || newPassword.length() > 20) {
            throw new RuntimeException("新密码长度为6-20个字符");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("两次输入的新密码不一致");
        }
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 算法不可用", ex);
        }
    }

    private record ResetCodeRecord(String codeHash, long expiresAt, long sentAt, int attempts) {
        private ResetCodeRecord withAttempts(int nextAttempts) {
            return new ResetCodeRecord(codeHash, expiresAt, sentAt, nextAttempts);
        }
    }
}
