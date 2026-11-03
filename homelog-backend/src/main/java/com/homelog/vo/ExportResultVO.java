package com.homelog.vo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 导出结果视图对象
 */
@Data
@Accessors(chain = true)
public class ExportResultVO {

    /**
     * 导出文件下载路径
     */
    private String downloadUrl;

    /**
     * 文件名
     */
    private String filename;

    /**
     * 文件大小（字节）
     */
    private long fileSize;

    /**
     * 导出记录数
     */
    private int recordCount;
}