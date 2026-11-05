package com.homelog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homelog.entity.Space;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 家庭空间 Mapper
 */
@Mapper
public interface SpaceMapper extends BaseMapper<Space> {

    @Select("SELECT * FROM jj_space WHERE owner_id = #{ownerId} LIMIT 1")
    Space selectByOwnerId(Long ownerId);
}