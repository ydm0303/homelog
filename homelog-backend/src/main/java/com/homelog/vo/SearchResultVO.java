package com.homelog.vo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 搜索结果视图对象
 */
@Data
@Accessors(chain = true)
public class SearchResultVO {

    private Long id;
    private Long spaceId;
    private Long moduleId;
    private String moduleKey;
    private String moduleName;
    private String title;
    private Long creatorId;
    private String creatorName;
    private Integer isPrivate;
    private String tags;
    private String remindDate;
    private String dataJson;
    private String imagesJson;
    private String createdAt;
    private String updatedAt;

    // 高亮片段（用于前端显示匹配上下文）
    private String highlightSnippet;
}