package com.homelog.controller;

import com.homelog.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传控制器
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileStorageService fileStorageService;

    /**
     * 上传文件
     * POST /api/files/upload
     * 参数: file (multipart), moduleKey (string), recordId (long, optional)
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("moduleKey") String moduleKey,
            @RequestParam(value = "recordId", required = false) Long recordId) {

        log.info("File upload: moduleKey={}, recordId={}, filename={}, size={}",
                moduleKey, recordId, file.getOriginalFilename(), file.getSize());

        String relativePath = fileStorageService.upload(file, moduleKey, recordId);

        Map<String, Object> response = new HashMap<>();
        response.put("url", relativePath);
        response.put("filename", file.getOriginalFilename());
        response.put("size", file.getSize());

        return ResponseEntity.ok(response);
    }
}