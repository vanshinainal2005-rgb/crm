package com.enterprise.crm.report;

import com.enterprise.crm.common.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/sales-by-rep")
    public ApiResponse<List<Map<String, Object>>> getSalesByRep() {
        return ApiResponse.success(reportService.getSalesByRep());
    }

    @GetMapping("/receivables-aging")
    public ApiResponse<List<Map<String, Object>>> getReceivablesAging() {
        return ApiResponse.success(reportService.getReceivablesAging());
    }

    @GetMapping("/inventory-valuation")
    public ApiResponse<List<Map<String, Object>>> getInventoryValuation() {
        return ApiResponse.success(reportService.getInventoryValuation());
    }

    @GetMapping("/export-csv")
    public ResponseEntity<String> exportCsv(@RequestParam(defaultValue = "SALES") String type) {
        String csvData = reportService.generateCsv(type);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + type.toLowerCase() + "_report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }
}
