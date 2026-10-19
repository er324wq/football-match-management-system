package com.football.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.football.dto.LoginDTO;
import com.football.entity.User;
import com.football.mapper.UserMapper;
import com.football.service.UserService;
import com.football.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Value("${INITIAL_ADMIN_PASSWORD:}")
    private String initialAdminPassword;

    @PostConstruct
    public void init() {
        User user = baseMapper.selectByUsername("admin");
        if (user == null) {
            if (initialAdminPassword == null || initialAdminPassword.trim().isEmpty()) {
                throw new IllegalStateException(
                        "INITIAL_ADMIN_PASSWORD must be set when initializing the admin account.");
            }
            user = new User();
            user.setUsername("admin");
            user.setPassword(passwordEncoder.encode(initialAdminPassword));
            user.setNickname("管理员");
            user.setPhone("13800000000");
            user.setEmail("admin@example.com");
            user.setRole("admin");
            user.setStatus(1);
            user.setDeleted(0);
            save(user);
        }
    }

    @Override
    public Map<String, Object> login(LoginDTO loginDTO) {
        String username = loginDTO.getUsername() == null ? "" : loginDTO.getUsername().trim();
        String password = loginDTO.getPassword() == null ? "" : loginDTO.getPassword().trim();

        User user = baseMapper.selectByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (!verifyPassword(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (!isBCryptHash(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(password));
            updateById(user);
        }

        user.setPassword(null);
        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("token", jwtService.createToken(user.getId(), user.getUsername(), user.getRole()));
        return result;
    }

    @Override
    public User getUserInfo(Long userId) {
        User user = getById(userId);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    private boolean verifyPassword(String raw, String encodedOrPlain) {
        if (encodedOrPlain == null) {
            return false;
        }
        if (isBCryptHash(encodedOrPlain)) {
            return passwordEncoder.matches(raw, encodedOrPlain);
        }
        return raw.equals(encodedOrPlain);
    }

    private boolean isBCryptHash(String value) {
        return value.startsWith("$2a$")
                || value.startsWith("$2b$")
                || value.startsWith("$2y$");
    }
}
