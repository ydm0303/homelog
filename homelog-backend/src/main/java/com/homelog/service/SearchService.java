package com.homelog.service;

import com.homelog.dto.SearchRequest;
import com.homelog.entity.Record;
import com.homelog.entity.Module;
import com.homelog.mapper.RecordMapper;
import com.homelog.mapper.ModuleMapper;
import com.homelog.security.CurrentUserService;
import com.homelog.security.Role;
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
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 全局搜索
     */
    public List<SearchResultVO> search(SearchRequest request) {
        Long spaceId = currentUserService.getCurrentSpaceId();
        Integer role = currentUserService.getCurrentRole();
        Long currentUserId = currentUserService.getCurrentUserId();

        // 计算偏移量
        int offset = (request.getPage() - 1) * request.getSize();

        // 查询记录（隐私过滤在 SQL 层完成）
        List<Record> records = recordMapper.searchGlobal(
                spaceId,
                request.getModuleId(),
                request.getQ(),
                request.getTag(),
                request.getStartDate(),
                request.getEndDate(),
                Role.canSeePrivate(role) ? 1 : 0,
                currentUserId,
                offset,
                request.getSize()
        );

        log.debug("Global search executed: spaceId={}, role={}, userId={}, keyword={}, moduleId={}, matched={}",
                spaceId, role, currentUserId, request.getQ(), request.getModuleId(), records.size());

        // 转换为 VO
        return records.stream()
                .map(r -> convertToVO(r, role, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * 搜索总数（隐私条件与 search 保持一致）
     */
    public long count(SearchRequest request) {
        Long spaceId = currentUserService.getCurrentSpaceId();
        Integer role = currentUserService.getCurrentRole();
        Long currentUserId = currentUserService.getCurrentUserId();

        return recordMapper.countSearchGlobal(
                spaceId,
                request.getModuleId(),
                request.getQ(),
                request.getTag(),
                request.getStartDate(),
                request.getEndDate(),
                Role.canSeePrivate(role) ? 1 : 0,
                currentUserId
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