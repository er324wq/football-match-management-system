package com.football.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.football.entity.Match;
import com.football.mapper.MatchMapper;
import com.football.mapper.MatchResultMapper;
import com.football.service.MatchService;
import com.football.service.MatchResultService;
import com.football.vo.StandingVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.football.entity.MatchResult;

import java.util.List;

@Service
public class MatchServiceImpl extends ServiceImpl<MatchMapper, Match> implements MatchService {

    @Autowired
    private MatchResultService matchResultService;

    @Autowired
    private MatchResultMapper matchResultMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createMatch(Match match) {
        boolean saved = save(match);
        if (saved && shouldHaveResult(match)) {
            syncMatchResult(match);
        }
        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateMatch(Match match) {
        boolean updated = updateById(match);
        if (!updated || match.getStatus() == null) {
            return updated;
        }
        if ("upcoming".equals(match.getStatus())) {
            MatchResult existing = matchResultService.getOne(new LambdaQueryWrapper<MatchResult>()
                    .eq(MatchResult::getMatchId, match.getId()));
            if (existing != null && !matchResultService.removeById(existing.getId())) {
                throw new IllegalStateException("关联赛果删除失败");
            }
        } else {
            syncMatchResult(match);
        }
        return true;
    }

    private boolean shouldHaveResult(Match match) {
        return match.getStatus() != null && !"upcoming".equals(match.getStatus());
    }

    private void syncMatchResult(Match match) {
        MatchResult result = matchResultService.getOne(new LambdaQueryWrapper<MatchResult>()
                .eq(MatchResult::getMatchId, match.getId()));
        boolean restoreDeleted = false;
        if (result == null) {
            result = matchResultMapper.selectIncludingDeletedByMatchId(match.getId());
            restoreDeleted = result != null && Integer.valueOf(1).equals(result.getDeleted());
            if (result == null) {
                result = new MatchResult();
                result.setMatchId(match.getId());
            }
        }
        if (match.getHomeScore() != null) {
            result.setHomeScore(match.getHomeScore());
        }
        if (match.getAwayScore() != null) {
            result.setAwayScore(match.getAwayScore());
        }
        if (match.getReferee() != null) {
            result.setReferee(match.getReferee());
        }
        boolean persisted;
        if (result.getId() == null) {
            persisted = matchResultService.save(result);
        } else if (restoreDeleted) {
            persisted = matchResultMapper.restoreDeletedResult(result) == 1;
        } else {
            persisted = matchResultService.updateById(result);
        }
        if (!persisted) {
            throw new IllegalStateException("比赛已保存，但关联赛果未能同步");
        }
    }

    @Override
    public List<Match> listWithDetails() {
        return baseMapper.selectAllWithDetails();
    }

    @Override
    public Page<Match> listWithDetails(Integer page, Integer size, String status) {
        return baseMapper.selectPageWithDetails(new Page<>(page, size), status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateMatchStatuses() {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.util.List<Match> matches = list(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Match>()
                    .eq(Match::getDeleted, 0));

            int updated = 0;
            for (Match match : matches) {
                if (match.getMatchTime() != null && match.getStatus() != null) {
                    java.time.LocalDateTime matchTime = match.getMatchTime();

                    if (match.getStatus().equals("upcoming") && now.isAfter(matchTime)) {
                        match.setStatus("in_progress");
                        if (!updateById(match)) {
                            throw new IllegalStateException("比赛状态更新失败: " + match.getId());
                        }
                        updated++;
                    } else if (match.getStatus().equals("in_progress")) {
                        java.time.Duration duration = java.time.Duration.between(matchTime, now);
                        long hoursDiff = duration.toHours();
                        if (hoursDiff > 24) {
                            match.setStatus("finished");
                            if (!updateById(match)) {
                                throw new IllegalStateException("比赛状态更新失败: " + match.getId());
                            }
                            updated++;
                        }
                    }
                }
            }
            return updated;
    }

    @Override
    public List<StandingVO> getStandings() {
        return baseMapper.selectStandings();
    }
}
