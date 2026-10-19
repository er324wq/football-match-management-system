package com.football.controller;

import com.football.dto.LoginDTO;
import com.football.service.UserService;
import com.football.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<ResultVO<Map<String, Object>>> login(@Validated @RequestBody LoginDTO loginDTO) {
        try {
            Map<String, Object> data = userService.login(loginDTO);
            return ResponseEntity.ok(ResultVO.success(data));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ResultVO.error(401, "用户名或密码错误"));
        }
    }

    @PostMapping("/logout")
    public ResultVO<Void> logout() {
        return ResultVO.success();
    }
}
