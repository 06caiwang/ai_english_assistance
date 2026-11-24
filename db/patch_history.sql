-- ============================================================
-- 历史记录模块（单词查询 / 作文生成）+ 打分记录补字段
-- 执行： mysql -u root english_app < db/patch_history.sql
-- ============================================================

SET NAMES utf8mb4;

-- ① 单词查询历史：同一用户同一个词只留一条，再查就更新（相当于置顶）
CREATE TABLE IF NOT EXISTS `translate_record` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT       NOT NULL,
  `text`       VARCHAR(255) NOT NULL COMMENT '查询的原文',
  `meaning`    VARCHAR(500) NOT NULL,
  `phonetic`   VARCHAR(100) DEFAULT NULL,
  `pos`        VARCHAR(50)  DEFAULT NULL,
  `example`    VARCHAR(500) DEFAULT NULL,
  `source`     VARCHAR(20)  DEFAULT NULL COMMENT 'ai / mock',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_text` (`user_id`, `text`),
  KEY `idx_user_time` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单词查询历史';

-- ② 作文生成历史：每次生成都是独立记录（同一主题不同版本也有价值）
CREATE TABLE IF NOT EXISTS `essay_generate` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `user_id`      BIGINT       NOT NULL,
  `topic`        VARCHAR(100) NOT NULL,
  `genre`        VARCHAR(20)  DEFAULT NULL,
  `target_words` INT          DEFAULT NULL,
  `level`        VARCHAR(20)  DEFAULT NULL,
  `essay`        TEXT         NOT NULL,
  `word_count`   INT          DEFAULT 0,
  `keywords_json` TEXT        DEFAULT NULL,
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作文生成历史';

-- ③ 打分记录补两列：只存分数的话，历史详情展不开四维与扣分
ALTER TABLE `essay`
  ADD COLUMN `dims_json` TEXT DEFAULT NULL COMMENT '四维分数 JSON' AFTER `comment`,
  ADD COLUMN `deductions_json` TEXT DEFAULT NULL COMMENT '扣分明细 JSON' AFTER `dims_json`;

SELECT (SELECT COUNT(*) FROM translate_record) AS translate_rows,
       (SELECT COUNT(*) FROM essay_generate)   AS generate_rows,
       (SELECT COUNT(*) FROM essay)            AS essay_rows;
