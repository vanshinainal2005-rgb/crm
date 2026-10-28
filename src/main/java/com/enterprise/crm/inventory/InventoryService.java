package com.enterprise.crm.inventory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;

import java.sql.Types;
import java.util.*;

@Service
public class InventoryService {

    private final JdbcTemplate jdbcTemplate;

    public InventoryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> getInventorySummary() {
        String sql = "SELECT p.product_id, p.sku, p.name, p.category, p.current_selling_price, " +
                     "i.inventory_id, NVL(i.available_stock, 0) AS available_stock, " +
                     "NVL(i.reserved_stock, 0) AS reserved_stock, " +
                     "NVL(i.reorder_threshold, 10) AS reorder_threshold, " +
                     "NVL(i.warehouse_location, 'MAIN-WAREHOUSE') AS warehouse_location, " +
                     "CASE WHEN NVL(i.available_stock, 0) <= NVL(i.reorder_threshold, 10) THEN 'LOW_STOCK' ELSE 'HEALTHY' END AS stock_status " +
                     "FROM PRODUCTS p " +
                     "LEFT JOIN INVENTORY i ON p.product_id = i.product_id " +
                     "ORDER BY CASE WHEN NVL(i.available_stock, 0) <= NVL(i.reorder_threshold, 10) THEN 1 ELSE 2 END, p.name ASC";
        return jdbcTemplate.queryForList(sql);
    }

    public Map<String, Object> receiveStock(Long productId, Integer quantity, String reason, Long currentUserId) {
        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName("CRM_INVENTORY_PKG")
                .withProcedureName("RECEIVE_STOCK")
                .declareParameters(
                        new SqlParameter("P_PRODUCT_ID", Types.NUMERIC),
                        new SqlParameter("P_QUANTITY", Types.NUMERIC),
                        new SqlParameter("P_REASON", Types.VARCHAR),
                        new SqlParameter("P_PERFORMED_BY", Types.NUMERIC),
                        new SqlOutParameter("O_NEW_STOCK", Types.NUMERIC),
                        new SqlOutParameter("O_STATUS", Types.VARCHAR),
                        new SqlOutParameter("O_MESSAGE", Types.VARCHAR)
                );

        Map<String, Object> inParams = new HashMap<>();
        inParams.put("P_PRODUCT_ID", productId);
        inParams.put("P_QUANTITY", quantity);
        inParams.put("P_REASON", reason != null ? reason : "Warehouse Stock Replenishment");
        inParams.put("P_PERFORMED_BY", currentUserId != null ? currentUserId : 1L);

        Map<String, Object> out = jdbcCall.execute(inParams);
        String status = (String) out.get("O_STATUS");
        String message = (String) out.get("O_MESSAGE");

        if ("ERROR".equalsIgnoreCase(status)) {
            throw new RuntimeException(message);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("newStock", out.get("O_NEW_STOCK"));
        result.put("message", message);
        return result;
    }

    public Map<String, Object> adjustInventory(Long productId, Integer newStock, String reason, Long currentUserId) {
        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName("CRM_INVENTORY_PKG")
                .withProcedureName("ADJUST_INVENTORY")
                .declareParameters(
                        new SqlParameter("P_PRODUCT_ID", Types.NUMERIC),
                        new SqlParameter("P_NEW_STOCK", Types.NUMERIC),
                        new SqlParameter("P_REASON", Types.VARCHAR),
                        new SqlParameter("P_PERFORMED_BY", Types.NUMERIC),
                        new SqlOutParameter("O_STATUS", Types.VARCHAR),
                        new SqlOutParameter("O_MESSAGE", Types.VARCHAR)
                );

        Map<String, Object> inParams = new HashMap<>();
        inParams.put("P_PRODUCT_ID", productId);
        inParams.put("P_NEW_STOCK", newStock);
        inParams.put("P_REASON", reason);
        inParams.put("P_PERFORMED_BY", currentUserId != null ? currentUserId : 1L);

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

    public List<Map<String, Object>> getMovements(Long productId) {
        StringBuilder sql = new StringBuilder(
                "SELECT m.*, p.name AS product_name, p.sku, u.full_name AS performed_by_name " +
                "FROM INVENTORY_MOVEMENTS m " +
                "JOIN PRODUCTS p ON m.product_id = p.product_id " +
                "LEFT JOIN USERS u ON m.performed_by = u.user_id "
        );

        if (productId != null) {
            sql.append("WHERE m.product_id = ? ");
            sql.append("ORDER BY m.created_at DESC FETCH FIRST 50 ROWS ONLY");
            return jdbcTemplate.queryForList(sql.toString(), productId);
        } else {
            sql.append("ORDER BY m.created_at DESC FETCH FIRST 50 ROWS ONLY");
            return jdbcTemplate.queryForList(sql.toString());
        }
    }
}
