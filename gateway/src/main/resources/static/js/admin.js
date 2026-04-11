/* =============================================================
   SpringBank — Admin Panel JavaScript
   Tüm admin işlemleri: kullanıcı yönetimi, işlem monitörü,
   hesap ops, fraud, sistem sağlığı, canlı loglar, aktivite log
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
  el.innerHTML = `<i class="ph-fill ${icons[type]||'ph-info'}" style="font-size:18px;color:${colors[type]||'var(--primary)'}; flex-shrink:0;"></i><span>${escHtml(msg)}</span>`;
  stack.appendChild(el);
  setTimeout(() => { el.style.opacity = '0'; el.style.transition = 'opacity 0.3s'; setTimeout(() => el.remove(), 300); }, 4000);
}

// ─── XSS Protection ──────────────────────────────────────────
function escHtml(str) {
  if (str === undefined || str === null) return '';
  const d = document.createElement('div');
  d.textContent = String(str);
  return d.innerHTML;
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
  const escaped = escHtml(str);
  if (str.length <= len) return `<span class="mono">${escaped}</span>`;
  return `<span class="mono" title="${escaped}">${escHtml(str.slice(0, len))}…</span>`;
}

// ─── Status Badge ─────────────────────────────────────────────
function badge(val) {
  if (val === undefined || val === null) return '<span class="badge badge-muted">—</span>';
  const s = String(val).toUpperCase();
  const escaped = escHtml(val);

  if (['COMPLETED', 'SUCCESS', 'ACTIVE', 'UP', 'TRUE', 'MONEY_TRANSFER_SUCCESS', 'TRANSFER_SUCCESS'].includes(s) || val === true)
    return `<span class="badge badge-success"><i class="ph-fill ph-check-circle"></i> ${escaped}</span>`;
  if (['FAILED', 'ERROR', 'DOWN', 'FALSE', 'DENIED', 'INACTIVE', 'BLOCK_MONEY_FAILED'].includes(s) || val === false)
    return `<span class="badge badge-danger"><i class="ph-fill ph-x-circle"></i> ${escaped}</span>`;
  if (['PENDING', 'PROGRESS', 'FRAUD_REVIEW', 'BLOCK_MONEY', 'CREATED'].includes(s))
    return `<span class="badge badge-warning"><i class="ph ph-clock"></i> ${escaped}</span>`;
  if (['DEPOSIT'].includes(s))  return `<span class="badge badge-teal"><i class="ph ph-plus"></i> ${escaped}</span>`;
  if (['WITHDRAW'].includes(s)) return `<span class="badge badge-warning"><i class="ph ph-minus"></i> ${escaped}</span>`;
  if (['TRANSFER', 'EFT'].includes(s)) return `<span class="badge badge-info"><i class="ph ph-arrows-left-right"></i> ${escaped}</span>`;
  if (['ADMIN'].includes(s)) return `<span class="badge badge-purple">${escaped}</span>`;
  if (['USER'].includes(s))  return `<span class="badge badge-info">${escaped}</span>`;
  return `<span class="badge badge-muted">${escaped}</span>`;
}

function openModal(id) { document.getElementById(id)?.classList.add('show'); }
function closeModal(id) { document.getElementById(id)?.classList.remove('show'); }

window.closeModal = closeModal;

document.addEventListener('keydown', e => {
  if (e.key === 'Escape') document.querySelectorAll('.modal-bg.show').forEach(m => m.classList.remove('show'));
});

// ─── Activity Log ─────────────────────────────────────────────
const activityItems = [];

function addActivity(type, text) {
  const item = {
    time: new Date().toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
    date: new Date().toLocaleDateString('tr-TR'),
    type,
    text
  };
  activityItems.unshift(item);
  if (activityItems.length > 200) activityItems.pop();
  renderActivity();
}

function renderActivity() {
  const log = document.getElementById('activityLog');
  const count = document.getElementById('activityCount');
  if (!log) return;
  if (count) count.textContent = `${activityItems.length} kayıt`;

  if (!activityItems.length) {
    log.innerHTML = `<div class="activity-item"><span class="activity-type-icon" style="color:var(--text-muted);"><i class="ph ph-info"></i></span><span class="activity-text">Henüz aktivite kaydı yok.</span></div>`;
    return;
  }

  const typeIcons = {
    info: { icon: 'ph-info', color: 'var(--primary)' },
    success: { icon: 'ph-check-circle', color: 'var(--success)' },
    warning: { icon: 'ph-warning', color: 'var(--warning)' },
    error: { icon: 'ph-x-circle', color: 'var(--danger)' },
    user: { icon: 'ph-user', color: 'var(--primary)' },
    money: { icon: 'ph-currency-circle-dollar', color: 'var(--success)' },
  };

  log.innerHTML = activityItems.map(a => {
    const t = typeIcons[a.type] || typeIcons.info;
    return `<div class="activity-item">
      <span class="activity-type-icon" style="color:${t.color};"><i class="ph-fill ${t.icon}"></i></span>
      <span class="activity-time">${a.time}</span>
      <span class="activity-text">${escHtml(a.text)}</span>
    </div>`;
  }).join('');
}

// ─── CSV Export ───────────────────────────────────────────────
function exportCSV(filename, headers, rows) {
  const bom = '\uFEFF';
  const csv = bom + [headers.join(','), ...rows.map(r => r.map(c => `"${String(c ?? '').replace(/"/g, '""')}"`).join(','))].join('\n');
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
  addActivity('info', `${filename} CSV dosyası indirildi`);
  toast(`${filename} indirildi`, 'success');
}

// ─── Clock ────────────────────────────────────────────────────
function startClock() {
  const el = document.getElementById('topbarClock');
  const tick = () => { if (el) el.textContent = new Date().toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit', second: '2-digit' }); };
  tick();
  setInterval(tick, 1000);
}

// ─── Dark Mode ────────────────────────────────────────────────
function initDarkMode() {
  const saved = localStorage.getItem('sb_theme') || 'light';
  if (saved === 'dark') document.documentElement.setAttribute('data-theme', 'dark');
  updateDarkModeIcon();

  document.getElementById('btnDarkMode')?.addEventListener('click', () => {
    const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
    if (isDark) {
      document.documentElement.removeAttribute('data-theme');
      localStorage.setItem('sb_theme', 'light');
    } else {
      document.documentElement.setAttribute('data-theme', 'dark');
      localStorage.setItem('sb_theme', 'dark');
    }
    updateDarkModeIcon();
    // Recreate charts with correct theme
    if (currentPage === 'dashboard') initDashboard();
  });
}

function updateDarkModeIcon() {
  const btn = document.getElementById('btnDarkMode');
  if (!btn) return;
  const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
  btn.innerHTML = isDark ? '<i class="ph ph-sun"></i>' : '<i class="ph ph-moon"></i>';
  btn.title = isDark ? 'Açık Tema' : 'Koyu Tema';
}

// ─── Auto Refresh ────────────────────────────────────────────
let autoRefreshTimer = null;

function initAutoRefresh() {
  const btn = document.getElementById('btnAutoRefresh');
  if (!btn) return;
  btn.addEventListener('click', () => {
    if (autoRefreshTimer) {
      clearInterval(autoRefreshTimer);
      autoRefreshTimer = null;
      btn.classList.remove('active');
      btn.title = 'Otomatik Yenileme (30s)';
      toast('Otomatik yenileme kapatıldı', 'info');
    } else {
      autoRefreshTimer = setInterval(() => {
        if (currentPage === 'dashboard') initDashboard();
      }, 30000);
      btn.classList.add('active');
      btn.title = 'Otomatik Yenileme AKTİF — Kapatmak için tıkla';
      toast('Otomatik yenileme açıldı (30s)', 'success');
    }
  });
}

// ─── Mobile Sidebar ──────────────────────────────────────────
function initMobileSidebar() {
  const hamburger = document.getElementById('btnHamburger');
  const sidebar = document.getElementById('sidebar');
  const overlay = document.getElementById('sidebarOverlay');

  const closeSidebar = () => {
    sidebar?.classList.remove('open');
    overlay?.classList.remove('show');
  };

  hamburger?.addEventListener('click', () => {
    sidebar?.classList.toggle('open');
    overlay?.classList.toggle('show');
  });

  overlay?.addEventListener('click', closeSidebar);

  // Close on nav item click (mobile)
  document.querySelectorAll('.nav-item[data-page]').forEach(btn => {
    btn.addEventListener('click', () => {
      if (window.innerWidth <= 768) closeSidebar();
    });
  });
}

// ─── Navigation ───────────────────────────────────────────────
const PAGE_TITLES = {
  dashboard: 'Dashboard', users: 'Kullanıcı Yönetimi',
  transactions: 'İşlem Monitörü', accounts: 'Hesap İşlemleri',
  fraud: 'Hata & Fraud Monitörü', health: 'Sistem Sağlığı',
  logs: 'Canlı Loglar', activity: 'Aktivite Log'
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
    case 'accounts':     loadTopAccounts(); loadAccountStats(); break;
    case 'fraud':        loadFraud(); break;
    case 'health':       checkHealth(); healthTimer = setInterval(checkHealth, 30000); break;
    case 'logs':         break;
    case 'activity':     renderActivity(); break;
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
    addActivity('warning', 'Admin çıkış yaptı');
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
let dashTxData    = []; // Store for CSV export

async function initDashboard() {
  try {
    const [totalUsers, activeData, roleData, moneySummary, txSummary, dailyStats] = await Promise.allSettled([
      api('GET', '/api/user-service/v1/admin/stats/total'),
      api('GET', '/api/user-service/v1/admin/stats/active'),
      api('GET', '/api/user-service/v1/admin/stats/roles'),
      api('GET', '/api/money-service/v1/admin/stats/summary'),
      api('GET', '/api/transaction-service/v1/admin/stats/summary'),
      api('GET', '/api/transaction-service/v1/admin/stats/daily?days=7')
    ]);

    const total    = totalUsers.status === 'fulfilled'  ? (totalUsers.value || 0) : 0;
    const active   = activeData.status === 'fulfilled'  ? (activeData.value?.active || 0) : 0;
    const roles    = roleData.status === 'fulfilled'    ? (roleData.value || {}) : {};
    const money    = moneySummary.status === 'fulfilled'? (moneySummary.value || {}) : {};
    const txSum    = txSummary.status === 'fulfilled'   ? (txSummary.value || {}) : {};
    const daily    = dailyStats.status === 'fulfilled'  ? (dailyStats.value || []) : [];

    document.getElementById('kpiTotalUsers').textContent  = total;
    document.getElementById('kpiActiveUsers').textContent = active;
    document.getElementById('kpiTodayTx').textContent     = txSum.totalCount || '—';
    document.getElementById('kpiErrorTx').textContent     = txSum.failed || '—';

    const kpiMoney = document.getElementById('kpiTotalMoney');
    if (kpiMoney) kpiMoney.textContent = formatMoney(money.totalBalance);
    const kpiBlocked = document.getElementById('kpiBlockedMoney');
    if (kpiBlocked) kpiBlocked.textContent = formatMoney(money.totalBlockedBalance);

    const errBadge = document.getElementById('errBadge');
    if (errBadge && txSum.failed > 0) errBadge.style.display = 'inline-block';

    renderRoleChart(roles);

    // Use daily stats from backend instead of client-side calculation
    if (daily.length > 0) {
      renderVolChartFromDaily(daily);
    } else if (txSum.countByType) {
      renderVolChartFromSummary(txSum);
    } else {
      showChartEmpty('volChart', 'volChartEmpty');
    }

    // Recent transactions
    const txAll = await api('GET', `/api/transaction-service/v1/transactions/daterange?startDate=${isoOffset(-7)}&endDate=${isoOffset(1)}`).catch(() => []);
    dashTxData = txAll || [];
    renderDashTxTable(dashTxData);
  } catch (e) {
    console.error('[Dashboard]', e);
  }
}

function showChartEmpty(canvasId, emptyId) {
  const canvas = document.getElementById(canvasId);
  const empty = document.getElementById(emptyId);
  if (canvas) canvas.style.display = 'none';
  if (empty) empty.style.display = 'flex';
}

function showChartCanvas(canvasId, emptyId) {
  const canvas = document.getElementById(canvasId);
  const empty = document.getElementById(emptyId);
  if (canvas) canvas.style.display = 'block';
  if (empty) empty.style.display = 'none';
}

function getChartColors() {
  const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
  return {
    grid: isDark ? '#30363D' : '#E1EAF2',
    text: isDark ? '#8B949E' : '#5B6B79',
  };
}

function renderRoleChart(roles) {
  const ctx = document.getElementById('roleChart');
  if (!ctx) return;
  const total = (roles.USER || 0) + (roles.ADMIN || 0);
  if (total === 0) { showChartEmpty('roleChart', 'roleChartEmpty'); return; }
  showChartCanvas('roleChart', 'roleChartEmpty');

  if (roleChartInst) roleChartInst.destroy();
  const colors = getChartColors();
  roleChartInst = new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: ['Kullanıcı', 'Admin'],
      datasets: [{ data: [roles.USER || 0, roles.ADMIN || 0], backgroundColor: ['#026CB6', '#0C9A5D'], borderWidth: 0, hoverOffset: 6 }]
    },
    options: {
      cutout: '68%', responsive: true, maintainAspectRatio: false,
      plugins: { legend: { position: 'bottom', labels: { color: colors.text, font: { family: 'Inter', size: 12 }, padding: 16 } } }
    }
  });
}

function renderVolChartFromDaily(daily) {
  const ctx = document.getElementById('volChart');
  if (!ctx) return;
  if (!daily.length) { showChartEmpty('volChart', 'volChartEmpty'); return; }
  showChartCanvas('volChart', 'volChartEmpty');

  const labels = daily.map(d => {
    const date = new Date(d.date);
    return date.toLocaleDateString('tr-TR', { day: 'numeric', month: 'short' });
  });
  const counts = daily.map(d => d.count || 0);
  const colors = getChartColors();

  if (volChartInst) volChartInst.destroy();
  volChartInst = new Chart(ctx, {
    type: 'bar',
    data: {
      labels,
      datasets: [{ label: 'İşlem', data: counts, backgroundColor: '#026CB6', borderRadius: 5, borderSkipped: false }]
    },
    options: {
      responsive: true, maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: {
        y: { ticks: { color: colors.text, font: { family: 'Inter', size: 11 } }, grid: { color: colors.grid } },
        x: { ticks: { color: colors.text, font: { family: 'Inter', size: 11 } }, grid: { display: false } }
      }
    }
  });
}

function renderVolChartFromSummary(txSum) {
  const ctx = document.getElementById('volChart');
  if (!ctx) return;
  const byType = txSum.countByType || {};
  const total = (byType.DEPOSIT || 0) + (byType.WITHDRAW || 0) + (byType.TRANSFER || 0);
  if (total === 0) { showChartEmpty('volChart', 'volChartEmpty'); return; }
  showChartCanvas('volChart', 'volChartEmpty');

  const colors = getChartColors();
  if (volChartInst) volChartInst.destroy();
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
        y: { ticks: { color: colors.text }, grid: { color: colors.grid } },
        x: { ticks: { color: colors.text }, grid: { display: false } }
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
      <td class="mono" style="font-size:11px;">${t.senderIban ? trunc(t.senderIban, 12) : escHtml(t.senderName || '-')}</td>
      <td>${badge(t.status)}</td>
    </tr>
  `).join('');
}

// Dashboard CSV Export
document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('btnExportDashTx')?.addEventListener('click', () => {
    if (!dashTxData.length) { toast('Export edilecek veri yok', 'warning'); return; }
    const headers = ['Tarih', 'ID', 'Tip', 'Tutar', 'Gönderen', 'Durum'];
    const rows = dashTxData.map(t => [formatDate(t.localDateTime), t.id, t.transactionType, t.money, t.senderIban || t.senderName || '', t.status]);
    exportCSV('dashboard_islemler.csv', headers, rows);
  });
});

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
    addActivity('info', `${allUsers.length} kullanıcı yüklendi`);
  } catch (e) {
    tbody.innerHTML = `<tr><td colspan="8" class="empty-msg" style="color:var(--danger);">Kullanıcılar yüklenemedi: ${escHtml(e.message)}</td></tr>`;
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
  tbody.innerHTML = paged.map((u, idx) => {
    const globalIdx = (usersPage - 1) * PAGE_SIZE + idx;
    return `
    <tr>
      <td>${trunc(u.id, 10)}</td>
      <td>${escHtml(u.name || '-')}</td>
      <td>${escHtml(u.surname || '-')}</td>
      <td>${escHtml(u.mail || '-')}</td>
      <td>
        <select class="role-select" data-user-idx="${globalIdx}" onchange="updateRole(this)">
          <option value="USER"  ${u.role === 'USER'  ? 'selected' : ''}>USER</option>
          <option value="ADMIN" ${u.role === 'ADMIN' ? 'selected' : ''}>ADMIN</option>
        </select>
      </td>
      <td>${u.active ? '<span class="badge badge-success">Aktif</span>' : '<span class="badge badge-muted">Pasif</span>'}</td>
      <td style="font-size:12px;">${formatDate(u.createdAt)}</td>
      <td>
        <button class="action-link" onclick="showUserDetail(${globalIdx})">Detay</button>
        ${u.active
          ? `<button class="action-link orange" onclick="toggleStatus(${globalIdx}, false)">Pasifleştir</button>`
          : `<button class="action-link green" onclick="toggleStatus(${globalIdx}, true)">Aktifleştir</button>`}
        <button class="action-link" onclick="showResetPwd(${globalIdx})">Şifre</button>
        <button class="action-link red" onclick="showDelete(${globalIdx})">Sil</button>
      </td>
    </tr>
  `;
  }).join('');
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
    addActivity('info', `Kullanıcı araması: ${allUsers.length} sonuç`);
  } catch (e) { toast(e.message, 'error'); }
}

// FIXED: Use index-based approach instead of string ID in onclick to prevent XSS
window.updateRole = async function(selectEl) {
  const idx = parseInt(selectEl.dataset.userIdx);
  const u = allUsers[idx];
  if (!u) return;
  const role = selectEl.value;
  try {
    await api('PATCH', `/api/user-service/v1/admin/updaterole/${u.id}?role=${role}`);
    toast(`Rol güncellendi: ${role}`, 'success');
    u.role = role;
    addActivity('user', `${u.name || u.id} kullanıcısının rolü ${role} olarak güncellendi`);
  } catch (e) { toast(e.message, 'error'); renderUsersTable(); }
};

window.toggleStatus = async function(idx, activate) {
  const u = allUsers[idx];
  if (!u) return;
  try {
    await api('POST', `/api/user-service/v1/admin/users/${u.id}/${activate ? 'activate' : 'deactivate'}`);
    toast(`Kullanıcı ${activate ? 'aktifleştirildi' : 'pasifleştirildi'}`, 'success');
    u.active = activate;
    renderUsersTable();
    addActivity('user', `${u.name || u.id} kullanıcısı ${activate ? 'aktifleştirildi' : 'pasifleştirildi'}`);
  } catch (e) { toast(e.message, 'error'); }
};

// User Detail Modal with inline edit
let editingUserId = null;

window.showUserDetail = async function(idx) {
  const u = allUsers[idx];
  if (!u) return;
  editingUserId = u.id;
  const body = document.getElementById('modalUserDetailBody');
  const foot = document.getElementById('modalUserDetailFoot');
  body.innerHTML = `<div class="empty-msg"><div class="loader-ring"></div> Yükleniyor...</div>`;
  if (foot) foot.style.display = 'none';
  openModal('modalUserDetail');

  try {
    let balData = { money: null, blockedMoney: null, userIban: null };
    try {
      const targetUUID = u?.keycloakUUID || u?.id;
      const balRes = await fetch('/api/money-service/v1/accounts/balance-info', {
        headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token()}`, 'X-User-KeycloakUUID': targetUUID }
      });
      if (balRes.ok) balData = await balRes.json();
    } catch (_) {}

    body.innerHTML = `
      <div class="sub-heading">Kişisel Bilgiler</div>
      <div class="kv-row"><span class="kv-key">ID</span><span class="kv-val mono" style="font-size:11px;">${escHtml(u?.id)}</span></div>
      <div class="kv-row"><span class="kv-key">Keycloak UUID</span><span class="kv-val mono" style="font-size:11px;">${escHtml(u?.keycloakUUID || '-')}</span></div>
      <div class="form-group" style="margin-top:12px;">
        <label class="form-label">Ad</label>
        <input type="text" class="form-control" id="editUserName" value="${escHtml(u?.name || '')}">
      </div>
      <div class="form-group">
        <label class="form-label">Soyad</label>
        <input type="text" class="form-control" id="editUserSurname" value="${escHtml(u?.surname || '')}">
      </div>
      <div class="form-group">
        <label class="form-label">E-posta</label>
        <input type="email" class="form-control" id="editUserEmail" value="${escHtml(u?.mail || '')}">
      </div>
      <div class="kv-row"><span class="kv-key">Rol</span><span class="kv-val">${badge(u?.role)}</span></div>
      <div class="kv-row"><span class="kv-key">Durum</span><span class="kv-val">${u?.active ? '<span class="badge badge-success">Aktif</span>' : '<span class="badge badge-muted">Pasif</span>'}</span></div>
      <div class="kv-row"><span class="kv-key">Kayıt Tarihi</span><span class="kv-val">${formatDate(u?.createdAt)}</span></div>
      <div class="sub-heading" style="margin-top:16px;">Hesap Bilgileri</div>
      <div class="kv-row"><span class="kv-key">IBAN</span><span class="kv-val mono">${escHtml(balData.userIban || '-')}</span></div>
      <div class="kv-row"><span class="kv-key">Bakiye</span><span class="kv-val" style="color:var(--success);font-size:16px;">${formatMoney(balData.money)}</span></div>
      <div class="kv-row"><span class="kv-key">Bloke Bakiye</span><span class="kv-val" style="color:var(--warning);">${formatMoney(balData.blockedMoney)}</span></div>
    `;
    if (foot) foot.style.display = 'flex';
  } catch (e) {
    body.innerHTML = `<div class="empty-msg" style="color:var(--danger);">${escHtml(e.message)}</div>`;
  }
};

// Save user edit
document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('btnSaveUserEdit')?.addEventListener('click', async () => {
    if (!editingUserId) return;
    const u = allUsers.find(x => x.id === editingUserId);
    if (!u) return;
    const newName = document.getElementById('editUserName')?.value?.trim();
    const newSurname = document.getElementById('editUserSurname')?.value?.trim();
    const newEmail = document.getElementById('editUserEmail')?.value?.trim();

    try {
      await api('PUT', '/api/user-service/v1/admin/updateuser', {
        id: editingUserId,
        name: newName || u.name,
        surname: newSurname || u.surname,
        mail: newEmail || u.mail,
        role: u.role,
        active: u.active
      });
      toast('Kullanıcı bilgileri güncellendi', 'success');
      u.name = newName || u.name;
      u.surname = newSurname || u.surname;
      u.mail = newEmail || u.mail;
      renderUsersTable();
      closeModal('modalUserDetail');
      addActivity('user', `${u.name} ${u.surname} kullanıcısı güncellendi`);
    } catch (e) { toast(e.message, 'error'); }
  });
});

window.showResetPwd = function(idx) {
  const u = allUsers[idx];
  if (!u) return;
  document.getElementById('resetPwdUserId').value = u.id;
  document.getElementById('newPwdInput').value = '';
  openModal('modalResetPwd');
};

window.showDelete = function(idx) {
  const u = allUsers[idx];
  if (!u) return;
  document.getElementById('btnConfirmDelete').onclick = () => confirmDelete(u.id);
  openModal('modalDelete');
};

async function confirmDelete(id) {
  try {
    const u = allUsers.find(x => x.id === id);
    await api('DELETE', `/api/user-service/v1/admin/deleteuser/${id}`);
    toast('Kullanıcı silindi', 'success');
    closeModal('modalDelete');
    allUsers = allUsers.filter(u => u.id !== id);
    renderUsersTable();
    addActivity('error', `${u?.name || id} kullanıcısı silindi`);
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
      addActivity('warning', `Kullanıcı ${id} şifresi sıfırlandı`);
    } catch (e) { toast(e.message, 'error'); }
  });
});

// Users CSV Export
document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('btnExportUsers')?.addEventListener('click', () => {
    if (!allUsers.length) { toast('Export edilecek kullanıcı yok', 'warning'); return; }
    const headers = ['ID', 'Ad', 'Soyad', 'E-posta', 'Rol', 'Durum', 'Kayıt Tarihi'];
    const rows = allUsers.map(u => [u.id, u.name, u.surname, u.mail, u.role, u.active ? 'Aktif' : 'Pasif', formatDate(u.createdAt)]);
    exportCSV('kullanicilar.csv', headers, rows);
  });
});

// ═══════════════════════════════════════════════════════════════
// 3. TRANSACTIONS
// ═══════════════════════════════════════════════════════════════
let currentTxData = []; // Store for filtering and CSV export

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
  document.getElementById('btnExportTx')?.addEventListener('click', () => {
    if (!currentTxData.length) { toast('Export edilecek işlem yok', 'warning'); return; }
    const headers = ['Tarih', 'Tx ID', 'Tip', 'Tutar', 'Sender IBAN', 'Alıcı', 'Durum', 'Hata'];
    const rows = currentTxData.map(t => [
      formatDate(t.localDateTime), t.id, t.transactionType, t.money,
      t.senderIban || '', t.receiverName || t.receiverIban || '', t.status, t.error ? 'Evet' : 'Hayır'
    ]);
    exportCSV('islemler.csv', headers, rows);
  });
});

async function loadTransactions() {
  const start = document.getElementById('txStart').value;
  const end   = document.getElementById('txEnd').value;
  if (!start || !end) { toast('Tarih aralığı seçin', 'warning'); return; }
  renderTxTable(null, 'loading');
  try {
    let txs = await api('GET', `/api/transaction-service/v1/transactions/daterange?startDate=${start}&endDate=${end}`) || [];

    // Client-side type and status filtering
    const typeFilter = document.getElementById('txTypeFilter')?.value;
    const statusFilter = document.getElementById('txStatusFilter')?.value;
    if (typeFilter) txs = txs.filter(t => t.transactionType === typeFilter);
    if (statusFilter) {
      if (statusFilter === 'FAILED') {
        txs = txs.filter(t => t.error === true);
      } else {
        txs = txs.filter(t => t.status === statusFilter);
      }
    }

    currentTxData = txs;
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
    currentTxData = txs;
    updateTxKpis(txs);
    renderTxTable(txs);
  } catch(e) { toast(e.message, 'error'); }
}

async function loadTopVolume() {
  renderTxTable(null, 'loading');
  try {
    const txs = await api('GET', '/api/transaction-service/v1/admin/stats/top-by-volume?limit=10') || [];
    currentTxData = txs;
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
    tbody.innerHTML = `<tr><td colspan="9" class="empty-msg">${errMsg ? '❌ ' + escHtml(errMsg) : 'İşlem bulunamadı.'}</td></tr>`;
    return;
  }
  // FIXED: Use data-index instead of JSON.stringify inline to prevent XSS
  tbody.innerHTML = txs.map((t, idx) => `
    <tr ${t.error ? 'style="background:var(--danger-light);"' : ''}>
      <td style="font-size:12px;">${formatDate(t.localDateTime)}</td>
      <td>${trunc(t.id, 8)}</td>
      <td>${badge(t.transactionType)}</td>
      <td style="font-weight:700;">${formatMoney(t.money)}</td>
      <td class="mono" style="font-size:11px;">${t.senderIban ? trunc(t.senderIban, 14) : '-'}</td>
      <td>${t.receiverName || t.receiverIban ? escHtml(t.receiverName || '') || trunc(t.receiverIban, 10) : '-'}</td>
      <td>${badge(t.status)}</td>
      <td>${t.error ? '<span class="badge badge-danger"><i class="ph-fill ph-x-circle"></i> Hata</span>' : '<span class="badge badge-muted">—</span>'}</td>
      <td><button class="action-link" data-tx-source="tx" data-tx-idx="${idx}" onclick="showTxDetailByIdx(this)">Detay</button></td>
    </tr>
  `).join('');
}

// FIXED: Safe transaction detail display — no JSON.stringify in onclick
window.showTxDetailByIdx = function(btn) {
  const source = btn.dataset.txSource;
  const idx = parseInt(btn.dataset.txIdx);
  let t;
  if (source === 'tx') t = currentTxData[idx];
  else if (source === 'fraud') t = currentFraudData[idx];
  else return;
  if (!t) return;

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
      <span class="kv-key">${escHtml(k)}</span>
      <span class="kv-val">${v !== undefined && v !== null && v !== '' ? escHtml(v) : '—'}</span>
    </div>
  `).join('');
  openModal('modalTxDetail');
};

// ═══════════════════════════════════════════════════════════════
// 4. ACCOUNT OPS
// ═══════════════════════════════════════════════════════════════
let opUserId = null;
let distChartInst = null;

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

async function loadAccountStats() {
  try {
    const [summary, dist] = await Promise.allSettled([
      api('GET', '/api/money-service/v1/admin/stats/summary'),
      api('GET', '/api/money-service/v1/admin/stats/distribution')
    ]);

    // Populate summary card
    if (summary.status === 'fulfilled' && summary.value) {
      const s = summary.value;
      document.getElementById('accSummaryTotal').textContent = s.totalAccounts || '—';
      document.getElementById('accSummaryAvg').textContent = formatMoney(s.averageBalance);
      document.getElementById('accSummaryRich').textContent = s.richAccounts || '0';
      document.getElementById('accSummaryZero').textContent = s.zeroBalanceAccounts || '0';
      document.getElementById('accSummaryBlocked').textContent = s.accountsWithBlockedFunds || '0';
    }

    // Render distribution chart
    if (dist.status === 'fulfilled' && dist.value) {
      renderDistChart(dist.value);
    } else {
      showChartEmpty('distChart', 'distChartEmpty');
    }
  } catch (e) {
    console.error('[AccountStats]', e);
  }
}

function renderDistChart(data) {
  const ctx = document.getElementById('distChart');
  if (!ctx) return;
  const labels = Object.keys(data);
  const values = Object.values(data);
  const total = values.reduce((a, b) => a + b, 0);
  if (total === 0) { showChartEmpty('distChart', 'distChartEmpty'); return; }
  showChartCanvas('distChart', 'distChartEmpty');

  const colors = getChartColors();
  if (distChartInst) distChartInst.destroy();
  distChartInst = new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels,
      datasets: [{
        data: values,
        backgroundColor: ['#64748b', '#026CB6', '#0C9A5D', '#E88F10', '#D83B3B'],
        borderWidth: 0, hoverOffset: 6
      }]
    },
    options: {
      cutout: '60%', responsive: true, maintainAspectRatio: false,
      plugins: { legend: { position: 'bottom', labels: { color: colors.text, font: { family: 'Inter', size: 11 }, padding: 12 } } }
    }
  });
}

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
    addActivity('money', `${type === 'deposit' ? 'Para yatırma' : 'Para çekme'}: ${formatMoney(amt)} (Kullanıcı: ${opUserId.slice(0,8)}...)`);
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
        <td class="mono" style="font-size:11px;">${escHtml(a.userIban || '-')}</td>
        <td style="font-weight:700;color:var(--success);">${formatMoney(a.money)}</td>
        <td style="color:var(--warning);">${formatMoney(a.blockedMoney)}</td>
        <td><button class="action-link" onclick="prefillLookup('${escHtml(a.userId)}')">Sorgula</button></td>
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
        <td class="mono" style="font-size:11px;">${escHtml(a.userIban || '-')}</td>
        <td style="font-weight:800;color:var(--success);">${formatMoney(a.balance)}</td>
        <td style="color:var(--warning);">${formatMoney(a.blockedMoney)}</td>
        <td><button class="action-link" onclick="prefillLookup('${escHtml(a.userId)}')">Sorgula</button></td>
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
    addActivity('money', `Bloke kaldırıldı: ${formatMoney(res.unblocked)} (Kullanıcı: ${opUserId.slice(0,8)}...)`);
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
        <td style="font-size:11px;">${escHtml(counterpart)}</td>
        <td>${badge(t.status)}</td>
        <td style="font-size:11px;">${formatDate(t.localDateTime)}</td>
      </tr>`;
    }).join('');
  } catch(e) { toast(e.message, 'error'); }
}


// ═══════════════════════════════════════════════════════════════
// 5. FRAUD & ERRORS — FIXED: Using admin error analysis endpoint
// ═══════════════════════════════════════════════════════════════
let currentFraudData = [];
let errorTypeChartInst = null;

document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('btnRefreshFraud')?.addEventListener('click', loadFraud);
  document.getElementById('btnExportFraud')?.addEventListener('click', () => {
    if (!currentFraudData.length) { toast('Export edilecek hata yok', 'warning'); return; }
    const headers = ['ID', 'Tip', 'Tutar', 'Hata Açıklaması', 'Gönderen', 'Alıcı', 'Tarih', 'Durum'];
    const rows = currentFraudData.map(e => [e.id, e.transactionType, e.money, e.errorDescription, e.senderName, e.receiverName, formatDate(e.localDateTime), e.status]);
    exportCSV('hatali_islemler.csv', headers, rows);
  });
});

async function loadFraud() {
  const tbody = document.getElementById('fraudTbody');
  tbody.innerHTML = `<tr><td colspan="9" class="empty-msg"><div class="loader-ring"></div> Yükleniyor...</td></tr>`;
  try {
    // FIXED: Use admin error analysis endpoint for richer data + transaction errors endpoint
    const [errorsRes, analysisRes] = await Promise.allSettled([
      api('GET', '/api/transaction-service/v1/transactions/errors'),
      api('GET', '/api/transaction-service/v1/admin/errors/analysis')
    ]);

    const errors = errorsRes.status === 'fulfilled' ? (errorsRes.value || []) : [];
    const analysis = analysisRes.status === 'fulfilled' ? (analysisRes.value || {}) : {};

    currentFraudData = errors;

    // KPI updates using analysis data (no extra API call needed for error rate)
    document.getElementById('errTotal').textContent = analysis.totalErrors ?? errors.length;

    // Error rate from analysis — already computed server-side data
    if (analysis.totalErrors !== undefined) {
      try {
        const summary = await api('GET', '/api/transaction-service/v1/admin/stats/summary');
        const totalCount = summary?.totalCount || 0;
        const rate = totalCount > 0 ? ((analysis.totalErrors / totalCount) * 100).toFixed(1) + '%' : '0%';
        document.getElementById('errRate').textContent = rate;
      } catch (_) { document.getElementById('errRate').textContent = '—'; }
    } else {
      document.getElementById('errRate').textContent = '—';
    }

    // Lost volume
    document.getElementById('errLostVolume').textContent = formatMoney(analysis.totalLostVolume);

    // Error badge on nav
    const navBadge = document.getElementById('errBadge');
    if (navBadge) { navBadge.style.display = errors.length > 0 ? 'inline-block' : 'none'; }

    if (!errors.length) {
      tbody.innerHTML = `<tr><td colspan="9" class="empty-msg">Hata kaydı bulunamadı.</td></tr>`;
      document.getElementById('errCommon').textContent = '—';
      showChartEmpty('errorTypeChart', 'errorTypeChartEmpty');
      return;
    }

    // Most common error type
    const typeCounts = {};
    errors.forEach(e => { typeCounts[e.transactionType] = (typeCounts[e.transactionType] || 0) + 1; });
    const mostCommon = Object.entries(typeCounts).sort(([,a],[,b])=>b-a)[0];
    document.getElementById('errCommon').textContent = mostCommon ? mostCommon[0] : '—';

    // Render error type chart
    renderErrorTypeChart(analysis.errorsByType || typeCounts);

    tbody.innerHTML = errors.map((e, idx) => `
      <tr>
        <td>${trunc(e.id, 8)}</td>
        <td>${badge(e.transactionType)}</td>
        <td style="font-weight:700;">${formatMoney(e.money)}</td>
        <td style="max-width:180px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" title="${escHtml(e.errorDescription||'')}">${escHtml(e.errorDescription || '-')}</td>
        <td>${escHtml(e.senderName || '-')}</td>
        <td>${escHtml(e.receiverName || '-')}</td>
        <td style="font-size:11px;">${formatDate(e.localDateTime)}</td>
        <td>${badge(e.status)}</td>
        <td><button class="action-link" data-tx-source="fraud" data-tx-idx="${idx}" onclick="showTxDetailByIdx(this)">Detay</button></td>
      </tr>
    `).join('');
  } catch (e) {
    tbody.innerHTML = `<tr><td colspan="9" class="empty-msg" style="color:var(--danger);">Hata: ${escHtml(e.message)}</td></tr>`;
  }
}

function renderErrorTypeChart(errorsByType) {
  const ctx = document.getElementById('errorTypeChart');
  if (!ctx) return;
  if (!errorsByType || Object.keys(errorsByType).length === 0) {
    showChartEmpty('errorTypeChart', 'errorTypeChartEmpty');
    return;
  }
  showChartCanvas('errorTypeChart', 'errorTypeChartEmpty');

  const labels = Object.keys(errorsByType);
  const values = Object.values(errorsByType);
  const colors = getChartColors();

  if (errorTypeChartInst) errorTypeChartInst.destroy();
  errorTypeChartInst = new Chart(ctx, {
    type: 'bar',
    data: {
      labels,
      datasets: [{
        label: 'Hata Sayısı',
        data: values,
        backgroundColor: ['#D83B3B', '#E88F10', '#026CB6'],
        borderRadius: 5, borderSkipped: false
      }]
    },
    options: {
      indexAxis: 'y',
      responsive: true, maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: {
        x: { ticks: { color: colors.text }, grid: { color: colors.grid } },
        y: { ticks: { color: colors.text, font: { family: 'Inter', size: 12, weight: '600' } }, grid: { display: false } }
      }
    }
  });
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
        <span class="health-name">${escHtml(svc.name)}</span>
        <div class="loader-ring"></div>
      </div>
      <div class="health-meta"><span>Kontrol ediliyor...</span><span>—</span></div>
    </div>
  `).join('');

  for (const svc of HEALTH_SVCS) {
    const cid = `hc-${svc.name.replace(/\s/g, '')}`;
    const t0 = performance.now();
    try {
      // FIXED: Use direct fetch instead of api() — health endpoints are permitAll
      // api() redirects to login on 401, breaking health checks
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 8000);

      const res = await fetch(svc.path, {
        method: 'GET',
        signal: controller.signal,
        headers: { 'Accept': 'application/json' }
      });
      clearTimeout(timeoutId);

      const dur = Math.round(performance.now() - t0);
      let isUp = false;
      let statusText = 'DOWN';
      let detailText = `${dur}ms`;

      if (res.ok) {
        try {
          const data = await res.json();
          isUp = data?.status === 'UP';
          statusText = isUp ? 'UP' : (data?.status || 'DOWN');
        } catch { isUp = false; }
      } else {
        statusText = `HTTP ${res.status}`;
        detailText = `${dur}ms — ${res.status === 401 ? 'Auth hatası' : res.status === 503 ? 'Servis hazır değil' : 'Hata'}`;
      }

      const card = document.getElementById(cid);
      if (card) {
        card.classList.toggle('up', isUp);
        card.classList.toggle('down', !isUp);
        card.innerHTML = `
          <div class="health-header">
            <span class="health-name">${escHtml(svc.name)}</span>
            <i class="ph-fill ${isUp ? 'ph-check-circle' : 'ph-x-circle'}" style="color:${isUp ? 'var(--success)' : 'var(--danger)'}; font-size:22px;"></i>
          </div>
          <div class="health-meta">
            <span>${isUp ? '<span class="badge badge-success">UP</span>' : `<span class="badge badge-danger">${escHtml(statusText)}</span>`}</span>
            <span>${detailText}</span>
          </div>
        `;
      }
    } catch (err) {
      const dur = Math.round(performance.now() - t0);
      const card = document.getElementById(cid);
      const errMsg = err.name === 'AbortError' ? 'Timeout (8s)' : 'Bağlantı hatası';
      if (card) {
        card.classList.add('down');
        card.innerHTML = `
          <div class="health-header">
            <span class="health-name">${escHtml(svc.name)}</span>
            <i class="ph-fill ph-warning-circle" style="color:var(--danger); font-size:22px;"></i>
          </div>
          <div class="health-meta">
            <span><span class="badge badge-danger">UNREACHABLE</span></span>
            <span>${errMsg} — ${dur}ms</span>
          </div>
        `;
      }
    }
  }
}

// ═══════════════════════════════════════════════════════════════
// 7. LIVE LOGS — kubectl SSE streaming + polling fallback
// ═══════════════════════════════════════════════════════════════
let logEventSource = null;
let logActiveSvc = null;
let logPollingTimer = null;
let logLastLineCount = 0;

document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.log-svc-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.log-svc-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      startLogs(btn.getAttribute('data-svc'));
    });
  });
  document.getElementById('btnStopLog')?.addEventListener('click', stopLogs);
  document.getElementById('btnClearLogs')?.addEventListener('click', clearLogs);
});

function startLogs(svc) {
  stopLogs();
  logActiveSvc = svc;
  const tail = document.getElementById('logTailCount')?.value || '200';
  let reconnectAttempts = 0;
  const MAX_RECONNECTS = 3;

  document.getElementById('btnStopLog').style.display = 'inline-flex';
  const term = document.getElementById('log-terminal');
  term.innerHTML = '';
  logLine(term, 'system', `[${svc}] Log stream başlatılıyor (tail=${tail})...`);

  updateLogStatus('connecting', svc);

  const tok = token();
  if (!tok) {
    logLine(term, 'error', '[HATA] Token bulunamadı — lütfen tekrar giriş yapın.');
    updateLogStatus('closed', svc);
    return;
  }

  // SSE bağlantısı — token query param olarak gönderilir
  // (EventSource API, Authorization header gönderemez, gateway'deki
  //  SseTokenQueryParamFilter bu parametreyi header'a çevirir)
  const url = `/api/gateway/admin/logs/${svc}?tail=${tail}&token=${encodeURIComponent(tok)}`;

  logEventSource = new EventSource(url);

  logEventSource.addEventListener('connected', e => {
    reconnectAttempts = 0;
    logLine(term, 'system', e.data);
    updateLogStatus('live', svc);
    addActivity('info', `Log stream başlatıldı: ${svc}`);
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
    updateLogStatus('closed', svc);
    cleanupEventSource();
  });

  // Sunucudan gelen özel 'error' event — kubectl hataları buradan gelir
  logEventSource.addEventListener('error', e => {
    if (e.data) {
      logLine(term, 'error', e.data);
      if (e.data.includes('kubectl') || e.data.includes('HATA')) {
        logLine(term, 'warn', 'SSE bağlantısı başarısız — polling moduna geçiliyor...');
        cleanupEventSource();
        startPollingLogs(svc, tail);
      }
    }
  });

  // Native SSE bağlantı hatası — reconnect limiti
  logEventSource.onerror = () => {
    if (!logEventSource) return;
    const state = logEventSource.readyState;
    if (state === EventSource.CLOSED) {
      logLine(term, 'warn', `[${svc}] SSE bağlantısı kapandı.`);
      logLine(term, 'system', 'Polling moduna geçiliyor...');
      updateLogStatus('closed', svc);
      cleanupEventSource();
      startPollingLogs(svc, tail);
    } else if (state === EventSource.CONNECTING) {
      reconnectAttempts++;
      if (reconnectAttempts > MAX_RECONNECTS) {
        logLine(term, 'error', `[${svc}] ${MAX_RECONNECTS} deneme sonrası SSE bağlantısı kurulamadı.`);
        logLine(term, 'system', 'Polling moduna geçiliyor...');
        updateLogStatus('closed', svc);
        cleanupEventSource();
        startPollingLogs(svc, tail);
      } else {
        updateLogStatus('reconnecting', svc);
        logLine(term, 'warn', `[${svc}] SSE bağlantısı kesildi, yeniden deneniyor... (${reconnectAttempts}/${MAX_RECONNECTS})`);
      }
    }
  };
}

// Polling fallback — SSE başarısız olduğunda kullanılır
function startPollingLogs(svc, tail) {
  if (logPollingTimer) return; // Zaten polling yapıyoruz
  logActiveSvc = svc;
  const term = document.getElementById('log-terminal');
  logLine(term, 'system', `[${svc}] Polling modu aktif (her 3 saniyede yeni loglar çekilecek)...`);
  updateLogStatus('polling', svc);

  // İlk yükleme
  fetchRecentLogs(svc, tail);

  // Periyodik polling
  logPollingTimer = setInterval(() => {
    if (!logActiveSvc) { stopPolling(); return; }
    fetchRecentLogs(logActiveSvc, '30'); // Son 30 satır
  }, 3000);
}

async function fetchRecentLogs(svc, lines) {
  const term = document.getElementById('log-terminal');
  try {
    const res = await fetch(`/api/gateway/admin/logs/${svc}/recent?lines=${lines}`, {
      headers: { 'Authorization': `Bearer ${token()}` }
    });
    if (!res.ok) {
      if (res.status === 401) {
        logLine(term, 'error', '[HATA] Yetkilendirme hatası — lütfen tekrar giriş yapın.');
        stopLogs();
        return;
      }
      logLine(term, 'error', `[HATA] HTTP ${res.status} — log verisi alınamadı.`);
      return;
    }
    const text = await res.text();
    if (!text || !text.trim()) return;

    const lines_arr = text.trim().split('\n');
    // Yeni satırları ekle (ilk yüklemede tümünü, sonraki polling'lerde sadece yenileri)
    const startIdx = logLastLineCount > 0 ? 0 : 0; // Tüm satırları göster
    lines_arr.forEach(line => {
      const trimmed = line.trim();
      if (!trimmed) return;
      if (trimmed.includes('ERROR')) {
        logLine(term, 'error', trimmed);
      } else if (trimmed.includes('WARN')) {
        logLine(term, 'warn', trimmed);
      } else {
        logLine(term, 'info', trimmed);
      }
    });
    logLastLineCount = lines_arr.length;
  } catch (e) {
    logLine(term, 'error', `[HATA] Polling hatası: ${e.message}`);
  }
}

function stopPolling() {
  if (logPollingTimer) {
    clearInterval(logPollingTimer);
    logPollingTimer = null;
  }
  logLastLineCount = 0;
}

function cleanupEventSource() {
  if (logEventSource) {
    logEventSource.close();
    logEventSource = null;
  }
}

function updateLogStatus(state, svc) {
  const dot = document.getElementById('logStatusDot');
  const txt = document.getElementById('logStatusText');
  switch(state) {
    case 'connecting':
      if (dot) dot.style.background = 'var(--warning)';
      if (txt) txt.textContent = `${svc} bağlanıyor...`;
      break;
    case 'live':
      if (dot) dot.style.background = 'var(--success)';
      if (txt) txt.textContent = `${svc} — canlı`;
      break;
    case 'polling':
      if (dot) dot.style.background = 'var(--primary)';
      if (txt) txt.textContent = `${svc} — polling`;
      break;
    case 'disconnected':
    case 'closed':
      if (dot) dot.style.background = 'var(--danger)';
      if (txt) txt.textContent = 'Bağlantı kesildi';
      break;
    case 'reconnecting':
      if (dot) dot.style.background = 'var(--warning)';
      if (txt) txt.textContent = `${svc} yeniden bağlanıyor...`;
      break;
    case 'idle':
      if (dot) dot.style.background = 'var(--text-muted)';
      if (txt) txt.textContent = 'Bağlı değil';
      break;
  }
}

function stopLogs() {
  cleanupEventSource();
  stopPolling();
  logActiveSvc = null;
  const btn = document.getElementById('btnStopLog');
  if (btn) btn.style.display = 'none';
  updateLogStatus('idle', '');
}

function logLine(term, type, text) {
  const div = document.createElement('div');
  const time = new Date().toLocaleTimeString('tr-TR');
  div.className = `log-line-${type}`;
  const hasTs = /^\d{4}-\d{2}/.test(text);
  div.textContent = hasTs ? text : `[${time}] ${text}`;
  term.appendChild(div);
  term.scrollTop = term.scrollHeight;
  // Max 500 satır tut
  while (term.children.length > 500) {
    term.removeChild(term.firstChild);
  }
}

function clearLogs() {
  const term = document.getElementById('log-terminal');
  if (term) {
    term.innerHTML = '';
    logLine(term, 'system', 'Terminal temizlendi — bir servis seçin.');
  }
  stopLogs();
  document.querySelectorAll('.log-svc-btn').forEach(b => b.classList.remove('active'));
}

// ═══════════════════════════════════════════════════════════════
// 8. ACTIVITY LOG PAGE
// ═══════════════════════════════════════════════════════════════
document.addEventListener('DOMContentLoaded', () => {
  document.getElementById('btnClearActivity')?.addEventListener('click', () => {
    activityItems.length = 0;
    renderActivity();
    toast('Aktivite log temizlendi', 'info');
  });
  document.getElementById('btnExportActivity')?.addEventListener('click', () => {
    if (!activityItems.length) { toast('Export edilecek aktivite yok', 'warning'); return; }
    const headers = ['Tarih', 'Saat', 'Tip', 'Açıklama'];
    const rows = activityItems.map(a => [a.date, a.time, a.type, a.text]);
    exportCSV('admin_aktivite.csv', headers, rows);
  });
});

// ═══════════════════════════════════════════════════════════════
// INIT
// ═══════════════════════════════════════════════════════════════
window.addEventListener('DOMContentLoaded', () => {
  if (!checkAdmin()) return;
  initSidebarUser();
  attachNav();
  startClock();
  initDarkMode();
  initAutoRefresh();
  initMobileSidebar();
  navigateTo('dashboard');
  addActivity('success', 'Admin paneli açıldı');
});
