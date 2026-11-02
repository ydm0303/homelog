package com.homelog.vo;

import com.homelog.entity.Field;
import lombok.Data;

/**
 * 字段元数据响应
 */
@Data
public class FieldVO {

    private Long id;
    private String fieldKey;
    private String fieldLabel;
    private String fieldType;
    private Integer isRequired;
    private Integer isPrivate;
    private String optionsJson;
    private Integer sortOrder;

    public static FieldVO from(Field field) {
        FieldVO vo = new FieldVO();
        vo.setId(field.getId());
        vo.setFieldKey(field.getFieldKey());
        vo.setFieldLabel(field.getFieldLabel());
        vo.setFieldType(field.getFieldType());
        vo.setIsRequired(field.getIsRequired());
        vo.setIsPrivate(field.getIsPrivate());
        vo.setOptionsJson(field.getOptionsJson());
        vo.setSortOrder(field.getSortOrder());
        return vo;
    }
}