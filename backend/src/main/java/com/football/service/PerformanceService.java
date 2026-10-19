package com.football.service;

import com.football.vo.PerformanceVO;

import java.util.List;
import java.util.Map;

public interface PerformanceService {
    PerformanceVO getMetrics();

    List<Map<String, Object>> getOperationLogs(String keyword, Integer limit);

    List<Map<String, Object>> getBackups();

    Map<String, Object> createBackup(String backupName, String remark, String operator);

    void restoreBackup(Long backupId, String operator);
}
