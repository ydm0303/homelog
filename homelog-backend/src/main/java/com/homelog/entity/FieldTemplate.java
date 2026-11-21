package com.homelog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 内置字段模板表（设计文档 v1.2 §6.1 / 落地路线步 3）。
 *
 * <p>以 {@code moduleKey} 关联 {@link ModuleTemplate}，而非 module_id ——
 * 模板 id 与实例化后的模块 id 没有对应关系。本表为<b>只读</b>。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("jj_field_template")
public class FieldTemplate {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("module_key")
    private String moduleKey;

    @TableField("field_key")
    private String fieldKey;

    @TableField("field_label")
    private String fieldLabel;

    @TableField("field_type")
    private String fieldType;

    @TableField("is_required")
    private Integer isRequired;

    @TableField("is_private")
    private Integer isPrivate;

    @TableField("options_json")
    private String optionsJson;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
