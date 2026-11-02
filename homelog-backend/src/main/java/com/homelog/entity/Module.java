package com.homelog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 模块表
 * 模块类型：1=内置预制 2=自定义表单
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("jj_module")
public class Module {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("space_id")
    private Long spaceId;

    @TableField("module_key")
    private String moduleKey;

    @TableField("name")
    private String name;

    @TableField("module_type")
    private Integer moduleType;

    @TableField("is_enable")
    private Integer isEnable;

    @TableField("show_in_home")
    private Integer showInHome;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("config_json")
    private String configJson;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}