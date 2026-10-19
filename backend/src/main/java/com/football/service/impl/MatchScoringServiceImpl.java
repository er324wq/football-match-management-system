package com.football.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.football.entity.MatchScoring;
import com.football.mapper.MatchScoringMapper;
import com.football.service.MatchScoringService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MatchScoringServiceImpl extends ServiceImpl<MatchScoringMapper, MatchScoring> implements MatchScoringService {

    @Override
    public List<MatchScoring> getByRefereeId(Long refereeId) {
        return baseMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MatchScoring>()
                        .eq("referee_id", refereeId)
                        .orderByDesc("create_time")
        );
    }

    @Override
    public List<MatchScoring> getByMatchId(Long matchId) {
        return baseMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MatchScoring>()
                        .eq("match_id", matchId)
                        .orderByDesc("create_time")
        );
    }

    @Override
    public MatchScoring createScoring(Long refereeId, Long matchId, Double score, String comment) {
        MatchScoring scoring = new MatchScoring();
        scoring.setRefereeId(refereeId);
        scoring.setMatchId(matchId);
        scoring.setScore(score);
        scoring.setComment(comment);
        scoring.setCreateTime(LocalDateTime.now());
        scoring.setUpdateTime(LocalDateTime.now());
        save(scoring);
        return scoring;
    }
}
