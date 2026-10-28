/**
 * Enterprise CRM: Master Application Logic
 * Integrates Spring Boot REST APIs and Oracle 26ai Cloud PL/SQL Workflows
 */

let currentUser = null;
let pipelineChart = null;
let revenueChart = null;
let allProductsCache = [];
let allCustomersCache = [];

// Initialize on DOM load
document.addEventListener('DOMContentLoaded', () => {
    initApp();
});

async function initApp() {
    await loadCurrentUser();
    setupNavigation();
    setupGlobalSearch();
    loadDashboard();
}

// ----------------------------------------------------------------------------
// Authentication & User Profile
// ----------------------------------------------------------------------------
async function loadCurrentUser() {
    try {
        const res = await fetch('/api/auth/me');
        const json = await res.json();
        if (json.success && json.data) {
            currentUser = json.data;
            updateUserUI();
        }
    } catch (e) {
        console.error('Failed to load current user', e);
    }
}

function updateUserUI() {
    if (!currentUser) return;
    document.getElementById('navUserName').innerText = currentUser.FULL_NAME || currentUser.USERNAME;
    document.getElementById('navUserDept').innerText = (currentUser.DEPARTMENT || 'Executive') + ' Dept';
    
    // Display Primary Role
    let primaryRole = 'ROLE_SUPER_ADMIN';
    if (currentUser.roles && currentUser.roles.length > 0) {
        primaryRole = currentUser.roles[0].ROLE_NAME;
        document.getElementById('navRolePill').innerText = currentUser.roles[0].DISPLAY_NAME;
    } else {
        document.getElementById('navRolePill').innerText = 'Super Administrator';
    }

    if (currentUser.unreadNotifications > 0) {
        const notifBadge = document.getElementById('notifBadge');
        if (notifBadge) {
            notifBadge.innerText = currentUser.unreadNotifications;
            notifBadge.classList.remove('d-none');
        }
    }
}

async function switchDemoRole(username) {
    try {
        showToast('Switching role...', 'info');
        const res = await fetch('/api/auth/demo-switch', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username })
        });
        const json = await res.json();
        if (json.success) {
            currentUser = json.data;
            updateUserUI();
            showToast(`Switched active profile to ${currentUser.FULL_NAME} (${currentUser.DEPARTMENT})`, 'success');
            // Refresh current view
            const activeNav = document.querySelector('.nav-link-custom.active');
            const view = activeNav ? activeNav.getAttribute('data-view') : 'dashboard';
            navigateToView(view);
        } else {
            showToast(json.message || 'Failed to switch role', 'danger');
        }
    } catch (e) {
        showToast('Error switching demo role: ' + e.message, 'danger');
    }
}

// ----------------------------------------------------------------------------
// Navigation Router
// ----------------------------------------------------------------------------
function setupNavigation() {
    document.querySelectorAll('.nav-link-custom').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const view = link.getAttribute('data-view');
            navigateToView(view);
        });
    });
}

function navigateToView(view) {
    document.querySelectorAll('.nav-link-custom').forEach(l => l.classList.remove('active'));
    const targetLink = document.querySelector(`.nav-link-custom[data-view="${view}"]`);
    if (targetLink) targetLink.classList.add('active');

    document.querySelectorAll('.view-container').forEach(v => v.classList.add('d-none'));
    const targetView = document.getElementById(`view-${view}`);
    if (targetView) targetView.classList.remove('d-none');

    // Route Actions
    switch (view) {
        case 'dashboard': loadDashboard(); break;
        case 'customers': loadCustomers(); break;
        case 'leads': loadLeads(); break;
        case 'pipeline': loadKanbanPipeline(); break;
        case 'tasks': loadTasks(); break;
        case 'products': loadProductsAndInventory(); break;
        case 'orders': loadOrders(); break;
        case 'invoices': loadInvoices(); break;
        case 'payments': loadPayments(); break;
        case 'commissions': loadCommissions(); break;
        case 'queues': loadQueues('SALES'); break;
        case 'reports': loadReports(); break;
        case 'audit': loadAuditLogs(); break;
    }
}

// ----------------------------------------------------------------------------
// 1. Dashboard View
// ----------------------------------------------------------------------------
async function loadDashboard() {
    try {
        const res = await fetch('/api/dashboard');
        const json = await res.json();
        if (!json.success || !json.data) return;

        const data = json.data;
        const kpis = data.kpis || {};

        document.getElementById('kpiTotalCustomers').innerText = kpis.totalCustomers || 0;
        document.getElementById('kpiNewLeads').innerText = kpis.newLeads || 0;
        document.getElementById('kpiActiveOpps').innerText = kpis.activeOpportunities || 0;
        document.getElementById('kpiPipelineValue').innerText = formatCurrency(kpis.pipelineValue || 0);
        document.getElementById('kpiWonRevenue').innerText = formatCurrency(kpis.wonRevenue || 0);
        document.getElementById('kpiReceivables').innerText = formatCurrency(kpis.receivablesTotal || 0);
        document.getElementById('kpiPendingTasks').innerText = kpis.pendingTasks || 0;

        // Render Donut Chart for Opportunity Stages
        renderStageDonutChart(data.stageDistribution || []);
        
        // Render Recent Activity
        renderRecentActivities(data.recentActivities || []);

        // Render Urgent Tasks
        renderUrgentTasks(data.urgentTasks || []);

        // Render Queue Status Pills
        renderQueueDashboard(data.queueStatus || []);
    } catch (e) {
        console.error('Failed to load dashboard', e);
    }
}

function renderStageDonutChart(stageDist) {
    const ctx = document.getElementById('pipelineStageChart');
    if (!ctx) return;

    const labels = stageDist.map(s => s.STAGE.replace('_', ' '));
    const counts = stageDist.map(s => s.COUNT);
    const colors = ['#3b82f6', '#10b981', '#f59e0b', '#8b5cf6', '#6366f1', '#ec4899', '#94a3b8'];

    if (pipelineChart) pipelineChart.destroy();

    pipelineChart = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: labels,
            datasets: [{
                data: counts,
                backgroundColor: colors,
                borderWidth: 2,
                borderColor: '#ffffff'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { position: 'bottom', labels: { boxWidth: 12, font: { size: 11 } } }
            },
            cutout: '70%'
        }
    });
}

function renderRecentActivities(activities) {
    const container = document.getElementById('recentActivityList');
    if (!container) return;
    if (activities.length === 0) {
        container.innerHTML = '<div class="text-muted text-center py-3">No recent activities recorded</div>';
        return;
    }

    container.innerHTML = activities.map(a => `
        <div class="d-flex align-items-start gap-3 py-2 border-bottom">
            <div class="rounded-circle bg-light d-flex align-items-center justify-content-center text-primary" style="width: 32px; height: 32px; flex-shrink: 0;">
                <i class="bi bi-clock-history"></i>
            </div>
            <div class="flex-grow-1">
                <div class="d-flex justify-content-between">
                    <span class="fw-semibold text-dark">${escapeHtml(a.SUMMARY)}</span>
                    <small class="text-muted">${formatDate(a.CREATED_AT)}</small>
                </div>
                <small class="text-secondary">${a.ENTITY_TYPE} #${a.ENTITY_ID} • by ${escapeHtml(a.PERFORMED_BY_NAME)}</small>
            </div>
        </div>
    `).join('');
}

function renderUrgentTasks(tasks) {
    const container = document.getElementById('urgentTasksList');
    if (!container) return;
    if (tasks.length === 0) {
        container.innerHTML = '<div class="text-muted text-center py-3">No pending urgent tasks!</div>';
        return;
    }

    container.innerHTML = tasks.map(t => `
        <div class="d-flex align-items-center justify-content-between py-2 border-bottom">
            <div>
                <div class="fw-semibold text-dark">${escapeHtml(t.TITLE)}</div>
                <small class="text-secondary">Assigned: ${escapeHtml(t.ASSIGNED_TO_NAME)} • Due: ${formatDate(t.DUE_DATE)}</small>
            </div>
            <span class="badge ${t.PRIORITY === 'URGENT' ? 'badge-urgent' : 'badge-pending'}">${t.PRIORITY}</span>
        </div>
    `).join('');
}

function renderQueueDashboard(queues) {
    const container = document.getElementById('dashboardQueueStatus');
    if (!container) return;
    container.innerHTML = queues.map(q => `
        <div class="d-flex align-items-center justify-content-between p-2 mb-2 rounded bg-light border">
            <span class="fw-medium">${escapeHtml(q.DISPLAY_TITLE)}</span>
            <span class="badge bg-primary rounded-pill">${q.PENDING_COUNT} Pending</span>
        </div>
    `).join('');
}

// ----------------------------------------------------------------------------
// 2. Customers View
// ----------------------------------------------------------------------------
async function loadCustomers() {
    const search = document.getElementById('customerSearchInput')?.value || '';
    const category = document.getElementById('customerCategoryFilter')?.value || '';
    const status = document.getElementById('customerStatusFilter')?.value || '';

    try {
        const url = `/api/customers?search=${encodeURIComponent(search)}&category=${encodeURIComponent(category)}&status=${encodeURIComponent(status)}`;
        const res = await fetch(url);
        const json = await res.json();
        if (!json.success) return;

        allCustomersCache = json.data || [];
        renderCustomerTable(allCustomersCache);
    } catch (e) {
        console.error('Failed to load customers', e);
    }
}

function renderCustomerTable(customers) {
    const tbody = document.getElementById('customersTableBody');
    if (!tbody) return;

    if (customers.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center py-4 text-muted">No customers found</td></tr>';
        return;
    }

    tbody.innerHTML = customers.map(c => `
        <tr style="cursor: pointer;" onclick="openCustomerDrawer(${c.CUSTOMER_ID})">
            <td><span class="fw-bold text-primary">${c.CUSTOMER_CODE}</span></td>
            <td>
                <div class="fw-semibold">${escapeHtml(c.NAME)}</div>
                <small class="text-muted">${escapeHtml(c.COMPANY || '-')}</small>
            </td>
            <td>${escapeHtml(c.EMAIL)}</td>
            <td>${escapeHtml(c.PHONE || '-')}</td>
            <td><span class="badge bg-light text-dark border">${c.CUSTOMER_CATEGORY}</span></td>
            <td>${escapeHtml(c.ASSIGNED_EMPLOYEE_NAME || 'Unassigned')}</td>
            <td><span class="badge-status badge-${(c.STATUS || '').toLowerCase()}">${c.STATUS}</span></td>
            <td class="text-end">
                <button class="btn btn-sm btn-outline-primary" onclick="event.stopPropagation(); openCustomerDrawer(${c.CUSTOMER_ID})">
                    <i class="bi bi-eye"></i> View 360
                </button>
            </td>
        </tr>
    `).join('');
}

async function openCustomerDrawer(customerId) {
    try {
        const res = await fetch(`/api/customers/${customerId}`);
        const json = await res.json();
        if (!json.success || !json.data) return;

        const c = json.data;
        document.getElementById('drawerCustCode').innerText = c.CUSTOMER_CODE;
        document.getElementById('drawerCustName').innerText = c.NAME;
        document.getElementById('drawerCustCompany').innerText = c.COMPANY || 'N/A';
        document.getElementById('drawerCustEmail').innerText = c.EMAIL;
        document.getElementById('drawerCustPhone').innerText = c.PHONE || 'N/A';
        document.getElementById('drawerCustCategory').innerText = c.CUSTOMER_CATEGORY;
        document.getElementById('drawerCustBilling').innerText = c.BILLING_ADDRESS || 'N/A';
        document.getElementById('drawerCustNotes').innerText = c.NOTES || 'No special notes recorded.';

        // Render Contacts
        const contactsContainer = document.getElementById('drawerContactsList');
        if (c.contacts && c.contacts.length > 0) {
            contactsContainer.innerHTML = c.contacts.map(ct => `
                <div class="p-2 border rounded mb-2 bg-light">
                    <div class="fw-semibold">${escapeHtml(ct.FIRST_NAME)} ${escapeHtml(ct.LAST_NAME || '')} ${ct.IS_PRIMARY === 'Y' ? '<span class="badge bg-primary">Primary</span>' : ''}</div>
                    <small class="text-muted">${escapeHtml(ct.DESIGNATION || 'Representative')} • ${escapeHtml(ct.EMAIL || '')} • ${escapeHtml(ct.PHONE || '')}</small>
                </div>
            `).join('');
        } else {
            contactsContainer.innerHTML = '<div class="text-muted small">No secondary contacts listed.</div>';
        }

        // Render Opps
        const oppsContainer = document.getElementById('drawerOppsList');
        if (c.opportunities && c.opportunities.length > 0) {
            oppsContainer.innerHTML = c.opportunities.map(o => `
                <div class="d-flex justify-content-between align-items-center p-2 border rounded mb-2">
                    <div>
                        <div class="fw-semibold">${escapeHtml(o.NAME)}</div>
                        <small class="text-muted">Stage: ${o.STAGE}</small>
                    </div>
                    <span class="fw-bold text-primary">${formatCurrency(o.DEAL_VALUE)}</span>
                </div>
            `).join('');
        } else {
            oppsContainer.innerHTML = '<div class="text-muted small">No deals currently linked.</div>';
        }

        // Render Invoices
        const invContainer = document.getElementById('drawerInvoicesList');
        if (c.invoices && c.invoices.length > 0) {
            invContainer.innerHTML = c.invoices.map(i => `
                <div class="d-flex justify-content-between align-items-center p-2 border rounded mb-2">
                    <div>
                        <span class="fw-bold">${i.INVOICE_NUMBER}</span>
                        <small class="d-block text-muted">Balance: ${formatCurrency(i.BALANCE_AMOUNT)}</small>
                    </div>
                    <span class="badge-status badge-${(i.PAYMENT_STATUS || '').toLowerCase().replace('_', '-')}">${i.PAYMENT_STATUS}</span>
                </div>
            `).join('');
        } else {
            invContainer.innerHTML = '<div class="text-muted small">No invoices billed yet.</div>';
        }

        const modal = new bootstrap.Modal(document.getElementById('customerDetailModal'));
        modal.show();
    } catch (e) {
        showToast('Failed to load customer profile: ' + e.message, 'danger');
    }
}

async function checkCustomerDuplicateRealtime() {
    const email = document.getElementById('newCustEmail')?.value || '';
    const phone = document.getElementById('newCustPhone')?.value || '';
    const alertBox = document.getElementById('custDuplicateAlert');

    if (!email && !phone) {
        alertBox.classList.add('d-none');
        return;
    }

    try {
        const res = await fetch(`/api/customers/check-duplicate?email=${encodeURIComponent(email)}&phone=${encodeURIComponent(phone)}`);
        const json = await res.json();
        if (json.success && json.data && json.data.isDuplicate) {
            alertBox.classList.remove('d-none');
            alertBox.innerHTML = `<i class="bi bi-exclamation-triangle-fill"></i> <strong>Warning:</strong> Duplicate customer detected! Matches ID #${json.data.duplicateCustomerId} (${escapeHtml(json.data.duplicateCustomerName)}).`;
        } else {
            alertBox.classList.add('d-none');
        }
    } catch (e) {
        // quiet fail
    }
}

async function saveNewCustomer() {
    const payload = {
        name: document.getElementById('newCustName').value,
        company: document.getElementById('newCustCompany').value,
        email: document.getElementById('newCustEmail').value,
        phone: document.getElementById('newCustPhone').value,
        category: document.getElementById('newCustCategory').value,
        billingAddress: document.getElementById('newCustBilling').value,
        shippingAddress: document.getElementById('newCustShipping').value,
        notes: document.getElementById('newCustNotes').value,
        assignedEmployeeId: currentUser ? currentUser.USER_ID : 1
    };

    if (!payload.name || !payload.email) {
        showToast('Please provide customer name and email.', 'warning');
        return;
    }

    try {
        const res = await fetch('/api/customers', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const json = await res.json();
        if (json.success) {
            showToast(json.message || 'Customer created successfully!', 'success');
            bootstrap.Modal.getInstance(document.getElementById('newCustomerModal')).hide();
            document.getElementById('newCustomerForm').reset();
            loadCustomers();
        } else {
            showToast(json.message || 'Error creating customer', 'danger');
        }
    } catch (e) {
        showToast('Server error: ' + e.message, 'danger');
    }
}

// ----------------------------------------------------------------------------
// 3. Leads & Lead Conversion
// ----------------------------------------------------------------------------
async function loadLeads() {
    const search = document.getElementById('leadSearchInput')?.value || '';
    const status = document.getElementById('leadStatusFilter')?.value || '';

    try {
        const res = await fetch(`/api/leads?search=${encodeURIComponent(search)}&status=${encodeURIComponent(status)}`);
        const json = await res.json();
        if (!json.success) return;

        const leads = json.data || [];
        const tbody = document.getElementById('leadsTableBody');
        if (!tbody) return;

        if (leads.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center py-4 text-muted">No leads registered</td></tr>';
            return;
        }

        tbody.innerHTML = leads.map(l => `
            <tr>
                <td><strong>#${l.LEAD_ID}</strong></td>
                <td>
                    <div class="fw-semibold">${escapeHtml(l.FIRST_NAME)} ${escapeHtml(l.LAST_NAME)}</div>
                    <small class="text-muted">${escapeHtml(l.COMPANY || 'Individual')}</small>
                </td>
                <td>${escapeHtml(l.EMAIL)}</td>
                <td>${escapeHtml(l.PHONE || '-')}</td>
                <td><span class="badge bg-light text-dark border">${l.LEAD_SOURCE}</span></td>
                <td><span class="badge-status badge-${(l.LEAD_STATUS || '').toLowerCase()}">${l.LEAD_STATUS}</span></td>
                <td>${formatCurrency(l.ESTIMATED_BUDGET || 0)}</td>
                <td class="text-end">
                    ${l.LEAD_STATUS !== 'CONVERTED' ? `
                        <button class="btn btn-sm btn-primary" onclick="openConvertLeadModal(${l.LEAD_ID}, '${escapeJs(l.COMPANY || l.FIRST_NAME + ' ' + l.LAST_NAME)}', ${l.ESTIMATED_BUDGET || 5000})">
                            <i class="bi bi-arrow-repeat"></i> Convert
                        </button>
                    ` : `<span class="badge bg-success">Converted</span>`}
                </td>
            </tr>
        `).join('');
    } catch (e) {
        console.error('Failed to load leads', e);
    }
}

async function saveNewLead() {
    const payload = {
        firstName: document.getElementById('newLeadFirstName').value,
        lastName: document.getElementById('newLeadLastName').value,
        company: document.getElementById('newLeadCompany').value,
        email: document.getElementById('newLeadEmail').value,
        phone: document.getElementById('newLeadPhone').value,
        leadSource: document.getElementById('newLeadSource').value,
        estimatedBudget: parseFloat(document.getElementById('newLeadBudget').value) || 0,
        notes: document.getElementById('newLeadNotes').value,
        assignedTo: currentUser ? currentUser.USER_ID : 4
    };

    if (!payload.firstName || !payload.email) {
        showToast('Please provide first name and email.', 'warning');
        return;
    }

    try {
        const res = await fetch('/api/leads', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const json = await res.json();
        if (json.success) {
            showToast(json.message || 'Lead created successfully!', 'success');
            bootstrap.Modal.getInstance(document.getElementById('newLeadModal')).hide();
            document.getElementById('newLeadForm').reset();
            loadLeads();
        } else {
            showToast(json.message || 'Error creating lead', 'danger');
        }
    } catch (e) {
        showToast('Error: ' + e.message, 'danger');
    }
}

function openConvertLeadModal(leadId, company, budget) {
    document.getElementById('convertLeadId').value = leadId;
    document.getElementById('convertDealName').value = company ? `Enterprise Deal - ${company}` : 'New Enterprise Opportunity';
    document.getElementById('convertDealValue').value = budget || 10000;
    const modal = new bootstrap.Modal(document.getElementById('convertLeadModal'));
    modal.show();
}

async function executeLeadConversion() {
    const leadId = document.getElementById('convertLeadId').value;
    const payload = {
        opportunityName: document.getElementById('convertDealName').value,
        dealValue: parseFloat(document.getElementById('convertDealValue').value) || 0
    };

    try {
        const res = await fetch(`/api/leads/${leadId}/convert`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const json = await res.json();
        if (json.success) {
            showToast(json.message || 'Lead successfully converted into Customer and Deal!', 'success');
            bootstrap.Modal.getInstance(document.getElementById('convertLeadModal')).hide();
            loadLeads();
        } else {
            showToast(json.message || 'Conversion failed', 'danger');
        }
    } catch (e) {
        showToast('Error: ' + e.message, 'danger');
    }
}

// ----------------------------------------------------------------------------
// 4. Sales Pipeline (Interactive Kanban Board)
// ----------------------------------------------------------------------------
async function loadKanbanPipeline() {
    try {
        const res = await fetch('/api/opportunities/kanban');
        const json = await res.json();
        if (!json.success || !json.data) return;

        const data = json.data;
        document.getElementById('pipelineTotalVal').innerText = formatCurrency(data.grandTotal || 0);
        document.getElementById('pipelineWeightedVal').innerText = formatCurrency(data.weightedForecastTotal || 0);

        const cols = data.columns || {};
        const stages = ['NEW_LEAD', 'QUALIFIED', 'CONTACTED', 'PROPOSAL', 'NEGOTIATION', 'WON', 'LOST'];

        stages.forEach(stg => {
            const colData = cols[stg] || { cards: [], count: 0, totalValue: 0 };
            const countEl = document.getElementById(`kanban-count-${stg}`);
            const valEl = document.getElementById(`kanban-val-${stg}`);
            const container = document.getElementById(`kanban-cards-${stg}`);

            if (countEl) countEl.innerText = colData.count;
            if (valEl) valEl.innerText = formatCurrency(colData.totalValue);

            if (container) {
                container.innerHTML = colData.cards.map(c => `
                    <div class="kanban-card" draggable="true" 
                         ondragstart="handleDragStart(event, ${c.OPPORTUNITY_ID}, '${c.STAGE}')"
                         ondragend="handleDragEnd(event)">
                        <div class="kanban-card-title">${escapeHtml(c.NAME)}</div>
                        <div class="kanban-card-company">${escapeHtml(c.CUSTOMER_NAME)}</div>
                        <div class="d-flex justify-content-between align-items-center mt-2">
                            <span class="kanban-card-val">${formatCurrency(c.DEAL_VALUE)}</span>
                            <span class="badge bg-light text-dark border small">${c.PROBABILITY}%</span>
                        </div>
                    </div>
                `).join('');
            }
        });

        // Setup dropzones
        stages.forEach(stg => {
            const container = document.getElementById(`kanban-cards-${stg}`);
            if (container) {
                container.ondragover = (e) => { e.preventDefault(); };
                container.ondrop = (e) => { handleDrop(e, stg); };
            }
        });
    } catch (e) {
        console.error('Failed to load Kanban pipeline', e);
    }
}

let draggedOpportunityId = null;
let draggedFromStage = null;

function handleDragStart(e, oppId, stage) {
    draggedOpportunityId = oppId;
    draggedFromStage = stage;
    e.target.classList.add('dragging');
}

function handleDragEnd(e) {
    e.target.classList.remove('dragging');
}

async function handleDrop(e, newStage) {
    e.preventDefault();
    if (!draggedOpportunityId || draggedFromStage === newStage) return;

    try {
        const res = await fetch(`/api/opportunities/${draggedOpportunityId}/stage`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                newStage: newStage,
                notes: `Moved from ${draggedFromStage} to ${newStage} via Kanban drag & drop`
            })
        });
        const json = await res.json();
        if (json.success) {
            showToast(`Deal moved to ${newStage.replace('_', ' ')}!`, 'success');
            loadKanbanPipeline();
        } else {
            showToast(json.message || 'Stage transition failed', 'danger');
        }
    } catch (err) {
        showToast('Error moving opportunity: ' + err.message, 'danger');
    }
}

// ----------------------------------------------------------------------------
// 5. Tasks & Follow-ups
// ----------------------------------------------------------------------------
async function loadTasks() {
    const status = document.getElementById('taskStatusFilter')?.value || '';
    const priority = document.getElementById('taskPriorityFilter')?.value || '';

    try {
        const res = await fetch(`/api/tasks?status=${encodeURIComponent(status)}&priority=${encodeURIComponent(priority)}`);
        const json = await res.json();
        if (!json.success) return;

        const tasks = json.data || [];
        const tbody = document.getElementById('tasksTableBody');
        if (!tbody) return;

        if (tasks.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-muted">No tasks found</td></tr>';
            return;
        }

        tbody.innerHTML = tasks.map(t => `
            <tr>
                <td><strong>#${t.TASK_ID}</strong></td>
                <td>
                    <div class="fw-semibold">${escapeHtml(t.TITLE)}</div>
                    <small class="text-muted">${escapeHtml(t.DESCRIPTION || '')}</small>
                </td>
                <td>${escapeHtml(t.CUSTOMER_NAME || 'Internal CRM')}</td>
                <td><span class="badge ${t.PRIORITY === 'URGENT' ? 'badge-urgent' : 'badge-pending'}">${t.PRIORITY}</span></td>
                <td>${formatDate(t.DUE_DATE)}</td>
                <td><span class="badge-status badge-${(t.STATUS || '').toLowerCase().replace('_', '-')}">${t.STATUS}</span></td>
                <td class="text-end">
                    ${t.STATUS !== 'COMPLETED' ? `
                        <button class="btn btn-sm btn-outline-success" onclick="updateTaskStatus(${t.TASK_ID}, 'COMPLETED')">
                            <i class="bi bi-check2"></i> Mark Done
                        </button>
                    ` : '<span class="text-success small fw-bold"><i class="bi bi-check-circle-fill"></i> Completed</span>'}
                </td>
            </tr>
        `).join('');
    } catch (e) {
        console.error('Failed to load tasks', e);
    }
}

async function updateTaskStatus(taskId, status) {
    try {
        const res = await fetch(`/api/tasks/${taskId}/status?status=${status}`, { method: 'PUT' });
        const json = await res.json();
        if (json.success) {
            showToast('Task updated!', 'success');
            loadTasks();
        }
    } catch (e) {
        showToast('Error updating task: ' + e.message, 'danger');
    }
}

async function saveNewTask() {
    const payload = {
        title: document.getElementById('newTaskTitle').value,
        description: document.getElementById('newTaskDescription').value,
        priority: document.getElementById('newTaskPriority').value,
        dueDate: document.getElementById('newTaskDueDate').value,
        customerId: document.getElementById('newTaskCustomer')?.value || null,
        assignedTo: currentUser ? currentUser.USER_ID : 1
    };

    if (!payload.title || !payload.dueDate) {
        showToast('Title and Due Date are mandatory.', 'warning');
        return;
    }

    try {
        const res = await fetch('/api/tasks', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const json = await res.json();
        if (json.success) {
            showToast('Task successfully scheduled!', 'success');
            bootstrap.Modal.getInstance(document.getElementById('newTaskModal')).hide();
            document.getElementById('newTaskForm').reset();
            loadTasks();
        } else {
            showToast(json.message || 'Error creating task', 'danger');
        }
    } catch (e) {
        showToast('Error: ' + e.message, 'danger');
    }
}

// ----------------------------------------------------------------------------
// 6. Products & Inventory
// ----------------------------------------------------------------------------
async function loadProductsAndInventory() {
    try {
        const res = await fetch('/api/inventory');
        const json = await res.json();
        if (!json.success) return;

        allProductsCache = json.data || [];
        const tbody = document.getElementById('productsTableBody');
        if (!tbody) return;

        if (allProductsCache.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center py-4 text-muted">No products catalogued</td></tr>';
            return;
        }

        tbody.innerHTML = allProductsCache.map(p => `
            <tr>
                <td><strong>${p.SKU}</strong></td>
                <td>
                    <div class="fw-semibold">${escapeHtml(p.NAME)}</div>
                    <small class="text-muted">${p.CATEGORY}</small>
                </td>
                <td>${formatCurrency(p.CURRENT_SELLING_PRICE)}</td>
                <td>
                    <span class="fw-bold ${p.AVAILABLE_STOCK <= p.REORDER_THRESHOLD ? 'text-danger' : 'text-success'}">
                        ${p.AVAILABLE_STOCK} units
                    </span>
                    <small class="text-muted d-block">Threshold: ${p.REORDER_THRESHOLD}</small>
                </td>
                <td>${p.RESERVED_STOCK || 0} units</td>
                <td><span class="badge ${p.STOCK_STATUS === 'LOW_STOCK' ? 'bg-danger' : 'bg-success'}">${p.STOCK_STATUS}</span></td>
                <td><small class="text-muted">${p.WAREHOUSE_LOCATION || 'MAIN-WAREHOUSE'}</small></td>
                <td class="text-end">
                    <button class="btn btn-sm btn-outline-primary" onclick="openReceiveStockModal(${p.PRODUCT_ID}, '${escapeJs(p.NAME)}')">
                        <i class="bi bi-box-arrow-in-down"></i> Receive
                    </button>
                    <button class="btn btn-sm btn-outline-secondary" onclick="openAdjustStockModal(${p.PRODUCT_ID}, '${escapeJs(p.NAME)}', ${p.AVAILABLE_STOCK})">
                        <i class="bi bi-sliders"></i> Adjust
                    </button>
                </td>
            </tr>
        `).join('');

        // Also load movements
        loadInventoryMovements();
    } catch (e) {
        console.error('Failed to load products/inventory', e);
    }
}

async function loadInventoryMovements() {
    try {
        const res = await fetch('/api/inventory/movements');
        const json = await res.json();
        if (!json.success) return;

        const movements = json.data || [];
        const tbody = document.getElementById('inventoryMovementsTableBody');
        if (!tbody) return;

        tbody.innerHTML = movements.map(m => `
            <tr>
                <td>${formatDate(m.CREATED_AT)}</td>
                <td><strong>${m.SKU}</strong> (${escapeHtml(m.PRODUCT_NAME)})</td>
                <td><span class="badge ${m.MOVEMENT_TYPE === 'STOCK_IN' ? 'bg-success' : (m.MOVEMENT_TYPE === 'RESERVE' ? 'bg-warning text-dark' : 'bg-secondary')}">${m.MOVEMENT_TYPE}</span></td>
                <td><strong>${m.QUANTITY}</strong></td>
                <td>${escapeHtml(m.REASON || '-')}</td>
                <td>${escapeHtml(m.PERFORMED_BY_NAME || 'System')}</td>
            </tr>
        `).join('');
    } catch (e) {
        console.error('Failed to load movements', e);
    }
}

function openReceiveStockModal(prodId, prodName) {
    document.getElementById('receiveStockProdId').value = prodId;
    document.getElementById('receiveStockProdName').innerText = prodName;
    const modal = new bootstrap.Modal(document.getElementById('receiveStockModal'));
    modal.show();
}

async function saveReceiveStock() {
    const prodId = document.getElementById('receiveStockProdId').value;
    const qty = parseInt(document.getElementById('receiveStockQty').value) || 0;
    const reason = document.getElementById('receiveStockReason').value;

    if (qty <= 0) {
        showToast('Please enter a valid quantity greater than zero.', 'warning');
        return;
    }

    try {
        const res = await fetch('/api/inventory/receive', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ productId: prodId, quantity: qty, reason: reason })
        });
        const json = await res.json();
        if (json.success) {
            showToast(json.message || 'Stock successfully received!', 'success');
            bootstrap.Modal.getInstance(document.getElementById('receiveStockModal')).hide();
            loadProductsAndInventory();
        } else {
            showToast(json.message || 'Error receiving stock', 'danger');
        }
    } catch (e) {
        showToast('Error: ' + e.message, 'danger');
    }
}

function openAdjustStockModal(prodId, prodName, currentStock) {
    document.getElementById('adjustStockProdId').value = prodId;
    document.getElementById('adjustStockProdName').innerText = prodName;
    document.getElementById('adjustStockNewCount').value = currentStock;
    const modal = new bootstrap.Modal(document.getElementById('adjustStockModal'));
    modal.show();
}

async function saveAdjustStock() {
    const prodId = document.getElementById('adjustStockProdId').value;
    const newStock = parseInt(document.getElementById('adjustStockNewCount').value);
    const reason = document.getElementById('adjustStockReason').value;

    if (isNaN(newStock) || newStock < 0 || !reason || reason.trim().length < 5) {
        showToast('Please enter a non-negative stock count and a mandatory reason (min 5 chars).', 'warning');
        return;
    }

    try {
        const res = await fetch('/api/inventory/adjust', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ productId: prodId, newStock: newStock, reason: reason })
        });
        const json = await res.json();
        if (json.success) {
            showToast(json.message || 'Inventory audit adjustment saved!', 'success');
            bootstrap.Modal.getInstance(document.getElementById('adjustStockModal')).hide();
            loadProductsAndInventory();
        } else {
            showToast(json.message || 'Adjustment error', 'danger');
        }
    } catch (e) {
        showToast('Error: ' + e.message, 'danger');
    }
}

// ----------------------------------------------------------------------------
// 7. Orders & Invoices Workflow
// ----------------------------------------------------------------------------
async function loadOrders() {
    const status = document.getElementById('orderStatusFilter')?.value || '';

    try {
        const res = await fetch(`/api/orders?status=${encodeURIComponent(status)}`);
        const json = await res.json();
        if (!json.success) return;

        const orders = json.data || [];
        const tbody = document.getElementById('ordersTableBody');
        if (!tbody) return;

        if (orders.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center py-4 text-muted">No orders found</td></tr>';
            return;
        }

        tbody.innerHTML = orders.map(o => `
            <tr>
                <td><strong>${o.ORDER_NUMBER}</strong></td>
                <td>${formatDate(o.ORDER_DATE)}</td>
                <td>
                    <div class="fw-semibold">${escapeHtml(o.CUSTOMER_NAME)}</div>
                    <small class="text-muted">${o.CUSTOMER_CODE}</small>
                </td>
                <td>${escapeHtml(o.SALES_REP_NAME || '-')}</td>
                <td>${o.ITEMS_COUNT} line items</td>
                <td><span class="fw-bold">${formatCurrency(o.TOTAL_AMOUNT)}</span></td>
                <td><span class="badge-status badge-${(o.ORDER_STATUS || '').toLowerCase()}">${o.ORDER_STATUS}</span></td>
                <td class="text-end">
                    ${o.ORDER_STATUS === 'DRAFT' ? `
                        <button class="btn btn-sm btn-success" onclick="confirmOrder(${o.ORDER_ID})">
                            <i class="bi bi-check-circle"></i> Confirm Order
                        </button>
                        <button class="btn btn-sm btn-outline-danger" onclick="cancelOrder(${o.ORDER_ID})">
                            <i class="bi bi-x-circle"></i> Cancel
                        </button>
                    ` : (o.INVOICE_NUMBER ? `
                        <button class="btn btn-sm btn-outline-primary" onclick="viewInvoiceByOrder(${o.ORDER_ID})">
                            <i class="bi bi-receipt"></i> Invoice #${o.INVOICE_NUMBER}
                        </button>
                    ` : '<span class="text-muted small">Confirmed</span>')}
                </td>
            </tr>
        `).join('');
    } catch (e) {
        console.error('Failed to load orders', e);
    }
}

async function confirmOrder(orderId) {
    if (!confirm('Confirm this order? This will atomically reserve stock and automatically generate Invoice.')) return;

    try {
        const res = await fetch(`/api/orders/${orderId}/confirm`, { method: 'POST' });
        const json = await res.json();
        if (json.success) {
            showToast(json.message || 'Order confirmed and Invoice generated!', 'success');
            loadOrders();
        } else {
            showToast(json.message || 'Order confirmation failed', 'danger');
        }
    } catch (e) {
        showToast('Error: ' + e.message, 'danger');
    }
}

async function cancelOrder(orderId) {
    const reason = prompt('Please enter the cancellation reason:');
    if (!reason) return;

    try {
        const res = await fetch(`/api/orders/${orderId}/cancel`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ reason: reason })
        });
        const json = await res.json();
        if (json.success) {
            showToast(json.message || 'Order cancelled and reserved stock released.', 'info');
            loadOrders();
        } else {
            showToast(json.message || 'Cancellation failed', 'danger');
        }
    } catch (e) {
        showToast('Error: ' + e.message, 'danger');
    }
}

function openCreateOrderModal() {
    populateCustomerDropdown('newOrderCustomer');
    populateProductDropdowns();
    resetOrderLineItems();
    const modal = new bootstrap.Modal(document.getElementById('newOrderModal'));
    modal.show();
}

function populateCustomerDropdown(elementId) {
    const select = document.getElementById(elementId);
    if (!select) return;
    select.innerHTML = '<option value="">-- Select Customer --</option>' +
        allCustomersCache.map(c => `<option value="${c.CUSTOMER_ID}">${escapeHtml(c.NAME)} (${c.CUSTOMER_CODE})</option>`).join('');
}

function resetOrderLineItems() {
    const container = document.getElementById('orderLineItemsContainer');
    if (!container) return;
    container.innerHTML = `
        <div class="row g-2 align-items-center mb-2 order-item-row">
            <div class="col-md-6">
                <select class="form-select form-select-sm order-prod-select" onchange="calculateOrderTotals()">
                    <option value="">-- Choose Product --</option>
                    ${allProductsCache.map(p => `<option value="${p.PRODUCT_ID}" data-price="${p.CURRENT_SELLING_PRICE}">${escapeHtml(p.NAME)} ($${p.CURRENT_SELLING_PRICE})</option>`).join('')}
                </select>
            </div>
            <div class="col-md-3">
                <input type="number" class="form-control form-control-sm order-qty-input" value="1" min="1" oninput="calculateOrderTotals()">
            </div>
            <div class="col-md-2">
                <span class="order-line-subtotal fw-bold small text-primary">$0.00</span>
            </div>
            <div class="col-md-1">
                <button type="button" class="btn btn-sm btn-link text-danger" onclick="removeOrderLineItem(this)"><i class="bi bi-trash"></i></button>
            </div>
        </div>
    `;
    calculateOrderTotals();
}

function addOrderLineItem() {
    const container = document.getElementById('orderLineItemsContainer');
    const newRow = document.createElement('div');
    newRow.className = 'row g-2 align-items-center mb-2 order-item-row';
    newRow.innerHTML = `
        <div class="col-md-6">
            <select class="form-select form-select-sm order-prod-select" onchange="calculateOrderTotals()">
                <option value="">-- Choose Product --</option>
                ${allProductsCache.map(p => `<option value="${p.PRODUCT_ID}" data-price="${p.CURRENT_SELLING_PRICE}">${escapeHtml(p.NAME)} ($${p.CURRENT_SELLING_PRICE})</option>`).join('')}
            </select>
        </div>
        <div class="col-md-3">
            <input type="number" class="form-control form-control-sm order-qty-input" value="1" min="1" oninput="calculateOrderTotals()">
        </div>
        <div class="col-md-2">
            <span class="order-line-subtotal fw-bold small text-primary">$0.00</span>
        </div>
        <div class="col-md-1">
            <button type="button" class="btn btn-sm btn-link text-danger" onclick="removeOrderLineItem(this)"><i class="bi bi-trash"></i></button>
        </div>
    `;
    container.appendChild(newRow);
    calculateOrderTotals();
}

function removeOrderLineItem(btn) {
    const rows = document.querySelectorAll('.order-item-row');
    if (rows.length > 1) {
        btn.closest('.order-item-row').remove();
        calculateOrderTotals();
    } else {
        showToast('At least one line item is required.', 'warning');
    }
}

function calculateOrderTotals() {
    let subtotal = 0;
    document.querySelectorAll('.order-item-row').forEach(row => {
        const select = row.querySelector('.order-prod-select');
        const qty = parseInt(row.querySelector('.order-qty-input').value) || 0;
        const opt = select.options[select.selectedIndex];
        const price = opt && opt.getAttribute('data-price') ? parseFloat(opt.getAttribute('data-price')) : 0;
        const lineTotal = price * qty;
        row.querySelector('.order-line-subtotal').innerText = formatCurrency(lineTotal);
        subtotal += lineTotal;
    });

    const tax = Math.round(subtotal * 0.18 * 100) / 100;
    const total = subtotal + tax;

    document.getElementById('newOrderSubtotal').innerText = formatCurrency(subtotal);
    document.getElementById('newOrderTax').innerText = formatCurrency(tax);
    document.getElementById('newOrderTotal').innerText = formatCurrency(total);
}

async function saveNewOrder() {
    const customerId = document.getElementById('newOrderCustomer').value;
    const notes = document.getElementById('newOrderNotes').value;

    if (!customerId) {
        showToast('Please select a customer for this order.', 'warning');
        return;
    }

    const items = [];
    document.querySelectorAll('.order-item-row').forEach(row => {
        const prodId = row.querySelector('.order-prod-select').value;
        const qty = parseInt(row.querySelector('.order-qty-input').value) || 0;
        if (prodId && qty > 0) {
            items.push({ productId: parseInt(prodId), quantity: qty });
        }
    });

    if (items.length === 0) {
        showToast('Please add at least one valid product line item.', 'warning');
        return;
    }

    const payload = {
        customerId: parseInt(customerId),
        notes: notes,
        items: items
    };

    try {
        const res = await fetch('/api/orders', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const json = await res.json();
        if (json.success) {
            showToast(json.message || 'Order created successfully!', 'success');
            bootstrap.Modal.getInstance(document.getElementById('newOrderModal')).hide();
            loadOrders();
        } else {
            showToast(json.message || 'Error creating order', 'danger');
        }
    } catch (e) {
        showToast('Error: ' + e.message, 'danger');
    }
}

// ----------------------------------------------------------------------------
// 8. Invoices & PDF Print Preview
// ----------------------------------------------------------------------------
async function loadInvoices() {
    const status = document.getElementById('invoiceStatusFilter')?.value || '';

    try {
        const res = await fetch(`/api/invoices?status=${encodeURIComponent(status)}`);
        const json = await res.json();
        if (!json.success) return;

        const invoices = json.data || [];
        const tbody = document.getElementById('invoicesTableBody');
        if (!tbody) return;

        if (invoices.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center py-4 text-muted">No invoices found</td></tr>';
            return;
        }

        tbody.innerHTML = invoices.map(i => `
            <tr>
                <td><strong>${i.INVOICE_NUMBER}</strong></td>
                <td>
                    <div class="fw-semibold">${escapeHtml(i.CUSTOMER_NAME)}</div>
                    <small class="text-muted">${i.ORDER_NUMBER}</small>
                </td>
                <td>${formatDate(i.ISSUE_DATE)}</td>
                <td>${formatDate(i.DUE_DATE)}</td>
                <td>${formatCurrency(i.TOTAL_AMOUNT)}</td>
                <td><span class="text-success fw-semibold">${formatCurrency(i.PAID_AMOUNT)}</span></td>
                <td><span class="text-danger fw-bold">${formatCurrency(i.BALANCE_AMOUNT)}</span></td>
                <td><span class="badge-status badge-${(i.PAYMENT_STATUS || '').toLowerCase().replace('_', '-')}">${i.PAYMENT_STATUS}</span></td>
                <td class="text-end">
                    <button class="btn btn-sm btn-outline-primary" onclick="viewInvoiceDetail(${i.INVOICE_ID})">
                        <i class="bi bi-printer"></i> View & Print
                    </button>
                    ${i.BALANCE_AMOUNT > 0 ? `
                        <button class="btn btn-sm btn-success" onclick="openRecordPaymentModal(${i.INVOICE_ID}, '${i.INVOICE_NUMBER}', ${i.BALANCE_AMOUNT})">
                            <i class="bi bi-credit-card"></i> Pay
                        </button>
                    ` : ''}
                </td>
            </tr>
        `).join('');
    } catch (e) {
        console.error('Failed to load invoices', e);
    }
}

async function viewInvoiceByOrder(orderId) {
    try {
        const res = await fetch(`/api/orders/${orderId}`);
        const json = await res.json();
        if (json.success && json.data && json.data.INVOICE_ID) {
            viewInvoiceDetail(json.data.INVOICE_ID);
        } else {
            showToast('Invoice not yet generated for this order.', 'info');
        }
    } catch (e) {
        showToast('Failed to find invoice', 'danger');
    }
}

async function viewInvoiceDetail(invoiceId) {
    try {
        const res = await fetch(`/api/invoices/${invoiceId}`);
        const json = await res.json();
        if (!json.success || !json.data) return;

        const inv = json.data;
        document.getElementById('modalInvNumber').innerText = inv.INVOICE_NUMBER;
        document.getElementById('modalInvDate').innerText = formatDate(inv.ISSUE_DATE);
        document.getElementById('modalInvDueDate').innerText = formatDate(inv.DUE_DATE);
        document.getElementById('modalInvCustName').innerText = inv.CUSTOMER_NAME;
        document.getElementById('modalInvCustBilling').innerText = inv.BILLING_ADDRESS || 'Pending address';
        document.getElementById('modalInvStatusBadge').className = `badge-status badge-${(inv.PAYMENT_STATUS || '').toLowerCase().replace('_', '-')}`;
        document.getElementById('modalInvStatusBadge').innerText = inv.PAYMENT_STATUS;

        const itemsBody = document.getElementById('modalInvItemsBody');
        if (inv.items) {
            itemsBody.innerHTML = inv.items.map(item => `
                <tr>
                    <td><strong>${item.SKU}</strong> - ${escapeHtml(item.DESCRIPTION)}</td>
                    <td class="text-center">${item.QUANTITY}</td>
                    <td class="text-end">${formatCurrency(item.UNIT_PRICE)}</td>
                    <td class="text-end">${item.TAX_RATE}%</td>
                    <td class="text-end fw-semibold">${formatCurrency(item.LINE_TOTAL)}</td>
                </tr>
            `).join('');
        }

        document.getElementById('modalInvSubtotal').innerText = formatCurrency(inv.SUBTOTAL);
        document.getElementById('modalInvTax').innerText = formatCurrency(inv.TAX_AMOUNT);
        document.getElementById('modalInvTotal').innerText = formatCurrency(inv.TOTAL_AMOUNT);
        document.getElementById('modalInvPaid').innerText = formatCurrency(inv.PAID_AMOUNT);
        document.getElementById('modalInvBalance').innerText = formatCurrency(inv.BALANCE_AMOUNT);

        const modal = new bootstrap.Modal(document.getElementById('invoicePrintModal'));
        modal.show();
    } catch (e) {
        showToast('Failed to load invoice: ' + e.message, 'danger');
    }
}

// ----------------------------------------------------------------------------
// 9. Payments Ledger
// ----------------------------------------------------------------------------
async function loadPayments() {
    try {
        const res = await fetch('/api/payments');
        const json = await res.json();
        if (!json.success) return;

        const payments = json.data || [];
        const tbody = document.getElementById('paymentsTableBody');
        if (!tbody) return;

        if (payments.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-muted">No payments recorded</td></tr>';
            return;
        }

        tbody.innerHTML = payments.map(p => `
            <tr>
                <td><strong>${p.PAYMENT_REFERENCE}</strong></td>
                <td>${formatDate(p.TRANSACTION_DATE)}</td>
                <td><strong>${p.INVOICE_NUMBER}</strong></td>
                <td>${escapeHtml(p.CUSTOMER_NAME)}</td>
                <td><span class="badge bg-light text-dark border">${p.PAYMENT_METHOD}</span></td>
                <td><span class="fw-bold text-success">${formatCurrency(p.AMOUNT)}</span></td>
                <td><span class="badge bg-success">COMPLETED</span></td>
            </tr>
        `).join('');
    } catch (e) {
        console.error('Failed to load payments', e);
    }
}

function openRecordPaymentModal(invoiceId, invoiceNumber, balance) {
    document.getElementById('payInvoiceId').value = invoiceId;
    document.getElementById('payInvoiceNumber').innerText = invoiceNumber;
    document.getElementById('payBalanceAmount').innerText = formatCurrency(balance);
    document.getElementById('payAmount').value = balance;
    document.getElementById('payIdempotencyKey').value = 'IDEMP-' + Math.random().toString(36).substring(2, 10).toUpperCase();

    const modal = new bootstrap.Modal(document.getElementById('recordPaymentModal'));
    modal.show();
}

async function savePayment() {
    const invoiceId = document.getElementById('payInvoiceId').value;
    const amount = parseFloat(document.getElementById('payAmount').value);
    const method = document.getElementById('payMethod').value;
    const idempotencyKey = document.getElementById('payIdempotencyKey').value;
    const notes = document.getElementById('payNotes').value;

    if (isNaN(amount) || amount <= 0) {
        showToast('Please specify a positive payment amount.', 'warning');
        return;
    }

    try {
        const res = await fetch('/api/payments', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                invoiceId: invoiceId,
                amount: amount,
                paymentMethod: method,
                idempotencyKey: idempotencyKey,
                notes: notes
            })
        });
        const json = await res.json();
        if (json.success) {
            showToast(json.message || 'Payment recorded successfully!', 'success');
            bootstrap.Modal.getInstance(document.getElementById('recordPaymentModal')).hide();
            loadInvoices();
            loadPayments();
        } else {
            showToast(json.message || 'Payment error', 'danger');
        }
    } catch (e) {
        showToast('Error: ' + e.message, 'danger');
    }
}

// ----------------------------------------------------------------------------
// 10. Commissions & Incentive Management
// ----------------------------------------------------------------------------
async function loadCommissions() {
    try {
        // Load Rules
        const rulesRes = await fetch('/api/commissions/rules');
        const rulesJson = await rulesRes.json();
        if (rulesJson.success) {
            const rulesTable = document.getElementById('commissionRulesTableBody');
            if (rulesTable) {
                rulesTable.innerHTML = (rulesJson.data || []).map(r => `
                    <tr>
                        <td><strong>${escapeHtml(r.RULE_NAME)}</strong></td>
                        <td>${formatCurrency(r.MIN_SALES_THRESHOLD)}</td>
                        <td>${formatCurrency(r.MAX_SALES_THRESHOLD)}</td>
                        <td><span class="badge bg-primary fs-6">${r.COMMISSION_RATE_PCT}%</span></td>
                        <td><span class="badge bg-success">ACTIVE (v${r.RULE_VERSION})</span></td>
                    </tr>
                `).join('');
            }
        }

        // Load Records
        const recordsRes = await fetch('/api/commissions/records');
        const recordsJson = await recordsRes.json();
        if (recordsJson.success) {
            const recordsTable = document.getElementById('commissionRecordsTableBody');
            if (recordsTable) {
                recordsTable.innerHTML = (recordsJson.data || []).map(cr => `
                    <tr>
                        <td><strong>#${cr.RECORD_ID}</strong></td>
                        <td>
                            <div class="fw-semibold">${escapeHtml(cr.EMPLOYEE_NAME)}</div>
                            <small class="text-muted">${cr.EMPLOYEE_EMAIL}</small>
                        </td>
                        <td>Order #${cr.ORDER_NUMBER}</td>
                        <td>${formatCurrency(cr.SALES_AMOUNT)}</td>
                        <td>${cr.COMMISSION_PCT}%</td>
                        <td><span class="fw-bold text-success">${formatCurrency(cr.CALCULATED_COMMISSION)}</span></td>
                        <td><span class="badge-status badge-${(cr.STATUS || '').toLowerCase()}">${cr.STATUS}</span></td>
                        <td class="text-end">
                            ${cr.STATUS === 'PENDING' ? `
                                <button class="btn btn-sm btn-success" onclick="updateCommissionStatus(${cr.RECORD_ID}, 'APPROVED')">
                                    <i class="bi bi-check2"></i> Approve
                                </button>
                                <button class="btn btn-sm btn-outline-danger" onclick="updateCommissionStatus(${cr.RECORD_ID}, 'REJECTED')">
                                    <i class="bi bi-x"></i> Reject
                                </button>
                            ` : (cr.STATUS === 'APPROVED' ? `
                                <button class="btn btn-sm btn-outline-success" onclick="updateCommissionStatus(${cr.RECORD_ID}, 'PAID')">
                                    <i class="bi bi-cash"></i> Mark Paid
                                </button>
                            ` : `<small class="text-muted">${cr.STATUS}</small>`)}
                        </td>
                    </tr>
                `).join('');
            }
        }
    } catch (e) {
        console.error('Failed to load commissions', e);
    }
}

async function updateCommissionStatus(recordId, status) {
    try {
        const res = await fetch(`/api/commissions/records/${recordId}/status?status=${status}`, { method: 'PUT' });
        const json = await res.json();
        if (json.success) {
            showToast(`Commission marked as ${status}!`, 'success');
            loadCommissions();
        }
    } catch (e) {
        showToast('Error: ' + e.message, 'danger');
    }
}

// ----------------------------------------------------------------------------
// 11. Department Work Queues
// ----------------------------------------------------------------------------
let activeQueueTab = 'SALES';

async function loadQueues(queueName) {
    activeQueueTab = queueName || activeQueueTab;
    document.querySelectorAll('.queue-tab-btn').forEach(b => {
        b.classList.remove('active');
        if (b.getAttribute('data-queue') === activeQueueTab) b.classList.add('active');
    });

    try {
        const res = await fetch(`/api/queues/items?queue=${activeQueueTab}`);
        const json = await res.json();
        if (!json.success) return;

        const items = json.data || [];
        const tbody = document.getElementById('queueItemsTableBody');
        if (!tbody) return;

        if (items.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-muted">No operational items currently queued in ${activeQueueTab}</td></tr>`;
            return;
        }

        tbody.innerHTML = items.map(item => `
            <tr>
                <td><strong>#${item.ASSIGNMENT_ID}</strong></td>
                <td>
                    <div class="fw-semibold">${escapeHtml(item.TITLE)}</div>
                    <small class="text-muted">Entity: ${item.ENTITY_TYPE} #${item.ENTITY_ID}</small>
                </td>
                <td><span class="badge ${item.PRIORITY === 'CRITICAL' ? 'bg-danger' : (item.PRIORITY === 'HIGH' ? 'bg-warning text-dark' : 'bg-secondary')}">${item.PRIORITY}</span></td>
                <td>${escapeHtml(item.ASSIGNED_TO_NAME || 'Unassigned')}</td>
                <td>${formatDate(item.CREATED_AT)}</td>
                <td><span class="badge-status badge-${(item.STATUS || '').toLowerCase().replace('_', '-')}">${item.STATUS}</span></td>
                <td class="text-end">
                    ${item.STATUS === 'OPEN' ? `
                        <button class="btn btn-sm btn-outline-primary" onclick="updateQueueItemStatus(${item.ASSIGNMENT_ID}, 'IN_PROGRESS')">
                            <i class="bi bi-play"></i> Start
                        </button>
                    ` : (item.STATUS === 'IN_PROGRESS' ? `
                        <button class="btn btn-sm btn-success" onclick="updateQueueItemStatus(${item.ASSIGNMENT_ID}, 'COMPLETED')">
                            <i class="bi bi-check2"></i> Complete
                        </button>
                        <button class="btn btn-sm btn-outline-danger" onclick="updateQueueItemStatus(${item.ASSIGNMENT_ID}, 'ESCALATED')">
                            <i class="bi bi-exclamation-triangle"></i> Escalate
                        </button>
                    ` : `<span class="badge bg-success">Done</span>`)}
                </td>
            </tr>
        `).join('');
    } catch (e) {
        console.error('Failed to load queue items', e);
    }
}

async function updateQueueItemStatus(assignmentId, status) {
    try {
        const res = await fetch(`/api/queues/items/${assignmentId}/status?status=${status}`, { method: 'PUT' });
        const json = await res.json();
        if (json.success) {
            showToast('Work queue item updated!', 'success');
            loadQueues(activeQueueTab);
        }
    } catch (e) {
        showToast('Error: ' + e.message, 'danger');
    }
}

// ----------------------------------------------------------------------------
// 12. Reports & Analytics
// ----------------------------------------------------------------------------
async function loadReports() {
    try {
        // 1. Sales by Rep
        const salesRes = await fetch('/api/reports/sales-by-rep');
        const salesJson = await salesRes.json();
        if (salesJson.success) {
            const tbody = document.getElementById('reportSalesRepTableBody');
            if (tbody) {
                tbody.innerHTML = (salesJson.data || []).map(r => `
                    <tr>
                        <td><strong>${escapeHtml(r.SALES_REP)}</strong></td>
                        <td>${r.TOTAL_ORDERS}</td>
                        <td class="fw-bold">${formatCurrency(r.TOTAL_REVENUE)}</td>
                        <td class="text-success">${formatCurrency(r.CONFIRMED_REVENUE)}</td>
                        <td>${formatCurrency(r.TOTAL_COMMISSION_EARNED)}</td>
                    </tr>
                `).join('');
            }
        }

        // 2. Receivables Aging
        const agingRes = await fetch('/api/reports/receivables-aging');
        const agingJson = await agingRes.json();
        if (agingJson.success && agingJson.data && agingJson.data.length > 0) {
            const ag = agingJson.data[0];
            document.getElementById('aging0to30').innerText = formatCurrency(ag.AGING_0_30 || 0);
            document.getElementById('aging31to60').innerText = formatCurrency(ag.AGING_31_60 || 0);
            document.getElementById('aging61to90').innerText = formatCurrency(ag.AGING_61_90 || 0);
            document.getElementById('aging90Plus').innerText = formatCurrency(ag.AGING_90_PLUS || 0);
            document.getElementById('agingTotal').innerText = formatCurrency(ag.TOTAL_RECEIVABLES || 0);
        }

        // 3. Inventory Valuation
        const invRes = await fetch('/api/reports/inventory-valuation');
        const invJson = await invRes.json();
        if (invJson.success) {
            const tbody = document.getElementById('reportInventoryTableBody');
            if (tbody) {
                tbody.innerHTML = (invJson.data || []).map(v => `
                    <tr>
                        <td><strong>${v.CATEGORY}</strong></td>
                        <td>${v.PRODUCT_COUNT} products</td>
                        <td>${v.TOTAL_UNITS_IN_STOCK} units</td>
                        <td>${formatCurrency(v.TOTAL_COST_VALUATION)}</td>
                        <td class="fw-bold text-primary">${formatCurrency(v.POTENTIAL_RETAIL_VALUATION)}</td>
                    </tr>
                `).join('');
            }
        }
    } catch (e) {
        console.error('Failed to load reports', e);
    }
}

function exportCsvReport(type) {
    window.location.href = `/api/reports/export-csv?type=${type}`;
    showToast(`Downloading ${type} report CSV...`, 'info');
}

// ----------------------------------------------------------------------------
// 13. Audit Trail
// ----------------------------------------------------------------------------
async function loadAuditLogs() {
    try {
        const res = await fetch('/api/audit/logs');
        const json = await res.json();
        if (!json.success) return;

        const tbody = document.getElementById('auditLogsTableBody');
        if (!tbody) return;

        tbody.innerHTML = (json.data || []).map(log => `
            <tr>
                <td>${formatDate(log.CREATED_AT)}</td>
                <td><strong>${escapeHtml(log.USERNAME || 'System')}</strong></td>
                <td><span class="badge bg-light text-dark border">${log.ACTION}</span></td>
                <td>${log.ENTITY_NAME} #${log.ENTITY_ID || ''}</td>
                <td><small class="text-muted">${escapeHtml(log.NEW_VALUES || log.OLD_VALUES || '-')}</small></td>
                <td><small>${log.IP_ADDRESS || '127.0.0.1'}</small></td>
            </tr>
        `).join('');
    } catch (e) {
        console.error('Failed to load audit logs', e);
    }
}

// ----------------------------------------------------------------------------
// Utilities & Formatting
// ----------------------------------------------------------------------------
function formatCurrency(num) {
    if (num === null || num === undefined) return '$0.00';
    return '$' + Number(num).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatDate(dateStr) {
    if (!dateStr) return '-';
    try {
        const d = new Date(dateStr);
        return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    } catch (e) {
        return dateStr;
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function escapeJs(str) {
    if (!str) return '';
    return String(str).replace(/'/g, "\\'").replace(/"/g, '\\"');
}

function showToast(message, type = 'info') {
    const toastContainer = document.getElementById('toastContainer');
    if (!toastContainer) return;

    const toastId = 'toast-' + Date.now();
    const toastHtml = `
        <div id="${toastId}" class="toast align-items-center text-white bg-${type} border-0 shadow-sm" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body small fw-medium">${message}</div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        </div>
    `;
    toastContainer.insertAdjacentHTML('beforeend', toastHtml);
    const toastEl = document.getElementById(toastId);
    const toast = new bootstrap.Toast(toastEl, { delay: 3500 });
    toast.show();
    toastEl.addEventListener('hidden.bs.toast', () => toastEl.remove());
}

function setupGlobalSearch() {
    const input = document.getElementById('globalSearchInput');
    if (!input) return;
    input.addEventListener('keyup', (e) => {
        if (e.key === 'Enter') {
            const q = input.value.trim();
            if (q) {
                navigateToView('customers');
                const custSearch = document.getElementById('customerSearchInput');
                if (custSearch) {
                    custSearch.value = q;
                    loadCustomers();
                }
            }
        }
    });
}
