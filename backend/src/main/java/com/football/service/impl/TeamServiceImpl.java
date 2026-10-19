package com.football.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.football.entity.Team;
import com.football.mapper.TeamMapper;
import com.football.service.TeamService;
import org.springframework.stereotype.Service;

@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team> implements TeamService {
}
