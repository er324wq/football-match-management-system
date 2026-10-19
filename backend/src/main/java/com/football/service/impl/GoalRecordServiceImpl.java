package com.football.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.football.entity.GoalRecord;
import com.football.mapper.GoalRecordMapper;
import com.football.service.GoalRecordService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GoalRecordServiceImpl extends ServiceImpl<GoalRecordMapper, GoalRecord> implements GoalRecordService {

    @Override
    public boolean save(GoalRecord goalRecord) {
        // goalTime is supplied by the match event form (minutes elapsed in the match).
        // Do not derive it from wall-clock time: that loses the submitted event minute.
        return super.save(goalRecord);
    }

    @Override
    public List<GoalRecord> listWithDetails() {
        return baseMapper.selectAllWithDetails();
    }

    @Override
    public Page<GoalRecord> listWithDetails(Integer page, Integer size, Long matchId, Long playerId) {
        return baseMapper.selectPageWithDetails(new Page<>(page, size), matchId, playerId);
    }
}
