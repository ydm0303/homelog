package com.homelog.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 修改空间名称请求（设计文档 v1.2 §6.5 / §10）。
 */
@Data
public class SpaceUpdateRequest {

    @NotBlank(message = "空间名称不能为空")
    @Size(max = 64, message = "空间名称不能超过 64 个字符")
    private String name;
}
