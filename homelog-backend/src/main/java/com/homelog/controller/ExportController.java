package com.homelog.controller;

import com.homelog.dto.ExportRequest;
import com.homelog.service.ExportService;
import com.homelog.vo.ExportResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.File;
import java.nio.file.Paths;

/**
 * 导出控制器
 */
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ExportController {

    private final ExportService exportService;

    @Value("${homelog.storage.local.base-path:./uploads}")
    private String basePath;

    /**
     * 导出数据
     * GET /api/export?scope=module&moduleId=1
     * GET /api/export?scope=all
     */
    @GetMapping
    public ResponseEntity<ExportResultVO> export(@Valid ExportRequest request) {
        log.info("收到数据导出请求：范围={}，模块ID={}", request.getScope(), request.getModuleId());
        ExportResultVO result = exportService.export(request);
        return ResponseEntity.ok(result);
    }

    /**
     * 下载导出文件
     * GET /api/export/download/{filename}
     */
    @GetMapping("/download/{filename}")
    public ResponseEntity<FileSystemResource> download(@PathVariable String filename) {
        String filePath = basePath + "/exports/" + filename;
        File file = new File(filePath);

        if (!file.exists()) {
            log.warn("Export file download failed, file not found: {}", filePath);
            return ResponseEntity.notFound().build();
        }

        FileSystemResource resource = new FileSystemResource(file);
        log.info("导出文件下载成功：文件名={}，大小={}", filename, file.length());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.length())
                .body(resource);
    }
}