package com.enterprise.crm.invoice;

import com.enterprise.crm.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> getInvoices(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        return ApiResponse.success(invoiceService.getAllInvoices(status, search));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getInvoiceById(@PathVariable Long id) {
        return ApiResponse.success(invoiceService.getInvoiceById(id));
    }
}
