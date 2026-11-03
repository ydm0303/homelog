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

    /**
     * 全局搜索（跨模块）
     */
    @Select("SELECT * FROM jj_record WHERE space_id = #{spaceId} " +
            "AND (#{moduleId} IS NULL OR module_id = #{moduleId}) " +
            "AND (#{keyword} IS NULL OR #{keyword} = '' OR title LIKE CONCAT('%', #{keyword}, '%') OR data_json LIKE CONCAT('%', #{keyword}, '%') OR tags LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND (#{tag} IS NULL OR #{tag} = '' OR tags LIKE CONCAT('%', #{tag}, '%')) " +
            "AND (#{startDate} IS NULL OR #{startDate} = '' OR remind_date >= #{startDate}) " +
            "AND (#{endDate} IS NULL OR #{endDate} = '' OR remind_date <= #{endDate}) " +
            "ORDER BY created_at DESC LIMIT #{offset}, #{size}")
    List<Record> searchGlobal(
            @Param("spaceId") Long spaceId,
            @Param("moduleId") Long moduleId,
            @Param("keyword") String keyword,
            @Param("tag") String tag,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("offset") int offset,
            @Param("size") int size
    );

    /**
     * 全局搜索总数
     */
    @Select("SELECT COUNT(*) FROM jj_record WHERE space_id = #{spaceId} " +
            "AND (#{moduleId} IS NULL OR module_id = #{moduleId}) " +
            "AND (#{keyword} IS NULL OR #{keyword} = '' OR title LIKE CONCAT('%', #{keyword}, '%') OR data_json LIKE CONCAT('%', #{keyword}, '%') OR tags LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND (#{tag} IS NULL OR #{tag} = '' OR tags LIKE CONCAT('%', #{tag}, '%')) " +
            "AND (#{startDate} IS NULL OR #{startDate} = '' OR remind_date >= #{startDate}) " +
            "AND (#{endDate} IS NULL OR #{endDate} = '' OR remind_date <= #{endDate})")
    Integer countSearchGlobal(
            @Param("spaceId") Long spaceId,
            @Param("moduleId") Long moduleId,
            @Param("keyword") String keyword,
            @Param("tag") String tag,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );
}