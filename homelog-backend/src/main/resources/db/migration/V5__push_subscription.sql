-- ============================================================================
-- 家记 · 增量迁移 V5__push_subscription.sql
-- 设计依据：Web Push 通知通道（浏览器推送，VAPID）
--
-- 解决什么：
--   到期提醒此前只有邮件一条通道（RemindTask → MailService）。本表保存浏览器
--   订阅信息（PushSubscription 的 endpoint + keys.p256dh + keys.auth），
--   让提醒任务可在邮件之外追加 Web Push 推送。
--
-- 数据语义：
--   · 一个用户可在多设备/多浏览器各有订阅 → 多行，按 user_id 反查全部推送目标；
--   · endpoint 由推送服务（FCM/Mozira/Apple…）签发，全局唯一标识一个订阅；
--   · (user_id, endpoint) 唯一 → 同一浏览器重复注册幂等 upsert，不产生重复推送；
--   · endpoint 失效（推送返回 404/410）时由服务端删除该行；
--   · p256dh / auth 是订阅端 ECDH 公钥与鉴权密钥，发送时用于载荷加密，按原文存储。
--
-- 幂等：CREATE TABLE IF NOT EXISTS，可重复执行。
-- ============================================================================

USE `homelog`;

CREATE TABLE IF NOT EXISTS `jj_push_subscription` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`     BIGINT       NOT NULL COMMENT '订阅所属用户ID',
    `endpoint`    VARCHAR(512) NOT NULL COMMENT '推送服务端签发的订阅 endpoint（全局唯一标识一个订阅）',
    `p256dh`      VARCHAR(255) NOT NULL COMMENT '订阅端 ECDH 公钥 keys.p256dh（base64url）',
    `auth`        VARCHAR(255) NOT NULL COMMENT '订阅端鉴权密钥 keys.auth（base64url）',
    `platform`    VARCHAR(20)  DEFAULT NULL COMMENT '来源平台：ios/android/desktop，仅用于展示与排查',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_endpoint` (`user_id`, `endpoint`),
    KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='浏览器 Web Push 订阅表';

-- ============================================================================
-- 回滚
--   本表为新增独立表，不影响任何既有数据；Web Push 通道下线时直接 DROP 即可。
-- DROP TABLE IF EXISTS `jj_push_subscription`;
-- ============================================================================

-- ============================================================================
-- END OF V5__push_subscription.sql
-- ============================================================================
