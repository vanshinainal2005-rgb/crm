-- ============================================================================
-- Enterprise CRM: PL/SQL Package Specifications and Bodies
-- File: 02_packages.sql
-- ============================================================================

-- ============================================================================
-- PACKAGE 1: CRM_CUSTOMER_PKG
-- ============================================================================
CREATE OR REPLACE PACKAGE CRM_CUSTOMER_PKG AS
    -- Function to normalize phone numbers (strip non-digits, ensure standard format)
    FUNCTION NORMALIZE_PHONE(p_phone IN VARCHAR2) RETURN VARCHAR2;

    -- Procedure to create customer with validation & duplicate detection
    PROCEDURE CREATE_CUSTOMER(
        p_name                 IN  VARCHAR2,
        p_email                IN  VARCHAR2,
        p_phone                IN  VARCHAR2,
        p_company              IN  VARCHAR2,
        p_billing_address      IN  VARCHAR2,
        p_shipping_address     IN  VARCHAR2,
        p_category             IN  VARCHAR2,
        p_assigned_employee_id IN  NUMBER,
        p_notes                IN  CLOB,
        p_created_by           IN  NUMBER,
        o_customer_id          OUT NUMBER,
        o_customer_code        OUT VARCHAR2,
        o_status               OUT VARCHAR2,
        o_message              OUT VARCHAR2
    );

    -- Procedure to update customer details
    PROCEDURE UPDATE_CUSTOMER(
        p_customer_id          IN  NUMBER,
        p_name                 IN  VARCHAR2,
        p_email                IN  VARCHAR2,
        p_phone                IN  VARCHAR2,
        p_company              IN  VARCHAR2,
        p_billing_address      IN  VARCHAR2,
        p_shipping_address     IN  VARCHAR2,
        p_category             IN  VARCHAR2,
        p_assigned_employee_id IN  NUMBER,
        p_status               IN  VARCHAR2,
        p_notes                IN  CLOB,
        p_updated_by           IN  NUMBER,
        o_status               OUT VARCHAR2,
        o_message              OUT VARCHAR2
    );

    -- Procedure to detect duplicate customer by email or normalized phone
    PROCEDURE DETECT_DUPLICATE_CUSTOMER(
        p_email          IN  VARCHAR2,
        p_phone          IN  VARCHAR2,
        p_exclude_id     IN  NUMBER,
        o_is_duplicate   OUT VARCHAR2,
        o_dup_cust_id    OUT NUMBER,
        o_dup_cust_name  OUT VARCHAR2
    );
END CRM_CUSTOMER_PKG;
/

CREATE OR REPLACE PACKAGE BODY CRM_CUSTOMER_PKG AS

    FUNCTION NORMALIZE_PHONE(p_phone IN VARCHAR2) RETURN VARCHAR2 IS
        v_digits VARCHAR2(40);
    BEGIN
        IF p_phone IS NULL THEN
            RETURN NULL;
        END IF;
        -- Strip non-numeric characters using regex
        v_digits := REGEXP_REPLACE(p_phone, '[^0-9]', '');
        -- If contains leading country code 91 or 1 and length > 10, extract last 10 digits
        IF LENGTH(v_digits) > 10 THEN
            v_digits := SUBSTR(v_digits, -10);
        END IF;
        RETURN v_digits;
    END NORMALIZE_PHONE;

    PROCEDURE DETECT_DUPLICATE_CUSTOMER(
        p_email          IN  VARCHAR2,
        p_phone          IN  VARCHAR2,
        p_exclude_id     IN  NUMBER,
        o_is_duplicate   OUT VARCHAR2,
        o_dup_cust_id    OUT NUMBER,
        o_dup_cust_name  OUT VARCHAR2
    ) IS
        v_norm_phone VARCHAR2(30);
    BEGIN
        o_is_duplicate := 'N';
        o_dup_cust_id := NULL;
        o_dup_cust_name := NULL;

        v_norm_phone := NORMALIZE_PHONE(p_phone);

        -- Check by email
        IF p_email IS NOT NULL THEN
            BEGIN
                SELECT customer_id, name 
                INTO o_dup_cust_id, o_dup_cust_name
                FROM CUSTOMERS
                WHERE LOWER(TRIM(email)) = LOWER(TRIM(p_email))
                  AND (p_exclude_id IS NULL OR customer_id <> p_exclude_id)
                  AND ROWNUM = 1;
                
                o_is_duplicate := 'Y';
                RETURN;
            EXCEPTION
                WHEN NO_DATA_FOUND THEN NULL;
            END;
        END IF;

        -- Check by normalized phone
        IF v_norm_phone IS NOT NULL THEN
            BEGIN
                SELECT customer_id, name 
                INTO o_dup_cust_id, o_dup_cust_name
                FROM CUSTOMERS
                WHERE normalized_phone = v_norm_phone
                  AND (p_exclude_id IS NULL OR customer_id <> p_exclude_id)
                  AND ROWNUM = 1;
                
                o_is_duplicate := 'Y';
                RETURN;
            EXCEPTION
                WHEN NO_DATA_FOUND THEN NULL;
            END;
        END IF;
    END DETECT_DUPLICATE_CUSTOMER;

    PROCEDURE CREATE_CUSTOMER(
        p_name                 IN  VARCHAR2,
        p_email                IN  VARCHAR2,
        p_phone                IN  VARCHAR2,
        p_company              IN  VARCHAR2,
        p_billing_address      IN  VARCHAR2,
        p_shipping_address     IN  VARCHAR2,
        p_category             IN  VARCHAR2,
        p_assigned_employee_id IN  NUMBER,
        p_notes                IN  CLOB,
        p_created_by           IN  NUMBER,
        o_customer_id          OUT NUMBER,
        o_customer_code        OUT VARCHAR2,
        o_status               OUT VARCHAR2,
        o_message              OUT VARCHAR2
    ) IS
        v_is_dup VARCHAR2(1);
        v_dup_id NUMBER;
        v_dup_name VARCHAR2(150);
        v_norm_phone VARCHAR2(30);
        v_code VARCHAR2(30);
        v_creator_name VARCHAR2(60);
    BEGIN
        -- 1. Validation
        IF p_name IS NULL OR TRIM(p_name) IS NULL THEN
            o_status := 'ERROR';
            o_message := 'Customer name cannot be empty.';
            RETURN;
        END IF;

        IF p_email IS NULL OR NOT REGEXP_LIKE(p_email, '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$') THEN
            o_status := 'ERROR';
            o_message := 'Valid email address is required.';
            RETURN;
        END IF;

        -- 2. Duplicate Check
        DETECT_DUPLICATE_CUSTOMER(p_email, p_phone, NULL, v_is_dup, v_dup_id, v_dup_name);
        IF v_is_dup = 'Y' THEN
            o_status := 'ERROR';
            o_message := 'Duplicate customer detected! Matches customer ID ' || v_dup_id || ' (' || v_dup_name || ').';
            RETURN;
        END IF;

        v_norm_phone := NORMALIZE_PHONE(p_phone);

        -- 3. Generate Customer Code: CUST-YYYY-XXXX
        SELECT 'CUST-' || TO_CHAR(SYSDATE, 'YYYY') || '-' || LPAD(NVL(MAX(customer_id), 0) + 1, 4, '0')
        INTO v_code
        FROM CUSTOMERS;

        -- 4. Insert Customer
        INSERT INTO CUSTOMERS (
            customer_code, name, email, phone, normalized_phone, company,
            billing_address, shipping_address, customer_category,
            assigned_employee_id, status, notes, created_at, updated_at
        ) VALUES (
            v_code, TRIM(p_name), LOWER(TRIM(p_email)), p_phone, v_norm_phone, p_company,
            p_billing_address, NVL(p_shipping_address, p_billing_address), NVL(p_category, 'STANDARD'),
            p_assigned_employee_id, 'ACTIVE', p_notes, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        ) RETURNING customer_id INTO o_customer_id;

        o_customer_code := v_code;

        -- 5. Create Contact Record if name provided
        INSERT INTO CONTACTS (customer_id, first_name, email, phone, is_primary)
        VALUES (o_customer_id, p_name, LOWER(TRIM(p_email)), p_phone, 'Y');

        -- 6. Log Activity & Audit
        SELECT NVL(username, 'SYSTEM') INTO v_creator_name FROM USERS WHERE user_id = p_created_by;

        INSERT INTO ACTIVITY_LOGS (entity_type, entity_id, activity_type, summary, details, performed_by)
        VALUES ('CUSTOMER', o_customer_id, 'STATUS_CHANGE', 'Customer created', 'Initial creation as ACTIVE with code ' || v_code, p_created_by);

        INSERT INTO AUDIT_LOGS (user_id, username, action, entity_name, entity_id, new_values)
        VALUES (p_created_by, v_creator_name, 'CREATE', 'CUSTOMERS', o_customer_id, 'Name: ' || p_name || ', Email: ' || p_email);

        o_status := 'SUCCESS';
        o_message := 'Customer successfully registered with code: ' || v_code;
    EXCEPTION
        WHEN OTHERS THEN
            o_status := 'ERROR';
            o_message := 'Failed to create customer: ' || SQLERRM;
    END CREATE_CUSTOMER;

    PROCEDURE UPDATE_CUSTOMER(
        p_customer_id          IN  NUMBER,
        p_name                 IN  VARCHAR2,
        p_email                IN  VARCHAR2,
        p_phone                IN  VARCHAR2,
        p_company              IN  VARCHAR2,
        p_billing_address      IN  VARCHAR2,
        p_shipping_address     IN  VARCHAR2,
        p_category             IN  VARCHAR2,
        p_assigned_employee_id IN  NUMBER,
        p_status               IN  VARCHAR2,
        p_notes                IN  CLOB,
        p_updated_by           IN  NUMBER,
        o_status               OUT VARCHAR2,
        o_message              OUT VARCHAR2
    ) IS
        v_is_dup VARCHAR2(1);
        v_dup_id NUMBER;
        v_dup_name VARCHAR2(150);
        v_norm_phone VARCHAR2(30);
        v_user_name VARCHAR2(60);
    BEGIN
        -- Duplicate check excluding current customer
        DETECT_DUPLICATE_CUSTOMER(p_email, p_phone, p_customer_id, v_is_dup, v_dup_id, v_dup_name);
        IF v_is_dup = 'Y' THEN
            o_status := 'ERROR';
            o_message := 'Email or phone already associated with customer ID ' || v_dup_id || ' (' || v_dup_name || ').';
            RETURN;
        END IF;

        v_norm_phone := NORMALIZE_PHONE(p_phone);

        UPDATE CUSTOMERS
        SET name = TRIM(p_name),
            email = LOWER(TRIM(p_email)),
            phone = p_phone,
            normalized_phone = v_norm_phone,
            company = p_company,
            billing_address = p_billing_address,
            shipping_address = p_shipping_address,
            customer_category = NVL(p_category, customer_category),
            assigned_employee_id = p_assigned_employee_id,
            status = NVL(p_status, status),
            notes = NVL(p_notes, notes),
            updated_at = CURRENT_TIMESTAMP
        WHERE customer_id = p_customer_id;

        IF SQL%ROWCOUNT = 0 THEN
            o_status := 'ERROR';
            o_message := 'Customer record not found for ID: ' || p_customer_id;
            RETURN;
        END IF;

        SELECT NVL(username, 'SYSTEM') INTO v_user_name FROM USERS WHERE user_id = p_updated_by;

        INSERT INTO AUDIT_LOGS (user_id, username, action, entity_name, entity_id, new_values)
        VALUES (p_updated_by, v_user_name, 'UPDATE', 'CUSTOMERS', p_customer_id, 'Updated customer ID ' || p_customer_id);

        o_status := 'SUCCESS';
        o_message := 'Customer updated successfully.';
    EXCEPTION
        WHEN OTHERS THEN
            o_status := 'ERROR';
            o_message := 'Failed to update customer: ' || SQLERRM;
    END UPDATE_CUSTOMER;

END CRM_CUSTOMER_PKG;
/

-- ============================================================================
-- PACKAGE 2: CRM_SALES_PKG
-- ============================================================================
CREATE OR REPLACE PACKAGE CRM_SALES_PKG AS
    -- Create new Sales Lead
    PROCEDURE CREATE_LEAD(
        p_first_name     IN  VARCHAR2,
        p_last_name      IN  VARCHAR2,
        p_company        IN  VARCHAR2,
        p_email          IN  VARCHAR2,
        p_phone          IN  VARCHAR2,
        p_lead_source    IN  VARCHAR2,
        p_assigned_to    IN  NUMBER,
        p_budget         IN  NUMBER,
        p_notes          IN  VARCHAR2,
        p_created_by     IN  NUMBER,
        o_lead_id        OUT NUMBER,
        o_status         OUT VARCHAR2,
        o_message        OUT VARCHAR2
    );

    -- Convert Qualified Lead into Customer and Opportunity
    PROCEDURE CONVERT_LEAD(
        p_lead_id            IN  NUMBER,
        p_opportunity_name   IN  VARCHAR2,
        p_deal_value         IN  NUMBER,
        p_expected_close     IN  DATE,
        p_converted_by       IN  NUMBER,
        o_customer_id        OUT NUMBER,
        o_opportunity_id     OUT NUMBER,
        o_status             OUT VARCHAR2,
        o_message            OUT VARCHAR2
    );

    -- Update Opportunity Stage and record audit history
    PROCEDURE UPDATE_OPPORTUNITY_STAGE(
        p_opportunity_id IN  NUMBER,
        p_new_stage      IN  VARCHAR2,
        p_deal_value     IN  NUMBER,
        p_probability    IN  NUMBER,
        p_lost_reason    IN  VARCHAR2,
        p_notes          IN  VARCHAR2,
        p_changed_by     IN  NUMBER,
        o_status         OUT VARCHAR2,
        o_message        OUT VARCHAR2
    );
END CRM_SALES_PKG;
/

CREATE OR REPLACE PACKAGE BODY CRM_SALES_PKG AS

    PROCEDURE CREATE_LEAD(
        p_first_name     IN  VARCHAR2,
        p_last_name      IN  VARCHAR2,
        p_company        IN  VARCHAR2,
        p_email          IN  VARCHAR2,
        p_phone          IN  VARCHAR2,
        p_lead_source    IN  VARCHAR2,
        p_assigned_to    IN  NUMBER,
        p_budget         IN  NUMBER,
        p_notes          IN  VARCHAR2,
        p_created_by     IN  NUMBER,
        o_lead_id        OUT NUMBER,
        o_status         OUT VARCHAR2,
        o_message        OUT VARCHAR2
    ) IS
        v_norm_phone VARCHAR2(30);
        v_queue_id NUMBER;
    BEGIN
        v_norm_phone := CRM_CUSTOMER_PKG.NORMALIZE_PHONE(p_phone);

        INSERT INTO LEADS (
            first_name, last_name, company, email, phone, normalized_phone,
            lead_source, lead_status, assigned_to, estimated_budget,
            qualification_notes, follow_up_date, created_at, updated_at
        ) VALUES (
            p_first_name, p_last_name, p_company, LOWER(TRIM(p_email)), p_phone, v_norm_phone,
            NVL(p_lead_source, 'WEBSITE'), 'NEW', p_assigned_to, NVL(p_budget, 0),
            p_notes, CURRENT_TIMESTAMP + INTERVAL '2' DAY, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        ) RETURNING lead_id INTO o_lead_id;

        -- Create departmental queue entry for Sales team
        BEGIN
            SELECT queue_id INTO v_queue_id FROM DEPARTMENT_QUEUES WHERE queue_name = 'SALES' AND ROWNUM = 1;
            INSERT INTO QUEUE_ASSIGNMENTS (
                queue_id, title, entity_type, entity_id, priority, status, assigned_to, created_by
            ) VALUES (
                v_queue_id, 'New Lead: ' || p_first_name || ' ' || p_last_name || ' (' || NVL(p_company, 'Individual') || ')',
                'LEAD', o_lead_id, 'HIGH', 'OPEN', p_assigned_to, p_created_by
            );
        EXCEPTION
            WHEN OTHERS THEN NULL;
        END;

        o_status := 'SUCCESS';
        o_message := 'Lead created successfully with ID: ' || o_lead_id;
    EXCEPTION
        WHEN OTHERS THEN
            o_status := 'ERROR';
            o_message := 'Failed to create lead: ' || SQLERRM;
    END CREATE_LEAD;

    PROCEDURE CONVERT_LEAD(
        p_lead_id            IN  NUMBER,
        p_opportunity_name   IN  VARCHAR2,
        p_deal_value         IN  NUMBER,
        p_expected_close     IN  DATE,
        p_converted_by       IN  NUMBER,
        o_customer_id        OUT NUMBER,
        o_opportunity_id     OUT NUMBER,
        o_status             OUT VARCHAR2,
        o_message            OUT VARCHAR2
    ) IS
        v_lead LEADS%ROWTYPE;
        v_cust_code VARCHAR2(30);
        v_cust_status VARCHAR2(20);
        v_cust_msg VARCHAR2(255);
    BEGIN
        SELECT * INTO v_lead FROM LEADS WHERE lead_id = p_lead_id;

        IF v_lead.lead_status = 'CONVERTED' THEN
            o_status := 'ERROR';
            o_message := 'Lead has already been converted.';
            RETURN;
        END IF;

        -- 1. Create Customer via CRM_CUSTOMER_PKG
        CRM_CUSTOMER_PKG.CREATE_CUSTOMER(
            p_name                 => v_lead.first_name || ' ' || v_lead.last_name,
            p_email                => v_lead.email,
            p_phone                => v_lead.phone,
            p_company              => v_lead.company,
            p_billing_address      => 'Pending address verification',
            p_shipping_address     => 'Pending address verification',
            p_category             => 'STANDARD',
            p_assigned_employee_id => v_lead.assigned_to,
            p_notes                => 'Converted from Lead ID ' || p_lead_id || '. Notes: ' || v_lead.qualification_notes,
            p_created_by           => p_converted_by,
            o_customer_id          => o_customer_id,
            o_customer_code        => v_cust_code,
            o_status               => v_cust_status,
            o_message              => v_cust_msg
        );

        IF v_cust_status <> 'SUCCESS' THEN
            o_status := 'ERROR';
            o_message := 'Customer conversion failed: ' || v_cust_msg;
            RETURN;
        END IF;

        -- 2. Create Initial Opportunity
        INSERT INTO OPPORTUNITIES (
            name, customer_id, lead_id, stage, deal_value, probability,
            expected_close_date, assigned_to, next_follow_up, created_at, updated_at
        ) VALUES (
            NVL(p_opportunity_name, 'Deal - ' || v_lead.company),
            o_customer_id, p_lead_id, 'QUALIFIED',
            NVL(p_deal_value, NVL(v_lead.estimated_budget, 1000)), 25,
            NVL(p_expected_close, SYSDATE + 30), v_lead.assigned_to,
            CURRENT_TIMESTAMP + INTERVAL '3' DAY, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        ) RETURNING opportunity_id INTO o_opportunity_id;

        -- 3. Record Opportunity History
        INSERT INTO OPPORTUNITY_HISTORY (
            opportunity_id, from_stage, to_stage, deal_value, probability, notes, changed_by
        ) VALUES (
            o_opportunity_id, 'NEW_LEAD', 'QUALIFIED', NVL(p_deal_value, v_lead.estimated_budget), 25,
            'Auto-created on lead conversion', p_converted_by
        );

        -- 4. Mark Lead Converted
        UPDATE LEADS
        SET lead_status = 'CONVERTED',
            converted_customer_id = o_customer_id,
            updated_at = CURRENT_TIMESTAMP
        WHERE lead_id = p_lead_id;

        -- 5. Mark Queue Item Completed
        UPDATE QUEUE_ASSIGNMENTS
        SET status = 'COMPLETED', updated_at = CURRENT_TIMESTAMP
        WHERE entity_type = 'LEAD' AND entity_id = p_lead_id;

        o_status := 'SUCCESS';
        o_message := 'Lead successfully converted to Customer ID: ' || o_customer_id || ' and Opportunity ID: ' || o_opportunity_id;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            o_status := 'ERROR';
            o_message := 'Lead not found for ID: ' || p_lead_id;
        WHEN OTHERS THEN
            o_status := 'ERROR';
            o_message := 'Lead conversion error: ' || SQLERRM;
    END CONVERT_LEAD;

    PROCEDURE UPDATE_OPPORTUNITY_STAGE(
        p_opportunity_id IN  NUMBER,
        p_new_stage      IN  VARCHAR2,
        p_deal_value     IN  NUMBER,
        p_probability    IN  NUMBER,
        p_lost_reason    IN  VARCHAR2,
        p_notes          IN  VARCHAR2,
        p_changed_by     IN  NUMBER,
        o_status         OUT VARCHAR2,
        o_message        OUT VARCHAR2
    ) IS
        v_current OPPORTUNITIES%ROWTYPE;
        v_calc_prob NUMBER(5,2);
    BEGIN
        SELECT * INTO v_current FROM OPPORTUNITIES WHERE opportunity_id = p_opportunity_id;

        -- Calculate default stage probabilities
        IF p_probability IS NOT NULL THEN
            v_calc_prob := p_probability;
        ELSE
            CASE p_new_stage
                WHEN 'NEW_LEAD' THEN v_calc_prob := 10;
                WHEN 'QUALIFIED' THEN v_calc_prob := 25;
                WHEN 'CONTACTED' THEN v_calc_prob := 40;
                WHEN 'PROPOSAL' THEN v_calc_prob := 60;
                WHEN 'NEGOTIATION' THEN v_calc_prob := 80;
                WHEN 'WON' THEN v_calc_prob := 100;
                WHEN 'LOST' THEN v_calc_prob := 0;
                ELSE v_calc_prob := v_current.probability;
            END CASE;
        END IF;

        UPDATE OPPORTUNITIES
        SET stage = p_new_stage,
            deal_value = NVL(p_deal_value, deal_value),
            probability = v_calc_prob,
            lost_reason = CASE WHEN p_new_stage = 'LOST' THEN p_lost_reason ELSE NULL END,
            updated_at = CURRENT_TIMESTAMP
        WHERE opportunity_id = p_opportunity_id;

        -- Record History
        INSERT INTO OPPORTUNITY_HISTORY (
            opportunity_id, from_stage, to_stage, deal_value, probability, notes, changed_by
        ) VALUES (
            p_opportunity_id, v_current.stage, p_new_stage,
            NVL(p_deal_value, v_current.deal_value), v_calc_prob, p_notes, p_changed_by
        );

        o_status := 'SUCCESS';
        o_message := 'Stage transitioned from ' || v_current.stage || ' to ' || p_new_stage;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            o_status := 'ERROR';
            o_message := 'Opportunity ID ' || p_opportunity_id || ' not found.';
        WHEN OTHERS THEN
            o_status := 'ERROR';
            o_message := 'Stage update failed: ' || SQLERRM;
    END UPDATE_OPPORTUNITY_STAGE;

END CRM_SALES_PKG;
/

-- ============================================================================
-- PACKAGE 3: CRM_INVENTORY_PKG
-- ============================================================================
CREATE OR REPLACE PACKAGE CRM_INVENTORY_PKG AS
    -- Register product and set initial inventory
    PROCEDURE CREATE_PRODUCT(
        p_sku             IN  VARCHAR2,
        p_name            IN  VARCHAR2,
        p_description     IN  VARCHAR2,
        p_category        IN  VARCHAR2,
        p_cost_price      IN  NUMBER,
        p_min_price       IN  NUMBER,
        p_max_price       IN  NUMBER,
        p_current_price   IN  NUMBER,
        p_initial_stock   IN  NUMBER,
        p_reorder_thresh  IN  NUMBER,
        p_created_by      IN  NUMBER,
        o_product_id      OUT NUMBER,
        o_status          OUT VARCHAR2,
        o_message         OUT VARCHAR2
    );

    -- Stock In (Purchase receipt)
    PROCEDURE RECEIVE_STOCK(
        p_product_id     IN  NUMBER,
        p_quantity       IN  NUMBER,
        p_reason         IN  VARCHAR2,
        p_performed_by   IN  NUMBER,
        o_new_stock      OUT NUMBER,
        o_status         OUT VARCHAR2,
        o_message        OUT VARCHAR2
    );

    -- Atomic stock reservation with SELECT FOR UPDATE to prevent negative inventory
    PROCEDURE RESERVE_STOCK(
        p_product_id     IN  NUMBER,
        p_quantity       IN  NUMBER,
        p_order_id       IN  NUMBER,
        p_performed_by   IN  NUMBER,
        o_status         OUT VARCHAR2,
        o_message        OUT VARCHAR2
    );

    -- Release reserved stock (e.g., cancelled order)
    PROCEDURE RELEASE_STOCK(
        p_product_id     IN  NUMBER,
        p_quantity       IN  NUMBER,
        p_order_id       IN  NUMBER,
        p_performed_by   IN  NUMBER,
        o_status         OUT VARCHAR2,
        o_message        OUT VARCHAR2
    );

    -- Adjust inventory with mandatory audit reason
    PROCEDURE ADJUST_INVENTORY(
        p_product_id     IN  NUMBER,
        p_new_stock      IN  NUMBER,
        p_reason         IN  VARCHAR2,
        p_performed_by   IN  NUMBER,
        o_status         OUT VARCHAR2,
        o_message        OUT VARCHAR2
    );
END CRM_INVENTORY_PKG;
/

CREATE OR REPLACE PACKAGE BODY CRM_INVENTORY_PKG AS

    PROCEDURE CREATE_PRODUCT(
        p_sku             IN  VARCHAR2,
        p_name            IN  VARCHAR2,
        p_description     IN  VARCHAR2,
        p_category        IN  VARCHAR2,
        p_cost_price      IN  NUMBER,
        p_min_price       IN  NUMBER,
        p_max_price       IN  NUMBER,
        p_current_price   IN  NUMBER,
        p_initial_stock   IN  NUMBER,
        p_reorder_thresh  IN  NUMBER,
        p_created_by      IN  NUMBER,
        o_product_id      OUT NUMBER,
        o_status          OUT VARCHAR2,
        o_message         OUT VARCHAR2
    ) IS
    BEGIN
        -- Validations
        IF p_min_price < p_cost_price THEN
            o_status := 'ERROR';
            o_message := 'Minimum selling price cannot be lower than cost price.';
            RETURN;
        END IF;

        IF p_max_price < p_min_price THEN
            o_status := 'ERROR';
            o_message := 'Maximum selling price cannot be lower than minimum selling price.';
            RETURN;
        END IF;

        IF p_current_price < p_min_price OR p_current_price > p_max_price THEN
            o_status := 'ERROR';
            o_message := 'Current price must be between min and max price limits.';
            RETURN;
        END IF;

        INSERT INTO PRODUCTS (
            sku, name, description, category, cost_price,
            min_selling_price, max_selling_price, current_selling_price,
            status, created_at, updated_at
        ) VALUES (
            UPPER(TRIM(p_sku)), TRIM(p_name), p_description, p_category, p_cost_price,
            p_min_price, p_max_price, p_current_price,
            'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        ) RETURNING product_id INTO o_product_id;

        INSERT INTO INVENTORY (
            product_id, available_stock, reserved_stock, reorder_threshold, updated_at
        ) VALUES (
            o_product_id, NVL(p_initial_stock, 0), 0, NVL(p_reorder_thresh, 10), CURRENT_TIMESTAMP
        );

        IF NVL(p_initial_stock, 0) > 0 THEN
            INSERT INTO INVENTORY_MOVEMENTS (
                product_id, movement_type, quantity, reason, reference_type, performed_by
            ) VALUES (
                o_product_id, 'STOCK_IN', p_initial_stock, 'Initial Opening Stock', 'MANUAL_AUDIT', p_created_by
            );
        END IF;

        o_status := 'SUCCESS';
        o_message := 'Product created successfully with SKU: ' || UPPER(TRIM(p_sku));
    EXCEPTION
        WHEN DUP_VAL_ON_INDEX THEN
            o_status := 'ERROR';
            o_message := 'Product with SKU ' || p_sku || ' already exists.';
        WHEN OTHERS THEN
            o_status := 'ERROR';
            o_message := 'Failed to create product: ' || SQLERRM;
    END CREATE_PRODUCT;

    PROCEDURE RECEIVE_STOCK(
        p_product_id     IN  NUMBER,
        p_quantity       IN  NUMBER,
        p_reason         IN  VARCHAR2,
        p_performed_by   IN  NUMBER,
        o_new_stock      OUT NUMBER,
        o_status         OUT VARCHAR2,
        o_message        OUT VARCHAR2
    ) IS
    BEGIN
        IF p_quantity <= 0 THEN
            o_status := 'ERROR';
            o_message := 'Quantity received must be greater than zero.';
            RETURN;
        END IF;

        UPDATE INVENTORY
        SET available_stock = available_stock + p_quantity,
            updated_at = CURRENT_TIMESTAMP
        WHERE product_id = p_product_id
        RETURNING available_stock INTO o_new_stock;

        INSERT INTO INVENTORY_MOVEMENTS (
            product_id, movement_type, quantity, reason, reference_type, performed_by
        ) VALUES (
            p_product_id, 'STOCK_IN', p_quantity, NVL(p_reason, 'Stock replenishment'), 'PURCHASE_RECEIPT', p_performed_by
        );

        o_status := 'SUCCESS';
        o_message := 'Stock received. Available balance: ' || o_new_stock;
    EXCEPTION
        WHEN OTHERS THEN
            o_status := 'ERROR';
            o_message := 'Stock receive failed: ' || SQLERRM;
    END RECEIVE_STOCK;

    PROCEDURE RESERVE_STOCK(
        p_product_id     IN  NUMBER,
        p_quantity       IN  NUMBER,
        p_order_id       IN  NUMBER,
        p_performed_by   IN  NUMBER,
        o_status         OUT VARCHAR2,
        o_message        OUT VARCHAR2
    ) IS
        v_avail NUMBER;
        v_reserved NUMBER;
    BEGIN
        IF p_quantity <= 0 THEN
            o_status := 'ERROR';
            o_message := 'Reservation quantity must be positive.';
            RETURN;
        END IF;

        -- ATOMIC LOCK: Lock row for update
        SELECT available_stock, reserved_stock
        INTO v_avail, v_reserved
        FROM INVENTORY
        WHERE product_id = p_product_id
        FOR UPDATE;

        IF v_avail < p_quantity THEN
            o_status := 'ERROR';
            o_message := 'Insufficient stock. Requested: ' || p_quantity || ', Available: ' || v_avail;
            RETURN;
        END IF;

        UPDATE INVENTORY
        SET available_stock = available_stock - p_quantity,
            reserved_stock = reserved_stock + p_quantity,
            updated_at = CURRENT_TIMESTAMP
        WHERE product_id = p_product_id;

        INSERT INTO INVENTORY_MOVEMENTS (
            product_id, movement_type, quantity, reason, reference_type, reference_id, performed_by
        ) VALUES (
            p_product_id, 'RESERVE', p_quantity, 'Stock reserved for Order #' || p_order_id, 'ORDER', p_order_id, p_performed_by
        );

        o_status := 'SUCCESS';
        o_message := 'Successfully reserved ' || p_quantity || ' units.';
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            o_status := 'ERROR';
            o_message := 'Product ID ' || p_product_id || ' has no inventory entry.';
        WHEN OTHERS THEN
            o_status := 'ERROR';
            o_message := 'Stock reservation failed: ' || SQLERRM;
    END RESERVE_STOCK;

    PROCEDURE RELEASE_STOCK(
        p_product_id     IN  NUMBER,
        p_quantity       IN  NUMBER,
        p_order_id       IN  NUMBER,
        p_performed_by   IN  NUMBER,
        o_status         OUT VARCHAR2,
        o_message        OUT VARCHAR2
    ) IS
        v_reserved NUMBER;
    BEGIN
        SELECT reserved_stock INTO v_reserved
        FROM INVENTORY
        WHERE product_id = p_product_id
        FOR UPDATE;

        IF v_reserved < p_quantity THEN
            -- release whatever was reserved
            UPDATE INVENTORY
            SET available_stock = available_stock + v_reserved,
                reserved_stock = 0,
                updated_at = CURRENT_TIMESTAMP
            WHERE product_id = p_product_id;
        ELSE
            UPDATE INVENTORY
            SET available_stock = available_stock + p_quantity,
                reserved_stock = reserved_stock - p_quantity,
                updated_at = CURRENT_TIMESTAMP
            WHERE product_id = p_product_id;
        END IF;

        INSERT INTO INVENTORY_MOVEMENTS (
            product_id, movement_type, quantity, reason, reference_type, reference_id, performed_by
        ) VALUES (
            p_product_id, 'RELEASE', p_quantity, 'Stock released from Order #' || p_order_id, 'ORDER', p_order_id, p_performed_by
        );

        o_status := 'SUCCESS';
        o_message := 'Stock released successfully.';
    EXCEPTION
        WHEN OTHERS THEN
            o_status := 'ERROR';
            o_message := 'Failed to release stock: ' || SQLERRM;
    END RELEASE_STOCK;

    PROCEDURE ADJUST_INVENTORY(
        p_product_id     IN  NUMBER,
        p_new_stock      IN  NUMBER,
        p_reason         IN  VARCHAR2,
        p_performed_by   IN  NUMBER,
        o_status         OUT VARCHAR2,
        o_message        OUT VARCHAR2
    ) IS
        v_current_stock NUMBER;
        v_diff NUMBER;
    BEGIN
        IF p_reason IS NULL OR LENGTH(TRIM(p_reason)) < 5 THEN
            o_status := 'ERROR';
            o_message := 'Mandatory adjustment reason (minimum 5 characters) is required.';
            RETURN;
        END IF;

        IF p_new_stock < 0 THEN
            o_status := 'ERROR';
            o_message := 'Stock count cannot be negative.';
            RETURN;
        END IF;

        SELECT available_stock INTO v_current_stock
        FROM INVENTORY
        WHERE product_id = p_product_id
        FOR UPDATE;

        v_diff := p_new_stock - v_current_stock;

        UPDATE INVENTORY
        SET available_stock = p_new_stock,
            updated_at = CURRENT_TIMESTAMP
        WHERE product_id = p_product_id;

        INSERT INTO INVENTORY_MOVEMENTS (
            product_id, movement_type, quantity, reason, reference_type, performed_by
        ) VALUES (
            p_product_id, 'ADJUSTMENT', v_diff, 'Audit Adjustment: ' || p_reason, 'MANUAL_AUDIT', p_performed_by
        );

        o_status := 'SUCCESS';
        o_message := 'Inventory adjusted. New stock: ' || p_new_stock || ' (delta: ' || v_diff || ')';
    EXCEPTION
        WHEN OTHERS THEN
            o_status := 'ERROR';
            o_message := 'Adjustment error: ' || SQLERRM;
    END ADJUST_INVENTORY;

END CRM_INVENTORY_PKG;
/

-- ============================================================================
-- PACKAGE 4: CRM_ORDER_PKG
-- ============================================================================
CREATE OR REPLACE PACKAGE CRM_ORDER_PKG AS
    -- Initialize an Order
    PROCEDURE CREATE_ORDER(
        p_customer_id        IN  NUMBER,
        p_sales_rep_id       IN  NUMBER,
        p_discount_amount    IN  NUMBER,
        p_tax_pct            IN  NUMBER,
        p_notes              IN  VARCHAR2,
        o_order_id           OUT NUMBER,
        o_order_number       OUT VARCHAR2,
        o_status             OUT VARCHAR2,
        o_message            OUT VARCHAR2
    );

    -- Add line item to order
    PROCEDURE ADD_ORDER_ITEM(
        p_order_id     IN  NUMBER,
        p_product_id   IN  NUMBER,
        p_quantity     IN  NUMBER,
        p_unit_price   IN  NUMBER,
        o_item_id      OUT NUMBER,
        o_status       OUT VARCHAR2,
        o_message      OUT VARCHAR2
    );

    -- Confirm order: reserve stock, generate invoice, push to shipping queue
    PROCEDURE CONFIRM_ORDER(
        p_order_id       IN  NUMBER,
        p_confirmed_by   IN  NUMBER,
        o_invoice_id     OUT NUMBER,
        o_invoice_number OUT VARCHAR2,
        o_status         OUT VARCHAR2,
        o_message        OUT VARCHAR2
    );

    -- Cancel order: release stock, update order status
    PROCEDURE CANCEL_ORDER(
        p_order_id       IN  NUMBER,
        p_reason         IN  VARCHAR2,
        p_cancelled_by   IN  NUMBER,
        o_status         OUT VARCHAR2,
        o_message        OUT VARCHAR2
    );
END CRM_ORDER_PKG;
/

-- Forward declaration of FINANCE PKG for circular dependency prevention
CREATE OR REPLACE PACKAGE CRM_FINANCE_PKG AS
    PROCEDURE GENERATE_INVOICE(
        p_order_id       IN  NUMBER,
        p_due_days       IN  NUMBER,
        o_invoice_id     OUT NUMBER,
        o_invoice_number OUT VARCHAR2,
        o_status         OUT VARCHAR2,
        o_message        OUT VARCHAR2
    );

    PROCEDURE RECORD_PAYMENT(
        p_invoice_id     IN  NUMBER,
        p_amount         IN  NUMBER,
        p_payment_method IN  VARCHAR2,
        p_payment_ref    IN  VARCHAR2,
        p_idempotency_key IN VARCHAR2,
        p_notes          IN  VARCHAR2,
        p_recorded_by    IN  NUMBER,
        o_payment_id     OUT NUMBER,
        o_remaining_bal  OUT NUMBER,
        o_status         OUT VARCHAR2,
        o_message        OUT VARCHAR2
    );

    PROCEDURE CALCULATE_COMMISSION(
        p_order_id           IN  NUMBER,
        p_invoice_id         IN  NUMBER,
        o_commission_id      OUT NUMBER,
        o_commission_amount  OUT NUMBER,
        o_status             OUT VARCHAR2,
        o_message            OUT VARCHAR2
    );
END CRM_FINANCE_PKG;
/

CREATE OR REPLACE PACKAGE BODY CRM_ORDER_PKG AS

    PROCEDURE CREATE_ORDER(
        p_customer_id        IN  NUMBER,
        p_sales_rep_id       IN  NUMBER,
        p_discount_amount    IN  NUMBER,
        p_tax_pct            IN  NUMBER,
        p_notes              IN  VARCHAR2,
        o_order_id           OUT NUMBER,
        o_order_number       OUT VARCHAR2,
        o_status             OUT VARCHAR2,
        o_message            OUT VARCHAR2
    ) IS
        v_order_num VARCHAR2(50);
    BEGIN
        SELECT 'ORD-' || TO_CHAR(SYSDATE, 'YYYYMMDD') || '-' || LPAD(NVL(MAX(order_id), 0) + 1, 4, '0')
        INTO v_order_num
        FROM ORDERS;

        INSERT INTO ORDERS (
            order_number, customer_id, order_date, subtotal, discount_amount,
            tax_amount, total_amount, order_status, assigned_sales_rep, notes,
            created_at, updated_at
        ) VALUES (
            v_order_num, p_customer_id, CURRENT_TIMESTAMP, 0, NVL(p_discount_amount, 0),
            0, 0, 'DRAFT', p_sales_rep_id, p_notes,
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        ) RETURNING order_id INTO o_order_id;

        o_order_number := v_order_num;
        o_status := 'SUCCESS';
        o_message := 'Draft order created with number: ' || v_order_num;
    EXCEPTION
        WHEN OTHERS THEN
            o_status := 'ERROR';
            o_message := 'Failed to create order: ' || SQLERRM;
    END CREATE_ORDER;

    PROCEDURE ADD_ORDER_ITEM(
        p_order_id     IN  NUMBER,
        p_product_id   IN  NUMBER,
        p_quantity     IN  NUMBER,
        p_unit_price   IN  NUMBER,
        o_item_id      OUT NUMBER,
        o_status       OUT VARCHAR2,
        o_message      OUT VARCHAR2
    ) IS
        v_prod PRODUCTS%ROWTYPE;
        v_price NUMBER(12,2);
        v_line_total NUMBER(12,2);
        v_subtotal NUMBER(12,2);
        v_tax NUMBER(12,2);
    BEGIN
        SELECT * INTO v_prod FROM PRODUCTS WHERE product_id = p_product_id;

        -- Validate price
        v_price := NVL(p_unit_price, v_prod.current_selling_price);
        IF v_price < v_prod.min_selling_price OR v_price > v_prod.max_selling_price THEN
            o_status := 'ERROR';
            o_message := 'Unit price (' || v_price || ') violates product limits [' || v_prod.min_selling_price || ' - ' || v_prod.max_selling_price || ']';
            RETURN;
        END IF;

        IF p_quantity <= 0 THEN
            o_status := 'ERROR';
            o_message := 'Quantity must be greater than zero.';
            RETURN;
        END IF;

        v_line_total := v_price * p_quantity;

        INSERT INTO ORDER_ITEMS (
            order_id, product_id, quantity, unit_price, discount, line_total
        ) VALUES (
            p_order_id, p_product_id, p_quantity, v_price, 0, v_line_total
        ) RETURNING item_id INTO o_item_id;

        -- Recalculate Order Subtotal, Tax (18%), and Total
        SELECT NVL(SUM(line_total), 0) INTO v_subtotal FROM ORDER_ITEMS WHERE order_id = p_order_id;
        v_tax := ROUND(v_subtotal * 0.18, 2); -- Standard 18% GST/VAT

        UPDATE ORDERS
        SET subtotal = v_subtotal,
            tax_amount = v_tax,
            total_amount = (v_subtotal + v_tax) - discount_amount,
            updated_at = CURRENT_TIMESTAMP
        WHERE order_id = p_order_id;

        o_status := 'SUCCESS';
        o_message := 'Item added to order.';
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            o_status := 'ERROR';
            o_message := 'Product ID ' || p_product_id || ' not found.';
        WHEN OTHERS THEN
            o_status := 'ERROR';
            o_message := 'Failed to add line item: ' || SQLERRM;
    END ADD_ORDER_ITEM;

    PROCEDURE CONFIRM_ORDER(
        p_order_id       IN  NUMBER,
        p_confirmed_by   IN  NUMBER,
        o_invoice_id     OUT NUMBER,
        o_invoice_number OUT VARCHAR2,
        o_status         OUT VARCHAR2,
        o_message        OUT VARCHAR2
    ) IS
        v_order ORDERS%ROWTYPE;
        v_res_status VARCHAR2(20);
        v_res_msg VARCHAR2(255);
        v_fin_status VARCHAR2(20);
        v_fin_msg VARCHAR2(255);
        v_shipping_queue_id NUMBER;
    BEGIN
        SELECT * INTO v_order FROM ORDERS WHERE order_id = p_order_id;

        IF v_order.order_status <> 'DRAFT' THEN
            o_status := 'ERROR';
            o_message := 'Order is already ' || v_order.order_status || '. Cannot re-confirm.';
            RETURN;
        END IF;

        -- 1. Reserve Stock for all items
        FOR item_rec IN (SELECT product_id, quantity FROM ORDER_ITEMS WHERE order_id = p_order_id) LOOP
            CRM_INVENTORY_PKG.RESERVE_STOCK(
                p_product_id   => item_rec.product_id,
                p_quantity     => item_rec.quantity,
                p_order_id     => p_order_id,
                p_performed_by => p_confirmed_by,
                o_status       => v_res_status,
                o_message      => v_res_msg
            );

            IF v_res_status <> 'SUCCESS' THEN
                o_status := 'ERROR';
                o_message := 'Stock reservation failed: ' || v_res_msg;
                RETURN;
            END IF;
        END LOOP;

        -- 2. Update Order Status to CONFIRMED
        UPDATE ORDERS
        SET order_status = 'CONFIRMED', updated_at = CURRENT_TIMESTAMP
        WHERE order_id = p_order_id;

        -- 3. Automatically Generate Invoice via CRM_FINANCE_PKG
        CRM_FINANCE_PKG.GENERATE_INVOICE(
            p_order_id       => p_order_id,
            p_due_days       => 30,
            o_invoice_id     => o_invoice_id,
            o_invoice_number => o_invoice_number,
            o_status         => v_fin_status,
            o_message        => v_fin_msg
        );

        -- 4. Push to Shipping Queue
        BEGIN
            SELECT queue_id INTO v_shipping_queue_id FROM DEPARTMENT_QUEUES WHERE queue_name = 'SHIPPING' AND ROWNUM = 1;
            INSERT INTO QUEUE_ASSIGNMENTS (
                queue_id, title, entity_type, entity_id, priority, status, created_by
            ) VALUES (
                v_shipping_queue_id, 'Shipment Packing for Order #' || v_order.order_number,
                'ORDER', p_order_id, 'HIGH', 'OPEN', p_confirmed_by
            );
        EXCEPTION
            WHEN OTHERS THEN NULL;
        END;

        o_status := 'SUCCESS';
        o_message := 'Order confirmed, stock reserved, invoice generated: ' || o_invoice_number;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            o_status := 'ERROR';
            o_message := 'Order ID ' || p_order_id || ' not found.';
        WHEN OTHERS THEN
            o_status := 'ERROR';
            o_message := 'Confirmation error: ' || SQLERRM;
    END CONFIRM_ORDER;

    PROCEDURE CANCEL_ORDER(
        p_order_id       IN  NUMBER,
        p_reason         IN  VARCHAR2,
        p_cancelled_by   IN  NUMBER,
        o_status         OUT VARCHAR2,
        o_message        OUT VARCHAR2
    ) IS
        v_order ORDERS%ROWTYPE;
        v_rel_status VARCHAR2(20);
        v_rel_msg VARCHAR2(255);
    BEGIN
        SELECT * INTO v_order FROM ORDERS WHERE order_id = p_order_id;

        IF v_order.order_status IN ('SHIPPED', 'DELIVERED', 'CANCELLED') THEN
            o_status := 'ERROR';
            o_message := 'Cannot cancel order in status: ' || v_order.order_status;
            RETURN;
        END IF;

        -- If order was CONFIRMED or PROCESSING, release reserved stock
        IF v_order.order_status IN ('CONFIRMED', 'PROCESSING') THEN
            FOR item_rec IN (SELECT product_id, quantity FROM ORDER_ITEMS WHERE order_id = p_order_id) LOOP
                CRM_INVENTORY_PKG.RELEASE_STOCK(
                    p_product_id   => item_rec.product_id,
                    p_quantity     => item_rec.quantity,
                    p_order_id     => p_order_id,
                    p_performed_by => p_cancelled_by,
                    o_status       => v_rel_status,
                    o_message      => v_rel_msg
                );
            END LOOP;
        END IF;

        UPDATE ORDERS
        SET order_status = 'CANCELLED',
            notes = notes || ' [Cancelled: ' || p_reason || ']',
            updated_at = CURRENT_TIMESTAMP
        WHERE order_id = p_order_id;

        o_status := 'SUCCESS';
        o_message := 'Order cancelled successfully and stock released.';
    EXCEPTION
        WHEN OTHERS THEN
            o_status := 'ERROR';
            o_message := 'Cancel order failed: ' || SQLERRM;
    END CANCEL_ORDER;

END CRM_ORDER_PKG;
/

-- ============================================================================
-- PACKAGE 5: CRM_FINANCE_PKG BODY
-- ============================================================================
CREATE OR REPLACE PACKAGE BODY CRM_FINANCE_PKG AS

    PROCEDURE GENERATE_INVOICE(
        p_order_id       IN  NUMBER,
        p_due_days       IN  NUMBER,
        o_invoice_id     OUT NUMBER,
        o_invoice_number OUT VARCHAR2,
        o_status         OUT VARCHAR2,
        o_message        OUT VARCHAR2
    ) IS
        v_order ORDERS%ROWTYPE;
        v_inv_num VARCHAR2(50);
    BEGIN
        SELECT * INTO v_order FROM ORDERS WHERE order_id = p_order_id;

        -- Generate unique invoice number: INV-YYYY-XXXX
        SELECT 'INV-' || TO_CHAR(SYSDATE, 'YYYY') || '-' || LPAD(NVL(MAX(invoice_id), 0) + 1, 5, '0')
        INTO v_inv_num
        FROM INVOICES;

        INSERT INTO INVOICES (
            invoice_number, order_id, customer_id, issue_date, due_date,
            subtotal, tax_amount, discount_amount, total_amount, paid_amount,
            balance_amount, payment_status, notes, created_at
        ) VALUES (
            v_inv_num, p_order_id, v_order.customer_id, TRUNC(SYSDATE),
            TRUNC(SYSDATE) + NVL(p_due_days, 30),
            v_order.subtotal, v_order.tax_amount, v_order.discount_amount,
            v_order.total_amount, 0, v_order.total_amount, 'UNPAID',
            'Auto-generated from Order #' || v_order.order_number, CURRENT_TIMESTAMP
        ) RETURNING invoice_id INTO o_invoice_id;

        o_invoice_number := v_inv_num;

        -- Copy line items
        FOR item_rec IN (
            SELECT oi.*, p.name AS prod_name
            FROM ORDER_ITEMS oi
            JOIN PRODUCTS p ON oi.product_id = p.product_id
            WHERE oi.order_id = p_order_id
        ) LOOP
            INSERT INTO INVOICE_ITEMS (
                invoice_id, product_id, description, quantity, unit_price, tax_rate, line_total
            ) VALUES (
                o_invoice_id, item_rec.product_id, item_rec.prod_name,
                item_rec.quantity, item_rec.unit_price, 18.00, item_rec.line_total
            );
        END LOOP;

        o_status := 'SUCCESS';
        o_message := 'Invoice created: ' || v_inv_num;
    EXCEPTION
        WHEN DUP_VAL_ON_INDEX THEN
            o_status := 'ERROR';
            o_message := 'Invoice already exists for order ID ' || p_order_id;
        WHEN OTHERS THEN
            o_status := 'ERROR';
            o_message := 'Invoice generation failed: ' || SQLERRM;
    END GENERATE_INVOICE;

    PROCEDURE RECORD_PAYMENT(
        p_invoice_id      IN  NUMBER,
        p_amount          IN  NUMBER,
        p_payment_method  IN  VARCHAR2,
        p_payment_ref     IN  VARCHAR2,
        p_idempotency_key IN  VARCHAR2,
        p_notes           IN  VARCHAR2,
        p_recorded_by     IN  NUMBER,
        o_payment_id      OUT NUMBER,
        o_remaining_bal   OUT NUMBER,
        o_status          OUT VARCHAR2,
        o_message         OUT VARCHAR2
    ) IS
        v_inv INVOICES%ROWTYPE;
        v_existing_pay PAYMENTS%ROWTYPE;
        v_comm_id NUMBER;
        v_comm_amt NUMBER;
        v_comm_stat VARCHAR2(20);
        v_comm_msg VARCHAR2(255);
    BEGIN
        -- 1. Idempotency Check: prevent duplicate payment recording
        BEGIN
            SELECT * INTO v_existing_pay
            FROM PAYMENTS
            WHERE idempotency_key = p_idempotency_key;

            o_payment_id := v_existing_pay.payment_id;
            SELECT balance_amount INTO o_remaining_bal FROM INVOICES WHERE invoice_id = p_invoice_id;
            o_status := 'SUCCESS';
            o_message := 'Duplicate submission detected: Returned previously recorded payment ID ' || v_existing_pay.payment_id;
            RETURN;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN NULL;
        END;

        -- 2. Lock invoice row
        SELECT * INTO v_inv FROM INVOICES WHERE invoice_id = p_invoice_id FOR UPDATE;

        IF p_amount <= 0 THEN
            o_status := 'ERROR';
            o_message := 'Payment amount must be greater than zero.';
            RETURN;
        END IF;

        IF p_amount > v_inv.balance_amount THEN
            o_status := 'ERROR';
            o_message := 'Payment amount (' || p_amount || ') exceeds outstanding balance (' || v_inv.balance_amount || ').';
            RETURN;
        END IF;

        -- 3. Record Payment
        INSERT INTO PAYMENTS (
            payment_reference, idempotency_key, invoice_id, customer_id, amount,
            payment_method, payment_status, transaction_date, notes, recorded_by
        ) VALUES (
            p_payment_ref, p_idempotency_key, p_invoice_id, v_inv.customer_id, p_amount,
            p_payment_method, 'COMPLETED', CURRENT_TIMESTAMP, p_notes, p_recorded_by
        ) RETURNING payment_id INTO o_payment_id;

        -- 4. Update Invoice Balance & Status
        o_remaining_bal := v_inv.balance_amount - p_amount;

        UPDATE INVOICES
        SET paid_amount = paid_amount + p_amount,
            balance_amount = o_remaining_bal,
            payment_status = CASE 
                WHEN o_remaining_bal = 0 THEN 'PAID'
                ELSE 'PARTIALLY_PAID'
            END
        WHERE invoice_id = p_invoice_id;

        -- 5. Trigger Commission Calculation if Paid in Full
        IF o_remaining_bal = 0 THEN
            CALCULATE_COMMISSION(
                p_order_id          => v_inv.order_id,
                p_invoice_id        => p_invoice_id,
                o_commission_id     => v_comm_id,
                o_commission_amount => v_comm_amt,
                o_status            => v_comm_stat,
                o_message           => v_comm_msg
            );
        END IF;

        o_status := 'SUCCESS';
        o_message := 'Payment recorded. Remaining balance: $' || o_remaining_bal;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            o_status := 'ERROR';
            o_message := 'Invoice ID ' || p_invoice_id || ' not found.';
        WHEN OTHERS THEN
            o_status := 'ERROR';
            o_message := 'Failed to record payment: ' || SQLERRM;
    END RECORD_PAYMENT;

    PROCEDURE CALCULATE_COMMISSION(
        p_order_id           IN  NUMBER,
        p_invoice_id         IN  NUMBER,
        o_commission_id      OUT NUMBER,
        o_commission_amount  OUT NUMBER,
        o_status             OUT VARCHAR2,
        o_message            OUT VARCHAR2
    ) IS
        v_order ORDERS%ROWTYPE;
        v_rule COMMISSION_RULES%ROWTYPE;
        v_comm NUMBER(12,2);
    BEGIN
        SELECT * INTO v_order FROM ORDERS WHERE order_id = p_order_id;

        IF v_order.assigned_sales_rep IS NULL THEN
            o_status := 'SKIPPED';
            o_message := 'No sales rep assigned to order. Commission calculation skipped.';
            RETURN;
        END IF;

        -- Match tiered commission rule based on order subtotal
        BEGIN
            SELECT * INTO v_rule
            FROM COMMISSION_RULES
            WHERE is_active = 'Y'
              AND v_order.subtotal >= min_sales_threshold
              AND v_order.subtotal <= max_sales_threshold
              AND ROWNUM = 1;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                -- Default fallback: 5% flat
                v_rule.rule_id := NULL;
                v_rule.rule_version := 1;
                v_rule.commission_rate_pct := 5.00;
        END;

        v_comm := ROUND(v_order.subtotal * (v_rule.commission_rate_pct / 100.0), 2);
        o_commission_amount := v_comm;

        INSERT INTO COMMISSION_RECORDS (
            employee_id, order_id, invoice_id, rule_id, rule_version,
            sales_amount, commission_pct, calculated_commission, status,
            notes, created_at
        ) VALUES (
            v_order.assigned_sales_rep, p_order_id, p_invoice_id, v_rule.rule_id,
            v_rule.rule_version, v_order.subtotal, v_rule.commission_rate_pct,
            v_comm, 'PENDING', 'Auto-calculated upon invoice settlement', CURRENT_TIMESTAMP
        ) RETURNING record_id INTO o_commission_id;

        o_status := 'SUCCESS';
        o_message := 'Commission of $' || v_comm || ' (' || v_rule.commission_rate_pct || '%) logged as PENDING.';
    EXCEPTION
        WHEN OTHERS THEN
            o_status := 'ERROR';
            o_message := 'Commission calculation error: ' || SQLERRM;
    END CALCULATE_COMMISSION;

END CRM_FINANCE_PKG;
/

-- ============================================================================
-- PACKAGE 6: CRM_REPORTING_PKG
-- ============================================================================
CREATE OR REPLACE PACKAGE CRM_REPORTING_PKG AS
    -- Return high-level summary KPIs
    PROCEDURE GET_CRM_KPI_METRICS(
        o_total_customers    OUT NUMBER,
        o_new_leads          OUT NUMBER,
        o_active_opps        OUT NUMBER,
        o_pipeline_value     OUT NUMBER,
        o_won_revenue        OUT NUMBER,
        o_receivables_total  OUT NUMBER,
        o_pending_tasks      OUT NUMBER
    );
END CRM_REPORTING_PKG;
/

CREATE OR REPLACE PACKAGE BODY CRM_REPORTING_PKG AS

    PROCEDURE GET_CRM_KPI_METRICS(
        o_total_customers    OUT NUMBER,
        o_new_leads          OUT NUMBER,
        o_active_opps        OUT NUMBER,
        o_pipeline_value     OUT NUMBER,
        o_won_revenue        OUT NUMBER,
        o_receivables_total  OUT NUMBER,
        o_pending_tasks      OUT NUMBER
    ) IS
    BEGIN
        SELECT COUNT(*) INTO o_total_customers FROM CUSTOMERS WHERE status = 'ACTIVE';
        SELECT COUNT(*) INTO o_new_leads FROM LEADS WHERE lead_status IN ('NEW', 'CONTACTED', 'QUALIFIED');
        SELECT COUNT(*), NVL(SUM(deal_value), 0) INTO o_active_opps, o_pipeline_value 
        FROM OPPORTUNITIES WHERE stage NOT IN ('WON', 'LOST');
        SELECT NVL(SUM(deal_value), 0) INTO o_won_revenue FROM OPPORTUNITIES WHERE stage = 'WON';
        SELECT NVL(SUM(balance_amount), 0) INTO o_receivables_total FROM INVOICES WHERE payment_status <> 'PAID';
        SELECT COUNT(*) INTO o_pending_tasks FROM TASKS WHERE status IN ('PENDING', 'IN_PROGRESS');
    END GET_CRM_KPI_METRICS;

END CRM_REPORTING_PKG;
/

COMMIT;
