package com.football.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.football.entity.MatchResult;
import com.football.mapper.MatchResultMapper;
import com.football.service.MatchResultService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MatchResultServiceImpl extends ServiceImpl<MatchResultMapper, MatchResult> implements MatchResultService {

    @Override
    public List<MatchResult> listWithMatchInfo() {
        return baseMapper.selectAllWithMatchInfo();
    }

    @Override
    public Page<MatchResult> listWithMatchInfo(Integer page, Integer size) {
        return baseMapper.selectPageWithMatchInfo(new Page<>(page, size));
    }
}
