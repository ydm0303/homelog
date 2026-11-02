package com.homelog.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * 记录创建请求
 */
@Data
public class RecordCreateRequest {

    @NotNull(message = "模块ID不能为空")
    private Long moduleId;

    private String title;

    @NotNull(message = "字段数据不能为空")
    private Map<String, Object> dataJson;

    private List<String> imagesJson;

    private String tags;

    private String remindDate;
}