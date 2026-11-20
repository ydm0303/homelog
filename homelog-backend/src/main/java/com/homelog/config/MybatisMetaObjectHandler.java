package com.homelog.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器。
 *
 * <p>所有实体的 {@code created_at}/{@code updated_at} 都标了
 * {@code @TableField(fill = FieldFill.INSERT [INSERT_UPDATE])}，MP 会把这类字段
 * <b>无条件</b>写进 INSERT/UPDATE 语句，不再走数据库的 {@code DEFAULT CURRENT_TIMESTAMP}。
 * 若没有本处理器，凡是未手动 {@code setCreatedAt(...)} 的插入（如注册时建空间、
 * 建 owner 成员关系、按模板实例化模块/字段）都会以 NULL 落库，
 * 触发 {@code Column 'created_at' cannot be null}。
 *
 * <p>用 strict 系列：仅当字段为 null 时填充，已有的手动赋值保持不变，二者不会冲突。
 */
@Component
@Slf4j
public class MybatisMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        log.info("自动填充处理器插入创建时间:{}", now);
        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
