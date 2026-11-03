package com.homelog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homelog.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 消息中心 Mapper
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    @Select("SELECT * FROM jj_message WHERE user_id = #{userId} AND is_read = 0 ORDER BY created_at DESC")
    List<Message> selectUnreadByUserId(Long userId);

    @Select("SELECT * FROM jj_message WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Message> selectByUserId(Long userId);

    @Select("SELECT COUNT(*) FROM jj_message WHERE user_id = #{userId} AND is_read = 0")
    Integer countUnreadByUserId(Long userId);

    /**
     * 带筛选条件的分页查询
     */
    @Select("SELECT * FROM jj_message WHERE user_id = #{userId} AND is_read = #{isRead} " +
            "AND (#{keyword} IS NULL OR #{keyword} = '' OR content LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND (#{startDate} IS NULL OR #{startDate} = '' OR DATE(created_at) >= #{startDate}) " +
            "AND (#{endDate} IS NULL OR #{endDate} = '' OR DATE(created_at) <= #{endDate}) " +
            "ORDER BY created_at DESC LIMIT #{offset}, #{size}")
    List<Message> selectByUserIdWithFilters(
            @Param("userId") Long userId,
            @Param("isRead") Integer isRead,
            @Param("keyword") String keyword,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("offset") int offset,
            @Param("size") int size
    );

    /**
     * 带筛选条件的总数查询
     */
    @Select("SELECT COUNT(*) FROM jj_message WHERE user_id = #{userId} AND is_read = #{isRead} " +
            "AND (#{keyword} IS NULL OR #{keyword} = '' OR content LIKE CONCAT('%', #{keyword}, '%')) " +
            "AND (#{startDate} IS NULL OR #{startDate} = '' OR DATE(created_at) >= #{startDate}) " +
            "AND (#{endDate} IS NULL OR #{endDate} = '' OR DATE(created_at) <= #{endDate})")
    Integer countByUserIdWithFilters(
            @Param("userId") Long userId,
            @Param("isRead") Integer isRead,
            @Param("keyword") String keyword,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );

    /**
     * 批量标记已读
     */
    @Update("UPDATE jj_message SET is_read = 1 WHERE user_id = #{userId} AND is_read = 0")
    void updateAllReadByUserId(@Param("userId") Long userId);
}