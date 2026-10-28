package com.enterprise.crm.order;

import com.enterprise.crm.common.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final JdbcTemplate jdbcTemplate;

    public OrderController(OrderService orderService, JdbcTemplate jdbcTemplate) {
        this.orderService = orderService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        return ApiResponse.success(orderService.getAllOrders(status, search));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getOrderById(@PathVariable Long id) {
        return ApiResponse.success(orderService.getOrderById(id));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> createOrder(
            @RequestBody Map<String, Object> payload,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        return ApiResponse.success(orderService.createOrderWithItems(payload, userId), "Order draft created");
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<Map<String, Object>> confirmOrder(
            @PathVariable Long id,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        return ApiResponse.success(orderService.confirmOrder(id, userId), "Order confirmed");
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Map<String, Object>> cancelOrder(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication auth) {
        String reason = body != null ? body.get("reason") : "Customer cancellation";
        Long userId = getCurrentUserId(auth);
        return ApiResponse.success(orderService.cancelOrder(id, reason, userId), "Order cancelled");
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
