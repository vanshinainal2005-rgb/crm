package com.enterprise.crm.sales;

import com.enterprise.crm.common.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/opportunities")
public class OpportunityController {

    private final OpportunityService opportunityService;
    private final JdbcTemplate jdbcTemplate;

    public OpportunityController(OpportunityService opportunityService, JdbcTemplate jdbcTemplate) {
        this.opportunityService = opportunityService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> getOpportunities(
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) String search) {
        return ApiResponse.success(opportunityService.getAllOpportunities(stage, search));
    }

    @GetMapping("/kanban")
    public ApiResponse<Map<String, Object>> getKanbanPipeline() {
        return ApiResponse.success(opportunityService.getKanbanPipeline());
    }

    @PutMapping("/{id}/stage")
    public ApiResponse<Map<String, Object>> updateStage(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            Authentication auth) {
        Long userId = getCurrentUserId(auth);
        return ApiResponse.success(opportunityService.updateStage(id, payload, userId), "Opportunity stage updated");
    }

    @GetMapping("/{id}/history")
    public ApiResponse<List<Map<String, Object>>> getOpportunityHistory(@PathVariable Long id) {
        return ApiResponse.success(opportunityService.getOpportunityHistory(id));
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
