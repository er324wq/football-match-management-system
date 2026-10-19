package com.football.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.football.entity.Match;
import com.football.vo.StandingVO;

import java.util.List;

public interface MatchService extends IService<Match> {

    List<Match> listWithDetails();

    Page<Match> listWithDetails(Integer page, Integer size, String status);

    int updateMatchStatuses();

    /** Persist a match and its optional result as one atomic operation. */
    boolean createMatch(Match match);

    /** Persist match changes and synchronize its result as one atomic operation. */
    boolean updateMatch(Match match);

    List<StandingVO> getStandings();
}
