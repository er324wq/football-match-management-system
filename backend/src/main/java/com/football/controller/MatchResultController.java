package com.football.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.football.entity.MatchResult;
import com.football.exception.ResourceNotFoundException;
import com.football.service.MatchResultService;
import com.football.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/match-result")
public class MatchResultController {

    @Autowired
    private MatchResultService matchResultService;

    @GetMapping("/list")
    public ResultVO<Page<MatchResult>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        Page<MatchResult> pageResult = matchResultService.listWithMatchInfo(page, size);
        return ResultVO.success(pageResult);
    }

    @GetMapping("/with-match")
    public ResultVO<java.util.List<MatchResult>> listWithMatchInfo() {
        java.util.List<MatchResult> list = matchResultService.listWithMatchInfo();
        return ResultVO.success(list);
    }

    @GetMapping("/{id}")
    public ResultVO<MatchResult> getById(@PathVariable Long id) {
        MatchResult matchResult = matchResultService.getById(id);
        if (matchResult != null) {
            return ResultVO.success(matchResult);
        }
        throw new ResourceNotFoundException("比赛结果不存在");
    }

    @PostMapping
    public ResultVO<Void> save(@RequestBody MatchResult matchResult) {
        boolean success = matchResultService.save(matchResult);
        return success ? ResultVO.success() : ResultVO.error("添加失败");
    }

    @PutMapping
    public ResultVO<Void> update(@RequestBody MatchResult matchResult) {
        boolean success = matchResultService.updateById(matchResult);
        return success ? ResultVO.success() : ResultVO.error("更新失败");
    }

    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@PathVariable Long id) {
        boolean success = matchResultService.removeById(id);
        return success ? ResultVO.success() : ResultVO.error("删除失败");
    }
}
