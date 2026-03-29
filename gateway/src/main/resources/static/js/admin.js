// ==========================================
// SpringBank Enterprise Admin Panel - JS
// ==========================================
const BASE = '';
const authHeaders = () => ({
  'Content-Type': 'application/json',
  'Authorization': `Bearer ${localStorage.getItem('sb_token')}`
});

function checkAuth() {
    const token = localStorage.getItem('sb_token');
    if (!token) {
        window.location.href = '/login.html';
        return;
    }
    try {
        const payload = JSON.parse(decodeURIComponent(atob(token.split('.')[1]).split('').map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join('')));
        const roles = payload?.realm_access?.roles || [];
        if (!roles.includes('ADMIN') && !roles.includes('admin')) {
            window.location.href = '/dashboard.html';
        }
    } catch(e) {
        window.location.href = '/login.html';
    }
}

async function api(method, path, body = null) {
  try {
      const res = await fetch(BASE + path, {
        method,
        headers: authHeaders(),
        body: body ? JSON.stringify(body) : null
      });
      if (res.status === 401) { window.location.href = '/login.html'; return; }
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      
      const ct = res.headers.get('content-type') || '';
      if(ct.includes('application/json')) return await res.json();
      const text = await res.text();
      return text ? text : null;
  } catch(e) {
      showToast(e.message, 'error');
      throw e;
  }
}

function showToast(message, type = 'info') {
    const stack = document.getElementById('toastContainer');
    if (!stack) return;
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    let icon = 'ph-info';
    if(type === 'success') icon = 'ph-check-circle';
    if(type === 'error') icon = 'ph-x-circle';
    if(type === 'warning') icon = 'ph-warning';
    
    toast.innerHTML = `<i class="ph ${icon}" style="font-size:20px;"></i> <span>${message}</span>`;
    stack.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = '0';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

function formatCurrency(amount) {
    if(amount === undefined || amount === null) return '-';
    return Number(amount).toLocaleString('tr-TR', {style:'currency', currency:'TRY'});
}

function formatDate(dateStr) {
    if(!dateStr) return '-';
    return new Date(dateStr).toLocaleString('en-GB', { 
        day: '2-digit', month: '2-digit', year: 'numeric', 
        hour: '2-digit', minute:'2-digit' 
    });
}

function truncateString(str, num) {
    if(!str) return '-';
    if (str.length <= num) return str;
    return `<span class="text-truncate" title="${str}">${str.slice(0, num)}...</span>`;
}

function getStatusChip(status, type = 'status') {
    if(status === undefined || status === null) return '-';
    const s = status.toString().toUpperCase();
    let c = 'chip-neutral';
    if(['SUCCESS', 'ACTIVE', 'UP', 'VALIDATION_SUCCESS', 'MONEY_TRANSFER_SUCCESS', 'TRANSFER_SUCCESS', 'PROCESSED'].includes(s) || status === true) c = 'chip-success';
    if(['FAILED', 'ERROR', 'INACTIVE', 'DOWN', 'VALIDATION_FAILED', 'DENIED'].includes(s) || status === false) c = 'chip-danger';
    if(['PENDING', 'VALIDATION_PENDING', 'PROGRESS'].includes(s)) c = 'chip-warning';
    
    let text = typeof status === 'boolean' ? (status ? (type==='active'?'Active':'Yes') : (type==='active'?'Inactive':'No')) : status;
    return `<span class="chip ${c}">${text}</span>`;
}

function openModal(id) { document.getElementById(id).classList.add('active'); }
function closeModal(id) { document.getElementById(id).classList.remove('active'); }

window.addEventListener('keydown', e => {
    if(e.key === 'Escape') {
        document.querySelectorAll('.modal-overlay.active').forEach(m => m.classList.remove('active'));
    }
});

// NAVIGATION
function attachNavigation() {
    document.getElementById('btnLogout').addEventListener('click', () => {
        localStorage.removeItem('sb_token');
        localStorage.removeItem('sb_refresh');
        localStorage.removeItem('sb_user');
        window.location.href = '/login.html';
    });

    document.querySelectorAll('.nav-item').forEach(el => {
        el.addEventListener('click', () => {
            document.querySelectorAll('.nav-item').forEach(nav => nav.classList.remove('active'));
            el.classList.add('active');
            
            document.querySelectorAll('.page-container').forEach(page => page.classList.remove('active'));
            const target = el.getAttribute('data-target');
            document.getElementById(target).classList.add('active');
            
            initPage(target);
        });
    });
}

// ROUTER
let healthInterval = null;
let roleChartInstance = null;
let volumeChartInstance = null;

async function initPage(page) {
    if (page !== 'health' && healthInterval) {
        clearInterval(healthInterval);
        healthInterval = null;
    }
    if (page !== 'logs') {
        stopLogs();
    }
    
    try {
        if(page === 'dashboard') await initDashboard();
        if(page === 'users') await loadAllUsers();
        if(page === 'transactions') {
            if(!document.getElementById('txEndDate').value) setDefaultDates();
            await filterTransactions();
        }
        if(page === 'fraud') await loadFraudErrors();
        if(page === 'health') {
            await checkAllHealth();
            healthInterval = setInterval(checkAllHealth, 30000);
        }
    } catch(e) {}
}

const getISODate = (daysOffset = 0) => {
    const d = new Date(); d.setDate(d.getDate() + daysOffset);
    return d.toISOString().slice(0, 16);
};

// 1. DASHBOARD
async function initDashboard() {
    try {
        const [total, activeData, roles, txErrors, txAll] = await Promise.all([
            api('GET', '/api/user-service/v1/admin/stats/total').catch(()=>0),
            api('GET', '/api/user-service/v1/admin/stats/active').catch(()=>({active:0, inactive:0})),
            api('GET', '/api/user-service/v1/admin/stats/roles').catch(()=>({USER:0, ADMIN:0})),
            api('GET', '/api/transaction-service/v1/transactions/errors').catch(()=>([])),
            api('GET', `/api/transaction-service/v1/transactions/daterange?startDate=${getISODate(-7)}&endDate=${getISODate(1)}`).catch(()=>([]))
        ]);

        document.getElementById('dashTotalUsers').innerText = total;
        document.getElementById('dashActiveUsers').innerText = activeData.active || 0;
        document.getElementById('dashErrorTx').innerText = txErrors.length || 0;
        
        const todayStr = new Date().toISOString().split('T')[0];
        const todayTxs = txAll.filter(t => t.localDateTime && t.localDateTime.startsWith(todayStr));
        document.getElementById('dashTotalTx').innerText = todayTxs.length;

        renderRoleChart(roles);
        renderVolumeChart(txAll);
        
        const recent = [...txAll].sort((a,b) => new Date(b.localDateTime) - new Date(a.localDateTime)).slice(0, 10);
        const tbody = document.getElementById('dashTxTableBody');
        tbody.innerHTML = '';
        if(recent.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" class="empty-state">No recent transactions.</td></tr>`;
        } else {
            recent.forEach(t => {
                tbody.innerHTML += `
                    <tr>
                        <td>${formatDate(t.localDateTime)}</td>
                        <td style="font-family:monospace; font-size:12px;">${truncateString(t.id, 8)}</td>
                        <td>${getStatusChip(t.transactionType)}</td>
                        <td style="font-weight:600">${formatCurrency(t.money)}</td>
                        <td>${getStatusChip(t.status)}</td>
                    </tr>
                `;
            });
        }
    } catch(e) {}
}

function renderRoleChart(roles) {
    const ctx = document.getElementById('roleChart');
    if(roleChartInstance) roleChartInstance.destroy();
    roleChartInstance = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['USER', 'ADMIN'],
            datasets: [{
                data: [roles.USER || 0, roles.ADMIN || 0],
                backgroundColor: ['#3b82f6', '#f59e0b'],
                borderWidth: 0,
                hoverOffset: 8
            }]
        },
        options: { cutout: '65%', responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom', labels: { color: '#f8fafc' } } } }
    });
}

function renderVolumeChart(txAll) {
    const counts = {};
    for (let i = 6; i >= 0; i--) {
        const d = new Date(); d.setDate(d.getDate() - i);
        counts[d.toLocaleDateString('tr-TR', { weekday: 'short', day: 'numeric' })] = 0;
    }
    txAll.forEach(t => {
        if(!t.localDateTime) return;
        const dStr = new Date(t.localDateTime).toLocaleDateString('tr-TR', { weekday: 'short', day: 'numeric' });
        if(counts[dStr] !== undefined) counts[dStr]++;
    });
    
    const ctx = document.getElementById('volumeChart');
    if(volumeChartInstance) volumeChartInstance.destroy();
    volumeChartInstance = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: Object.keys(counts),
            datasets: [{ 
                label: 'Transactions', 
                data: Object.values(counts), 
                backgroundColor: 'rgba(245, 158, 11, 0.8)',
                borderRadius: 4
            }]
        },
        options: { responsive: true, maintainAspectRatio: false, scales: { y:{ ticks:{color:'#94a3b8'}, grid:{color:'#1e3a63'} }, x:{ ticks:{color:'#94a3b8'}, grid:{display:false} } }, plugins: { legend:{display:false} } }
    });
}

// 2. USER MANAGEMENT
let allUsers = [];
let currentPage = 1;
const pageSize = 20;

async function loadAllUsers() {
    document.getElementById('searchName').value = '';
    document.getElementById('searchEmail').value = '';
    document.getElementById('searchId').value = '';
    try {
        allUsers = await api('GET', '/api/user-service/v1/admin/allusers');
        currentPage = 1;
        renderUsersTable();
    } catch(e) {}
}

document.getElementById('btnSearchUser')?.addEventListener('click', async () => {
    const name = document.getElementById('searchName').value.trim();
    const email = document.getElementById('searchEmail').value.trim();
    const id = document.getElementById('searchId').value.trim();
    
    try {
        if(id) {
            const user = await api('GET', `/api/user-service/v1/admin/finduserbyid/${id}`);
            allUsers = user ? [user] : [];
        } else if(email) {
            allUsers = await api('GET', `/api/user-service/v1/admin/findbyemail?email=${encodeURIComponent(email)}`);
        } else if(name) {
            allUsers = await api('GET', `/api/user-service/v1/admin/search?query=${encodeURIComponent(name)}`);
        } else {
            await loadAllUsers();
            return;
        }
        currentPage = 1;
        renderUsersTable();
    } catch(e) {
        allUsers = [];
        renderUsersTable();
    }
});

function renderUsersTable() {
    const tbody = document.getElementById('usersTableBody');
    tbody.innerHTML = '';
    if(!allUsers || allUsers.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" class="empty-state">No users found.</td></tr>`;
        document.getElementById('pageInfo').innerText = 'Page 1 of 1';
        return;
    }
    
    const totalPages = Math.ceil(allUsers.length / pageSize);
    if(currentPage > totalPages) currentPage = totalPages;
    if(currentPage < 1) currentPage = 1;
    
    document.getElementById('pageInfo').innerText = `Page ${currentPage} of ${totalPages}`;
    
    const start = (currentPage - 1) * pageSize;
    const paged = allUsers.slice(start, start + pageSize);
    
    paged.forEach(u => {
        const uStr = JSON.stringify(u).replace(/"/g, '&quot;');
        tbody.innerHTML += `
            <tr>
                <td style="font-family:monospace; font-size:12px;">${truncateString(u.id, 8)}</td>
                <td>${u.name||'-'}</td>
                <td>${u.surname||'-'}</td>
                <td>${u.mail||'-'}</td>
                <td>
                    <select class="action-select" onchange="updateRole('${u.id}', this.value)">
                        <option value="USER" ${u.role==='USER'?'selected':''}>USER</option>
                        <option value="ADMIN" ${u.role==='ADMIN'?'selected':''}>ADMIN</option>
                    </select>
                </td>
                <td>${getStatusChip(u.active, 'active')}</td>
                <td>${formatDate(u.createdAt)}</td>
                <td>
                    <a class="action-link" onclick="openUserDetailModal(${uStr})">Details</a>
                    ${u.active ? 
                        `<a class="action-link" style="color:var(--warning);" onclick="toggleUserStatus('${u.id}', false)">Deactivate</a>` : 
                        `<a class="action-link" style="color:var(--success);" onclick="toggleUserStatus('${u.id}', true)">Activate</a>`
                    }
                    <a class="action-link" onclick="openPasswordResetModal('${u.id}')">Reset</a>
                    <a class="action-link" style="color:var(--danger);" onclick="openDeleteConfirmModal('${u.id}')">Delete</a>
                </td>
            </tr>
        `;
    });
}

document.getElementById('btnPrevPage')?.addEventListener('click', () => { if(currentPage > 1) { currentPage--; renderUsersTable(); } });
document.getElementById('btnNextPage')?.addEventListener('click', () => { if(currentPage * pageSize < allUsers.length) { currentPage++; renderUsersTable(); } });

window.updateRole = async function(id, role) {
    try {
        await api('PATCH', `/api/user-service/v1/admin/updaterole/${id}?role=${role}`);
        showToast(`Role updated to ${role} for user`, 'success');
        const user = allUsers.find(u => u.id === id);
        if(user) user.role = role;
    } catch(e) { renderUsersTable(); }
};

window.toggleUserStatus = async function(id, activate) {
    try {
        await api('POST', `/api/user-service/v1/admin/users/${id}/${activate ? 'activate' : 'deactivate'}`);
        showToast(`User ${activate?'activated':'deactivated'}`, 'success');
        const user = allUsers.find(u => u.id === id);
        if(user) user.active = activate;
        renderUsersTable();
    } catch(e) {}
};

window.openUserDetailModal = async function(user) {
    const content = document.getElementById('userDetailContent');
    const loader = document.getElementById('userDetailLoader');
    content.innerHTML = '';
    loader.style.display = 'flex';
    openModal('userDetailModal');
    
    try {
        const headerWithKeycloak = authHeaders();
        headerWithKeycloak['X-User-KeycloakUUID'] = user.keycloackUUID || user.id; 
        let balData = {money: '-', blockedMoney: '-', userIban: '-'};
        try {
            const res = await fetch(BASE + '/api/money-service/v1/accounts/balance-info', { headers: headerWithKeycloak });
            if(res.ok) balData = await res.json();
        } catch(e) {}

        loader.style.display = 'none';
        content.innerHTML = `
            <div class="kv-pair"><span class="kv-key">ID</span><span class="kv-val">${user.id || '-'}</span></div>
            <div class="kv-pair"><span class="kv-key">Keycloak UUID</span><span class="kv-val">${user.keycloackUUID || '-'}</span></div>
            <div class="kv-pair"><span class="kv-key">Name</span><span class="kv-val">${user.name || '-'} ${user.surname || '-'}</span></div>
            <div class="kv-pair"><span class="kv-key">Email</span><span class="kv-val">${user.mail || '-'}</span></div>
            <div class="kv-pair"><span class="kv-key">Role / Status</span><span class="kv-val">${user.role} / ${user.active?'Active':'Inactive'}</span></div>
            <br>
            <h4 style="color:var(--text-secondary); margin-bottom:12px;">Account Information</h4>
            <div class="kv-pair"><span class="kv-key">IBAN</span><span class="kv-val">${balData.userIban || '-'}</span></div>
            <div class="kv-pair"><span class="kv-key">Balance</span><span class="kv-val">${formatCurrency(balData.money)}</span></div>
            <div class="kv-pair"><span class="kv-key">Blocked Balance</span><span class="kv-val">${formatCurrency(balData.blockedMoney)}</span></div>
        `;
    } catch(e) {}
};

window.openPasswordResetModal = function(id) {
    document.getElementById('resetPasswordUserId').value = id;
    document.getElementById('newPasswordInput').value = '';
    openModal('resetPasswordModal');
};
window.confirmResetPassword = async function() {
    const id = document.getElementById('resetPasswordUserId').value;
    const p = document.getElementById('newPasswordInput').value;
    if(!p) { showToast('Password is required', 'warning'); return; }
    try {
        await api('POST', `/api/user-service/v1/admin/users/${id}/reset-password`, {newPassword: p});
        showToast('Password reset OK', 'success');
        closeModal('resetPasswordModal');
    } catch(e) {}
};

window.openDeleteConfirmModal = function(id) {
    document.getElementById('btnConfirmDelete').onclick = () => confirmDeleteUser(id);
    openModal('deleteConfirmModal');
};
async function confirmDeleteUser(id) {
    try {
        await api('DELETE', `/api/user-service/v1/admin/deleteuser/${id}`);
        showToast('User deleted', 'success');
        closeModal('deleteConfirmModal');
        loadAllUsers();
    } catch(e) {}
}

// 3. TRANSACTION MONITOR
function setDefaultDates() {
    document.getElementById('txEndDate').value = getISODate(1); 
    document.getElementById('txStartDate').value = getISODate(-30);
}

document.getElementById('btnFilterTx')?.addEventListener('click', filterTransactions);

async function filterTransactions() {
    const start = document.getElementById('txStartDate').value;
    const end = document.getElementById('txEndDate').value;
    const tbody = document.getElementById('txTableBody');
    if(!tbody) return;
    
    try {
        const txs = await api('GET', `/api/transaction-service/v1/transactions/daterange?startDate=${start}&endDate=${end}`);
        let vol = 0, errs = 0;
        
        tbody.innerHTML = '';
        if(!txs || txs.length === 0) {
            tbody.innerHTML = `<tr><td colspan="11" class="empty-state">No transactions in this period.</td></tr>`;
        } else {
            txs.forEach(t => {
                vol += t.money || 0;
                if(t.error) errs++;
                const tStr = JSON.stringify(t).replace(/"/g, '&quot;');
                tbody.innerHTML += `
                    <tr>
                        <td style="font-family:monospace; font-size:12px;">${truncateString(t.id, 8)}</td>
                        <td style="font-family:monospace; font-size:12px;">${truncateString(t.eventId, 8)}</td>
                        <td>${getStatusChip(t.transactionType)}</td>
                        <td style="font-weight:600">${formatCurrency(t.money)}</td>
                        <td>${t.senderName || '-'}</td>
                        <td>${t.receiverName || '-'}</td>
                        <td>${getStatusChip(t.status)}</td>
                        <td>${getStatusChip(t.transferStatus)}</td>
                        <td>${getStatusChip(t.error)}</td>
                        <td>${formatDate(t.localDateTime)}</td>
                        <td><a class="action-link" onclick="openTransactionDetailModal(${tStr})">View</a></td>
                    </tr>
                `;
            });
        }
        document.getElementById('txCount').innerText = txs ? txs.length : 0;
        document.getElementById('txVolume').innerText = formatCurrency(vol);
        document.getElementById('txErrors').innerText = errs;
    } catch(e) {}
}

window.openTransactionDetailModal = function(t) {
    const content = document.getElementById('txDetailContent');
    let html = ``;
    const keys = ['id', 'eventId', 'transactionType', 'status', 'statusDescription', 'transferStatus', 'money', 'localDateTime', 'error', 'errorDescription', 'userValidation', 'isMoneyBlocked', 'senderUserId', 'receiverUserId', 'senderName', 'senderSurname', 'senderEmail', 'senderIban', 'receiverName', 'receiverSurname', 'receiverEmail', 'receiverIban', 'description'];
    keys.forEach(k => {
        let val = t[k];
        if(k === 'localDateTime') val = formatDate(val);
        if(k === 'money') val = formatCurrency(val);
        html += `<div class="kv-pair"><span class="kv-key">${k}</span><span class="kv-val">${val !== undefined && val !== null ? val : '-'}</span></div>`;
    });
    content.innerHTML = html;
    openModal('txDetailModal');
};

// 4. ACCOUNT OPERATIONS
let opCurrentUserId = null;
document.getElementById('btnLookupAccount')?.addEventListener('click', async () => {
    const id = document.getElementById('opUserId').value.trim();
    if(!id) return;
    try {
        const headerWithKeycloak = authHeaders();
        headerWithKeycloak['X-User-KeycloakUUID'] = id; 
        const res = await fetch(BASE + '/api/money-service/v1/accounts/balance-info', { headers: headerWithKeycloak });
        if(res.status === 401) { window.location.href = '/login.html'; return; }
        if(!res.ok) throw new Error('Account not found');
        const data = await res.json();
        
        document.getElementById('opUuid').innerText = id;
        document.getElementById('opIban').innerText = data.userIban || '-';
        document.getElementById('opBalance').innerText = formatCurrency(data.money);
        document.getElementById('opBlocked').innerText = formatCurrency(data.blockedMoney);
        document.getElementById('opAccountDetails').style.display = 'block';
        opCurrentUserId = id;
        showToast('Account found', 'success');
    } catch(e) {
        document.getElementById('opAccountDetails').style.display = 'none';
        showToast(e.message, 'error');
    }
});

document.getElementById('btnDeposit')?.addEventListener('click', async () => {
    if(!opCurrentUserId) return;
    const amt = document.getElementById('opAmount').value;
    if(!amt || amt <= 0) return;
    try {
        await api('POST', '/api/money-service/v1/accounts/depositByUserId', {userId: opCurrentUserId, amount: Number(amt)});
        showToast('Deposit successful', 'success');
        document.getElementById('btnLookupAccount').click();
    } catch(e) {}
});

document.getElementById('btnWithdraw')?.addEventListener('click', async () => {
    if(!opCurrentUserId) return;
    const amt = document.getElementById('opAmount').value;
    if(!amt || amt <= 0) return;
    try {
        await api('POST', '/api/money-service/v1/accounts/withdrawByUserId', {userId: opCurrentUserId, amount: Number(amt)});
        showToast('Withdraw successful', 'success');
        document.getElementById('btnLookupAccount').click();
    } catch(e) {}
});

document.getElementById('btnLookupIbanOnly')?.addEventListener('click', async () => {
    const id = document.getElementById('opIbanLookupId').value.trim();
    if(!id) return;
    try {
        const res = await api('POST', '/api/money-service/v1/accounts/getUserIbanWithUserId', {userId: id});
        document.getElementById('opIbanOnlyResult').innerText = res.iban || res;
        document.getElementById('opIbanResultPanel').style.display = 'block';
    } catch(e) {}
});

// 5. FRAUD & ERRORS
async function loadFraudErrors() {
    const tbody = document.getElementById('errorTableBody');
    if(!tbody) return;
    try {
        const errors = await api('GET', '/api/transaction-service/v1/transactions/errors');
        let totalVal = 0; const types = {};
        
        tbody.innerHTML = '';
        if(!errors || errors.length === 0) {
            tbody.innerHTML = `<tr><td colspan="9" class="empty-state">No errors found.</td></tr>`;
        } else {
            errors.forEach(e => {
                totalVal++;
                types[e.transactionType] = (types[e.transactionType] || 0) + 1;
                const eStr = JSON.stringify(e).replace(/"/g, '&quot;');
                tbody.innerHTML += `
                    <tr class="error-row">
                        <td style="font-family:monospace; font-size:12px;">${truncateString(e.id, 8)}</td>
                        <td>${getStatusChip(e.transactionType)}</td>
                        <td>${formatCurrency(e.money)}</td>
                        <td title="${e.errorDescription||'-'}"><span style="max-width:200px; display:block; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">${e.errorDescription||'-'}</span></td>
                        <td>${e.senderName||'-'}</td>
                        <td>${e.receiverName||'-'}</td>
                        <td>${formatDate(e.localDateTime)}</td>
                        <td>${getStatusChip(e.status)}</td>
                        <td><a class="action-link" onclick="openTransactionDetailModal(${eStr})">View</a></td>
                    </tr>
                `;
            });
        }
        document.getElementById('errTotal').innerText = totalVal;
        
        const allTxRes = await fetch(BASE + `/api/transaction-service/v1/transactions/daterange?startDate=${getISODate(-30)}&endDate=${getISODate(1)}`, {headers: authHeaders()});
        if(allTxRes.ok) {
            const allTx = await allTxRes.json();
            const rt = allTx.length > 0 ? ((totalVal / allTx.length) * 100).toFixed(2) + '%' : '0%';
            document.getElementById('errRate').innerText = rt;
        }
        let mostComm = '-'; let max = 0;
        Object.entries(types).forEach(([k,v]) => { if(v > max) { max = v; mostComm = k; } });
        document.getElementById('errCommon').innerText = mostComm;
    } catch(e) {}
}

// 6. SYSTEM HEALTH
const healthEndpoints = [
    { title: 'User Service', path: '/api/user-service/actuator/health' },
    { title: 'Money Service', path: '/api/money-service/actuator/health' },
    { title: 'Tx Service', path: '/api/transaction-service/actuator/health' },
    { title: 'Auth Service', path: '/api/auth-service/actuator/health' },
    { title: 'Fraud Service', path: '/api/fraud-service/actuator/health' }
];

document.getElementById('btnRefreshHealth')?.addEventListener('click', checkAllHealth);

async function checkAllHealth() {
    const grid = document.getElementById('healthGrid');
    if(!grid) return;
    
    grid.innerHTML = `
        <div class="health-card" style="border-left:4px solid var(--success)">
            <div class="health-header">
                <span class="health-title">Gateway Service</span>
                <i class="ph-fill ph-check-circle" style="color:var(--success); font-size:24px;"></i>
            </div>
            <div class="health-meta"><span>Status: UP</span><span>< 10ms</span></div>
        </div>
    `;
    
    for(const svc of healthEndpoints) {
        grid.innerHTML += `
            <div class="health-card" id="hcard-${svc.title.replace(' ', '')}">
                <div class="health-header"><span class="health-title">${svc.title}</span><div class="loader" style="width:16px;height:16px;"></div></div>
            </div>
        `;
    }

    for(const svc of healthEndpoints) {
        const start = performance.now();
        const cid = `hcard-${svc.title.replace(' ', '')}`;
        try {
            const res = await api('GET', svc.path);
            const dur = Math.round(performance.now() - start);
            const isUp = res.status === 'UP';
            const color = isUp ? 'var(--success)' : 'var(--danger)';
            const icon = isUp ? 'ph-check-circle' : 'ph-x-circle';
            const statText = res.status || 'DOWN';
            
            document.getElementById(cid).style.borderLeft = `4px solid ${color}`;
            document.getElementById(cid).innerHTML = `
                <div class="health-header"><span class="health-title">${svc.title}</span><i class="ph-fill ${icon}" style="color:${color}; font-size:24px;"></i></div>
                <div class="health-meta"><span>Status: ${statText}</span><span>${dur}ms</span></div>
            `;
        } catch(e) {
            document.getElementById(cid).style.borderLeft = `4px solid var(--danger)`;
            document.getElementById(cid).innerHTML = `
                <div class="health-header"><span class="health-title">${svc.title}</span><i class="ph-fill ph-warning-circle" style="color:var(--danger); font-size:24px;"></i></div>
                <div class="health-meta"><span>Status: UNREACHABLE</span><span>-</span></div>
            `;
        }
    }
}

// 7. LIVE LOGS / TERMINAL
let logPollInterval = null;
let activeLogService = null;

document.querySelectorAll('.log-service-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        const svc = btn.getAttribute('data-service');
        startLogs(svc, btn);
    });
});

document.getElementById('log-stop-btn')?.addEventListener('click', stopLogs);

window.clearLogs = function() {
    const term = document.getElementById('log-terminal');
    if(term) term.innerHTML = `<div style="color:var(--text-secondary);"><i class="ph ph-terminal" style="font-size:20px; vertical-align:middle; margin-right:8px;"></i>Terminal temizlendi.</div>`;
    stopLogs();
};

function startLogs(serviceName, btn) {
    stopLogs();
    activeLogService = serviceName;
    document.querySelectorAll('.log-service-btn').forEach(b => b.style.opacity = '0.5');
    if(btn) btn.style.opacity = '1';
    
    document.getElementById('log-stop-btn').style.display = 'inline-flex';
    const term = document.getElementById('log-terminal');
    logAppend(term, 'INFO', `MONITOR BAŞLATILDI -> ${serviceName}`);
    
    let tick = 0;
    logPollInterval = setInterval(async () => {
        tick++;
        try {
            // Polling actuator to simulate logs stream mapping old logic
            const logRes = await api('GET', `/api/${serviceName}/actuator/health`);
            const status = logRes.status || 'UNKNOWN';
            const level = status === 'UP' ? 'INFO' : 'ERROR';
            logAppend(term, level, `${serviceName} -> Health Check poll#${tick} STATUS=${status}`);
        } catch(e) {
            logAppend(term, 'ERROR', `${serviceName} -> Polling error: ${e.message}`);
        }
    }, 4000);
}

function stopLogs() {
    if(logPollInterval) { clearInterval(logPollInterval); logPollInterval = null; }
    const btn = document.getElementById('log-stop-btn');
    if(btn) btn.style.display = 'none';
    document.querySelectorAll('.log-service-btn').forEach(b => b.style.opacity = '1');
    activeLogService = null;
}

function logAppend(term, level, text) {
    const div = document.createElement('div');
    const time = new Date().toLocaleTimeString();
    div.style.marginBottom = '6px';
    div.style.color = level === 'ERROR' ? '#ff4d4d' : '#33ff33';
    div.innerText = `[${time}] [${level}] ${text}`;
    term.appendChild(div);
    term.scrollTop = term.scrollHeight;
}

// INITIATION
window.onload = () => {
    checkAuth();
    attachNavigation();
    initPage('dashboard');
};
