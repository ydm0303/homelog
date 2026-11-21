package com.homelog.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;import org.springframework.security.access.AccessDeniedException;
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

        log.warn("Validation failed: path={}, errors={}", request.getRequestURI(), errors);
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
        log.warn("Illegal argument: path={}, message={}", request.getRequestURI(), ex.getMessage());
        return body;
    }

    /**
     * 处理权限不足异常
     * <p>必须显式列出：本类末尾的 {@code @ExceptionHandler(Exception.class)} 会吞掉
     * 任何未单独声明的异常并返回 500。设计文档 v1.2 §15 M3 —— 越权统一收敛到 403，
     * 不再让「权限不足」伪装成「服务器故障」。
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, Object> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", 403);
        body.put("message", ex.getMessage() != null && !ex.getMessage().trim().isEmpty()
                ? ex.getMessage() : "权限不足，无法访问该资源");
        body.put("path", request.getRequestURI());
        body.put("timestamp", System.currentTimeMillis());
        body.put("traceId", generateTraceId());
        log.warn("Access denied: path={}, reason={}", request.getRequestURI(), ex.getMessage());
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
        log.error("Runtime error: path={}", request.getRequestURI(), ex);
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
        log.error("Unexpected error: path={}", request.getRequestURI(), ex);
        return body;
    }

    private String generateTraceId() {
        return java.util.UUID.randomUUID().toString().substring(0, 8);
    }
}