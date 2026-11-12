package com.homelog.service;

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
import com.homelog.vo.ImportErrorVO;
import com.homelog.vo.ImportResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 药品 CSV 导入服务
 * 固定列头模板：药品名、有效期、存放位置、用法用量、适用症状、禁忌、购买日期、剩余数量、绑定成员邮箱
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImportService {

    private final ModuleMapper moduleMapper;
    private final FieldMapper fieldMapper;
    private final RecordMapper recordMapper;
    private final SpaceMemberMapper spaceMemberMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${homelog.storage.local.base-path:./uploads}")
    private String basePath;

    // 药品模块固定列头（按顺序）
    private static final String[] DRUG_CSV_HEADERS = {
            "药品名", "有效期", "存放位置", "用法用量", "适用症状", "禁忌", "购买日期", "剩余数量", "绑定成员邮箱"
    };

    // 列头到字段 key 的映射
    private static final String[] DRUG_FIELD_KEYS = {
            "drug_name", "expire_date", "location", "usage_dosage",
            "indications", "contraindication", "purchase_date", "quantity", "member_email"
    };

    /**
     * 预检模式：解析 CSV，验证数据，返回错误详情（不入库）
     */
    public ImportResultVO preview(MultipartFile file) {
        return processImport(file, true);
    }

    /**
     * 正式导入：解析 CSV，验证通过的数据入库，返回统计结果
     */
    @Transactional
    public ImportResultVO importDrug(MultipartFile file) {
        return processImport(file, false);
    }

    /**
     * 核心处理逻辑
     */
    private ImportResultVO processImport(MultipartFile file, boolean previewOnly) {
        ImportResultVO result = new ImportResultVO();
        List<ImportErrorVO> errors = new ArrayList<>();
        int successCount = 0;
        int totalRows = 0;

        log.info("开始处理 CSV 导入：是否仅预检={}", previewOnly);

        // 获取药品模块
        Long spaceId = 1L;
        Module drugModule = moduleMapper.selectBySpaceAndKey(spaceId, "drug");
        if (drugModule == null) {
            throw new RuntimeException("药品模块不存在，请先初始化");
        }

        // 获取药品模块字段定义（用于验证）
        List<Field> fields = fieldMapper.selectByModuleId(drugModule.getId());
        Map<String, Field> fieldMap = fields.stream()
                .collect(Collectors.toMap(Field::getFieldKey, f -> f));

        // 获取空间成员邮箱映射（用于绑定成员）
        List<SpaceMember> members = spaceMemberMapper.selectBySpaceId(spaceId);
        Map<String, Long> emailToUserId = members.stream()
                .collect(Collectors.toMap(
                        m -> {
                            User user = userMapper.selectById(m.getUserId());
                            return user != null ? user.getEmail() : "";
                        },
                        m -> m.getUserId()
                ));

        try (CSVParser parser = CSVParser.parse(file.getInputStream(), StandardCharsets.UTF_8,
                CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim())) {

            // 验证列头
            List<String> headers = parser.getHeaderNames();
            if (!validateHeaders(headers)) {
                throw new RuntimeException("CSV 列头不匹配，期望: " + String.join(",", DRUG_CSV_HEADERS));
            }

            for (CSVRecord record : parser) {
                totalRows++;
                long rowNumLong = record.getRecordNumber() + 1; // 含表头
                int rowNum = (int) rowNumLong;

                try {
                    // 解析行数据
                    Map<String, Object> dataJson = parseRow(record, fieldMap, emailToUserId, errors, rowNum);

                    if (errors.stream().anyMatch(e -> e.getRowNum() == rowNum)) {
                        log.warn("CSV import row {} skipped due to validation errors", rowNum);
                        continue; // 该行有错误，跳过
                    }

                    if (!previewOnly) {
                        // 正式入库
                        saveRecord(drugModule.getId(), dataJson);
                    }
                    successCount++;

                } catch (Exception e) {
                    log.warn("CSV import row {} failed: {}", rowNum, e.getMessage());
                    errors.add(new ImportErrorVO()
                            .setRowNum(rowNum)
                            .setRowData(DRUG_CSV_HEADERS) // 简化：记录列头
                            .setErrorMessage(e.getMessage()));
                }
            }

        } catch (IOException e) {
            log.error("CSV parse failed", e);
            throw new RuntimeException("CSV 解析失败: " + e.getMessage());
        }

        result.setTotalRows(totalRows);
        result.setSuccessRows(successCount);
        result.setFailedRows(errors.size());
        result.setErrors(errors);

        // 预检模式：生成错误 CSV 下载
        if (previewOnly && !errors.isEmpty()) {
            String errorCsvPath = generateErrorCsv(errors);
            result.setErrorCsvPath(errorCsvPath);
        }

        return result;
    }

    /**
     * 验证 CSV 列头
     */
    private boolean validateHeaders(List<String> headers) {
        if (headers == null || headers.size() != DRUG_CSV_HEADERS.length) {
            return false;
        }
        for (int i = 0; i < DRUG_CSV_HEADERS.length; i++) {
            if (!DRUG_CSV_HEADERS[i].equals(headers.get(i).trim())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 解析单行数据
     */
    private Map<String, Object> parseRow(CSVRecord record, Map<String, Field> fieldMap,
                                         Map<String, Long> emailToUserId,
                                         List<ImportErrorVO> errors, int rowNum) {

        Map<String, Object> dataJson = new java.util.HashMap<>();

        for (int i = 0; i < DRUG_FIELD_KEYS.length; i++) {
            String fieldKey = DRUG_FIELD_KEYS[i];
            String value = record.get(i); // 按列索引获取
            Field field = fieldMap.get(fieldKey);

            if (value == null || value.trim().isEmpty()) {
                // 必填字段校验
                if (field != null && field.getIsRequired() == 1) {
                    errors.add(new ImportErrorVO()
                            .setRowNum(rowNum)
                            .setErrorMessage("必填字段为空: " + DRUG_CSV_HEADERS[i]));
                }
                continue;
            }

            value = value.trim();

            // 类型转换与校验
            try {
                switch (fieldKey) {
                    case "drug_name":
                    case "location":
                    case "usage_dosage":
                    case "indications":
                    case "contraindication":
                        dataJson.put(fieldKey, value);
                        break;

                    case "expire_date":
                    case "purchase_date":
                        // 日期格式校验
                        LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
                        dataJson.put(fieldKey, value);
                        break;

                    case "quantity":
                        // 数字校验
                        Integer.parseInt(value);
                        dataJson.put(fieldKey, Integer.parseInt(value));
                        break;

                    case "member_email":
                        // 邮箱校验 + 成员存在性校验
                        if (!value.contains("@")) {
                            errors.add(new ImportErrorVO()
                                    .setRowNum(rowNum)
                                    .setErrorMessage("邮箱格式不正确: " + value));
                        } else if (!emailToUserId.containsKey(value)) {
                            errors.add(new ImportErrorVO()
                                    .setRowNum(rowNum)
                                    .setErrorMessage("成员不存在: " + value));
                        } else {
                            dataJson.put("member_id", emailToUserId.get(value));
                        }
                        break;

                    default:
                        dataJson.put(fieldKey, value);
                }
            } catch (Exception e) {
                errors.add(new ImportErrorVO()
                        .setRowNum(rowNum)
                        .setErrorMessage(DRUG_CSV_HEADERS[i] + " 格式错误: " + e.getMessage()));
            }
        }

        return dataJson;
    }

    /**
     * 保存记录
     */
    private void saveRecord(Long moduleId, Map<String, Object> dataJson) {
        Long spaceId = 1L;
        Long currentUserId = 1L;

        // 处理提醒日期（使用有效期）
        LocalDate remindDate = null;
        if (dataJson.containsKey("expire_date")) {
            remindDate = LocalDate.parse((String) dataJson.get("expire_date"), DateTimeFormatter.ISO_LOCAL_DATE);
        }

        // 处理私密字段
        int isPrivate = 0;
        if (dataJson.containsKey("is_private")) {
            isPrivate = Boolean.TRUE.equals(dataJson.get("is_private")) ? 1 : 0;
        }

        // 构建标题（使用药品名）
        String title = (String) dataJson.getOrDefault("drug_name", "药品记录");

        Record record = new Record()
                .setSpaceId(spaceId)
                .setModuleId(moduleId)
                .setTitle(title)
                .setCreatorId(currentUserId)
                .setIsPrivate(isPrivate)
                .setTags(null)
                .setRemindDate(remindDate)
                .setDataJson(toJsonString(dataJson))
                .setImagesJson("[]")
                .setCreatedAt(LocalDateTime.now())
                .setUpdatedAt(LocalDateTime.now());

        recordMapper.insert(record);
    }

    /**
     * 生成错误 CSV 供下载
     */
    private String generateErrorCsv(List<ImportErrorVO> errors) {
        try {
            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String filename = String.format("import_errors_%s.csv", dateStr);
            String dirPath = basePath + "/import_errors";
            java.nio.file.Path dir = java.nio.file.Paths.get(dirPath);
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Path filePath = dir.resolve(filename);

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
                 CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT
                         .withHeader("行号", "错误信息"))) {

                for (ImportErrorVO error : errors) {
                    printer.printRecord(error.getRowNum(), error.getErrorMessage());
                }
                printer.flush();

                java.nio.file.Files.write(filePath, baos.toByteArray());
            }

            return "/api/files/import_errors/" + filename;

        } catch (IOException e) {
            log.error("Generate error CSV failed", e);
            return null;
        }
    }

    private String toJsonString(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }
}