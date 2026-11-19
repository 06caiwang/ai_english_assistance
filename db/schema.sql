-- ============================================================
-- 英语学习 App · 建库建表（7 张表）
-- MySQL 8.0 / utf8mb4
-- 执行： mysql -u root english_app < db/schema.sql
-- 注意：character 是保留字倾向，所有 SQL 里必须加反引号
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `user_food`;
DROP TABLE IF EXISTS `food`;
DROP TABLE IF EXISTS `essay`;
DROP TABLE IF EXISTS `user_character`;
DROP TABLE IF EXISTS `character`;
DROP TABLE IF EXISTS `pet`;
DROP TABLE IF EXISTS `user_word`;
DROP TABLE IF EXISTS `word`;
DROP TABLE IF EXISTS `user`;

-- ① 用户
CREATE TABLE `user` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `username`   VARCHAR(32)  NOT NULL,
  `password`   VARCHAR(100) NOT NULL COMMENT 'BCrypt 哈希',
  `nickname`   VARCHAR(32)  DEFAULT NULL,
  `coin`       INT          NOT NULL DEFAULT 0,
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- ② 单词（初始化导入，运行期只读）
CREATE TABLE `word` (
  `id`       BIGINT       NOT NULL AUTO_INCREMENT,
  `spelling` VARCHAR(64)  NOT NULL,
  `phonetic` VARCHAR(64)  DEFAULT NULL,
  `meaning`  VARCHAR(255) NOT NULL,
  `example`  VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_spelling` (`spelling`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单词库';

-- ③ 用户学习状态（进度 + 复习 + 错题 三合一）
CREATE TABLE `user_word` (
  `id`             BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`        BIGINT   NOT NULL,
  `word_id`        BIGINT   NOT NULL,
  `stage`          TINYINT  NOT NULL DEFAULT 0 COMMENT '复习阶段 0-4',
  `next_review_at` DATETIME DEFAULT NULL,
  `wrong_count`    INT      NOT NULL DEFAULT 0,
  `updated_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_word` (`user_id`, `word_id`),
  KEY `idx_review` (`user_id`, `next_review_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习进度';

-- ④ 宠物
CREATE TABLE `pet` (
  `id`           BIGINT      NOT NULL AUTO_INCREMENT,
  `user_id`      BIGINT      NOT NULL,
  `name`         VARCHAR(32) NOT NULL DEFAULT 'Luna',
  `hunger`       TINYINT     NOT NULL DEFAULT 100,
  `level`        INT         NOT NULL DEFAULT 1,
  `exp`          INT         NOT NULL DEFAULT 0,
  `character_id` BIGINT      NOT NULL DEFAULT 1 COMMENT '当前使用的形象',
  `last_fed_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物';

-- ⑤ 形象字典（初始化导入，运行期只读）
CREATE TABLE `character` (
  `id`    BIGINT       NOT NULL AUTO_INCREMENT,
  `name`  VARCHAR(32)  NOT NULL,
  `image` VARCHAR(100) NOT NULL COMMENT 'drawable 资源名',
  `price` INT          NOT NULL DEFAULT 0 COMMENT '0 = 注册即送',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='形象字典';

-- ⑥ 用户已解锁的形象
CREATE TABLE `user_character` (
  `id`           BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`      BIGINT   NOT NULL,
  `character_id` BIGINT   NOT NULL,
  `unlocked_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_character` (`user_id`, `character_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='已解锁形象';

-- ⑦ 作文批改
CREATE TABLE `essay` (
  `id`         BIGINT   NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT   NOT NULL,
  `content`    TEXT     NOT NULL,
  `score`      INT      DEFAULT NULL,
  `comment`    TEXT     DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作文批改记录';


-- ⑧ 食物字典（整改 P0 新增）
CREATE TABLE `food` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(32)  NOT NULL,
  `image`       VARCHAR(100) NOT NULL COMMENT 'drawable 资源名',
  `restore`     TINYINT      NOT NULL COMMENT '恢复饥饿值',
  `price`       INT          NOT NULL COMMENT '售价（金币）',
  `description` VARCHAR(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_food_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食物字典';

-- ⑨ 食物背包（整改 P0 新增）
CREATE TABLE `user_food` (
  `id`      BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `food_id` BIGINT NOT NULL,
  `count`   INT    NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_food` (`user_id`, `food_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食物背包';

SET FOREIGN_KEY_CHECKS = 1;
