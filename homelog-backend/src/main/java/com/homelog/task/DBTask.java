package com.homelog.task;

import com.homelog.entity.Space;
import com.homelog.mapper.SpaceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DBTask {

    private final SpaceMapper spaceMapper;

    /**
     * 每天 08:00 执行到期扫描
     */
    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void dbTask() {
        // 查询当天到期的记录
        Space space = spaceMapper.selectByOwnerId(1L);
        log.info("---------->数据库保活任务---------->");
    }
}
