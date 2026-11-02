-- ============================================================================
-- 家记 · Flyway 基线迁移脚本 V1__init.sql
-- 适用：MySQL 8.0+
-- 字符集：utf8mb4，排序规则：utf8mb4_0900_ai_ci
-- 表前缀：jj_
-- 说明：
--   1. 单文件可直接 flyway migrate 执行
--   2. 包含全部业务表结构 + 4 个内置预制模块的种子数据（模块+字段）
--   3. 种子数据使用 INSERT IGNORE 保证幂等性（重复跑不报错）
--   4. MVP 范围：单空间（space_id=1），前端隐藏切换器；证件字段仅权限控制，不加密
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 数据库与字符集设置
-- ----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS `jiaji` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `jiaji`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================================
-- 1. 账号与空间
-- ============================================================================

-- 1.1 用户表
DROP TABLE IF EXISTS `jj_user`;
CREATE TABLE `jj_user` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username`      VARCHAR(64)  NOT NULL COMMENT '登录名/邮箱',
    `password`      VARCHAR(128) NOT NULL COMMENT 'BCrypt加密密码',
    `email`         VARCHAR(128) DEFAULT NULL COMMENT '接收提醒邮箱',
    `nick_name`     VARCHAR(64)  DEFAULT NULL COMMENT '昵称/显示名',
    `avatar`        VARCHAR(255) DEFAULT NULL COMMENT '头像相对路径',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

-- 1.2 家庭空间表
DROP TABLE IF EXISTS `jj_space`;
CREATE TABLE `jj_space` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`        VARCHAR(64) NOT NULL COMMENT '空间名称',
    `owner_id`    BIGINT      NOT NULL COMMENT '创建人/管理员用户ID',
    `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_owner` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='家庭空间表';

-- 1.3 空间成员表
DROP TABLE IF EXISTS `jj_space_member`;
CREATE TABLE `jj_space_member` (
    `id`             BIGINT  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `space_id`       BIGINT  NOT NULL COMMENT '空间ID',
    `user_id`        BIGINT  NOT NULL COMMENT '用户ID',
    `role`           TINYINT NOT NULL COMMENT '角色：1=管理员 2=可编辑 3=只读',
    `receive_mail`   TINYINT NOT NULL DEFAULT 1 COMMENT '是否接收邮件提醒：1=是 0=否',
    `created_at`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_space_user` (`space_id`, `user_id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_space_role` (`space_id`, `role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='空间成员表';

-- ============================================================================
-- 2. 模块与字段元数据
-- ============================================================================

-- 2.1 模块表
DROP TABLE IF EXISTS `jj_module`;
CREATE TABLE `jj_module` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `space_id`      BIGINT      NOT NULL COMMENT '所属空间ID',
    `module_key`    VARCHAR(64) NOT NULL COMMENT '模块稳定标识（药品:drug, 待办:todo, 相册:note, 证件:idoc）',
    `name`          VARCHAR(64) NOT NULL COMMENT '显示名称（管理员可改别名）',
    `module_type`   TINYINT     NOT NULL COMMENT '模块类型：1=内置预制 2=自定义表单',
    `is_enable`     TINYINT     NOT NULL DEFAULT 1 COMMENT '是否启用：1=启用 0=禁用（数据保留）',
    `show_in_home`  TINYINT     NOT NULL DEFAULT 1 COMMENT '是否在首页看板展示：1=是 0=否',
    `sort_order`    INT         NOT NULL DEFAULT 0 COMMENT '菜单排序，越小越靠前',
    `config_json`   TEXT        DEFAULT NULL COMMENT '模块级扩展配置(JSON)',
    `created_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_module` (`space_id`, `module_key`),
    KEY `idx_space_enable` (`space_id`, `is_enable`),
    KEY `idx_space_sort` (`space_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模块表（预制+自定义）';

-- 2.2 字段元数据表
DROP TABLE IF EXISTS `jj_field`;
CREATE TABLE `jj_field` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `module_id`     BIGINT       NOT NULL COMMENT '所属模块ID',
    `field_key`     VARCHAR(64)  NOT NULL COMMENT '字段标识（英文，如 drug_name, expire_date）',
    `field_label`   VARCHAR(64)  NOT NULL COMMENT '字段中文标签',
    `field_type`    VARCHAR(32)  NOT NULL COMMENT '字段类型：text/textarea/number/date/image/member/select/multi_select/tag/boolean',
    `is_required`   TINYINT      NOT NULL DEFAULT 0 COMMENT '是否必填：1=是 0=否',
    `is_private`    TINYINT      NOT NULL DEFAULT 0 COMMENT '仅管理员可见：1=是 0=否',
    `options_json`  TEXT         DEFAULT NULL COMMENT '单选/多选选项 JSON，格式：[{"label":"高","value":"high"},...]',
    `sort_order`    INT          NOT NULL DEFAULT 0 COMMENT '字段排序',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_field` (`module_id`, `field_key`),
    KEY `idx_module` (`module_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字段元数据表';

-- ============================================================================
-- 3. 数据存储（EAV + JSON）
-- ============================================================================

-- 3.1 通用记录表
DROP TABLE IF EXISTS `jj_record`;
CREATE TABLE `jj_record` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `space_id`      BIGINT       NOT NULL COMMENT '所属空间ID',
    `module_id`     BIGINT       NOT NULL COMMENT '所属模块ID',
    `title`         VARCHAR(128) DEFAULT NULL COMMENT '记录标题（搜索/列表展示用）',
    `creator_id`    BIGINT       NOT NULL COMMENT '创建者用户ID',
    `is_private`    TINYINT      NOT NULL DEFAULT 0 COMMENT '记录级私密：1=仅管理员可见 0=按权限可见',
    `tags`          VARCHAR(255) DEFAULT NULL COMMENT '标签，逗号分隔',
    `remind_date`   DATE         DEFAULT NULL COMMENT '提醒日期（药品到期/待办截止/证件到期）',
    `data_json`     JSON         NOT NULL COMMENT '字段值存储 {fieldKey: value}，NOT NULL 保证有值',
    `images_json`   JSON         DEFAULT NULL COMMENT '图片URL数组（相对路径：/uploads/{moduleKey}/{recordId}/xxx.jpg）',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_space_module` (`space_id`, `module_id`),
    KEY `idx_remind` (`remind_date`),
    KEY `idx_creator` (`creator_id`),
    KEY `idx_created` (`created_at`),
    KEY `idx_space_module_remind` (`space_id`, `module_id`, `remind_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通用记录表（EAV+JSON）';

-- ============================================================================
-- 4. 提醒与消息
-- ============================================================================

-- 4.1 消息中心表
DROP TABLE IF EXISTS `jj_message`;
CREATE TABLE `jj_message` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `space_id`    BIGINT       NOT NULL COMMENT '所属空间ID',
    `user_id`     BIGINT       NOT NULL COMMENT '接收人用户ID',
    `record_id`   BIGINT       DEFAULT NULL COMMENT '关联记录ID',
    `content`     VARCHAR(255) NOT NULL COMMENT '消息内容',
    `is_read`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读：1=已读 0=未读',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`, `is_read`),
    KEY `idx_space_user` (`space_id`, `user_id`),
    KEY `idx_record` (`record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='消息中心表';

-- ============================================================================
-- 5. MVP 专用：预制模块种子数据（内置 4 个模块 + 字段定义）
-- 使用 INSERT IGNORE 保证幂等性（重复执行不报错）
-- 假设默认空间 space_id = 1（用户注册后自动创建）
-- ============================================================================

-- 5.1 插入默认空间（仅用于种子数据关联，实际业务由注册流程创建）
INSERT IGNORE INTO `jj_space` (`id`, `name`, `owner_id`, `created_at`)
VALUES (1, '我的家庭', 1, CURRENT_TIMESTAMP);

-- 5.2 插入预制模块（module_type=1 表示内置预制）
-- 模块 key 固定：drug(药品档案)、todo(家庭待办)、note(生活记事相册)、idoc(证件档案)
INSERT IGNORE INTO `jj_module` (`id`, `space_id`, `module_key`, `name`, `module_type`, `is_enable`, `show_in_home`, `sort_order`, `config_json`, `created_at`)
VALUES
    (1, 1, 'drug',   '药品档案',   1, 1, 1, 10, NULL, CURRENT_TIMESTAMP),
    (2, 1, 'todo',   '家庭待办',   1, 1, 1, 20, NULL, CURRENT_TIMESTAMP),
    (3, 1, 'note',   '生活记事相册', 1, 1, 1, 30, NULL, CURRENT_TIMESTAMP),
    (4, 1, 'idoc',   '证件档案',   1, 1, 1, 40, NULL, CURRENT_TIMESTAMP);

-- 5.3 插入药品档案模块字段 (module_id=1)
INSERT IGNORE INTO `jj_field` (`id`, `module_id`, `field_key`, `field_label`, `field_type`, `is_required`, `is_private`, `options_json`, `sort_order`)
VALUES
    (1,  1, 'drug_name',      '药品名',        'text',        1, 0, NULL, 10),
    (2,  1, 'expire_date',    '有效期',        'date',        1, 0, NULL, 20),
    (3,  1, 'location',       '存放位置',      'text',        0, 0, NULL, 30),
    (4,  1, 'usage_dosage',   '用法用量',      'text',        0, 0, NULL, 40),
    (5,  1, 'indications',    '适用症状',      'text',        0, 0, NULL, 50),
    (6,  1, 'contraindication','禁忌',         'textarea',    0, 0, NULL, 60),
    (7,  1, 'purchase_date',  '购买日期',      'date',        0, 0, NULL, 70),
    (8,  1, 'quantity',       '剩余数量',      'number',      0, 0, NULL, 80),
    (9,  1, 'member_id',      '绑定成员',      'member',      0, 0, NULL, 90),
    (10, 1, 'tags',           '标签',          'tag',         0, 0, NULL, 100),
    (11, 1, 'is_private',     '是否私密',      'boolean',     0, 0, NULL, 110);

-- 5.4 插入家庭待办模块字段 (module_id=2)
INSERT IGNORE INTO `jj_field` (`id`, `module_id`, `field_key`, `field_label`, `field_type`, `is_required`, `is_private`, `options_json`, `sort_order`)
VALUES
    (20, 2, 'title',          '标题',          'text',        1, 0, NULL, 10),
    (21, 2, 'due_date',       '截止日期',      'date',        0, 0, NULL, 20),
    (22, 2, 'assignee_id',    '分配给',        'member',      0, 0, NULL, 30),
    (23, 2, 'priority',       '优先级',        'select',      0, 0, '[{"label":"高","value":"high"},{"label":"中","value":"medium"},{"label":"低","value":"low"}]', 40),
    (24, 2, 'repeat_rule',    '循环规则',      'text',        0, 0, NULL, 50),
    (25, 2, 'remark',         '备注',          'textarea',    0, 0, NULL, 60),
    (26, 2, 'tags',           '标签',          'tag',         0, 0, NULL, 70),
    (27, 2, 'is_private',     '是否私密',      'boolean',     0, 0, NULL, 80);

-- 5.5 插入生活记事相册模块字段 (module_id=3)
INSERT IGNORE INTO `jj_field` (`id`, `module_id`, `field_key`, `field_label`, `field_type`, `is_required`, `is_private`, `options_json`, `sort_order`)
VALUES
    (30, 3, 'title',          '标题',          'text',        1, 0, NULL, 10),
    (31, 3, 'content',        '内容',          'textarea',    0, 0, NULL, 20),
    (32, 3, 'images',         '图片',          'image',       0, 0, NULL, 30),
    (33, 3, 'shoot_date',     '拍摄日期',      'date',        0, 0, NULL, 40),
    (34, 3, 'location',       '地点',          'text',        0, 0, NULL, 50),
    (35, 3, 'tags',           '标签',          'tag',         0, 0, NULL, 60),
    (36, 3, 'is_private',     '是否私密',      'boolean',     0, 0, NULL, 70);

-- 5.6 插入证件档案模块字段 (module_id=4)
-- 注意：证件号字段 is_private=1（仅管理员可见），MVP 不做字段加密，仅权限控制
INSERT IGNORE INTO `jj_field` (`id`, `module_id`, `field_key`, `field_label`, `field_type`, `is_required`, `is_private`, `options_json`, `sort_order`)
VALUES
    (40, 4, 'doc_type',       '证件类型',      'select',      1, 0,
        '[{"label":"身份证","value":"id_card"},{"label":"护照","value":"passport"},{"label":"驾驶证","value":"driver_license"},{"label":"医保卡","value":"medical_insurance"},{"label":"保单","value":"policy"},{"label":"其他","value":"other"}]', 10),
    (41, 4, 'doc_number',     '证件号',        'text',        1, 1, NULL, 20),
    (42, 4, 'holder_id',      '持有人',        'member',      1, 0, NULL, 30),
    (43, 4, 'issue_date',     '签发日期',      'date',        0, 0, NULL, 40),
    (44, 4, 'expire_date',    '有效期',        'date',        0, 0, NULL, 50),
    (45, 4, 'issue_authority','发证机关',      'text',        0, 0, NULL, 60),
    (46, 4, 'remark',         '备注',          'textarea',    0, 0, NULL, 70),
    (47, 4, 'scan_image',     '扫描件',        'image',       0, 0, NULL, 80),
    (48, 4, 'tags',           '标签',          'tag',         0, 0, NULL, 90),
    (49, 4, 'is_private',     '是否私密',      'boolean',     0, 0, NULL, 100);

-- ============================================================================
-- 6. 还原外键检查
-- ============================================================================
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- DBA 审核意见（请在 Code Review 时逐项确认）
-- ============================================================================
-- DBA 审核意见:
--
-- 1. 索引完整性
--    ✅ jj_user: uk_username(唯一登录), idx_email(邮箱查询)
--    ✅ jj_space: idx_owner(创建人反查)
--    ✅ jj_space_member: uk_space_user(防重复加入), idx_user(用户反查空间), idx_space_role(空间内按角色查)
--    ✅ jj_module: uk_module(空间内模块 key 唯一), idx_space_enable(启用模块列表), idx_space_sort(菜单排序)
--    ✅ jj_field: uk_field(模块内字段 key 唯一), idx_module(模块加载字段)
--    ✅ jj_record: idx_space_module(核心列表查询), idx_remind(到期扫描), idx_creator(我的记录),
--           idx_created(时间倒序), idx_space_module_remind(模块内按提醒日期查)
--    ✅ jj_message: idx_user(用户未读列表), idx_space_user(空间内用户消息), idx_record(记录关联消息)
--    结论：覆盖所有高频查询模式，避免全表扫描。MVP 数据量小（家庭级），索引维护成本可接受。
--
-- 2. 字段类型合理性
--    ✅ VARCHAR 长度：username/email 64/128 足够；module_key/field_key 64 支持英文标识；
--    ✅ JSON 字段：data_json 存储动态字段值，NOT NULL 保证有值；images_json 存相对路径数组；
--    ✅ field_type 使用 VARCHAR(32) 存枚举字符串（text/date/image/...），便于扩展；
--    ✅ module_type/role/is_private 等使用 TINYINT，节省空间；
--    ✅ tags 使用 VARCHAR(255) 逗号分隔，家庭场景标签数少，满足需求；如需高性能可后期改关联表。
--    结论：类型选择合理，兼顾扩展性与存储效率。
--
-- 3. 外键/约束
--    ⚠️ 未显式定义 FOREIGN KEY：考虑到 MyBatis-Plus/JPA 代码层维护引用完整性，
--       且家庭场景写入并发低、数据量小，显式 FK 会增加迁移/回滚/测试数据准备复杂度。
--       代码层已保证：space_id 隔离、module_id 归属校验、creator_id 存在性校验。
--    ✅ UNIQUE 约束充分：uk_username、uk_space_user、uk_module、uk_field 均防止重复脏数据。
--    结论：MVP 阶段以应用层约束为主，数据库层保留唯一约束；v1.1 可按需补 FK。
--
-- 4. 分区/归档策略
--    ⚠️ jj_record 暂不分区：MVP 预估单家庭 3 年 < 5 万行，单表性能足够。
--       预留方案：按年/月 RANGE 分区 remind_date 或 created_at；归档策略：迁移 3 年前数据到 jj_record_archive 表。
--    结论：当前不分区，预留 idx_remind/idx_created 支持未来分区裁剪。
--
-- 5. 字符集/排序规则
--    ✅ 数据库/表/列统一 utf8mb4 + utf8mb4_0900_ai_ci
--       - 支持 Emoji、生僻字
--       - _ai_ci 不区分重音/大小写，适配中文搜索
--    结论：统一规范，无乱码风险。
--
-- 6. 种子数据幂等性
--    ✅ 所有 INSERT 均使用 INSERT IGNORE，重复执行不报错、不重复插入。
--    ✅ 模块/字段 ID 显式指定，保证跨环境 ID 一致（便于前端硬编码 key 映射）。
--    结论：满足 Flyway 重复迁移、多环境部署幂等要求。
--
-- 7. MVP 范围边界确认
--    ✅ 无多空间切换表/UI：jj_space 仅 1 条默认数据，前端隐藏切换器。
--    ✅ 无证件加密字段：jj_field.is_private=1 做权限控制，无加密列、无密钥管理表。
--    ✅ 无通用导入相关表：仅药品模块 CSV 导入（后端硬编码映射），无导入任务/映射/错误表。
--    ✅ 无浏览器推送表：仅消息中心 jj_message + 邮件发送记录（可扩展日志表）。
--    结论：严格对齐 PRD MVP 范围，无过度设计。
--
-- ============================================================================
-- END OF V1__init.sql
-- ============================================================================