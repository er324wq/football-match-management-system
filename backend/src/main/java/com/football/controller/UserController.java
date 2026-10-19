package com.football.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.football.entity.User;
import com.football.exception.ResourceNotFoundException;
import com.football.service.UserService;
import com.football.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Objects;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/list")
    public ResultVO<Page<User>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role) {

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, 0);
        if (username != null && !username.trim().isEmpty()) {
            wrapper.like(User::getUsername, username.trim());
        }
        if (role != null && !role.trim().isEmpty()) {
            wrapper.eq(User::getRole, role.trim());
        }
        wrapper.orderByDesc(User::getCreateTime);

        Page<User> pageResult = userService.page(new Page<>(page, size), wrapper);
        pageResult.getRecords().forEach(item -> item.setPassword(null));
        return ResultVO.success(pageResult);
    }

    @GetMapping("/{id}")
    public ResultVO<User> getById(@PathVariable Long id,
                                  @RequestAttribute("userId") Long authenticatedUserId,
                                  @RequestAttribute("role") String role) {
        if (!"admin".equals(role) && !Objects.equals(authenticatedUserId, id)) {
            throw new AccessDeniedException("无权查看其他用户信息");
        }
        User user = userService.getById(id);
        if (user != null && user.getDeleted() == 0) {
            user.setPassword(null);
            return ResultVO.success(user);
        }
        throw new ResourceNotFoundException("用户不存在");
    }

    @PostMapping
    public ResultVO<User> save(@RequestBody User user) {
        normalizeFields(user);
        if (isBlank(user.getUsername())) {
            return ResultVO.error("用户名不能为空");
        }
        if (isBlank(user.getPhone())) {
            return ResultVO.error("手机号不能为空");
        }
        if (isBlank(user.getEmail())) {
            return ResultVO.error("邮箱不能为空");
        }
        if (!isValidPhone(user.getPhone())) {
            return ResultVO.error("手机号格式不正确");
        }
        if (!isValidEmail(user.getEmail())) {
            return ResultVO.error("邮箱格式不正确");
        }

        LambdaQueryWrapper<User> usernameWrapper = new LambdaQueryWrapper<>();
        usernameWrapper.eq(User::getUsername, user.getUsername()).eq(User::getDeleted, 0);
        if (userService.count(usernameWrapper) > 0) {
            return ResultVO.error("用户名已存在");
        }
        LambdaQueryWrapper<User> phoneWrapper = new LambdaQueryWrapper<>();
        phoneWrapper.eq(User::getPhone, user.getPhone()).eq(User::getDeleted, 0);
        if (userService.count(phoneWrapper) > 0) {
            return ResultVO.error("手机号已存在");
        }
        LambdaQueryWrapper<User> emailWrapper = new LambdaQueryWrapper<>();
        emailWrapper.eq(User::getEmail, user.getEmail()).eq(User::getDeleted, 0);
        if (userService.count(emailWrapper) > 0) {
            return ResultVO.error("邮箱已存在");
        }

        String rawPassword = user.getPassword() == null ? "" : user.getPassword().trim();
        if (rawPassword.isEmpty()) {
            return ResultVO.error("密码不能为空");
        }
        user.setPassword(passwordEncoder.encode(rawPassword));
        boolean success = userService.save(user);
        if (success) {
            user.setPassword(null);
            return ResultVO.success(user);
        }
        return ResultVO.error("添加失败");
    }

    @PostMapping("/register")
    public ResultVO<Void> register(@RequestBody User user) {
        normalizeFields(user);
        if (isBlank(user.getUsername())) {
            return ResultVO.error("用户名不能为空");
        }
        if (isBlank(user.getPhone())) {
            return ResultVO.error("手机号不能为空");
        }
        if (isBlank(user.getEmail())) {
            return ResultVO.error("邮箱不能为空");
        }
        if (!isValidPhone(user.getPhone())) {
            return ResultVO.error("手机号格式不正确");
        }
        if (!isValidEmail(user.getEmail())) {
            return ResultVO.error("邮箱格式不正确");
        }

        String rawPassword = user.getPassword() == null ? "" : user.getPassword().trim();
        if (rawPassword.isEmpty()) {
            return ResultVO.error("密码不能为空");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername());
        wrapper.eq(User::getDeleted, 0);
        User existUser = userService.getOne(wrapper);
        if (existUser != null) {
            return ResultVO.error("用户名已存在");
        }

        LambdaQueryWrapper<User> phoneWrapper = new LambdaQueryWrapper<>();
        phoneWrapper.eq(User::getPhone, user.getPhone()).eq(User::getDeleted, 0);
        if (userService.count(phoneWrapper) > 0) {
            return ResultVO.error("手机号已存在");
        }

        LambdaQueryWrapper<User> emailWrapper = new LambdaQueryWrapper<>();
        emailWrapper.eq(User::getEmail, user.getEmail()).eq(User::getDeleted, 0);
        if (userService.count(emailWrapper) > 0) {
            return ResultVO.error("邮箱已存在");
        }

        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole("user");
        user.setStatus(1);
        boolean success = userService.save(user);
        return success ? ResultVO.success() : ResultVO.error("注册失败");
    }

    @PutMapping
    public ResultVO<Void> update(@RequestBody User user,
                                 @RequestAttribute("userId") Long authenticatedUserId,
                                 @RequestAttribute("role") String role) {
        if (!"admin".equals(role)) {
            if (!Objects.equals(authenticatedUserId, user.getId())) {
                throw new AccessDeniedException("无权修改其他用户信息");
            }
            if (user.getRole() != null || user.getStatus() != null || user.getDeleted() != null
                    || user.getPassword() != null) {
                throw new AccessDeniedException("普通用户只能修改个人资料");
            }
            User profile = new User();
            profile.setId(user.getId());
            profile.setNickname(user.getNickname());
            profile.setPhone(user.getPhone());
            profile.setEmail(user.getEmail());
            profile.setAvatar(user.getAvatar());
            profile.setSignature(user.getSignature());
            user = profile;
        }
        normalizeFields(user);
        if (user.getId() == null) {
            return ResultVO.error("用户ID不能为空");
        }

        User existing = userService.getById(user.getId());
        if (existing == null || (existing.getDeleted() != null && existing.getDeleted() == 1)) {
            return ResultVO.error("用户不存在");
        }

        if (!isBlank(user.getPhone()) && !isValidPhone(user.getPhone())) {
            return ResultVO.error("手机号格式不正确");
        }
        if (!isBlank(user.getEmail()) && !isValidEmail(user.getEmail())) {
            return ResultVO.error("邮箱格式不正确");
        }

        if (!isBlank(user.getUsername())) {
            LambdaQueryWrapper<User> usernameWrapper = new LambdaQueryWrapper<>();
            usernameWrapper.eq(User::getUsername, user.getUsername())
                    .eq(User::getDeleted, 0)
                    .ne(User::getId, user.getId());
            if (userService.count(usernameWrapper) > 0) {
                return ResultVO.error("用户名已存在");
            }
        }

        if (!isBlank(user.getPhone())) {
            LambdaQueryWrapper<User> phoneWrapper = new LambdaQueryWrapper<>();
            phoneWrapper.eq(User::getPhone, user.getPhone())
                    .eq(User::getDeleted, 0)
                    .ne(User::getId, user.getId());
            if (userService.count(phoneWrapper) > 0) {
                return ResultVO.error("手机号已存在");
            }
        }

        if (!isBlank(user.getEmail())) {
            LambdaQueryWrapper<User> emailWrapper = new LambdaQueryWrapper<>();
            emailWrapper.eq(User::getEmail, user.getEmail())
                    .eq(User::getDeleted, 0)
                    .ne(User::getId, user.getId());
            if (userService.count(emailWrapper) > 0) {
                return ResultVO.error("邮箱已存在");
            }
        }

        if (user.getPassword() != null) {
            String pwd = user.getPassword().trim();
            if (pwd.isEmpty()) {
                user.setPassword(null);
            } else if (!isEncodedPassword(pwd)) {
                user.setPassword(passwordEncoder.encode(pwd));
            }
        }

        boolean success = userService.updateById(user);
        return success ? ResultVO.success() : ResultVO.error("更新失败");
    }

    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@PathVariable Long id) {
        boolean success = userService.removeById(id);
        return success ? ResultVO.success() : ResultVO.error("删除失败");
    }

    private boolean isEncodedPassword(String pwd) {
        return pwd.startsWith("$2a$") || pwd.startsWith("$2b$") || pwd.startsWith("$2y$");
    }

    private void normalizeFields(User user) {
        if (user == null) {
            return;
        }
        user.setUsername(trimToNull(user.getUsername()));
        user.setNickname(trimToNull(user.getNickname()));
        user.setPhone(trimToNull(user.getPhone()));
        user.setEmail(trimToNull(user.getEmail()));
        user.setAvatar(trimToNull(user.getAvatar()));
        user.setSignature(trimToNull(user.getSignature()));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^1\\d{10}$");
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }
}
