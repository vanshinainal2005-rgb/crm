package com.enterprise.crm.sales;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;

import java.sql.Types;
import java.util.*;

@Service
public class OpportunityService {

    private final JdbcTemplate jdbcTemplate;

    public OpportunityService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> getAllOpportunities(String stage, String search) {
        StringBuilder sql = new StringBuilder(
                "SELECT o.*, c.name AS customer_name, c.customer_code, u.full_name AS assigned_to_name " +
                "FROM OPPORTUNITIES o " +
                "JOIN CUSTOMERS c ON o.customer_id = c.customer_id " +
                "LEFT JOIN USERS u ON o.assigned_to = u.user_id " +
                "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();
        if (stage != null && !stage.trim().isEmpty() && !"ALL".equalsIgnoreCase(stage)) {
            sql.append("AND o.stage = ? ");
            params.add(stage.trim().toUpperCase());
        }

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(o.name) LIKE ? OR LOWER(c.name) LIKE ?) ");
            String q = "%" + search.trim().toLowerCase() + "%";
            params.add(q);
            params.add(q);
        }

        sql.append("ORDER BY o.deal_value DESC, o.opportunity_id DESC");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public Map<String, Object> getKanbanPipeline() {
        List<String> stages = Arrays.asList("NEW_LEAD", "QUALIFIED", "CONTACTED", "PROPOSAL", "NEGOTIATION", "WON", "LOST");
        Map<String, Object> kanban = new LinkedHashMap<>();

        double grandTotal = 0.0;
        double weightedTotal = 0.0;

        for (String stg : stages) {
            String sql = "SELECT o.*, c.name AS customer_name, c.company, u.full_name AS assigned_to_name " +
                         "FROM OPPORTUNITIES o " +
                         "JOIN CUSTOMERS c ON o.customer_id = c.customer_id " +
                         "LEFT JOIN USERS u ON o.assigned_to = u.user_id " +
                         "WHERE o.stage = ? ORDER BY o.deal_value DESC";
            List<Map<String, Object>> cards = jdbcTemplate.queryForList(sql, stg);

            double stageTotal = 0.0;
            for (Map<String, Object> card : cards) {
                double val = ((Number) card.get("DEAL_VALUE")).doubleValue();
                double prob = ((Number) card.get("PROBABILITY")).doubleValue();
                stageTotal += val;
                grandTotal += val;
                weightedTotal += (val * (prob / 100.0));
            }

            Map<String, Object> stageData = new HashMap<>();
            stageData.put("cards", cards);
            stageData.put("count", cards.size());
            stageData.put("totalValue", stageTotal);
            kanban.put(stg, stageData);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("columns", kanban);
        response.put("grandTotal", grandTotal);
        response.put("weightedForecastTotal", Math.round(weightedTotal * 100.0) / 100.0);
        return response;
    }

    public Map<String, Object> updateStage(Long opportunityId, Map<String, Object> payload, Long currentUserId) {
        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName("CRM_SALES_PKG")
                .withProcedureName("UPDATE_OPPORTUNITY_STAGE")
                .declareParameters(
                        new SqlParameter("P_OPPORTUNITY_ID", Types.NUMERIC),
                        new SqlParameter("P_NEW_STAGE", Types.VARCHAR),
                        new SqlParameter("P_DEAL_VALUE", Types.NUMERIC),
                        new SqlParameter("P_PROBABILITY", Types.NUMERIC),
                        new SqlParameter("P_LOST_REASON", Types.VARCHAR),
                        new SqlParameter("P_NOTES", Types.VARCHAR),
                        new SqlParameter("P_CHANGED_BY", Types.NUMERIC),
                        new SqlOutParameter("O_STATUS", Types.VARCHAR),
                        new SqlOutParameter("O_MESSAGE", Types.VARCHAR)
                );

        Map<String, Object> inParams = new HashMap<>();
        inParams.put("P_OPPORTUNITY_ID", opportunityId);
        inParams.put("P_NEW_STAGE", payload.get("newStage"));
        inParams.put("P_DEAL_VALUE", payload.get("dealValue"));
        inParams.put("P_PROBABILITY", payload.get("probability"));
        inParams.put("P_LOST_REASON", payload.get("lostReason"));
        inParams.put("P_NOTES", payload.get("notes"));
        inParams.put("P_CHANGED_BY", currentUserId != null ? currentUserId : 1L);

        Map<String, Object> out = jdbcCall.execute(inParams);
        String status = (String) out.get("O_STATUS");
        String message = (String) out.get("O_MESSAGE");

        if ("ERROR".equalsIgnoreCase(status)) {
            throw new RuntimeException(message);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("message", message);
        return result;
    }

    public List<Map<String, Object>> getOpportunityHistory(Long opportunityId) {
        String sql = "SELECT h.*, u.full_name AS changed_by_name " +
                     "FROM OPPORTUNITY_HISTORY h " +
                     "LEFT JOIN USERS u ON h.changed_by = u.user_id " +
                     "WHERE h.opportunity_id = ? ORDER BY h.changed_at DESC";
        return jdbcTemplate.queryForList(sql, opportunityId);
    }
}
