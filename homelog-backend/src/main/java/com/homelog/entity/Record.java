package com.homelog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 通用记录表 (EAV + JSON)
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("jj_record")
public class Record {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("space_id")
    private Long spaceId;

    @TableField("module_id")
    private Long moduleId;

    @TableField("title")
    private String title;

    @TableField("creator_id")
    private Long creatorId;

    @TableField("is_private")
    private Integer isPrivate;

    @TableField("tags")
    private String tags;

    @TableField("remind_date")
    private LocalDate remindDate;

    @TableField("data_json")
    private String dataJson;

    @TableField("images_json")
    private String imagesJson;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}