package com.homelog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homelog.entity.ModuleTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 内置模块模板 Mapper（只读）
 */
@Mapper
public interface ModuleTemplateMapper extends BaseMapper<ModuleTemplate> {

    @Select("SELECT * FROM jj_module_template ORDER BY sort_order, id")
    List<ModuleTemplate> selectAllOrdered();
}
