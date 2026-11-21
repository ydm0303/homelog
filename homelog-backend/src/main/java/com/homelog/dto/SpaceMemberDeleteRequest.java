package com.homelog.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 空间成员删除请求（设计文档 v1.2 §6.7）。
 *
 * <p>移除成员会连带删除该成员在本空间创建的内容与图片，属破坏性操作，
 * 必须显式携带 {@code confirm=true} 才执行。
 */
@Data
public class SpaceMemberDeleteRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /** 二次确认标志：必须为 true 才会执行连带删除 */
    private Boolean confirm;
}
