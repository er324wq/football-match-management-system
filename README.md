# 足球比赛管理系统

一个基于 Spring Boot 与 Vue 的前后端分离足球比赛管理系统，作为本科毕业设计与求职项目展示。项目包含管理后台、赛事相关业务页面、JWT 认证授权、数据统计，以及本地/隔离环境下的测试与技术审计记录。

## 技术栈

| 层级 | 主要技术 |
|---|---|
| 后端 | Java 8、Spring Boot 2.7.18、Spring Security、MyBatis-Plus 3.5.3.1、MySQL 8 |
| 认证 | Auth0 `java-jwt`、BCrypt、无状态 JWT 认证 |
| 前端 | Vue 2.6、Element UI 2.15、Vuex、Vue Router、Axios |
| 测试 | JUnit、Spring Boot Test、MockMvc/TestRestTemplate、隔离 MySQL schema |

## 已实现模块

- 账号与角色管理、登录认证及后端角色授权
- 球队、球员和球队成员管理
- 比赛赛程、比赛申请与审批、比赛结果管理
- 进球记录、比赛评分及个人/球队表现
- 通知、公告、仪表盘
- 积分榜、射手榜等赛事统计
- REST API 输入校验与统一异常响应

## 项目结构

```text
backend/                 Spring Boot 后端、SQL 初始化脚本和测试
frontend/                Vue 前端源码及构建配置
phase2-evidence/         SQL/索引实验报告和证据
phase3-evidence/         认证授权及业务回归测试证据
phase4-evidence/         事务验证证据
phase5-evidence/         事务与 API 异常验证证据
TECHNICAL_AUDIT.md       技术审计
TECHNICAL_RESULTS.md     技术成果和验证范围
RESUME_PROJECT_RESULTS.md 求职项目说明与技术问答
JWT_SECURITY_NOTES.md    JWT 实现笔记
database_schema.md       数据库结构说明
database_er.md           数据库 ER 说明
```

阶段证据保留报告、测试结果和实验数据；历史快照目录 `before/`、`after/` 不纳入公开提交，以避免重复存放整份源代码和旧配置。个人运行日志、数据库备份、IDE 文件及构建输出也不会提交。

## 本地运行

### 环境要求

- JDK 8
- Maven 3.6+
- MySQL 8
- Node.js 与 npm（建议使用与项目依赖兼容的版本）

### 数据库

创建本地数据库 `football_admin`，按需执行 `backend/src/main/resources/db/init.sql`。SQL 只写入无公开可用口令的裁判和普通用户测试种子；管理员账号由应用首次启动时通过 `INITIAL_ADMIN_PASSWORD` 初始化。

自动化测试的 `backend/src/test/resources/application.properties` 使用 Spring Boot `${random.uuid}` 为测试上下文临时生成管理员初始化口令和 JWT secret；这些值不会写入项目源码，也不会覆盖已有管理员。

### 设置后端环境变量

Spring Boot 不会自动读取 `.env` 文件。`.env.example` 仅列出变量名和占位符；请在启动进程的 shell 中设置真实本地值，不要提交 `.env` 或真实凭据。

PowerShell 示例（替换尖括号中的占位内容）：

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "<your-local-mysql-password>"
$env:JWT_SECRET = "<random-secret-at-least-32-bytes>"
$env:INITIAL_ADMIN_PASSWORD = "<strong-one-time-admin-password>"
$env:JWT_EXPIRATION = "86400000"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:8085"
```

之后在项目目录启动后端：

```powershell
cd backend
mvn spring-boot:run
```

后端默认监听 `http://localhost:8090`。数据库地址、端口和库名仍按 `application.yml` 中的本地开发配置；数据库用户名、密码及 JWT 密钥通过环境变量提供。若数据库中没有 `admin` 账号，应用会使用 `INITIAL_ADMIN_PASSWORD` 创建一次；已有管理员账号不会被覆盖。

### 启动前端

```powershell
cd frontend
npm install
npm run dev
```

前端默认运行于 `http://localhost:8085`，开发代理将 `/api` 请求转发到后端 `http://localhost:8090`。

## 测试与验证范围

阶段 5 归档的完整 Maven 测试结果为 **38/38 通过，0 失败、0 错误、0 跳过**。本次管理员凭据整改后，相关 `UserServiceLoginTest` 为 **5/5 通过**；本次只运行该定向单元测试，没有重跑全量集成套件。验证使用本机和隔离 MySQL schema；结果摘要见 `phase5-evidence/2026-10-17/PUBLIC_TEST_RESULTS.md`。

SQL/索引实验使用隔离 schema 与合成数据。项目未据此声称生产环境性能提升，也未完成生产部署验证、生产数据压测或生产环境安全认证。详细范围与限制见 `TECHNICAL_RESULTS.md`、`TECHNICAL_AUDIT.md` 和各阶段证据报告。

## 技术成果与工程实践

- **认证与授权：** JWT 签名和有效期校验、角色授权，以及匿名访问、越权访问的 HTTP 行为验证。
- **事务可靠性：** 使用故障注入验证指定赛事工作流失败时的事务回滚，并检查写入状态。
- **SQL 与索引实验：** 在隔离 schema 上比较 SQL 执行计划和候选索引，保留实验过程、数据与结论；实验不代表生产基准。
- **自动化回归：** 使用单元、Web 层和隔离数据库集成测试覆盖主要业务路径、异常状态及数据一致性。
- **审计与边界说明：** 保留技术审计、测试报告、阶段结果和待改进事项，不把未验证能力描述为已完成。

## 相关文档

- [技术审计](TECHNICAL_AUDIT.md)
- [技术结果与验证记录](TECHNICAL_RESULTS.md)
- [项目成果说明](RESUME_PROJECT_RESULTS.md)
- [JWT 与 Spring Security 笔记](JWT_SECURITY_NOTES.md)
- [数据库结构](database_schema.md)
