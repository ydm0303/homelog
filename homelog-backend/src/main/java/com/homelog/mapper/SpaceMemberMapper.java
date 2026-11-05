package com.homelog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homelog.entity.SpaceMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 空间成员 Mapper
 */
@Mapper
public interface SpaceMemberMapper extends BaseMapper<SpaceMember> {

    @Select("SELECT * FROM jj_space_member WHERE space_id = #{spaceId} AND user_id = #{userId}")
    SpaceMember selectBySpaceAndUser(Long spaceId, Long userId);

    @Select("SELECT * FROM jj_space_member WHERE space_id = #{spaceId} AND user_id = #{userId}")
    SpaceMember selectBySpaceIdAndUserId(Long spaceId, Long userId);

    @Select("SELECT * FROM jj_space_member WHERE space_id = #{spaceId}")
    List<SpaceMember> selectBySpaceId(Long spaceId);

    @Select("SELECT * FROM jj_space_member WHERE user_id = #{userId}")
    List<SpaceMember> selectByUserId(Long userId);
}