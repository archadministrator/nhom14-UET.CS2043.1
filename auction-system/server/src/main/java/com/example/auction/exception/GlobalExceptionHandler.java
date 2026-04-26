package com.example.auction.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Lớp xử lý lỗi tập trung cho toàn bộ ứng dụng (Abstraction).
 * Giúp trừu tượng hóa việc bắt lỗi và định dạng phản hồi API nhất quán.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", "Đã xảy ra lỗi hệ thống: " + ex.getMessage());
        body.put("status", 500);

        return ResponseEntity.internalServerError().body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", ex.getMessage());
        body.put("status", 400);

        return ResponseEntity.badRequest().body(body);
    }
}
