package com.homelog.controller;

import com.homelog.service.ImportService;
import com.homelog.vo.ImportResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 导入控制器（仅药品模块 CSV 导入）
 */
@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
@Slf4j
public class ImportController {

    private final ImportService importService;

    /**
     * 预检：解析 CSV，验证数据，返回错误详情（不入库）
     * POST /api/import/drug/csv/preview
     */
    @PostMapping("/drug/csv/preview")
    public ResponseEntity<ImportResultVO> preview(
            @RequestParam("file") MultipartFile file) {
        log.info("收到药品 CSV 预检导入请求：文件名={}，大小={}", file.getOriginalFilename(), file.getSize());
        ImportResultVO result = importService.preview(file);
        log.info("药品 CSV 预检导入完成：总行数={}，成功={}，失败={}",
                result.getTotalRows(), result.getSuccessRows(), result.getFailedRows());
        return ResponseEntity.ok(result);
    }

    /**
     * 正式导入：解析 CSV，验证通过的数据入库
     * POST /api/import/drug/csv
     */
    @PostMapping("/drug/csv")
    public ResponseEntity<ImportResultVO> importDrug(
            @RequestParam("file") MultipartFile file) {
        log.info("收到药品 CSV 导入请求：文件名={}，大小={}", file.getOriginalFilename(), file.getSize());
        ImportResultVO result = importService.importDrug(file);
        log.info("药品 CSV 导入完成：总行数={}，成功={}，失败={}",
                result.getTotalRows(), result.getSuccessRows(), result.getFailedRows());
        return ResponseEntity.ok(result);
    }

    /**
     * 下载错误 CSV
     * GET /api/import/errors/{filename}
     */
    @GetMapping("/errors/{filename}")
    public ResponseEntity<byte[]> downloadErrorCsv(@PathVariable String filename) {
        // 这里简化处理，实际应从文件系统读取
        Map<String, Object> response = new HashMap<>();
        response.put("message", "错误文件下载功能待完善");
        return ResponseEntity.ok().build();
    }
}