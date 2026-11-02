package com.homelog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homelog.entity.Space;
import org.apache.ibatis.annotations.Mapper;

/**
 * 家庭空间 Mapper
 */
@Mapper
public interface SpaceMapper extends BaseMapper<Space> {

    Space selectByOwnerId(Long ownerId);
}