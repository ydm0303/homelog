package com.homelog.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 发起空间邀请请求（步 4）。
 *
 * <p>注意没有 token / 链接相关字段：按决策 D2，邀请是服务端记录，
 * 被邀请人从站内通知应答，不接受客户端传入邀请人档位等可提权信息。
 */
@Data
public class InvitationCreateRequest {

    /** 被邀请人的用户名或邮箱（多标识，与登录一致）；格式先行拦截，真实性由后端查库校验 */
    @NotBlank(message = "请输入对方的账号或注册邮箱")
    @Pattern(
            regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$|^[A-Za-z0-9_-]{3,32}$",
            message = "格式不正确：请输入合法的用户名（字母/数字/_/-，3~32位）或邮箱地址"
    )
    private String identifier;

    /** 授予角色：2=admin 3=member 4=readonly（owner 不可邀请产生） */
    @NotNull(message = "角色不能为空")
    private Integer role;
}
