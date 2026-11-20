package com.homelog.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 空间邀请 VO（步 4）。
 *
 * <p>不暴露被邀请人邮箱等个人信息给邀请方之外的视角；
 * inviteeNickname / inviterNickname 由 service 用 User 表补齐，供前端文案直接渲染
 * 「<b>张三</b> 邀请你加入 <b>【张家】</b>，身份：普通成员」。
 */
@Data
@Accessors(chain = true)
public class InvitationVO {

    private Long id;
    private Long spaceId;
    private String spaceName;

    private Long inviterId;
    private String inviterNickname;

    private Long inviteeId;
    private String inviteeNickname;

    /** 授予角色：2=admin 3=member 4=readonly */
    private Integer role;
    private String roleLabel;

    /** 1=pending 2=accepted 3=rejected 4=cancelled */
    private Integer status;
    private String statusLabel;

    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
}
