package com.football.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.football.entity.CardRecord;

import java.util.List;

public interface CardRecordService extends IService<CardRecord> {

    List<CardRecord> listWithDetails();

    Page<CardRecord> listWithDetails(Integer page, Integer size, Long matchId, Long playerId, String cardType);
}
