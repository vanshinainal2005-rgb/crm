package com.enterprise.crm.customer;

import com.enterprise.crm.common.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final JdbcTemplate jdbcTemplate;

    public CustomerController(CustomerService customerService, JdbcTemplate jdbcTemplate) {
        this.customerService = customerService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> getCustomers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(customerService.getAllCustomers(search, category, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getCustomerById(@PathVariable Long id) {
        return ApiResponse.success(customerService.getCustomerById(id));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> createCustomer(
            @RequestBody Map<String, Object> payload,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        return ApiResponse.success(customerService.createCustomer(payload, userId), "Customer created");
    }

    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> updateCustomer(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        return ApiResponse.success(customerService.updateCustomer(id, payload, userId), "Customer updated");
    }

    @GetMapping("/check-duplicate")
    public ApiResponse<Map<String, Object>> checkDuplicate(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Long excludeId) {
        return ApiResponse.success(customerService.checkDuplicate(email, phone, excludeId));
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
