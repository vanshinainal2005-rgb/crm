package com.enterprise.crm.inventory;

import com.enterprise.crm.common.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final JdbcTemplate jdbcTemplate;

    public InventoryController(InventoryService inventoryService, JdbcTemplate jdbcTemplate) {
        this.inventoryService = inventoryService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> getInventorySummary() {
        return ApiResponse.success(inventoryService.getInventorySummary());
    }

    @PostMapping("/receive")
    public ApiResponse<Map<String, Object>> receiveStock(
            @RequestBody Map<String, Object> payload,
            Authentication auth) {
        Long productId = ((Number) payload.get("productId")).longValue();
        Integer quantity = ((Number) payload.get("quantity")).intValue();
        String reason = (String) payload.get("reason");
        Long userId = getCurrentUserId(auth);

        return ApiResponse.success(inventoryService.receiveStock(productId, quantity, reason, userId), "Stock received");
    }

    @PostMapping("/adjust")
    public ApiResponse<Map<String, Object>> adjustInventory(
            @RequestBody Map<String, Object> payload,
            Authentication auth) {
        Long productId = ((Number) payload.get("productId")).longValue();
        Integer newStock = ((Number) payload.get("newStock")).intValue();
        String reason = (String) payload.get("reason");
        Long userId = getCurrentUserId(auth);

        return ApiResponse.success(inventoryService.adjustInventory(productId, newStock, reason, userId), "Inventory adjusted");
    }

    @GetMapping("/movements")
    public ApiResponse<List<Map<String, Object>>> getMovements(@RequestParam(required = false) Long productId) {
        return ApiResponse.success(inventoryService.getMovements(productId));
    }

    private Long getCurrentUserId(Authentication auth) {
        if (auth == null || auth.getName() == null) return 1L;
        try {
            return jdbcTemplate.queryForObject("SELECT user_id FROM USERS WHERE LOWER(username) = LOWER(?)", Long.class, auth.getName());
        } catch (Exception e) {
            return 1L;
        }
    }
}
