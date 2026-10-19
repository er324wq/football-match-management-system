package com.football.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.football.entity.Player;
import com.football.vo.TopScorerVO;

import java.util.List;

public interface PlayerService extends IService<Player> {

    List<Player> listWithTeamName();

    List<TopScorerVO> getTopScorers();
}
