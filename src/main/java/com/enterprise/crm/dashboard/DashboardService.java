package com.enterprise.crm.dashboard;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;

import java.sql.Types;
import java.util.*;

@Service
public class DashboardService {

    private final JdbcTemplate jdbcTemplate;

    public DashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> getDashboardData() {
        Map<String, Object> dashboard = new HashMap<>();

        // 1. Call PL/SQL Package CRM_REPORTING_PKG.GET_CRM_KPI_METRICS
        try {
            SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                    .withCatalogName("CRM_REPORTING_PKG")
                    .withProcedureName("GET_CRM_KPI_METRICS")
                    .declareParameters(
                            new SqlOutParameter("O_TOTAL_CUSTOMERS", Types.NUMERIC),
                            new SqlOutParameter("O_NEW_LEADS", Types.NUMERIC),
                            new SqlOutParameter("O_ACTIVE_OPPS", Types.NUMERIC),
                            new SqlOutParameter("O_PIPELINE_VALUE", Types.NUMERIC),
                            new SqlOutParameter("O_WON_REVENUE", Types.NUMERIC),
                            new SqlOutParameter("O_RECEIVABLES_TOTAL", Types.NUMERIC),
                            new SqlOutParameter("O_PENDING_TASKS", Types.NUMERIC)
                    );

            Map<String, Object> out = jdbcCall.execute();
            Map<String, Object> kpis = new HashMap<>();
            kpis.put("totalCustomers", out.get("O_TOTAL_CUSTOMERS"));
            kpis.put("newLeads", out.get("O_NEW_LEADS"));
            kpis.put("activeOpportunities", out.get("O_ACTIVE_OPPS"));
            kpis.put("pipelineValue", out.get("O_PIPELINE_VALUE"));
            kpis.put("wonRevenue", out.get("O_WON_REVENUE"));
            kpis.put("receivablesTotal", out.get("O_RECEIVABLES_TOTAL"));
            kpis.put("pendingTasks", out.get("O_PENDING_TASKS"));
            dashboard.put("kpis", kpis);
        } catch (Exception e) {
            // Fallback direct SQL if PL/SQL package fails
            Map<String, Object> kpis = new HashMap<>();
            kpis.put("totalCustomers", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM CUSTOMERS WHERE status = 'ACTIVE'", Integer.class));
            kpis.put("newLeads", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM LEADS WHERE lead_status <> 'CONVERTED'", Integer.class));
            kpis.put("activeOpportunities", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM OPPORTUNITIES WHERE stage NOT IN ('WON', 'LOST')", Integer.class));
            kpis.put("pipelineValue", jdbcTemplate.queryForObject("SELECT NVL(SUM(deal_value), 0) FROM OPPORTUNITIES WHERE stage NOT IN ('WON', 'LOST')", Double.class));
            kpis.put("wonRevenue", jdbcTemplate.queryForObject("SELECT NVL(SUM(deal_value), 0) FROM OPPORTUNITIES WHERE stage = 'WON'", Double.class));
            kpis.put("receivablesTotal", jdbcTemplate.queryForObject("SELECT NVL(SUM(balance_amount), 0) FROM INVOICES WHERE payment_status <> 'PAID'", Double.class));
            kpis.put("pendingTasks", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TASKS WHERE status IN ('PENDING', 'IN_PROGRESS')", Integer.class));
            dashboard.put("kpis", kpis);
        }

        // 2. Stage Breakdown for Chart.js
        String stageSql = "SELECT stage, COUNT(*) AS count, NVL(SUM(deal_value), 0) AS total_value " +
                          "FROM OPPORTUNITIES GROUP BY stage ORDER BY count DESC";
        dashboard.put("stageDistribution", jdbcTemplate.queryForList(stageSql));

        // 3. Department Queues Summary
        String queueSql = "SELECT dq.queue_id, dq.queue_name, dq.display_title, " +
                          "SUM(CASE WHEN qa.status IN ('OPEN', 'IN_PROGRESS') THEN 1 ELSE 0 END) AS pending_count " +
                          "FROM DEPARTMENT_QUEUES dq " +
                          "LEFT JOIN QUEUE_ASSIGNMENTS qa ON dq.queue_id = qa.queue_id " +
                          "GROUP BY dq.queue_id, dq.queue_name, dq.display_title ORDER BY dq.queue_id";
        dashboard.put("queueStatus", jdbcTemplate.queryForList(queueSql));

        // 4. Recent Customer Activity Timeline
        String actSql = "SELECT a.activity_id, a.entity_type, a.entity_id, a.activity_type, a.summary, " +
                        "a.created_at, NVL(u.full_name, 'System') AS performed_by_name " +
                        "FROM ACTIVITY_LOGS a " +
                        "LEFT JOIN USERS u ON a.performed_by = u.user_id " +
                        "ORDER BY a.created_at DESC FETCH FIRST 6 ROWS ONLY";
        dashboard.put("recentActivities", jdbcTemplate.queryForList(actSql));

        // 5. Urgent Tasks Due Today or Overdue
        String taskSql = "SELECT t.task_id, t.title, t.priority, t.status, t.due_date, " +
                         "u.full_name AS assigned_to_name " +
                         "FROM TASKS t " +
                         "JOIN USERS u ON t.assigned_to = u.user_id " +
                         "WHERE t.status IN ('PENDING', 'IN_PROGRESS') " +
                         "ORDER BY CASE t.priority WHEN 'URGENT' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 ELSE 4 END, t.due_date ASC " +
                         "FETCH FIRST 5 ROWS ONLY";
        dashboard.put("urgentTasks", jdbcTemplate.queryForList(taskSql));

        return dashboard;
    }
}
