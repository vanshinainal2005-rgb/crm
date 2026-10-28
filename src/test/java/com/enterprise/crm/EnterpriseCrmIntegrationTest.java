package com.enterprise.crm;

import com.enterprise.crm.customer.CustomerService;
import com.enterprise.crm.inventory.InventoryService;
import com.enterprise.crm.order.OrderService;
import com.enterprise.crm.payment.PaymentService;
import com.enterprise.crm.sales.OpportunityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EnterpriseCrmIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private OpportunityService opportunityService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private com.enterprise.crm.dashboard.DashboardService dashboardService;

    @Test
    @Order(1)
    @DisplayName("Verify Oracle Database Connection & Version")
    void testDatabaseConnectivity() {
        String version = jdbcTemplate.queryForObject("SELECT BANNER_FULL FROM V$VERSION", String.class);
        assertNotNull(version);
        assertTrue(version.contains("Oracle"), "Must be Oracle Database: " + version);
        System.out.println(">>> Verified Oracle Connection: " + version);
    }

    @Test
    @Order(2)
    @DisplayName("Test Customer Duplicate Detection via CRM_CUSTOMER_PKG")
    void testCustomerDuplicateDetection() {
        // Customer 1 in seed data has email: procurement@acmecloud.com
        Map<String, Object> dupCheck = customerService.checkDuplicate("procurement@acmecloud.com", null, null);
        assertTrue((Boolean) dupCheck.get("isDuplicate"), "Should detect existing customer by email");
        assertNotNull(dupCheck.get("duplicateCustomerId"));

        // Unique email check
        Map<String, Object> nonDup = customerService.checkDuplicate("completely.unique." + UUID.randomUUID() + "@enterprise.com", null, null);
        assertFalse((Boolean) nonDup.get("isDuplicate"), "Should not detect duplicate for random email");
    }

    @Test
    @Order(3)
    @DisplayName("Test Opportunity Stage Transition & Audit Trail via CRM_SALES_PKG")
    void testOpportunityStageUpdate() {
        // Opportunity 1 (Acme Global Cloud Deployment)
        Map<String, Object> payload = Map.of(
                "newStage", "NEGOTIATION",
                "dealValue", 85000.0,
                "notes", "Automated test negotiation update"
        );
        Map<String, Object> res = opportunityService.updateStage(1L, payload, 1L);
        assertNotNull(res.get("message"));

        // Verify history table contains audit record
        List<Map<String, Object>> history = opportunityService.getOpportunityHistory(1L);
        assertFalse(history.isEmpty(), "Opportunity history must have audit records");
    }

    @Test
    @Order(4)
    @DisplayName("Test Inventory Stock Movement & Atomic Reservation via CRM_INVENTORY_PKG")
    void testInventoryReceiveAndStockLevel() {
        // Product 4: Rugged Industrial IoT Logistics Scanner
        Integer initialStock = jdbcTemplate.queryForObject(
                "SELECT available_stock FROM INVENTORY WHERE product_id = 4", Integer.class);
        assertNotNull(initialStock);

        Map<String, Object> receiveResult = inventoryService.receiveStock(4L, 10, "Test Inbound Receipt", 1L);
        assertNotNull(receiveResult.get("newStock"));

        Integer updatedStock = jdbcTemplate.queryForObject(
                "SELECT available_stock FROM INVENTORY WHERE product_id = 4", Integer.class);
        assertEquals(initialStock + 10, updatedStock.intValue());
    }

    @Test
    @Order(5)
    @DisplayName("Test Order Creation, Stock Reservation & Automated Invoice Generation")
    void testOrderToInvoiceFlow() {
        // Create draft order for Customer 1 with Product 1
        Map<String, Object> payload = Map.of(
                "customerId", 1L,
                "notes", "Test Automated Order",
                "items", List.of(Map.of("productId", 1L, "quantity", 1, "unitPrice", 22500.0))
        );

        Map<String, Object> orderRes = orderService.createOrderWithItems(payload, 1L);
        Long orderId = (Long) orderRes.get("orderId");
        assertNotNull(orderId);

        // Confirm order: calls CRM_ORDER_PKG.CONFIRM_ORDER -> reserves stock and creates invoice
        Map<String, Object> confirmRes = orderService.confirmOrder(orderId, 1L);
        assertNotNull(confirmRes.get("invoiceNumber"));
        assertNotNull(confirmRes.get("invoiceId"));

        String orderStatus = jdbcTemplate.queryForObject(
                "SELECT order_status FROM ORDERS WHERE order_id = ?", String.class, orderId);
        assertEquals("CONFIRMED", orderStatus);
    }

    @Test
    @Order(6)
    @DisplayName("Test Payment Recording Idempotency via CRM_FINANCE_PKG")
    void testPaymentIdempotency() {
        String testKey = "IDEMP-TEST-" + UUID.randomUUID().toString().substring(0, 8);

        Map<String, Object> pay1 = paymentService.recordPayment(Map.of(
                "invoiceId", 2L,
                "amount", 100.0,
                "paymentMethod", "BANK_TRANSFER",
                "paymentReference", "REF-" + testKey,
                "idempotencyKey", testKey,
                "notes", "First submission"
        ), 1L);
        assertNotNull(pay1.get("paymentId"));

        // Re-submit identical payment with same idempotency key
        Map<String, Object> pay2 = paymentService.recordPayment(Map.of(
                "invoiceId", 2L,
                "amount", 100.0,
                "paymentMethod", "BANK_TRANSFER",
                "paymentReference", "REF-" + testKey,
                "idempotencyKey", testKey,
                "notes", "Duplicate submission attempt"
        ), 1L);

        // Must return identical paymentId and duplicate prevention message
        assertEquals(pay1.get("paymentId"), pay2.get("paymentId"), "Idempotency key must prevent duplicate recording");
        assertTrue(((String) pay2.get("message")).contains("Duplicate submission detected"));
    }

    @Test
    @Order(7)
    @DisplayName("Test Dashboard Data Loading")
    void testDashboardData() {
        Map<String, Object> dashboard = dashboardService.getDashboardData();
        assertNotNull(dashboard, "Dashboard data must not be null");
        assertTrue(dashboard.containsKey("kpis"), "Dashboard must contain kpis");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> kpis = (Map<String, Object>) dashboard.get("kpis");
        assertNotNull(kpis.get("totalCustomers"), "totalCustomers must not be null");
        System.out.println(">>> Dashboard KPIs: " + kpis);

        assertTrue(dashboard.containsKey("stageDistribution"), "Must have stageDistribution");
        assertTrue(dashboard.containsKey("queueStatus"), "Must have queueStatus");
        assertTrue(dashboard.containsKey("urgentTasks"), "Must have urgentTasks");
        System.out.println(">>> Dashboard Loaded Successfully with 0 errors!");
    }
}
