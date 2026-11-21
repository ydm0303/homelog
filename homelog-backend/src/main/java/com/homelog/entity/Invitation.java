package com.homelog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 家庭空间邀请表（设计文档 v1.2 §5.2 / 落地路线步 4）。
 *
 * <p>按决策 D2「邀请去掉 token 链接层」：没有 token / expires_at 列，
 * 邀请完全走站内通知承载，被邀请人凭 invitationId 接受或拒绝，
 * 服务端强制校验 {@code invitee_id == 当前登录用户}。
 *
 * <p>邀请必须指定系统内已存在的账号（决策：不支持匿名链接），
 * invitee_id 在发起时即解析邮箱落库，避免接受时再查。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("jj_invitation")
public class Invitation {

    /** 待处理 */
    public static final int STATUS_PENDING = 1;
    /** 已接受 */
    public static final int STATUS_ACCEPTED = 2;
    /** 已拒绝 */
    public static final int STATUS_REJECTED = 3;
    /** 已撤销（邀请人取消，或因邀请人被移除/降权、空间解散而级联撤销） */
    public static final int STATUS_CANCELLED = 4;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("space_id")
    private Long spaceId;

    @TableField("inviter_id")
    private Long inviterId;

    @TableField("invitee_id")
    private Long inviteeId;

    /** 授予角色：2=admin 3=member 4=readonly（owner 永不通过邀请产生） */
    @TableField("role")
    private Integer role;

    /** 1=pending 2=accepted 3=rejected 4=cancelled */
    @TableField("status")
    private Integer status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 应答时间（接受/拒绝/撤销时写入） */
    @TableField("responded_at")
    private LocalDateTime respondedAt;
}
