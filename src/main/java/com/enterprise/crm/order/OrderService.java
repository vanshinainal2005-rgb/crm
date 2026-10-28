package com.enterprise.crm.order;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Types;
import java.util.*;

@Service
public class OrderService {

    private final JdbcTemplate jdbcTemplate;

    public OrderService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> getAllOrders(String status, String search) {
        StringBuilder sql = new StringBuilder(
                "SELECT o.*, c.name AS customer_name, c.customer_code, u.full_name AS sales_rep_name, " +
                "(SELECT COUNT(*) FROM ORDER_ITEMS WHERE order_id = o.order_id) AS items_count, " +
                "i.invoice_number, i.payment_status " +
                "FROM ORDERS o " +
                "JOIN CUSTOMERS c ON o.customer_id = c.customer_id " +
                "LEFT JOIN USERS u ON o.assigned_sales_rep = u.user_id " +
                "LEFT JOIN INVOICES i ON o.order_id = i.order_id " +
                "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            sql.append("AND o.order_status = ? ");
            params.add(status.trim().toUpperCase());
        }

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(o.order_number) LIKE ? OR LOWER(c.name) LIKE ?) ");
            String q = "%" + search.trim().toLowerCase() + "%";
            params.add(q);
            params.add(q);
        }

        sql.append("ORDER BY o.order_id DESC");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public Map<String, Object> getOrderById(Long orderId) {
        String sql = "SELECT o.*, c.name AS customer_name, c.email AS customer_email, c.billing_address, " +
                     "u.full_name AS sales_rep_name, i.invoice_number, i.invoice_id, i.payment_status " +
                     "FROM ORDERS o " +
                     "JOIN CUSTOMERS c ON o.customer_id = c.customer_id " +
                     "LEFT JOIN USERS u ON o.assigned_sales_rep = u.user_id " +
                     "LEFT JOIN INVOICES i ON o.order_id = i.order_id " +
                     "WHERE o.order_id = ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, orderId);
        if (rows.isEmpty()) {
            throw new RuntimeException("Order not found: " + orderId);
        }

        Map<String, Object> order = new HashMap<>(rows.get(0));

        String itemsSql = "SELECT oi.*, p.name AS product_name, p.sku " +
                          "FROM ORDER_ITEMS oi " +
                          "JOIN PRODUCTS p ON oi.product_id = p.product_id " +
                          "WHERE oi.order_id = ?";
        order.put("items", jdbcTemplate.queryForList(itemsSql, orderId));
        return order;
    }

    @Transactional
    public Map<String, Object> createOrderWithItems(Map<String, Object> payload, Long currentUserId) {
        Long customerId = ((Number) payload.get("customerId")).longValue();
        Long salesRepId = payload.get("salesRepId") != null ? ((Number) payload.get("salesRepId")).longValue() : currentUserId;
        Number discountVal = (Number) payload.getOrDefault("discountAmount", 0);
        String notes = (String) payload.get("notes");
        List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");

        if (items == null || items.isEmpty()) {
            throw new RuntimeException("Order must contain at least one line item.");
        }

        // 1. Create Order Header via PL/SQL CRM_ORDER_PKG.CREATE_ORDER
        SimpleJdbcCall createCall = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName("CRM_ORDER_PKG")
                .withProcedureName("CREATE_ORDER")
                .declareParameters(
                        new SqlParameter("P_CUSTOMER_ID", Types.NUMERIC),
                        new SqlParameter("P_SALES_REP_ID", Types.NUMERIC),
                        new SqlParameter("P_DISCOUNT_AMOUNT", Types.NUMERIC),
                        new SqlParameter("P_TAX_PCT", Types.NUMERIC),
                        new SqlParameter("P_NOTES", Types.VARCHAR),
                        new SqlOutParameter("O_ORDER_ID", Types.NUMERIC),
                        new SqlOutParameter("O_ORDER_NUMBER", Types.VARCHAR),
                        new SqlOutParameter("O_STATUS", Types.VARCHAR),
                        new SqlOutParameter("O_MESSAGE", Types.VARCHAR)
                );

        Map<String, Object> inParams = new HashMap<>();
        inParams.put("P_CUSTOMER_ID", customerId);
        inParams.put("P_SALES_REP_ID", salesRepId);
        inParams.put("P_DISCOUNT_AMOUNT", discountVal != null ? discountVal.doubleValue() : 0.0);
        inParams.put("P_TAX_PCT", 18.0);
        inParams.put("P_NOTES", notes);

        Map<String, Object> out = createCall.execute(inParams);
        String status = (String) out.get("O_STATUS");
        if ("ERROR".equalsIgnoreCase(status)) {
            throw new RuntimeException((String) out.get("O_MESSAGE"));
        }

        Long orderId = ((Number) out.get("O_ORDER_ID")).longValue();
        String orderNumber = (String) out.get("O_ORDER_NUMBER");

        // 2. Add Line Items via CRM_ORDER_PKG.ADD_ORDER_ITEM
        SimpleJdbcCall addItemCall = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName("CRM_ORDER_PKG")
                .withProcedureName("ADD_ORDER_ITEM")
                .declareParameters(
                        new SqlParameter("P_ORDER_ID", Types.NUMERIC),
                        new SqlParameter("P_PRODUCT_ID", Types.NUMERIC),
                        new SqlParameter("P_QUANTITY", Types.NUMERIC),
                        new SqlParameter("P_UNIT_PRICE", Types.NUMERIC),
                        new SqlOutParameter("O_ITEM_ID", Types.NUMERIC),
                        new SqlOutParameter("O_STATUS", Types.VARCHAR),
                        new SqlOutParameter("O_MESSAGE", Types.VARCHAR)
                );

        for (Map<String, Object> item : items) {
            Long prodId = ((Number) item.get("productId")).longValue();
            Integer qty = ((Number) item.get("quantity")).intValue();
            Double price = item.get("unitPrice") != null ? ((Number) item.get("unitPrice")).doubleValue() : null;

            Map<String, Object> itemParams = new HashMap<>();
            itemParams.put("P_ORDER_ID", orderId);
            itemParams.put("P_PRODUCT_ID", prodId);
            itemParams.put("P_QUANTITY", qty);
            itemParams.put("P_UNIT_PRICE", price);

            Map<String, Object> itemOut = addItemCall.execute(itemParams);
            if ("ERROR".equalsIgnoreCase((String) itemOut.get("O_STATUS"))) {
                throw new RuntimeException((String) itemOut.get("O_MESSAGE"));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("orderNumber", orderNumber);
        result.put("message", "Draft Order #" + orderNumber + " created successfully.");
        return result;
    }

    public Map<String, Object> confirmOrder(Long orderId, Long currentUserId) {
        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName("CRM_ORDER_PKG")
                .withProcedureName("CONFIRM_ORDER")
                .declareParameters(
                        new SqlParameter("P_ORDER_ID", Types.NUMERIC),
                        new SqlParameter("P_CONFIRMED_BY", Types.NUMERIC),
                        new SqlOutParameter("O_INVOICE_ID", Types.NUMERIC),
                        new SqlOutParameter("O_INVOICE_NUMBER", Types.VARCHAR),
                        new SqlOutParameter("O_STATUS", Types.VARCHAR),
                        new SqlOutParameter("O_MESSAGE", Types.VARCHAR)
                );

        Map<String, Object> inParams = new HashMap<>();
        inParams.put("P_ORDER_ID", orderId);
        inParams.put("P_CONFIRMED_BY", currentUserId != null ? currentUserId : 1L);

        Map<String, Object> out = jdbcCall.execute(inParams);
        String status = (String) out.get("O_STATUS");
        String message = (String) out.get("O_MESSAGE");

        if ("ERROR".equalsIgnoreCase(status)) {
            throw new RuntimeException(message);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("invoiceId", out.get("O_INVOICE_ID"));
        result.put("invoiceNumber", out.get("O_INVOICE_NUMBER"));
        result.put("message", message);
        return result;
    }

    public Map<String, Object> cancelOrder(Long orderId, String reason, Long currentUserId) {
        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName("CRM_ORDER_PKG")
                .withProcedureName("CANCEL_ORDER")
                .declareParameters(
                        new SqlParameter("P_ORDER_ID", Types.NUMERIC),
                        new SqlParameter("P_REASON", Types.VARCHAR),
                        new SqlParameter("P_CANCELLED_BY", Types.NUMERIC),
                        new SqlOutParameter("O_STATUS", Types.VARCHAR),
                        new SqlOutParameter("O_MESSAGE", Types.VARCHAR)
                );

        Map<String, Object> inParams = new HashMap<>();
        inParams.put("P_ORDER_ID", orderId);
        inParams.put("P_REASON", reason != null ? reason : "Customer Request");
        inParams.put("P_CANCELLED_BY", currentUserId != null ? currentUserId : 1L);

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
}
