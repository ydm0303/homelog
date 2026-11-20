-- ============================================================================
-- 家记 · 增量迁移 V4__space_templates.sql      【落地路线 步 3 · 数据侧】
-- 设计依据：家记账号角色与家庭空间设计.md v1.2 §6.1 / §16 Q1
--
-- 解决什么：
--   注册要「建私有空间」，但新空间必须是<可用的>家 —— 首页要能看到药品/待办/相册/证件
--   四个模块及其字段。现状是这些元数据只存在于 space_id=1（见 V1__init.sql:170-175），
--   这正是 AuthController.java:112-114 那条注释说的「另建空间会导致菜单与数据全部落空」。
--
--   本脚本建立<b>模板表</b>作为内置模块/字段的权威来源，注册时按模板为新空间实例化。
--
-- 为什么是独立模板表，而不是「从 space 1 复制」或「写在 Java 常量里」：
--   · 从 space 1 复制：space 1 现在是一个真实的家（owner=user 1）。若该家庭把
--     「药品档案」改名为「我家药箱」或禁用了相册模块，新建的家庭会继承这些私人改动。错。
--   · Java 常量：内置模块的 canonical 字段集是 V1 + V2 的并集（药品模块后来加了
--     unit / min_stock 两个字段）。常量表要求代码与迁移脚本长期同步，且新增内置模块
--     必须改代码重新发布，DBA 无法自行维护。
--   · 独立模板表：与任何真实空间解耦、不可能泄漏进用户数据、数据驱动可维护。
--
-- 前置：schema 本身已支持每空间一套内置模块 ——
--   jj_module 唯一键是 uk_module(space_id, module_key)、jj_field 是 uk_field(module_id, field_key)，
--   均为<b>空间维度</b>唯一，V1 把模块钉在 space_id=1 只是种子数据的偶然。
--
-- 幂等：模板表建表 + 引导插入均包裹在存储过程内，判空后才灌数据，可重复执行。
--
-- ⚠️ 执行顺序：V3 必须先于 V4（本脚本不依赖 V3 的列，但同属一轮改造，保持顺序便于回滚）
-- ⚠️ 本脚本不改动任何用户数据，风险低于 V3；不执行本脚本时，注册新账号仍走旧逻辑（加入 space 1）。
-- ============================================================================

USE `homelog`;

-- ============================================================================
-- 【第 1 步】模块模板表
--   字段与 jj_module 对齐，仅去掉 id/space_id（实例化时由自增与新空间填充）。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `jj_module_template` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `module_key`    VARCHAR(64) NOT NULL COMMENT '模块稳定标识（drug/todo/note/idoc）',
    `name`          VARCHAR(64) NOT NULL COMMENT '默认显示名称',
    `module_type`   TINYINT     NOT NULL DEFAULT 1 COMMENT '模块类型：内置预制=1',
    `is_enable`     TINYINT     NOT NULL DEFAULT 1 COMMENT '新空间内是否默认启用',
    `show_in_home`  TINYINT     NOT NULL DEFAULT 1 COMMENT '是否默认在首页看板展示',
    `sort_order`    INT         NOT NULL DEFAULT 0 COMMENT '默认菜单排序',
    `config_json`   TEXT        DEFAULT NULL COMMENT '默认模块级扩展配置(JSON)',
    `created_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_module_key` (`module_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内置模块模板表（新空间实例化来源）';

-- ============================================================================
-- 【第 2 步】字段模板表
--   以 module_key 关联模板模块（不用 module_id，模板与实例的 id 无对应关系）。
-- ============================================================================
CREATE TABLE IF NOT EXISTS `jj_field_template` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `module_key`    VARCHAR(64) NOT NULL COMMENT '所属模板模块标识',
    `field_key`     VARCHAR(64) NOT NULL COMMENT '字段标识（英文，如 drug_name）',
    `field_label`   VARCHAR(64) NOT NULL COMMENT '字段中文标签',
    `field_type`    VARCHAR(32) NOT NULL COMMENT '字段类型：text/textarea/number/date/image/member/select/multi_select/tag/boolean',
    `is_required`   TINYINT     NOT NULL DEFAULT 0 COMMENT '是否必填：1=是 0=否',
    `is_private`    TINYINT     NOT NULL DEFAULT 0 COMMENT '管理档（owner/admin）专属可见：1=是 0=否',
    `options_json`  TEXT        DEFAULT NULL COMMENT '单选/多选选项 JSON',
    `sort_order`    INT         NOT NULL DEFAULT 0 COMMENT '字段排序',
    `created_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_field` (`module_key`, `field_key`),
    KEY `idx_template_module` (`module_key`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内置字段模板表（新空间实例化来源）';

-- ============================================================================
-- 【第 3 步】从现存内置模块一次性引导模板数据
--   仅在模板表为空时执行 —— 之后模板表即为独立权威来源，
--   不再随 space 1 的变化而变动。
--
--   取 space_id=1 AND module_type=1：即四个内置模块（自定义表单 module_type=2 不进入模板）。
--   jj_field 无 space_id，按 module_id 关联取，自动包含 V2 追加的 unit / min_stock。
-- ============================================================================
DROP PROCEDURE IF EXISTS `sp_homelog_v4_bootstrap_templates`;

DELIMITER $$
CREATE PROCEDURE `sp_homelog_v4_bootstrap_templates`()
BEGIN
    DECLARE v_tpl_modules INT DEFAULT 0;
    DECLARE v_source_ok   INT DEFAULT 0;

    SELECT COUNT(*) INTO v_tpl_modules FROM `jj_module_template`;

    -- 源数据必须存在：全新库（只跑了 V1）里 space 1 的四个内置模块是种子数据，必然在；
    -- 若被人删空则跳过引导，交由人工插入模板，避免静默造出「没有内置模块」的模板表。
    SELECT COUNT(*) INTO v_source_ok
      FROM `jj_module`
     WHERE `space_id` = 1 AND `module_type` = 1;

    IF v_tpl_modules = 0 AND v_source_ok > 0 THEN

        INSERT INTO `jj_module_template`
            (`module_key`, `name`, `module_type`, `is_enable`, `show_in_home`, `sort_order`, `config_json`)
        SELECT m.`module_key`, m.`name`, m.`module_type`, m.`is_enable`,
               m.`show_in_home`, m.`sort_order`, m.`config_json`
          FROM `jj_module` m
         WHERE m.`space_id` = 1
           AND m.`module_type` = 1
         ORDER BY m.`sort_order`, m.`id`;

        INSERT INTO `jj_field_template`
            (`module_key`, `field_key`, `field_label`, `field_type`,
             `is_required`, `is_private`, `options_json`, `sort_order`)
        SELECT m.`module_key`, f.`field_key`, f.`field_label`, f.`field_type`,
               f.`is_required`, f.`is_private`, f.`options_json`, f.`sort_order`
          FROM `jj_field` f
          JOIN `jj_module` m ON m.`id` = f.`module_id`
         WHERE m.`space_id` = 1
           AND m.`module_type` = 1
         ORDER BY m.`sort_order`, f.`sort_order`, f.`id`;

    END IF;
END$$
DELIMITER ;

CALL `sp_homelog_v4_bootstrap_templates`();
DROP PROCEDURE IF EXISTS `sp_homelog_v4_bootstrap_templates`;


-- ============================================================================
-- 【第 4 节】验证 SQL（执行后手工跑，预期结果已标注）
-- ============================================================================

-- 4.1 模板模块应为 4 个：drug / todo / note / idoc
-- SELECT module_key, name, module_type, sort_order FROM jj_module_template ORDER BY sort_order;

-- 4.2 模板字段总数预期 38：药品 13（含 V2 的 unit/min_stock）、待办 8、相册 7、证件 10
-- SELECT module_key, COUNT(*) AS field_count
--   FROM jj_field_template GROUP BY module_key ORDER BY module_key;

-- 4.3 私密字段应只有证件号一条（idoc.doc_number, is_private=1）
-- SELECT module_key, field_key, field_label, is_private
--   FROM jj_field_template WHERE is_private = 1;

-- 4.4 与 space 1 现状逐字段比对，确认引导无遗漏（预期 0 行差异）
-- SELECT 'only_in_space1' AS side, m.module_key, f.field_key
--   FROM jj_field f JOIN jj_module m ON m.id=f.module_id
--  WHERE m.space_id=1 AND m.module_type=1
--  EXCEPT
-- SELECT 'only_in_template', t.module_key, t.field_key FROM jj_field_template t
--   UNION ALL
-- SELECT 'only_in_template', t.module_key, t.field_key
--   FROM jj_field_template t
--  WHERE NOT EXISTS (SELECT 1 FROM jj_field f JOIN jj_module m ON m.id=f.module_id
--                    WHERE m.space_id=1 AND m.module_type=1
--                      AND m.module_key=t.module_key AND f.field_key=t.field_key);
--   （MySQL 8 支持 EXCEPT；若版本不支持，用上面 NOT EXISTS 那半段单独查即可）


-- ============================================================================
-- 【第 5 节】代码侧配套（本脚本生效的前提）
--
--   新增：
--     entity/ModuleTemplate.java     entity/FieldTemplate.java
--     mapper/ModuleTemplateMapper.java  mapper/FieldTemplateMapper.java
--     service/SpaceSeedService.java     —— 为新空间实例化内置模块与字段
--   改动：
--     controller/AuthController.java —— 注册改为「建私有空间 + owner + 实例化模板」，
--                                       不再加入 space 1，并删除 DEFAULT_SPACE_ID 常量
--   注意：模板表是<b>只读</b>的，业务代码不提供模板增删改接口。
-- ============================================================================

-- ============================================================================
-- 【第 6 节】回滚
--   模板表不影响任何用户数据，直接 DROP 即可；已注册的老账号不受影响。
-- DROP TABLE IF EXISTS `jj_field_template`;
-- DROP TABLE IF EXISTS `jj_module_template`;
-- ============================================================================

-- ============================================================================
-- END OF V4__space_templates.sql
-- ============================================================================
