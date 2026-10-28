package com.enterprise.crm.customer;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;

import java.sql.Types;
import java.util.*;

@Service
public class CustomerService {

    private final JdbcTemplate jdbcTemplate;

    public CustomerService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> getAllCustomers(String search, String category, String status) {
        StringBuilder sql = new StringBuilder(
                "SELECT c.customer_id, c.customer_code, c.name, c.email, c.phone, c.company, " +
                "c.customer_category, c.status, c.created_at, " +
                "u.full_name AS assigned_employee_name, " +
                "(SELECT COUNT(*) FROM OPPORTUNITIES WHERE customer_id = c.customer_id) AS opp_count, " +
                "(SELECT NVL(SUM(deal_value), 0) FROM OPPORTUNITIES WHERE customer_id = c.customer_id) AS total_pipeline_value " +
                "FROM CUSTOMERS c " +
                "LEFT JOIN USERS u ON c.assigned_employee_id = u.user_id " +
                "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(c.name) LIKE ? OR LOWER(c.email) LIKE ? OR LOWER(c.company) LIKE ? OR c.customer_code LIKE ?) ");
            String q = "%" + search.trim().toLowerCase() + "%";
            params.add(q);
            params.add(q);
            params.add(q);
            params.add(q);
        }

        if (category != null && !category.trim().isEmpty() && !"ALL".equalsIgnoreCase(category)) {
            sql.append("AND c.customer_category = ? ");
            params.add(category.trim().toUpperCase());
        }

        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            sql.append("AND c.status = ? ");
            params.add(status.trim().toUpperCase());
        }

        sql.append("ORDER BY c.customer_id DESC");

        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public Map<String, Object> getCustomerById(Long customerId) {
        String custSql = "SELECT c.*, u.full_name AS assigned_employee_name FROM CUSTOMERS c " +
                         "LEFT JOIN USERS u ON c.assigned_employee_id = u.user_id " +
                         "WHERE c.customer_id = ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(custSql, customerId);
        if (rows.isEmpty()) {
            throw new RuntimeException("Customer not found with ID: " + customerId);
        }

        Map<String, Object> customer = new HashMap<>(rows.get(0));

        // Associated Contacts
        String contactsSql = "SELECT * FROM CONTACTS WHERE customer_id = ? ORDER BY is_primary DESC, contact_id ASC";
        customer.put("contacts", jdbcTemplate.queryForList(contactsSql, customerId));

        // Associated Opportunities
        String oppSql = "SELECT o.*, u.full_name AS assigned_to_name FROM OPPORTUNITIES o " +
                        "LEFT JOIN USERS u ON o.assigned_to = u.user_id " +
                        "WHERE o.customer_id = ? ORDER BY o.created_at DESC";
        customer.put("opportunities", jdbcTemplate.queryForList(oppSql, customerId));

        // Associated Invoices
        String invSql = "SELECT * FROM INVOICES WHERE customer_id = ? ORDER BY issue_date DESC";
        customer.put("invoices", jdbcTemplate.queryForList(invSql, customerId));

        // Associated Tasks
        String taskSql = "SELECT t.*, u.full_name AS assigned_to_name FROM TASKS t " +
                         "LEFT JOIN USERS u ON t.assigned_to = u.user_id " +
                         "WHERE t.customer_id = ? ORDER BY t.due_date ASC";
        customer.put("tasks", jdbcTemplate.queryForList(taskSql, customerId));

        return customer;
    }

    public Map<String, Object> createCustomer(Map<String, Object> payload, Long currentUserId) {
        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName("CRM_CUSTOMER_PKG")
                .withProcedureName("CREATE_CUSTOMER")
                .declareParameters(
                        new SqlParameter("P_NAME", Types.VARCHAR),
                        new SqlParameter("P_EMAIL", Types.VARCHAR),
                        new SqlParameter("P_PHONE", Types.VARCHAR),
                        new SqlParameter("P_COMPANY", Types.VARCHAR),
                        new SqlParameter("P_BILLING_ADDRESS", Types.VARCHAR),
                        new SqlParameter("P_SHIPPING_ADDRESS", Types.VARCHAR),
                        new SqlParameter("P_CATEGORY", Types.VARCHAR),
                        new SqlParameter("P_ASSIGNED_EMPLOYEE_ID", Types.NUMERIC),
                        new SqlParameter("P_NOTES", Types.CLOB),
                        new SqlParameter("P_CREATED_BY", Types.NUMERIC),
                        new SqlOutParameter("O_CUSTOMER_ID", Types.NUMERIC),
                        new SqlOutParameter("O_CUSTOMER_CODE", Types.VARCHAR),
                        new SqlOutParameter("O_STATUS", Types.VARCHAR),
                        new SqlOutParameter("O_MESSAGE", Types.VARCHAR)
                );

        Map<String, Object> inParams = new HashMap<>();
        inParams.put("P_NAME", payload.get("name"));
        inParams.put("P_EMAIL", payload.get("email"));
        inParams.put("P_PHONE", payload.get("phone"));
        inParams.put("P_COMPANY", payload.get("company"));
        inParams.put("P_BILLING_ADDRESS", payload.get("billingAddress"));
        inParams.put("P_SHIPPING_ADDRESS", payload.get("shippingAddress"));
        inParams.put("P_CATEGORY", payload.getOrDefault("category", "STANDARD"));
        inParams.put("P_ASSIGNED_EMPLOYEE_ID", payload.get("assignedEmployeeId"));
        inParams.put("P_NOTES", payload.get("notes"));
        inParams.put("P_CREATED_BY", currentUserId != null ? currentUserId : 1L);

        Map<String, Object> out = jdbcCall.execute(inParams);
        String status = (String) out.get("O_STATUS");
        String message = (String) out.get("O_MESSAGE");

        if ("ERROR".equalsIgnoreCase(status)) {
            throw new RuntimeException(message);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("customerId", out.get("O_CUSTOMER_ID"));
        result.put("customerCode", out.get("O_CUSTOMER_CODE"));
        result.put("message", message);
        return result;
    }

    public Map<String, Object> updateCustomer(Long customerId, Map<String, Object> payload, Long currentUserId) {
        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName("CRM_CUSTOMER_PKG")
                .withProcedureName("UPDATE_CUSTOMER")
                .declareParameters(
                        new SqlParameter("P_CUSTOMER_ID", Types.NUMERIC),
                        new SqlParameter("P_NAME", Types.VARCHAR),
                        new SqlParameter("P_EMAIL", Types.VARCHAR),
                        new SqlParameter("P_PHONE", Types.VARCHAR),
                        new SqlParameter("P_COMPANY", Types.VARCHAR),
                        new SqlParameter("P_BILLING_ADDRESS", Types.VARCHAR),
                        new SqlParameter("P_SHIPPING_ADDRESS", Types.VARCHAR),
                        new SqlParameter("P_CATEGORY", Types.VARCHAR),
                        new SqlParameter("P_ASSIGNED_EMPLOYEE_ID", Types.NUMERIC),
                        new SqlParameter("P_STATUS", Types.VARCHAR),
                        new SqlParameter("P_NOTES", Types.CLOB),
                        new SqlParameter("P_UPDATED_BY", Types.NUMERIC),
                        new SqlOutParameter("O_STATUS", Types.VARCHAR),
                        new SqlOutParameter("O_MESSAGE", Types.VARCHAR)
                );

        Map<String, Object> inParams = new HashMap<>();
        inParams.put("P_CUSTOMER_ID", customerId);
        inParams.put("P_NAME", payload.get("name"));
        inParams.put("P_EMAIL", payload.get("email"));
        inParams.put("P_PHONE", payload.get("phone"));
        inParams.put("P_COMPANY", payload.get("company"));
        inParams.put("P_BILLING_ADDRESS", payload.get("billingAddress"));
        inParams.put("P_SHIPPING_ADDRESS", payload.get("shippingAddress"));
        inParams.put("P_CATEGORY", payload.get("category"));
        inParams.put("P_ASSIGNED_EMPLOYEE_ID", payload.get("assignedEmployeeId"));
        inParams.put("P_STATUS", payload.get("status"));
        inParams.put("P_NOTES", payload.get("notes"));
        inParams.put("P_UPDATED_BY", currentUserId != null ? currentUserId : 1L);

        Map<String, Object> out = jdbcCall.execute(inParams);
        String status = (String) out.get("O_STATUS");
        String message = (String) out.get("O_MESSAGE");

        if ("ERROR".equalsIgnoreCase(status)) {
            throw new RuntimeException(message);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("message", message);
        return res;
    }

    public Map<String, Object> checkDuplicate(String email, String phone, Long excludeId) {
        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName("CRM_CUSTOMER_PKG")
                .withProcedureName("DETECT_DUPLICATE_CUSTOMER")
                .declareParameters(
                        new SqlParameter("P_EMAIL", Types.VARCHAR),
                        new SqlParameter("P_PHONE", Types.VARCHAR),
                        new SqlParameter("P_EXCLUDE_ID", Types.NUMERIC),
                        new SqlOutParameter("O_IS_DUPLICATE", Types.VARCHAR),
                        new SqlOutParameter("O_DUP_CUST_ID", Types.NUMERIC),
                        new SqlOutParameter("O_DUP_CUST_NAME", Types.VARCHAR)
                );

        Map<String, Object> inParams = new HashMap<>();
        inParams.put("P_EMAIL", email);
        inParams.put("P_PHONE", phone);
        inParams.put("P_EXCLUDE_ID", excludeId);

        Map<String, Object> out = jdbcCall.execute(inParams);
        Map<String, Object> result = new HashMap<>();
        result.put("isDuplicate", "Y".equalsIgnoreCase((String) out.get("O_IS_DUPLICATE")));
        result.put("duplicateCustomerId", out.get("O_DUP_CUST_ID"));
        result.put("duplicateCustomerName", out.get("O_DUP_CUST_NAME"));
        return result;
    }
}
