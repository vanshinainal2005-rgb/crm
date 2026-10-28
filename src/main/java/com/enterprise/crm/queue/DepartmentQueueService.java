package com.enterprise.crm.queue;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DepartmentQueueService {

    private final JdbcTemplate jdbcTemplate;

    public DepartmentQueueService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> getQueuesSummary() {
        String sql = "SELECT dq.queue_id, dq.queue_name, dq.display_title, dq.description, " +
                     "COUNT(qa.assignment_id) AS total_items, " +
                     "SUM(CASE WHEN qa.status IN ('OPEN', 'IN_PROGRESS') THEN 1 ELSE 0 END) AS pending_items, " +
                     "SUM(CASE WHEN qa.priority = 'CRITICAL' AND qa.status <> 'COMPLETED' THEN 1 ELSE 0 END) AS critical_items " +
                     "FROM DEPARTMENT_QUEUES dq " +
                     "LEFT JOIN QUEUE_ASSIGNMENTS qa ON dq.queue_id = qa.queue_id " +
                     "GROUP BY dq.queue_id, dq.queue_name, dq.display_title, dq.description " +
                     "ORDER BY dq.queue_id ASC";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getQueueItems(String queueName, String status) {
        StringBuilder sql = new StringBuilder(
                "SELECT qa.*, dq.queue_name, dq.display_title AS queue_title, " +
                "u.full_name AS assigned_to_name, creator.full_name AS created_by_name " +
                "FROM QUEUE_ASSIGNMENTS qa " +
                "JOIN DEPARTMENT_QUEUES dq ON qa.queue_id = dq.queue_id " +
                "LEFT JOIN USERS u ON qa.assigned_to = u.user_id " +
                "LEFT JOIN USERS creator ON qa.created_by = creator.user_id " +
                "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();
        if (queueName != null && !queueName.trim().isEmpty() && !"ALL".equalsIgnoreCase(queueName)) {
            sql.append("AND dq.queue_name = ? ");
            params.add(queueName.trim().toUpperCase());
        }

        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            sql.append("AND qa.status = ? ");
            params.add(status.trim().toUpperCase());
        }

        sql.append("ORDER BY CASE qa.priority WHEN 'CRITICAL' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 ELSE 4 END, qa.assignment_id DESC");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public Map<String, Object> updateItemStatus(Long assignmentId, String newStatus, Long userId) {
        String sql = "UPDATE QUEUE_ASSIGNMENTS SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE assignment_id = ?";
        jdbcTemplate.update(sql, newStatus, assignmentId);

        Map<String, Object> res = new HashMap<>();
        res.put("message", "Queue assignment #" + assignmentId + " status updated to " + newStatus);
        return res;
    }

    public Map<String, Object> createQueueItem(Map<String, Object> payload, Long currentUserId) {
        String queueName = (String) payload.get("queueName");
        Long queueId = jdbcTemplate.queryForObject("SELECT queue_id FROM DEPARTMENT_QUEUES WHERE queue_name = ?", Long.class, queueName.toUpperCase());

        String title = (String) payload.get("title");
        String entityType = (String) payload.getOrDefault("entityType", "TASK");
        Long entityId = payload.get("entityId") != null ? ((Number) payload.get("entityId")).longValue() : 0L;
        String priority = (String) payload.getOrDefault("priority", "MEDIUM");
        Long assignedTo = payload.get("assignedTo") != null ? ((Number) payload.get("assignedTo")).longValue() : currentUserId;

        String sql = "INSERT INTO QUEUE_ASSIGNMENTS (queue_id, title, entity_type, entity_id, priority, status, assigned_to, created_by) " +
                     "VALUES (?, ?, ?, ?, ?, 'OPEN', ?, ?)";
        jdbcTemplate.update(sql, queueId, title, entityType, entityId, priority, assignedTo, currentUserId);

        Map<String, Object> res = new HashMap<>();
        res.put("message", "Task successfully enqueued into " + queueName + " queue");
        return res;
    }
}
