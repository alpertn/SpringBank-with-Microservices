/* =============================================================
   SpringBank — Admin Panel JavaScript
   Tüm admin işlemleri: kullanıcı yönetimi, işlem monitörü,
   hesap ops, fraud, sistem sağlığı, canlı loglar
   ============================================================= */

'use strict';

// ─── Helpers ─────────────────────────────────────────────────
const token = () => localStorage.getItem('sb_token');
const authH = () => ({ 'Content-Type': 'application/json', 'Authorization': `Bearer ${token()}` });

function parseJwt() {
  try {
    const t = token();
    if (!t) return null;
    const b = t.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(decodeURIComponent(atob(b).split('').map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join('')));
  } catch { return null; }
}

function checkAdmin() {
  const t = token();
  if (!t) { window.location.href = '/login.html'; return false; }
  try {
    const p = parseJwt();
    const roles = p?.realm_access?.roles || [];
    if (!roles.includes('ADMIN') && !roles.includes('admin')) {
      window.location.href = '/dashboard.html';
      return false;
    }
    return true;
  } catch { window.location.href = '/login.html'; return false; }
}

async function api(method, path, body = null) {
  const opts = { method, headers: authH() };
  if (body !== null) opts.body = JSON.stringify(body);
  const res = await fetch(path, opts);
  if (res.status === 401) { window.location.href = '/login.html'; return null; }
  if (!res.ok) throw new Error(`HTTP ${res.status}: ${await res.text().catch(() => '')}`);
  const ct = res.headers.get('content-type') || '';
  if (ct.includes('application/json')) return res.json();
  const txt = await res.text();
  return txt || null;
}

function toast(msg, type = 'info') {
  const stack = document.getElementById('toast-stack');
  if (!stack) return;
  const el = document.createElement('div');
  el.className = `toast-item ${type}`;
  const icons = { success: 'ph-check-circle', error: 'ph-x-circle', warning: 'ph-warning', info: 'ph-info' };
  const colors = { success: 'var(--success)', error: 'var(--danger)', warning: 'var(--warning)', info: 'var(--primary)' };
  el.innerHTML = `<i class="ph-fill ${icons[type]||'ph-info'}" style="font-size:18px;color:${colors[type]||'var(--primary)'}; flex-shrink:0;"></i><span>${msg}</span>`;
  stack.appendChild(el);
  setTimeout(() => { el.style.opacity = '0'; el.style.transition = 'opacity 0.3s'; setTimeout(() => el.remove(), 300); }, 4000);
}

function formatDate(d) {
  if (!d) return '-';
  return new Date(d).toLocaleString('tr-TR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}

function formatMoney(n) {
  if (n === undefined || n === null) return '-';
  return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(n);
}

function trunc(str, len = 10) {
  if (!str) return '-';
  if (str.length <= len) return `<span class="mono">${str}</span>`;
  return `<span class="mono" title="${str}">${str.slice(0, len)}…</span>`;
}

// ─── Status Badge ─────────────────────────────────────────────
function badge(val) {
  if (val === undefined || val === null) return '<span class="badge badge-muted">—</span>';
  const s = String(val).toUpperCase();

  // Transaction statuses
  if (['COMPLETED', 'SUCCESS', 'ACTIVE', 'UP', 'TRUE', 'MONEY_TRANSFER_SUCCESS', 'TRANSFER_SUCCESS'].includes(s) || val === true)
    return `<span class="badge badge-success"><i class="ph-fill ph-check-circle"></i> ${val}</span>`;
  if (['FAILED', 'ERROR', 'DOWN', 'FALSE', 'DENIED', 'INACTIVE', 'BLOCK_MONEY_FAILED'].includes(s) || val === false)
    return `<span class="badge badge-danger"><i class="ph-fill ph-x-circle"></i> ${val}</span>`;
  if (['PENDING', 'PROGRESS', 'FRAUD_REVIEW', 'BLOCK_MONEY', 'CREATED'].includes(s))
    return `<span class="badge badge-warning"><i class="ph ph-clock"></i> ${val}</span>`;
  if (['DEPOSIT'].includes(s))  return `<span class="badge badge-teal"><i class="ph ph-plus"></i> ${val}</span>`;
  if (['WITHDRAW'].includes(s)) return `<span class="badge badge-warning"><i class="ph ph-minus"></i> ${val}</span>`;
  if (['TRANSFER', 'EFT'].includes(s)) return `<span class="badge badge-info"><i class="ph ph-arrows-left-right"></i> ${val}</span>`;
  if (['ADMIN'].includes(s)) return `<span class="badge badge-purple">${val}</span>`;
  if (['USER'].includes(s))  return `<span class="badge badge-info">${val}</span>`;
  return `<span class="badge badge-muted">${val}</span>`;
}

function openModal(id) { document.getElementById(id)?.classList.add('show'); }
function closeModal(id) { document.getElementById(id)?.classList.remove('show'); }

window.closeModal = closeModal;

document.addEventListener('keydown', e => {
  if (e.key === 'Escape') document.querySelectorAll('.modal-bg.show').forEach(m => m.classList.remove('show'));
});

// ─── Clock ────────────────────────────────────────────────────
function startClock() {
  const el = document.getElementById('topbarClock');
  setInterval(() => { if (el) el.textContent = new Date().toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit', second: '2-digit' }); }, 1000);
}

// ─── Navigation ───────────────────────────────────────────────
const PAGE_TITLES = {
  dashboard: 'Dashboard', users: 'Kullanıcı Yönetimi',
  transactions: 'İşlem Monitörü', accounts: 'Hesap İşlemleri',
  fraud: 'Hata & Fraud Monitörü', health: 'Sistem Sağlığı', logs: 'Canlı Loglar'
};

let currentPage = '';
let healthTimer = null;

function navigateTo(page) {
  if (currentPage === page) return;
  currentPage = page;

  document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
  document.querySelector(`.nav-item[data-page="${page}"]`)?.classList.add('active');

  document.querySelectorAll('.page-wrap').forEach(p => p.classList.remove('active'));
  document.getElementById(`page-${page}`)?.classList.add('active');

  document.getElementById('topbarTitle').textContent = PAGE_TITLES[page] || page;

  if (healthTimer && page !== 'health') { clearInterval(healthTimer); healthTimer = null; }
  if (page !== 'logs') stopLogs();

  switch (page) {
    case 'dashboard':    initDashboard(); break;
    case 'users':        loadUsers(); break;
    case 'transactions': initTxPage(); break;
    case 'accounts':     loadTopAccounts(); break;
    case 'fraud':        loadFraud(); break;
    case 'health':       checkHealth(); healthTimer = setInterval(checkHealth, 30000); break;
    case 'logs':         break;
  }
}

function attachNav() {
  document.querySelectorAll('.nav-item[data-page]').forEach(btn => {
    btn.addEventListener('click', () => navigateTo(btn.getAttribute('data-page')));
  });

  document.getElementById('btnLogout')?.addEventListener('click', () => {
    localStorage.removeItem('sb_token');
    localStorage.removeItem('sb_refresh');
    localStorage.removeItem('sb_user');
    window.location.href = '/login.html';
  });

  document.getElementById('btnRefreshDash')?.addEventListener('click', initDashboard);
}

function initSidebarUser() {
  const p = parseJwt();
  const name = p?.preferred_username || p?.name || 'Admin';
  const el = document.getElementById('sidebarAdminName');
  if (el) el.textContent = name;
  const init = document.getElementById('sidebarInitials');
  if (init) init.textContent = name.charAt(0).toUpperCase();
}

// ═══════════════════════════════════════════════════════════════
// 1. DASHBOARD
// ═══════════════════════════════════════════════════════════════
let roleChartInst = null;
let volChartInst  = null;

async function initDashboard() {
  try {
    const [totalUsers, activeData, roleData, txCount, moneySummary, txSummary] = await Promise.allSettled([
      api('GET', '/api/user-service/v1/admin/stats/total'),
      api('GET', '/api/user-service/v1/admin/stats/active'),
      api('GET', '/api/user-service/v1/admin/stats/roles'),
      api('GET', '/api/transaction-service/v1/admin/stats/count'),
      api('GET', '/api/money-service/v1/admin/stats/summary'),
      api('GET', '/api/transaction-service/v1/admin/stats/summary')
    ]);

    const total    = totalUsers.status === 'fulfilled'  ? (totalUsers.value || 0) : 0;
    const active   = activeData.status === 'fulfilled'  ? (activeData.value?.active || 0) : 0;
    const roles    = roleData.status === 'fulfilled'    ? (roleData.value || {}) : {};
    const txCounts = txCount.status === 'fulfilled'     ? (txCount.value || {}) : {};
    const money    = moneySummary.status === 'fulfilled'? (moneySummary.value || {}) : {};
    const txSum    = txSummary.status === 'fulfilled'   ? (txSummary.value || {}) : {};

    document.getElementById('kpiTotalUsers').textContent  = total;
    document.getElementById('kpiActiveUsers').textContent = active;
    document.getElementById('kpiTodayTx').textContent     = txCounts.total || txSum.totalCount || '—';
    document.getElementById('kpiErrorTx').textContent     = txCounts.errors || txSum.failed || '—';

    // Extra KPI cards — money stats
    const kpiMoney = document.getElementById('kpiTotalMoney');
    if (kpiMoney) kpiMoney.textContent = formatMoney(money.totalBalance);
    const kpiBlocked = document.getElementById('kpiBlockedMoney');
    if (kpiBlocked) kpiBlocked.textContent = formatMoney(money.totalBlockedBalance);

    const errBadge = document.getElementById('errBadge');
    if (errBadge && txCounts.errors > 0) errBadge.style.display = 'inline-block';

    renderRoleChart(roles);

    // Tx tipi grafiği için summary'i kullan
    if (txSum.countByType) {
      renderVolChartFromSummary(txSum);
    } else {
      // Fallback: daterange ile
      const txAll = await api('GET', `/api/transaction-service/v1/transactions/daterange?startDate=${isoOffset(-7)}&endDate=${isoOffset(1)}`).catch(() => []);
      renderVolChart(txAll || []);
      renderDashTxTable(txAll || []);
      return;
    }

    const txAll = await api('GET', `/api/transaction-service/v1/transactions/daterange?startDate=${isoOffset(-7)}&endDate=${isoOffset(1)}`).catch(() => []);
    renderDashTxTable(txAll || []);
  } catch (e) {
    console.error('[Dashboard]', e);
  }
}

function renderRoleChart(roles) {
  const ctx = document.getElementById('roleChart');
  if (!ctx) return;
  if (roleChartInst) roleChartInst.destroy();
  roleChartInst = new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: ['Kullanıcı', 'Admin'],
      datasets: [{ data: [roles.USER || 0, roles.ADMIN || 0], backgroundColor: ['#026CB6', '#0C9A5D'], borderWidth: 0, hoverOffset: 6 }]
    },
    options: {
      cutout: '68%', responsive: true, maintainAspectRatio: false,
      plugins: { legend: { position: 'bottom', labels: { color: '#5B6B79', font: { family: 'Inter', size: 12 }, padding: 16 } } }
    }
  });
}

function renderVolChart(txAll) {
  const ctx = document.getElementById('volChart');
  if (!ctx) return;
  const labels = [];
  const counts = {};
  for (let i = 6; i >= 0; i--) {
    const d = new Date(); d.setDate(d.getDate() - i);
    const k = d.toLocaleDateString('tr-TR', { day: 'numeric', month: 'short' });
    labels.push(k); counts[k] = 0;
  }
  txAll.forEach(t => {
    if (!t.localDateTime) return;
    const k = new Date(t.localDateTime).toLocaleDateString('tr-TR', { day: 'numeric', month: 'short' });
    if (counts[k] !== undefined) counts[k]++;
  });
  if (volChartInst) volChartInst.destroy();
  volChartInst = new Chart(ctx, {
    type: 'bar',
    data: {
      labels,
      datasets: [{ label: 'İşlem', data: Object.values(counts), backgroundColor: '#026CB6', borderRadius: 5, borderSkipped: false }]
    },
    options: {
      responsive: true, maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: {
        y: { ticks: { color: '#5B6B79', font: { family: 'Inter', size: 11 } }, grid: { color: '#E1EAF2' } },
        x: { ticks: { color: '#5B6B79', font: { family: 'Inter', size: 11 } }, grid: { display: false } }
      }
    }
  });
}

function renderVolChartFromSummary(txSum) {
  const ctx = document.getElementById('volChart');
  if (!ctx) return;
  if (volChartInst) volChartInst.destroy();
  const byType = txSum.countByType || {};
  volChartInst = new Chart(ctx, {
    type: 'bar',
    data: {
      labels: ['DEPOSIT', 'WITHDRAW', 'TRANSFER'],
      datasets: [{
        label: 'İşlem Sayısı',
        data: [byType.DEPOSIT || 0, byType.WITHDRAW || 0, byType.TRANSFER || 0],
        backgroundColor: ['#0C9A5D', '#E88F10', '#026CB6'],
        borderRadius: 5, borderSkipped: false
      }]
    },
    options: {
      responsive: true, maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: {
        y: { ticks: { color: '#5B6B79' }, grid: { color: '#E1EAF2' } },
        x: { ticks: { color: '#5B6B79' }, grid: { display: false } }
      }
    }
  });
}

function renderDashTxTable(allTx) {
  const tbody = document.getElementById('dashTxTbody');
  if (!tbody) return;
  const recent = [...allTx].sort((a, b) => new Date(b.localDateTime) - new Date(a.localDateTime)).slice(0, 10);
  if (!recent.length) { tbody.innerHTML = `<tr><td colspan="6" class="empty-msg">İşlem bulunamadı.</td></tr>`; return; }
  tbody.innerHTML = recent.map(t => `
    <tr>
      <td>${formatDate(t.localDateTime)}</td>
      <td>${trunc(t.id, 8)}</td>
      <td>${badge(t.transactionType)}</td>
      <td style="font-weight:700;">${formatMoney(t.money)}</td>
      <td class="mono" style="font-size:11px;">${t.senderIban ? trunc(t.senderIban, 12) : (t.senderName || '-')}</td>
      <td>${badge(t.status)}</td>
    </tr>
  `).join('');
}

// ═══════════════════════════════════════════════════════════════
// 2. USERS
// ═══════════════════════════════════════════════════════════════
let allUsers = [];
let usersPage = 1;
const PAGE_SIZE = 20;

async function loadUsers() {
  document.getElementById('searchName').value = '';
  document.getElementById('searchEmail').value = '';
  document.getElementById('searchId').value = '';
  const tbody = document.getElementById('usersTbody');
  tbody.innerHTML = `<tr><td colspan="8" class="empty-msg"><div class="loader-ring"></div> Yükleniyor...</td></tr>`;
  try {
    allUsers = await api('GET', '/api/user-service/v1/admin/allusers') || [];
    usersPage = 1;
    renderUsersTable();
  } catch (e) {
    tbody.innerHTML = `<tr><td colspan="8" class="empty-msg" style="color:var(--danger);">Kullanıcılar yüklenemedi: ${e.message}</td></tr>`;
  }
}

function renderUsersTable() {
  const tbody = document.getElementById('usersTbody');
  if (!allUsers || allUsers.length === 0) {
    tbody.innerHTML = `<tr><td colspan="8" class="empty-msg">Kullanıcı bulunamadı.</td></tr>`;
    document.getElementById('pageInfo').textContent = 'Sayfa 1 / 1';
    return;
  }
  const totalP = Math.ceil(allUsers.length / PAGE_SIZE);
  if (usersPage > totalP) usersPage = totalP;
  if (usersPage < 1) usersPage = 1;
  document.getElementById('pageInfo').textContent = `Sayfa ${usersPage} / ${totalP} (${allUsers.length} kullanıcı)`;

  const paged = allUsers.slice((usersPage - 1) * PAGE_SIZE, usersPage * PAGE_SIZE);
  tbody.innerHTML = paged.map(u => `
    <tr>
      <td>${trunc(u.id, 10)}</td>
      <td>${u.name || '-'}</td>
      <td>${u.surname || '-'}</td>
      <td>${u.mail || '-'}</td>
      <td>
        <select class="role-select" onchange="updateRole('${u.id}', this.value)">
          <option value="USER"  ${u.role === 'USER'  ? 'selected' : ''}>USER</option>
          <option value="ADMIN" ${u.role === 'ADMIN' ? 'selected' : ''}>ADMIN</option>
        </select>
      </td>
      <td>${u.active ? '<span class="badge badge-success">Aktif</span>' : '<span class="badge badge-muted">Pasif</span>'}</td>
      <td style="font-size:12px;">${formatDate(u.createdAt)}</td>
      <td>
        <a class="action-link" onclick="showUserDetail('${u.id}')">Detay</a>
        ${u.active
          ? `<a class="action-link orange" onclick="toggleStatus('${u.id}', false)">Pasifleştir</a>`
          : `<a class="action-link green" onclick="toggleStatus('${u.id}', true)">Aktifleştir</a>`}
        <a class="action-link" onclick="showResetPwd('${u.id}')">Şifre</a>
        <a class="action-link red" onclick="showDelete('${u.id}')">Sil</a>
      </td>
    </tr>
  `).join('');
}

// Search
document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('btnSearch')?.addEventListener('click', searchUsers);
  document.getElementById('searchName')?.addEventListener('keydown', e => { if (e.key === 'Enter') searchUsers(); });
  document.getElementById('searchEmail')?.addEventListener('keydown', e => { if (e.key === 'Enter') searchUsers(); });
  document.getElementById('searchId')?.addEventListener('keydown', e => { if (e.key === 'Enter') searchUsers(); });
  document.getElementById('btnLoadAll')?.addEventListener('click', loadUsers);
  document.getElementById('btnPrev')?.addEventListener('click', () => { if (usersPage > 1) { usersPage--; renderUsersTable(); } });
  document.getElementById('btnNext')?.addEventListener('click', () => { if (usersPage * PAGE_SIZE < allUsers.length) { usersPage++; renderUsersTable(); } });
});

async function searchUsers() {
  const name  = document.getElementById('searchName').value.trim();
  const email = document.getElementById('searchEmail').value.trim();
  const id    = document.getElementById('searchId').value.trim();
  try {
    if (id)    { const u = await api('GET', `/api/user-service/v1/admin/finduserbyid/${id}`); allUsers = u ? [u] : []; }
    else if (email)  { allUsers = await api('GET', `/api/user-service/v1/admin/findbyemail?email=${encodeURIComponent(email)}`) || []; }
    else if (name)   { allUsers = await api('GET', `/api/user-service/v1/admin/search?query=${encodeURIComponent(name)}`) || []; }
    else { await loadUsers(); return; }
    usersPage = 1;
    renderUsersTable();
  } catch (e) { toast(e.message, 'error'); }
}

window.updateRole = async function(id, role) {
  try {
    await api('PATCH', `/api/user-service/v1/admin/updaterole/${id}?role=${role}`);
    toast(`Rol güncellendi: ${role}`, 'success');
    const u = allUsers.find(x => x.id === id);
    if (u) u.role = role;
  } catch (e) { toast(e.message, 'error'); renderUsersTable(); }
};

window.toggleStatus = async function(id, activate) {
  try {
    await api('POST', `/api/user-service/v1/admin/users/${id}/${activate ? 'activate' : 'deactivate'}`);
    toast(`Kullanıcı ${activate ? 'aktifleştirildi' : 'pasifleştirildi'}`, 'success');
    const u = allUsers.find(x => x.id === id);
    if (u) u.active = activate;
    renderUsersTable();
  } catch (e) { toast(e.message, 'error'); }
};

// User Detail Modal — admin kendi hesabı yerine hedef kullanıcının UUID'si ile sorguluyor
window.showUserDetail = async function(id) {
  const body = document.getElementById('modalUserDetailBody');
  body.innerHTML = `<div class="empty-msg"><div class="loader-ring"></div> Yükleniyor...</div>`;
  openModal('modalUserDetail');

  try {
    const u = allUsers.find(x => x.id === id);
    // Balance-info endpoint admin header ile kullanıcı UUID'sini geçiyor
    let balData = { money: null, blockedMoney: null, userIban: null };
    try {
      const targetUUID = u?.keycloackUUID || u?.id || id;
      const balRes = await fetch('/api/money-service/v1/accounts/balance-info', {
        headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token()}`, 'X-User-KeycloakUUID': targetUUID }
      });
      if (balRes.ok) balData = await balRes.json();
    } catch (_) {}

    body.innerHTML = `
      <div class="sub-heading">Kişisel Bilgiler</div>
      <div class="kv-row"><span class="kv-key">ID</span><span class="kv-val mono" style="font-size:11px;">${u?.id || id}</span></div>
      <div class="kv-row"><span class="kv-key">Keycloak UUID</span><span class="kv-val mono" style="font-size:11px;">${u?.keycloackUUID || '-'}</span></div>
      <div class="kv-row"><span class="kv-key">Ad Soyad</span><span class="kv-val">${u?.name || '-'} ${u?.surname || ''}</span></div>
      <div class="kv-row"><span class="kv-key">E-posta</span><span class="kv-val">${u?.mail || '-'}</span></div>
      <div class="kv-row"><span class="kv-key">Rol</span><span class="kv-val">${badge(u?.role)}</span></div>
      <div class="kv-row"><span class="kv-key">Durum</span><span class="kv-val">${u?.active ? '<span class="badge badge-success">Aktif</span>' : '<span class="badge badge-muted">Pasif</span>'}</span></div>
      <div class="kv-row"><span class="kv-key">Kayıt Tarihi</span><span class="kv-val">${formatDate(u?.createdAt)}</span></div>
      <div class="sub-heading" style="margin-top:16px;">Hesap Bilgileri</div>
      <div class="kv-row"><span class="kv-key">IBAN</span><span class="kv-val mono">${balData.userIban || '-'}</span></div>
      <div class="kv-row"><span class="kv-key">Bakiye</span><span class="kv-val" style="color:var(--success);font-size:16px;">${formatMoney(balData.money)}</span></div>
      <div class="kv-row"><span class="kv-key">Bloke Bakiye</span><span class="kv-val" style="color:var(--warning);">${formatMoney(balData.blockedMoney)}</span></div>
    `;
  } catch (e) {
    body.innerHTML = `<div class="empty-msg" style="color:var(--danger);">${e.message}</div>`;
  }
};

window.showResetPwd = function(id) {
  document.getElementById('resetPwdUserId').value = id;
  document.getElementById('newPwdInput').value = '';
  openModal('modalResetPwd');
};

window.showDelete = function(id) {
  document.getElementById('btnConfirmDelete').onclick = () => confirmDelete(id);
  openModal('modalDelete');
};

async function confirmDelete(id) {
  try {
    await api('DELETE', `/api/user-service/v1/admin/deleteuser/${id}`);
    toast('Kullanıcı silindi', 'success');
    closeModal('modalDelete');
    allUsers = allUsers.filter(u => u.id !== id);
    renderUsersTable();
  } catch (e) { toast(e.message, 'error'); }
}

document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('btnConfirmResetPwd')?.addEventListener('click', async () => {
    const id = document.getElementById('resetPwdUserId').value;
    const pwd = document.getElementById('newPwdInput').value;
    if (!pwd || pwd.length < 4) { toast('Şifre en az 4 karakter olmalı', 'warning'); return; }
    try {
      await api('POST', `/api/user-service/v1/admin/users/${id}/reset-password`, { newPassword: pwd });
      toast('Şifre sıfırlandı', 'success');
      closeModal('modalResetPwd');
    } catch (e) { toast(e.message, 'error'); }
  });
});

// ═══════════════════════════════════════════════════════════════
// 3. TRANSACTIONS
// ═══════════════════════════════════════════════════════════════
function isoOffset(days) {
  const d = new Date();
  d.setDate(d.getDate() + days);
  return d.toISOString().slice(0, 16);
}

function initTxPage() {
  if (!document.getElementById('txEnd').value) {
    document.getElementById('txStart').value = isoOffset(-30);
    document.getElementById('txEnd').value   = isoOffset(1);
  }
  loadTransactions();
}

document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('btnTxFilter')?.addEventListener('click', loadTransactions);
  document.getElementById('btnShowStuck')?.addEventListener('click', loadStuckTransactions);
  document.getElementById('btnTopVolume')?.addEventListener('click', loadTopVolume);
});

async function loadTransactions() {
  const start = document.getElementById('txStart').value;
  const end   = document.getElementById('txEnd').value;
  if (!start || !end) { toast('Tarih aralığı seçin', 'warning'); return; }
  renderTxTable(null, 'loading');
  try {
    const txs = await api('GET', `/api/transaction-service/v1/transactions/daterange?startDate=${start}&endDate=${end}`) || [];
    updateTxKpis(txs);
    renderTxTable(txs);
  } catch (e) {
    renderTxTable([], 'error', e.message);
  }
}

async function loadStuckTransactions() {
  renderTxTable(null, 'loading');
  try {
    const txs = await api('GET', '/api/transaction-service/v1/admin/stuck?olderThanMinutes=30') || [];
    toast(`${txs.length} takılı işlem bulundu`, txs.length > 0 ? 'warning' : 'success');
    updateTxKpis(txs);
    renderTxTable(txs);
  } catch(e) { toast(e.message, 'error'); }
}

async function loadTopVolume() {
  renderTxTable(null, 'loading');
  try {
    const txs = await api('GET', '/api/transaction-service/v1/admin/stats/top-by-volume?limit=10') || [];
    updateTxKpis(txs);
    renderTxTable(txs);
  } catch(e) { toast(e.message, 'error'); }
}

function updateTxKpis(txs) {
  let vol = 0, errs = 0;
  txs.forEach(t => { vol += (t.money || 0); if (t.error) errs++; });
  document.getElementById('txCount').textContent = txs.length;
  document.getElementById('txVol').textContent   = formatMoney(vol);
  document.getElementById('txErrs').textContent  = errs;
}

function renderTxTable(txs, state, errMsg) {
  const tbody = document.getElementById('txTbody');
  if (state === 'loading') {
    tbody.innerHTML = `<tr><td colspan="9" class="empty-msg"><div class="loader-ring"></div> Yükleniyor...</td></tr>`;
    return;
  }
  if (!txs || !txs.length) {
    tbody.innerHTML = `<tr><td colspan="9" class="empty-msg">${errMsg ? '❌ ' + errMsg : 'İşlem bulunamadı.'}</td></tr>`;
    return;
  }
  tbody.innerHTML = txs.map(t => `
    <tr ${t.error ? 'style="background:var(--danger-light);"' : ''}>
      <td style="font-size:12px;">${formatDate(t.localDateTime)}</td>
      <td>${trunc(t.id, 8)}</td>
      <td>${badge(t.transactionType)}</td>
      <td style="font-weight:700;">${formatMoney(t.money)}</td>
      <td class="mono" style="font-size:11px;">${t.senderIban ? trunc(t.senderIban, 14) : '-'}</td>
      <td>${t.receiverName || t.receiverIban ? (t.receiverName || trunc(t.receiverIban, 10)) : '-'}</td>
      <td>${badge(t.status)}</td>
      <td>${t.error ? '<span class="badge badge-danger"><i class="ph-fill ph-x-circle"></i> Hata</span>' : '<span class="badge badge-muted">—</span>'}</td>
      <td><a class="action-link" onclick='showTxDetail(${JSON.stringify(t)})'>Detay</a></td>
    </tr>
  `).join('');
}

window.showTxDetail = function(t) {
  const body = document.getElementById('modalTxDetailBody');
  const fields = [
    ['ID', t.id], ['Event ID', t.eventId], ['İşlem Tipi', t.transactionType],
    ['Durum', t.status], ['Durum Açıklaması', t.statusDescription],
    ['Tutar', formatMoney(t.money)], ['Tarih', formatDate(t.localDateTime)],
    ['Hata', t.error ? 'Evet' : 'Hayır'], ['Hata Açıklaması', t.errorDescription],
    ['Para Bloke', t.isMoneyBlocked], ['User Validation', t.userValidation],
    ['Gönderen ID', t.senderUserId], ['Gönderen Adı', t.senderName], ['Gönderen Soyad', t.senderSurname],
    ['Gönderen E-posta', t.senderEmail], ['Gönderen IBAN', t.senderIban],
    ['Alıcı ID', t.receiverUserId], ['Alıcı Adı', t.receiverName], ['Alıcı Soyad', t.receiverSurname],
    ['Alıcı E-posta', t.receiverEmail], ['Alıcı IBAN', t.receiverIban],
    ['Açıklama', t.description]
  ];
  body.innerHTML = fields.map(([k, v]) => `
    <div class="kv-row">
      <span class="kv-key">${k}</span>
      <span class="kv-val">${v !== undefined && v !== null && v !== '' ? v : '—'}</span>
    </div>
  `).join('');
  openModal('modalTxDetail');
};

// ═══════════════════════════════════════════════════════════════
// 4. ACCOUNT OPS
// ═══════════════════════════════════════════════════════════════
let opUserId = null;

document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('btnLoadAllAccounts')?.addEventListener('click', loadAllAccounts);
  document.getElementById('btnRefreshTop')?.addEventListener('click', loadTopAccounts);
  document.getElementById('btnUnblock')?.addEventListener('click', unblockAccount);
  document.getElementById('btnIbanTxSearch')?.addEventListener('click', searchByIban);
  document.getElementById('btnLookup')?.addEventListener('click', lookupAccount);
  document.getElementById('btnDeposit')?.addEventListener('click', () => opTransaction('deposit'));
  document.getElementById('btnWithdraw')?.addEventListener('click', () => opTransaction('withdraw'));
  document.getElementById('btnIbanLookup')?.addEventListener('click', lookupIban);
  document.getElementById('btnTxHistory')?.addEventListener('click', loadTxHistory);
});

async function lookupAccount() {
  const id = document.getElementById('opUserId').value.trim();
  if (!id) { toast('Kullanıcı ID giriniz', 'warning'); return; }
  try {
    const res = await fetch('/api/money-service/v1/accounts/balance-info', {
      headers: { ...authH(), 'X-User-KeycloakUUID': id }
    });
    if (res.status === 401) { window.location.href = '/login.html'; return; }
    if (!res.ok) throw new Error('Hesap bulunamadı');
    const data = await res.json();
    document.getElementById('opIban').textContent    = data.userIban || '-';
    document.getElementById('opBal').textContent     = formatMoney(data.money);
    document.getElementById('opBlocked').textContent = formatMoney(data.blockedMoney);
    document.getElementById('opUuid').textContent    = id;
    document.getElementById('accountBox').style.display = 'block';
    opUserId = id;
    toast('Hesap bulundu', 'success');
  } catch (e) {
    document.getElementById('accountBox').style.display = 'none';
    toast(e.message, 'error');
  }
}

async function opTransaction(type) {
  if (!opUserId) return;
  const amt = parseFloat(document.getElementById('opAmount').value);
  if (!amt || amt <= 0) { toast('Geçerli bir tutar girin', 'warning'); return; }
  try {
    const endpoint = type === 'deposit' ? '/api/money-service/v1/accounts/depositByUserId' : '/api/money-service/v1/accounts/withdrawByUserId';
    await api('POST', endpoint, { userId: opUserId, amount: amt });
    toast(`${type === 'deposit' ? '✅ Para yatırıldı' : '✅ Para çekildi'}: ${formatMoney(amt)}`, 'success');
    document.getElementById('opAmount').value = '';
    await lookupAccount();
  } catch (e) { toast(e.message, 'error'); }
}

async function lookupIban() {
  const id = document.getElementById('ibanLookupId').value.trim();
  if (!id) return;
  try {
    const res = await api('POST', '/api/money-service/v1/accounts/getUserIbanWithUserId', { userId: id });
    const iban = res?.userIban || res?.iban || (typeof res === 'string' ? res : '-');
    document.getElementById('ibanResult').textContent = iban;
    document.getElementById('ibanBox').style.display = 'block';
  } catch (e) { toast(e.message, 'error'); }
}

async function loadTxHistory() {
  const id = document.getElementById('txHistoryId').value.trim();
  if (!id) { toast('Kullanıcı ID giriniz', 'warning'); return; }
  try {
    const txs = await api('GET', `/api/transaction-service/v1/transactions/gettransactionhistorywithid?id=${id}`) || [];
    const tbody = document.getElementById('txHistoryTbody');
    if (!txs.length) { tbody.innerHTML = '<tr><td colspan="4" class="empty-msg">İşlem bulunamadı.</td></tr>'; }
    else {
      tbody.innerHTML = txs.map(t => `
        <tr>
          <td>${badge(t.transactionType)}</td>
          <td style="font-weight:700;">${formatMoney(t.money)}</td>
          <td>${badge(t.status)}</td>
          <td style="font-size:11px;">${formatDate(t.localDateTime)}</td>
        </tr>
      `).join('');
    }
    document.getElementById('txHistoryResult').style.display = 'block';
  } catch (e) { toast(e.message, 'error'); }
}

async function loadAllAccounts() {
  const card  = document.getElementById('allAccountsCard');
  const tbody = document.getElementById('allAccountsTbody');
  card.style.display = 'block';
  tbody.innerHTML = `<tr><td colspan="5" class="empty-msg"><div class="loader-ring"></div> Yükleniyor...</td></tr>`;
  try {
    const data = await api('GET', '/api/money-service/v1/admin/accounts?page=0&size=100') || [];
    document.getElementById('accountsTotal').textContent = `${data.length} hesap`;
    if (!data.length) { tbody.innerHTML = `<tr><td colspan="5" class="empty-msg">Hesap bulunamadı.</td></tr>`; return; }
    tbody.innerHTML = data.map(a => `
      <tr>
        <td class="mono" style="font-size:11px;">${trunc(a.userId, 12)}</td>
        <td class="mono" style="font-size:11px;">${a.userIban || '-'}</td>
        <td style="font-weight:700;color:var(--success);">${formatMoney(a.money)}</td>
        <td style="color:var(--warning);">${formatMoney(a.blockedMoney)}</td>
        <td><a class="action-link" onclick="prefillLookup('${a.userId}')">Sorgula</a></td>
      </tr>
    `).join('');
  } catch(e) { toast(e.message, 'error'); }
}

async function loadTopAccounts() {
  const tbody = document.getElementById('topAccountsTbody');
  if (!tbody) return;
  tbody.innerHTML = `<tr><td colspan="6" class="empty-msg"><div class="loader-ring"></div></td></tr>`;
  try {
    const data = await api('GET', '/api/money-service/v1/admin/accounts/top?limit=10') || [];
    if (!data.length) { tbody.innerHTML = `<tr><td colspan="6" class="empty-msg">Veri yok.</td></tr>`; return; }
    tbody.innerHTML = data.map((a, i) => `
      <tr>
        <td><strong style="color:var(--warning);">#${i+1}</strong></td>
        <td class="mono" style="font-size:11px;">${trunc(a.userId, 12)}</td>
        <td class="mono" style="font-size:11px;">${a.userIban || '-'}</td>
        <td style="font-weight:800;color:var(--success);">${formatMoney(a.balance)}</td>
        <td style="color:var(--warning);">${formatMoney(a.blockedMoney)}</td>
        <td><a class="action-link" onclick="prefillLookup('${a.userId}')">Sorgula</a></td>
      </tr>
    `).join('');
  } catch(e) { toast(e.message, 'error'); }
}

window.prefillLookup = function(userId) {
  document.getElementById('opUserId').value = userId;
  lookupAccount();
};

async function unblockAccount() {
  if (!opUserId) { toast('Önce bir hesap sorgulayın', 'warning'); return; }
  try {
    const res = await api('POST', '/api/money-service/v1/admin/account/unblock', { userId: opUserId });
    toast(`✅ Bloke kaldırıldı: ${formatMoney(res.unblocked)}`, 'success');
    await lookupAccount();
    await loadTopAccounts();
  } catch(e) { toast(e.message, 'error'); }
}

async function searchByIban() {
  const iban = document.getElementById('ibanTxSearch').value.trim();
  if (!iban) { toast('IBAN giriniz', 'warning'); return; }
  try {
    const txs = await api('GET', `/api/transaction-service/v1/admin/byiban?iban=${encodeURIComponent(iban)}`) || [];
    const tbody = document.getElementById('ibanTxTbody');
    document.getElementById('ibanTxResult').style.display = 'block';
    if (!txs.length) { tbody.innerHTML = '<tr><td colspan="5" class="empty-msg">İşlem bulunamadı.</td></tr>'; return; }
    tbody.innerHTML = txs.map(t => {
      const isOwnSender  = t.senderIban === iban;
      const counterpart  = isOwnSender ? (t.receiverName || t.receiverIban || '-') : (t.senderName || t.senderIban || '-');
      return `<tr>
        <td>${badge(t.transactionType)}</td>
        <td style="font-weight:700;">${formatMoney(t.money)}</td>
        <td style="font-size:11px;">${counterpart}</td>
        <td>${badge(t.status)}</td>
        <td style="font-size:11px;">${formatDate(t.localDateTime)}</td>
      </tr>`;
    }).join('');
  } catch(e) { toast(e.message, 'error'); }
}


// ═══════════════════════════════════════════════════════════════
// 5. FRAUD & ERRORS
// ═══════════════════════════════════════════════════════════════
document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('btnRefreshFraud')?.addEventListener('click', loadFraud);
});

async function loadFraud() {
  const tbody = document.getElementById('fraudTbody');
  tbody.innerHTML = `<tr><td colspan="9" class="empty-msg"><div class="loader-ring"></div> Yükleniyor...</td></tr>`;
  try {
    const errors = await api('GET', '/api/transaction-service/v1/transactions/errors') || [];
    document.getElementById('errTotal').textContent = errors.length;

    // Error badge on nav
    const navBadge = document.getElementById('errBadge');
    if (navBadge) { navBadge.style.display = errors.length > 0 ? 'inline-block' : 'none'; }

    if (!errors.length) {
      tbody.innerHTML = `<tr><td colspan="9" class="empty-msg">Hata kaydı bulunamadı.</td></tr>`;
      document.getElementById('errRate').textContent = '0%';
      document.getElementById('errCommon').textContent = '—';
      return;
    }

    // Error rate
    try {
      const all = await api('GET', `/api/transaction-service/v1/transactions/daterange?startDate=${isoOffset(-30)}&endDate=${isoOffset(1)}`);
      const rate = all?.length > 0 ? ((errors.length / all.length) * 100).toFixed(1) + '%' : '—';
      document.getElementById('errRate').textContent = rate;
    } catch (_) { document.getElementById('errRate').textContent = '—'; }

    // Most common error type
    const typeCounts = {};
    errors.forEach(e => { typeCounts[e.transactionType] = (typeCounts[e.transactionType] || 0) + 1; });
    const mostCommon = Object.entries(typeCounts).sort(([,a],[,b])=>b-a)[0];
    document.getElementById('errCommon').textContent = mostCommon ? mostCommon[0] : '—';

    tbody.innerHTML = errors.map(e => `
      <tr>
        <td>${trunc(e.id, 8)}</td>
        <td>${badge(e.transactionType)}</td>
        <td style="font-weight:700;">${formatMoney(e.money)}</td>
        <td style="max-width:180px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" title="${e.errorDescription||''}">${e.errorDescription || '-'}</td>
        <td>${e.senderName || '-'}</td>
        <td>${e.receiverName || '-'}</td>
        <td style="font-size:11px;">${formatDate(e.localDateTime)}</td>
        <td>${badge(e.status)}</td>
        <td><a class="action-link" onclick='showTxDetail(${JSON.stringify(e)})'>Detay</a></td>
      </tr>
    `).join('');
  } catch (e) {
    tbody.innerHTML = `<tr><td colspan="9" class="empty-msg" style="color:var(--danger);">Hata: ${e.message}</td></tr>`;
  }
}

// ═══════════════════════════════════════════════════════════════
// 6. SYSTEM HEALTH
// ═══════════════════════════════════════════════════════════════
const HEALTH_SVCS = [
  { name: 'Gateway',             path: '/actuator/health' },
  { name: 'User Service',        path: '/api/user-service/actuator/health' },
  { name: 'Money Service',       path: '/api/money-service/actuator/health' },
  { name: 'Transaction Service', path: '/api/transaction-service/actuator/health' },
  { name: 'Auth Service',        path: '/api/auth-service/actuator/health' },
  { name: 'Fraud Service',       path: '/api/fraud-service/actuator/health' },
];

document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('btnRefreshHealth')?.addEventListener('click', checkHealth);
});

async function checkHealth() {
  const grid = document.getElementById('healthGrid');
  if (!grid) return;

  grid.innerHTML = HEALTH_SVCS.map(svc => `
    <div class="health-card" id="hc-${svc.name.replace(/\s/g, '')}">
      <div class="health-header">
        <span class="health-name">${svc.name}</span>
        <div class="loader-ring"></div>
      </div>
      <div class="health-meta"><span>Kontrol ediliyor...</span><span>—</span></div>
    </div>
  `).join('');

  for (const svc of HEALTH_SVCS) {
    const cid = `hc-${svc.name.replace(/\s/g, '')}`;
    const t0 = performance.now();
    try {
      const res = await api('GET', svc.path);
      const dur = Math.round(performance.now() - t0);
      const isUp = res?.status === 'UP';
      const card = document.getElementById(cid);
      if (card) {
        card.classList.toggle('up', isUp);
        card.classList.toggle('down', !isUp);
        card.innerHTML = `
          <div class="health-header">
            <span class="health-name">${svc.name}</span>
            <i class="ph-fill ${isUp ? 'ph-check-circle' : 'ph-x-circle'}" style="color:${isUp ? 'var(--success)' : 'var(--danger)'}; font-size:22px;"></i>
          </div>
          <div class="health-meta">
            <span>${isUp ? '<span class="badge badge-success">UP</span>' : '<span class="badge badge-danger">DOWN</span>'}</span>
            <span>${dur}ms</span>
          </div>
        `;
      }
    } catch (_) {
      const card = document.getElementById(cid);
      if (card) {
        card.classList.add('down');
        card.innerHTML = `
          <div class="health-header">
            <span class="health-name">${svc.name}</span>
            <i class="ph-fill ph-warning-circle" style="color:var(--danger); font-size:22px;"></i>
          </div>
          <div class="health-meta">
            <span><span class="badge badge-danger">UNREACHABLE</span></span>
            <span>—</span>
          </div>
        `;
      }
    }
  }
}

// ═══════════════════════════════════════════════════════════════
// 7. LIVE LOGS — kubectl SSE streaming
// ═══════════════════════════════════════════════════════════════
let logEventSource = null;

document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.log-svc-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.log-svc-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      startLogs(btn.getAttribute('data-svc'));
    });
  });
  document.getElementById('btnStopLog')?.addEventListener('click', stopLogs);
});

function startLogs(svc) {
  stopLogs();
  const tail = document.getElementById('logTailCount')?.value || '200';
  const dot  = document.getElementById('logStatusDot');
  const txt  = document.getElementById('logStatusText');

  document.getElementById('btnStopLog').style.display = 'inline-flex';
  const term = document.getElementById('log-terminal');
  term.innerHTML = `<span class="log-line-system">// [${svc}] kubectl log stream başlatılıyor (tail=${tail})...</span><br>`;

  if (dot) { dot.style.background = 'var(--warning)'; }
  if (txt) txt.textContent = `${svc} bağlanıyor...`;

  // EventSource header gönderemez — token query param olarak gönderiliyor
  // SseTokenWebFilter bu token'ı Authorization header'a inject eder
  const tok = token();
  const url = `/api/gateway/admin/logs/${svc}?tail=${tail}&token=${encodeURIComponent(tok || '')}`;
  logEventSource = new EventSource(url);

  logEventSource.addEventListener('connected', e => {
    logLine(term, 'system', e.data);
    if (dot) dot.style.background = 'var(--success)';
    if (txt) txt.textContent = `${svc} — canlı`;
  });

  logEventSource.addEventListener('log', e => {
    const text = e.data || '';
    if (text.includes('ERROR')) {
      logLine(term, 'error', text);
    } else if (text.includes('WARN')) {
      logLine(term, 'warn', text);
    } else {
      logLine(term, 'info', text);
    }
  });

  logEventSource.addEventListener('disconnected', e => {
    logLine(term, 'system', e.data);
    if (dot) dot.style.background = 'var(--text-muted)';
    if (txt) txt.textContent = 'Bağlı değil';
    stopLogs();
  });

  // Sunucudan gelen özel 'error' event — sadece log kaydeder, bağlantıyı kesmez
  logEventSource.addEventListener('error', e => {
    if (e.data) logLine(term, 'error', e.data);
  });

  // Native SSE bağlantı hatası — sadece kalıcı kapanmada durdur
  logEventSource.onerror = () => {
    if (!logEventSource) return;
    const state = logEventSource.readyState;
    if (state === EventSource.CLOSED) {
      // Kalıcı kapanma
      logLine(term, 'warn', `[${svc}] SSE bağlantısı kalıcı olarak kapandı.`);
      if (dot) dot.style.background = 'var(--danger)';
      if (txt) txt.textContent = 'Bağlantı kesildi';
      stopLogs();
    } else if (state === EventSource.CONNECTING) {
      // Browser otomatik yeniden bağlanma deniyor — bekle
      if (dot) dot.style.background = 'var(--warning)';
      if (txt) txt.textContent = `${svc} yeniden bağlanıyor...`;
      logLine(term, 'warn', `[${svc}] Bağlantı kesildi, yeniden bağlanılıyor...`);
    }
  };
}

function stopLogs() {
  if (logEventSource) {
    logEventSource.close();
    logEventSource = null;
  }
  const btn = document.getElementById('btnStopLog');
  if (btn) btn.style.display = 'none';
  document.querySelectorAll('.log-svc-btn').forEach(b => b.classList.remove('active'));
}

function logLine(term, type, text) {
  const div = document.createElement('div');
  const time = new Date().toLocaleTimeString('tr-TR');
  div.className = `log-line-${type}`;
  // Timestamp ekle ama kubectl zaten timestamp veriyorsa tekrar ekleme
  const hasTs = /^\d{4}-\d{2}/.test(text);
  div.textContent = hasTs ? text : `[${time}] ${text}`;
  term.appendChild(div);
  term.scrollTop = term.scrollHeight;
  // Max 500 satir tut
  while (term.children.length > 500) {
    term.removeChild(term.firstChild);
  }
}

window.clearLogs = function() {
  const term = document.getElementById('log-terminal');
  if (term) term.innerHTML = '<span class="log-line-system">// Terminal temizlendi — bir servis seçin.</span>';
  stopLogs();
};

// ═══════════════════════════════════════════════════════════════
// INIT
// ═══════════════════════════════════════════════════════════════
window.addEventListener('DOMContentLoaded', () => {
  if (!checkAdmin()) return;
  initSidebarUser();
  attachNav();
  startClock();
  navigateTo('dashboard');
});
