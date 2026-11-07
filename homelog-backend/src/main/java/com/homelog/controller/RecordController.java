package com.homelog.controller;

import com.homelog.dto.RecordCreateRequest;
import com.homelog.dto.RecordUpdateRequest;
import com.homelog.dto.RecordDeleteRequest;
import com.homelog.dto.RecordDetailRequest;
import com.homelog.service.RecordService;
import com.homelog.vo.RecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 记录控制器 - 通用 CRUD 接口
 * 规范：只使用 GET/POST，参数通过 query 或 body 传递
 */
@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
@Slf4j
@Validated
public class RecordController {

    private final RecordService recordService;

    /**
     * 列表查询
     * GET /api/records/list?moduleId=1&page=1&size=20&keyword=&tag=
     */
    @GetMapping("/list")
    public ResponseEntity<List<RecordVO>> list(
            @RequestParam Long moduleId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag) {

        log.info("List records: moduleId={}, page={}, size={}, keyword={}, tag={}", moduleId, page, size, keyword, tag);

        List<RecordVO> records = recordService.list(moduleId, page, size, keyword, tag);
        log.info("List records result: moduleId={}, count={}", moduleId, records.size());
        return ResponseEntity.ok(records);
    }

    /**
     * 详情查询
     * GET /api/records/detail?recordId=xxx&moduleId=xxx
     */
    @GetMapping("/detail")
    public ResponseEntity<RecordVO> detail(@Valid RecordDetailRequest request) {

        log.info("Get record detail: moduleId={}, recordId={}", request.getModuleId(), request.getRecordId());

        RecordVO record = recordService.get(request.getModuleId(), request.getRecordId());
        if (record == null) {
            log.warn("Record not found or not accessible: moduleId={}, recordId={}",
                    request.getModuleId(), request.getRecordId());
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(record);
    }

    /**
     * 创建记录
     * POST /api/records/create
     */
    @PostMapping("/create")
    public ResponseEntity<RecordVO> create(@Valid @RequestBody RecordCreateRequest request) {
        log.info("Create record: moduleId={}, title={}", request.getModuleId(), request.getTitle());

        RecordVO record = recordService.create(request);
        log.info("Record created: moduleId={}, recordId={}", request.getModuleId(), record.getId());
        return ResponseEntity.ok(record);
    }

    /**
     * 更新记录
     * POST /api/records/update
     */
    @PostMapping("/update")
    public ResponseEntity<RecordVO> update(@Valid @RequestBody RecordUpdateRequest request) {
        log.info("Update record: moduleId={}, recordId={}", request.getModuleId(), request.getRecordId());

        RecordVO record = recordService.update(request.getModuleId(), request.getRecordId(), request);
        log.info("Record updated: moduleId={}, recordId={}", request.getModuleId(), request.getRecordId());
        return ResponseEntity.ok(record);
    }

    /**
     * 删除记录
     * POST /api/records/delete
     */
    @PostMapping("/delete")
    public ResponseEntity<Void> delete(@Valid @RequestBody RecordDeleteRequest request) {
        log.info("Delete record: moduleId={}, recordId={}", request.getModuleId(), request.getRecordId());

        recordService.delete(request.getModuleId(), request.getRecordId());
        log.info("Record deleted: moduleId={}, recordId={}", request.getModuleId(), request.getRecordId());
        return ResponseEntity.ok().build();
    }
}