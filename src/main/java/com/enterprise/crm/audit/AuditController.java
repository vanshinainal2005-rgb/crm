package com.enterprise.crm.audit;

import com.enterprise.crm.common.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final JdbcTemplate jdbcTemplate;

    public AuditController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/logs")
    public ApiResponse<List<Map<String, Object>>> getAuditLogs() {
        String sql = "SELECT * FROM AUDIT_LOGS ORDER BY created_at DESC FETCH FIRST 50 ROWS ONLY";
        return ApiResponse.success(jdbcTemplate.queryForList(sql));
    }

    @GetMapping("/notifications")
    public ApiResponse<List<Map<String, Object>>> getNotifications(Authentication auth) {
        Long userId = getCurrentUserId(auth);
        String sql = "SELECT * FROM NOTIFICATIONS WHERE user_id = ? ORDER BY created_at DESC";
        return ApiResponse.success(jdbcTemplate.queryForList(sql, userId));
    }

    @PutMapping("/notifications/{id}/read")
    public ApiResponse<Map<String, String>> markRead(@PathVariable Long id) {
        jdbcTemplate.update("UPDATE NOTIFICATIONS SET is_read = 'Y' WHERE notification_id = ?", id);
        return ApiResponse.success(Map.of("message", "Notification marked as read"));
    }

    private Long getCurrentUserId(Authentication auth) {
        if (auth == null || auth.getName() == null) return 1L;
        try {
            return jdbcTemplate.queryForObject("SELECT user_id FROM USERS WHERE LOWER(username) = LOWER(?)", Long.class, auth.getName());
        } catch (Exception e) {
            return 1L;
        }
    }
}
