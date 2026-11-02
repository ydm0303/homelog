package com.homelog.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 空间成员创建请求
 */
@Data
public class SpaceMemberCreateRequest {

    @NotNull(message = "邮箱不能为空")
    private String email;

    @NotNull(message = "角色不能为空")
    private Integer role; // 1=admin, 2=edit, 3=readonly

    private Integer receiveMail = 1;
}