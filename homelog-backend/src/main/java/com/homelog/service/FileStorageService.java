package com.homelog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件存储服务（本地存储）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    @Value("${homelog.storage.local.base-path:./uploads}")
    private String basePath;

    @Value("${homelog.storage.local.base-url:/api/files}")
    private String baseUrl;

    /**
     * 上传文件
     *
     * @param file      文件
     * @param moduleKey 模块 key（用于目录隔离）
     * @param recordId  记录 ID（可选，用于目录隔离）
     * @return 相对访问路径（如 /api/files/drug/123/uuid.jpg）
     */
    public String upload(MultipartFile file, String moduleKey, Long recordId) {
        // 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("只能上传图片文件");
        }

        // 校验文件大小（5MB）
        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("图片大小不能超过 5MB");
        }

        try {
            // 生成存储路径：/moduleKey/recordId/ 或 /moduleKey/temp/
            String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String dirPath;
            if (recordId != null) {
                dirPath = String.format("%s/%s/%d/%s", basePath, moduleKey, recordId, datePath);
            } else {
                dirPath = String.format("%s/%s/temp/%s", basePath, moduleKey, datePath);
            }

            Path dir = Paths.get(dirPath);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            // 生成文件名：uuid + 原扩展名
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString().replace("-", "") + extension;

            // 保存文件
            Path filePath = dir.resolve(filename);
            Files.write(filePath, file.getBytes());

            // 返回相对访问路径
            String relativePath;
            if (recordId != null) {
                relativePath = String.format("%s/%s/%d/%s/%s", baseUrl, moduleKey, recordId, datePath, filename);
            } else {
                relativePath = String.format("%s/%s/temp/%s/%s", baseUrl, moduleKey, datePath, filename);
            }

            log.info("文件上传成功：{} -> {}", originalFilename, relativePath);
            return relativePath;

        } catch (IOException e) {
            log.error("File upload failed", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     */
    public void delete(String relativePath) {
        try {
            // 从相对路径解析物理路径
            String pathStr = relativePath.replace(baseUrl, basePath);
            Path filePath = Paths.get(pathStr);
            Files.deleteIfExists(filePath);
            log.info("文件删除成功：{}", relativePath);
        } catch (IOException e) {
            log.warn("File delete failed: {}", relativePath, e);
        }
    }
}