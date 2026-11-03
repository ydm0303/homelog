package com.homelog.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 导入结果视图对象
 */
@Data
@Accessors(chain = true)
public class ImportResultVO {

    /**
     * 总行数（含表头）
     */
    private int totalRows;

    /**
     * 成功导入行数
     */
    private int successRows;

    /**
     * 失败行数
     */
    private int failedRows;

    /**
     * 错误详情
     */
    private List<ImportErrorVO> errors;

    /**
     * 错误 CSV 下载路径（仅预检模式返回）
     */
    private String errorCsvPath;
}