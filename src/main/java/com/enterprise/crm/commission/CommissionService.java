package com.enterprise.crm.commission;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CommissionService {

    private final JdbcTemplate jdbcTemplate;

    public CommissionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> getCommissionRules() {
        String sql = "SELECT * FROM COMMISSION_RULES ORDER BY min_sales_threshold ASC";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getCommissionRecords(String status) {
        StringBuilder sql = new StringBuilder(
                "SELECT cr.*, u.full_name AS employee_name, u.email AS employee_email, " +
                "o.order_number, i.invoice_number, " +
                "appr.full_name AS approved_by_name " +
                "FROM COMMISSION_RECORDS cr " +
                "JOIN USERS u ON cr.employee_id = u.user_id " +
                "JOIN ORDERS o ON cr.order_id = o.order_id " +
                "LEFT JOIN INVOICES i ON cr.invoice_id = i.invoice_id " +
                "LEFT JOIN USERS appr ON cr.approved_by = appr.user_id " +
                "WHERE 1=1 "
        );

        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            sql.append("AND cr.status = ? ");
            sql.append("ORDER BY cr.record_id DESC");
            return jdbcTemplate.queryForList(sql.toString(), status.trim().toUpperCase());
        }

        sql.append("ORDER BY cr.record_id DESC");
        return jdbcTemplate.queryForList(sql.toString());
    }

    public Map<String, Object> updateCommissionStatus(Long recordId, String newStatus, Long approverUserId) {
        String sql = "UPDATE COMMISSION_RECORDS SET status = ?, approved_by = ?, approved_at = CURRENT_TIMESTAMP " +
                     "WHERE record_id = ?";
        jdbcTemplate.update(sql, newStatus, approverUserId, recordId);

        Map<String, Object> res = new HashMap<>();
        res.put("message", "Commission record #" + recordId + " marked as " + newStatus);
        return res;
    }
}
