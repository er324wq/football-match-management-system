# 阶段 5：事务与接口异常验证证据

日期：2026-10-17。项目无 Git 元数据；本目录保留本阶段初始 before 快照、最终 after 快照、Maven 原始日志和隔离 MySQL schema 记录。阶段 2、3、4 目录未改写。

## 运行环境

- Windows PowerShell；JDK 8.0.504；Spring Boot 2.7.18；MyBatis-Plus 3.5.3.1。
- MySQL Server `26.7.0`，本地 root 账号具备 `CREATE`、`DROP`、`TRIGGER`、DML 权限。密码仅经当前进程环境变量传递，没有写入命令证据、配置或报告。
- 临时 schema：`football_admin_phase5_tx_20261017`；基于仓库 `backend/src/main/resources/db/init.sql` 创建 13 张 InnoDB 表。应用测试数据源通过 `SPRING_DATASOURCE_URL` 指向该库；Phase 5 和旧 Phase 4 测试都在测试代码内检查 `SELECT DATABASE()`。
- 建库与清理脚本：`setup_phase5_schema.ps1`、`cleanup_phase5_schema.ps1`。脚本拒绝 schema 已存在/不存在等非预期状态，仅允许操作名称完全匹配的临时 schema。

## 实际修改文件及原因

| 文件 | 改动 |
|---|---|
| `backend/src/main/java/com/football/service/impl/ApplicationServiceImpl.java` | 排赛、阵容替换、审批的后续写入检查；异常传播以触发已存在事务；缺失关联记录返回受控异常 |
| `backend/src/main/java/com/football/service/impl/TeamMemberServiceImpl.java` | 成员与用户角色同步纳入事务；重复成员幂等；复用停用唯一关系；检查角色更新结果 |
| `backend/src/main/java/com/football/controller/TeamController.java` | 球队解散中成员/球员/球队更新失败转异常，避免正常返回后部分提交 |
| `backend/src/main/java/com/football/controller/TeamMemberController.java` | 缺失成员修改/删除映射为 404 |
| `backend/src/main/java/com/football/exception/ApiExceptionHandler.java` | 参数/认证/授权/缺失/约束冲突/未知异常映射适当 HTTP 状态，保留 ResultVO 包络并隐藏未知异常细节 |
| `backend/src/main/java/com/football/exception/ResourceNotFoundException.java` | 表达业务资源不存在 |
| `backend/src/main/java/com/football/controller/{ApplicationController,AuthController,GoalRecordController,MatchController,MatchResultController,NotificationController,PlayerController,UserController}.java` | 参数错误抛受控 400；常用详情资源不存在返回 404；登录错误不回显底层异常；GoalRecord 未知异常不再被吞掉 |
| `frontend/src/api/request.js` | 读取非 2xx `ResultVO.msg`，保留原 401 清会话行为 |
| `backend/src/test/java/com/football/integration/Phase5WorkflowIntegrationTest.java` | 新增 9 项隔离 schema 的 HTTP/API/回滚集成测试 |
| `backend/src/test/java/com/football/integration/TransactionalWorkflowIntegrationTest.java` | 显式启用 Phase 5 时允许旧事务回归套件验证当前指定测试 schema；仍有 schema 白名单断言 |
| `backend/src/test/java/com/football/security/SecurityIntegrationTest.java` | 断言过滤链真实返回 401/403 且 envelope code 保持一致 |
| `backend/src/test/java/com/football/controller/MatchControllerTest.java` | 配合异常化缺失比赛行为更新断言 |
| `TECHNICAL_AUDIT.md` / `TECHNICAL_RESULTS.md` / `RESUME_PROJECT_RESULTS.md` | 记录真实审计、实现、验证与局限 |

初始 SHA 清单列出的文件在 `before/` 中有逐文件副本；新增文件没有前身。因初始工作区无 Git，本目录也保留最终改动文件逐文件 after 快照与 SHA。初始 before 清单未覆盖的前序阶段工作均未重置；before/after 的覆盖范围与验证情况见 `snapshot_verification.txt`。

## 故障注入与结果

测试使用真实 MySQL trigger `SIGNAL SQLSTATE '45000'` 制造指定 SQL 失败，再从 TestRestTemplate 发起真实 HTTP 请求，并通过 JDBC 查询事务结束后的表状态。trigger 每例在 finally 中删除；完整测试结束前确认 trigger 数量为 0。

| 测试路径 | 异常点 | HTTP | 数据库断言 |
|---|---|---:|---|
| 赛事排赛 | event_match 映射 INSERT | 500 | 新建比赛 0 行；映射 0 行 |
| 阵容覆盖 | 新阵容 application INSERT | 500 | 原阵容保留 1 行；未出现半套新记录 |
| 球队解散 | team 软删除 UPDATE | 500 | team 未删除；成员仍 active；user.role、player.team_id 回滚；通知数量不变 |
| 入队审批 | player INSERT | 500 | application 仍 pending；无 team_member；user.role 不变；无 player |
| 成员幂等 | 同成员重复新增、停用后重加 | 200 | `(team_id,user_id)` 仅 1 行，最终 status=1 |
| 参数错误 | `page=not-a-number` | 400 | `ResultVO.code=400`，不泄露绑定异常名 |
| 资源不存在 | 比赛 ID 不存在 | 404 | `ResultVO.code=404` |
| 唯一键冲突 | 重复插入已存在 match_id 的赛果 | 409 | `ResultVO.code=409`，无重复键/索引细节 |
| 未认证/越权 | Security 过滤链请求 | 401/403 | HTTP 状态与 envelope code 一致 |
| 未预期异常 | 数据库 trigger SQL 错误 | 500 | 客户端只见通用消息，不包含 SQLSTATE、SQLException 或 trigger 文本 |

命令：

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/football_admin_phase5_tx_20261017?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
$env:SPRING_DATASOURCE_USERNAME='root'
$env:SPRING_DATASOURCE_PASSWORD=$env:DB_PASSWORD
mvn -f backend/pom.xml test -Dphase3.db.integration=true -Dphase4.db.integration=true -Dphase5.db.integration=true
```

最终结果：38 tests / 0 failures / 0 errors / 0 skipped / BUILD SUCCESS。`MAVEN_PHASE5_FINAL_RESULTS.txt` 保存最终运行完整输出，Surefire XML/TXT 位于 `backend/target/surefire-reports/`。此前两次调试失败原因是 schema 守卫固定值和夹具重复记录，不是业务断言；修复守卫/测试夹具后，从 init.sql 新建干净库完成最终全量测试。

## 清理和业务库不变性

- 测试前真实业务库核心表精确计数：application=0、goal_record=5、match=4、match_result=2、notification=0、player=8、team=4、team_member=0、user=3。
- 真实库关键索引：match 的 `idx_match_time/home_team/away_team/status`；match_result 的 `uk_match_id` 与 `idx_match_id`；goal_record 的 `idx_match_id/player_id`；team_member 的 `uk_team_member_team_user(team_id,user_id)`、`idx_team_member_user_id/status`。
- 测试后重复读取上述计数和索引元数据，数据行逐值对照相同，索引定义相同。见 `business_db_before.txt`、`business_db_after.txt`、`business_db_comparison.txt`。`user` 表精确行数为 3；信息架构的 `TABLE_ROWS` 是估算值，不用于本次对照。
- 临时库清理前：trigger=0、application=0、team_member=0、notification=0、match=4、match_result=2；见 `isolated_schema_before_cleanup.txt`。
- `cleanup_phase5_schema.ps1` 仅删除精确 Phase 5 schema；随后 `information_schema.schemata` 确认该 schema 不存在。见 `final_verification.txt`。
- 未向 `football_admin` 执行测试 DML、DDL、触发器创建或删除。

## 保留与复现限制

完整快照 SHA-256 清单：`snapshot_before_sha256.csv`、`snapshot_after_sha256.csv`；逐项校验说明：`snapshot_verification.txt`。阶段 2/3/4 证据目录及阶段 4 `TRANSACTION_EXPERIMENT.md` 保留。测试只证明上述隔离环境/种子状态下列出的事务路径，不证明生产数据规模、部署环境或所有业务事务语义。

新增关联缺失用例：入队审批指向不存在球队时 HTTP 404，申请保持 pending、无成员记录且用户角色不变；成员删除不存在关系返回 HTTP 404。
