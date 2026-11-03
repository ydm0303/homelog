package com.homelog.service;

import com.homelog.dto.ExportRequest;
import com.homelog.entity.Field;
import com.homelog.entity.Module;
import com.homelog.entity.Record;
import com.homelog.entity.SpaceMember;
import com.homelog.entity.User;
import com.homelog.mapper.FieldMapper;
import com.homelog.mapper.ModuleMapper;
import com.homelog.mapper.RecordMapper;
import com.homelog.mapper.SpaceMemberMapper;
import com.homelog.mapper.UserMapper;
import com.homelog.vo.ExportResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.stream.Collectors;

/**
 * 数据导出服务
 * 支持单模块 Excel 导出、全空间 ZIP 打包导出
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {

    private final ModuleMapper moduleMapper;
    private final FieldMapper fieldMapper;
    private final RecordMapper recordMapper;
    private final SpaceMemberMapper spaceMemberMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${homelog.storage.local.base-path:./uploads}")
    private String basePath;

    @Value("${homelog.storage.local.base-url:/api/files}")
    private String baseUrl;

    /**
     * 导出数据
     */
    @Transactional(readOnly = true)
    public ExportResultVO export(ExportRequest request) {
        Long spaceId = 1L;

        if ("module".equals(request.getScope())) {
            return exportSingleModule(spaceId, request.getModuleId());
        } else if ("all".equals(request.getScope())) {
            return exportAllSpace(spaceId);
        } else {
            throw new RuntimeException("不支持的导出范围: " + request.getScope());
        }
    }

    /**
     * 单模块导出 Excel
     */
    private ExportResultVO exportSingleModule(Long spaceId, Long moduleId) {
        Module module = moduleMapper.selectById(moduleId);
        if (module == null || !module.getSpaceId().equals(spaceId)) {
            throw new RuntimeException("模块不存在");
        }

        // 获取字段定义
        List<Field> fields = fieldMapper.selectByModuleId(moduleId);
        List<String> fieldKeys = fields.stream().map(Field::getFieldKey).collect(Collectors.toList());
        List<String> fieldLabels = fields.stream().map(Field::getFieldLabel).collect(Collectors.toList());

        // 获取所有记录（不分页）
        List<Record> records = recordMapper.selectBySpaceAndModule(spaceId, moduleId);

        // 获取成员映射
        List<SpaceMember> members = spaceMemberMapper.selectBySpaceId(spaceId);
        Map<Long, String> userIdToName = new HashMap<>();
        for (SpaceMember m : members) {
            User user = userMapper.selectById(m.getUserId());
            if (user != null) {
                userIdToName.put(m.getUserId(), user.getNickName() != null ? user.getNickName() : user.getUsername());
            }
        }

        // 生成 Excel
        String filename = String.format("%s_%s.xlsx", module.getModuleKey(), LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        String filePath = basePath + "/exports/" + filename;

        try {
            generateModuleExcel(filePath, module, fieldKeys, fieldLabels, records, userIdToName);

            File file = new File(filePath);
            long fileSize = file.length();

            ExportResultVO result = new ExportResultVO()
                    .setDownloadUrl(baseUrl + "/exports/" + filename)
                    .setFilename(filename)
                    .setFileSize(fileSize)
                    .setRecordCount(records.size());

            log.info("Module export completed: module={}, records={}, fileSize={}", module.getModuleKey(), records.size(), fileSize);
            return result;

        } catch (IOException e) {
            log.error("Excel generation failed", e);
            throw new RuntimeException("Excel 生成失败: " + e.getMessage());
        }
    }

    /**
     * 全空间导出 ZIP (Excel + 图片)
     */
    private ExportResultVO exportAllSpace(Long spaceId) {
        // 获取所有启用模块
        List<Module> modules = moduleMapper.selectEnabledBySpaceId(spaceId);

        // 获取成员映射
        List<SpaceMember> members = spaceMemberMapper.selectBySpaceId(spaceId);
        Map<Long, String> userIdToName = new HashMap<>();
        for (SpaceMember m : members) {
            User user = userMapper.selectById(m.getUserId());
            if (user != null) {
                userIdToName.put(m.getUserId(), user.getNickName() != null ? user.getNickName() : user.getUsername());
            }
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String zipFilename = String.format("homelog_full_export_%s.zip", timestamp);
        String zipPath = basePath + "/exports/" + zipFilename;

        int totalRecords = 0;

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath))) {
            // 为每个模块生成 Excel 并加入 ZIP
            for (Module module : modules) {
                List<Field> fields = fieldMapper.selectByModuleId(module.getId());
                List<String> fieldKeys = fields.stream().map(Field::getFieldKey).collect(Collectors.toList());
                List<String> fieldLabels = fields.stream().map(Field::getFieldLabel).collect(Collectors.toList());

                List<Record> records = recordMapper.selectBySpaceAndModule(spaceId, module.getId());
                totalRecords += records.size();

                // 生成模块 Excel 到临时文件
                String excelFilename = String.format("%s.xlsx", module.getModuleKey());
                String tempExcelPath = basePath + "/exports/temp_" + excelFilename;

                generateModuleExcel(tempExcelPath, module, fieldKeys, fieldLabels, records, userIdToName);

                // 加入 ZIP
                addFileToZipCorrect(zos, tempExcelPath, "data/" + excelFilename);

                // 清理临时文件
                Files.deleteIfExists(Paths.get(tempExcelPath));
            }

            // 打包所有图片
            packImagesToZip(zos, spaceId);

            File zipFile = new File(zipPath);
            long fileSize = zipFile.length();

            ExportResultVO result = new ExportResultVO()
                    .setDownloadUrl(baseUrl + "/exports/" + zipFilename)
                    .setFilename(zipFilename)
                    .setFileSize(fileSize)
                    .setRecordCount(totalRecords);

            log.info("Full space export completed: modules={}, records={}, fileSize={}", modules.size(), totalRecords, fileSize);
            return result;

        } catch (IOException e) {
            log.error("ZIP generation failed", e);
            throw new RuntimeException("ZIP 生成失败: " + e.getMessage());
        }
    }

    /**
     * 生成单模块 Excel（使用 SXSSFWorkbook 流式写入，支持大数据量）
     */
    private void generateModuleExcel(String filePath, Module module, List<String> fieldKeys,
                                     List<String> fieldLabels, List<Record> records,
                                     Map<Long, String> userIdToName) throws IOException {

        // 确保目录存在
        Files.createDirectories(Paths.get(filePath).getParent());

        // 使用 SXSSFWorkbook (流式写入，窗口大小 100 行)
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet(module.getName());

            // 表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // 数据样式
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setWrapText(true);

            // 写入表头
            Row headerRow = sheet.createRow(0);
            String[] headers = new String[fieldKeys.size() + 7]; // 基础字段 + 动态字段
            headers[0] = "记录ID";
            headers[1] = "标题";
            headers[2] = "创建人";
            headers[3] = "创建时间";
            headers[4] = "更新时间";
            headers[5] = "提醒日期";
            headers[6] = "是否私密";
            for (int i = 0; i < fieldLabels.size(); i++) {
                headers[7 + i] = fieldLabels.get(i);
            }

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 写入数据
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            for (int r = 0; r < records.size(); r++) {
                Record record = records.get(r);
                Row row = sheet.createRow(r + 1);

                // 基础字段
                row.createCell(0).setCellValue(record.getId());
                row.createCell(1).setCellValue(record.getTitle() != null ? record.getTitle() : "");
                row.createCell(2).setCellValue(userIdToName.getOrDefault(record.getCreatorId(), "未知"));
                row.createCell(3).setCellValue(record.getCreatedAt() != null ? record.getCreatedAt().format(dtf) : "");
                row.createCell(4).setCellValue(record.getUpdatedAt() != null ? record.getUpdatedAt().format(dtf) : "");
                row.createCell(5).setCellValue(record.getRemindDate() != null ? record.getRemindDate().format(df) : "");
                row.createCell(6).setCellValue(record.getIsPrivate() == 1 ? "是" : "否");

                // 动态字段
                Map<String, Object> dataJson = parseJson(record.getDataJson());
                for (int i = 0; i < fieldKeys.size(); i++) {
                    String key = fieldKeys.get(i);
                    Object value = dataJson.get(key);
                    Cell cell = row.createCell(7 + i);
                    cell.setCellStyle(dataStyle);
                    if (value != null) {
                        if (value instanceof Number) {
                            cell.setCellValue(((Number) value).doubleValue());
                        } else if (value instanceof Boolean) {
                            cell.setCellValue((Boolean) value);
                        } else {
                            cell.setCellValue(value.toString());
                        }
                    }
                }
            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 写入文件
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }
        }
    }

    /**
     * 打包图片到 ZIP
     */
    private void packImagesToZip(ZipOutputStream zos, Long spaceId) throws IOException {
        Path uploadsDir = Paths.get(basePath);
        if (!Files.exists(uploadsDir)) {
            return;
        }

        Files.walk(uploadsDir)
                .filter(Files::isRegularFile)
                .filter(p -> {
                    String ext = p.toString().substring(p.toString().lastIndexOf(".") + 1).toLowerCase();
                    return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("gif") || ext.equals("webp");
                })
                .forEach(p -> {
                    try {
                        String relativePath = uploadsDir.relativize(p).toString().replace("\\", "/");
                        addFileToZipCorrect(zos, p.toString(), "images/" + relativePath);
                    } catch (IOException e) {
                        log.warn("Failed to add image to zip: {}", p, e);
                    }
                });
    }

    /**
     * 添加文件到 ZIP
     */
    private void addFileToZipCorrect(ZipOutputStream zos, String filePath, String entryName) throws IOException {
        try (java.io.FileInputStream fis = new java.io.FileInputStream(filePath)) {
            ZipEntry entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }
            zos.closeEntry();
        }
    }

    /**
     * 解析 JSON 字符串
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}