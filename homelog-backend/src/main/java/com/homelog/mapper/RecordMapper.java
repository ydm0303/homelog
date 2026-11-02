package com.homelog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homelog.entity.Record;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 记录 Mapper
 */
@Mapper
public interface RecordMapper extends BaseMapper<Record> {

    @Select("SELECT * FROM jj_record WHERE space_id = #{spaceId} AND module_id = #{moduleId} ORDER BY created_at DESC")
    List<Record> selectBySpaceAndModule(@Param("spaceId") Long spaceId, @Param("moduleId") Long moduleId);

    @Select("SELECT * FROM jj_record WHERE space_id = #{spaceId} AND module_id = #{moduleId} AND id = #{id}")
    Record selectByIdAndSpaceAndModule(@Param("spaceId") Long spaceId, @Param("moduleId") Long moduleId, @Param("id") Long id);

    @Select("SELECT * FROM jj_record WHERE space_id = #{spaceId} AND remind_date IS NOT NULL AND remind_date <= #{date} ORDER BY remind_date")
    List<Record> selectDueRecords(@Param("spaceId") Long spaceId, @Param("date") String date);

    @Select("SELECT * FROM jj_record WHERE space_id = #{spaceId} AND title LIKE CONCAT('%', #{keyword}, '%') ORDER BY created_at DESC")
    List<Record> searchByKeyword(@Param("spaceId") Long spaceId, @Param("keyword") String keyword);
}