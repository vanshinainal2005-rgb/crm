package com.enterprise.crm.invoice;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class InvoiceService {

    private final JdbcTemplate jdbcTemplate;

    public InvoiceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> getAllInvoices(String status, String search) {
        StringBuilder sql = new StringBuilder(
                "SELECT i.*, c.name AS customer_name, c.customer_code, o.order_number " +
                "FROM INVOICES i " +
                "JOIN CUSTOMERS c ON i.customer_id = c.customer_id " +
                "JOIN ORDERS o ON i.order_id = o.order_id " +
                "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            sql.append("AND i.payment_status = ? ");
            params.add(status.trim().toUpperCase());
        }

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(i.invoice_number) LIKE ? OR LOWER(c.name) LIKE ? OR LOWER(o.order_number) LIKE ?) ");
            String q = "%" + search.trim().toLowerCase() + "%";
            params.add(q);
            params.add(q);
            params.add(q);
        }

        sql.append("ORDER BY i.invoice_id DESC");
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    public Map<String, Object> getInvoiceById(Long invoiceId) {
        String sql = "SELECT i.*, c.name AS customer_name, c.email AS customer_email, c.phone AS customer_phone, " +
                     "c.billing_address, c.shipping_address, c.customer_code, o.order_number, o.order_date " +
                     "FROM INVOICES i " +
                     "JOIN CUSTOMERS c ON i.customer_id = c.customer_id " +
                     "JOIN ORDERS o ON i.order_id = o.order_id " +
                     "WHERE i.invoice_id = ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, invoiceId);
        if (rows.isEmpty()) {
            throw new RuntimeException("Invoice not found: " + invoiceId);
        }

        Map<String, Object> invoice = new HashMap<>(rows.get(0));

        // Invoice Line Items
        String itemsSql = "SELECT ii.*, p.sku FROM INVOICE_ITEMS ii " +
                          "JOIN PRODUCTS p ON ii.product_id = p.product_id " +
                          "WHERE ii.invoice_id = ?";
        invoice.put("items", jdbcTemplate.queryForList(itemsSql, invoiceId));

        // Payments History for this Invoice
        String paySql = "SELECT * FROM PAYMENTS WHERE invoice_id = ? ORDER BY transaction_date DESC";
        invoice.put("payments", jdbcTemplate.queryForList(paySql, invoiceId));

        return invoice;
    }
}
