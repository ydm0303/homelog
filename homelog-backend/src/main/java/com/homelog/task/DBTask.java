package com.homelog.task;

import com.homelog.mapper.SpaceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 数据库保活任务
 *
 * <p>目的只是周期性触碰一次数据库，保持连接池活跃，不产生业务副作用。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DBTask {

    private final SpaceMapper spaceMapper;

    /**
     * 每天 08:00 执行
     *
     * <p>原实现写死 {@code selectByOwnerId(1L)}，把保活查询绑在了「user 1 存在且拥有空间」上。
     * 步 3 支持多空间、且 owner 由各自私有空间决定后，这个假设不再成立（user 1 未必是任何空间owner，
     * 或其空间可能已解散），保活任务会静默查空。改为与具体用户无关的活跃空间计数。
     */
    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void dbTask() {
        int activeSpaces = spaceMapper.selectAllActive().size();
        log.info("---------->数据库保活任务---------->活跃空间数={}", activeSpaces);
    }
}
