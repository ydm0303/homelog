-- ============================================================================
-- 家记 · Flyway 增量迁移 V2__drug_fields.sql
-- 内容：为药品档案模块（module_id=1）补充"单位"与"最低库存"两个字段
-- 幂等：INSERT IGNORE，重复执行不报错、不重复插入
-- ============================================================================

USE `homelog`;

INSERT IGNORE INTO `jj_field`
    (`id`, `module_id`, `field_key`, `field_label`, `field_type`, `is_required`, `is_private`, `options_json`, `sort_order`)
VALUES
    (12, 1, 'unit',      '单位',      'select', 0, 0,
        '[{"label":"盒","value":"盒"},{"label":"瓶","value":"瓶"},{"label":"袋","value":"袋"},{"label":"支","value":"支"},{"label":"粒","value":"粒"},{"label":"贴","value":"贴"}]', 85),
    (13, 1, 'min_stock', '最低库存',  'number', 0, 0, NULL, 86);

-- ============================================================================
-- END OF V2__drug_fields.sql
-- ============================================================================
