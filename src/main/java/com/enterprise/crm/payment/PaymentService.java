package com.enterprise.crm.payment;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;

import java.sql.Types;
import java.util.*;

@Service
public class PaymentService {

    private final JdbcTemplate jdbcTemplate;

    public PaymentService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> getAllPayments(String search) {
        StringBuilder sql = new StringBuilder(
                "SELECT p.*, i.invoice_number, i.total_amount AS invoice_total, " +
                "c.name AS customer_name, c.customer_code, u.full_name AS recorded_by_name " +
                "FROM PAYMENTS p " +
                "JOIN INVOICES i ON p.invoice_id = i.invoice_id " +
                "JOIN CUSTOMERS c ON p.customer_id = c.customer_id " +
                "LEFT JOIN USERS u ON p.recorded_by = u.user_id " +
                "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(p.payment_reference) LIKE ? OR LOWER(i.invoice_number) LIKE ? OR LOWER(c.name) LIKE ?) ");
            String q = "%" + search.trim().toLowerCase() + "%";
            params.add(q);
            params.add(q);
            params.add(q);
        }

        sql.append("ORDER BY p.payment_id DESC");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public Map<String, Object> recordPayment(Map<String, Object> payload, Long currentUserId) {
        Long invoiceId = ((Number) payload.get("invoiceId")).longValue();
        Double amount = ((Number) payload.get("amount")).doubleValue();
        String method = (String) payload.getOrDefault("paymentMethod", "BANK_TRANSFER");
        String notes = (String) payload.get("notes");

        String paymentRef = (String) payload.get("paymentReference");
        if (paymentRef == null || paymentRef.trim().isEmpty()) {
            paymentRef = "PAY-" + System.currentTimeMillis();
        }

        String idempotencyKey = (String) payload.get("idempotencyKey");
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            idempotencyKey = "IDEMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName("CRM_FINANCE_PKG")
                .withProcedureName("RECORD_PAYMENT")
                .declareParameters(
                        new SqlParameter("P_INVOICE_ID", Types.NUMERIC),
                        new SqlParameter("P_AMOUNT", Types.NUMERIC),
                        new SqlParameter("P_PAYMENT_METHOD", Types.VARCHAR),
                        new SqlParameter("P_PAYMENT_REF", Types.VARCHAR),
                        new SqlParameter("P_IDEMPOTENCY_KEY", Types.VARCHAR),
                        new SqlParameter("P_NOTES", Types.VARCHAR),
                        new SqlParameter("P_RECORDED_BY", Types.NUMERIC),
                        new SqlOutParameter("O_PAYMENT_ID", Types.NUMERIC),
                        new SqlOutParameter("O_REMAINING_BAL", Types.NUMERIC),
                        new SqlOutParameter("O_STATUS", Types.VARCHAR),
                        new SqlOutParameter("O_MESSAGE", Types.VARCHAR)
                );

        Map<String, Object> inParams = new HashMap<>();
        inParams.put("P_INVOICE_ID", invoiceId);
        inParams.put("P_AMOUNT", amount);
        inParams.put("P_PAYMENT_METHOD", method);
        inParams.put("P_PAYMENT_REF", paymentRef);
        inParams.put("P_IDEMPOTENCY_KEY", idempotencyKey);
        inParams.put("P_NOTES", notes);
        inParams.put("P_RECORDED_BY", currentUserId != null ? currentUserId : 1L);

        Map<String, Object> out = jdbcCall.execute(inParams);
        String status = (String) out.get("O_STATUS");
        String message = (String) out.get("O_MESSAGE");

        if ("ERROR".equalsIgnoreCase(status)) {
            throw new RuntimeException(message);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("paymentId", out.get("O_PAYMENT_ID"));
        result.put("remainingBalance", out.get("O_REMAINING_BAL"));
        result.put("message", message);
        return result;
    }
}
