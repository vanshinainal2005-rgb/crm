# Enterprise CRM — Customer Relationship & Business Operations Management System

A production-grade, full-stack **Enterprise Customer Relationship Management (CRM) and Business Operations System** built with **Java 21**, **Spring Boot 3**, **Spring Security**, and **Oracle Database 26ai Cloud**, featuring business logic encapsulated in **Oracle PL/SQL packages** and a responsive enterprise web interface inspired by Salesforce, HubSpot, and Linear.

---

## 1. System Overview & Key Features

- **Live Database-Driven Architecture**: Fully functional backend with zero mocked data. All metrics, pipeline stages, customer profiles, inventory stocks, tax invoices, and department queues execute against live Oracle 26ai tables.
- **Enterprise Role-Based Access Control (RBAC)**: Supports 9 distinct enterprise roles with an instant role-switching drawer for evaluation.
- **Oracle PL/SQL Package Orchestration**:
  - `CRM_CUSTOMER_PKG`: Phone number normalization and fuzzy duplicate detection.
  - `CRM_SALES_PKG`: Lead lifecycle and atomic lead-to-opportunity conversion.
  - `CRM_INVENTORY_PKG`: Pessimistic row-level locking (`SELECT FOR UPDATE`) to prevent negative inventory balances.
  - `CRM_ORDER_PKG`: Purchase order validation, stock reservation, and total tax computation.
  - `CRM_FINANCE_PKG`: Automated Net-30 invoice generation, idempotent payment settlement, and tiered commission calculations.
  - `CRM_REPORTING_PKG`: Executive KPI aggregation.
- **Interactive UI/UX**:
  - Executive telemetry dashboard with live Chart.js visualizations.
  - 7-stage visual Kanban pipeline with drag-and-drop stage progression and probability-weighted forecasting.
  - Customer 360 profile drawer with contacts, deals, orders, and activity timelines.
  - Order-to-Invoice workflow with printable tax invoice preview.
  - Concurrency-safe payment recording with client idempotency key protection.
  - Departmental work queues for Sales, Shipping, Finance, MIS, and System Administration.
  - Business intelligence reports with CSV export capabilities.

---

## 2. Technology Stack

| Layer | Technologies |
| :--- | :--- |
| **Frontend** | HTML5, CSS3 (Custom enterprise design system), JavaScript (ES6+), Bootstrap 5.3, Chart.js 4.4, FontAwesome 6 |
| **Backend** | Java 21 LTS, Spring Boot 3.2.5, Spring Security 6, Spring JDBC (`JdbcTemplate`, `SimpleJdbcCall`), Maven |
| **Database** | Oracle Database 26ai Cloud, Oracle PL/SQL, Oracle Thin Driver (`ojdbc11` 23.3) |
| **Testing** | JUnit 5, Spring Boot Test, Live Oracle Cloud Integration Test Suite |
| **Documentation** | Swagger / OpenAPI 3 (`springdoc-openapi-starter-webmvc-ui`) |

---

## 3. Seeded Demo Accounts (All 9 Enterprise Roles)

The system is pre-seeded with 9 distinct enterprise user accounts. Use these credentials to test role-specific workflows, or use the **Quick Role Switcher** in the top navigation bar to switch personas instantly:

| Role Identifier | Role Display Name | Username | Password | Department |
| :--- | :--- | :--- | :--- | :--- |
| `ROLE_SUPER_ADMIN` | Super Administrator | `superadmin` | `SuperAdmin@2026` | Executive Management |
| `ROLE_ADMIN` | System Administrator | `admin` | `AdminPass@2026` | IT & Administration |
| `ROLE_SALES_MANAGER` | Sales Manager | `salesmanager` | `SalesMgr@2026` | Commercial Sales |
| `ROLE_SALES_REP` | Senior Sales Agent | `salesrep` | `SalesRep@2026` | Commercial Sales |
| `ROLE_CUSTOMER_SUPPORT`| Customer Support Lead | `support` | `Support@2026` | Customer Success |
| `ROLE_INVENTORY_MANAGER`| Warehouse & Inventory Mgr | `inventory` | `Inventory@2026` | Supply Chain & Logistics |
| `ROLE_FINANCE_TEAM` | Finance & Billing Officer | `finance` | `Finance@2026` | Corporate Finance |
| `ROLE_SHIPPING_TEAM` | Fulfillment & Logistics Lead | `shipping` | `Shipping@2026` | Warehouse Logistics |
| `ROLE_MIS_ANALYST` | Senior MIS / BI Analyst | `misanalyst` | `MisAnalyst@2026` | Business Intelligence |

---

## 4. Oracle Database 26ai Connection Details

The database is hosted on **Oracle 26ai Cloud**:
- **Hostname**: `db.freesql.com`
- **Port**: `1521` (TCP) / `2484` (TCPS)
- **Service Name**: `26ai_un3c1`
- **Schema / Username**: `MDRAZATECH_SCHEMA_GMAXC`
- **JDBC Connection String**: `jdbc:oracle:thin:@//db.freesql.com:1521/26ai_un3c1`

### Database Scripts Reference:
1. [01_schema.sql](file:///Users/arbajraza/Downloads/CU%20Projects%20/PL%20SQL%20Projects%20/src/main/resources/db/01_schema.sql): 27 tables, primary keys, identity sequences, foreign keys, unique constraints, and indexes.
2. [02_packages.sql](file:///Users/arbajraza/Downloads/CU%20Projects%20/PL%20SQL%20Projects%20/src/main/resources/db/02_packages.sql): Specifications and bodies for all 6 PL/SQL packages.
3. [03_seed_data.sql](file:///Users/arbajraza/Downloads/CU%20Projects%20/PL%20SQL%20Projects%20/src/main/resources/db/03_seed_data.sql): Seed data for roles, users, customers, leads, opportunities, products, inventory, orders, invoices, payments, and queues.

---

## 5. Quick Start & Execution

### 5.1 Prerequisites
- **JDK 21** or higher.
- **Apache Maven 3.8+**.
- Network connectivity to port `1521` on `db.freesql.com`.

### 5.2 Build & Run Backend Server
```bash
# Clone or navigate to the project directory
cd "/Users/arbajraza/Downloads/CU Projects /PL SQL Projects "

# Compile the application
mvn clean compile

# Launch the Spring Boot application
mvn spring-boot:run
```

Once started, the application is available at:
- **Web Application Portal**: `http://localhost:8080/`
- **Interactive Swagger UI**: `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI JSON Docs**: `http://localhost:8080/v3/api-docs`

---

## 6. Running Automated Integration Tests

To run the integration test suite verifying live Oracle 26ai cloud connectivity, PL/SQL package execution, concurrency locking, and payment idempotency:

```bash
mvn test
```

### Verified Test Cases:
1. `testDatabaseConnectivityAndVersion()`: Validates Oracle 26ai driver handshake.
2. `testPlSqlCustomerDuplicateDetection()`: Tests phone normalization and duplicate prevention via `CRM_CUSTOMER_PKG`.
3. `testOpportunityStageTransitionAndAuditHistory()`: Verifies Kanban stage progression and immutable audit logging.
4. `testConcurrencySafeInventoryReservation()`: Validates pessimistic `SELECT FOR UPDATE` locking during stock reservation.
5. `testOrderToInvoiceWorkflow()`: Verifies order placement, item price validation, and automatic Net-30 invoice generation.
6. `testPaymentProcessingAndIdempotency()`: Tests payment recording and rejection of duplicate transactions using the same idempotency key.

---

## 7. Project Documentation Deliverables

Detailed technical documentation is available in the `docs/` folder:

1. [Entity-Relationship (ER) Diagram & Schema Specification](file:///Users/arbajraza/Downloads/CU%20Projects%20/PL%20SQL%20Projects%20/docs/er_diagram.md)
   - Mermaid ER diagram and entity dictionary for all 27 tables.
2. [System Architecture & Technical Design](file:///Users/arbajraza/Downloads/CU%20Projects%20/PL%20SQL%20Projects%20/docs/architecture_diagram.md)
   - Component layering, Spring Security filters, PL/SQL orchestration, and concurrency locking flow.
3. [RESTful API Documentation](file:///Users/arbajraza/Downloads/CU%20Projects%20/PL%20SQL%20Projects%20/docs/api_documentation.md)
   - Request and response schemas, query parameters, and status codes.
4. [Academic College Project Report](file:///Users/arbajraza/Downloads/CU%20Projects%20/PL%20SQL%20Projects%20/docs/college_project_report.md)
   - Complete formal project report with Abstract, Methodology, Module Details, Results, and Future Scope.
5. [Viva Voce Questions & Answers](file:///Users/arbajraza/Downloads/CU%20Projects%20/PL%20SQL%20Projects%20/docs/viva_questions_and_answers.md)
   - 30 comprehensive viva questions covering Java 21, Spring Boot, Oracle SQL, PL/SQL, concurrency, and security.
6. [.env.example](file:///Users/arbajraza/Downloads/CU%20Projects%20/PL%20SQL%20Projects%20/.env.example)
   - Sanitized environment variable configuration template.
