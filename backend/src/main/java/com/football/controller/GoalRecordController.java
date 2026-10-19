package com.football.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.football.entity.GoalRecord;
import com.football.exception.ResourceNotFoundException;
import com.football.service.GoalRecordService;
import com.football.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/goal-record")
public class GoalRecordController {

    @Autowired
    private GoalRecordService goalRecordService;

    @GetMapping("/list")
    public ResultVO<Page<GoalRecord>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long matchId,
            @RequestParam(required = false) Long playerId) {

        Page<GoalRecord> pageResult = goalRecordService.listWithDetails(page, size, matchId, playerId);
        return ResultVO.success(pageResult);
    }

    @GetMapping("/with-details")
    public ResultVO<java.util.List<GoalRecord>> listWithDetails() {
        java.util.List<GoalRecord> list = goalRecordService.listWithDetails();
        return ResultVO.success(list);
    }

    @GetMapping("/{id}")
    public ResultVO<GoalRecord> getById(@PathVariable Long id) {
        GoalRecord goalRecord = goalRecordService.getById(id);
        if (goalRecord != null) {
            return ResultVO.success(goalRecord);
        }
        throw new ResourceNotFoundException("进球记录不存在");
    }

    @PostMapping
    public ResultVO<Void> save(@Valid @RequestBody GoalRecord goalRecord) {
        boolean success = goalRecordService.save(goalRecord);
        return success ? ResultVO.success() : ResultVO.error("添加失败");
    }

    @PutMapping
    public ResultVO<Void> update(@Valid @RequestBody GoalRecord goalRecord) {
        boolean success = goalRecordService.updateById(goalRecord);
        return success ? ResultVO.success() : ResultVO.error("更新失败");
    }

    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@PathVariable Long id) {
        boolean success = goalRecordService.removeById(id);
        return success ? ResultVO.success() : ResultVO.error("删除失败");
    }
}
