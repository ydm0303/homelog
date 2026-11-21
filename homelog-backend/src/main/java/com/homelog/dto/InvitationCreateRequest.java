package com.homelog.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 发起空间邀请请求（步 4）。
 *
 * <p>注意没有 token / 链接相关字段：按决策 D2，邀请是服务端记录，
 * 被邀请人从站内通知应答，不接受客户端传入邀请人档位等可提权信息。
 */
@Data
public class InvitationCreateRequest {

    @NotBlank(message = "邮箱不能为空")
    private String email;

    /** 授予角色：2=admin 3=member 4=readonly（owner 不可邀请产生） */
    @NotNull(message = "角色不能为空")
    private Integer role;
}
