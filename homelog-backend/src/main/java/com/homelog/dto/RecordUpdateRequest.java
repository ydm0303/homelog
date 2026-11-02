package com.homelog.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * 记录更新请求
 */
@Data
public class RecordUpdateRequest {

    @NotNull(message = "记录ID不能为空")
    private Long recordId;

    @NotNull(message = "模块ID不能为空")
    private Long moduleId;

    private String title;

    private Map<String, Object> dataJson;

    private List<String> imagesJson;

    private String tags;

    private String remindDate;
}