package com.enterprise.crm.lead;

import com.enterprise.crm.common.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leads")
public class LeadController {

    private final LeadService leadService;
    private final JdbcTemplate jdbcTemplate;

    public LeadController(LeadService leadService, JdbcTemplate jdbcTemplate) {
        this.leadService = leadService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> getLeads(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(leadService.getAllLeads(search, status));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> createLead(
            @RequestBody Map<String, Object> payload,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        return ApiResponse.success(leadService.createLead(payload, userId), "Lead created");
    }

    @PostMapping("/{id}/convert")
    public ApiResponse<Map<String, Object>> convertLead(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        return ApiResponse.success(leadService.convertLead(id, payload, userId), "Lead converted successfully");
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
