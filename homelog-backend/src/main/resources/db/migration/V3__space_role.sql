-- ============================================================================
-- 家记 · 增量迁移 V3__space_role.sql      【落地路线 步 1】
-- 设计依据：家记账号角色与家庭空间设计.md v1.2 §5.2 / §4.3 / §13
--
-- 做四件事：
--   ① 补列：jj_space(status,updated_at)、jj_space_member(status,nickname,updated_at)、
--            jj_message(type,ref_id)
--   ② 新建邀请表 jj_invitation —— 按决策 D2，无 token / 无 expires_at 列
--   ③ 补齐空间 owner 的成员记录（jj_space.owner_id 有值但成员表缺行的情况）
--   ④ 角色编号重排：老 1/2/3 → 新 1=owner / 2=admin / 3=member / 4=readonly（决策 D4+D6）
--
-- 幂等性：全部包在存储过程内，用 information_schema 判重，可安全重复执行；
--         第 ④ 步（唯一会改数据的一步）额外用 status 列是否存在做一次性闸门，
--         绝不会二次串档。
--
-- ⚠️⚠️ 执行前必读：本脚本一跑，库里 role 数值的含义就地改变 ⚠️⚠️
--   必须与「配套代码改动」一起上线，否则旧代码会把新数值读错（权限判定静默错位，
--   不会报错、只会放行/拒绝错人）。配套清单见文件末尾【第 7 节】。
--
--   推荐操作顺序（停机窗口约 1 分钟）：
--     1) 停后端服务
--     2) 备份：mysqldump --databases homelog > backup_before_V3.sql
--     3) 执行本脚本
--     4) 跑【第 8 节】验证 SQL，确认角色分布与预期一致
--     5) 部署含步 1 改动的后端 + 前端
--     6) 起服，登录验证
--
--   若只在开发库跑、且服务不停：请确保第 5 步的代码同一天上线，不要隔夜。
-- ============================================================================

USE `homelog`;

DROP PROCEDURE IF EXISTS `sp_homelog_v3_space_role`;

DELIMITER $$

CREATE PROCEDURE `sp_homelog_v3_space_role`()
BEGIN

    DECLARE v_has_space_status   INT DEFAULT 0;
    DECLARE v_has_member_status  INT DEFAULT 0;
    DECLARE v_has_msg_type       INT DEFAULT 0;
    DECLARE v_has_msg_refid      INT DEFAULT 0;

    -- ========================================================================
    -- 【第 1 步】jj_space：补 updated_at、status
    -- ========================================================================
    SELECT COUNT(*) INTO v_has_space_status
      FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME   = 'jj_space'
       AND COLUMN_NAME  = 'status';

    IF v_has_space_status = 0 THEN
        ALTER TABLE `jj_space`
            ADD COLUMN `status`     TINYINT  NOT NULL DEFAULT 1
                COMMENT '状态：1=正常 2=已解散' AFTER `owner_id`,
            ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
    END IF;

    -- ========================================================================
    -- 【第 2 步】jj_message：补 type、ref_id
    --   修现存缺陷 M1：MessageService.createMessage 的 type 形参此前只进日志未落库，
    --   本轮 family_invitation 通知依赖它，必须补上。
    --   默认 'record' 与历史消息语义一致（历史消息全部由记录提醒/指派产生）。
    -- ========================================================================
    SELECT COUNT(*) INTO v_has_msg_type
      FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME   = 'jj_message'
       AND COLUMN_NAME  = 'type';

    IF v_has_msg_type = 0 THEN
        ALTER TABLE `jj_message`
            ADD COLUMN `type` VARCHAR(32) NOT NULL DEFAULT 'record'
                COMMENT '类型：record=记录相关 remind=提醒 family_invitation=入家邀请 family_result=邀请回执'
                AFTER `user_id`;
    END IF;

    SELECT COUNT(*) INTO v_has_msg_refid
      FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME   = 'jj_message'
       AND COLUMN_NAME  = 'ref_id';

    IF v_has_msg_refid = 0 THEN
        ALTER TABLE `jj_message`
            ADD COLUMN `ref_id` BIGINT DEFAULT NULL
                COMMENT '业务引用ID：family_invitation/family_result 类消息指向 jj_invitation.id'
                AFTER `record_id`,
            ADD KEY `idx_type_user` (`type`, `user_id`, `is_read`);
    END IF;

    -- ========================================================================
    -- 【第 3 步】新建邀请表 jj_invitation（无 token 列 —— 决策 D2）
    --   注意：不建 (space_id, invitee_id) 唯一约束。它会与 §9「被移除后可重新邀请」冲突；
    --   重复 pending 靠事务内查询去重，最终一致性由 jj_space_member.uk_space_user 兜底。
    -- ========================================================================
    CREATE TABLE IF NOT EXISTS `jj_invitation` (
        `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
        `space_id`     BIGINT      NOT NULL COMMENT '目标空间ID',
        `inviter_id`   BIGINT      NOT NULL COMMENT '邀请人用户ID',
        `invitee_id`   BIGINT      NOT NULL COMMENT '被邀请人用户ID（必须是系统内已存在账号）',
        `role`         TINYINT     NOT NULL COMMENT '授予角色：2=admin 3=member 4=readonly',
        `status`       TINYINT     NOT NULL DEFAULT 1 COMMENT '1=pending 2=accepted 3=rejected 4=cancelled',
        `created_at`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发起时间',
        `responded_at` DATETIME    DEFAULT NULL COMMENT '应答时间',
        PRIMARY KEY (`id`),
        KEY `idx_space_status`   (`space_id`, `status`),
        KEY `idx_invitee_status` (`invitee_id`, `status`),
        KEY `idx_inviter`        (`inviter_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭空间邀请表';

    -- ========================================================================
    -- 【第 4 步】补齐缺失的 owner 成员记录
    --   背景：历史数据里存在 jj_space.owner_id 指向的用户没有对应 jj_space_member 行
    --   （早期注册逻辑遗留）。若缺 owner 行，第 5 步重排后该空间会「没有 owner」，
    --   导致没人能管成员/解散空间。INSERT IGNORE 借 uk_space_user 天然幂等。
    -- ========================================================================
    INSERT IGNORE INTO `jj_space_member`
        (`space_id`, `user_id`, `role`, `receive_mail`, `created_at`)
    SELECT s.`id`, s.`owner_id`, 1, 1, NOW()
      FROM `jj_space` s
     WHERE s.`owner_id` IS NOT NULL
       AND NOT EXISTS (
            SELECT 1 FROM `jj_space_member` m
             WHERE m.`space_id` = s.`id`
               AND m.`user_id`  = s.`owner_id`
       );

    -- ========================================================================
    -- 【第 5 步】角色编号重排 + jj_space_member 补列   ★ 唯一改数据的一步 ★
    --
    --   闸门：以 status 列是否存在判断「本轮迁移是否已执行过」。
    --         已执行过 → 整段跳过（含 UPDATE 与 ALTER），重复执行安全。
    --
    --   映射（决策 D4：以 jj_space.owner_id 判唯一 owner；其余老管理员降 admin）：
    --       owner_id 指向的用户            → 1  (owner)
    --       老 1 且非 owner_id             → 2  (admin)
    --       老 2（可编辑）                 → 3  (member)
    --       老 3（只读）                   → 4  (readonly)
    --
    --   UPDATE 的 CASE 读的是该行更新前的原值（MySQL 单趟更新，不会二次读取已写值），
    --   因此可安全地原位改写，无需临时偏移值。
    -- ========================================================================
    SELECT COUNT(*) INTO v_has_member_status
      FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME   = 'jj_space_member'
       AND COLUMN_NAME  = 'status';

    IF v_has_member_status = 0 THEN

        UPDATE `jj_space_member` sm
          JOIN `jj_space` s ON s.`id` = sm.`space_id`
           SET sm.`role` = CASE
                WHEN sm.`user_id` = s.`owner_id` THEN 1
                WHEN sm.`role` = 1               THEN 2
                WHEN sm.`role` = 2               THEN 3
                WHEN sm.`role` = 3               THEN 4
                ELSE sm.`role`
           END;

        ALTER TABLE `jj_space_member`
            MODIFY COLUMN `role` TINYINT NOT NULL
                COMMENT '角色：1=owner 所有者 2=admin 管理员 3=member 普通成员 4=readonly 只读',
            ADD COLUMN `status`     TINYINT     NOT NULL DEFAULT 1
                COMMENT '状态：1=active 在籍 2=left 已退出 3=removed 已移除' AFTER `role`,
            ADD COLUMN `nickname`   VARCHAR(32) DEFAULT NULL
                COMMENT '该空间内显示名' AFTER `status`,
            ADD COLUMN `updated_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
                ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

    END IF;

END$$

DELIMITER ;

CALL `sp_homelog_v3_space_role`();
DROP PROCEDURE IF EXISTS `sp_homelog_v3_space_role`;


-- ============================================================================
-- 【第 6 节】设计文档 §13 步 1 的数据侧完成说明
--
--   本轮【不做】的事（避免误解）：
--     · 不改任何业务数据（记录/模块/字段/消息内容原样保留）
--     · 不新建用户空间：老用户仍在原 space_id=1，不自动获得新私有空间
--       （注册建新家属步 3，需先定 Q1「内置模块怎么 seed」）
--     · 不插入邀请数据：jj_invitation 建空表，数据属步 4
-- ============================================================================


-- ============================================================================
-- 【第 7 节】★ 配套代码改动清单 ★  本脚本执行后必须一起上线
--
--   后端 1 个文件、前端 1 个文件，都是「常量表」级别的小改：
--
--   A. homelog-backend/src/main/java/com/homelog/security/Role.java
--        ADMIN = 1;        EDIT = 2;       READONLY = 3;
--     → OWNER = 1;        ADMIN = 2;      MEMBER = 3;  READONLY = 4;
--      并把 canXxx 系列方法改为基于新档位（canManageMembers = owner||admin，
--      canWrite = owner||admin||member，canSeePrivate = owner||admin，
--      ownContentOnly = member，canDelete = owner||admin）。
--      DEFAULT 兜底改为 READONLY(=4)。
--      注：步 0 已把所有业务代码收敛到这些语义方法，所以这一步只动 Role.java 一个文件。
--
--   B. homelog-frontend/src/utils/role.ts
--      ROLE 常量表同步重排（READONLY 由 3 变 4），canXxx 逻辑同上，
--      roleLabel 增加 owner 档文案（如「所有者」）。
--
--   C. UserDetailsServiceImpl 的 ROLE_* 映射需为 owner 增设权限名（如 ROLE_OWNER），
--      并确认 CurrentUserService.authorityToRole 互逆更新。
--
--   未做 A/B 就跑本脚本的后果：
--     · 老「只读」用户（库里已变 4）被代码判为「未知档」→ 走 DEFAULT 只读，侥幸正常
--     · 老「可编辑」用户（库里已变 3）被代码当成「只读」→ 突然不能编辑（体验降级，会被告警）
--     · 老「管理员」被降成 2 的那些 → 代码认 2=可编辑 → 失去成员管理权（功能缺失）
--     · 新 owner（库里 1）行为不变 —— 也就是说故障是「静默的」，必须靠第 8 节主动验证
-- ============================================================================


-- ============================================================================
-- 【第 8 节】验证 SQL（执行完本脚本后手工跑，预期结果已标注）
-- ============================================================================

-- 8.1 每个空间应恰好只有 1 个 active owner，且该 owner = jj_space.owner_id
--     预期：每个 space_id 一行，owner_cnt = 1，owner_is_space_owner = 1
-- SELECT sm.space_id,
--        SUM(sm.role = 1)                          AS owner_cnt,
--        MAX(CASE WHEN sm.user_id = s.owner_id THEN 1 ELSE 0 END) AS owner_is_space_owner
--   FROM jj_space_member sm JOIN jj_space s ON s.id = sm.space_id
--  WHERE sm.status = 1
--  GROUP BY sm.space_id;

-- 8.2 角色分布，与迁移前的老分布对照
--     预期：不存在 role=1 的大批量用户（老管理员应大部分变成 2）
-- SELECT role, COUNT(*) FROM jj_space_member WHERE status = 1 GROUP BY role ORDER BY role;

-- 8.3 迁移前后总数必须相等（第 4 步可能少量增加 owner 行，其余不应变化）
-- SELECT COUNT(*) AS member_total FROM jj_space_member;

-- 8.4 不应存在 owner_id 无成员行的空间
--     预期：0 行
-- SELECT s.id, s.owner_id FROM jj_space s
--  WHERE NOT EXISTS (SELECT 1 FROM jj_space_member m
--                    WHERE m.space_id = s.id AND m.user_id = s.owner_id);

-- 8.5 新列就位确认
-- DESCRIBE jj_space_member;
-- DESCRIBE jj_message;
-- SHOW CREATE TABLE jj_invitation;


-- ============================================================================
-- 【第 9 节】回滚（仅在部署代码前发现异常时使用；代码上线后不可直接回滚数值）
--
--   注意：回滚只做 role 数值反向映射，不能撤销 jj_invitation 等新表/新列。
--   新列与空表对旧代码无害（旧实体不引用它们），可保留不清理。
--
-- UPDATE `jj_space_member` sm
--   JOIN `jj_space` s ON s.`id` = sm.`space_id`
--    SET sm.`role` = CASE
--         WHEN sm.`user_id` = s.`owner_id` THEN 1
--         WHEN sm.`role` = 2               THEN 1
--         WHEN sm.`role` = 3               THEN 2
--         WHEN sm.`role` = 4               THEN 3
--         ELSE sm.`role` END;
--
-- 若想连 status/列一起回退：请用第 3 步的备份整库还原，别手工 DROP COLUMN。
-- ============================================================================


-- ============================================================================
-- END OF V3__space_role.sql
-- ============================================================================
