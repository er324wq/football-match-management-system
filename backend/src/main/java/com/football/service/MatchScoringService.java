package com.football.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.football.entity.MatchScoring;

import java.util.List;

public interface MatchScoringService extends IService<MatchScoring> {

    List<MatchScoring> getByRefereeId(Long refereeId);

    List<MatchScoring> getByMatchId(Long matchId);

    MatchScoring createScoring(Long refereeId, Long matchId, Double score, String comment);
}
