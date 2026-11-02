package com.homelog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homelog.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

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
}