package com.homelog.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/common")
@Slf4j
public class CommonController {
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        log.info("收到应用保活请求======================================》");
        return ResponseEntity.ok("ok");
    }
}
