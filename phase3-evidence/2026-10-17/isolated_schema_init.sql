-- 创建数据库
CREATE DATABASE IF NOT EXISTS football_admin_phase3_it_20261017 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE football_admin_phase3_it_20261017;

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

-- UserService bootstraps admin with a test-only random INITIAL_ADMIN_PASSWORD.
-- Remaining sample-account hashes use random one-time values; their plaintext is not retained.
INSERT INTO user (id, username, password, nickname, phone, email, role, status) VALUES
(2, 'referee1', '$2a$10$QxDZrkt2BsAgk8Y3ATmpVeJJDC88Kq.qvaKNnIwcG9ajiojYB8QV.', '裁判1', '13800000001', 'referee1@example.com', 'referee', 1)
ON DUPLICATE KEY UPDATE
password = VALUES(password),
nickname = VALUES(nickname),
phone = VALUES(phone),
email = VALUES(email),
role = VALUES(role),
status = VALUES(status);

INSERT INTO user (id, username, password, nickname, phone, email, role, status) VALUES
(3, 'user1', '$2a$10$2bgAwRjOWgXUg5UmckKODOUV.05OCNOw8uiNGCtCmgVB3TcufGMiO', '普通用户', '13800000002', 'user1@example.com', 'user', 1)
ON DUPLICATE KEY UPDATE
password = VALUES(password),
nickname = VALUES(nickname),
phone = VALUES(phone),
email = VALUES(email),
role = VALUES(role),
status = VALUES(status);

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

-- 插入示例比赛数据
INSERT INTO `match` (home_team_id, away_team_id, match_time, stadium, status) VALUES
(1, 2, '2024-12-10 20:00:00', '市体育场', 'finished'),
(3, 4, '2024-12-11 19:30:00', '区体育馆', 'finished'),
(1, 3, '2025-01-01 20:00:00', '市体育场', 'upcoming'),
(2, 4, '2025-01-02 19:30:00', '区体育馆', 'upcoming');

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

-- 插入示例进球记录
INSERT INTO goal_record (match_id, player_id, goal_time) VALUES
(1, 1, 25),  -- 本泽马 25分钟
(1, 3, 45),  -- 莱万 45分钟
(1, 2, 75),  -- 莫德里奇 75分钟
(2, 7, 60),  -- 萨拉赫 60分钟
(2, 8, 85);  -- 范戴克 85分钟

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

-- 团队成员表（TeamMember 实体及团队成员接口使用）
CREATE TABLE IF NOT EXISTS team_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    team_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) DEFAULT 'member' COMMENT 'captain/admin/member',
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_team_member_team_user (team_id, user_id),
    INDEX idx_team_member_user_id (user_id),
    INDEX idx_team_member_status (status),
    CONSTRAINT fk_team_member_team FOREIGN KEY (team_id) REFERENCES team(id) ON DELETE CASCADE,
    CONSTRAINT fk_team_member_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='球队成员表';

-- 红黄牌记录表（与 CardRecordServiceImpl 的运行时建表结构一致）
CREATE TABLE IF NOT EXISTS card_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    match_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    card_type VARCHAR(20) NOT NULL,
    card_time INT NOT NULL,
    reason VARCHAR(255) DEFAULT NULL,
    deleted TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_card_match_id (match_id),
    INDEX idx_card_player_id (player_id),
    INDEX idx_card_type (card_type),
    CONSTRAINT fk_card_record_match FOREIGN KEY (match_id) REFERENCES `match`(id) ON DELETE CASCADE,
    CONSTRAINT fk_card_record_player FOREIGN KEY (player_id) REFERENCES player(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='红黄牌记录表';

-- 裁判评分表（MatchScoring 实体及评分接口使用）
CREATE TABLE IF NOT EXISTS match_scoring (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    referee_id BIGINT NOT NULL,
    match_id BIGINT NOT NULL,
    score DOUBLE DEFAULT NULL,
    comment TEXT DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_match_scoring_referee_id (referee_id),
    INDEX idx_match_scoring_match_id (match_id),
    CONSTRAINT fk_match_scoring_referee FOREIGN KEY (referee_id) REFERENCES user(id) ON DELETE CASCADE,
    CONSTRAINT fk_match_scoring_match FOREIGN KEY (match_id) REFERENCES `match`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比赛裁判评分表';

-- 性能管理模块使用的系统表（结构与 PerformanceServiceImpl 一致）
CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    module_name VARCHAR(64) NOT NULL,
    action_name VARCHAR(128) NOT NULL,
    operator_name VARCHAR(64) DEFAULT NULL,
    detail TEXT DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_operation_log_module (module_name),
    INDEX idx_operation_log_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

CREATE TABLE IF NOT EXISTS db_backup (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    backup_name VARCHAR(128) NOT NULL,
    remark VARCHAR(255) DEFAULT NULL,
    operator_name VARCHAR(64) DEFAULT NULL,
    table_count INT DEFAULT 0,
    row_count INT DEFAULT 0,
    backup_content LONGTEXT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_db_backup_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据库备份记录';

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
