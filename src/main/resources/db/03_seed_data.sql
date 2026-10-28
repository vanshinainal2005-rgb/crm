-- ============================================================================
-- Enterprise CRM: Seed Data Script
-- File: 03_seed_data.sql
-- ============================================================================

-- 1. SEED ROLES
INSERT INTO ROLES (role_id, role_name, display_name, description)
VALUES (1, 'ROLE_SUPER_ADMIN', 'Super Administrator', 'Full root access to all system modules, configurations, and audit logs');

INSERT INTO ROLES (role_id, role_name, display_name, description)
VALUES (2, 'ROLE_ADMIN', 'Administrator', 'Administrative oversight, user account management, and configuration');

INSERT INTO ROLES (role_id, role_name, display_name, description)
VALUES (3, 'ROLE_SALES_MANAGER', 'Sales Manager', 'Pipeline oversight, quota allocation, commission approvals, and sales analytics');

INSERT INTO ROLES (role_id, role_name, display_name, description)
VALUES (4, 'ROLE_SALES_AGENT', 'Sales Representative', 'Lead generation, customer management, deals, quotes, and task tracking');

INSERT INTO ROLES (role_id, role_name, display_name, description)
VALUES (5, 'ROLE_CUSTOMER_SUPPORT', 'Customer Support', 'Customer tickets, service requests, communication logs, and customer health');

INSERT INTO ROLES (role_id, role_name, display_name, description)
VALUES (6, 'ROLE_INVENTORY_MANAGER', 'Inventory Manager', 'Product catalog, stock levels, warehouse adjustments, and replenishment');

INSERT INTO ROLES (role_id, role_name, display_name, description)
VALUES (7, 'ROLE_FINANCE_TEAM', 'Finance Team', 'Invoice verification, payment reconciliation, receivables aging, and commission payouts');

INSERT INTO ROLES (role_id, role_name, display_name, description)
VALUES (8, 'ROLE_SHIPPING_TEAM', 'Shipping Team', 'Order fulfillment, packing, dispatch verification, and carrier tracking');

INSERT INTO ROLES (role_id, role_name, display_name, description)
VALUES (9, 'ROLE_MIS_ANALYST', 'MIS Analyst', 'Executive reporting, data quality analysis, custom BI exports, and KPI dashboards');

-- 2. SEED DEPARTMENT QUEUES
INSERT INTO DEPARTMENT_QUEUES (queue_id, queue_name, display_title, description, is_active)
VALUES (1, 'SALES', 'Sales Operations Queue', 'Inbound leads, qualification follow-ups, and customer onboarding reviews', 'Y');

INSERT INTO DEPARTMENT_QUEUES (queue_id, queue_name, display_title, description, is_active)
VALUES (2, 'MIS', 'MIS & BI Reporting Queue', 'Custom reporting requests, data reconciliations, and executive briefings', 'Y');

INSERT INTO DEPARTMENT_QUEUES (queue_id, queue_name, display_title, description, is_active)
VALUES (3, 'FINANCE', 'Finance & Billing Queue', 'Pending invoice approvals, credit checks, and payment verifications', 'Y');

INSERT INTO DEPARTMENT_QUEUES (queue_id, queue_name, display_title, description, is_active)
VALUES (4, 'SHIPPING', 'Fulfillment & Logistics Queue', 'Confirmed orders awaiting dispatch, customs clearance, and courier tracking', 'Y');

INSERT INTO DEPARTMENT_QUEUES (queue_id, queue_name, display_title, description, is_active)
VALUES (5, 'ADMIN', 'System Administration Queue', 'User access provisioning, role elevation, and security incident alerts', 'Y');

-- 3. SEED COMMISSION RULES (TIERED INCENTIVES)
INSERT INTO COMMISSION_RULES (rule_id, rule_name, rule_version, target_role, min_sales_threshold, max_sales_threshold, commission_rate_pct, is_active)
VALUES (1, 'Standard Tier 1 (< $10k)', 1, 'ROLE_SALES_AGENT', 0, 10000, 5.00, 'Y');

INSERT INTO COMMISSION_RULES (rule_id, rule_name, rule_version, target_role, min_sales_threshold, max_sales_threshold, commission_rate_pct, is_active)
VALUES (2, 'Growth Tier 2 ($10k - $25k)', 1, 'ROLE_SALES_AGENT', 10000.01, 25000, 8.00, 'Y');

INSERT INTO COMMISSION_RULES (rule_id, rule_name, rule_version, target_role, min_sales_threshold, max_sales_threshold, commission_rate_pct, is_active)
VALUES (3, 'Premier Tier 3 ($25k - $50k)', 1, 'ROLE_SALES_AGENT', 25000.01, 50000, 10.00, 'Y');

INSERT INTO COMMISSION_RULES (rule_id, rule_name, rule_version, target_role, min_sales_threshold, max_sales_threshold, commission_rate_pct, is_active)
VALUES (4, 'Executive Tier 4 (> $50k)', 1, 'ROLE_SALES_AGENT', 50000.01, 99999999.99, 12.50, 'Y');

-- 4. SEED USERS (Password for all demo accounts: 'password123')
-- BCrypt hash for 'password123': $2a$10$vI8aWBnW3fID.ZQ4/zo1G.q1lRpH1x1Yh7a2k7l/h9r2L0q6k5wO6
INSERT INTO USERS (user_id, username, password_hash, full_name, email, phone, department, status)
VALUES (1, 'superadmin', '$2a$10$vI8aWBnW3fID.ZQ4/zo1G.q1lRpH1x1Yh7a2k7l/h9r2L0q6k5wO6', 'Alexander Wright', 'alexander.wright@enterprisecrm.io', '+1-555-0101', 'Executive', 'ACTIVE');

INSERT INTO USERS (user_id, username, password_hash, full_name, email, phone, department, status)
VALUES (2, 'admin', '$2a$10$vI8aWBnW3fID.ZQ4/zo1G.q1lRpH1x1Yh7a2k7l/h9r2L0q6k5wO6', 'Samantha Hayes', 'samantha.hayes@enterprisecrm.io', '+1-555-0102', 'Administration', 'ACTIVE');

INSERT INTO USERS (user_id, username, password_hash, full_name, email, phone, department, status)
VALUES (3, 'salesmgr', '$2a$10$vI8aWBnW3fID.ZQ4/zo1G.q1lRpH1x1Yh7a2k7l/h9r2L0q6k5wO6', 'Marcus Vance', 'marcus.vance@enterprisecrm.io', '+1-555-0103', 'Sales', 'ACTIVE');

INSERT INTO USERS (user_id, username, password_hash, full_name, email, phone, department, status)
VALUES (4, 'salesagent', '$2a$10$vI8aWBnW3fID.ZQ4/zo1G.q1lRpH1x1Yh7a2k7l/h9r2L0q6k5wO6', 'Sarah Jenkins', 'sarah.jenkins@enterprisecrm.io', '+1-555-0104', 'Sales', 'ACTIVE');

INSERT INTO USERS (user_id, username, password_hash, full_name, email, phone, department, status)
VALUES (5, 'support', '$2a$10$vI8aWBnW3fID.ZQ4/zo1G.q1lRpH1x1Yh7a2k7l/h9r2L0q6k5wO6', 'David Chen', 'david.chen@enterprisecrm.io', '+1-555-0105', 'Support', 'ACTIVE');

INSERT INTO USERS (user_id, username, password_hash, full_name, email, phone, department, status)
VALUES (6, 'inventory', '$2a$10$vI8aWBnW3fID.ZQ4/zo1G.q1lRpH1x1Yh7a2k7l/h9r2L0q6k5wO6', 'Elena Rostova', 'elena.rostova@enterprisecrm.io', '+1-555-0106', 'Operations', 'ACTIVE');

INSERT INTO USERS (user_id, username, password_hash, full_name, email, phone, department, status)
VALUES (7, 'finance', '$2a$10$vI8aWBnW3fID.ZQ4/zo1G.q1lRpH1x1Yh7a2k7l/h9r2L0q6k5wO6', 'Robert Sterling', 'robert.sterling@enterprisecrm.io', '+1-555-0107', 'Finance', 'ACTIVE');

INSERT INTO USERS (user_id, username, password_hash, full_name, email, phone, department, status)
VALUES (8, 'shipping', '$2a$10$vI8aWBnW3fID.ZQ4/zo1G.q1lRpH1x1Yh7a2k7l/h9r2L0q6k5wO6', 'Liam O''Connor', 'liam.oconnor@enterprisecrm.io', '+1-555-0108', 'Logistics', 'ACTIVE');

INSERT INTO USERS (user_id, username, password_hash, full_name, email, phone, department, status)
VALUES (9, 'misanalyst', '$2a$10$vI8aWBnW3fID.ZQ4/zo1G.q1lRpH1x1Yh7a2k7l/h9r2L0q6k5wO6', 'Priya Sharma', 'priya.sharma@enterprisecrm.io', '+1-555-0109', 'Analytics', 'ACTIVE');

-- 5. MAP USER ROLES
INSERT INTO USER_ROLES (user_id, role_id) VALUES (1, 1);
INSERT INTO USER_ROLES (user_id, role_id) VALUES (2, 2);
INSERT INTO USER_ROLES (user_id, role_id) VALUES (3, 3);
INSERT INTO USER_ROLES (user_id, role_id) VALUES (4, 4);
INSERT INTO USER_ROLES (user_id, role_id) VALUES (5, 5);
INSERT INTO USER_ROLES (user_id, role_id) VALUES (6, 6);
INSERT INTO USER_ROLES (user_id, role_id) VALUES (7, 7);
INSERT INTO USER_ROLES (user_id, role_id) VALUES (8, 8);
INSERT INTO USER_ROLES (user_id, role_id) VALUES (9, 9);

-- 6. SEED CUSTOMERS
INSERT INTO CUSTOMERS (customer_id, customer_code, name, email, phone, normalized_phone, company, billing_address, shipping_address, customer_category, assigned_employee_id, status, notes)
VALUES (1, 'CUST-2026-0001', 'Acme Cloud Systems Corp', 'procurement@acmecloud.com', '+1-415-555-2671', '4155552671', 'Acme Corporation', '500 Howard Street, Suite 400, San Francisco, CA 94105', '500 Howard Street, Suite 400, San Francisco, CA 94105', 'ENTERPRISE', 4, 'ACTIVE', 'Key enterprise partner. High lifetime value prospect.');

INSERT INTO CUSTOMERS (customer_id, customer_code, name, email, phone, normalized_phone, company, billing_address, shipping_address, customer_category, assigned_employee_id, status, notes)
VALUES (2, 'CUST-2026-0002', 'Nexus Global Logistics Inc', 'accounts@nexuslogistics.com', '+1-312-555-8891', '3125558891', 'Nexus Logistics Group', '233 S Wacker Dr, Floor 68, Chicago, IL 60606', 'Warehouse 4B, 1200 Logistics Blvd, Joliet, IL 60436', 'VIP', 4, 'ACTIVE', 'Expanding national supply chain deployment.');

INSERT INTO CUSTOMERS (customer_id, customer_code, name, email, phone, normalized_phone, company, billing_address, shipping_address, customer_category, assigned_employee_id, status, notes)
VALUES (3, 'CUST-2026-0003', 'Zenith BioPharma Technologies', 'contact@zenithbiopharma.org', '+1-617-555-3419', '6175553419', 'Zenith Health Sciences', '100 Binney St, Cambridge, MA 02142', '100 Binney St, Cambridge, MA 02142', 'ENTERPRISE', 4, 'ACTIVE', 'Multi-year healthcare compliance and security contract.');

INSERT INTO CUSTOMERS (customer_id, customer_code, name, email, phone, normalized_phone, company, billing_address, shipping_address, customer_category, assigned_employee_id, status, notes)
VALUES (4, 'CUST-2026-0004', 'Apex Global Retail Solutions', 'vendor@apexretail.net', '+1-206-555-9011', '2065559011', 'Apex Brands International', '410 Terry Ave N, Seattle, WA 98109', 'Distribution Hub 9, 8800 S 228th St, Kent, WA 98031', 'STANDARD', 4, 'ACTIVE', 'Omnichannel retail modernization initiative.');

INSERT INTO CUSTOMERS (customer_id, customer_code, name, email, phone, normalized_phone, company, billing_address, shipping_address, customer_category, assigned_employee_id, status, notes)
VALUES (5, 'CUST-2026-0005', 'Horizon Capital & FinTech', 'info@horizonfintech.io', '+1-212-555-7721', '2125557721', 'Horizon Financial Group', '1 World Trade Center, 45th Floor, New York, NY 10007', '1 World Trade Center, 45th Floor, New York, NY 10007', 'VIP', 3, 'ACTIVE', 'Automated trading compliance and auditing infrastructure.');

-- 7. SEED CONTACTS
INSERT INTO CONTACTS (contact_id, customer_id, first_name, last_name, email, phone, designation, is_primary)
VALUES (1, 1, 'Julian', 'Mercer', 'j.mercer@acmecloud.com', '+1-415-555-2672', 'Chief Technology Officer', 'Y');

INSERT INTO CONTACTS (contact_id, customer_id, first_name, last_name, email, phone, designation, is_primary)
VALUES (2, 2, 'Clara', 'Danvers', 'c.danvers@nexuslogistics.com', '+1-312-555-8892', 'VP of Supply Chain', 'Y');

INSERT INTO CONTACTS (contact_id, customer_id, first_name, last_name, email, phone, designation, is_primary)
VALUES (3, 3, 'Dr. Aris', 'Thorne', 'a.thorne@zenithbiopharma.org', '+1-617-555-3420', 'Director of Research IT', 'Y');

-- 8. SEED LEADS
INSERT INTO LEADS (lead_id, first_name, last_name, company, email, phone, normalized_phone, lead_source, lead_status, assigned_to, estimated_budget, qualification_notes)
VALUES (1, 'Ethan', 'Hunt', 'Vanguard Aerospace LLC', 'ehunt@vanguardaero.com', '+1-310-555-9921', '3105559921', 'EVENT', 'QUALIFIED', 4, 45000.00, 'Met at Aerospace Summit. Interested in enterprise CRM and dispatch integration.');

INSERT INTO LEADS (lead_id, first_name, last_name, company, email, phone, normalized_phone, lead_source, lead_status, assigned_to, estimated_budget, qualification_notes)
VALUES (2, 'Miriam', 'Alvarez', 'Starlight Media Group', 'miriam@starlightmedia.tv', '+1-323-555-4301', '3235554301', 'WEBSITE', 'CONTACTED', 4, 25000.00, 'Downloaded whitepaper on CRM analytics. Follow-up demo requested.');

INSERT INTO LEADS (lead_id, first_name, last_name, company, email, phone, normalized_phone, lead_source, lead_status, assigned_to, estimated_budget, qualification_notes)
VALUES (3, 'Kenneth', 'Cole', 'Pacific Maritime Shipping', 'kcole@pacificmaritime.net', '+1-415-555-6612', '4155556612', 'REFERRAL', 'NEW', 4, 60000.00, 'Referred by Clara Danvers at Nexus Logistics. Urgently looking for order orchestration.');

-- 9. SEED OPPORTUNITIES (PIPELINE)
INSERT INTO OPPORTUNITIES (opportunity_id, name, customer_id, stage, deal_value, probability, expected_close_date, assigned_to, next_follow_up)
VALUES (1, 'Acme Global Cloud Deployment', 1, 'NEGOTIATION', 75000.00, 80.00, DATE '2026-11-30', 4, CURRENT_TIMESTAMP + INTERVAL '1' DAY);

INSERT INTO OPPORTUNITIES (opportunity_id, name, customer_id, stage, deal_value, probability, expected_close_date, assigned_to, next_follow_up)
VALUES (2, 'Nexus Fleet Management Suite', 2, 'PROPOSAL', 48000.00, 60.00, DATE '2026-12-15', 4, CURRENT_TIMESTAMP + INTERVAL '3' DAY);

INSERT INTO OPPORTUNITIES (opportunity_id, name, customer_id, stage, deal_value, probability, expected_close_date, assigned_to, next_follow_up)
VALUES (3, 'Zenith BioData Compliance Engine', 3, 'WON', 95000.00, 100.00, DATE '2026-10-15', 4, NULL);

INSERT INTO OPPORTUNITIES (opportunity_id, name, customer_id, stage, deal_value, probability, expected_close_date, assigned_to, next_follow_up)
VALUES (4, 'Apex OmniChannel Order Bridge', 4, 'CONTACTED', 32000.00, 40.00, DATE '2026-12-31', 4, CURRENT_TIMESTAMP + INTERVAL '5' DAY);

INSERT INTO OPPORTUNITIES (opportunity_id, name, customer_id, stage, deal_value, probability, expected_close_date, assigned_to, next_follow_up)
VALUES (5, 'Horizon Secure Gateway Upgrade', 5, 'QUALIFIED', 54000.00, 25.00, DATE '2027-01-20', 3, CURRENT_TIMESTAMP + INTERVAL '2' DAY);

-- 10. SEED OPPORTUNITY HISTORY
INSERT INTO OPPORTUNITY_HISTORY (opportunity_id, from_stage, to_stage, deal_value, probability, notes, changed_by)
VALUES (1, 'PROPOSAL', 'NEGOTIATION', 75000.00, 80.00, 'Executive sponsor agreed to master contract terms', 4);

INSERT INTO OPPORTUNITY_HISTORY (opportunity_id, from_stage, to_stage, deal_value, probability, notes, changed_by)
VALUES (3, 'NEGOTIATION', 'WON', 95000.00, 100.00, 'Contract fully signed and initial PO received', 4);

-- 11. SEED PRODUCTS & INVENTORY
INSERT INTO PRODUCTS (product_id, sku, name, description, category, cost_price, min_selling_price, max_selling_price, current_selling_price, status)
VALUES (1, 'CRM-ENT-LIC-01', 'Enterprise CRM Core Platform License', 'Annual multi-tenant enterprise core system subscription for up to 500 seats', 'SOFTWARE', 12000.00, 18000.00, 30000.00, 22500.00, 'ACTIVE');

INSERT INTO PRODUCTS (product_id, sku, name, description, category, cost_price, min_selling_price, max_selling_price, current_selling_price, status)
VALUES (2, 'CRM-MOD-INV-02', 'Automated Inventory & Warehouse Module', 'High-throughput real-time inventory tracking, multi-warehouse allocation engine', 'SOFTWARE', 6000.00, 9500.00, 16000.00, 12000.00, 'ACTIVE');

INSERT INTO PRODUCTS (product_id, sku, name, description, category, cost_price, min_selling_price, max_selling_price, current_selling_price, status)
VALUES (3, 'CRM-MOD-FIN-03', 'Financial Reconciliation & Invoicing Suite', 'Automated multi-currency billing, tax calculations, commission automation, and ledger', 'SOFTWARE', 8000.00, 12000.00, 20000.00, 15000.00, 'ACTIVE');

INSERT INTO PRODUCTS (product_id, sku, name, description, category, cost_price, min_selling_price, max_selling_price, current_selling_price, status)
VALUES (4, 'HW-IOT-SCAN-04', 'Rugged Industrial IoT Logistics Scanner', 'IP67 ruggedized barcode and RFID handheld reader with real-time sync', 'HARDWARE', 450.00, 750.00, 1200.00, 890.00, 'ACTIVE');

INSERT INTO PRODUCTS (product_id, sku, name, description, category, cost_price, min_selling_price, max_selling_price, current_selling_price, status)
VALUES (5, 'SVC-IMPL-PRO-05', 'Dedicated Enterprise Solution Architecture Onboarding', '40 hours of tailored enterprise onboarding, database schema mapping, and training', 'SERVICE', 4000.00, 6500.00, 10000.00, 8000.00, 'ACTIVE');

-- INVENTORY FOR PRODUCTS
INSERT INTO INVENTORY (inventory_id, product_id, available_stock, reserved_stock, reorder_threshold, warehouse_location)
VALUES (1, 1, 999, 15, 10, 'CLOUD-DIGITAL-VAULT');

INSERT INTO INVENTORY (inventory_id, product_id, available_stock, reserved_stock, reorder_threshold, warehouse_location)
VALUES (2, 2, 999, 10, 10, 'CLOUD-DIGITAL-VAULT');

INSERT INTO INVENTORY (inventory_id, product_id, available_stock, reserved_stock, reorder_threshold, warehouse_location)
VALUES (3, 3, 999, 8, 10, 'CLOUD-DIGITAL-VAULT');

INSERT INTO INVENTORY (inventory_id, product_id, available_stock, reserved_stock, reorder_threshold, warehouse_location)
VALUES (4, 4, 185, 25, 30, 'WAREHOUSE-BAY-A12');

INSERT INTO INVENTORY (inventory_id, product_id, available_stock, reserved_stock, reorder_threshold, warehouse_location)
VALUES (5, 5, 500, 12, 10, 'PROFESSIONAL-SERVICES-POOL');

-- 12. SEED ORDERS
INSERT INTO ORDERS (order_id, order_number, customer_id, order_date, subtotal, discount_amount, tax_amount, total_amount, order_status, assigned_sales_rep, notes)
VALUES (1, 'ORD-20261001-0001', 3, DATE '2026-10-05', 45500.00, 1500.00, 7920.00, 51920.00, 'CONFIRMED', 4, 'Zenith BioPharma Phase 1 rollout order.');

INSERT INTO ORDERS (order_id, order_number, customer_id, order_date, subtotal, discount_amount, tax_amount, total_amount, order_status, assigned_sales_rep, notes)
VALUES (2, 'ORD-20261010-0002', 1, DATE '2026-10-12', 34500.00, 1000.00, 6030.00, 39530.00, 'CONFIRMED', 4, 'Acme Cloud deployment expansion.');

INSERT INTO ORDERS (order_id, order_number, customer_id, order_date, subtotal, discount_amount, tax_amount, total_amount, order_status, assigned_sales_rep, notes)
VALUES (3, 'ORD-20261020-0003', 2, DATE '2026-10-20', 21350.00, 500.00, 3753.00, 24603.00, 'DRAFT', 4, 'Hardware and scanner order under customer review.');

-- ORDER ITEMS
INSERT INTO ORDER_ITEMS (item_id, order_id, product_id, quantity, unit_price, discount, line_total)
VALUES (1, 1, 1, 1, 22500.00, 0, 22500.00);

INSERT INTO ORDER_ITEMS (item_id, order_id, product_id, quantity, unit_price, discount, line_total)
VALUES (2, 1, 3, 1, 15000.00, 0, 15000.00);

INSERT INTO ORDER_ITEMS (item_id, order_id, product_id, quantity, unit_price, discount, line_total)
VALUES (3, 1, 5, 1, 8000.00, 0, 8000.00);

INSERT INTO ORDER_ITEMS (item_id, order_id, product_id, quantity, unit_price, discount, line_total)
VALUES (4, 2, 1, 1, 22500.00, 0, 22500.00);

INSERT INTO ORDER_ITEMS (item_id, order_id, product_id, quantity, unit_price, discount, line_total)
VALUES (5, 2, 2, 1, 12000.00, 0, 12000.00);

INSERT INTO ORDER_ITEMS (item_id, order_id, product_id, quantity, unit_price, discount, line_total)
VALUES (6, 3, 4, 20, 890.00, 0, 17800.00);

INSERT INTO ORDER_ITEMS (item_id, order_id, product_id, quantity, unit_price, discount, line_total)
VALUES (7, 3, 5, 1, 3550.00, 0, 3550.00);

-- 13. SEED INVOICES
INSERT INTO INVOICES (invoice_id, invoice_number, order_id, customer_id, issue_date, due_date, subtotal, tax_amount, discount_amount, total_amount, paid_amount, balance_amount, payment_status, notes)
VALUES (1, 'INV-2026-00001', 1, 3, DATE '2026-10-06', DATE '2026-11-05', 45500.00, 7920.00, 1500.00, 51920.00, 51920.00, 0.00, 'PAID', 'Settled via wire transfer.');

INSERT INTO INVOICES (invoice_id, invoice_number, order_id, customer_id, issue_date, due_date, subtotal, tax_amount, discount_amount, total_amount, paid_amount, balance_amount, payment_status, notes)
VALUES (2, 'INV-2026-00002', 2, 1, DATE '2026-10-13', DATE '2026-11-12', 34500.00, 6030.00, 1000.00, 39530.00, 20000.00, 19530.00, 'PARTIALLY_PAID', 'Initial 50% milestone received.');

-- INVOICE ITEMS
INSERT INTO INVOICE_ITEMS (item_id, invoice_id, product_id, description, quantity, unit_price, tax_rate, line_total)
VALUES (1, 1, 1, 'Enterprise CRM Core Platform License', 1, 22500.00, 18.00, 22500.00);

INSERT INTO INVOICE_ITEMS (item_id, invoice_id, product_id, description, quantity, unit_price, tax_rate, line_total)
VALUES (2, 1, 3, 'Financial Reconciliation & Invoicing Suite', 1, 15000.00, 18.00, 15000.00);

INSERT INTO INVOICE_ITEMS (item_id, invoice_id, product_id, description, quantity, unit_price, tax_rate, line_total)
VALUES (3, 1, 5, 'Dedicated Enterprise Solution Architecture Onboarding', 1, 8000.00, 18.00, 8000.00);

INSERT INTO INVOICE_ITEMS (item_id, invoice_id, product_id, description, quantity, unit_price, tax_rate, line_total)
VALUES (4, 2, 1, 'Enterprise CRM Core Platform License', 1, 22500.00, 18.00, 22500.00);

INSERT INTO INVOICE_ITEMS (item_id, invoice_id, product_id, description, quantity, unit_price, tax_rate, line_total)
VALUES (5, 2, 2, 'Automated Inventory & Warehouse Module', 1, 12000.00, 18.00, 12000.00);

-- 14. SEED PAYMENTS
INSERT INTO PAYMENTS (payment_id, payment_reference, idempotency_key, invoice_id, customer_id, amount, payment_method, payment_status, notes, recorded_by)
VALUES (1, 'PAY-ZENITH-2026-WIRE-01', 'IDEMP-ZENITH-001', 1, 3, 51920.00, 'WIRE', 'COMPLETED', 'Full settlement received via JP Morgan Chase Federal Wire', 7);

INSERT INTO PAYMENTS (payment_id, payment_reference, idempotency_key, invoice_id, customer_id, amount, payment_method, payment_status, notes, recorded_by)
VALUES (2, 'PAY-ACME-2026-PART-02', 'IDEMP-ACME-002', 2, 1, 20000.00, 'BANK_TRANSFER', 'COMPLETED', 'Milestone payment #1 received via ACH', 7);

-- 15. SEED COMMISSION RECORDS
INSERT INTO COMMISSION_RECORDS (record_id, employee_id, order_id, invoice_id, rule_id, rule_version, sales_amount, commission_pct, calculated_commission, status, approved_by, approved_at, paid_at, notes)
VALUES (1, 4, 1, 1, 3, 1, 45500.00, 10.00, 4550.00, 'APPROVED', 3, CURRENT_TIMESTAMP, NULL, 'Commission approved by Sales Manager Marcus Vance');

INSERT INTO COMMISSION_RECORDS (record_id, employee_id, order_id, invoice_id, rule_id, rule_version, sales_amount, commission_pct, calculated_commission, status, approved_by, notes)
VALUES (2, 4, 2, 2, 3, 1, 34500.00, 10.00, 3450.00, 'PENDING', NULL, 'Pending full customer settlement on Invoice #INV-2026-00002');

-- 16. SEED TASKS
INSERT INTO TASKS (task_id, title, description, customer_id, opportunity_id, assigned_to, created_by, priority, status, due_date, reminder_date)
VALUES (1, 'Final Contract Review with Acme General Counsel', 'Coordinate with legal regarding SLA guarantees and data tenancy requirements.', 1, 1, 4, 3, 'URGENT', 'IN_PROGRESS', CURRENT_TIMESTAMP + INTERVAL '1' DAY, CURRENT_TIMESTAMP + INTERVAL '12' HOUR);

INSERT INTO TASKS (task_id, title, description, customer_id, opportunity_id, assigned_to, created_by, priority, status, due_date, reminder_date)
VALUES (2, 'Schedule Warehouse Pilot for Nexus Logistics', 'Demonstrate RFID scanner synchronicity at Joliet logistics hub.', 2, 2, 4, 4, 'HIGH', 'PENDING', CURRENT_TIMESTAMP + INTERVAL '3' DAY, CURRENT_TIMESTAMP + INTERVAL '2' DAY);

INSERT INTO TASKS (task_id, title, description, customer_id, opportunity_id, assigned_to, created_by, priority, status, due_date)
VALUES (3, 'Deliver Onboarding Architecture Blueprint to Zenith', 'Finalize database schema documentation and API endpoint mapping.', 3, 3, 5, 4, 'MEDIUM', 'COMPLETED', CURRENT_TIMESTAMP - INTERVAL '2' DAY);

INSERT INTO TASKS (task_id, title, description, customer_id, opportunity_id, assigned_to, created_by, priority, status, due_date)
VALUES (4, 'Quarterly Billing Reconciliation with Horizon Capital', 'Verify monthly invoice schedules against executed service level agreements.', 5, 5, 7, 1, 'HIGH', 'PENDING', CURRENT_TIMESTAMP + INTERVAL '4' DAY);

-- 17. SEED DEPARTMENT QUEUE ASSIGNMENTS
INSERT INTO QUEUE_ASSIGNMENTS (assignment_id, queue_id, title, entity_type, entity_id, priority, status, assigned_to, created_by)
VALUES (1, 1, 'Inbound Qualified Lead: Ethan Hunt (Vanguard Aerospace)', 'LEAD', 1, 'HIGH', 'OPEN', 4, 1);

INSERT INTO QUEUE_ASSIGNMENTS (assignment_id, queue_id, title, entity_type, entity_id, priority, status, assigned_to, created_by)
VALUES (2, 4, 'Shipment Packing for Order #ORD-20261001-0001 (Zenith)', 'ORDER', 1, 'MEDIUM', 'IN_PROGRESS', 8, 4);

INSERT INTO QUEUE_ASSIGNMENTS (assignment_id, queue_id, title, entity_type, entity_id, priority, status, assigned_to, created_by)
VALUES (3, 3, 'Reconcile Balance for Invoice #INV-2026-00002 ($19,530.00)', 'INVOICE', 2, 'HIGH', 'OPEN', 7, 7);

INSERT INTO QUEUE_ASSIGNMENTS (assignment_id, queue_id, title, entity_type, entity_id, priority, status, assigned_to, created_by)
VALUES (4, 2, 'Q3 Executive Sales vs Pipeline Variance Report Request', 'REPORT', 1, 'MEDIUM', 'OPEN', 9, 3);

INSERT INTO QUEUE_ASSIGNMENTS (assignment_id, queue_id, title, entity_type, entity_id, priority, status, assigned_to, created_by)
VALUES (5, 5, 'Security Audit: Verify Role Permissions for Financial Transactions', 'AUDIT', 1, 'LOW', 'OPEN', 2, 1);

-- 18. SEED NOTIFICATIONS
INSERT INTO NOTIFICATIONS (notification_id, user_id, title, message, type, is_read, link_url)
VALUES (1, 4, 'New Lead Assigned', 'Lead Vanguard Aerospace (Ethan Hunt) has been assigned to your sales queue.', 'INFO', 'N', '/leads');

INSERT INTO NOTIFICATIONS (notification_id, user_id, title, message, type, is_read, link_url)
VALUES (2, 4, 'Commission Approved', 'Your commission of $4,550.00 for Zenith BioPharma order #ORD-20261001-0001 was approved.', 'SUCCESS', 'N', '/commissions');

INSERT INTO NOTIFICATIONS (notification_id, user_id, title, message, type, is_read, link_url)
VALUES (3, 7, 'Payment Received', 'Wire transfer of $51,920.00 received from Zenith BioPharma Technologies.', 'SUCCESS', 'Y', '/payments');

-- 19. SEED AUDIT LOGS
INSERT INTO AUDIT_LOGS (audit_id, user_id, username, action, entity_name, entity_id, old_values, new_values, ip_address)
VALUES (1, 1, 'superadmin', 'INITIALIZE', 'SYSTEM', 1, NULL, 'Enterprise CRM System Initialized with Full Schema and Sample Data', '127.0.0.1');

-- 20. RESTART ALL IDENTITY SEQUENCES BEYOND SEED IDS
DECLARE
    PROCEDURE reset_seq(p_table VARCHAR2, p_col VARCHAR2) IS
        v_val NUMBER;
    BEGIN
        EXECUTE IMMEDIATE 'SELECT NVL(MAX(' || p_col || '), 0) + 10 FROM ' || p_table INTO v_val;
        EXECUTE IMMEDIATE 'ALTER TABLE ' || p_table || ' MODIFY ' || p_col || ' GENERATED BY DEFAULT AS IDENTITY (RESTART START WITH ' || v_val || ')';
    EXCEPTION
        WHEN OTHERS THEN NULL;
    END;
BEGIN
    reset_seq('CUSTOMERS', 'customer_id');
    reset_seq('CONTACTS', 'contact_id');
    reset_seq('LEADS', 'lead_id');
    reset_seq('OPPORTUNITIES', 'opportunity_id');
    reset_seq('OPPORTUNITY_HISTORY', 'history_id');
    reset_seq('TASKS', 'task_id');
    reset_seq('ACTIVITY_LOGS', 'activity_id');
    reset_seq('PRODUCTS', 'product_id');
    reset_seq('PRODUCT_VARIANTS', 'variant_id');
    reset_seq('INVENTORY', 'inventory_id');
    reset_seq('INVENTORY_MOVEMENTS', 'movement_id');
    reset_seq('ORDERS', 'order_id');
    reset_seq('ORDER_ITEMS', 'item_id');
    reset_seq('INVOICES', 'invoice_id');
    reset_seq('INVOICE_ITEMS', 'item_id');
    reset_seq('PAYMENTS', 'payment_id');
    reset_seq('COMMISSION_RULES', 'rule_id');
    reset_seq('COMMISSION_RECORDS', 'record_id');
    reset_seq('QUEUE_ASSIGNMENTS', 'assignment_id');
    reset_seq('NOTIFICATIONS', 'notification_id');
    reset_seq('AUDIT_LOGS', 'audit_id');
END;
/

COMMIT;


