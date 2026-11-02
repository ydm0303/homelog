package com.homelog.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 记录详情请求
 */
@Data
public class RecordDetailRequest {

    @NotNull(message = "记录ID不能为空")
    private Long recordId;

    @NotNull(message = "模块ID不能为空")
    private Long moduleId;
}