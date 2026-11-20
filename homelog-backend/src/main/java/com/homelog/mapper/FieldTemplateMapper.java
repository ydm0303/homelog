package com.homelog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homelog.entity.FieldTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 内置字段模板 Mapper（只读）
 */
@Mapper
public interface FieldTemplateMapper extends BaseMapper<FieldTemplate> {

    @Select("SELECT * FROM jj_field_template WHERE module_key = #{moduleKey} ORDER BY sort_order, id")
    List<FieldTemplate> selectByModuleKey(@Param("moduleKey") String moduleKey);
}
