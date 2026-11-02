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

        List<RecordVO> records = recordService.list(moduleId, page, size, keyword, tag);
        return ResponseEntity.ok(records);
    }

    /**
     * 详情查询
     * GET /api/records/detail?recordId=xxx&moduleId=xxx
     */
    @GetMapping("/detail")
    public ResponseEntity<RecordVO> detail(@Valid RecordDetailRequest request) {

        RecordVO record = recordService.get(request.getModuleId(), request.getRecordId());
        if (record == null) {
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
        RecordVO record = recordService.create(request);
        return ResponseEntity.ok(record);
    }

    /**
     * 更新记录
     * POST /api/records/update
     */
    @PostMapping("/update")
    public ResponseEntity<RecordVO> update(@Valid @RequestBody RecordUpdateRequest request) {

        RecordVO record = recordService.update(request.getModuleId(), request.getRecordId(), request);
        return ResponseEntity.ok(record);
    }

    /**
     * 删除记录
     * POST /api/records/delete
     */
    @PostMapping("/delete")
    public ResponseEntity<Void> delete(@Valid @RequestBody RecordDeleteRequest request) {

        recordService.delete(request.getModuleId(), request.getRecordId());
        return ResponseEntity.ok().build();
    }
}