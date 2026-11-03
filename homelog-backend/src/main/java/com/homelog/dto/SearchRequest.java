package com.homelog.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 搜索请求参数
 */
@Data
@Accessors(chain = true)
public class SearchRequest {

    /**
     * 搜索关键词
     */
    private String q;

    /**
     * 模块 ID 筛选
     */
    private Long moduleId;

    /**
     * 标签筛选
     */
    private String tag;

    /**
     * 页码，从 1 开始
     */
    private Integer page = 1;

    /**
     * 每页大小
     */
    private Integer size = 20;

    /**
     * 开始日期
     */
    private String startDate;

    /**
     * 结束日期
     */
    private String endDate;
}