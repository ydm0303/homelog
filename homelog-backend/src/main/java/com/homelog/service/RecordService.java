package com.homelog.service;

import com.homelog.dto.RecordCreateRequest;
import com.homelog.dto.RecordUpdateRequest;
import com.homelog.entity.Field;
import com.homelog.entity.Module;
import com.homelog.entity.Record;
import com.homelog.entity.SpaceMember;
import com.homelog.entity.User;
import com.homelog.mapper.FieldMapper;
import com.homelog.mapper.ModuleMapper;
import com.homelog.mapper.RecordMapper;
import com.homelog.mapper.SpaceMemberMapper;
import com.homelog.mapper.UserMapper;
import com.homelog.security.CurrentUserService;
import com.homelog.vo.PageResult;
import com.homelog.vo.RecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
    private final UserMapper userMapper;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 列表查询（返回 total，筛选全部在服务端完成）
     */
    public PageResult<RecordVO> list(Long moduleId, Integer page, Integer size,
                                     String keyword, String tag,
                                     String startDate, String endDate,
                                     String creator, Boolean privateOnly) {
        Long spaceId = currentUserService.getCurrentSpaceId();
        Integer role = currentUserService.getCurrentRole();
        Long currentUserId = currentUserService.getCurrentUserId();

        Module module = moduleMapper.selectById(moduleId);
        if (module == null || !module.getSpaceId().equals(spaceId)) {
            return new PageResult<>(new ArrayList<>(), 0, page, size);
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

        // 创建时间范围筛选
        if (startDate != null && !startDate.trim().isEmpty()) {
            LocalDate start = LocalDate.parse(startDate.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
            records = records.stream()
                    .filter(r -> r.getCreatedAt() != null
                            && !r.getCreatedAt().toLocalDate().isBefore(start))
                    .collect(Collectors.toList());
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            LocalDate end = LocalDate.parse(endDate.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
            records = records.stream()
                    .filter(r -> r.getCreatedAt() != null
                            && !r.getCreatedAt().toLocalDate().isAfter(end))
                    .collect(Collectors.toList());
        }

        // 创建人筛选：按昵称/用户名模糊匹配
        Map<Long, String> userNameMap = loadSpaceUserMap(spaceId);
        if (creator != null && !creator.trim().isEmpty()) {
            String creatorKeyword = creator.trim().toLowerCase();
            records = records.stream()
                    .filter(r -> {
                        String name = userNameMap.get(r.getCreatorId());
                        return name != null && name.toLowerCase().contains(creatorKeyword);
                    })
                    .collect(Collectors.toList());
        }

        // 仅私密：非管理员不允许查看私密记录，直接返回空
        if (Boolean.TRUE.equals(privateOnly)) {
            if (role != 1) {
                return new PageResult<>(new ArrayList<>(), 0, page, size);
            }
            records = records.stream()
                    .filter(r -> r.getIsPrivate() != null && r.getIsPrivate() == 1)
                    .collect(Collectors.toList());
        }

        // 私密过滤
        final Integer currentRole = role;
        final Long viewerId = currentUserId;
        records = records.stream()
                .filter(r -> canViewRecord(r, currentRole, viewerId))
                .collect(Collectors.toList());

        long total = records.size();
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 20 : size;
        int start = (safePage - 1) * safeSize;
        if (start >= records.size()) {
            return new PageResult<>(new ArrayList<>(), total, safePage, safeSize);
        }
        int end = Math.min(start + safeSize, records.size());

        List<RecordVO> data = records.subList(start, end).stream()
                .map(r -> toVO(r, module, userNameMap, role))
                .collect(Collectors.toList());
        return new PageResult<>(data, total, safePage, safeSize);
    }

    /**
     * 标签聚合：返回当前空间可见记录的全部标签。
     * moduleId 为空时覆盖空间内所有模块；否则仅返回指定模块。
     */
    public List<String> listTags(Long moduleId) {
        Long spaceId = currentUserService.getCurrentSpaceId();
        Integer role = currentUserService.getCurrentRole();
        Long currentUserId = currentUserService.getCurrentUserId();

        List<Module> modules = moduleMapper.selectAllBySpaceId(spaceId);
        Set<Long> allowedModuleIds = new TreeSet<>();
        for (Module module : modules) {
            if (moduleId == null || moduleId.equals(module.getId())) {
                allowedModuleIds.add(module.getId());
            }
        }
        if (allowedModuleIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Record> records = moduleId != null
                ? recordMapper.selectBySpaceAndModule(spaceId, moduleId)
                : recordMapper.selectBySpaceId(spaceId);

        Set<String> tags = new TreeSet<>();
        for (Record record : records) {
            if (!allowedModuleIds.contains(record.getModuleId())) continue;
            if (!canViewRecord(record, role, currentUserId)) continue;
            if (record.getTags() == null || record.getTags().trim().isEmpty()) continue;
            for (String tag : record.getTags().split("[,，]")) {
                String trimmed = tag.trim();
                if (!trimmed.isEmpty()) {
                    tags.add(trimmed);
                }
            }
        }
        return new ArrayList<>(tags);
    }

    /**
     * 首页聚合数据：模块计数、今日待办、即将到期、最近记录。
     * 一次查询空间内记录，避免前端按模块 N+1 请求。
     */
    public Map<String, Object> getDashboardSummary() {
        Long spaceId = currentUserService.getCurrentSpaceId();
        Integer role = currentUserService.getCurrentRole();
        Long currentUserId = currentUserService.getCurrentUserId();

        Map<Long, Module> moduleMap = new HashMap<>();
        List<Module> modules = moduleMapper.selectEnabledBySpaceId(spaceId);
        for (Module module : modules) {
            moduleMap.put(module.getId(), module);
        }

        Map<Long, String> userNameMap = loadSpaceUserMap(spaceId);
        List<Record> all = recordMapper.selectBySpaceId(spaceId).stream()
                .filter(r -> moduleMap.containsKey(r.getModuleId()))
                .filter(r -> canViewRecord(r, role, currentUserId))
                .collect(Collectors.toList());

        List<Map<String, Object>> moduleStats = new ArrayList<>();
        Map<Long, Integer> counts = new HashMap<>();
        for (Record record : all) {
            counts.merge(record.getModuleId(), 1, Integer::sum);
        }
        for (Module module : modules) {
            Map<String, Object> stat = new HashMap<>();
            stat.put("moduleId", module.getId());
            stat.put("moduleKey", module.getModuleKey());
            stat.put("name", module.getName());
            stat.put("showInHome", module.getShowInHome());
            stat.put("count", counts.getOrDefault(module.getId(), 0));
            moduleStats.add(stat);
        }

        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(30);
        List<RecordVO> todos = all.stream()
                .filter(r -> {
                    Module m = moduleMap.get(r.getModuleId());
                    if (m == null || !"todo".equals(m.getModuleKey())) return false;
                    Map<String, Object> data = parseJson(r.getDataJson());
                    return !Boolean.TRUE.equals(data.get("checked"));
                })
                .sorted((a, b) -> compareCreatedAtDesc(a, b))
                .limit(5)
                .map(r -> toVO(r, moduleMap.get(r.getModuleId()), userNameMap, role))
                .collect(Collectors.toList());

        List<RecordVO> expiring = all.stream()
                .filter(r -> r.getRemindDate() != null
                        && !r.getRemindDate().isBefore(today)
                        && !r.getRemindDate().isAfter(deadline))
                .sorted((a, b) -> a.getRemindDate().compareTo(b.getRemindDate()))
                .limit(5)
                .map(r -> toVO(r, moduleMap.get(r.getModuleId()), userNameMap, role))
                .collect(Collectors.toList());

        List<RecordVO> recent = all.stream()
                .sorted((a, b) -> compareCreatedAtDesc(a, b))
                .limit(8)
                .map(r -> toVO(r, moduleMap.get(r.getModuleId()), userNameMap, role))
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("modules", moduleStats);
        result.put("todos", todos);
        result.put("expiring", expiring);
        result.put("recent", recent);
        return result;
    }

    private int compareCreatedAtDesc(Record a, Record b) {
        if (a.getCreatedAt() == null) return 1;
        if (b.getCreatedAt() == null) return -1;
        return b.getCreatedAt().compareTo(a.getCreatedAt());
    }

    private Map<Long, String> loadSpaceUserMap(Long spaceId) {
        Map<Long, String> map = new HashMap<>();
        List<SpaceMember> members = spaceMemberMapper.selectBySpaceId(spaceId);
        if (members == null) {
            return map;
        }
        for (SpaceMember member : members) {
            User user = userMapper.selectById(member.getUserId());
            if (user != null) {
                map.put(user.getId(), user.getNickName() != null && !user.getNickName().trim().isEmpty()
                        ? user.getNickName() : user.getUsername());
            }
        }
        return map;
    }

    private RecordVO toVO(Record record, Module module, Map<Long, String> userNameMap, Integer viewerRole) {
        String creatorName = userNameMap != null ? userNameMap.get(record.getCreatorId()) : null;
        if (creatorName == null) {
            User creator = userMapper.selectById(record.getCreatorId());
            creatorName = creator != null
                    ? (creator.getNickName() != null && !creator.getNickName().trim().isEmpty()
                        ? creator.getNickName() : creator.getUsername())
                    : "用户";
        }
        RecordVO vo = RecordVO.from(record,
                module != null ? module.getModuleKey() : "",
                module != null ? module.getName() : "",
                creatorName);
        removePrivateFieldsForViewer(vo, record.getModuleId(), viewerRole);
        return vo;
    }

    /**
     * 非管理员不返回私密字段值，避免列表/首页摘要泄露私密内容。
     */
    private void removePrivateFieldsForViewer(RecordVO vo, Long moduleId, Integer viewerRole) {
        if (viewerRole != null && viewerRole == 1) {
            return;
        }
        if (vo.getDataJson() == null || moduleId == null) {
            return;
        }
        List<Field> fields = fieldMapper.selectByModuleId(moduleId);
        if (fields == null) {
            return;
        }
        for (Field field : fields) {
            if (field.getIsPrivate() != null && field.getIsPrivate() == 1) {
                vo.getDataJson().remove(field.getFieldKey());
            }
        }
    }

    /**
     * 详情查询
     */
    public RecordVO get(Long moduleId, Long recordId) {
        Long spaceId = currentUserService.getCurrentSpaceId();
        Integer role = currentUserService.getCurrentRole();
        Long currentUserId = currentUserService.getCurrentUserId();

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

        return toVO(record, module, null, role);
    }

    /**
     * 创建记录
     */
    @Transactional
    public RecordVO create(RecordCreateRequest request) {
        Long spaceId = currentUserService.getCurrentSpaceId();
        Long currentUserId = currentUserService.getCurrentUserId();
        Integer role = currentUserService.getCurrentRole();

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
        log.info("记录创建成功：记录ID={}，模块ID={}，创建者ID={}", record.getId(), module.getId(), currentUserId);

        return toVO(record, module, null, role);
    }

    /**
     * 更新记录
     */
    @Transactional
    public RecordVO update(Long moduleId, Long recordId, RecordUpdateRequest request) {
        Long spaceId = currentUserService.getCurrentSpaceId();
        Long currentUserId = currentUserService.getCurrentUserId();
        Integer role = currentUserService.getCurrentRole();

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
        log.info("记录更新成功：记录ID={}，模块ID={}，用户ID={}", recordId, moduleId, currentUserId);

        return toVO(record, module, null, role);
    }

    /**
     * 删除记录
     */
    @Transactional
    public void delete(Long moduleId, Long recordId) {
        Long spaceId = currentUserService.getCurrentSpaceId();
        Long currentUserId = currentUserService.getCurrentUserId();
        Integer role = currentUserService.getCurrentRole();

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
        log.info("记录删除成功：记录ID={}，模块ID={}，用户ID={}", recordId, moduleId, currentUserId);
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