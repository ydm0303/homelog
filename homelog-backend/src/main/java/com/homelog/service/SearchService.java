package com.homelog.service;

import com.homelog.dto.SearchRequest;
import com.homelog.entity.Record;
import com.homelog.entity.Module;
import com.homelog.mapper.RecordMapper;
import com.homelog.mapper.ModuleMapper;
import com.homelog.vo.SearchResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局搜索服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final RecordMapper recordMapper;
    private final ModuleMapper moduleMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取当前空间ID（MVP 单空间）
     */
    private Long getCurrentSpaceId() {
        return 1L;
    }

    /**
     * 获取当前用户角色（简化）
     */
    private Integer getCurrentUserRole() {
        return 1; // 默认管理员，实际应从 SecurityContext 获取
    }

    /**
     * 获取当前用户ID（简化）
     */
    private Long getCurrentUserId() {
        return 1L;
    }

    /**
     * 全局搜索
     */
    public List<SearchResultVO> search(SearchRequest request) {
        Long spaceId = getCurrentSpaceId();
        Integer role = getCurrentUserRole();
        Long currentUserId = getCurrentUserId();

        // 计算偏移量
        int offset = (request.getPage() - 1) * request.getSize();

        // 查询记录
        List<Record> records = recordMapper.searchGlobal(
                spaceId,
                request.getModuleId(),
                request.getQ(),
                request.getTag(),
                request.getStartDate(),
                request.getEndDate(),
                offset,
                request.getSize()
        );

        // 转换为 VO
        return records.stream()
                .map(r -> convertToVO(r, role, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * 搜索总数
     */
    public long count(SearchRequest request) {
        Long spaceId = getCurrentSpaceId();
        return recordMapper.countSearchGlobal(
                spaceId,
                request.getModuleId(),
                request.getQ(),
                request.getTag(),
                request.getStartDate(),
                request.getEndDate()
        );
    }

    /**
     * 转换为 VO，包含高亮片段
     */
    private SearchResultVO convertToVO(Record record, Integer role, Long currentUserId) {
        Module module = moduleMapper.selectById(record.getModuleId());

        SearchResultVO vo = new SearchResultVO()
                .setId(record.getId())
                .setSpaceId(record.getSpaceId())
                .setModuleId(record.getModuleId())
                .setModuleKey(module != null ? module.getModuleKey() : "")
                .setModuleName(module != null ? module.getName() : "")
                .setTitle(record.getTitle())
                .setCreatorId(record.getCreatorId())
                .setCreatorName("用户") // 实际应查用户表
                .setIsPrivate(record.getIsPrivate())
                .setTags(record.getTags())
                .setRemindDate(record.getRemindDate() != null ? record.getRemindDate().toString() : null)
                .setDataJson(record.getDataJson())
                .setImagesJson(record.getImagesJson())
                .setCreatedAt(record.getCreatedAt() != null ? record.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .setUpdatedAt(record.getUpdatedAt() != null ? record.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);

        // 生成高亮片段
        if (record.getDataJson() != null) {
            vo.setHighlightSnippet(generateHighlightSnippet(record.getDataJson()));
        }

        return vo;
    }

    /**
     * 生成高亮片段（简化：返回前 200 字符）
     */
    private String generateHighlightSnippet(String dataJson) {
        try {
            Map<String, Object> data = objectMapper.readValue(dataJson, Map.class);
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (!"is_private".equals(entry.getKey())) {
                    sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("; ");
                    if (sb.length() > 200) break;
                }
            }
            return sb.length() > 200 ? sb.substring(0, 200) + "..." : sb.toString();
        } catch (Exception e) {
            return dataJson.length() > 200 ? dataJson.substring(0, 200) + "..." : dataJson;
        }
    }
}