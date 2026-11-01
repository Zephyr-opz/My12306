package com.cdvtc.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cdvtc.demo.dto.LoginRequest;
import com.cdvtc.demo.dto.ChangePasswordRequest;
import com.cdvtc.demo.dto.RegisterRequest;
import com.cdvtc.demo.dto.UserCreateRequest;
import com.cdvtc.demo.dto.UserUpdateRequest;
import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.mapper.UserMapper;
import com.cdvtc.demo.security.UserRole;
import com.cdvtc.demo.util.JwtUtil;
import com.cdvtc.demo.vo.LoginResponse;
import com.cdvtc.demo.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<UserMapper, User> {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        User user = findByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }

        return new LoginResponse(jwtUtil.generateToken(user.getUsername()), convertToVO(user));
    }

    public void register(RegisterRequest request) {
        if (lambdaQuery().eq(User::getUsername, request.getUsername()).exists()) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(UserRole.USER.name());
        user.setStatus(1);
        save(user);
    }

    public UserVO getCurrentUser(String username) {
        User user = findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return convertToVO(user);
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        PasswordChangePolicy.validate(request.getOldPassword(), request.getNewPassword(),
                request.getConfirmPassword(), passwordEncoder, user.getPassword());
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        updateById(user);
    }

    public User findActiveUser(String username) {
        return lambdaQuery()
                .eq(User::getUsername, username)
                .eq(User::getStatus, 1)
                .one();
    }

    public IPage<UserVO> getUserList(Integer page, Integer size, String keyword) {
        Page<User> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like(User::getUsername, keyword)
                    .or().like(User::getRealName, keyword)
                    .or().like(User::getEmail, keyword)
                    .or().like(User::getPhone, keyword));
        }
        wrapper.orderByDesc(User::getCreateTime);
        return page(pageParam, wrapper).convert(this::convertToVO);
    }

    public UserVO createUser(UserCreateRequest request) {
        if (lambdaQuery().eq(User::getUsername, request.getUsername()).exists()) {
            throw new RuntimeException("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(UserRole.from(request.getRole()).name());
        user.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        save(user);
        return convertToVO(user);
    }

    public void updateUser(UserUpdateRequest request, String operatorUsername) {
        User user = getById(request.getId());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (StringUtils.hasText(request.getRealName())) {
            user.setRealName(request.getRealName());
        }
        if (StringUtils.hasText(request.getEmail())) {
            user.setEmail(request.getEmail());
        }
        if (StringUtils.hasText(request.getPhone())) {
            user.setPhone(request.getPhone());
        }
        if (StringUtils.hasText(request.getRole())) {
            String nextRole = UserRole.from(request.getRole()).name();
            if (UserRole.ADMIN.name().equals(user.getRole()) && UserRole.USER.name().equals(nextRole)
                    && countAdmins() <= 1) {
                throw new RuntimeException("系统至少需要一个管理员");
            }
            user.setRole(nextRole);
        }
        if (request.getStatus() != null) {
            if (user.getUsername().equals(operatorUsername) && request.getStatus() == 0) {
                throw new RuntimeException("不能禁用当前登录账号");
            }
            if (UserRole.ADMIN.name().equals(user.getRole()) && request.getStatus() == 0 && countAdmins() <= 1) {
                throw new RuntimeException("系统至少需要一个管理员");
            }
            user.setStatus(request.getStatus());
        }
        updateById(user);
    }

    public void deleteUser(Long id, String operatorUsername) {
        User user = getById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getUsername().equals(operatorUsername)) {
            throw new RuntimeException("不能删除当前登录账号");
        }
        if (UserRole.ADMIN.name().equals(user.getRole()) && countAdmins() <= 1) {
            throw new RuntimeException("系统至少需要一个管理员");
        }
        removeById(id);
    }

    private User findByUsername(String username) {
        return lambdaQuery().eq(User::getUsername, username).one();
    }

    private long countAdmins() {
        return lambdaQuery().eq(User::getRole, UserRole.ADMIN.name()).eq(User::getDeleted, 0).count();
    }

    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
