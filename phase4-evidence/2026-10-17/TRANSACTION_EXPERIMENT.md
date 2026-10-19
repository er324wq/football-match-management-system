# Phase 4 事务验证记录

日期：2026-10-17。仓库没有 Git 元数据；改动前先保存 before 快照，修改后保存 after 快照，两个 SHA-256 清单分别为 `snapshot_before_sha256.csv` 和 `snapshot_after_sha256.csv`。

## 执行环境与隔离
- JDK 8.0.504；项目 Maven wrapper 未配置，本机执行 IntelliJ 内置 Maven 3；MySQL 服务器 `SELECT VERSION()` 返回 26.7.0。
- 从仓库 `backend/src/main/resources/db/init.sql` 生成唯一临时 schema `football_admin_phase4_tx_20261017`，确认其原先不存在后才创建。包含 13 张表，全部 InnoDB；初始 match=4、match_result=2、team=4、application=0。
- 测试设置独立 BEFORE INSERT/UPDATE trigger 以 `SIGNAL SQLSTATE '45000'` 制造数据库异常；触发器在测试 finally 中删除。测试套件结束后仅删除该精确临时 schema。
- 集成测试代码要求 `-Dphase4.db.integration=true`，并在创建触发器前断言 `SELECT DATABASE()` 精确匹配临时 schema，防止连接到 `football_admin` 执行故障注入。

## 真实回滚场景
| 流程 | 故障点 | HTTP | 事务后断言 |
|---|---|---:|---|
| 完成态比赛创建 + 赛果同步 | INSERT match_result | 500 | match marker=0；无关联赛果 |
| 球队创建 + 批准申请写入 | INSERT application | 500 | team marker=0；application marker=0 |
| 过期比赛批量状态更新 | UPDATE match | 500 | 目标 match 状态仍是 upcoming |
| 审批创建球队 | INSERT team | 500 | application 状态仍是 pending；team marker=0 |
| finished→upcoming→finished | 两次成功状态更新 | 200 / 200 | `match_result` 仍仅 1 行，重新激活为 deleted=0，比分更新为 3–1 |

异常为 MySQL `java.sql.SQLException`，触发 SQLSTATE 45000，经过 MyBatis/Spring DAO 转换；当前未配置统一全局持久层异常响应，因此 HTTP 为 Spring 默认 500。实际响应与 DB 最终状态均由集成测试断言。完整测试输出 `MAVEN_PHASE4_FINAL_RESULTS.txt`，Surefire 单类汇总位于 `backend/target/surefire-reports/com.football.integration.TransactionalWorkflowIntegrationTest.txt`。

## 全量测试
执行：
`mvn -f backend/pom.xml test -Dphase3.db.integration=true -Dphase4.db.integration=true`

结果：29 tests / 0 failures / 0 errors / 0 skipped / BUILD SUCCESS。原 24 项包括 MatchController 5、Security 11、GoalRecordService 1、UserService 2、Phase 3 DB 5；新增本阶段真实 HTTP+MySQL 5 项（4 个回滚故障场景、1 个状态往返）。

## 清理与不变性
测试 schema 已删除，`information_schema.schemata` 对其查询为 0。真实业务 schema 只做只读 `COUNT(*)`，结果 user=3、team=4、player=8、match=4、match_result=2、goal_record=5；未对其执行写入、DDL 或删除。阶段 3 checksums 逐项复核：41 条中 37 条哈希不变；4 条差异均为本阶段预期更新的当前工作文件（TECHNICAL_AUDIT.md、TECHNICAL_RESULTS.md、RESUME_PROJECT_RESULTS.md、MatchControllerTest.java）。阶段 3 的 evidence 目录快照和原始结果文件保留，逐项对照见 `phase3_evidence_preservation_check.csv`。

## 限制
测试证明列明的四条失败回滚路径和一个赛果状态往返，不代表所有现存业务事务均覆盖。赛事排赛、阵容写入、球队解散、入队审批成员同步和通知 best-effort 行为仍待单独验证；无性能测试数据。
