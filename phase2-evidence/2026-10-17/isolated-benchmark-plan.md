# 隔离 SQL 基准数据方案（未执行）

状态：方案已形成，尚未创建 schema、生成数据或测量。原因：真实业务库只有个位数样本；当前阶段没有证据支持生产 DDL，也不应把生成数据写入 `football_admin`。

## 隔离边界

- 使用全新、唯一命名的 schema，例如 `football_admin_bench_20261017_a1`，不使用 `football_admin`，不从业务库复制用户/业务数据。
- DDL 依据 `backend/src/main/resources/db/init.sql` 中 `team`、`player`、`match`、`match_result`、`goal_record` 五张表，保留真实字段类型、外键及基线索引。
- 建库前先通过 `information_schema.schemata` 确认目标名不存在；只允许 `CREATE DATABASE` 新名字及在该 schema 内建表/插入合成行。脚本不含 DROP/TRUNCATE/DELETE，也不修改真实 schema。
- 合成记录使用固定随机种子或确定性公式，固定逻辑删除比例、比赛状态、球队分布和进球分布；保留 seed、生成 SQL/脚本版本、各表精确行数。

## 建议规模与顺序

1. 创建 128 支合成球队。
2. 创建 12,800 名合成球员，均匀分配球队，并加入固定比例 `deleted=1` 行。
3. 创建 20,000 场比赛，交替主客队，按固定时间序列，固定分布 `finished/ended/upcoming` 与软删除标记。
4. 为已结束比赛生成唯一赛果，保持 `match_result.match_id` 唯一。
5. 生成约 100,000 条进球记录，球员和比赛关联有效，设置固定软删除比例。

该规模是后续基准提案，不是当前项目实际用户规模或实际数据量。生成前先确认本地磁盘、MySQL 可用空间和测试窗口；资源不足则缩小规模并记录。

## 对照方法

- 查询使用 `DATABASE_EXPLAIN_BASELINE.md` 中 Mapper 原始 SQL，不改语义。比赛列表固定首/深分页参数及有/无 status 两种条件；射手榜、积分榜分别校验结果行及排序一致。
- 保留基线索引副本；候选变体仅在隔离 schema 上测试：`match(deleted, match_time)`、`match(deleted, status, match_time)`、`goal_record(deleted, player_id)`。每次只改变一个候选，先执行 ANALYZE TABLE，再记录传统 EXPLAIN、EXPLAIN ANALYZE、rows/loops、索引选择和查询结果校验。
- `match_result(match_id)` 上现存的 `idx_match_id` 与 `uk_match_id` 重复，仅在隔离对照中比较保留/不保留普通索引的读取和批量写入；不得直接从真实 schema 删除。
- 每种方案固定机器、JDK/驱动（若测 API）、数据、参数；预热后至少重复多轮并报告中位数/P95（若测量样本足够），同时单独记录写入耗时与错误数。数据库直连 SQL 时间不得写成 HTTP API 响应时间。
- 若只分析 SQL，不做 API 压测，则不报告吞吐量、并发、P95/P99、CPU/内存或 Redis 命中率。

## 当前不做的事项

不创建 schema、不写入任何数据、不添加/删除真实索引、不修改 Mapper SQL、不声称性能改善。只有在隔离对照显示稳定收益且结果一致后，再提出真实 schema 迁移；真实库迁移应另行备份并审查回滚步骤。
