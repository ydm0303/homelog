package com.homelog.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 邀请应答请求（接受 / 拒绝 / 撤销共用），凭 invitationId 定位邀请。
 *
 * <p>服务端强制校验身份：accept/reject 要求 {@code invitee_id == 当前登录用户}，
 * cancel 要求 {@code inviter_id == 当前登录用户}，且邀请须仍为待处理。
 * 不接受客户端传入角色等字段——授予角色在发起时已由服务端确定（§3 服务端权威原则）。
 */
@Data
public class InvitationRespondRequest {

    @NotNull(message = "邀请ID不能为空")
    private Long invitationId;
}
