# 项目现状审计

审计日期：2026-10-17
事实来源：当前工作区代码、配置、MySQL `football_admin` 数据库、Maven 构建与本机运行结果。

## 仓库与验证边界

- 工作目录没有 `.git` 元数据，无法读取 Git 状态或区分既有未提交变更；本轮未重置、清理或覆盖项目文件。
- 后端 `src/main/java` 有 74 个 Java 源文件；原来没有 `src/test` 测试代码，本轮增加后见测试结果文件。
- 已在当前机器找到 JDK 8 和 IntelliJ 内置 Maven。数据库配置不再展示密码值。
- 后端实际配置端口为 8090，前端开发服务器配置为 8085；README 的 8080/8081 与当前配置不一致。
- 当前配置 JDBC 地址为 `localhost:3306/football_admin`，实测 MySQL 服务在 3306，连接成功。

## 技术栈（按依赖和源码确认）

| 范围 | 已使用技术 |
|---|---|
| 后端 | Java 8、Spring Boot 2.7.18、Spring Web、Spring Security、Bean Validation、MyBatis-Plus 3.5.3.1、MySQL Connector/J 8.0.33、Auth0 `java-jwt` 4.4.0、Lombok |
| 数据库 | MySQL（本机服务；数据库名 `football_admin`） |
| 前端 | Vue 2.6.11、Vue Router 3.2、Vuex 3、Axios 0.21.1、Element UI 2.15.12、Webpack 3 |
| 当前未确认使用 | Redis、Docker、Nginx、JMeter：未发现可验证配置或运行证据 |

Spring Security 配置中定义了 BCrypt `PasswordEncoder`。目前没有 MQ 或微服务依赖证据。

## 后端结构与架构

当前主链路为 `Controller → Service / ServiceImpl → Mapper / MyBatis-Plus → MySQL`。Controller、Service 接口与实现、Mapper、Entity、少量 DTO/VO、Config、Security 均存在。

- DTO：存在 `LoginDTO`；不是所有请求均通过 DTO 接收。
- VO：存在 `ResultVO`、排行榜和性能展示 VO。
- 统一响应：多数接口返回 `ResultVO`；部分认证/鉴权错误的 HTTP 状态和响应体约定需继续统一。
- 异常处理：Security EntryPoint、AccessDeniedHandler 和 JWT Filter 有 JSON 错误响应；未发现覆盖全局业务异常的统一 `@ControllerAdvice`。
- 参数校验：登录 DTO 使用 Validation；其他接口以 Controller 手工检查为主。
- 日志：初始基线的 MyBatis `StdOutImpl` 会输出查询行（含用户 BCrypt 哈希）；本轮已改为 `NoLoggingImpl`，最近测试输出未再出现查询行。
- 权限：SecurityFilterChain 使用无状态会话、JWT Filter、角色规则；没有 `anyRequest().permitAll()`。

## 实际业务模块

用户/认证、球队、球队成员、球员、比赛、比赛结果、进球记录、红黄牌、裁判评分、赛事/球队申请、通知、积分榜/射手榜、性能指标/操作日志/数据库备份。模块名以 Controller、Service 和实体为依据。

## 数据库现状

实测 `football_admin` 有 13 张基础表：`application`、`card_record`、`db_backup`、`goal_record`、`match`、`match_result`、`match_scoring`、`notification`、`operation_log`、`player`、`team`、`team_member`、`user`。`information_schema` 查询到 12 个外键引用关系。

初始化数据复核：user 3 行、team 4 行、player 8 行、match 4 行、match_result 2 行、goal_record 5 行。该数量只代表本次审计时数据库状态，不代表业务规模。

阶段 2 于 2026-10-17 再次执行精确 `COUNT(*)` 时，当前行数为 user 3、team 4、player 8、match 4、match_result 2、goal_record 5，与前次审计样本行数一致。精确计数和索引元数据保存在 `phase2-evidence/2026-10-17/database_metadata.txt`。

核心关联：

- team 1:N player；team/user 通过 team_member 形成 N:M（team_member 有外键约束）。
- team 1:N match（主队/客队各一条外键）。
- match 1:0..1 match_result（`match_result.match_id` 唯一）。
- match/player 1:N goal_record、card_record。
- match/user(referee) 1:N match_scoring。
- user 到 application、notification 的关联是业务字段；这两张表当前没有 user 外键。user 到 team_member 有外键。

核心索引已包含用户 username 唯一键、比赛时间/状态/主客队索引、赛果 match_id 唯一键、进球 match_id/player_id、球队名称/城市、球员 team_id 等。应以实际查询计划和更大规模测试决定索引调整，不能由目前个位数种子数据推断性能。

### 只读 EXPLAIN 基线

执行环境：本机 MySQL，初始化数据如上；`EXPLAIN` 采用服务器 TREE 计划输出。它是估算执行计划，不是查询耗时测量。

| 查询 | 当前计划摘要 |
|---|---|
| 比赛分页列表 | `match` 估算 4 行 table scan 后按 `match_time` 排序；球队按主键查找，赛果按唯一索引 `uk_match_id` 查找 |
| 射手榜 | `goal_record` 估算 5 行 table scan，player/team 按主键查找；聚合使用 temporary table，结果再排序 |
| 球队列表 | 估算 4 行 table scan 后按 `create_time` 排序 |
| 球员列表 | 估算 8 行 table scan，再过滤 `status` 与 `deleted` |
| 积分榜 | 比赛结果当前估算 2 行 scan；match 通过主键查找，分别对主/客队 UNION ALL，再 temporary table 聚合和排序 |

当前数据极少，扫描表比索引查找更便宜是合理选择；本阶段没有据此盲目加索引。原始 SQL、传统 EXPLAIN 和 EXPLAIN ANALYZE 输出已保存到 `phase2-evidence/2026-10-17/`，摘要和逐条解读见 `DATABASE_EXPLAIN_BASELINE.md`。

阶段 2 保存了核心 SQL、传统 EXPLAIN 与 EXPLAIN ANALYZE 原始输出：`phase2-evidence/2026-10-17/explain_queries.sql`、`traditional_explain.txt`、`explain_output.txt`。小表上的单次 EXPLAIN ANALYZE 仅为诊断值，不构成性能基准或前后优化证据。

## 安全审计摘要

- JWT 由 Auth0 `java-jwt` 签发；包含 `userId`、`username`、`role`、签发与过期时间。公开整理时已移除配置文件中的默认密钥，启动需通过 `JWT_SECRET` 提供强随机密钥。
- JWT Filter 验签、解析 claim，并写入 `SecurityContext` 和 request attributes；过期/无效 Token 返回 401。
- 数据库角色只有 `admin`、`referee`、`user`；未新增角色。
- 管理员路由、裁判/管理员写操作、匿名只读数据均有规则。阶段 1 发现并修复评分列表授权缺口：普通用户不应读取全体评分；“裁判评分”用户路由限制为裁判/管理员。
- 前端 Axios 从 localStorage 取 JWT 并发送 Bearer Token，收到 401 清除本地会话；Vue Router 的路由限制只是用户体验层，后端权限才是访问控制。
- 未发现 `/admin/access`、固定管理员 token 或明确伪造登录绕过路径。
- 用户服务兼容明文密码并在登录成功时迁移 BCrypt；这带来历史明文密码兼容风险，待清理旧数据后移除兼容分支。
- 历史文档中的默认账号说明与本机 seed 密码验证不一致；已记录的候选登录均返回 401。现已移除 SQL 中的管理员账号种子与服务启动固定密码，首次管理员由 `INITIAL_ADMIN_PASSWORD` 初始化；测试种子密码哈希使用未保存的随机口令生成。
- 初始登录错误信息区分“账号禁用”和“用户名密码错误”；本轮已统一为通用凭证错误。JWT 无服务端撤销表，登出仅为客户端删除 Token，Token 在到期前仍可使用。
- 初始实现中部分 Controller 将越权拒绝包在 HTTP 200 响应体内；本轮已改为 Spring Security `AccessDeniedException`，以统一 403 处理器返回真实 HTTP 403，集成测试覆盖用户资料、个人统计、通知和用户更新。
- `SecurityConfig` 原本将 `/api/match-result/with-match-info` 标记为公开，但 Controller 实际路径是 `/api/match-result/with-match`；本轮已修正并加入匿名访问集成断言。
- `application.yml` 不再内置数据库密码或 JWT fallback；需要通过 `DB_PASSWORD`、`JWT_SECRET` 注入运行配置。MyBatis 查询行输出已关闭。
- CORS 当前使用精确来源配置（默认 `http://localhost:8085`）并允许 credentials；没有 `* + credentials` 组合。

## 基线执行状态与优先级

| 优先级 | 问题 | 证据 | 后续动作 |
|---|---|---|---|
| P1 | 正确账号的真实 HTTP 登录成功未验证 | 当前 seed 候选密码两次登录均 401；服务单元测试只 mock Mapper/Encoder | 获取可用测试凭证或使用隔离测试用户/测试库后验证，禁止改真实账户密码 |
| P1 | 用户/通知/个人表现越权错误的 HTTP 状态语义不统一 | 初始 Controller 使用 HTTP 200 + `ResultVO.code=403` | 已在 User/Statistics/Notification Controller 改为真实 403；集成测试通过 |
| P1 | SQL 日志输出用户哈希 | 初始配置 `StdOutImpl` | 已改 `NoLoggingImpl`；最新测试输出未见查询行 |
| P1 | 登录区分禁用账号与错误凭证；明文密码兼容仍在 | 初始 `UserServiceImpl` | 已统一登录失败提示并由单元测试覆盖；明文兼容尚未移除 |
| P1 | Spring Boot 未使用的默认安全用户生成临时密码日志 | 初始启动日志含 generated password 警告 | 已排除 `UserDetailsServiceAutoConfiguration`；最新测试没有该警告 |
| P2 | 积分榜、射手榜有全量聚合与排序 | Mapper SQL 与实际 EXPLAIN | 大数据量基准前保持现状；用真实数据规模评估 |
| P2 | 比赛状态更新读取全部未删除比赛并逐条更新，异常被吞 | `MatchServiceImpl.updateMatchStatuses()` | 单独设计事务/批量更新与失败日志测试 |
| P2 | README 端口和登录凭证说明与实测不符 | README 对比 yml/前端配置及登录结果 | 更新文档，等待确认有效演示凭证 |
| P3 | 无 Redis、Docker、Nginx、压测实施证据 | 配置/依赖扫描 | 后续阶段按真实需求决定是否实施 |

## 阶段 3：核心业务调用链与测试审计（2026-10-17）

### 实际调用链

| 业务 | Controller | Service / ServiceImpl | Mapper / 数据库 |
|---|---|---|---|
| 比赛分页/详情 | `MatchController.list/getById` | `MatchServiceImpl.listWithDetails/getById` | `MatchMapper.selectPageWithDetails` 或 MyBatis-Plus `BaseMapper`；分页 SQL 左关联主客队及赛果 |
| 比赛创建/修改 | `MatchController.save/update` | `MatchService` 的 `save/updateById` | Match 表；非 upcoming 状态另查写 `MatchResultService`，按 `match_id` 插入/更新；状态回到 upcoming 时按 `match_id` 删除赛果 |
| 球队 | `TeamController` CRUD | MyBatis-Plus `TeamService`（无自定义实现） | `TeamMapper`/`team`；创建时若提供 userId，还会写 approved `application`；解散方法带 `@Transactional` 并更新成员、解绑球员及通知 |
| 球员 | `PlayerController` CRUD/列表 | MyBatis-Plus `PlayerService`；`PlayerServiceImpl.listWithTeamName/getTopScorers` | `PlayerMapper` 与 `player`；team_id 关联 team，SQL 结果通过 `goal_record` 汇总射手 |
| 赛果 | `MatchResultController` CRUD | MyBatis-Plus `MatchResultService`；自定义读取走 `MatchResultServiceImpl` | `MatchResultMapper`；`match_result.match_id` 唯一键和 FK 指向比赛 |
| 进球记录 | `GoalRecordController` CRUD/详情 | `GoalRecordServiceImpl.save/listWithDetails` | `GoalRecordMapper`、`goal_record`；match_id/player_id 外键；详情查询关联 match/team/player |
| 射手榜 | `StatisticsController.getTopScorers` | `PlayerServiceImpl.getTopScorers` | `PlayerMapper.selectTopScorers` 按球员分组统计有效进球数并按进球数降序 |
| 积分榜 | `StatisticsController.getStandings` | `MatchServiceImpl.getStandings` | `MatchMapper.selectStandings` 两支主/客队 `UNION ALL` 后按队聚合胜平负、进失球、净胜球和积分 |

### 当前代码规则及关系约束

- 比赛状态列表 SQL 按 `deleted=0`，可选 status 过滤后按 match_time 倒序；Controller 没有对 page/size、主客队相同、队伍是否启用或比赛时间作参数校验。
- 比赛创建/修改仅当 status 非空且不等于 `upcoming` 时同步结果；缺省 status 不生成赛果。更新到 `upcoming` 会物理删除关联赛果。比分是否允许负数、未来状态值是否合法，当前没有明确校验/约束。
- 数据库 `match` 主客队外键级联 team 删除；`player.team_id` 外键在 team 删除时置空；`match_result.match_id` 唯一且外键级联；进球记录的 match/player 外键级联。team_member 有 team/user 外键和 `(team_id,user_id)` 唯一约束。进球球员是否必须属于参赛队，数据库与 Controller 均未验证。
- 积分榜当前将 status 为 `finished` 或 `ended` 的比赛计入，胜 3 分、平局各 1 分、负 0 分；排序 points DESC、goal_difference DESC、goals_for DESC。平分且前三项相同没有最终 tie-breaker。
- 射手榜过滤删除球员与删除进球记录，但未过滤禁用球员；SQL 写 LEFT JOIN 但 `WHERE g.deleted=0` 会排除无进球球员。是否需要零进球球员或排除禁用球员待产品确认。
- `init.sql` 中上述外键没有覆盖 application.user_id 与 notification.user_id；这两项关联完整性目前依赖应用层。

### 新发现与测试边界

- `GoalRecordServiceImpl.save()` 原先把前端提交的 goalTime 替换为当前系统时间分钟取模 90。前端表单实际提供 1–130 分钟输入，该实现破坏事件时间正确性。阶段 3 已删除覆盖逻辑，并在实体/Controller 校验 goalTime 非空且为 1–130；新增服务单测与隔离 MySQL 持久化/关联查询测试。
- `MatchController.save/update` 在一次 HTTP 请求内分别调用比赛与赛果服务，但方法本身未标注 `@Transactional`；服务层各自事务不能保证两表原子性。异常吞吐/失败返回的处理亦不足，仍需事务边界设计与真实回滚测试。
- TeamController.create team 后再写 application，但 create 方法未见 `@Transactional`；application 写失败可能造成孤立球队。Dissolve 有事务，但其他多表业务需逐项审计。
- MatchController、MatchResultController、TeamController、PlayerController 的写接口普遍无 Bean Validation；本阶段只修复有明确前端范围且已证实被覆盖的进球时间字段。比赛、赛果非负比分等规则需确认后设计 DTO/校验。
- 真实 MySQL 集成验证只在一次性 `football_admin_phase3_it_20261017` schema 上运行并清理。`football_admin` 行数复核未变化；没有对真实业务数据执行测试写入/删除/DDL。

## 阶段 4：事务一致性与异常处理（2026-10-17）

### 事务边界审计与修复

- MatchController 原先先直接写 `match`，随后用独立 MatchResultService 查询并写 `match_result`；Controller 无事务，第二步布尔返回值被忽略。现改为调用 MatchService 的公开 `createMatch/updateMatch`，两个方法由 Spring 事务代理执行并同步赛果；关联赛果写入/更新/删除失败时抛异常，触发两表回滚。业务条件保持原样：仅非空且非 `upcoming` 状态同步赛果，更新状态为 `upcoming` 删除关联赛果，状态为空时不动赛果。
- `TeamController.save` 原先写入 team 后尝试写已批准 `create_team` application，忽略 application 保存失败并仍返回成功。现为公开 Controller 方法添加 `@Transactional(rollbackFor=Exception.class)`，若申请写入返回 false 则抛异常回滚 team。该 Controller 先前已有公开 `dissolve` 事务方法，因此使用现有 Spring 事务代理模式，不存在自调用绕过。
- `ApplicationServiceImpl.updateStatus` 已有公开 Service 事务代理，审批 create_team 时先写 team 再更新 application；此前忽略两步的 false 返回。现对 team 保存和 application 状态更新失败显式抛异常。通知创建当前被 catch 后忽略，按现有行为作为 best-effort 副作用保留，不将通知成功纳入原子性承诺。
- `MatchServiceImpl.updateMatchStatuses` 原先无事务、逐条更新且 catch 所有 Exception 后返回 0；现加入 `rollbackFor=Exception.class`，更新 false 时抛错，不再吞异常。接口仍保留现有 GET 路径和状态转换规则。
- 发现并修复状态往返的逻辑删除冲突：比赛状态改为 `upcoming` 时，MyBatis-Plus 逻辑删除赛果，但唯一键 `uk_match_id` 仍占用该 match_id；后续再次同步赛果会因普通查询看不到已删除行而尝试重复 INSERT。现 Mapper 可按 match_id 读取逻辑删除行并在同一事务内恢复，保存原有赛果唯一行。

### Spring 与数据库验证

- 项目只配置一个 Spring datasource，MyBatis Mapper 与 JdbcTemplate 使用同一 datasource；测试注入的事务管理器为 `DataSourceTransactionManager`（运行时实现为 Spring `JdbcTransactionManager` 子类）。事务方法是从 Controller/其他 Bean 外部调用的 public 方法；没有依赖同类 self-invocation。
- 一次性 schema `football_admin_phase4_tx_20261017` 使用项目 `init.sql` 创建，MySQL 服务器报告版本 `26.7.0`，13 张表均为 InnoDB。该 schema 初始核心表行数：match 4、match_result 2、team 4、application 0。
- 通过隔离 schema 临时 BEFORE INSERT/UPDATE trigger 触发 SQLSTATE `45000`。四个真实 HTTP 失败场景均返回 HTTP 500；测试对 SQL 写入前后状态执行数据库断言。比赛结果写入失败后 marker match 与 result 不存在；球队 application 写入失败后 marker team 与 application 均不存在；批量状态写入失败后目标 match 状态仍为 `upcoming`；审批创建球队失败后 application 仍为 `pending` 且无对应 team。
- 完整 Maven 回归测试 29 项通过，0 失败、0 错误、0 跳过；原阶段 3 的 24 项全部保留并通过，新增 4 个真实 MySQL/HTTP 回滚测试及 1 个赛果逻辑删除恢复 HTTP 集成测试。数据库集成测试没有用 mock 代替真实回滚。
- 临时 schema 在最终测试后删除。业务库只读复核精确计数：user 3、team 4、player 8、match 4、match_result 2、goal_record 5；没有对 `football_admin` 执行测试写入、DDL 或删除。

### 剩余风险与规则边界

- DB trigger 注入异常对应 HTTP 500（Spring 默认未处理持久层异常响应）；目前没有统一的全局异常响应体，本阶段未扩大异常映射范围，避免意外改变已有 API 语义。
- `randomArrangeMatches`、`saveTeamLineup` 等其他已有事务方法仍有若干 Service 写方法返回布尔值未逐一检查，通知异常仍按现有实现忽略；本阶段没有把它们声明为已验证原子工作流，需后续为各业务路径补充可控测试后再决定修复。
- 球员加入球队时多项成员/用户/球员写入的完整回滚行为尚未做独立失败注入验证。
- 比赛分页参数、比分非负、球员所属球队、零进球和禁用球员射手榜语义仍待产品/业务确认，本阶段未改变这些业务规则。


## 阶段 5：剩余事务路径与统一异常响应（2026-10-17）

### 审计结论

- 排赛：`ApplicationController.randomArrangeMatches` → `ApplicationServiceImpl.randomArrangeMatches`（公开 Service 方法、`@Transactional(rollbackFor=Exception.class)`）→ `match` 与 `application(event_match)`。原流程先清旧比赛/映射，再逐对写比赛与映射；此前没有检查所有布尔写入结果，映射保存失败可能留下比赛。本轮已对删除、清理和新增写入结果失败进行异常传播。旧映射到比赛的关系没有外键，孤儿映射仍可能存在；不改变排赛规则。
- 阵容：`ApplicationController.saveTeamLineup` → `ApplicationServiceImpl.saveTeamLineup`（公开事务方法）→ 删除旧 `application(match_lineup)` 并写新阵容；此前写入结果被忽略。本轮对旧阵容清理和新记录保存失败抛出异常，使旧阵容与新阵容替换原子化。无效/非本队成员仍按既有逻辑跳过；通知仍是 catch 后忽略的 best-effort 副作用。
- 球队解散：`TeamController.dissolve`（Controller 公开事务方法，外部 HTTP 进入 Spring 代理）→ `team_member`、`user.role`、`player.team_id`、`notification`、逻辑删除 `team`；之前成员/球员/球队更新失败有路径正常返回错误 envelope，导致此前写入提交。本轮失败改为抛异常，保证事务回滚；通知异常仍向上抛并回滚，与当前事务行为一致。
- 入队审批：`ApplicationController.updateStatus` → `ApplicationServiceImpl.updateStatus` → 成员行、用户角色、球员档案、申请状态。之前关联记录不存在可能被静默跳过，写入布尔值也未全部检查。本轮将缺失关联引用作为 404/400 失败并检查成员、角色、球员、申请状态写入；最外层事务覆盖成功/失败状态。
- 直接成员增删：`TeamMemberServiceImpl.addMember/removeMember` 增加事务，覆盖 `team_member` 与 `user.role` 同步。数据库 `(team_id,user_id)` 唯一键也覆盖禁用行；重复激活现在幂等，已停用行复用并激活，不再因唯一键冲突而无法重新加入。

全部表均为 InnoDB；项目只有一个 DataSource，MyBatis 与 JdbcTemplate 同源。相关事务均由 Controller/其他 Bean 调用公开 Service/Controller Bean 方法，不依赖同类自调用。阶段 5 测试以 `SELECT DATABASE()` 精确守卫 `football_admin_phase5_tx_20261017`。

### 异常响应约定与兼容性

新增 `ResourceNotFoundException` 与 `ApiExceptionHandler`。保留既有 JSON `ResultVO {code,msg,data}` 包络，不统一改写正常业务响应。针对异常路径使用实际 HTTP 状态：参数/绑定错误 400、未认证 401、无权限 403、资源不存在 404、完整性约束冲突 409、不支持方法 405、未预期服务端异常 500。未预期异常客户端只见通用“服务器内部错误”；服务端日志记录 HTTP method、path、异常类型与异常堆栈，不记录请求体、凭证 Header 或 Token。Security FilterChain 的 EntryPoint/DeniedHandler 仍由过滤链返回 401/403，并新增响应 envelope 断言。登录错误消息改为固定文案；`GoalRecordController` 移除异常吞掉及 `printStackTrace/System.out`，让全局异常处理器产生 500。前端 Axios 在 HTTP 错误时读取 envelope 的 `msg`，401 会话处理保留原逻辑。

状态码变化仅发生于此前显式抛出的参数、缺失资源、重复约束和未预期异常路径；这类响应由 HTTP 非 2xx 拒绝，前端已显示响应 `msg`。其他既有 `ResultVO.error(...)` 仍有部分返回 HTTP 200，本阶段没有全局重写所有 API 契约。

### 故障注入与回归结果

隔离测试 schema 从 `init.sql` 创建，13 表、全部 InnoDB。MySQL 26.7.0；JDK 8.0.504；Spring Boot 随机 HTTP 端口。数据库异常由临时 `BEFORE INSERT/UPDATE` trigger 使用 SQLSTATE 45000 生成；API 日志记录的 Spring 外层类型是 `org.springframework.jdbc.UncategorizedSQLException`，MySQL 根因是 `java.sql.SQLException`。原阶段 4 的事务测试守卫在显式 Phase 5 属性下切换到本阶段 schema，仍拒绝任何其他 schema。

| 路径/场景 | 注入/请求 | HTTP | 最终数据库断言 | 结果 |
|---|---|---:|---|---|
| 赛事排赛 | 插入 event_match 触发器失败 | 500 | 新赛程 match=0，event_match 映射=0 | PASS |
| 阵容覆盖 | 新阵容插入失败 | 500 | 原阵容仍 1 行且未被替换 | PASS |
| 球队解散 | 逻辑删除 team 更新失败 | 500 | team 未删除、成员 status=1、role/player 关系恢复、通知数量不变 | PASS |
| 入队审批 | player 插入失败 | 500 | application 仍 pending、成员=0、角色不变、无球员档案 | PASS |
| 成员重复加入 | 重复 POST、删除后再次 POST | 200 | 唯一关系始终 1 行，停用后复用并恢复 status=1 | PASS |
| 参数类型错误 | 比赛列表 page 非整数 | 400 | ResultVO code=400，无异常名泄露 | PASS |
| 资源不存在 | 查询不存在比赛 | 404 | ResultVO code=404 | PASS |
| 重复赛果 | 重复 match_id INSERT | 409 | ResultVO code=409，不含 MySQL duplicate/索引详情 | PASS |
| 未认证/越权 | Security 集成 HTTP 请求 | 401 / 403 | JSON code 分别为 401 / 403 | PASS |
| 未预期 DB 异常 | trigger 故障注入 | 500 | 通用错误 envelope，不含 SQLSTATE/异常类/触发器消息 | PASS |

完整 Maven 测试 38 项通过、0 失败、0 错误、0 跳过（原 29 项保留，阶段 5 新增 9 项）。命令、输出和 Surefire 原始报告见 `phase5-evidence/2026-10-17/MAVEN_PHASE5_FINAL_RESULTS.txt` 与 `backend/target/surefire-reports/`。第一次运行中有测试 schema 守卫名称不匹配及夹具重复查询两次测试错误；没有业务断言失败。守卫按显式 Phase 5 目标修正、夹具修正后，最终从干净测试 schema 全量通过。失败日志保留为审计轨迹，不计入最终通过结果。

### 数据隔离与剩余风险

测试 schema 清理前以精确 schema 名确认目标，最终清理脚本仅能删除 `football_admin_phase5_tx_20261017`。真实库 `football_admin` 只读保存精确核心表行数和 `match`、`match_result`、`goal_record`、`team_member` 索引元数据到 `phase5-evidence/2026-10-17/business_db_before.txt` 与 `business_db_after.txt`；对比未变化。真实库未执行测试 DML、DDL 或删除。临时 schema 清理结果及触发器/夹具数见 `final_verification.txt`。

- `application` 中旧排赛映射没有数据库外键；删除失败可回滚，但历史孤儿映射清理策略尚未调整。
- 阵容写入跳过非法 userId/非本队成员的业务语义保持现状，是否应改成整请求拒绝待产品确认。
- 通知目前仍是 best-effort（阵容）或与解散事务联动（解散），不同业务处理方式来自既有实现；是否统一需要业务决定。
- `IllegalArgumentException` 映射 400；部分旧接口仍通过 HTTP 200 + `ResultVO.error` 表达业务错误，本轮有意保留兼容性。
- 事务失败返回 HTTP 500 是服务端存储/未知故障语义；详细堆栈仅服务端日志可见。没有独立 log sink 或日志字段脱敏集成验证。
- JWT 401/403 过滤链行为由已有集成测试确认；未认证密码正确登录仍受真实 admin 密码未知这一历史限制，不在本阶段修改。

修改前快照清单 `snapshot_before_sha256.csv` 与修改后清单 `snapshot_after_sha256.csv` 位于本目录；校验结果见 `snapshot_verification.txt`。阶段 2、3、4 证据目录及阶段 4 原始事务实验文件保持原样。
# 最终收尾审计（2026-10-17）

本节是阶段 0–5 的状态汇总；各阶段原始证据和历史快照均保留，不覆盖历史结果。

## 当前验证结论

- 阶段 4 当时 Maven 全量测试为 29/29；阶段 5 在原 29 项基础上新增 9 项后，最近一次完整 Maven 测试为 **38/38 通过、0 失败、0 错误、0 跳过**。原始输出：`phase5-evidence/2026-10-17/MAVEN_PHASE5_FINAL_RESULTS.txt`；阶段 4 历史结果仍见 `phase4-evidence/2026-10-17/TRANSACTION_EXPERIMENT.md`。
- 本次收尾执行 `mvn -f backend/pom.xml -DskipTests test-compile`，Java 8 下 BUILD SUCCESS；编译 76 个主源文件及 7 个测试源文件。存在已记录的 deprecated/unchecked 编译提示，不是编译失败。
- 阶段 1 安全集成测试覆盖匿名 401、越权 403 与授权路径；阶段 5 再次验证 Security FilterChain 错误响应不会被 MVC 全局处理器改写为 500。配置使用鉴权后兜底，未发现 `anyRequest().permitAll()`。
- 阶段 2 的 EXPLAIN/隔离合成数据结果只代表隔离实验条件；`match_result.match_id` 重复索引候选经独立 schema 验证，真实业务库未修改。阶段 3 的数据一致性/权限测试及阶段 4、5 的 MySQL 故障注入均有各自证据；阶段 5 四条多表事务链路以 HTTP 故障请求后查询隔离库最终状态证明回滚。
- 本次收尾没有连接或写入真实业务数据库，没有执行 DDL、删除、压测或重跑带故障注入的真实数据库集成测试。阶段 5 报告中的业务库前后只读行数/索引核验是该阶段历史证据，不是本次新增测量。

## 尚存高风险与边界

1. **备份恢复仍不适合视为生产安全。** `PerformanceServiceImpl.restoreBackup` 的实现对业务表使用 `TRUNCATE`；MySQL 的隐式提交使外层 Spring 事务不能提供整库原子回滚保证，故中途失败可能留下部分恢复状态。动态表/列名也需白名单约束。该破坏性路径本阶段未执行或验证，生产环境应先禁用/限制，之后在隔离库重做安全设计与故障注入。
2. **部署配置必须提供独立凭据。** `application.yml` 不再包含数据库密码或 JWT 默认回退值；空用户表初始化管理员必须通过 `INITIAL_ADMIN_PASSWORD` 环境变量提供口令。报告不记录任何秘密值。
3. 登录兼容路径仍允许旧明文密码验证并在成功后升级 BCrypt；需在数据迁移和用户影响评估后制定淘汰计划。
4. JWT 当前无服务端撤销/即时失效机制；用户禁用或角色变更后，已签发 token 在过期前的行为需要明确策略。不能据此宣称具备 token 黑名单或即时注销。
5. `ApplicationServiceImpl` 对缺失/非法比赛时间存在回退到当前时间的逻辑；部分解析失败可得到 null。是否拒绝输入属于业务规则待确认，存在生成错误赛程元数据的风险。
6. 通知部分采用 best-effort；部分旧接口仍以 HTTP 200 + `ResultVO.error` 表达业务错误。赛事映射外键、阵容非法成员策略、分页/非负比分约束、射手榜零进球及禁用球员语义、积分榜完全同分 tie-break 仍未统一定义或覆盖。
7. 阶段 5 的测试证明指定隔离 schema 和代码路径行为，不等同生产部署验证。生产日志采集/脱敏、真实部署、Redis、Docker、JMeter/并发性能、CPU/JVM 资源表现均未验证或未实施；不得声称相关成果。

## 最终判断

按求职展示与本机可复现演示范围，现有代码、自动化测试和阶段证据足以支撑已验证的安全、查询实验、业务一致性及事务回滚讲解。此判断不等同于生产就绪；备份恢复和部署凭据是上线前阻断项。性能压测及其余边界测试作为后续增强项。
