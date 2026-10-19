# 阶段 3 测试运行说明

测试日期：2026-10-17。MySQL 集成测试需显式开启，并且必须把应用连接指向一次性测试 schema。

## 安全运行步骤

在 PowerShell 当前进程安全设置 `DB_PASSWORD`（不要将密码写入命令历史、报告或提交），然后：

```powershell
.\phase3-evidence\2026-10-17\setup_phase3_schema.ps1
$env:SPRING_DATASOURCE_URL = 'jdbc:mysql://localhost:3306/football_admin_phase3_it_20261017?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf-8&allowPublicKeyRetrieval=true'
$env:JAVA_HOME = '<your JDK 8 path>'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn -f backend/pom.xml '-Dphase3.db.integration=true' test -q
```

MySQL 测试类使用 `@Transactional`，进球插入用例在断言后回滚。HTTP 集成测试只读公开数据/用户数据，或在安全过滤器/参数校验阶段拒绝写请求。执行后检查命令退出码和 Surefire 报告，再运行：

```powershell
.\phase3-evidence\2026-10-17\cleanup_phase3_schema.ps1
```

清理脚本只会删除精确名称为 `football_admin_phase3_it_20261017` 且确认存在一次的 schema。不要对 `football_admin` 使用此脚本。

仅运行 Mockito 单元测试可使用：

```powershell
mvn -f backend/pom.xml '-Dtest=MatchControllerTest,GoalRecordServiceTest,UserServiceLoginTest' test -q
```

没有 `-Dphase3.db.integration=true` 时，`FootballDatabaseIntegrationTest` 会跳过；现有 `SecurityIntegrationTest` 仍按 `application.yml` 的 JDBC 设置启动，因此运行全量测试时应显式覆写到隔离 schema。
