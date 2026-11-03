package com.homelog.vo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 导入错误详情
 */
@Data
@Accessors(chain = true)
public class ImportErrorVO {

    /**
     * 行号（从 1 开始，含表头）
     */
    private int rowNum;

    /**
     * 原始行数据
     */
    private String[] rowData;

    /**
     * 错误信息
     */
    private String errorMessage;
}