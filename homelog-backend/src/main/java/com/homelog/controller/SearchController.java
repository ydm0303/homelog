package com.homelog.controller;

import com.homelog.dto.SearchRequest;
import com.homelog.service.SearchService;
import com.homelog.vo.SearchResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局搜索控制器
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
@Validated
public class SearchController {

    private final SearchService searchService;

    /**
     * 全局搜索
     * GET /api/search?q=关键词&moduleId=&tag=&page=1&size=20&startDate=&endDate=
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> search(@Valid SearchRequest request) {
        log.info("Global search request: {}", request);

        List<SearchResultVO> results = searchService.search(request);
        long total = searchService.count(request);

        Map<String, Object> response = new HashMap<>();
        response.put("data", results);
        response.put("total", total);
        response.put("page", request.getPage());
        response.put("size", request.getSize());

        return ResponseEntity.ok(response);
    }
}