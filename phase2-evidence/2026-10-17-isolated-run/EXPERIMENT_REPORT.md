# 隔离数据库索引实验报告

日期：2026-10-17。真实业务库 `football_admin` 全程未执行 DDL/DML。实验在临时 schema `football_admin_bench_20261017_a1` 中执行，结束后已删除并验证 schema 不存在。

## 环境与可恢复性

- MySQL `VERSION()=26.7.0`，`version_comment=MySQL Community Server - GPL`；账号 `root@localhost` 已有全局 CREATE/DROP/ALTER/INDEX 权限，本轮没有变更权限。
- 实验前保存 `before/` 中的 SQL 报告、Mapper、Controller、DDL 文件；`SHA256_manifest.csv` 记录源文件和快照 SHA-256。所有实验脚本、原始输出、样本 CSV 均留在本目录；目录内 `artifact_checksums.csv` 为完成后的证据校验清单。
- 实验 schema 预先确认不存在；生成 SQL 用不带 `IF NOT EXISTS` 的 `CREATE DATABASE`，避免误用旧库。最终只删除本轮创建的准确 schema 名称。清理证据见 `cleanup_verification.txt`。

## 实际索引定义与依赖

真实 `football_admin.match_result` 的 `SHOW CREATE TABLE` 显示：

- `UNIQUE KEY uk_match_id (match_id)`；
- `KEY idx_match_id (match_id)`；
- 外键 `match_result_ibfk_1 (match_id) REFERENCES match(id) ON DELETE CASCADE`；
- 两个索引都是单列、全长、升序 BTREE，列定义相同。唯一键同时提供唯一性和该 FK 子列所需索引。

应用写入路径在 `MatchController.saveMatchResult()` 中先按 `match_id` 查找，之后插入或更新；恢复到 upcoming 时 `deleteMatchResult()` 按同列删除。比赛列表、积分榜也按 `match_id` 关联结果。基线 EXPLAIN 选择 `uk_match_id`，不是普通 `idx_match_id`。

在隔离 schema 删除 `idx_match_id` 后，外键仍保留、查找继续使用 `uk_match_id`；添加回索引也成功。结果散列在删除/恢复普通索引后相同。真实数据库结构未修改。

## 合成数据与生成方式

`generate_benchmark_setup.ps1` 确定性生成 `setup_and_synthetic_data.sql`，按 `init.sql` 的核心字段与基线索引建立 InnoDB 表；行值由序号公式产生，不包含真实用户或业务数据。执行前 `ANALYZE TABLE` 成功。精确行数：

| 表 | 行数 |
|---|---:|
| team | 128 |
| player | 12,800 |
| match | 20,000 |
| match_result | 13,000 |
| goal_record | 100,000 |

球员、比赛、赛果、进球均使用确定性状态/软删除分布；比赛状态为 finished 12,000、ended 1,000、in_progress 3,000、upcoming 4,000；软删除比例按每 20 行一条生成。该分布只为隔离实验设定，不代表线上数据特征。

## 数据库侧查询实验

使用 Mapper 对应查询（完整 SQL 及生成器在本目录和上一级 `phase2-evidence/2026-10-17/`），在基线状态预热 3 次、测量 20 次；候选索引一次只加一个，之后删除并 `ANALYZE TABLE` 恢复基线。时间来自 `EXPLAIN ANALYZE` 根节点实际执行时间，单位 ms；是数据库端、带迭代器分析开销的观察值，不是客户端 SQL 总耗时或 HTTP 响应。

| 查询/状态 | EXPLAIN 与索引行为 | Median / P95（ms） |
|---|---|---:|
| 比赛分页，基线 | `idx_match_time` backward index scan，10 行 LIMIT | 0.0955 / 0.140 |
| 比赛分页，候选 `(deleted,match_time)` | 改走候选索引；估算约 9,732 行，执行端仍只返回 10 行；本组时间更高 | 0.261 / 0.344 |
| 比赛分页，恢复基线 | 计划回到 `idx_match_time` | 0.0824 / 0.113 |
| status 分页，基线 | 仍按 `idx_match_time` 反向扫描并过滤 status | 10.55 / 11.70 |
| status 分页，候选 `(deleted,status,match_time)` | 优化器仍选择 `idx_match_time`，候选索引未被选中 | 10.15 / 11.60 |
| 射手榜，基线（带重复赛果索引） | player scan + `goal_record.idx_player_id`，聚合临时表与排序 | 329 / 365 |
| 射手榜，候选 `(deleted,player_id)` 第一次 | goal_record 改为按 `(deleted,player_id)` 覆盖索引查找；结果行 SHA-256 不变 | 143.5 / 168 |
| 射手榜，候选复测 | 基线→候选→基线后再次加入候选的 A-B-A 后续 B 组 | 136.5 / 145 |
| 射手榜，恢复基线 | 基线访问计划恢复，结果行 SHA-256 不变 | 353.5 / 394 |
| 积分榜，基线 | match 全表扫描、两分支 UNION/临时聚合 | 153.5 / 170 |
| 积分榜，候选 match `(status,deleted)` | EXPLAIN 未采用该索引，读取方式基本未变 | 153 / 180 |

射手榜候选索引在本合成数据上表现稳定优于三个基线组（中位数 329、372.5、353.5ms）及两个候选组（143.5、136.5ms）；这是“候选实验”证据，不能推出生产收益，因为软删除比例、球员/进球分布及硬件负载是人工设定。**不建议据此直接为真实数据库加索引**，应先用更接近目标业务的数据分布再验证。

## 重复索引对照

真实索引定义确认 `uk_match_id` 与 `idx_match_id` 完全同列；在隔离库先有二者执行基线，再移除普通索引，保留唯一键与外键。删除前后比赛分页、射手榜、积分榜的规范化结果行数/ SHA-256 一致。计划仍以 `uk_match_id` 查找赛果。

核心 SQL 20 次采样的读查询中位数（基线有重复索引 → 只留唯一索引）分别为：比赛分页 `0.0955 → 0.0922 ms`；射手榜 `329 → 359 ms`；积分榜 `153.5 → 153 ms`。前后样本组存在环境波动，不能据此声称读取性能提升或退化。

写入对照使用两张字段、主键、唯一索引、外键相同的隔离 helper 表，唯一差异是重复表多一个普通 `match_id` BTREE。交替顺序各 10 次，每次从同一 13,000 行结果来源执行 `INSERT ... SELECT`，计时含提交、排除清表：

| 目标索引 | 样本 | 每次行数 | Median / P95（ms） | Min–Max（ms） |
|---|---:|---:|---:|---:|
| UNIQUE + 重复普通索引 | 10 | 13,000 | 303.914 / 341.917 | 279.091–341.917 |
| 仅 UNIQUE | 10 | 13,000 | 278.499 / 293.992 | 233.797–293.992 |

这组实验支持**建议在未来 schema/migration 清理普通重复索引**：其不参与当前选择计划，唯一索引满足访问与 FK 要求，且同数据写入对照的中位数/P95 较低。该写入结论只对本机合成批量写入场景成立；生产库上未删除索引、未运行 DDL，也未验证其他工作负载。建议后续采用显式、可回滚的迁移，而不是直接改生产库。

## Spring Boot HTTP API 验证

Spring Boot 2.7.18 实际启动于本机 `18091`，通过环境变量把 datasource 指向隔离 schema。公开接口使用 HttpClient 顺序请求：每个状态 3 次预热、20 次测量；时间为客户端读完整响应的 loopback wall-clock，包含 Controller/Service/JDBC/JSON 序列化和响应读取，不等同数据库侧 EXPLAIN ANALYZE。无并发；无 CPU/JVM/数据库资源采样。

| 状态 | 比赛列表 median/P95 ms | 射手榜 median/P95 ms | 积分榜 median/P95 ms | HTTP/业务错误 |
|---|---:|---:|---:|---:|
| 基线索引（重复索引在） | 9.247 / 10.893 | 355.341 / 393.024 | 139.016 / 152.227 | 0 |
| 射手榜候选索引在 | 7.884 / 8.460 | 158.332 / 195.658 | 137.486 / 151.897 | 0 |
| 射手榜候选撤销 | 7.624 / 8.308 | 367.564 / 428.952 | 151.488 / 177.628 | 0 |
| 比赛分页候选索引在 | 6.643 / 7.384 | 382.496 / 441.821 | 171.812 / 199.172 | 0 |
| 比赛分页候选撤销 | 6.482 / 6.879 | 363.604 / 420.527 | 135.405 / 142.961 | 0 |

每个 endpoint 的 HTTP 状态均为 200、`ResultVO.code=0`；比赛页响应 10 条、射手榜 12,160 条、积分榜 128 条。比赛候选 API 时间与恢复基线接近，不主张改善。射手榜 API 候选 A-B-A 与 SQL 计划测量方向一致，但只在本合成环境成立。API 分位数和数据库 EXPLAIN 分位数分别记录，不混用。

## Regression 与真实业务库保护

本轮后 `mvn -f backend/pom.xml test -q` 退出码 0：`SecurityIntegrationTest` 9/9、`UserServiceLoginTest` 2/2，失败/错误/跳过均为 0。真实库清理前复核 exact row counts 为 user 3、team 4、player 8、match 4、match_result 2、goal_record 5，且 `match_result` 两个原始索引仍存在，与实验前元数据一致。

**生产结论：** 可建议后续迁移移除 `match_result.idx_match_id`；本轮没有更改 `init.sql`、Mapper 或真实库结构。射手榜复合索引仅作为隔离测试候选；其余候选不建议增加。性能数字只描述本次固定合成数据与本机测试条件，不代表真实用户规模、线上负载或生产性能。
