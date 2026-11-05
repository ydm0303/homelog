package com.homelog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homelog.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM jj_user WHERE username = #{username}")
    User selectByUsername(String username);

    @Select("SELECT * FROM jj_user WHERE email = #{email}")
    User selectByEmail(String email);
}