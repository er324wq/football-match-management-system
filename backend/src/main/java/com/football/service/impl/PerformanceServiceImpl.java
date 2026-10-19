package com.football.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.football.service.PerformanceService;
import com.football.vo.PerformanceVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PerformanceServiceImpl implements PerformanceService {

    private static final Set<String> SYSTEM_TABLES = new HashSet<>(Arrays.asList(
            "db_backup", "operation_log"
    ));

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void initTables() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS operation_log (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "module_name VARCHAR(64) NOT NULL," +
                "action_name VARCHAR(128) NOT NULL," +
                "operator_name VARCHAR(64) DEFAULT NULL," +
                "detail TEXT DEFAULT NULL," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "INDEX idx_operation_log_module (module_name)," +
                "INDEX idx_operation_log_time (create_time)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS db_backup (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "backup_name VARCHAR(128) NOT NULL," +
                "remark VARCHAR(255) DEFAULT NULL," +
                "operator_name VARCHAR(64) DEFAULT NULL," +
                "table_count INT DEFAULT 0," +
                "row_count INT DEFAULT 0," +
                "backup_content LONGTEXT NOT NULL," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "INDEX idx_db_backup_time (create_time)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    @Override
    public PerformanceVO getMetrics() {
        PerformanceVO vo = new PerformanceVO();

        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

        double cpuUsage = calculateCpuUsage(osBean);

        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
        double memoryUsage = (double) usedMemory / maxMemory * 100;

        Integer httpRequests = safeCount("SELECT COUNT(*) FROM operation_log");
        String dbStatus = "UP";
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        } catch (Exception e) {
            dbStatus = "DOWN";
        }

        vo.setHealth("UP");
        vo.setCpuUsage(String.format("%.2f", cpuUsage));
        vo.setMemoryUsed(String.format("%.2f", memoryUsage));
        vo.setHttpRequests(String.valueOf(httpRequests == null ? 0 : httpRequests));
        vo.setDbStatus(dbStatus);

        return vo;
    }

    @Override
    public List<Map<String, Object>> getOperationLogs(String keyword, Integer limit) {
        int finalLimit = (limit == null || limit <= 0) ? 200 : Math.min(limit, 1000);
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) {
            return jdbcTemplate.queryForList(
                    "SELECT id, module_name AS moduleName, action_name AS actionName, operator_name AS operatorName, detail, create_time AS createTime " +
                            "FROM operation_log ORDER BY id DESC LIMIT ?",
                    finalLimit
            );
        }

        String like = "%" + kw + "%";
        return jdbcTemplate.queryForList(
                "SELECT id, module_name AS moduleName, action_name AS actionName, operator_name AS operatorName, detail, create_time AS createTime " +
                        "FROM operation_log " +
                        "WHERE module_name LIKE ? OR action_name LIKE ? OR operator_name LIKE ? OR detail LIKE ? " +
                        "ORDER BY id DESC LIMIT ?",
                like, like, like, like, finalLimit
        );
    }

    @Override
    public List<Map<String, Object>> getBackups() {
        return jdbcTemplate.queryForList(
                "SELECT id, backup_name AS backupName, remark, operator_name AS operatorName, table_count AS tableCount, row_count AS rowCount, create_time AS createTime " +
                        "FROM db_backup ORDER BY id DESC"
        );
    }

    @Override
    public Map<String, Object> createBackup(String backupName, String remark, String operator) {
        String dbName = currentDatabase();
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_type='BASE TABLE' ORDER BY table_name",
                String.class, dbName
        );

        Map<String, Object> data = new LinkedHashMap<>();
        int totalRows = 0;
        int tableCount = 0;

        for (String table : tables) {
            if (SYSTEM_TABLES.contains(table.toLowerCase())) {
                continue;
            }
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM `" + table + "`");
            data.put(table, rows);
            totalRows += rows.size();
            tableCount++;
        }

        try {
            String content = objectMapper.writeValueAsString(data);
            String name = (backupName == null || backupName.trim().isEmpty())
                    ? "backup_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                    : backupName.trim();
            String op = (operator == null || operator.trim().isEmpty()) ? "system" : operator.trim();
            String finalRemark = remark == null ? "" : remark.trim();

            jdbcTemplate.update(
                    "INSERT INTO db_backup (backup_name, remark, operator_name, table_count, row_count, backup_content, create_time) VALUES (?, ?, ?, ?, ?, ?, NOW())",
                    name, finalRemark, op, tableCount, totalRows, content
            );
            logOperation("performance", "create_backup", op, "name=" + name + ", tables=" + tableCount + ", rows=" + totalRows);

            Map<String, Object> result = new HashMap<>();
            result.put("backupName", name);
            result.put("tableCount", tableCount);
            result.put("rowCount", totalRows);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("创建备份失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreBackup(Long backupId, String operator) {
        if (backupId == null) {
            throw new IllegalArgumentException("备份ID不能为空");
        }
        String op = (operator == null || operator.trim().isEmpty()) ? "system" : operator.trim();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT backup_name AS backupName, backup_content AS backupContent FROM db_backup WHERE id = ? LIMIT 1",
                backupId
        );
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("备份不存在");
        }

        Map<String, Object> row = rows.get(0);
        String backupName = String.valueOf(row.get("backupName"));
        String content = String.valueOf(row.get("backupContent"));

        try {
            Map<String, List<Map<String, Object>>> backupData = objectMapper.readValue(
                    content, new TypeReference<Map<String, List<Map<String, Object>>>>() {
                    });

            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");

            List<String> tableOrder = buildRestoreTableOrder(backupData.keySet());
            for (String table : tableOrder) {
                if (SYSTEM_TABLES.contains(table.toLowerCase())) {
                    continue;
                }
                jdbcTemplate.execute("TRUNCATE TABLE `" + table + "`");
            }

            for (String table : tableOrder) {
                if (SYSTEM_TABLES.contains(table.toLowerCase())) {
                    continue;
                }
                List<Map<String, Object>> tableRows = backupData.get(table);
                if (tableRows == null || tableRows.isEmpty()) {
                    continue;
                }
                for (Map<String, Object> dataRow : tableRows) {
                    insertRow(table, dataRow);
                }
            }

            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
            logOperation("performance", "restore_backup", op, "backupId=" + backupId + ", backupName=" + backupName);
        } catch (Exception e) {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
            throw new RuntimeException("恢复备份失败: " + e.getMessage(), e);
        }
    }

    private void insertRow(String table, Map<String, Object> row) {
        List<String> columns = new ArrayList<>(row.keySet());
        StringBuilder sql = new StringBuilder("INSERT INTO `").append(table).append("` (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("`").append(columns.get(i)).append("`");
        }
        sql.append(") VALUES (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("?");
        }
        sql.append(")");

        Object[] args = new Object[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            Object value = row.get(columns.get(i));
            if (value instanceof String && isDatetimeString((String) value)) {
                args[i] = Timestamp.valueOf(((String) value).replace("T", " ").substring(0, 19));
            } else {
                args[i] = value;
            }
        }
        jdbcTemplate.update(sql.toString(), args);
    }

    private boolean isDatetimeString(String value) {
        return value != null && value.length() >= 19
                && Character.isDigit(value.charAt(0))
                && value.contains("-") && (value.contains(" ") || value.contains("T"));
    }

    private List<String> buildRestoreTableOrder(Set<String> tables) {
        List<String> preferred = Arrays.asList(
                "user", "team", "team_member", "player", "match", "match_result",
                "goal_record", "card_record", "application", "notification"
        );
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String t : preferred) {
            if (tables.contains(t)) {
                result.add(t);
            }
        }
        List<String> others = new ArrayList<>(tables);
        Collections.sort(others);
        result.addAll(others);
        return new ArrayList<>(result);
    }

    private Integer safeCount(String sql) {
        try {
            return jdbcTemplate.queryForObject(sql, Integer.class);
        } catch (Exception e) {
            return 0;
        }
    }

    private String currentDatabase() {
        return jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
    }

    private void logOperation(String module, String action, String operator, String detail) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO operation_log (module_name, action_name, operator_name, detail, create_time) VALUES (?, ?, ?, ?, NOW())",
                    module, action, operator, detail
            );
        } catch (Exception ignored) {
        }
    }

    private double calculateCpuUsage(OperatingSystemMXBean osBean) {
        try {
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean =
                        (com.sun.management.OperatingSystemMXBean) osBean;
                double cpuLoad = sunOsBean.getProcessCpuLoad() * 100;
                if (cpuLoad < 0) {
                    return 0.0;
                }
                if (cpuLoad > 100) {
                    return 100.0;
                }
                return cpuLoad;
            } else {
                double systemLoadAverage = osBean.getSystemLoadAverage();
                int availableProcessors = osBean.getAvailableProcessors();

                if (systemLoadAverage < 0) {
                    return 0.0;
                }

                double cpuUsage = (systemLoadAverage / availableProcessors) * 100;

                if (cpuUsage < 0) {
                    cpuUsage = 0;
                }
                if (cpuUsage > 100) {
                    cpuUsage = 100;
                }

                return cpuUsage;
            }
        } catch (Exception e) {
            return 0.0;
        }
    }
}
