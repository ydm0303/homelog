package com.homelog.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 模块创建请求
 */
@Data
public class ModuleCreateRequest {

    @NotBlank(message = "模块名称不能为空")
    private String name;

    @NotBlank(message = "模块 Key 不能为空")
    private String moduleKey;

    private Integer sortOrder;
}