package com.homelog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 内置模块模板表（设计文档 v1.2 §6.1 / 落地路线步 3）。
 *
 * <p>新空间创建时按本表实例化内置模块，使模板与任何真实空间解耦：
 * 某家庭把「药品档案」改名或禁用，不会影响后来者拿到的默认模块。
 * 本表为<b>只读</b>，业务侧不提供增删改接口。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("jj_module_template")
public class ModuleTemplate {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

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
