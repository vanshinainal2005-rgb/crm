package com.enterprise.crm.report;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ReportService {

    private final JdbcTemplate jdbcTemplate;

    public ReportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> getSalesByRep() {
        String sql = "SELECT u.user_id, u.full_name AS sales_rep, u.email, " +
                     "COUNT(o.order_id) AS total_orders, " +
                     "NVL(SUM(o.total_amount), 0) AS total_revenue, " +
                     "NVL(SUM(CASE WHEN o.order_status = 'CONFIRMED' THEN o.total_amount ELSE 0 END), 0) AS confirmed_revenue, " +
                     "NVL(SUM(cr.calculated_commission), 0) AS total_commission_earned " +
                     "FROM USERS u " +
                     "JOIN USER_ROLES ur ON u.user_id = ur.user_id " +
                     "JOIN ROLES r ON ur.role_id = r.role_id " +
                     "LEFT JOIN ORDERS o ON u.user_id = o.assigned_sales_rep " +
                     "LEFT JOIN COMMISSION_RECORDS cr ON u.user_id = cr.employee_id " +
                     "WHERE r.role_name IN ('ROLE_SALES_AGENT', 'ROLE_SALES_MANAGER') " +
                     "GROUP BY u.user_id, u.full_name, u.email " +
                     "ORDER BY total_revenue DESC";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getReceivablesAging() {
        String sql = "SELECT " +
                     "SUM(CASE WHEN (TRUNC(SYSDATE) - due_date) <= 30 THEN balance_amount ELSE 0 END) AS aging_0_30, " +
                     "SUM(CASE WHEN (TRUNC(SYSDATE) - due_date) BETWEEN 31 AND 60 THEN balance_amount ELSE 0 END) AS aging_31_60, " +
                     "SUM(CASE WHEN (TRUNC(SYSDATE) - due_date) BETWEEN 61 AND 90 THEN balance_amount ELSE 0 END) AS aging_61_90, " +
                     "SUM(CASE WHEN (TRUNC(SYSDATE) - due_date) > 90 THEN balance_amount ELSE 0 END) AS aging_90_plus, " +
                     "SUM(balance_amount) AS total_receivables " +
                     "FROM INVOICES WHERE payment_status <> 'PAID'";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getInventoryValuation() {
        String sql = "SELECT p.category, " +
                     "COUNT(p.product_id) AS product_count, " +
                     "NVL(SUM(i.available_stock), 0) AS total_units_in_stock, " +
                     "NVL(SUM(i.available_stock * p.cost_price), 0) AS total_cost_valuation, " +
                     "NVL(SUM(i.available_stock * p.current_selling_price), 0) AS potential_retail_valuation " +
                     "FROM PRODUCTS p " +
                     "LEFT JOIN INVENTORY i ON p.product_id = i.product_id " +
                     "GROUP BY p.category ORDER BY potential_retail_valuation DESC";
        return jdbcTemplate.queryForList(sql);
    }

    public String generateCsv(String reportType) {
        StringBuilder csv = new StringBuilder();

        if ("SALES".equalsIgnoreCase(reportType)) {
            csv.append("Sales Rep,Email,Total Orders,Total Revenue,Confirmed Revenue,Commission Earned\n");
            for (Map<String, Object> row : getSalesByRep()) {
                csv.append(escapeCsv(row.get("SALES_REP"))).append(",")
                   .append(escapeCsv(row.get("EMAIL"))).append(",")
                   .append(row.get("TOTAL_ORDERS")).append(",")
                   .append(row.get("TOTAL_REVENUE")).append(",")
                   .append(row.get("CONFIRMED_REVENUE")).append(",")
                   .append(row.get("TOTAL_COMMISSION_EARNED")).append("\n");
            }
        } else if ("INVENTORY".equalsIgnoreCase(reportType)) {
            csv.append("Category,Product Count,Available Units,Total Cost Valuation,Potential Retail Valuation\n");
            for (Map<String, Object> row : getInventoryValuation()) {
                csv.append(escapeCsv(row.get("CATEGORY"))).append(",")
                   .append(row.get("PRODUCT_COUNT")).append(",")
                   .append(row.get("TOTAL_UNITS_IN_STOCK")).append(",")
                   .append(row.get("TOTAL_COST_VALUATION")).append(",")
                   .append(row.get("POTENTIAL_RETAIL_VALUATION")).append("\n");
            }
        } else {
            csv.append("Customer,Invoice Number,Issue Date,Due Date,Total Amount,Paid Amount,Balance,Status\n");
            String sql = "SELECT c.name AS customer_name, i.invoice_number, i.issue_date, i.due_date, " +
                         "i.total_amount, i.paid_amount, i.balance_amount, i.payment_status " +
                         "FROM INVOICES i JOIN CUSTOMERS c ON i.customer_id = c.customer_id ORDER BY i.due_date ASC";
            for (Map<String, Object> row : jdbcTemplate.queryForList(sql)) {
                csv.append(escapeCsv(row.get("CUSTOMER_NAME"))).append(",")
                   .append(escapeCsv(row.get("INVOICE_NUMBER"))).append(",")
                   .append(row.get("ISSUE_DATE")).append(",")
                   .append(row.get("DUE_DATE")).append(",")
                   .append(row.get("TOTAL_AMOUNT")).append(",")
                   .append(row.get("PAID_AMOUNT")).append(",")
                   .append(row.get("BALANCE_AMOUNT")).append(",")
                   .append(row.get("PAYMENT_STATUS")).append("\n");
            }
        }

        return csv.toString();
    }

    private String escapeCsv(Object val) {
        if (val == null) return "";
        String s = val.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
