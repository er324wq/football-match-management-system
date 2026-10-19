# 快照与可恢复性说明

- 本工作区无 Git 元数据；阶段 3 修改前已复制 `GoalRecordServiceImpl.java`、`MatchController.java`、`SecurityIntegrationTest.java` 及三份技术报告到 `before/`，修改后版本保存在 `after/`。
- `GoalRecord.java` 和 `GoalRecordController.java` 的窄范围修改前快照是在修改后由当前文件反向去除本次新增的校验 import、注解和 `@Valid` 参数注解得到，标记为 `RECONSTRUCTED_FROM_NARROW_PATCH`。它们可辅助还原修改前语义，但不是原字节级备份。当前完整版本已逐字节复制到 `after/` 并记录 SHA-256。
- 新增测试文件没有修改前文件，状态为 `NEW`；其当前版本均在 `after/` 中。
- `snapshot_current_comparison.csv` 逐项记录本阶段目标文件的变化状态、快照来源和当前/修改后 SHA-256。`artifact_checksums.csv` 对本阶段证据文件、源文件和最终报告生成校验值。
