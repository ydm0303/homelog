package com.homelog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 字段元数据表
 * 字段类型：text/textarea/number/date/image/member/select/multi_select/tag/boolean
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("jj_field")
public class Field {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("module_id")
    private Long moduleId;

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