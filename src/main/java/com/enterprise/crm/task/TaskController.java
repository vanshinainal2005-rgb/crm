package com.enterprise.crm.task;

import com.enterprise.crm.common.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final JdbcTemplate jdbcTemplate;

    public TaskController(TaskService taskService, JdbcTemplate jdbcTemplate) {
        this.taskService = taskService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> getTasks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long assignedTo) {
        return ApiResponse.success(taskService.getAllTasks(status, priority, assignedTo));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> createTask(
            @RequestBody Map<String, Object> payload,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        return ApiResponse.success(taskService.createTask(payload, userId), "Task created");
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Map<String, Object>> updateTaskStatus(
            @PathVariable Long id,
            @RequestParam String status,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        return ApiResponse.success(taskService.updateTaskStatus(id, status, userId), "Task status updated");
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
