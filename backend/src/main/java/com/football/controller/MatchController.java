package com.football.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.football.entity.Match;
import com.football.exception.ResourceNotFoundException;
import com.football.service.MatchService;
import com.football.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/match")
public class MatchController {

    @Autowired
    private MatchService matchService;

    @GetMapping("/list")
    public ResultVO<Page<Match>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status) {

        Page<Match> pageResult = matchService.listWithDetails(page, size, status);
        return ResultVO.success(pageResult);
    }

    @GetMapping("/all")
    public ResultVO<java.util.List<Match>> getAllMatches() {
        java.util.List<Match> list = matchService.listWithDetails();
        return ResultVO.success(list);
    }

    @GetMapping("/update-status")
    public ResultVO<java.lang.Integer> updateMatchStatuses() {
        int updated = matchService.updateMatchStatuses();
        return ResultVO.success(updated);
    }

    @GetMapping("/{id}")
    public ResultVO<Match> getById(@PathVariable Long id) {
        Match match = matchService.getById(id);
        if (match != null && match.getDeleted() == 0) {
            return ResultVO.success(match);
        }
        throw new ResourceNotFoundException("比赛不存在");
    }

    @PostMapping
    public ResultVO<Void> save(@RequestBody Match match) {
        boolean success = matchService.createMatch(match);
        return success ? ResultVO.success() : ResultVO.error("添加失败");
    }

    @PutMapping
    public ResultVO<Void> update(@RequestBody Match match) {
        boolean success = matchService.updateMatch(match);
        return success ? ResultVO.success() : ResultVO.error("更新失败");
    }

    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@PathVariable Long id) {
        boolean success = matchService.removeById(id);
        return success ? ResultVO.success() : ResultVO.error("删除失败");
    }
}
