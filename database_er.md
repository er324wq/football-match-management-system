# 足球管理系统数据库ER图

## 更新时间

2026-04-19

---

## 一、数据库ER图

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                                        USER (用户表)                                              │
├─────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ PK │ id                BIGINT      主键ID                                                                        │
│     │ username         VARCHAR(50) 用户名(唯一)                                                                  │
│     │ password         VARCHAR(100) 密码(BCrypt加密)                                                             │
│     │ nickname         VARCHAR(50) 昵称                                                                           │
│     │ phone            VARCHAR(20) 手机号                                                                        │
│     │ email            VARCHAR(100) 邮箱                                                                         │
│     │ avatar           VARCHAR(255) 头像URL                                                                     │
│     │ signature        VARCHAR(255) 个性签名                                                                     │
│     │ role             VARCHAR(20)  角色(admin/referee/user)                                                    │
│     │ status           TINYINT     状态(1启用 0禁用)                                                            │
│     │ deleted          TINYINT     逻辑删除                                                                    │
│     │ create_time      DATETIME    创建时间                                                                      │
│     │ update_time      DATETIME    更新时间                                                                      │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
         │
         │ 1:N
         │ 1:N
         │ 1:N
         ▼
┌──────────────────────────────────┐    ┌─────────────────────────────────────────────────────────────────────────────┐
│      NOTIFICATION (通知表)        │    │                    TEAM_MEMBER (球队成员表)                                │
├──────────────────────────────────┤    ├─────────────────────────────────────────────────────────────────────────────┤
│ PK │ id                BIGINT    │    │ PK │ id                BIGINT    主键ID                                    │
│     │ user_id          BIGINT    │◄──│     │ team_id           BIGINT    球队ID (FK)                              │
│     │ title            VARCHAR   │    │     │ user_id           BIGINT    用户ID (FK)                               │
│     │ content          TEXT      │    │     │ role              VARCHAR   角色(captain/admin/member)               │
│     │ type             VARCHAR   │    │     │ status            INT       状态                                         │
│     │ is_read          TINYINT  │    │     │ create_time       DATETIME  创建时间                                   │
│     │ create_time      DATETIME  │    │     │ update_time       DATETIME  更新时间                                   │
└──────────────────────────────────┘    └─────────────────────────────────────────────────────────────────────────────┘
                                            ▲                               ▲
                                            │                               │
                 ┌──────────────────────────┴───────────────────────────────┘
                 │
                 │ N:1
                 ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                                        TEAM (球队表)                                              │
├─────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ PK │ id                BIGINT      主键ID                                                                        │
│     │ name             VARCHAR(100) 球队名称                                                                     │
│     │ coach            VARCHAR(50)  教练                                                                         │
│     │ city             VARCHAR(50)  所属城市                                                                   │
│     │ description      TEXT          描述                                                                        │
│     │ status           TINYINT      状态(1启用 0禁用)                                                          │
│     │ deleted          TINYINT      逻辑删除                                                                   │
│     │ create_time      DATETIME     创建时间                                                                     │
│     │ update_time      DATETIME     更新时间                                                                     │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
         │
         │ 1:N
         │
         ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                                        PLAYER (球员表)                                            │
├─────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ PK │ id                BIGINT      主键ID                                                                        │
│     │ name             VARCHAR(50)  姓名                                                                         │
│     │ age              INT          年龄                                                                         │
│     │ position         VARCHAR(20)  位置(前锋/中场/后卫/守门员)                                                  │
│     │ jersey_number    INT          球衣号码                                                                     │
│ FK │ team_id          BIGINT       所属球队ID (REFERENCES team(id))                                            │
│     │ status           TINYINT      状态(1启用 0禁用)                                                          │
│     │ deleted          TINYINT      逻辑删除                                                                   │
│     │ create_time      DATETIME     创建时间                                                                     │
│     │ update_time      DATETIME     更新时间                                                                     │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
         │
         │ 1:N
         │
         ▼
┌──────────────────────────────────┐
│       CARD_RECORD (黄红牌记录表)  │
├──────────────────────────────────┤
│ PK │ id                BIGINT    │
│ FK │ match_id          BIGINT    │ ◄──┐
│ FK │ player_id         BIGINT    │◄───│──── from PLAYER
│     │ card_type        VARCHAR   │     │
│     │ card_time        INT       │     │
│     │ reason           VARCHAR   │     │
│     │ deleted          INT       │     │
│     │ create_time      DATETIME  │     │
│     │ update_time      DATETIME  │     │
└──────────────────────────────────┘     │
                                        │
┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                                        MATCH (比赛表)                                              │
├─────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ PK │ id                BIGINT      主键ID                                                                        │
│ FK │ home_team_id      BIGINT      主队ID (REFERENCES team(id))                                                 │
│ FK │ away_team_id      BIGINT      客队ID (REFERENCES team(id))                                                 │
│     │ match_time       DATETIME    比赛时间                                                                     │
│     │ stadium          VARCHAR(100) 比赛场地                                                                   │
│     │ status           VARCHAR(20)  状态(upcoming/ongoing/finished)                                               │
│     │ tag              VARCHAR(50)  比赛标签                                                                   │
│     │ deleted          TINYINT     逻辑删除                                                                   │
│     │ create_time      DATETIME     创建时间                                                                     │
│     │ update_time      DATETIME     更新时间                                                                     │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
         │
         │ 1:1
         │ 1:N
         │
         ▼
┌──────────────────────────────────┐    ┌─────────────────────────────────────────────────────────────────────┐
│      MATCH_RESULT (比赛结果表)    │    │              MATCH_SCORING (比赛评分表)                              │
├──────────────────────────────────┤    ├─────────────────────────────────────────────────────────────────────┤
│ PK │ id                BIGINT    │    │ PK │ id                BIGINT    主键ID                                │
│ FK │ match_id          BIGINT    │◄──│     │ referee_id        BIGINT    裁判ID (FK -> USER)                  │
│     │ home_score       INT       │    │ FK │ match_id          BIGINT    比赛ID (FK)                           │
│     │ away_score       INT       │    │     │ score             DOUBLE     评分                                  │
│     │ referee          VARCHAR   │    │     │ comment           TEXT      评语                                   │
│     │ deleted          INT       │    │     │ create_time       DATETIME  创建时间                               │
│     │ create_time      DATETIME  │    │     │ update_time       DATETIME  更新时间                               │
│     │ update_time      DATETIME  │    └─────────────────────────────────────────────────────────────────────┘
└──────────────────────────────────┘
         │
         │ 1:N
         ▼
┌──────────────────────────────────┐
│      GOAL_RECORD (进球记录表)     │
├──────────────────────────────────┤
│ PK │ id                BIGINT    │
│ FK │ match_id          BIGINT    │ ◄──┐
│ FK │ player_id         BIGINT    │◄───│──── from PLAYER
│     │ goal_time        INT       │     │
│     │ deleted          INT       │     │
│     │ create_time      DATETIME  │     │
│     │ update_time      DATETIME  │     │
└──────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                                   APPLICATION (申请记录表)                                        │
├─────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│ PK │ id                BIGINT      主键ID                                                                        │
│ FK │ user_id           BIGINT      申请人ID (REFERENCES user(id))                                              │
│     │ type             VARCHAR(50) 类型: team/match/create/create_team                                         │
│     │ target_id        BIGINT      关联球队或比赛ID                                                              │
│     │ name             VARCHAR(200) 创办比赛或球队名称                                                          │
│     │ time             VARCHAR     计划比赛时间                                                                  │
│     │ location         VARCHAR     地点                                                                          │
│     │ match_type       VARCHAR     比赛类型                                                                       │
│     │ description      TEXT         描述                                                                          │
│     │ reason          TEXT         申请理由                                                                       │
│     │ status          VARCHAR     状态(pending/approved/rejected)                                               │
│     │ create_time     DATETIME     创建时间                                                                       │
│     │ update_time     DATETIME     更新时间                                                                       │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 二、表关系说明

### 1. 用户相关
```
USER ─────┬──── APPLICATION (1:N)    : 一个用户可以提交多个申请
          ├──── NOTIFICATION (1:N)   : 一个用户可以收到多个通知
          └──── TEAM_MEMBER (1:N)    : 一个用户可以是多个球队的成员
```

### 2. 球队相关
```
TEAM ─────┬──── PLAYER (1:N)          : 一个球队可以有多个球员
          ├──── TEAM_MEMBER (1:N)     : 一个球队可以有多个成员
          ├──── MATCH as home (1:N)  : 一个球队可以作为主队参加多场比赛
          └──── MATCH as away (1:N)   : 一个球队可以作为客队参加多场比赛
```

### 3. 球员相关
```
PLAYER ───┬──── GOAL_RECORD (1:N)    : 一个球员可以有多条进球记录
           └──── CARD_RECORD (1:N)     : 一个球员可以有多张黄红牌记录
```

### 4. 比赛相关
```
MATCH ────┬──── MATCH_RESULT (1:1)    : 一场比赛对应一个比赛结果
          ├──── GOAL_RECORD (1:N)     : 一场比赛可以有多个进球记录
          ├──── CARD_RECORD (1:N)     : 一场比赛可以有多张黄红牌记录
          ├──── MATCH_SCORING (1:N)   : 一场比赛可以有多条评分记录
          └──── APPLICATION (1:N)      : 一场比赛可以对应多个申请
```

---

## 三、实体关系详解

### 3.1 User (用户)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，用户唯一标识 |
| username | VARCHAR | 用户名，唯一 |
| password | VARCHAR | BCrypt加密后的密码 |
| nickname | VARCHAR | 昵称 |
| phone | VARCHAR | 手机号 |
| email | VARCHAR | 邮箱 |
| avatar | VARCHAR | 头像URL |
| signature | VARCHAR | 个性签名 |
| role | VARCHAR | 角色：admin(管理员)/referee(裁判)/user(普通用户) |
| status | INT | 状态：1启用 0禁用 |

**关联关系：**
- 1:N → Application (用户提交申请)
- 1:N → Notification (用户接收通知)
- 1:N → TeamMember (用户加入球队)
- 1:N → MatchScoring (裁判评分比赛)

### 3.2 Team (球队)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键，球队唯一标识 |
| name | VARCHAR | 球队名称 |
| coach | VARCHAR | 教练 |
| city | VARCHAR | 所属城市 |
| description | TEXT | 描述信息 |
| status | INT | 状态：1启用 0禁用 |

**关联关系：**
- 1:N → Player (球队拥有球员)
- 1:N → TeamMember (球队拥有成员)
- 1:N → Match (作为主队)
- 1:N → Match (作为客队)

### 3.3 Player (球员)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR | 球员姓名 |
| age | INT | 年龄 |
| position | VARCHAR | 位置：前锋/中场/后卫/守门员 |
| jersey_number | INT | 球衣号码 |
| team_id | BIGINT | 所属球队ID (外键) |
| status | INT | 状态 |

**关联关系：**
- N:1 → Team (球员属于球队)
- 1:N → GoalRecord (球员进球记录)
- 1:N → CardRecord (球员黄红牌记录)

### 3.4 Match (比赛)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| home_team_id | BIGINT | 主队ID (外键) |
| away_team_id | BIGINT | 客队ID (外键) |
| match_time | DATETIME | 比赛时间 |
| stadium | VARCHAR | 比赛场地 |
| status | VARCHAR | 状态：upcoming/ongoing/finished |

**关联关系：**
- N:1 → Team (主队)
- N:1 → Team (客队)
- 1:1 → MatchResult (比赛结果)
- 1:N → GoalRecord (进球记录)
- 1:N → CardRecord (黄红牌记录)
- 1:N → MatchScoring (比赛评分)

### 3.5 MatchResult (比赛结果)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| match_id | BIGINT | 比赛ID (外键，唯一) |
| home_score | INT | 主队得分 |
| away_score | INT | 客队得分 |
| referee | VARCHAR | 裁判姓名 |

### 3.6 GoalRecord (进球记录)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| match_id | BIGINT | 比赛ID (外键) |
| player_id | BIGINT | 球员ID (外键) |
| goal_time | INT | 进球时间(分钟) |

### 3.7 Application (申请记录)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 申请人ID (外键) |
| type | VARCHAR | 类型：team/match/create/create_team |
| target_id | BIGINT | 关联目标ID |
| name | VARCHAR | 名称 |
| time | VARCHAR | 计划时间 |
| location | VARCHAR | 地点 |
| match_type | VARCHAR | 比赛类型 |
| description | TEXT | 描述 |
| reason | TEXT | 申请理由 |
| status | VARCHAR | 状态：pending/approved/rejected |

### 3.8 Notification (通知)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户ID (外键) |
| title | VARCHAR | 标题 |
| content | TEXT | 内容 |
| type | VARCHAR | 类型 |
| is_read | TINYINT | 是否已读 |

### 3.9 TeamMember (球队成员)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| team_id | BIGINT | 球队ID (外键) |
| user_id | BIGINT | 用户ID (外键) |
| role | VARCHAR | 角色：captain/admin/member |
| status | INT | 状态 |

### 3.10 CardRecord (黄红牌记录)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| match_id | BIGINT | 比赛ID (外键) |
| player_id | BIGINT | 球员ID (外键) |
| card_type | VARCHAR | 牌类型：黄牌/红牌 |
| card_time | INT | 出牌时间(分钟) |
| reason | VARCHAR | 原因 |

### 3.11 MatchScoring (比赛评分)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| referee_id | BIGINT | 裁判ID (外键->User) |
| match_id | BIGINT | 比赛ID (外键) |
| score | DOUBLE | 评分 |
| comment | TEXT | 评语 |

---

## 四、业务流程图

### 4.1 用户注册与球队创建流程
```
用户注册 → 申请创建球队 → 管理员审批 → 创建球队成功 → 用户成为队长
                                      ↓ 审批拒绝
                               收到通知(拒绝原因)
```

### 4.2 球员加入球队流程
```
用户申请加入球队 → 球队队长审批 → 加入成功 → 成为球队成员
                                      ↓ 审批拒绝
                               收到通知(拒绝原因)
```

### 4.3 比赛流程
```
创建比赛申请 → 审批通过 → 分配主客队 → 比赛进行 → 记录比分/进球/黄红牌 → 生成比赛报告
```

---

## 五、索引设计

| 表名 | 索引名 | 字段 | 类型 |
|------|--------|------|------|
| user | idx_username | username | 普通索引 |
| user | idx_role | role | 普通索引 |
| user | idx_phone | phone | 普通索引 |
| user | idx_email | email | 普通索引 |
| team | idx_name | name | 普通索引 |
| team | idx_city | city | 普通索引 |
| player | idx_name | name | 普通索引 |
| player | idx_team_id | team_id | 普通索引 |
| match | idx_match_time | match_time | 普通索引 |
| match | idx_home_team | home_team_id | 普通索引 |
| match | idx_away_team | away_team_id | 普通索引 |
| match | idx_status | status | 普通索引 |
| match_result | idx_match_id | match_id | 唯一索引 |
| goal_record | idx_match_id | match_id | 普通索引 |
| goal_record | idx_player_id | player_id | 普通索引 |
| application | idx_user_id | user_id | 普通索引 |
| application | idx_type | type | 普通索引 |
| application | idx_status | status | 普通索引 |
| notification | idx_user_id | user_id | 普通索引 |
| notification | idx_create_time | create_time | 普通索引 |
| team_member | - | team_id, user_id | 联合索引 |

---

## 六、外键约束

| 表名 | 字段 | 引用表 | 引用字段 | 删除策略 |
|------|------|--------|----------|----------|
| player | team_id | team | id | SET NULL |
| match | home_team_id | team | id | CASCADE |
| match | away_team_id | team | id | CASCADE |
| match_result | match_id | match | id | CASCADE |
| goal_record | match_id | match | id | CASCADE |
| goal_record | player_id | player | id | CASCADE |
| card_record | match_id | match | id | CASCADE |
| card_record | player_id | player | id | CASCADE |
| match_scoring | match_id | match | id | CASCADE |
| match_scoring | referee_id | user | id | - |
