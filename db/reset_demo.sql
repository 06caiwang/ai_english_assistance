-- 把演示账号恢复到初始状态（跑完 smoke 或演示前执行一次）
-- 用法： mysql -u root english_app < db/reset_demo.sql

SET NAMES utf8mb4;
SET @uid = (SELECT id FROM `user` WHERE username = 'demo01');

UPDATE `user` SET coin = 60, nickname = '演示' WHERE id = @uid;

UPDATE `pet` SET hunger = 100, exp = 0, level = 1, character_id = 1,
                 last_fed_at = NOW() WHERE user_id = @uid;

-- 学习进度清空，保证"今日单词"能出 10 个新词
DELETE FROM `user_word` WHERE user_id = @uid;
DELETE FROM `essay` WHERE user_id = @uid;

-- 背包：各 2 个
DELETE FROM `user_food` WHERE user_id = @uid;
INSERT INTO `user_food` (user_id, food_id, `count`) VALUES (@uid, 1, 2), (@uid, 2, 2);

-- 形象只留默认的熊猫
DELETE FROM `user_character` WHERE user_id = @uid AND character_id <> 1;

SELECT (SELECT coin FROM `user` WHERE id = @uid)        AS coin,
       (SELECT hunger FROM `pet` WHERE user_id = @uid)  AS hunger,
       (SELECT COUNT(*) FROM `user_food` WHERE user_id = @uid) AS food_rows,
       (SELECT COUNT(*) FROM `user`)                    AS total_users;
