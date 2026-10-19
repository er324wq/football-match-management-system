# 技术改造与验证结果

更新时间：2026-10-17。所有数据仅记录本工作区实际执行情况。

## 阶段 4：事务一致性与异常处理（2026-10-17）

### 实现（A级：已实现并经过回归/隔离数据库测试）

| 修改文件 | 内容 | 验证 |
|---|---|---|
| `backend/src/main/java/com/football/service/MatchService.java` | 增加比赛创建及更新的业务事务入口 | Controller 单元测试及 HTTP/MySQL 故障注入 |
| `backend/src/main/java/com/football/service/impl/MatchServiceImpl.java` | 比赛与赛果同步纳入单一事务；写失败抛异常；比赛批量状态更新加事务并移除吞异常逻辑 | 两表回滚及批量状态回滚 HTTP 集成验证 |
| `backend/src/main/java/com/football/mapper/MatchResultMapper.java` | 增加读取逻辑删除赛果和原位恢复方法，解决 `upcoming` 状态逻辑删除后唯一键仍占用导致不能重新录入赛果 | 真实 HTTP 测试验证 finished→upcoming→finished，恢复为 1 条活跃赛果且分数更新 |
| `backend/src/main/java/com/football/controller/MatchController.java` | 通过 MatchService 事务入口创建/更新；移除 Controller 中独立、忽略结果的赛果写入逻辑 | 原 MatchController 测试 5 项通过 |
| `backend/src/main/java/com/football/controller/TeamController.java` | `save` 使用事务；application 写入 false 时抛异常，使球队写入回滚 | 真实 HTTP+MySQL 故障注入 |
| `backend/src/main/java/com/football/service/impl/ApplicationServiceImpl.java` | 审批写入 team / 更新 application 返回失败时抛异常 | 审批故障下真实 HTTP+MySQL 状态回滚 |
| `backend/src/test/java/com/football/controller/MatchControllerTest.java` | 控制器测试改为验证调用事务业务入口，不再模拟被移出的 Controller 私有 SQL 编排 | 5/5 PASS |
| `backend/src/test/java/com/football/integration/TransactionalWorkflowIntegrationTest.java` | 增加五项仅在 `phase4.db.integration=true` 且当前数据库名精确匹配临时 schema 时启用的真实 HTTP+MySQL 事务/赛果状态转换测试 | 5/5 PASS |

### 原子性与运行证据

- 比赛写入流程：match INSERT/UPDATE → 查找并 INSERT/UPDATE match_result；重置为 upcoming 时删除 match_result。现在都通过一个 public Spring 事务 Service 方法。第二步 SQL 异常触发回滚。
- 球队创建：team INSERT → 若提供 userId 且无已批准申请则 INSERT application。现同处于公开 Controller 事务方法，失败抛异常回滚两步。
- 审批创建球队：application 查找 → team INSERT → application 状态/target 更新。已有 Service 事务边界；本轮补充检查写入 false，并实测 team INSERT 失败后 application 保持 pending。
- 比赛状态更新：逐行扫描符合现有转换规则的比赛并更新，改为在一个事务内执行；异常现在传播并使此前更新回滚，而不是 catch 后返回 0。
- 比赛赛果逻辑删除后重新进入非 upcoming 状态时，从唯一键保留的逻辑删除记录恢复 `deleted=0` 并更新比分，不再插入冲突行；往返状态行为由真实 HTTP+MySQL 集成测试覆盖。
- Spring Boot 自动配置单一 JDBC 事务管理器；真实集成测试断言事务管理器类型，MyBatis 与 JDBC 操作共用应用 datasource。MySQL 版本输出为 `26.7.0`，项目 schema 13/13 表为 InnoDB。
- 环境：Windows；JDK 8.0.504；IntelliJ 内置 Maven；MySQL localhost:3306。测试 schema `football_admin_phase4_tx_20261017` 来源项目 `init.sql`，初始 13 表；最终已删除。
- 故障注入为 MySQL trigger `SIGNAL SQLSTATE '45000'`。API 响应均为 HTTP 500；测试对数据库最终状态断言：

| 故障场景 | 注入异常 | HTTP | 回滚后状态 |
|---|---|---:|---|
| 完成态比赛创建后写赛果失败 | `java.sql.SQLException`（MyBatis 包装为 Uncategorized SQLException） | 500 | marker 比赛 0 行；关联赛果 0 行 |
| 创建球队后写申请失败 | `java.sql.SQLException` | 500 | marker team 0 行；application 0 行 |
| 比赛状态批量更新失败 | `java.sql.SQLException` | 500 | 目标比赛仍为 `upcoming` |
| 审批申请时创建球队失败 | `java.sql.SQLException` | 500 | 申请仍为 `pending`；marker team 0 行 |
| finished→upcoming→finished | 无注入；两次成功状态更新 | 200 / 200 | match_result 总计 1 行，deleted=0，比分更新为 3–1 |

- 全量命令：`mvn -f backend/pom.xml test -Dphase3.db.integration=true -Dphase4.db.integration=true`，实际结果 29 run / 0 failures / 0 errors / 0 skipped，BUILD SUCCESS。分组：MatchController 5、Phase 3 DB 5、Phase 4 tx DB/HTTP 5、Security HTTP 11、GoalRecordService 1、UserService 2。
- 真实业务库未执行测试写入、DDL 或删除。测试后只读 `COUNT(*)`：user 3、team 4、player 8、match 4、match_result 2、goal_record 5，和阶段前记录一致；临时 schema 查询结果为不存在。
- 命令输出、测试报告、隔离 schema 初始化/清理脚本、测试 schema SQL、before 快照和 SHA-256 记录见 `phase4-evidence/2026-10-17/`。

### 尚未充分验证

- 阶段 4 的 A 级覆盖上述 4 个真实故障回滚路径、1 个结果状态往返和 Maven 全回归。其他已有事务业务（赛事随机排赛、阵容写入、球队解散、入队审批成员同步）还没有真实故障注入测试。
- 应用通知方法捕获异常并按 best-effort 处理；是否应与审批/阵容事务强一致尚无业务规则，暂不改变。
- 不包含性能测量；没有吞吐量、延迟或性能变化结论。

## 环境与项目

- Java 8；Spring Boot 2.7.18；MyBatis-Plus 3.5.3.1；Auth0 java-jwt 4.4.0；Spring Security；MySQL。
- 后端 HTTP 端口：8090；前端开发端口：8085；MySQL 服务：本机 3306；数据库：`football_admin`。
- Maven：IntelliJ IDEA 内置 Maven。`mvn test` 实际编译 74 个后端源文件。
- 数据库有 13 张表、12 个外键引用；初始化样例数据行数见 `TECHNICAL_AUDIT.md`。
- 项目目录没有 Git 元数据，所以未记录 Git diff/status；没有重置任何文件。

## 阶段 0：基线

| 项目 | 结果 |
|---|---|
| MySQL 连接 | PASS；连接本机 3306 的 `football_admin` |
| 后端编译 | PASS；Maven 编译 74 个 Java 文件 |
| 后端测试原基线 | 原来没有测试代码；初次 `mvn test` 是 BUILD SUCCESS、0 tests |
| 后端启动 | PASS；Spring Boot/Tomcat 在 8090 启动并成功建立 Hikari/MySQL 连接 |
| 匿名公开 API | PASS；`GET /api/match/list?page=1&size=10` 返回 HTTP 200 |
| 匿名受保护 API | PASS；`GET /api/user/list?page=1&size=10` 返回 HTTP 401 |
| 前端构建 | 尚未执行。已有 `frontend/dist`，为避免覆盖现有构建产物，本阶段未运行会重写 dist 的 build 命令 |
| 正确账号登录 | 尚未验证；当前数据库 seed 的 admin 对 README/SQL 注释中的两个候选密码均返回 401。未修改用户密码或新增账户 |

启动日志证明服务能连接数据库。初始启动曾打印开发用 generated security password 警告；本轮排除未使用的默认用户自动配置后，最近测试启动未再出现该警告。

## 阶段 1：Spring Security + JWT

### 当前认证链路

`POST /api/auth/login` → `UserServiceImpl.login()` 查询用户、验证 BCrypt/历史密码 → `JwtService.createToken()` 签发 Token → 返回 user+token → 前端 Vuex/localStorage 保存 → Axios 添加 `Authorization: Bearer ...` → `JwtAuthenticationFilter` 验签、校验有效期和必需 claim → 建立 `Authentication`/`SecurityContext` → `SecurityConfig` 按角色授权。

### 修改记录

| 文件 | 修改 | 原因 |
|---|---|---|
| `backend/src/main/java/com/football/config/SecurityConfig.java` | 限制评分接口角色；修正公开比赛结果路由 `/with-match` 与 Controller 实际路径一致 | 修复评分接口越权并让真实只读赛事数据路由匹配公开规则 |
| `backend/src/main/java/com/football/controller/UserController.java` | 用户资料/更新越权时抛出 `AccessDeniedException` | 使越权响应从 HTTP 200 envelope 改为 HTTP 403 |
| `backend/src/main/java/com/football/controller/StatisticsController.java` | 他人个人表现越权时抛出 `AccessDeniedException` | 使越权响应返回 HTTP 403 |
| `backend/src/main/java/com/football/controller/NotificationController.java` | 他人通知读取/修改越权时抛出 `AccessDeniedException` | 使越权响应返回 HTTP 403 |
| `backend/src/main/java/com/football/service/impl/UserServiceImpl.java` | 禁用账号与错误密码统一为通用凭证错误 | 避免错误响应暴露账号状态 |
| `backend/src/main/java/com/football/FootballAdminApplication.java` | 排除未使用的 `UserDetailsServiceAutoConfiguration` | 消除自定义 JWT 认证外自动生成的默认用户密码 |
| `backend/src/main/resources/application.yml` | MyBatis 日志改为 `NoLoggingImpl` | 防止 stdout 输出 SQL 行和用户密码哈希 |
| `backend/src/test/java/com/football/security/SecurityIntegrationTest.java` | 新增 8 个本机 HTTP 集成测试 | 验证匿名/公开访问、非法/过期 Token、角色越权、用户自身/他人资料边界、错误密码 |
| `backend/src/test/java/com/football/service/UserServiceLoginTest.java` | 新增 2 个 Service 单元测试 | 验证成功登录生成 Token 且响应用户不含密码、禁用账号拒绝登录 |

### 角色与路由

| 主体 | 已验证的接口规则 |
|---|---|
| 匿名 | 比赛列表只读接口可访问；用户列表返回 401 |
| `user` | `/api/user/list` 与 `/api/scoring/referee` 返回 403；本人资料可读，他人资料业务拒绝 |
| `referee` | `/api/scoring/all` 返回 403；本人评分列表可访问 |
| `admin` | `/api/user/list` 和 `/api/scoring/all` 均可访问 |

### 自动化测试

命令：`mvn -f backend/pom.xml test -q`（JDK 8、IntelliJ 内置 Maven；集成测试使用本机 MySQL 与随机 HTTP 端口）。

| 测试集 | 用例数 | 通过 | 失败 | 证据 |
|---|---:|---:|---:|---|
| SecurityIntegrationTest | 9 | 9 | 0 | `backend/target/surefire-reports/TEST-com.football.security.SecurityIntegrationTest.xml` |
| UserServiceLoginTest | 2 | 2 | 0 | `backend/target/surefire-reports/TEST-com.football.service.UserServiceLoginTest.xml` |
| 合计 | 11 | 11 | 0 | Maven Surefire 报告 |

用例覆盖：匿名受保护请求、匿名公开请求、格式错误/错误签名/过期 Token、普通用户访问管理员接口、管理员访问管理员接口、裁判/用户评分权限、用户本人/他人资料访问、错误密码 HTTP 401、登录 Service 成功/禁用分支。

最近一次测试启动了随机端口 Spring Boot 测试实例并连接本机 MySQL；11 项用例全部通过。测试输出中没有 mapper 查询行，也没有生成默认安全密码警告。

**尚未验证：**真实数据库账户正确密码对应的成功 HTTP 登录。集成测试里的成功签发测试使用 mock Mapper/PasswordEncoder；JWT 权限集成测试由当前 `JwtService` 签发测试 Token，不能替代真实账号密码验证。无法确认当前 seed 账号的有效密码，因此没有标记端到端登录成功。

### 仍需处理的安全项

- 上述用户、通知及个人统计越权分支已返回 HTTP 403；其他未逐项覆盖的 Controller 仍需按业务继续审计。
- 登录错误信息已统一为通用凭证错误；明文密码兼容分支仍待清理。
- 兼容明文密码并在成功登录后升级 BCrypt；清理历史数据后应移除兼容分支。
- JWT 登出是客户端删除；服务端没有撤销机制。用户被禁用或角色变更后，既有 Token 在过期前不会自动失效。
- 本地 `application.yml` 仍有 DB/JWT fallback；部署环境应通过环境变量注入强密钥，避免使用代码内默认值。
- 前端 localStorage Token 受同源脚本/XSS 风险影响；应持续保持严格 XSS 防护。

## 阶段 2：数据库查询分析与隔离实验

先对比赛分页、射手榜、积分榜 Mapper SQL 完成真实库只读审计和调用链定位（详见 `DATABASE_EXPLAIN_BASELINE.md`）。随后按独立方案在一次性 MySQL schema 中生成确定性数据并比较索引，实验结束已删除该 schema。真实 `football_admin` 未做 DDL/DML，SQL/索引/分页源码未改。

隔离数据：128 team、12,800 player、20,000 match、13,000 match_result、100,000 goal_record。核心查询每个状态 3 次预热、20 次 `EXPLAIN ANALYZE` 采样；实际 Spring Boot 2.7.18 指向该隔离 schema，公开接口每状态 3 次预热、20 次串行 HTTP 调用。实验完整原始输出、SQL、生成脚本、样本 CSV、SHA-256 快照、数据库清理与解释见 `phase2-evidence/2026-10-17-isolated-run/EXPERIMENT_REPORT.md`。

**索引结果（只代表本机合成负载）：** `match_result.match_id` 上普通 `idx_match_id` 与唯一 `uk_match_id` 定义相同；删除隔离库中的普通索引后，唯一键仍满足关联/FK、三条核心查询结果 hash 一致，查询计划继续使用唯一键。10 次交替的 13,000 行批量写入，双索引表 median/P95 为 303.914/341.917ms，仅唯一索引为 278.499/293.992ms。证据支持建议未来通过可回滚迁移移除普通重复索引；本轮未更改真实库或 `init.sql`。

射手榜候选 `(deleted,player_id)` 在隔离数据上产生覆盖索引访问：EXPLAIN ANALYZE 的三个基线组 median 为 329/372.5/353.5ms，两个候选组为 143.5/136.5ms；同一实际 API 射手榜 median 为基线 355.341ms、候选 158.332ms、撤销后 367.564ms，HTTP 状态和业务码均成功，响应 12,160 条。因候选数据分布是人工生成、且未用代表性真实分布复测，不把它当生产性能提升，也不建议立即应用到真实库。比赛分页 `(deleted,match_time)` 候选比基线慢；status/standings 相关候选未被优化器采用，没有明确收益。

数据库执行计划时间和 HTTP 完整响应时间分开保存，不能互相替代。所有 API 是本机单用户串行请求；并发、CPU/JVM/数据库资源采样和线上验证未做。测试条件、所有查询各组 median/P95、行数和错误数见实验报告；没有虚构优化前后真实业务库性能数据。

**阶段 1 回归：** 隔离 schema 清理且 API 进程停止后，重新执行 `mvn -f backend/pom.xml test -q`，退出码 0；`SecurityIntegrationTest` 9/9、`UserServiceLoginTest` 2/2，失败/错误/跳过均为 0。认证授权代码未修改。

## 尚未实施或尚未测试的阶段

- Redis：未实现、未配置、未测试。
- JMeter/并发/吞吐量/P95/P99/错误率：尚未测试。暂无数据。
- Docker/Linux/Nginx 部署：未发现项目部署配置，尚未构建或部署。
- 前端 build：尚未执行；前端页面访问正常与否未重新验证。
- 性能提升百分比、缓存命中率、用户规模、数据规模：暂无数据，不得作为简历成果。

## 阶段 3：核心业务自动化测试与质量保障

**调用链审计：** 真实 Controller→Service→Mapper 路由、数据库约束和已知规则已补充到 `TECHNICAL_AUDIT.md`。覆盖比赛分页/写入及赛果同步、球队创建/解散关联、球员球队关系、进球明细、射手榜聚合和积分榜 SQL。赛果唯一约束、比赛/球队/球员/进球外键，以及 team_member 唯一关系均以 `init.sql` 为依据。

**确认并修复的问题：** `GoalRecordServiceImpl.save()` 原先无条件用服务器当前时钟覆盖请求中的 `goalTime`，与前端实际提交的比赛分钟字段冲突。移除覆盖后，使用 Bean Validation 将进球分钟限制为非空 1–130（与前端输入控件范围一致）；Controller 保存和更新使用 `@Valid`。服务测试断言保存保持请求值；隔离 MySQL 测试实际插入并读回 57 分钟，且详情 JOIN 返回预期比赛、球员和球队关联。

**新增测试：** `MatchControllerTest` 5 项（已结束比赛创建赛果、修改复用既有赛果、改回 upcoming 删除赛果、比赛创建失败不创建赛果、不存在比赛返回业务错误）；`GoalRecordServiceTest` 1 项；`SecurityIntegrationTest` 新增权限/校验用例 2 项（普通用户与裁判不能创建比赛、普通用户不能创建进球；裁判进球请求缺失/超界分钟返回 HTTP 400）；`FootballDatabaseIntegrationTest` 5 项（积分榜聚合/排序、射手榜聚合、进球值与关联查询、重复赛果唯一约束、球员球队外键拒绝非法关联）。

**数据隔离与实际测试：** 为本阶段建立一次性 schema `football_admin_phase3_it_20261017`，基于 `init.sql` 中的结构与确定性种子运行真实 MySQL Mapper/Service 集成测试；进球写入用例以 Spring 测试事务回滚。Spring Boot 安全 HTTP 测试运行于该隔离 schema。全部测试后查询业务库核心行数仍为 user=3、team=4、player=8、match=4、match_result=2、goal_record=5，`match_result` 原索引保留；随后只删除本阶段临时 schema，并确认其不存在。

**射手榜一致性断言：** 隔离 MySQL 测试事务中为已有球员插入一条 goal_record，读回 goalTime=57，并重新查询射手榜确认该球员计数从 1 变为 2；测试事务结束后回滚。

**完整 Maven 测试结果：** 在 JDK 8 下执行 `mvn -f backend/pom.xml '-Dphase3.db.integration=true' test -q`，同时通过 `SPRING_DATASOURCE_URL` 指向临时 schema。Surefire 报告：`MatchControllerTest` 5/5、`FootballDatabaseIntegrationTest` 5/5、`GoalRecordServiceTest` 1/1、`SecurityIntegrationTest` 11/11、`UserServiceLoginTest` 2/2；总计 **24 项，失败 0、错误 0、跳过 0**。输出在 `backend/target/surefire-reports/`，复现和清理步骤见 `phase3-evidence/2026-10-17/TESTING.md`。

**未覆盖/待确认：** 尚无真实 HTTP 创建并提交成功的比赛/赛果/进球集成流程（为避免破坏现存业务数据，写入仅在单测 mock 或事务隔离测试内）；数据库实测未覆盖错误 team/player 外键输入、跨队进球约束或重复赛果插入；积分榜相同 points/GD/goals_for 时无 tie-breaker；射手榜是否列零进球或过滤禁用球员未确认。比赛与赛果同步、球队创建与申请写入缺少方法级事务边界；未增加多表回滚测试。比赛/比分分页参数及非负比分校验尚未实现。没有声称覆盖全部核心业务或线上端到端写流程。

**证据与文件：** 新测试源代码位于 `backend/src/test/java/com/football/{controller,service,security,integration}/`；测试 schema 初始化副本、自动创建/清理脚本、测试说明和业务库复核见 `phase3-evidence/2026-10-17/`。本阶段没有修改真实业务库、Mapper SQL 或表结构，没有性能结论。


## 阶段 5：事务边界与统一接口异常处理（2026-10-17）

### 已实现并测试（A级）

- 对赛事排赛、阵容覆盖、球队解散、入队审批成员同步逐项审计写入顺序；仅在需要多表一致性的既有事务流程中检查写入结果并将失败传播，避免异常前半段数据正常提交。
- `TeamMemberServiceImpl.addMember/removeMember` 以事务包住成员与用户角色同步；同一队/用户重复加入保持单行幂等，停用后重新加入复用唯一行。
- 新增 `ResourceNotFoundException` 与全局 `ApiExceptionHandler`，保留 `ResultVO` JSON 字段，异常路径映射 400/401/403/404/409/405/500；未知异常对客户端仅返回通用文本，Security FilterChain 自身 401/403 Handler 保持有效。
- 修复部分 Controller 对不存在资源的 HTTP 200 错误包络；移除进球 Controller 捕获未知异常后伪装成功风格响应的代码；登录错误文案统一，不回显底层认证异常；前端 Axios 展示服务端 envelope 消息。
- 隔离 MySQL 中真实 HTTP trigger 故障注入覆盖 4 条事务链路：排赛映射插入失败、阵容新写入失败、球队解散最后更新失败、入队审批球员档案插入失败。每项在返回 HTTP 500 后均通过 JDBC 断言早期写入已回滚。
- API 状态码测试覆盖参数错误 400、资源缺失 404、唯一键重复 409、Security 未认证 401、越权 403、数据库故障 500 脱敏；重复成员请求与停用后再加入的数据库状态也已断言。
- 故障注入日志中观察到的最外层持久层异常为 `org.springframework.jdbc.UncategorizedSQLException`，MySQL 根因 `java.sql.SQLException` / SQLSTATE 45000；客户端响应只含通用错误 envelope，不回显 SQL 或 trigger 消息。
- 最终完整 Maven 测试 **38/38 通过，0 失败、0 错误、0 跳过**；原 29 项全部保留。测试详情、命令和日志见 `phase5-evidence/2026-10-17/TRANSACTION_AND_API_ERRORS.md`、`MAVEN_PHASE5_FINAL_RESULTS.txt` 及 Surefire 报告。

### 测试边界与未完成事项

- 真实回滚结果来自一次性 MySQL schema `football_admin_phase5_tx_20261017`，不是生产库验证；该 schema 已按精确名称清理。`football_admin` 核心表行数及关键索引前后只读对照未变。
- 通知依旧按原行为部分 best-effort；不做 outbox/统一消息事务。
- 赛事映射无 FK、阵容中非成员跳过、分页和比分业务约束等待确认规则未改变。
- 全局处理器保留历史 envelope，但尚未把所有旧 `ResultVO.error` HTTP 200 接口统一成非 2xx。
- 生产环境日志采集/脱敏未部署验证；未知异常的客户端安全消息由集成测试验证，日志系统运维仍待确认。

### 阶段证据

本阶段 before/after 文件清单及 SHA-256、隔离 schema 初始化 SQL、setup/cleanup 脚本、业务库只读基线/复核、Maven 最终日志均归档在 `phase5-evidence/2026-10-17/`。阶段 2、3、4 历史证据未覆盖。
# 最终收尾状态（2026-10-17）

本轮为只读代码/证据复核，仅更新报告；未改业务代码、测试代码或数据库。

## 已验证成果汇总

- **安全与授权：** JWT/Spring Security 请求链路和角色边界有集成测试证据；阶段 1 的 11 项认证回归结果保留在阶段证据中，阶段 5 HTTP 回归再覆盖匿名 401、越权 403，并确认 MVC 全局异常处理不覆盖 Security 的响应。
- **SQL/索引：** 阶段 2 使用隔离 schema 和合成数据记录 SQL/EXPLAIN 与候选索引实验。`match_result.match_id` 重复索引只形成经实验支持的移除建议，未改真实业务库。实验规模、采样条件和局限见 `phase2-evidence/2026-10-17-isolated-run/EXPERIMENT_REPORT.md`。
- **业务数据一致性：** 阶段 3 总计 24 项测试通过，涵盖隔离 MySQL Mapper/Service、进球与射手榜断言和 Security HTTP 测试；边界与规则待确认事项仍保留。
- **事务：** 阶段 4 为 29/29；阶段 5 在隔离 MySQL schema 通过排赛、阵容替换、球队解散、入队审批四条 HTTP 故障注入回滚断言，并测试成员重复请求幂等。
- **异常语义：** 已验证参数错误 400、匿名 401、越权 403、资源缺失 404、唯一约束冲突 409、未知数据库异常 500 通用 envelope；已有正常响应的 `ResultVO` 结构保持兼容。

## 当前构建与测试状态

| 检查 | 实际结果 | 证据/范围 |
|---|---|---|
| 阶段 4 全量测试 | 29/29 通过 | 历史阶段 4 结果；不是当前最新计数 |
| 最近一次完整 Maven 测试 | **38/38 通过，0 失败、0 错误、0 跳过** | `phase5-evidence/2026-10-17/MAVEN_PHASE5_FINAL_RESULTS.txt` 与 Surefire 原始报告；原 29 项加阶段 5 新增 9 项 |
| 本次收尾编译检查 | `mvn -f backend/pom.xml -DskipTests test-compile`，Java 8 下 BUILD SUCCESS；76 主源文件、7 测试源文件编译 | 本次检查未运行测试；有 deprecated/unchecked 提示 |
| 本次真实业务库操作 | 未执行 | 没有 DDL、测试写入、删除或破坏性操作 |
| 生产 API/部署/压测 | 未验证 | 不形成生产性能或部署结论 |

本次收尾没有重跑使用临时 schema/trigger 的全量集成套件；38/38 是阶段 5 最终实际运行记录。阶段 5 的临时 schema 清理及真实业务库只读前后对照见 `phase5-evidence/2026-10-17/`。

## 未解决风险

- 备份恢复路径使用 MySQL `TRUNCATE`，不能依靠 Spring 事务保证整体回滚；动态标识符白名单也需加强。该路径未在本轮执行，建议视作生产上线阻断项。
- `application.yml` 已移除数据库密码和 JWT 默认回退值；空用户表管理员由 `INITIAL_ADMIN_PASSWORD` 初始化。SQL 测试用户使用随机、未保存原文口令生成的哈希。旧明文密码兼容逻辑仍待淘汰。
- JWT 没有服务端撤销机制；申请解析的非法时间回退、通知 best-effort、旧接口 HTTP 200 错误包络和若干待确认业务规则均保留，详见 `TECHNICAL_AUDIT.md`。
- Redis、Docker/Linux 部署、生产日志/脱敏、并发压测、CPU/JVM/数据库资源观察均未验证或未实施。本报告不包含性能百分比、吞吐量、P95/P99 或命中率结论。

## 最终状态

**当前项目已经达到可用于求职展示的验证程度，后续性能压测和剩余边界测试属于增强项。** 该结论限定于已验证代码和本机/隔离环境演示，不代表生产部署就绪；备份恢复与部署凭据风险应先处理再上线。
