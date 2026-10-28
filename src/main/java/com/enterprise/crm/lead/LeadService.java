package com.enterprise.crm.lead;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;

import java.sql.Types;
import java.util.*;

@Service
public class LeadService {

    private final JdbcTemplate jdbcTemplate;

    public LeadService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> getAllLeads(String search, String status) {
        StringBuilder sql = new StringBuilder(
                "SELECT l.*, u.full_name AS assigned_to_name, c.name AS converted_customer_name " +
                "FROM LEADS l " +
                "LEFT JOIN USERS u ON l.assigned_to = u.user_id " +
                "LEFT JOIN CUSTOMERS c ON l.converted_customer_id = c.customer_id " +
                "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(l.first_name) LIKE ? OR LOWER(l.last_name) LIKE ? OR LOWER(l.company) LIKE ? OR LOWER(l.email) LIKE ?) ");
            String q = "%" + search.trim().toLowerCase() + "%";
            params.add(q);
            params.add(q);
            params.add(q);
            params.add(q);
        }

        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            sql.append("AND l.lead_status = ? ");
            params.add(status.trim().toUpperCase());
        }

        sql.append("ORDER BY l.lead_id DESC");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public Map<String, Object> createLead(Map<String, Object> payload, Long currentUserId) {
        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName("CRM_SALES_PKG")
                .withProcedureName("CREATE_LEAD")
                .declareParameters(
                        new SqlParameter("P_FIRST_NAME", Types.VARCHAR),
                        new SqlParameter("P_LAST_NAME", Types.VARCHAR),
                        new SqlParameter("P_COMPANY", Types.VARCHAR),
                        new SqlParameter("P_EMAIL", Types.VARCHAR),
                        new SqlParameter("P_PHONE", Types.VARCHAR),
                        new SqlParameter("P_LEAD_SOURCE", Types.VARCHAR),
                        new SqlParameter("P_ASSIGNED_TO", Types.NUMERIC),
                        new SqlParameter("P_BUDGET", Types.NUMERIC),
                        new SqlParameter("P_NOTES", Types.VARCHAR),
                        new SqlParameter("P_CREATED_BY", Types.NUMERIC),
                        new SqlOutParameter("O_LEAD_ID", Types.NUMERIC),
                        new SqlOutParameter("O_STATUS", Types.VARCHAR),
                        new SqlOutParameter("O_MESSAGE", Types.VARCHAR)
                );

        Map<String, Object> inParams = new HashMap<>();
        inParams.put("P_FIRST_NAME", payload.get("firstName"));
        inParams.put("P_LAST_NAME", payload.get("lastName"));
        inParams.put("P_COMPANY", payload.get("company"));
        inParams.put("P_EMAIL", payload.get("email"));
        inParams.put("P_PHONE", payload.get("phone"));
        inParams.put("P_LEAD_SOURCE", payload.getOrDefault("leadSource", "WEBSITE"));
        inParams.put("P_ASSIGNED_TO", payload.get("assignedTo"));
        inParams.put("P_BUDGET", payload.getOrDefault("estimatedBudget", 0));
        inParams.put("P_NOTES", payload.get("notes"));
        inParams.put("P_CREATED_BY", currentUserId != null ? currentUserId : 1L);

        Map<String, Object> out = jdbcCall.execute(inParams);
        String status = (String) out.get("O_STATUS");
        String message = (String) out.get("O_MESSAGE");

        if ("ERROR".equalsIgnoreCase(status)) {
            throw new RuntimeException(message);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("leadId", out.get("O_LEAD_ID"));
        result.put("message", message);
        return result;
    }

    public Map<String, Object> convertLead(Long leadId, Map<String, Object> payload, Long currentUserId) {
        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName("CRM_SALES_PKG")
                .withProcedureName("CONVERT_LEAD")
                .declareParameters(
                        new SqlParameter("P_LEAD_ID", Types.NUMERIC),
                        new SqlParameter("P_OPPORTUNITY_NAME", Types.VARCHAR),
                        new SqlParameter("P_DEAL_VALUE", Types.NUMERIC),
                        new SqlParameter("P_EXPECTED_CLOSE", Types.DATE),
                        new SqlParameter("P_CONVERTED_BY", Types.NUMERIC),
                        new SqlOutParameter("O_CUSTOMER_ID", Types.NUMERIC),
                        new SqlOutParameter("O_OPPORTUNITY_ID", Types.NUMERIC),
                        new SqlOutParameter("O_STATUS", Types.VARCHAR),
                        new SqlOutParameter("O_MESSAGE", Types.VARCHAR)
                );

        Map<String, Object> inParams = new HashMap<>();
        inParams.put("P_LEAD_ID", leadId);
        inParams.put("P_OPPORTUNITY_NAME", payload.get("opportunityName"));
        inParams.put("P_DEAL_VALUE", payload.get("dealValue"));
        inParams.put("P_EXPECTED_CLOSE", new java.sql.Date(System.currentTimeMillis() + 30L * 24 * 3600 * 1000));
        inParams.put("P_CONVERTED_BY", currentUserId != null ? currentUserId : 1L);

        Map<String, Object> out = jdbcCall.execute(inParams);
        String status = (String) out.get("O_STATUS");
        String message = (String) out.get("O_MESSAGE");

        if ("ERROR".equalsIgnoreCase(status)) {
            throw new RuntimeException(message);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("customerId", out.get("O_CUSTOMER_ID"));
        result.put("opportunityId", out.get("O_OPPORTUNITY_ID"));
        result.put("message", message);
        return result;
    }
}
