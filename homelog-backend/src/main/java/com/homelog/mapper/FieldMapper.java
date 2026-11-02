package com.homelog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homelog.entity.Field;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 字段元数据 Mapper
 */
@Mapper
public interface FieldMapper extends BaseMapper<Field> {

    @Select("SELECT * FROM jj_field WHERE module_id = #{moduleId} ORDER BY sort_order")
    List<Field> selectByModuleId(Long moduleId);
}