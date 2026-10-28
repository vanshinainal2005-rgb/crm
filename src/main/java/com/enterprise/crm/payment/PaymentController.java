package com.enterprise.crm.payment;

import com.enterprise.crm.common.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final JdbcTemplate jdbcTemplate;

    public PaymentController(PaymentService paymentService, JdbcTemplate jdbcTemplate) {
        this.paymentService = paymentService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> getPayments(@RequestParam(required = false) String search) {
        return ApiResponse.success(paymentService.getAllPayments(search));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> recordPayment(
            @RequestBody Map<String, Object> payload,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        return ApiResponse.success(paymentService.recordPayment(payload, userId), "Payment recorded");
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
