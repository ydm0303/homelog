package com.homelog.vo;

import com.homelog.entity.Field;
import com.homelog.entity.Module;
import lombok.Data;

import java.util.List;

/**
 * 模块元数据响应（前端动态渲染用）
 */
@Data
public class ModuleMetaVO {

    private Long id;
    private String moduleKey;
    private String name;
    private Integer moduleType;
    private Integer isEnable;
    private Integer showInHome;
    private Integer sortOrder;
    private String configJson;
    private List<FieldVO> fields;

    public static ModuleMetaVO from(Module module, List<Field> fields) {
        ModuleMetaVO vo = new ModuleMetaVO();
        vo.setId(module.getId());
        vo.setModuleKey(module.getModuleKey());
        vo.setName(module.getName());
        vo.setModuleType(module.getModuleType());
        vo.setIsEnable(module.getIsEnable());
        vo.setShowInHome(module.getShowInHome());
        vo.setSortOrder(module.getSortOrder());
        vo.setConfigJson(module.getConfigJson());
        vo.setFields(fields.stream().map(FieldVO::from).collect(java.util.stream.Collectors.toList()));
        return vo;
    }
}