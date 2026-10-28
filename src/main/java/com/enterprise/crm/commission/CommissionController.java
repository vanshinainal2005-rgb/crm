package com.enterprise.crm.commission;

import com.enterprise.crm.common.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/commissions")
public class CommissionController {

    private final CommissionService commissionService;
    private final JdbcTemplate jdbcTemplate;

    public CommissionController(CommissionService commissionService, JdbcTemplate jdbcTemplate) {
        this.commissionService = commissionService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/rules")
    public ApiResponse<List<Map<String, Object>>> getRules() {
        return ApiResponse.success(commissionService.getCommissionRules());
    }

    @GetMapping("/records")
    public ApiResponse<List<Map<String, Object>>> getRecords(@RequestParam(required = false) String status) {
        return ApiResponse.success(commissionService.getCommissionRecords(status));
    }

    @PutMapping("/records/{id}/status")
    public ApiResponse<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        return ApiResponse.success(commissionService.updateCommissionStatus(id, status, userId), "Status updated");
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
