package com.homelog.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 空间成员删除请求
 */
@Data
public class SpaceMemberDeleteRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;
}