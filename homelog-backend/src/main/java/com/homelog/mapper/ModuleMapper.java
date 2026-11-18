package com.homelog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homelog.entity.Module;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 模块 Mapper
 */
@Mapper
public interface ModuleMapper extends BaseMapper<Module> {

    @Select("SELECT * FROM jj_module WHERE space_id = #{spaceId} AND is_enable = 1 ORDER BY sort_order")
    List<Module> selectEnabledBySpaceId(@Param("spaceId")Long spaceId);

    @Select("SELECT * FROM jj_module WHERE space_id = #{spaceId} ORDER BY sort_order")
    List<Module> selectAllBySpaceId(@Param("spaceId")Long spaceId);

    @Select("SELECT * FROM jj_module WHERE space_id = #{spaceId} AND module_key = #{moduleKey}")
    Module selectBySpaceAndKey(@Param("spaceId")Long spaceId,@Param("moduleKey") String moduleKey);
}