package com.homelog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homelog.entity.Space;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 家庭空间 Mapper
 */
@Mapper
public interface SpaceMapper extends BaseMapper<Space> {

    @Select("SELECT * FROM jj_space WHERE owner_id = #{ownerId} LIMIT 1")
    Space selectByOwnerId(Long ownerId);

    /**
     * 全部正常状态的空间（已解散的不处理）。
     * <p>供定时任务遍历所有家庭使用 —— 步 3 支持多空间后，
     * 任何按 {@code space_id=1} 硬编码的批处理都只会覆盖最早那一个家。
     */
    @Select("SELECT * FROM jj_space WHERE status = 1 ORDER BY id")
    List<Space> selectAllActive();
}