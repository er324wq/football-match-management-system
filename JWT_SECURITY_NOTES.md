# Spring Security + JWT 实现笔记

## 当前实现

- 登录入口：`POST /api/auth/login`。用户名通过 `UserMapper` 查询，密码使用 BCrypt 校验；仍能读取历史明文密码，首次成功后会升级为 BCrypt。
- 登录成功时 `JwtService` 使用配置中的 HMAC256 secret 生成 token。Claim 包含 `userId`、`username`、`role`、`iat`、`exp`，不包含密码。默认有效期为 24 小时，可用 `JWT_EXPIRATION` 配置。
- `JwtAuthenticationFilter` 从 `Authorization: Bearer <token>` 取 token，校验签名和过期时间，只接受 `admin`、`referee`、`user` 三种角色；之后建立 `Authentication`，写入 `SecurityContext`，并向 Controller 提供 `userId`、`username`、`role` 请求属性。
- Security 使用无状态会话。缺少凭证时，允许公开接口继续处理，其余接口由认证入口返回 401；签名、格式或过期错误由 JWT Filter 返回统一 JSON 401；已认证但角色不符返回 403。
- 浏览器把 token 保存在 Vuex/localStorage，Axios 请求加入 Bearer Header。401 时清除本地认证状态并转到登录页。登出只清除浏览器凭证，服务端没有 token 撤销表。
- `JWT_SECRET` 通过必需环境变量配置，不再提供仓库内默认密钥。部署时应设置独立的高熵 secret（至少 32 UTF-8 字节）。
- 数据库用户名和密码通过 `DB_USERNAME`、`DB_PASSWORD` 配置；密码没有仓库内默认值。`.env.example` 仅提供占位符，Spring Boot 不会自动加载 `.env` 文件。
- 若数据库中不存在管理员，应用必须通过 `INITIAL_ADMIN_PASSWORD` 环境变量完成首次初始化；缺少变量时启动会明确失败，不会创建固定口令账号。已有管理员不会被覆盖。

## 当前权限矩阵

| 角色 | 可访问范围 |
|---|---|
| 未登录访客 | 登录/注册；比赛、球队、球员、积分榜、射手榜等公开 GET 数据 |
| `user` | 公开数据；已认证的申请、通知、个人接口及其他默认受保护接口；不能访问管理后台 API |
| `referee` | `user` 权限；比赛结果、进球、牌记录和评分写操作 |
| `admin` | 用户与性能管理、球队/球员/球队成员管理、赛程管理、申请审核与编排，以及裁判可写的比赛记录 |

球队成员关系中的 `captain`/`member` 是 `team_member.role` 的局部值，不是 `user.role` 全局角色，当前未映射成 Spring Security authority。球队成员修改接口暂由管理员控制。

## 面试问题

### 1. 为什么需要 JWT？

当前前后端分离项目需要让后端识别后续请求的调用用户。JWT 登录后携带签名凭证，后端每次校验后建立认证上下文；本项目配置为无状态，不依赖服务端 HTTP Session。

### 2. JWT 的组成是什么？

JWT 由 Header、Payload、Signature 三部分组成。项目使用 Auth0 `java-jwt` 创建 HMAC256 签名，Payload 只放用户 ID、用户名、角色和签发/过期时间。签名不加密 Payload，因此不能放密码或敏感信息。

### 3. JWT 如何验证？

Filter 读取 Bearer token，`JwtService` 用同一 secret 构建的 verifier 校验 HMAC256 签名和 `exp`，再读取身份 Claim 并校验角色在允许列表中。任何验证失败都清理认证上下文并返回 401。

### 4. Filter 在哪里执行？

`JwtAuthenticationFilter` 是 Spring Security 中的 `OncePerRequestFilter`，插在 `UsernamePasswordAuthenticationFilter` 之前。它被加入 Security Filter Chain，并关闭 Servlet 容器的重复自动注册。

### 5. SecurityContext 是什么？

它是 Spring Security 当前请求的安全上下文，保存当前 `Authentication`。本项目 Filter 验证 token 后将认证对象写入其中，后续授权规则据此判断是否登录及其 authority。

### 6. Authentication 是什么？

它表示已认证主体及其权限。本项目主体含 userId 和 username，authorities 形如 `ROLE_admin`、`ROLE_referee`、`ROLE_user`，不存储密码。

### 7. 为什么不能只靠前端路由控制权限？

浏览器代码、localStorage 和路由守卫都由客户端控制，攻击者可以直接调用 API。前端守卫只改善页面导航；真实访问控制必须由服务端每次请求验证凭证并检查角色。

### 8. Spring Security 如何进行权限控制？

项目在 `SecurityConfig` 中使用 URL、HTTP 方法和 `hasRole`/`hasAnyRole` 配置规则。未明确放行或赋予角色的请求默认要求已认证。认证失败返回 401，角色不足返回 403。

### 9. JWT 过期怎么办？

Verifier 会拒绝已过期 token，后端返回 401，Axios 清除 token 和用户状态并导航到登录页，用户重新登录获取新 token。本项目尚未实现 refresh token。

### 10. JWT 被盗怎么办？

当前 token 在过期前无法由服务端主动撤销；登出只删除当前浏览器副本。应使用 HTTPS、避免日志记录 token、缩短有效期，并在需要即时撤销时增加服务端 denylist 或 token 版本/撤销机制。本项目目前未实现这些撤销机制。

## 验证状态

前端生产构建已通过。后端运行与 API 集成用例尚未执行：当前环境未发现 Java/Maven 命令，也没有运行中的后端或数据库监听端口。因此登录、401/403、过期 token 和不同角色的真实 HTTP 结果仍待具备 Java、MySQL 的运行环境后验证；不能标记为已测试通过。
