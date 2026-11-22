package com.homelog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homelog.entity.PushSubscription;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Web Push 订阅 Mapper
 */
@Mapper
public interface PushSubscriptionMapper extends BaseMapper<PushSubscription> {

    @Select("SELECT * FROM jj_push_subscription WHERE user_id = #{userId}")
    List<PushSubscription> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM jj_push_subscription WHERE user_id = #{userId} AND endpoint = #{endpoint} LIMIT 1")
    PushSubscription selectByUserIdAndEndpoint(@Param("userId") Long userId, @Param("endpoint") String endpoint);

    @Delete("DELETE FROM jj_push_subscription WHERE user_id = #{userId} AND endpoint = #{endpoint}")
    int deleteByUserIdAndEndpoint(@Param("userId") Long userId, @Param("endpoint") String endpoint);

    @Delete("DELETE FROM jj_push_subscription WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
