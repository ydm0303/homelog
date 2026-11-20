package com.homelog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 空间成员表
 * 角色：1=owner 所有者 2=admin 管理员 3=member 普通成员 4=readonly 只读
 * 状态：1=active 在籍 2=left 已退出 3=removed 已移除
 * 档位语义统一见 {@code com.homelog.security.Role}
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("jj_space_member")
public class SpaceMember {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("space_id")
    private Long spaceId;

    @TableField("user_id")
    private Long userId;

    @TableField("role")
    private Integer role;

    /** 在籍状态：1=active 2=left 3=removed */
    @TableField("status")
    private Integer status;

    /** 该空间内显示名 */
    @TableField("nickname")
    private String nickname;

    @TableField("receive_mail")
    private Integer receiveMail;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}