package com.homelog.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 消息查询请求参数
 */
@Data
@Accessors(chain = true)
public class MessageQueryRequest {

    /**
     * 页码，从 1 开始
     */
    private Integer page = 1;

    /**
     * 每页大小
     */
    private Integer size = 20;

    /**
     * 读取状态：0=未读，1=已读，null=全部
     */
    private Integer isRead;

    /**
     * 关键词搜索
     */
    private String keyword;

    /**
     * 开始日期
     */
    private String startDate;

    /**
     * 结束日期
     */
    private String endDate;
}