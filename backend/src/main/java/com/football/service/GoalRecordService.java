package com.football.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.football.entity.GoalRecord;

import java.util.List;

public interface GoalRecordService extends IService<GoalRecord> {

    List<GoalRecord> listWithDetails();

    Page<GoalRecord> listWithDetails(Integer page, Integer size, Long matchId, Long playerId);
}
