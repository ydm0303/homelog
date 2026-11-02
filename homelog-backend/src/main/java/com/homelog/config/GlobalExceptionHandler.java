package com.homelog.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器 - 统一错误响应格式
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidationException(Exception ex, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", 40001); // 4xxxx 参数级错误
        body.put("message", "参数校验失败");
        body.put("path", request.getRequestURI());
        body.put("timestamp", System.currentTimeMillis());
        body.put("traceId", generateTraceId());

        Map<String, String> errors = new HashMap<>();
        if (ex instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException e = (MethodArgumentNotValidException) ex;
            e.getBindingResult().getAllErrors().forEach(error -> {
                String fieldName = ((FieldError) error).getField();
                String errorMessage = error.getDefaultMessage();
                errors.put(fieldName, errorMessage);
            });
        } else if (ex instanceof BindException) {
            BindException e = (BindException) ex;
            e.getBindingResult().getAllErrors().forEach(error -> {
                String fieldName = ((FieldError) error).getField();
                String errorMessage = error.getDefaultMessage();
                errors.put(fieldName, errorMessage);
            });
        }
        body.put("details", errors);

        log.warn("Validation failed: {}", errors);
        return body;
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", 40002);
        body.put("message", ex.getMessage());
        body.put("path", request.getRequestURI());
        body.put("timestamp", System.currentTimeMillis());
        body.put("traceId", generateTraceId());
        log.warn("Illegal argument: {}", ex.getMessage());
        return body;
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", 10001); // 1xxxx 系统级错误
        body.put("message", ex.getMessage());
        body.put("path", request.getRequestURI());
        body.put("timestamp", System.currentTimeMillis());
        body.put("traceId", generateTraceId());
        log.error("Runtime error: ", ex);
        return body;
    }

    /**
     * 处理通用异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleException(Exception ex, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", 10000);
        body.put("message", "服务器内部错误");
        body.put("path", request.getRequestURI());
        body.put("timestamp", System.currentTimeMillis());
        body.put("traceId", generateTraceId());
        log.error("Unexpected error: ", ex);
        return body;
    }

    private String generateTraceId() {
        return java.util.UUID.randomUUID().toString().substring(0, 8);
    }
}