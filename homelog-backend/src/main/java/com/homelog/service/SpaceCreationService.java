package com.homelog.service;

import com.homelog.entity.Field;
import com.homelog.entity.FieldTemplate;
import com.homelog.entity.Module;
import com.homelog.entity.ModuleTemplate;
import com.homelog.entity.Space;
import com.homelog.entity.SpaceMember;
import com.homelog.mapper.FieldMapper;
import com.homelog.mapper.FieldTemplateMapper;
import com.homelog.mapper.ModuleMapper;
import com.homelog.mapper.ModuleTemplateMapper;
import com.homelog.mapper.SpaceMemberMapper;
import com.homelog.mapper.SpaceMapper;
import com.homelog.security.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 空间创建服务（设计文档 v1.2 §6.1 / 落地路线步 3）。
 *
 * <p>把「建一个可用的家」这件事收敛成一步：插入 {@code jj_space} → 以 owner 身份插入
 * 唯一成员关系 → 按模板实例化内置模块与字段。三者同事务，任一失败整体回滚，
 * 不会留下「有家无模块」「有 owner 无家」的半态。
 *
 * <p><b>为什么必须有第 3 步</b>：{@code jj_module} 带 {@code space_id}，
 * {@code jj_field} 挂在 {@code module_id} 下。新空间若不实例化内置模块，
 * 用户登录后菜单与首页全空 —— 这正是 {@code AuthController} 早年放弃
 * 「每新用户建空间」的原因（见其历史注释）。
 *
 * <p>模板来源是 {@code jj_module_template}/{@code jj_field_template}（V4 迁移建立），
 * 与任何真实空间解耦：某个家庭改别名 / 禁模块不会污染后来者的默认配置。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SpaceCreationService {

    /** 空间默认名 */
    private static final String DEFAULT_SPACE_NAME = "我的家庭";

    /** 空间状态：正常 */
    private static final int SPACE_STATUS_ACTIVE = 1;

    /** 成员状态：在籍 */
    private static final int MEMBER_STATUS_ACTIVE = 1;

    /** 内置预制模块 */
    private static final int MODULE_TYPE_BUILT_IN = 1;

    private final SpaceMapper spaceMapper;
    private final SpaceMemberMapper spaceMemberMapper;
    private final ModuleMapper moduleMapper;
    private final FieldMapper fieldMapper;
    private final ModuleTemplateMapper moduleTemplateMapper;
    private final FieldTemplateMapper fieldTemplateMapper;

    /**
     * 为指定用户创建一个私有家庭空间，并按模板实例化内置模块。
     *
     * @param ownerId   空间所有者用户 ID
     * @param spaceName 空间名称，为空时用默认名
     * @return 新建空间 ID
     * @throws IllegalStateException 内置模块模板缺失（V4 迁移未执行或被清空）
     */
    @Transactional
    public Long createFamilySpaceWithPresets(Long ownerId, String spaceName) {
        if (ownerId == null) {
            throw new IllegalArgumentException("创建空间需要有效的所有者 ID");
        }

        // 1. 建空间，owner 固定为创建者（不支持转让）
        Space space = new Space()
                .setName(spaceName == null || spaceName.trim().isEmpty() ? DEFAULT_SPACE_NAME : spaceName.trim())
                .setOwnerId(ownerId)
                .setStatus(SPACE_STATUS_ACTIVE);
        spaceMapper.insert(space);
        Long spaceId = space.getId();

        // 2. owner 成员关系（角色 1，在籍）
        SpaceMember owner = new SpaceMember()
                .setSpaceId(spaceId)
                .setUserId(ownerId)
                .setRole(Role.OWNER)
                .setStatus(MEMBER_STATUS_ACTIVE)
                .setReceiveMail(1);
        spaceMemberMapper.insert(owner);

        // 3. 实例化内置模块 —— 失败必须整体回滚，故用异常而非静默跳过
        int moduleCount = seedPresetModules(spaceId);

        log.info("家庭空间创建成功：spaceId={}, ownerId={}, 内置模块数={}", spaceId, ownerId, moduleCount);
        return spaceId;
    }

    /**
     * 按模板为新空间复制内置模块及其字段。
     *
     * @return 实例化的模块数量
     */
    private int seedPresetModules(Long spaceId) {
        List<ModuleTemplate> templates = moduleTemplateMapper.selectAllOrdered();
        if (templates == null || templates.isEmpty()) {
            // 模板表为空 = V4__space_templates.sql 未执行或被清空。
            // 此时若继续，会建出一个没有任何模块的家：用户看不到任何功能、
            // 且这半态会被持久化下来难以察觉。宁可让注册显式失败并报出迁移名。
            log.error("Preset module templates missing, aborting space seeding: spaceId={}", spaceId);
            throw new IllegalStateException(
                    "内置模块模板缺失，无法初始化新家庭空间。请确认数据库迁移 V4__space_templates.sql 已执行。");
        }

        int moduleCount = 0;
        int fieldCount = 0;
        for (ModuleTemplate template : templates) {
            Module module = new Module()
                    .setSpaceId(spaceId)
                    .setModuleKey(template.getModuleKey())
                    .setName(template.getName())
                    .setModuleType(template.getModuleType() != null ? template.getModuleType() : MODULE_TYPE_BUILT_IN)
                    .setIsEnable(template.getIsEnable() != null ? template.getIsEnable() : 1)
                    .setShowInHome(template.getShowInHome() != null ? template.getShowInHome() : 1)
                    .setSortOrder(template.getSortOrder() != null ? template.getSortOrder() : 0)
                    .setConfigJson(template.getConfigJson());
            moduleMapper.insert(module);
            moduleCount++;

            List<FieldTemplate> fieldTemplates = fieldTemplateMapper.selectByModuleKey(template.getModuleKey());
            if (fieldTemplates == null) {
                continue;
            }
            for (FieldTemplate ft : fieldTemplates) {
                Field field = new Field()
                        .setModuleId(module.getId())
                        .setFieldKey(ft.getFieldKey())
                        .setFieldLabel(ft.getFieldLabel())
                        .setFieldType(ft.getFieldType())
                        .setIsRequired(ft.getIsRequired() != null ? ft.getIsRequired() : 0)
                        .setIsPrivate(ft.getIsPrivate() != null ? ft.getIsPrivate() : 0)
                        .setOptionsJson(ft.getOptionsJson())
                        .setSortOrder(ft.getSortOrder() != null ? ft.getSortOrder() : 0);
                fieldMapper.insert(field);
                fieldCount++;
            }
        }

        log.info("内置模块实例化完成：spaceId={}, modules={}, fields={}", spaceId, moduleCount, fieldCount);
        return moduleCount;
    }
}
