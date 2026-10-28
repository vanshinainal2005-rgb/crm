package com.enterprise.crm.task;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;

@Service
public class TaskService {

    private final JdbcTemplate jdbcTemplate;

    public TaskService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> getAllTasks(String status, String priority, Long assignedTo) {
        StringBuilder sql = new StringBuilder(
                "SELECT t.*, u.full_name AS assigned_to_name, c.name AS customer_name, o.name AS opportunity_name " +
                "FROM TASKS t " +
                "JOIN USERS u ON t.assigned_to = u.user_id " +
                "LEFT JOIN CUSTOMERS c ON t.customer_id = c.customer_id " +
                "LEFT JOIN OPPORTUNITIES o ON t.opportunity_id = o.opportunity_id " +
                "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            sql.append("AND t.status = ? ");
            params.add(status.trim().toUpperCase());
        }

        if (priority != null && !priority.trim().isEmpty() && !"ALL".equalsIgnoreCase(priority)) {
            sql.append("AND t.priority = ? ");
            params.add(priority.trim().toUpperCase());
        }

        if (assignedTo != null) {
            sql.append("AND t.assigned_to = ? ");
            params.add(assignedTo);
        }

        sql.append("ORDER BY CASE t.priority WHEN 'URGENT' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 ELSE 4 END, t.due_date ASC");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public Map<String, Object> createTask(Map<String, Object> payload, Long currentUserId) {
        String title = (String) payload.get("title");
        String description = (String) payload.get("description");
        Long customerId = payload.get("customerId") != null ? ((Number) payload.get("customerId")).longValue() : null;
        Long opportunityId = payload.get("opportunityId") != null ? ((Number) payload.get("opportunityId")).longValue() : null;
        Long assignedTo = payload.get("assignedTo") != null ? ((Number) payload.get("assignedTo")).longValue() : currentUserId;
        String priority = (String) payload.getOrDefault("priority", "MEDIUM");
        String dueDateStr = (String) payload.get("dueDate");

        Timestamp dueDate = dueDateStr != null ? Timestamp.valueOf(dueDateStr.replace("T", " ").substring(0, 19)) : new Timestamp(System.currentTimeMillis() + 86400000L);

        String sql = "INSERT INTO TASKS (title, description, customer_id, opportunity_id, assigned_to, created_by, priority, status, due_date, reminder_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING', ?, ?)";
        jdbcTemplate.update(sql, title, description, customerId, opportunityId, assignedTo, currentUserId, priority, dueDate, dueDate);

        // Add to activity logs
        if (customerId != null) {
            jdbcTemplate.update("INSERT INTO ACTIVITY_LOGS (entity_type, entity_id, activity_type, summary, performed_by) VALUES (?, ?, ?, ?, ?)",
                    "CUSTOMER", customerId, "TASK", "Task created: " + title, currentUserId);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("message", "Task successfully created");
        return res;
    }

    public Map<String, Object> updateTaskStatus(Long taskId, String newStatus, Long currentUserId) {
        String sql = "UPDATE TASKS SET status = ?, completed_at = CASE WHEN ? = 'COMPLETED' THEN CURRENT_TIMESTAMP ELSE NULL END WHERE task_id = ?";
        jdbcTemplate.update(sql, newStatus, newStatus, taskId);

        Map<String, Object> res = new HashMap<>();
        res.put("message", "Task status updated to " + newStatus);
        return res;
    }
}
