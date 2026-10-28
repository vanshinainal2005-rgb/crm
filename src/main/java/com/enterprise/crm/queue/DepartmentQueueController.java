package com.enterprise.crm.queue;

import com.enterprise.crm.common.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/queues")
public class DepartmentQueueController {

    private final DepartmentQueueService queueService;
    private final JdbcTemplate jdbcTemplate;

    public DepartmentQueueController(DepartmentQueueService queueService, JdbcTemplate jdbcTemplate) {
        this.queueService = queueService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/summary")
    public ApiResponse<List<Map<String, Object>>> getSummary() {
        return ApiResponse.success(queueService.getQueuesSummary());
    }

    @GetMapping("/items")
    public ApiResponse<List<Map<String, Object>>> getItems(
            @RequestParam(required = false) String queue,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(queueService.getQueueItems(queue, status));
    }

    @PutMapping("/items/{id}/status")
    public ApiResponse<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        return ApiResponse.success(queueService.updateItemStatus(id, status, userId), "Queue item updated");
    }

    @PostMapping("/items")
    public ApiResponse<Map<String, Object>> createItem(
            @RequestBody Map<String, Object> payload,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        return ApiResponse.success(queueService.createQueueItem(payload, userId), "Item enqueued");
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
