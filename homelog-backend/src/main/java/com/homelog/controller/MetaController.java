package com.homelog.controller;

import com.homelog.dto.ModuleUpdateRequest;
import com.homelog.dto.ModuleCreateRequest;
import com.homelog.entity.Field;
import com.homelog.entity.Module;
import com.homelog.mapper.ModuleMapper;
import com.homelog.mapper.FieldMapper;
import com.homelog.security.CurrentUserService;
import com.homelog.security.Role;
import com.homelog.vo.ModuleMetaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 元数据控制器 - 提供模块/字段配置给前端动态渲染
 * 规范：只使用 GET/POST，参数通过 query 或 body 传递
 */
@RestController
@RequestMapping("/api/meta")
@RequiredArgsConstructor
@Slf4j
public class MetaController {

    private final ModuleMapper moduleMapper;
    private final FieldMapper fieldMapper;
    private final CurrentUserService currentUserService;

    /**
     * 获取当前空间启用的模块列表（所有成员可见）
     * GET /api/meta/modules/list
     */
    @GetMapping("/modules/list")
    public List<ModuleMetaVO> getEnabledModules() {
        Long spaceId = currentUserService.getCurrentSpaceId();
        log.info("查询空间已启用模块：空间ID={}", spaceId);

        List<Module> modules = moduleMapper.selectEnabledBySpaceId(spaceId);
        return modules.stream()
                .map(module -> {
                    List<Field> fields = fieldMapper.selectByModuleId(module.getId());
                    return ModuleMetaVO.from(module, fields);
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取当前空间所有模块（管理员用，包含禁用的）
     * GET /api/meta/modules/all
     */
    @GetMapping("/modules/all")
    public List<ModuleMetaVO> getAllModules() {
        Long spaceId = currentUserService.getCurrentSpaceId();
        Integer role = currentUserService.getCurrentRole();
        log.info("查询空间全部模块：空间ID={}，用户角色={}", spaceId, role);

        // 只有管理员可查看所有模块
        if (!Role.canManageModule(role)) {
            log.debug("Non-admin requests all modules, fallback to enabled only: spaceId={}", spaceId);
            return getEnabledModules();
        }

        List<Module> modules = moduleMapper.selectAllBySpaceId(spaceId);
        return modules.stream()
                .map(module -> {
                    List<Field> fields = fieldMapper.selectByModuleId(module.getId());
                    return ModuleMetaVO.from(module, fields);
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取单个模块详情（含字段定义）
     * GET /api/meta/modules/detail?moduleId=xxx
     */
    @GetMapping("/modules/detail")
    public ModuleMetaVO getModule(@RequestParam Long moduleId) {
        Long spaceId = currentUserService.getCurrentSpaceId();
        Module module = moduleMapper.selectById(moduleId);

        if (module == null || !module.getSpaceId().equals(spaceId)) {
            log.warn("Get module detail failed, module not found in space: moduleId={}, spaceId={}", moduleId, spaceId);
            return null;
        }

        List<Field> fields = fieldMapper.selectByModuleId(moduleId);
        return ModuleMetaVO.from(module, fields);
    }

    /**
     * 更新模块（开关/排序/别名） - 仅管理员
     * POST /api/meta/modules/update
     */
    @PostMapping("/modules/update")
    public ModuleMetaVO updateModule(@RequestBody ModuleUpdateRequest request) {
        Long spaceId = currentUserService.getCurrentSpaceId();
        Integer role = currentUserService.getCurrentRole();
        log.info("更新模块配置：空间ID={}，模块ID={}", spaceId, request.getModuleId());

        if (!Role.canManageModule(role)) {
            log.warn("Update module denied, not an admin: spaceId={}, moduleId={}, role={}",
                    spaceId, request.getModuleId(), role);
            throw new RuntimeException("权限不足，仅管理员可修改模块配置");
        }

        Module module = moduleMapper.selectById(request.getModuleId());
        if (module == null || !module.getSpaceId().equals(spaceId)) {
            log.warn("Update module failed, module not found in space: moduleId={}, spaceId={}",
                    request.getModuleId(), spaceId);
            throw new RuntimeException("模块不存在");
        }

        module.setName(request.getName());
        module.setIsEnable(request.getIsEnable());
        module.setShowInHome(request.getShowInHome());
        module.setSortOrder(request.getSortOrder());
        module.setConfigJson(request.getConfigJson());

        moduleMapper.updateById(module);

        List<Field> fields = fieldMapper.selectByModuleId(request.getModuleId());
        log.info("模块配置更新成功：模块ID={}", request.getModuleId());
        return ModuleMetaVO.from(module, fields);
    }

    /**
     * 创建自定义模块 - 仅管理员
     * POST /api/meta/modules/create
     */
    @PostMapping("/modules/create")
    public ModuleMetaVO createModule(@RequestBody ModuleCreateRequest request) {
        Long spaceId = currentUserService.getCurrentSpaceId();
        Integer role = currentUserService.getCurrentRole();
        log.info("创建自定义模块：空间ID={}，模块标识={}，名称={}",
                spaceId, request.getModuleKey(), request.getName());

        if (!Role.canManageModule(role)) {
            log.warn("Create module denied, not an admin: spaceId={}, role={}", spaceId, role);
            throw new RuntimeException("权限不足，仅管理员可创建模块");
        }

        Module module = new Module()
                .setSpaceId(spaceId)
                .setName(request.getName())
                .setModuleKey(request.getModuleKey())
                .setModuleType(2) // 自定义
                .setIsEnable(1)
                .setShowInHome(0)
                .setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 99)
                .setConfigJson("{}");

        moduleMapper.insert(module);

        List<Field> fields = fieldMapper.selectByModuleId(module.getId());
        log.info("自定义模块创建成功：模块ID={}，模块标识={}", module.getId(), module.getModuleKey());
        return ModuleMetaVO.from(module, fields);
    }

    /**
     * 删除自定义模块 - 仅管理员
     * POST /api/meta/modules/delete
     */
    @PostMapping("/modules/delete")
    public void deleteModule(@RequestParam Long moduleId) {
        Long spaceId = currentUserService.getCurrentSpaceId();
        Integer role = currentUserService.getCurrentRole();
        log.info("删除模块：空间ID={}，模块ID={}", spaceId, moduleId);

        if (!Role.canManageModule(role)) {
            log.warn("Delete module denied, not an admin: spaceId={}, moduleId={}, role={}", spaceId, moduleId, role);
            throw new RuntimeException("权限不足，仅管理员可删除模块");
        }

        Module module = moduleMapper.selectById(moduleId);
        if (module == null || !module.getSpaceId().equals(spaceId)) {
            log.warn("Delete module failed, module not found in space: moduleId={}, spaceId={}", moduleId, spaceId);
            throw new RuntimeException("模块不存在");
        }

        if (module.getModuleType() != 2) {
            log.warn("Delete module denied, built-in module cannot be deleted: moduleId={}", moduleId);
            throw new RuntimeException("内置模块不可删除");
        }

        moduleMapper.deleteById(moduleId);
        log.info("模块删除成功：模块ID={}", moduleId);
    }
}