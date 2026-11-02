package com.homelog.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 模块更新请求
 */
@Data
public class ModuleUpdateRequest {

    @NotNull(message = "模块ID不能为空")
    private Long moduleId;

    private String name;

    private Integer isEnable;

    private Integer showInHome;

    private Integer sortOrder;

    private String configJson;
}