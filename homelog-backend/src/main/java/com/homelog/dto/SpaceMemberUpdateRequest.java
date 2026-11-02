package com.homelog.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 空间成员更新请求
 */
@Data
public class SpaceMemberUpdateRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    private Integer role; // 1=admin, 2=edit, 3=readonly

    private Integer receiveMail;
}