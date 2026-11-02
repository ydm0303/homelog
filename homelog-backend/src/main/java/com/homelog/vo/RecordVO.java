package com.homelog.vo;

import com.homelog.entity.Record;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 记录响应
 */
@Data
public class RecordVO {

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
    private LocalDate remindDate;
    private Map<String, Object> dataJson;
    private List<String> imagesJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RecordVO from(Record record, String moduleKey, String moduleName, String creatorName) {
        RecordVO vo = new RecordVO();
        vo.setId(record.getId());
        vo.setSpaceId(record.getSpaceId());
        vo.setModuleId(record.getModuleId());
        vo.setModuleKey(moduleKey);
        vo.setModuleName(moduleName);
        vo.setTitle(record.getTitle());
        vo.setCreatorId(record.getCreatorId());
        vo.setCreatorName(creatorName);
        vo.setIsPrivate(record.getIsPrivate());
        vo.setTags(record.getTags());
        vo.setRemindDate(record.getRemindDate());
        vo.setDataJson(parseJson(record.getDataJson()));
        vo.setImagesJson(parseJsonArray(record.getImagesJson()));
        vo.setCreatedAt(record.getCreatedAt());
        vo.setUpdatedAt(record.getUpdatedAt());
        return vo;
    }

    private static Map<String, Object> parseJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new java.util.HashMap<>();
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            return new java.util.HashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> parseJsonArray(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new java.util.ArrayList<>();
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, List.class);
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }
}