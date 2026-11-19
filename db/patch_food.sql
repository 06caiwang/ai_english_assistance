-- ============================================================
-- 整改 P0：食物与背包（在 schema.sql 基础上增量执行）
-- 执行： mysql -u root english_app < db/patch_food.sql
-- ============================================================

SET NAMES utf8mb4;

-- ① 食物字典（只读）
CREATE TABLE IF NOT EXISTS `food` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(32)  NOT NULL,
  `image`       VARCHAR(100) NOT NULL COMMENT 'drawable 资源名',
  `restore`     TINYINT      NOT NULL COMMENT '恢复饥饿值',
  `price`       INT          NOT NULL COMMENT '售价（金币）',
  `description` VARCHAR(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_food_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食物字典';

-- ② 用户背包（食物库存）
CREATE TABLE IF NOT EXISTS `user_food` (
  `id`      BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `food_id` BIGINT NOT NULL,
  `count`   INT    NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_food` (`user_id`, `food_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='食物背包';

-- ③ 种子数据：单位成本基本持平，贵在省事
DELETE FROM `food`;
INSERT INTO `food` (`id`, `name`, `image`, `restore`, `price`, `description`) VALUES
(1, '胡萝卜', 'food_carrot',       10,  5, '应急小食，聊胜于无'),
(2, '面包',   'food_bread',        25, 12, '主力口粮，性价比最高'),
(3, '熟牛肉', 'food_beef',         50, 25, '一天两顿，省心'),
(4, '金苹果', 'food_golden_apple', 100, 55, '一键喂满，懒人必备');
