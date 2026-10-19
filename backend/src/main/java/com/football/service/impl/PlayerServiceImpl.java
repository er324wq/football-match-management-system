package com.football.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.football.entity.Player;
import com.football.mapper.PlayerMapper;
import com.football.service.PlayerService;
import com.football.vo.TopScorerVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlayerServiceImpl extends ServiceImpl<PlayerMapper, Player> implements PlayerService {

    @Override
    public List<Player> listWithTeamName() {
        return baseMapper.selectAllWithTeamName();
    }

    @Override
    public List<TopScorerVO> getTopScorers() {
        return baseMapper.selectTopScorers();
    }
}
