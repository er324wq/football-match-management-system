# 核心查询 EXPLAIN 基线

执行日期：2026-10-17
环境：本机 MySQL `football_admin`，表结构与样例行数见 `TECHNICAL_AUDIT.md`。执行 `EXPLAIN` 是只读操作。本服务器输出 TREE 计划。计划成本/rows 是优化器估算，不是接口耗时或实际扫描行数。

## 比赛列表

SQL 来源：`backend/src/main/java/com/football/mapper/MatchMapper.java` 的 `selectPageWithDetails`（status 条件为空时）及 MyBatis 分页插件。

查询形态：

```sql
SELECT m.*, ht.name AS home_team_name, at.name AS away_team_name,
       mr.home_score, mr.away_score, mr.referee
FROM `match` m
LEFT JOIN team ht ON m.home_team_id = ht.id
LEFT JOIN team at ON m.away_team_id = at.id
LEFT JOIN match_result mr ON m.id = mr.match_id
WHERE m.deleted = 0
ORDER BY m.match_time DESC
LIMIT 0, 10;
```

计划摘要：`match` 估算 4 行 table scan，按 match_time 排序；home/away team 各使用 PRIMARY 单行 lookup；match_result 使用唯一索引 `uk_match_id` 单行 lookup；最后 limit 10。

## 射手榜

SQL 来源：`backend/src/main/java/com/football/mapper/PlayerMapper.java` 的 `selectTopScorers`。

```sql
SELECT p.id AS player_id, p.name AS player_name, t.name AS team_name,
       COUNT(g.id) AS goal_count
FROM player p
LEFT JOIN goal_record g ON p.id = g.player_id
LEFT JOIN team t ON p.team_id = t.id
WHERE p.deleted = 0 AND g.deleted = 0
GROUP BY p.id, p.name, t.name
ORDER BY goal_count DESC;
```

计划摘要：goal_record 估算 5 行 table scan，再按 player 主键 lookup；team 按主键 lookup；聚合使用 temporary table，之后按 goal_count 排序。注意 `WHERE g.deleted=0` 使没有进球记录的球员无法通过该过滤条件，虽然 SQL 写了 LEFT JOIN；这是结果语义待核对项，修改前需确认榜单是否应显示零进球球员。

## 球队列表

SQL 来源：Controller 使用 MyBatis-Plus 分页和逻辑删除条件。

```sql
SELECT id, name, coach, city, description, status, deleted, create_time, update_time
FROM team
WHERE deleted = 0
ORDER BY create_time DESC
LIMIT 0, 10;
```

计划摘要：team 估算 4 行 table scan 后按 create_time 排序。

## 球员列表

SQL 来源：球员列表 Controller + MyBatis-Plus 分页和逻辑删除条件。

```sql
SELECT id, name, age, position, jersey_number, team_id, status, deleted, create_time, update_time
FROM player
WHERE deleted = 0 AND status = 1
LIMIT 0, 10;
```

计划摘要：player 估算 8 行 table scan 后过滤 status 与 deleted。

## 积分榜

SQL 来源：`MatchMapper.selectStandings()`。对符合 finished/ended 状态的比赛分别生成主队、客队记录，`UNION ALL` 后分组聚合胜平负、进失球和积分，再排序。

计划摘要：match_result 每个分支估算 2 行 table scan；match 用 PRIMARY 查找；team 主队/客队分别用 PRIMARY 查找；UNION ALL 物化后，对临时结果聚合和排序。

## 阶段 2 复核（2026-10-17）

**调用链**

| SQL | Controller | Service | Mapper |
|---|---|---|---|
| 比赛分页列表 | `MatchController.list` (`GET /api/match/list`) | `MatchServiceImpl.listWithDetails(page,size,status)` | `MatchMapper.selectPageWithDetails`；MyBatis-Plus PaginationInnerInterceptor 追加分页 SQL |
| 射手榜 | `StatisticsController.getTopScorers` (`GET /api/statistics/top-scorers`) | `PlayerServiceImpl.getTopScorers` | `PlayerMapper.selectTopScorers` |
| 积分榜 | `StatisticsController.getStandings` (`GET /api/statistics/standings`) | `MatchServiceImpl.getStandings` | `MatchMapper.selectStandings` |

**数据库状态。** 只读精确计数：`match=4`、`match_result=2`、`goal_record=5`、`player=8`、`team=4`。当前数据不足以代表大表工作负载。MySQL 返回 `VERSION()=26.7.0`、`version_comment=MySQL Community Server - GPL`；连接目标为 `football_admin`。

**执行计划解读。** 完整 SQL 与传统 EXPLAIN、TREE EXPLAIN ANALYZE 原始结果见 `phase2-evidence/2026-10-17/explain_queries.sql` 与 `phase2-evidence/2026-10-17/explain_output.txt`。

1. **比赛列表：** `match` 访问类型 `ALL`，估算 4 行，`Using where; Using filesort`。`idx_match_time` 存在，但计划选择小表扫描后排序；4 行上扫描后排序的代价极低，不能据此判为慢。两张 team 别名都是 `eq_ref`/PRIMARY 单行查找；`match_result` 是 `eq_ref`/`uk_match_id` 单行查找。当前 `possible_keys` 中赛果同时有 `uk_match_id` 与 `idx_match_id`，两者列相同，普通索引是唯一索引的重复索引候选；是否清理需在隔离库验证收益和写入影响，本轮不改现有库。
2. **射手榜：** 优化器从 `goal_record` 以 `ALL` 访问估算 5 行，`possible_keys=idx_player_id`，但 `key=NULL`；因为驱动侧需要筛选 `g.deleted=0`，且表只有 5 行，扫描较便宜。之后对 player 和 team 使用 `eq_ref`/PRIMARY。聚合使用 temporary，按计算出的 `goal_count` 排序使用 filesort；排序键是聚合值，常规单表索引不能直接消除该聚合排序。SQL 中 `WHERE g.deleted=0` 会把没有匹配 goal_record 的球员过滤掉，语义上等价于不保留零进球球员；产品是否希望列出零进球球员仍待确认，未改语义。
3. **积分榜：** 两个 UNION 分支各扫描 `match_result`（每支估算 2 行），随后按 `match_id` 对 match 做 `eq_ref`/PRIMARY 查找并过滤 `status` 和 `deleted`；team 通过 PRIMARY `eq_ref`。UNION 派生表估算 4 行，`Using temporary; Using filesort` 用于聚合/按积分、净胜球、进球数排序。全量聚合与临时表是该查询逻辑的自然成本；小样本下扫描 2 行赛果显然不构成索引缺失证据。`idx_status` 虽在 possible_keys 中，但当前连接顺序从仅 2 行 match_result 驱动，直接主键查 match 更合理。

**单次 EXPLAIN ANALYZE 诊断值（不是性能基准）。** 本次各运行一次，Tree 顶层实际耗时显示：比赛列表约 `0.11 ms`、射手榜约 `0.377 ms`、积分榜约 `0.50 ms`。前一次同一比赛查询约 `0.75 ms`，体现单次计时受缓存/调度/客户端环境扰动；这些数值不具有可比性，也不能当作 API 响应时间、稳定数据库耗时、P95/P99 或优化成果。计划中的 actual rows/loops 是本次执行观察值，不外推至其他数据规模。

**优化决定。** 本阶段不改业务 SQL、不加/删现有数据库索引、不改分页：目前没有足够数据证明候选改动会带来收益，删重复索引也需测量写入成本并以迁移方式实施。保留测试样本生成设计在 `phase2-evidence/2026-10-17/isolated-benchmark-plan.md`。本次没有对 `football_admin` 执行 DDL/DML。

## 隔离索引与核心查询实验（2026-10-17）

后续验证在临时 schema `football_admin_bench_20261017_a1` 完成，规模为 team 128、player 12,800、match 20,000、match_result 13,000、goal_record 100,000。该 schema 已删除并确认不存在；合成数据生成器、实际 SQL/EXPLAIN 输出、20 次数据库侧采样、HTTP 样本、写入对照、清理验证与实验解释见 `phase2-evidence/2026-10-17-isolated-run/EXPERIMENT_REPORT.md`。

真实 `match_result` 的两个 `match_id` BTREE 确认是相同单列索引：唯一 `uk_match_id` 与普通 `idx_match_id`。隔离库移除普通索引后外键保持有效、计划仍使用唯一键、三条查询结果 hash 相同；10 轮 13,000 行批量写入对照的中位数为重复索引 303.914ms、仅唯一索引 278.499ms。该证据支持建议未来以可回滚迁移删除普通重复索引，真实库本轮未改。

其他候选仅在合成样本上验证：`goal_record(deleted,player_id)` 在射手榜查询形成覆盖索引，数据库 `EXPLAIN ANALYZE` 候选 A-B-A 中位数约 143.5/136.5ms，前后基线约 329/353.5ms；HTTP 射手榜 API 相应为 158.332ms，撤销后 367.564ms（20 次、单用户）。这是隔离合成数据的候选结果，不直接推广到生产。比赛 `(deleted,match_time)` 候选没有稳定收益，status/standings 候选未被优化器采用。

所有 HTTP API 测量与 EXPLAIN ANALYZE 分开统计；API 是单机 loopback 顺序请求完整响应时间，不是纯 SQL 时间。无并发压测、CPU/JVM/DB 资源观测，也没有生产数据上的前后对比。实验结束后 `football_admin` 的核心表精确行数和原索引与实验前一致；阶段 1 测试 11/11 通过。
