# 足球管理系统数据库建表语句

## 目录

- [数据库初始化脚本](#数据库初始化脚本)
- [性能管理菜单脚本](#性能管理菜单脚本)

---

## 数据库初始化脚本

### 创建数据库

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS football_admin DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE football_admin;
```

### 用户表 (user)

```sql
-- 用户表
CREATE TABLE IF NOT EXISTS user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
    nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    signature VARCHAR(255) DEFAULT NULL COMMENT '个性签名',
    role VARCHAR(20) DEFAULT 'user' COMMENT '角色(admin/referee/user)',
    status TINYINT DEFAULT 1 COMMENT '状态(1启用 0禁用)',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除(1删除 0未删除)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_role (role),
    INDEX idx_phone (phone),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

### 球队表 (team)

```sql
-- 球队表
CREATE TABLE IF NOT EXISTS team (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '球队名称',
    coach VARCHAR(50) DEFAULT NULL COMMENT '教练',
    city VARCHAR(50) DEFAULT NULL COMMENT '所属城市',
    description TEXT DEFAULT NULL COMMENT '描述',
    status TINYINT DEFAULT 1 COMMENT '状态(1启用 0禁用)',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_name (name),
    INDEX idx_city (city)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='球队表';
```

### 球员表 (player)

```sql
-- 球员表
CREATE TABLE IF NOT EXISTS player (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    age INT DEFAULT NULL COMMENT '年龄',
    position VARCHAR(20) DEFAULT NULL COMMENT '位置(前锋/中场/后卫/守门员)',
    jersey_number INT DEFAULT NULL COMMENT '球衣号码',
    team_id BIGINT DEFAULT NULL COMMENT '所属球队ID',
    status TINYINT DEFAULT 1 COMMENT '状态(1启用 0禁用)',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_name (name),
    INDEX idx_team_id (team_id),
    FOREIGN KEY (team_id) REFERENCES team(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='球员表';
```

### 比赛表 (match)

```sql
-- 比赛表
CREATE TABLE IF NOT EXISTS `match` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    home_team_id BIGINT NOT NULL COMMENT '主队ID',
    away_team_id BIGINT NOT NULL COMMENT '客队ID',
    match_time DATETIME NOT NULL COMMENT '比赛时间',
    stadium VARCHAR(100) DEFAULT NULL COMMENT '比赛场地',
    status VARCHAR(20) DEFAULT 'upcoming' COMMENT '状态(upcoming/ongoing/finished)',
    tag VARCHAR(50) DEFAULT NULL COMMENT '比赛标签',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_match_time (match_time),
    INDEX idx_home_team (home_team_id),
    INDEX idx_away_team (away_team_id),
    INDEX idx_status (status),
    FOREIGN KEY (home_team_id) REFERENCES team(id) ON DELETE CASCADE,
    FOREIGN KEY (away_team_id) REFERENCES team(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比赛表';
```

### 比赛结果表 (match_result)

```sql
-- 比赛结果表
CREATE TABLE IF NOT EXISTS match_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    match_id BIGINT NOT NULL COMMENT '比赛ID',
    home_score INT DEFAULT 0 COMMENT '主队比分',
    away_score INT DEFAULT 0 COMMENT '客队比分',
    referee VARCHAR(50) DEFAULT NULL COMMENT '裁判',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_match_id (match_id),
    UNIQUE KEY uk_match_id (match_id),
    FOREIGN KEY (match_id) REFERENCES `match`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比赛结果表';
```

### 进球记录表 (goal_record)

```sql
-- 进球记录表
CREATE TABLE IF NOT EXISTS goal_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    match_id BIGINT NOT NULL COMMENT '比赛ID',
    player_id BIGINT NOT NULL COMMENT '球员ID',
    goal_time INT NOT NULL COMMENT '进球时间(分钟)',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_match_id (match_id),
    INDEX idx_player_id (player_id),
    FOREIGN KEY (match_id) REFERENCES `match`(id) ON DELETE CASCADE,
    FOREIGN KEY (player_id) REFERENCES player(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='进球记录表';
```

### 申请记录表 (application)

```sql
-- 申请记录表（球队申请、参赛申请、创办比赛申请等）
CREATE TABLE IF NOT EXISTS `application` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '申请人用户ID',
    type VARCHAR(50) NOT NULL COMMENT '类型: team/match/create/create_team',
    target_id BIGINT DEFAULT NULL COMMENT '关联球队或比赛ID',
    name VARCHAR(200) DEFAULT NULL COMMENT '创办比赛或球队名称',
    schedule_time VARCHAR(100) DEFAULT NULL COMMENT '计划比赛时间(字符串)',
    location VARCHAR(200) DEFAULT NULL COMMENT '地点',
    match_type VARCHAR(50) DEFAULT NULL COMMENT '比赛类型',
    description TEXT COMMENT '描述',
    reason TEXT COMMENT '申请理由',
    status VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/approved/rejected',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_type (type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申请记录表';
```

### 通知表 (notification)

```sql
-- Notification table for user reminders
CREATE TABLE IF NOT EXISTS notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    type VARCHAR(20) DEFAULT 'info',
    is_read TINYINT(1) DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notification_user_id (user_id),
    INDEX idx_notification_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='notification';
```

---

## 示例数据

### 用户数据

以下示例只说明种子用户记录结构。管理员账号不由 SQL 写入；应用在缺少 `admin` 时读取 `INITIAL_ADMIN_PASSWORD` 并创建 BCrypt 密码。测试用户的 SQL 哈希是由随机、未保存口令生成的种子值，不提供可登录的公开口令。

```sql
-- 插入裁判账号
INSERT INTO user (id, username, password, nickname, phone, email, role, status) VALUES
(2, 'referee1', '<random BCrypt hash>', '裁判1', '13800000001', 'referee1@example.com', 'referee', 1)
ON DUPLICATE KEY UPDATE
password = VALUES(password),
nickname = VALUES(nickname),
phone = VALUES(phone),
email = VALUES(email),
role = VALUES(role),
status = VALUES(status);

-- 插入普通用户账号
INSERT INTO user (id, username, password, nickname, phone, email, role, status) VALUES
(3, 'user1', '<random BCrypt hash>', '普通用户', '13800000002', 'user1@example.com', 'user', 1)
ON DUPLICATE KEY UPDATE
password = VALUES(password),
nickname = VALUES(nickname),
phone = VALUES(phone),
email = VALUES(email),
role = VALUES(role),
status = VALUES(status);
```

### 球队数据

```sql
-- 插入示例球队数据
INSERT INTO team (name, coach, city, description) VALUES
('猛虎队', '张三', '北京', '本地强队')
ON DUPLICATE KEY UPDATE
coach = VALUES(coach),
city = VALUES(city),
description = VALUES(description);

INSERT INTO team (name, coach, city, description) VALUES
('飞鹰队', '李四', '上海', '实力强劲')
ON DUPLICATE KEY UPDATE
coach = VALUES(coach),
city = VALUES(city),
description = VALUES(description);

INSERT INTO team (name, coach, city, description) VALUES
('雄狮队', '王五', '广州', '传统劲旅')
ON DUPLICATE KEY UPDATE
coach = VALUES(coach),
city = VALUES(city),
description = VALUES(description);

INSERT INTO team (name, coach, city, description) VALUES
('猎豹队', '赵六', '深圳', '新兴球队')
ON DUPLICATE KEY UPDATE
coach = VALUES(coach),
city = VALUES(city),
description = VALUES(description);
```

### 球员数据

```sql
-- 插入示例球员数据
INSERT INTO player (name, age, position, jersey_number, team_id) VALUES
('李四', 25, '前锋', 9, 1),
('王五', 28, '中场', 10, 1),
('赵六', 26, '前锋', 9, 2),
('钱七', 23, '中场', 8, 2),
('孙八', 27, '前锋', 10, 3),
('周九', 29, '中场', 8, 3),
('吴十', 24, '前锋', 11, 4),
('郑一', 22, '后卫', 4, 4);
```

### 比赛数据

```sql
-- 插入示例比赛数据
INSERT INTO `match` (home_team_id, away_team_id, match_time, stadium, status) VALUES
(1, 2, '2024-12-10 20:00:00', '市体育场', 'finished'),
(3, 4, '2024-12-11 19:30:00', '区体育馆', 'finished'),
(1, 3, '2025-01-01 20:00:00', '市体育场', 'upcoming'),
(2, 4, '2025-01-02 19:30:00', '区体育馆', 'upcoming');
```

### 比赛结果数据

```sql
-- 插入示例比赛结果
INSERT INTO match_result (match_id, home_score, away_score, referee) VALUES
(1, 2, 1, '张裁判')
ON DUPLICATE KEY UPDATE
home_score = VALUES(home_score),
away_score = VALUES(away_score),
referee = VALUES(referee);

INSERT INTO match_result (match_id, home_score, away_score, referee) VALUES
(2, 0, 2, '李裁判')
ON DUPLICATE KEY UPDATE
home_score = VALUES(home_score),
away_score = VALUES(away_score),
referee = VALUES(referee);
```

### 进球记录数据

```sql
-- 插入示例进球记录
INSERT INTO goal_record (match_id, player_id, goal_time) VALUES
(1, 1, 25),
(1, 3, 45),
(1, 2, 75),
(2, 7, 60),
(2, 8, 85);
```

---

## 数据库兼容性迁移脚本

```sql
-- Compatible migration for older MySQL versions (without "ADD COLUMN IF NOT EXISTS")
SET @db_name = DATABASE();

SET @sql_stmt = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db_name
        AND TABLE_NAME = 'notification'
        AND COLUMN_NAME = 'user_id'
    ),
    'SELECT 1',
    'ALTER TABLE notification ADD COLUMN user_id BIGINT NOT NULL'
  )
);
PREPARE stmt FROM @sql_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_stmt = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db_name
        AND TABLE_NAME = 'user'
        AND COLUMN_NAME = 'phone'
    ),
    'SELECT 1',
    'ALTER TABLE user ADD COLUMN phone VARCHAR(20) NULL COMMENT ''手机号'' AFTER nickname'
  )
);
PREPARE stmt FROM @sql_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_stmt = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db_name
        AND TABLE_NAME = 'user'
        AND COLUMN_NAME = 'email'
    ),
    'SELECT 1',
    'ALTER TABLE user ADD COLUMN email VARCHAR(100) NULL COMMENT ''邮箱'' AFTER phone'
  )
);
PREPARE stmt FROM @sql_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_stmt = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db_name
        AND TABLE_NAME = 'user'
        AND COLUMN_NAME = 'avatar'
    ),
    'SELECT 1',
    'ALTER TABLE user ADD COLUMN avatar VARCHAR(255) NULL COMMENT ''头像URL'' AFTER email'
  )
);
PREPARE stmt FROM @sql_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_stmt = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db_name
        AND TABLE_NAME = 'user'
        AND COLUMN_NAME = 'signature'
    ),
    'SELECT 1',
    'ALTER TABLE user ADD COLUMN signature VARCHAR(255) NULL COMMENT ''个性签名'' AFTER avatar'
  )
);
PREPARE stmt FROM @sql_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_stmt = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db_name
        AND TABLE_NAME = 'notification'
        AND COLUMN_NAME = 'title'
    ),
    'SELECT 1',
    'ALTER TABLE notification ADD COLUMN title VARCHAR(255) NOT NULL'
  )
);
PREPARE stmt FROM @sql_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_stmt = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db_name
        AND TABLE_NAME = 'notification'
        AND COLUMN_NAME = 'content'
    ),
    'SELECT 1',
    'ALTER TABLE notification ADD COLUMN content TEXT NULL'
  )
);
PREPARE stmt FROM @sql_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_stmt = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db_name
        AND TABLE_NAME = 'notification'
        AND COLUMN_NAME = 'type'
    ),
    'SELECT 1',
    'ALTER TABLE notification ADD COLUMN type VARCHAR(20) DEFAULT ''info'''
  )
);
PREPARE stmt FROM @sql_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_stmt = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db_name
        AND TABLE_NAME = 'notification'
        AND COLUMN_NAME = 'is_read'
    ),
    'SELECT 1',
    'ALTER TABLE notification ADD COLUMN is_read TINYINT(1) DEFAULT 0'
  )
);
PREPARE stmt FROM @sql_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql_stmt = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = @db_name
        AND TABLE_NAME = 'notification'
        AND COLUMN_NAME = 'create_time'
    ),
    'SELECT 1',
    'ALTER TABLE notification ADD COLUMN create_time DATETIME DEFAULT CURRENT_TIMESTAMP'
  )
);
PREPARE stmt FROM @sql_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
```

---

## 性能管理菜单脚本

```sql
-- add_performance_menu.sql
INSERT INTO sys_menu (name, path, icon, parent_id, sort_order, created_at, updated_at) VALUES ('性能管理', '/performance', 'el-icon-data-analysis', 1, 10, NOW(), NOW());
```

---

## 表关系说明

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│    user     │     │    team     │     │   player   │
├─────────────┤     ├─────────────┤     ├─────────────┤
│ id          │     │ id          │◄────│ team_id    │
│ username    │     │ name        │     │ id          │
│ password    │     │ coach       │     │ name        │
│ nickname    │     │ city        │     │ position    │
│ role        │     │ description │     │ jersey_num  │
│ status      │     │ status      │     │ status      │
└─────────────┘     └─────────────┘     └─────────────┘
       │                   ▲                   │
       │                   │                   │
       ▼                   │                   │
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│application  │     │   match     │     │ goal_record │
├─────────────┤     ├─────────────┤     ├─────────────┤
│ id          │     │ id          │     │ id          │
│ user_id     │     │ home_team_id│────►│ match_id    │
│ type        │     │ away_team_id│     │ player_id   │
│ target_id   │     │ match_time  │     │ goal_time   │
│ status      │     │ stadium     │     └─────────────┘
└─────────────┘     │ status      │
                   └─────────────┘
                         ▲
                         │
                   ┌─────────────┐
                   │match_result │
                   ├─────────────┤
                   │ id          │
                   │ match_id    │────►
                   │ home_score  │
                   │ away_score  │
                   │ referee     │
                   └─────────────┘
```

---

## 测试账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | 由 `INITIAL_ADMIN_PASSWORD` 初始化 | 管理员 |
| referee1 | 无公开默认口令 | 裁判 |
| user1 | 无公开默认口令 | 普通用户 |
