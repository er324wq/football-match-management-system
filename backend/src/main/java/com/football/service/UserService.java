package com.football.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.football.dto.LoginDTO;
import com.football.entity.User;
import com.football.vo.ResultVO;

import java.util.Map;

public interface UserService extends IService<User> {

    Map<String, Object> login(LoginDTO loginDTO);

    User getUserInfo(Long userId);
}
