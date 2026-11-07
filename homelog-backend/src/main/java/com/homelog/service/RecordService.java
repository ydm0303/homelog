package com.homelog.service;

import com.homelog.dto.RecordCreateRequest;
import com.homelog.dto.RecordUpdateRequest;
import com.homelog.entity.Field;
import com.homelog.entity.Module;
import com.homelog.entity.Record;
import com.homelog.entity.SpaceMember;
import com.homelog.mapper.FieldMapper;
import com.homelog.mapper.ModuleMapper;
import com.homelog.mapper.RecordMapper;
import com.homelog.mapper.SpaceMemberMapper;
import com.homelog.vo.RecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 记录服务 - EAV+JSON 通用存取
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecordService {

    private final RecordMapper recordMapper;
    private final ModuleMapper moduleMapper;
    private final FieldMapper fieldMapper;
    private final SpaceMemberMapper spaceMemberMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return 1L;
        // 简化：实际应从用户表查询
        return 1L;
    }

    /**
     * 获取当前空间ID（MVP 单空间）
     */
    private Long getCurrentSpaceId() {
        return 1L;
    }

    /**
     * 获取当前用户角色
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
     * 列表查询
     */
    public List<RecordVO> list(Long moduleId, Integer page, Integer size, String keyword, String tag) {
        Long spaceId = getCurrentSpaceId();
        Integer role = getCurrentUserRole();

        Module module = moduleMapper.selectById(moduleId);
        if (module == null || !module.getSpaceId().equals(spaceId)) {
            return new ArrayList<>();
        }

        List<Record> records = recordMapper.selectBySpaceAndModule(spaceId, moduleId);

        // 关键词搜索
        if (keyword != null && !keyword.trim().isEmpty()) {
            records = records.stream()
                    .filter(r -> matchesKeyword(r, keyword))
                    .collect(Collectors.toList());
        }

        // 标签筛选
        if (tag != null && !tag.trim().isEmpty()) {
            records = records.stream()
                    .filter(r -> r.getTags() != null && r.getTags().contains(tag))
                    .collect(Collectors.toList());
        }

        // 私密过滤
        final Integer currentRole = role;
        final Long currentUserId = getCurrentUserId();
        records = records.stream()
                .filter(r -> canViewRecord(r, currentRole, currentUserId))
                .collect(Collectors.toList());

        // 分页
        int start = (page - 1) * size;
        int end = Math.min(start + size, records.size());
        if (start >= records.size()) {
            return new ArrayList<>();
        }

        List<Record> pageRecords = records.subList(start, end);
        return pageRecords.stream()
                .map(r -> RecordVO.from(r, module.getModuleKey(), module.getName(), "用户"))
                .collect(Collectors.toList());
    }

    /**
     * 详情查询
     */
    public RecordVO get(Long moduleId, Long recordId) {
        Long spaceId = getCurrentSpaceId();
        Integer role = getCurrentUserRole();
        Long currentUserId = getCurrentUserId();

        Module module = moduleMapper.selectById(moduleId);
        if (module == null || !module.getSpaceId().equals(spaceId)) {
            return null;
        }

        Record record = recordMapper.selectByIdAndSpaceAndModule(spaceId, moduleId, recordId);
        if (record == null) {
            return null;
        }

        if (!canViewRecord(record, role, currentUserId)) {
            return null;
        }

        return RecordVO.from(record, module.getModuleKey(), module.getName(), "用户");
    }

    /**
     * 创建记录
     */
    @Transactional
    public RecordVO create(RecordCreateRequest request) {
        Long spaceId = getCurrentSpaceId();
        Long currentUserId = getCurrentUserId();
        Integer role = getCurrentUserRole();

        Module module = moduleMapper.selectById(request.getModuleId());
        if (module == null || !module.getSpaceId().equals(spaceId)) {
            log.warn("Create record failed, module not found: moduleId={}, spaceId={}", request.getModuleId(), spaceId);
            throw new RuntimeException("模块不存在");
        }

        // 权限检查：只读不能创建
        if (role == 3) {
            log.warn("Create record denied, read-only role: moduleId={}, userId={}", module.getId(), currentUserId);
            throw new RuntimeException("只读权限，无法创建记录");
        }

        // 验证必填字段
        validateRequiredFields(module.getId(), request.getDataJson());

        // 处理提醒日期
        LocalDate remindDate = null;
        if (request.getRemindDate() != null && !request.getRemindDate().trim().isEmpty()) {
            remindDate = LocalDate.parse(request.getRemindDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        }

        Record record = new Record()
                .setSpaceId(spaceId)
                .setModuleId(module.getId())
                .setTitle(request.getTitle())
                .setCreatorId(currentUserId)
                .setIsPrivate(request.getDataJson().get("is_private") != null ?
                        (Boolean.TRUE.equals(request.getDataJson().get("is_private")) ? 1 : 0) : 0)
                .setTags(request.getTags())
                .setRemindDate(remindDate)
                .setDataJson(toJsonString(request.getDataJson()))
                .setImagesJson(toJsonArrayString(request.getImagesJson()))
                .setCreatedAt(LocalDateTime.now())
                .setUpdatedAt(LocalDateTime.now());

        recordMapper.insert(record);
        log.info("Record created: recordId={}, moduleId={}, creatorId={}", record.getId(), module.getId(), currentUserId);

        return RecordVO.from(record, module.getModuleKey(), module.getName(), "用户");
    }

    /**
     * 更新记录
     */
    @Transactional
    public RecordVO update(Long moduleId, Long recordId, RecordUpdateRequest request) {
        Long spaceId = getCurrentSpaceId();
        Long currentUserId = getCurrentUserId();
        Integer role = getCurrentUserRole();

        Module module = moduleMapper.selectById(moduleId);
        if (module == null || !module.getSpaceId().equals(spaceId)) {
            log.warn("Update record failed, module not found: moduleId={}, spaceId={}", moduleId, spaceId);
            throw new RuntimeException("模块不存在");
        }

        Record record = recordMapper.selectByIdAndSpaceAndModule(spaceId, moduleId, recordId);
        if (record == null) {
            log.warn("Update record failed, record not found: moduleId={}, recordId={}", moduleId, recordId);
            throw new RuntimeException("记录不存在");
        }

        // 权限检查：只读不能修改；编辑只能改自己的
        if (role == 3) {
            log.warn("Update record denied, read-only role: recordId={}, userId={}", recordId, currentUserId);
            throw new RuntimeException("只读权限，无法修改记录");
        }
        if (role == 2 && !record.getCreatorId().equals(currentUserId)) {
            log.warn("Update record denied, not the creator: recordId={}, userId={}, creatorId={}",
                    recordId, currentUserId, record.getCreatorId());
            throw new RuntimeException("只能修改自己创建的记录");
        }

        // 合并数据
        Map<String, Object> dataJson = parseJson(record.getDataJson());
        if (request.getDataJson() != null) {
            dataJson.putAll(request.getDataJson());
        }

        LocalDate remindDate = record.getRemindDate();
        if (request.getRemindDate() != null && !request.getRemindDate().trim().isEmpty()) {
            remindDate = LocalDate.parse(request.getRemindDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        }

        record.setTitle(request.getTitle() != null ? request.getTitle() : record.getTitle())
                .setDataJson(toJsonString(dataJson))
                .setTags(request.getTags() != null ? request.getTags() : record.getTags())
                .setRemindDate(remindDate)
                .setUpdatedAt(LocalDateTime.now());

        // 仅在请求显式携带图片列表时才更新，避免未传 imagesJson 时把已有图片覆盖掉
        if (request.getImagesJson() != null) {
            record.setImagesJson(toJsonArrayString(request.getImagesJson()));
        }

        recordMapper.updateById(record);
        log.info("Record updated: recordId={}, moduleId={}, userId={}", recordId, moduleId, currentUserId);

        return RecordVO.from(record, module.getModuleKey(), module.getName(), "用户");
    }

    /**
     * 删除记录
     */
    @Transactional
    public void delete(Long moduleId, Long recordId) {
        Long spaceId = getCurrentSpaceId();
        Long currentUserId = getCurrentUserId();
        Integer role = getCurrentUserRole();

        Module module = moduleMapper.selectById(moduleId);
        if (module == null || !module.getSpaceId().equals(spaceId)) {
            log.warn("Delete record failed, module not found: moduleId={}, spaceId={}", moduleId, spaceId);
            throw new RuntimeException("模块不存在");
        }

        Record record = recordMapper.selectByIdAndSpaceAndModule(spaceId, moduleId, recordId);
        if (record == null) {
            log.warn("Delete record failed, record not found: moduleId={}, recordId={}", moduleId, recordId);
            throw new RuntimeException("记录不存在");
        }

        // 权限检查：只读/编辑不能删除（仅管理员可删）
        if (role != 1) {
            log.warn("Delete record denied, not an admin: recordId={}, role={}, userId={}", recordId, role, currentUserId);
            throw new RuntimeException("仅管理员可删除记录");
        }

        recordMapper.deleteById(recordId);
        log.info("Record deleted: recordId={}, moduleId={}, userId={}", recordId, moduleId, currentUserId);
    }

    /**
     * 验证必填字段
     */
    private void validateRequiredFields(Long moduleId, Map<String, Object> dataJson) {
        List<Field> fields = fieldMapper.selectByModuleId(moduleId);
        for (Field field : fields) {
            if (field.getIsRequired() == 1) {
                Object value = dataJson.get(field.getFieldKey());
                if (value == null || value.toString().trim().isEmpty()) {
                    throw new RuntimeException("必填字段缺失: " + field.getFieldLabel());
                }
            }
        }
    }

    /**
     * 判断是否可查看记录
     */
    private boolean canViewRecord(Record record, Integer role, Long currentUserId) {
        // 管理员可见所有
        if (role == 1) return true;

        // 记录级私密：仅管理员可见
        if (record.getIsPrivate() == 1) return false;

        // 自己创建的可见
        if (record.getCreatorId().equals(currentUserId)) return true;

        // 其他情况：编辑/只读可见公共记录
        return true;
    }

    /**
     * 关键词匹配
     */
    private boolean matchesKeyword(Record record, String keyword) {
        if (record.getTitle() != null && record.getTitle().contains(keyword)) return true;
        if (record.getTags() != null && record.getTags().contains(keyword)) return true;
        if (record.getDataJson() != null && record.getDataJson().contains(keyword)) return true;
        return false;
    }

    private String toJsonString(Object obj) {
        if (obj == null) return "{}";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * 序列化列表为 JSON 数组字符串；为空时返回 "[]"（而非 "{}"，保持与 images_json 列语义一致）
     */
    private String toJsonArrayString(Object obj) {
        if (obj == null) return "[]";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new java.util.HashMap<>();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return new java.util.HashMap<>();
        }
    }
}