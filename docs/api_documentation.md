# Enterprise CRM — RESTful API Documentation

This document describes all REST API endpoints available in the **Enterprise Customer Relationship Management (CRM) & Operations System**.

Base URL: `http://localhost:8080`  
Swagger / OpenAPI Interactive UI: `http://localhost:8080/swagger-ui/index.html`  
OpenAPI JSON Specification: `http://localhost:8080/v3/api-docs`

---

## 1. Authentication & Session APIs

### 1.1 Switch Role (Evaluation & Instant Demo Mode)
- **Endpoint**: `POST /api/auth/demo-switch`
- **Description**: Switches the active session security context to any of the 9 seeded enterprise roles without requiring password re-entry.
- **Request Body**:
  ```json
  {
    "username": "superadmin"
  }
  ```
- **Response**:
  ```json
  {
    "success": true,
    "message": "Switched role to: superadmin",
    "data": {
      "USER_ID": 1,
      "USERNAME": "superadmin",
      "FULL_NAME": "Alexander Wright",
      "EMAIL": "alexander.wright@enterprisecrm.io",
      "DEPARTMENT": "Executive",
      "roles": [
        { "ROLE_NAME": "ROLE_SUPER_ADMIN", "DISPLAY_NAME": "Super Administrator" }
      ]
    }
  }
  ```

### 1.2 Get Current Authenticated User
- **Endpoint**: `GET /api/auth/me`
- **Description**: Returns profile, role assignments, and unread notification counts for the authenticated user.

---

## 2. Executive Dashboard APIs

### 2.1 Get Live Executive Telemetry
- **Endpoint**: `GET /api/dashboard`
- **Description**: Calls PL/SQL package `CRM_REPORTING_PKG.GET_CRM_KPI_METRICS` to return real-time system metrics, deal distribution for Chart.js, department work queues summary, urgent tasks, and activity logs.
- **Response Format**:
  ```json
  {
    "success": true,
    "message": "Dashboard data loaded",
    "data": {
      "kpis": {
        "totalCustomers": 5,
        "newLeads": 3,
        "activeOpportunities": 4,
        "pipelineValue": 219000.0,
        "wonRevenue": 95000.0,
        "receivablesTotal": 45980.0,
        "pendingTasks": 3
      },
      "stageDistribution": [
        { "STAGE": "WON", "COUNT": 1, "TOTAL_VALUE": 95000 },
        { "STAGE": "NEGOTIATION", "COUNT": 1, "TOTAL_VALUE": 85000 }
      ],
      "queueStatus": [
        { "QUEUE_ID": 1, "QUEUE_NAME": "SALES", "PENDING_COUNT": 1 },
        { "QUEUE_ID": 4, "QUEUE_NAME": "SHIPPING", "PENDING_COUNT": 2 }
      ],
      "urgentTasks": [...]
    }
  }
  ```

---

## 3. Customer & Contact Management APIs

### 3.1 List Customers
- **Endpoint**: `GET /api/customers`
- **Query Parameters**:
  - `search` (optional): Filter by name, email, company, or customer code.
  - `category` (optional): `ENTERPRISE`, `VIP`, `STANDARD`, `STRATEGIC`.

### 3.2 Customer 360 Full Profile
- **Endpoint**: `GET /api/customers/{id}`
- **Description**: Returns complete 360-degree customer view, including contacts, pipeline opportunities, order history, invoices, and activity logs.

### 3.3 Create Customer with Duplicate Detection
- **Endpoint**: `POST /api/customers`
- **Description**: Normalizes phone numbers (E.164) and executes `CRM_CUSTOMER_PKG.DETECT_DUPLICATE_CUSTOMER` before inserting.
- **Request Body**:
  ```json
  {
    "name": "Acme Cloud Systems Corp",
    "email": "procurement@acmecloud.com",
    "phone": "+1-415-555-2671",
    "company": "Acme Corporation",
    "customerCategory": "ENTERPRISE",
    "billingAddress": "100 Market St, Suite 400, San Francisco, CA 94105",
    "shippingAddress": "500 Logistics Way, Oakland, CA 94607",
    "assignedEmployee": 4
  }
  ```

---

## 4. Lead Management APIs

### 4.1 List Leads
- **Endpoint**: `GET /api/leads`
- **Query Parameters**: `status`, `search`.

### 4.2 Convert Lead to Customer & Opportunity
- **Endpoint**: `POST /api/leads/{id}/convert`
- **Description**: Invokes PL/SQL package `CRM_SALES_PKG.CONVERT_LEAD` to atomically convert an inbound lead into an active Customer record and create an initial Qualified Opportunity.

---

## 5. Sales Pipeline & Kanban APIs

### 5.1 Get Kanban Pipeline
- **Endpoint**: `GET /api/opportunities/kanban`
- **Description**: Returns opportunities grouped by 7 pipeline stages (`NEW_LEAD`, `QUALIFIED`, `CONTACTED`, `PROPOSAL`, `NEGOTIATION`, `WON`, `LOST`), along with total pipeline value and probability-weighted revenue forecast.

### 5.2 Drag-and-Drop Stage Transition
- **Endpoint**: `PUT /api/opportunities/{id}/stage`
- **Description**: Updates deal stage and logs an immutable transition event in `OPPORTUNITY_HISTORY`.
- **Request Body**:
  ```json
  {
    "newStage": "WON",
    "lostReason": null,
    "notes": "Contract signed and countersigned by CFO"
  }
  ```

---

## 6. Product Catalog & Concurrency-Safe Inventory APIs

### 6.1 List Products
- **Endpoint**: `GET /api/products`
- **Query Parameters**: `search`, `category`.

### 6.2 Receive Stock (Stock-In)
- **Endpoint**: `POST /api/inventory/stock-in`
- **Description**: Calls `CRM_INVENTORY_PKG.RECEIVE_STOCK` and inserts an `INVENTORY_MOVEMENTS` audit record.

### 6.3 Audit Inventory Adjustment
- **Endpoint**: `POST /api/inventory/adjust`
- **Description**: Performs stock correction with a mandatory business justification reason.

---

## 7. Order & Invoicing APIs

### 7.1 List Orders
- **Endpoint**: `GET /api/orders`
- **Query Parameters**: `customerId`, `status`.

### 7.2 Create Purchase Order
- **Endpoint**: `POST /api/orders`
- **Description**: Verifies product pricing floors/ceilings, calculates subtotals and 18% GST/tax, locks inventory, and registers the order.
- **Request Body**:
  ```json
  {
    "customerId": 1,
    "notes": "Expedited enterprise rollout",
    "items": [
      {
        "productId": 1,
        "quantity": 1,
        "unitPrice": 22500.00,
        "discountPct": 0.00
      }
    ]
  }
  ```

### 7.3 Confirm Order & Auto-Generate Tax Invoice
- **Endpoint**: `POST /api/orders/{id}/confirm`
- **Description**: Transitions order to `CONFIRMED`, triggers `CRM_FINANCE_PKG.GENERATE_INVOICE`, and enqueues a fulfillment ticket in the Shipping Department Queue.

### 7.4 List Invoices
- **Endpoint**: `GET /api/invoices`
- **Query Parameters**: `customerId`, `status`.

---

## 8. Financial Settlement & Payment APIs

### 8.1 Record Payment with Idempotency Protection
- **Endpoint**: `POST /api/payments`
- **Description**: Deducts accounts receivable balance, sets invoice status, and records settlement with client UUID idempotency protection.
- **Request Body**:
  ```json
  {
    "invoiceId": 2,
    "amount": 5000.00,
    "paymentMethod": "WIRE_TRANSFER",
    "paymentReference": "WIRE-20261026-9041",
    "idempotencyKey": "d3b07384-d113-40a2-9b57-010023405781",
    "notes": "Milestone partial payment"
  }
  ```

---

## 9. Departmental Work Queue APIs

### 9.1 Queue Summary
- **Endpoint**: `GET /api/queues/summary`
- **Description**: Returns operational ticket counts across `SALES`, `MIS`, `FINANCE`, `SHIPPING`, and `ADMIN`.

### 9.2 List Queue Items
- **Endpoint**: `GET /api/queues/items`
- **Query Parameters**: `queue` (`SALES`, `MIS`, `FINANCE`, `SHIPPING`, `ADMIN`), `status` (`OPEN`, `IN_PROGRESS`, `RESOLVED`).

### 9.3 Update Ticket Status
- **Endpoint**: `PUT /api/queues/items/{id}/status?status=RESOLVED`

---

## 10. Reports & Analytics APIs

- **`GET /api/reports/sales-by-rep`**: Rep revenue, orders closed, and commission earned.
- **`GET /api/reports/receivables-aging`**: Accounts receivable breakdown into 0-30, 31-60, 61-90, and 90+ day aging buckets.
- **`GET /api/reports/inventory-valuation`**: Stock on hand, inventory valuation at cost vs selling price, and reorder alerts.
- **`GET /api/reports/sales/csv`**: Export sales report as downloadable CSV.
- **`GET /api/reports/receivables/csv`**: Export accounts receivable report as downloadable CSV.
