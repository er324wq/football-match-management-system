package com.football.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.football.entity.MatchResult;

import java.util.List;

public interface MatchResultService extends IService<MatchResult> {

    List<MatchResult> listWithMatchInfo();

    Page<MatchResult> listWithMatchInfo(Integer page, Integer size);
}
