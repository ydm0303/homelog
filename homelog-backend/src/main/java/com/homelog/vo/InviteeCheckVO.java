package com.homelog.vo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 邀请预检结果（发起前真实性校验用）。
 *
 * <p>仅返回脱敏后的可识别信息，供前端给出「确认邀请 xxx？」这类正向反馈；
 * 不暴露密码等敏感字段。{@code exists=false} 时 {@link #nickname}/{@link #email} 为空。
 */
@Data
@Accessors(chain = true)
public class InviteeCheckVO {

    /** 标识是否对应一个已注册账号 */
    private boolean exists;

    /** 命中账号的展示名（昵称优先，回退用户名） */
    private String nickname;

    /** 命中账号的邮箱（脱敏，如 ab***@qq.com），用于让邀请人核对是否本人 */
    private String email;
}
