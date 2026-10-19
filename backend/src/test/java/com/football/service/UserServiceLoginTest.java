package com.football.service;

import com.football.dto.LoginDTO;
import com.football.entity.User;
import com.football.mapper.UserMapper;
import com.football.security.JwtService;
import com.football.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class UserServiceLoginTest {

    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        userService = new UserServiceImpl();
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
        ReflectionTestUtils.setField(userService, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(userService, "jwtService", jwtService);
    }

    @Test
    void successfulLoginReturnsTokenAndPasswordFreeUser() {
        User user = new User();
        user.setId(8L);
        user.setUsername("sample-user");
        user.setPassword("$2a$10$existing-bcrypt-hash");
        user.setRole("user");
        user.setStatus(1);
        when(userMapper.selectByUsername("sample-user")).thenReturn(user);
        when(passwordEncoder.matches("plain-password", user.getPassword())).thenReturn(true);
        when(jwtService.createToken(8L, "sample-user", "user")).thenReturn("signed-test-token");

        LoginDTO login = new LoginDTO();
        login.setUsername("sample-user");
        login.setPassword("plain-password");
        Map<String, Object> result = userService.login(login);

        assertEquals("signed-test-token", result.get("token"));
        User returnedUser = (User) result.get("user");
        assertEquals("sample-user", returnedUser.getUsername());
        assertNull(returnedUser.getPassword());
    }

    @Test
    void disabledAccountCannotLogin() {
        User user = new User();
        user.setUsername("disabled-user");
        user.setPassword("$2a$10$existing-bcrypt-hash");
        user.setStatus(0);
        when(userMapper.selectByUsername("disabled-user")).thenReturn(user);

        LoginDTO login = new LoginDTO();
        login.setUsername("disabled-user");
        login.setPassword("plain-password");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.login(login));
        assertEquals("用户名或密码错误", exception.getMessage());
    }

    @Test
    void missingAdminRequiresConfiguredBootstrapPassword() {
        when(userMapper.selectByUsername("admin")).thenReturn(null);
        ReflectionTestUtils.setField(userService, "initialAdminPassword", " ");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> userService.init());
        assertTrue(exception.getMessage().contains("INITIAL_ADMIN_PASSWORD"));
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void missingAdminIsCreatedWithConfiguredBootstrapPassword() {
        String bootstrapPassword = UUID.randomUUID().toString();
        when(userMapper.selectByUsername("admin")).thenReturn(null);
        when(passwordEncoder.encode(bootstrapPassword)).thenReturn("encoded-test-password");
        when(userMapper.insert(any(User.class))).thenReturn(1);
        ReflectionTestUtils.setField(userService, "initialAdminPassword", bootstrapPassword);

        userService.init();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        assertEquals("admin", captor.getValue().getUsername());
        assertEquals("encoded-test-password", captor.getValue().getPassword());
    }

    @Test
    void existingAdminIsNotChangedByBootstrapConfiguration() {
        User existingAdmin = new User();
        existingAdmin.setUsername("admin");
        existingAdmin.setPassword("existing-password-hash");
        when(userMapper.selectByUsername("admin")).thenReturn(existingAdmin);

        userService.init();

        verify(userMapper, never()).insert(any(User.class));
        verify(passwordEncoder, never()).encode(any(String.class));
    }
}
