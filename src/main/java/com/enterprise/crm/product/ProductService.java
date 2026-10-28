package com.enterprise.crm.product;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;

import java.sql.Types;
import java.util.*;

@Service
public class ProductService {

    private final JdbcTemplate jdbcTemplate;

    public ProductService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> getAllProducts(String category, String search) {
        StringBuilder sql = new StringBuilder(
                "SELECT p.*, " +
                "NVL(i.available_stock, 0) AS available_stock, " +
                "NVL(i.reserved_stock, 0) AS reserved_stock, " +
                "NVL(i.reorder_threshold, 10) AS reorder_threshold, " +
                "NVL(i.warehouse_location, 'MAIN-WAREHOUSE') AS warehouse_location " +
                "FROM PRODUCTS p " +
                "LEFT JOIN INVENTORY i ON p.product_id = i.product_id " +
                "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();
        if (category != null && !category.trim().isEmpty() && !"ALL".equalsIgnoreCase(category)) {
            sql.append("AND p.category = ? ");
            params.add(category.trim().toUpperCase());
        }

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(p.name) LIKE ? OR LOWER(p.sku) LIKE ?) ");
            String q = "%" + search.trim().toLowerCase() + "%";
            params.add(q);
            params.add(q);
        }

        sql.append("ORDER BY p.product_id ASC");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public Map<String, Object> createProduct(Map<String, Object> payload, Long currentUserId) {
        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                .withCatalogName("CRM_INVENTORY_PKG")
                .withProcedureName("CREATE_PRODUCT")
                .declareParameters(
                        new SqlParameter("P_SKU", Types.VARCHAR),
                        new SqlParameter("P_NAME", Types.VARCHAR),
                        new SqlParameter("P_DESCRIPTION", Types.VARCHAR),
                        new SqlParameter("P_CATEGORY", Types.VARCHAR),
                        new SqlParameter("P_COST_PRICE", Types.NUMERIC),
                        new SqlParameter("P_MIN_PRICE", Types.NUMERIC),
                        new SqlParameter("P_MAX_PRICE", Types.NUMERIC),
                        new SqlParameter("P_CURRENT_PRICE", Types.NUMERIC),
                        new SqlParameter("P_INITIAL_STOCK", Types.NUMERIC),
                        new SqlParameter("P_REORDER_THRESH", Types.NUMERIC),
                        new SqlParameter("P_CREATED_BY", Types.NUMERIC),
                        new SqlOutParameter("O_PRODUCT_ID", Types.NUMERIC),
                        new SqlOutParameter("O_STATUS", Types.VARCHAR),
                        new SqlOutParameter("O_MESSAGE", Types.VARCHAR)
                );

        Map<String, Object> inParams = new HashMap<>();
        inParams.put("P_SKU", payload.get("sku"));
        inParams.put("P_NAME", payload.get("name"));
        inParams.put("P_DESCRIPTION", payload.get("description"));
        inParams.put("P_CATEGORY", payload.get("category"));
        inParams.put("P_COST_PRICE", payload.get("costPrice"));
        inParams.put("P_MIN_PRICE", payload.get("minPrice"));
        inParams.put("P_MAX_PRICE", payload.get("maxPrice"));
        inParams.put("P_CURRENT_PRICE", payload.get("currentPrice"));
        inParams.put("P_INITIAL_STOCK", payload.getOrDefault("initialStock", 0));
        inParams.put("P_REORDER_THRESH", payload.getOrDefault("reorderThreshold", 10));
        inParams.put("P_CREATED_BY", currentUserId != null ? currentUserId : 1L);

        Map<String, Object> out = jdbcCall.execute(inParams);
        String status = (String) out.get("O_STATUS");
        String message = (String) out.get("O_MESSAGE");

        if ("ERROR".equalsIgnoreCase(status)) {
            throw new RuntimeException(message);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("productId", out.get("O_PRODUCT_ID"));
        result.put("message", message);
        return result;
    }
}
