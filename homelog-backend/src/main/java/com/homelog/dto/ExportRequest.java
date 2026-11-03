package com.homelog.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 导出请求参数
 */
@Data
@Accessors(chain = true)
public class ExportRequest {

    /**
     * 导出范围：module=单模块，all=全空间
     */
    private String scope;

    /**
     * 模块 ID（scope=module 时必填）
     */
    private Long moduleId;
}