package com.football.controller;

import com.football.service.PerformanceService;
import com.football.vo.PerformanceVO;
import com.football.vo.ResultVO;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/performance")
public class PerformanceController {

    @Autowired
    private PerformanceService performanceService;

    @GetMapping("/metrics")
    public ResultVO<PerformanceVO> getMetrics() {
        PerformanceVO metrics = performanceService.getMetrics();
        return ResultVO.success(metrics);
    }

    @GetMapping("/logs")
    public ResultVO<List<Map<String, Object>>> getLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer limit) {
        return ResultVO.success(performanceService.getOperationLogs(keyword, limit));
    }

    @GetMapping("/backups")
    public ResultVO<List<Map<String, Object>>> getBackups() {
        return ResultVO.success(performanceService.getBackups());
    }

    @PostMapping("/backup")
    public ResultVO<Map<String, Object>> createBackup(@RequestBody(required = false) Map<String, Object> body) {
        String backupName = body == null ? null : toStr(body.get("backupName"));
        String remark = body == null ? null : toStr(body.get("remark"));
        String operator = body == null ? null : toStr(body.get("operator"));
        return ResultVO.success(performanceService.createBackup(backupName, remark, operator));
    }

    @PostMapping("/restore/{backupId}")
    public ResultVO<Void> restoreBackup(
            @PathVariable Long backupId,
            @RequestBody(required = false) Map<String, Object> body) {
        String operator = body == null ? null : toStr(body.get("operator"));
        performanceService.restoreBackup(backupId, operator);
        return ResultVO.success();
    }

    private String toStr(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }
}
