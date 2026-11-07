package com.homelog.service;

import com.homelog.dto.SearchRequest;
import com.homelog.entity.Record;
import com.homelog.entity.Module;
import com.homelog.entity.User;
import com.homelog.mapper.RecordMapper;
import com.homelog.mapper.ModuleMapper;
import com.homelog.mapper.UserMapper;
import com.homelog.vo.SearchResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取当前空间ID（MVP 单空间）
     */
    private Long getCurrentSpaceId() {
        return 1L;
    }

    /**
     * 获取当前用户角色：从 SecurityContext 的 ROLE_* 权限解析（1=管理员 2=编辑 3=只读）
     */
    private Integer getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return 3;

        return auth.getAuthorities().stream()
                .filter(a -> a.getAuthority().startsWith("ROLE_"))
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .map(role -> {
                    switch (role) {
                        case "ADMIN": return 1;
                        case "EDIT": return 2;
                        default: return 3;
                    }
                })
                .findFirst()
                .orElse(3);
    }

    /**
     * 获取当前用户ID：由 SecurityContext 的用户名反查 jj_user
     */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;

        User user = userMapper.selectByUsername(auth.getName());
        return user != null ? user.getId() : null;
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

        // 查询记录（隐私过滤在 SQL 层完成）
        List<Record> records = recordMapper.searchGlobal(
                spaceId,
                request.getModuleId(),
                request.getQ(),
                request.getTag(),
                request.getStartDate(),
                request.getEndDate(),
                role == 1 ? 1 : 0,
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
        Long spaceId = getCurrentSpaceId();
        Integer role = getCurrentUserRole();
        Long currentUserId = getCurrentUserId();

        return recordMapper.countSearchGlobal(
                spaceId,
                request.getModuleId(),
                request.getQ(),
                request.getTag(),
                request.getStartDate(),
                request.getEndDate(),
                role == 1 ? 1 : 0,
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