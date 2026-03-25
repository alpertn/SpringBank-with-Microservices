// ═══════════════════════════════════════════════════════
//  SpringBank Admin Panel – JS
// ═══════════════════════════════════════════════════════

let healthInterval    = null;
let logPollInterval   = null;
let activeLogService  = null;
let weeklyChart       = null;
let typeChart         = null;

const SERVICES = ['user-service','money-service','transaction-service','auth-service','fraud-service'];

// ── Init ───────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  API.checkAdmin();
  const ud = API.getUserData();
  document.getElementById('user-name').innerText = (ud.Name || 'Admin') + ' ' + (ud.surname || '');
  document.getElementById('logout-btn').addEventListener('click', () => API.logout());

  // Tab switching
  document.querySelectorAll('.admin-tab').forEach(tab => {
    tab.addEventListener('click', () => {
      document.querySelectorAll('.admin-tab').forEach(t => t.classList.remove('active'));
      document.querySelectorAll('.admin-panel').forEach(p => p.classList.remove('active'));
      tab.classList.add('active');
      const target = tab.getAttribute('data-target');
      document.getElementById(target).classList.add('active');
      onTabSwitch(target);
    });
  });

  // Initial loads
  loadStats();
  loadHealth();
  loadNotifications();

  // Auto-refresh health every 30s
  healthInterval = setInterval(loadHealth, 30000);
});

function switchTab(panelId) {
  document.querySelectorAll('.admin-tab').forEach(t => t.classList.remove('active'));
  document.querySelectorAll('.admin-panel').forEach(p => p.classList.remove('active'));
  const tab = document.querySelector(`[data-target="${panelId}"]`);
  if (tab) tab.classList.add('active');
  document.getElementById(panelId).classList.add('active');
  onTabSwitch(panelId);
}

function onTabSwitch(tabId) {
  if (tabId === 'panel-charts')  initCharts();
  if (tabId !== 'panel-logs')    stopLogs();
}

// ═════════════════════════════════════════════════════
//  SİSTEM ÖZETİ
// ═════════════════════════════════════════════════════
async function loadStats() {
  const res = await API.call('/api/user-service/v1/admin/stats/total');
  if (res && res.ok) {
    const total = await res.text();
    document.getElementById('stat-users').innerText = total;
  }
  // also refresh health summary
  await updateHealthSummary();
}

function updateHealthSummary() {
  document.getElementById('stat-services-up').innerText   = '—';
  document.getElementById('stat-services-down').innerText = '—';
  document.getElementById('stats-summary').innerHTML =
    `<span style="color:var(--text-muted)">Servis durumu izleme için <strong>kubectl port-forward</strong> veya harici bir health aggregator gereklidir.</span>`;
}

// ═════════════════════════════════════════════════════
//  SERVİS DURUMU (HEALTH)
// ═════════════════════════════════════════════════════
function loadHealth() {
  document.getElementById('health-grid').innerHTML = `
    <div class="health-card" style="grid-column:1/-1; border-left:none;">
      <div class="health-icon"><i class="ph ph-info"></i></div>
      <div>
        <div class="health-name">Kubernetes ortamında çalışır</div>
        <div class="health-status" style="color:var(--text-muted)">
          Servis health bilgisi actuator endpoint'leri üzerinden okunur.<br>
          Kubernetes'te pod'lar canlıyken her servis kartı güncel durumu gösterir.
        </div>
      </div>
    </div>`;
}

function renderHealthGrid(services) {
  const grid = document.getElementById('health-grid');
  const icons = {
    'user-service':        'ph-users',
    'money-service':       'ph-currency-dollar',
    'transaction-service': 'ph-arrows-left-right',
    'auth-service':        'ph-key',
    'fraud-service':       'ph-shield-warning',
  };

  grid.innerHTML = services.map(s => {
    const status   = s.status || 'UNKNOWN';
    const icon     = icons[s.service] || 'ph-circle';
    const statusTr = status === 'UP' ? 'Çevrimiçi' : status === 'DOWN' ? 'Çevrimdışı' : 'Bilinmiyor';
    const iconName = status === 'UP' ? 'ph-check-circle' : status === 'DOWN' ? 'ph-x-circle' : 'ph-question';

    return `
      <div class="health-card ${status}">
        <div class="health-icon ${status}"><i class="ph ${icon}"></i></div>
        <div>
          <div class="health-name">${s.service}</div>
          <div class="health-status ${status}"><i class="ph ${iconName}"></i> ${statusTr}</div>
        </div>
      </div>
    `;
  }).join('');
}

// ═════════════════════════════════════════════════════
//  LOG İZLEME
// ═════════════════════════════════════════════════════
function startLogs(serviceName, btn) {
  stopLogs();
  activeLogService = serviceName;

  document.querySelectorAll('.log-service-btn').forEach(b => b.classList.remove('active'));
  if (btn) btn.classList.add('active');

  const terminal = document.getElementById('log-terminal');
  const stopBtn  = document.getElementById('log-stop-btn');

  terminal.innerHTML = `<div class="log-line INFO">[${now()}] LOG AKIŞI BAŞLATILDI → ${serviceName}</div>
<div class="log-line INFO">[${now()}] Bağlanılıyor: ${serviceName}/actuator/health ...</div>`;
  stopBtn.style.display = 'inline-flex';

  // Poll actuator/logfile or health — gerçek log stream olmadığından
  // actuator/health endpoint'ini sorgulayıp simüle ediyoruz.
  // Gerçek ortamda Kubernetes log API veya log aggregator eklenebilir.
  let tick = 0;
  logPollInterval = setInterval(async () => {
    tick++;
    try {
      const logRes = await API.call(`/api/gateway/admin/health`);
      if (!logRes || !logRes.ok) {
        appendLog(terminal, 'ERROR', `[${now()}] ${serviceName} → Erişim Hatası`);
        return;
      }
      const services = await logRes.json();
      const svc = services.find(s => s.service === serviceName);
      const status = svc ? svc.status : 'UNKNOWN';
      const level  = status === 'UP' ? 'INFO' : 'ERROR';
      appendLog(terminal, level,
        `[${now()}] ${serviceName} → actuator/health : STATUS=${status} | poll#${tick}`
      );
    } catch(e) {
      appendLog(terminal, 'ERROR', `[${now()}] ${serviceName} → Polling hatası: ${e.message}`);
    }
  }, 4000);
}

function stopLogs() {
  if (logPollInterval) { clearInterval(logPollInterval); logPollInterval = null; }
  document.getElementById('log-stop-btn').style.display = 'none';
  document.querySelectorAll('.log-service-btn').forEach(b => b.classList.remove('active'));
  activeLogService = null;
}

function clearLogs() {
  document.getElementById('log-terminal').innerHTML = `<div class="log-empty"><i class="ph ph-terminal"></i><span>Terminal temizlendi.</span></div>`;
  stopLogs();
}

function appendLog(terminal, level, text) {
  const div = document.createElement('div');
  div.className = `log-line ${level}`;
  div.textContent = text;
  terminal.appendChild(div);
  terminal.scrollTop = terminal.scrollHeight;
}

function now() {
  return new Date().toLocaleTimeString('tr-TR', { hour12: false });
}

// ═════════════════════════════════════════════════════
//  GRAFİKLER
// ═════════════════════════════════════════════════════
async function initCharts() {
  if (weeklyChart || typeChart) return; // Already rendered

  // ── Weekly bar chart (mock data — real data gelince burayı API'ye bağla) ──
  const days = getLast7Days();
  const weeklyData = [12, 24, 18, 35, 28, 41, 30]; // replace with real API

  const wCtx = document.getElementById('chart-weekly').getContext('2d');
  weeklyChart = new Chart(wCtx, {
    type: 'bar',
    data: {
      labels: days,
      datasets: [{
        label: 'İşlem Adedi',
        data: weeklyData,
        backgroundColor: 'rgba(2, 108, 182, 0.15)',
        borderColor: '#026CB6',
        borderWidth: 2,
        borderRadius: 6,
        borderSkipped: false,
      }]
    },
    options: {
      responsive: true, maintainAspectRatio: false,
      plugins: { legend: { display: false } },
      scales: {
        y: { beginAtZero: true, grid: { color: '#E1EAF2' }, ticks: { font: { family: 'Inter', size: 11 } } },
        x: { grid: { display: false }, ticks: { font: { family: 'Inter', size: 11 } } }
      }
    }
  });

  // ── Donut type chart (mock) ──
  const tCtx = document.getElementById('chart-types').getContext('2d');
  typeChart = new Chart(tCtx, {
    type: 'doughnut',
    data: {
      labels: ['Transfer (EFT)', 'Para Yatırma', 'Para Çekme'],
      datasets: [{
        data: [55, 30, 15],
        backgroundColor: ['#026CB6', '#0C9A5D', '#E88F10'],
        borderWidth: 0,
        hoverOffset: 8,
      }]
    },
    options: {
      responsive: true, maintainAspectRatio: false,
      cutout: '68%',
      plugins: {
        legend: {
          position: 'bottom',
          labels: { font: { family: 'Inter', size: 12 }, padding: 16 }
        }
      }
    }
  });

  const total = weeklyData.reduce((a,b) => a+b, 0);
  document.getElementById('chart-summary').innerHTML =
    `Son 7 günde toplam <strong>${total}</strong> işlem gerçekleştirildi. 
     Günlük ortalama: <strong>${(total/7).toFixed(0)}</strong> işlem.`;
}

function getLast7Days() {
  const days = [], opts = { weekday: 'short', day: 'numeric' };
  for (let i = 6; i >= 0; i--) {
    const d = new Date(); d.setDate(d.getDate() - i);
    days.push(d.toLocaleDateString('tr-TR', opts));
  }
  return days;
}

// ═════════════════════════════════════════════════════
//  BİLDİRİMLER / UYARILAR
// ═════════════════════════════════════════════════════
function loadNotifications() {
  const list = document.getElementById('notif-list');
  const notifs = [
    {
      type: 'info',
      icon: 'ph-info',
      title: 'Sistem bilgisi',
      desc: 'Fraud servisi, yüksek tutarlı işlemleri otomatik olarak işaretleyebilir.',
      time: 'Sistem mesajı'
    },
    {
      type: 'warning',
      icon: 'ph-clock',
      title: 'Blokeli Bakiye Uyarısı',
      desc: 'Transfer sürecinde para bloke edilir; işlem tamamlandığında otomatik serbest bırakılır.',
      time: 'Sistem mesajı'
    }
  ];

  list.innerHTML = notifs.map(n => `
    <div class="notif-item">
      <div class="notif-item-icon ${n.type}"><i class="ph ${n.icon}"></i></div>
      <div class="notif-item-text">
        <div class="notif-item-title">${n.title}</div>
        <div class="notif-item-desc">${n.desc}</div>
        <div class="notif-item-time">${n.time}</div>
      </div>
    </div>
  `).join('');
  updateNotifBadge(0);
}

function updateNotifBadge(count) {
  const badge = document.getElementById('notif-count');
  if (count > 0) {
    badge.innerText = count;
    badge.style.display = 'flex';
  } else {
    badge.style.display = 'none';
  }
}

// ═════════════════════════════════════════════════════
//  KULLANICI YÖNETİMİ
// ═════════════════════════════════════════════════════
async function searchUser() {
  const q = document.getElementById('search-query').value.trim();
  const resDiv = document.getElementById('user-search-res');
  if (!q) return;

  API.showLoading();
  let url = '/api/user-service/v1/admin/search?query=' + encodeURIComponent(q);
  if (q.includes('@')) url = '/api/user-service/v1/admin/findbyemail?email=' + encodeURIComponent(q);

  const res = await API.call(url);
  API.hideLoading();

  if (res && res.ok) {
    const data = await res.json();
    const rows = Array.isArray(data) ? data : [data];
    if (rows.length === 0) { resDiv.innerHTML = 'Kayıt bulunamadı.'; return; }
    let html = `<table style="width:100%; text-align:left; border-collapse:collapse; font-size:13px;">
      <tr style="border-bottom:1px solid var(--border); color:var(--text-muted)"><th style="padding:8px">ID</th><th>Email</th><th>İsim</th><th>Rol</th></tr>`;
    rows.forEach(r => {
      html += `<tr>
        <td style="padding:8px;font-family:monospace;font-size:11px">${r.id}</td>
        <td>${r.email}</td>
        <td>${r.name} ${r.surname}</td>
        <td><span class="badge badge-${r.role === 'ADMIN' ? 'danger' : 'primary'}">${r.role}</span></td>
      </tr>`;
    });
    html += `</table>`;
    resDiv.innerHTML = html;
  } else {
    resDiv.innerHTML = '<span style="color:var(--danger)">Arama başarısız.</span>';
  }
}

async function updateRole() {
  const uid  = document.getElementById('role-uid').value.trim();
  const role = document.getElementById('role-sel').value;
  if (!uid) return;
  API.showLoading();
  const res = await API.call(`/api/user-service/v1/admin/updaterole/${uid}?role=${role}`, 'PATCH');
  API.hideLoading();
  if (res && res.ok) API.toast('Yetki güncellendi', 'success');
  else               API.toast('Yetki güncelleme başarısız', 'danger');
}

// ═════════════════════════════════════════════════════
//  BAKİYE YÖNETİMİ
// ═════════════════════════════════════════════════════
async function checkWallet() {
  const uid    = document.getElementById('wallet-uid').value.trim();
  const resDiv = document.getElementById('wallet-res');
  if (!uid) return;
  API.showLoading();
  const res = await API.call('/api/money-service/v1/accounts/getUserIbanWithUserId', 'POST', { userId: uid });
  API.hideLoading();
  if (res && res.ok) {
    const data = await res.json();
    resDiv.innerHTML = `
      <div style="padding:16px; background:var(--bg2); border-radius:8px; display:flex; flex-direction:column; gap:6px;">
        <div><span style="color:var(--text-muted); font-size:12px; font-weight:600;">IBAN</span><br>
             <span style="font-family:monospace; font-size:13px">${data.iban}</span></div>
        <div style="font-size:18px; color:var(--success); font-weight:700;">${API.formatMoney(data.money)}</div>
        <div style="color:var(--warning); font-size:13px;">Blokeli: ${API.formatMoney(data.blockedMoney)}</div>
      </div>`;
  } else {
    resDiv.innerHTML = '<span style="color:var(--danger)">Cüzdan bilgisi bulunamadı.</span>';
  }
}

async function adminDeposit() {
  const uid = document.getElementById('money-uid').value.trim();
  const amt = parseFloat(document.getElementById('money-amt').value);
  if (!uid || !amt) return;
  API.showLoading();
  const res = await API.call('/api/money-service/v1/accounts/depositByUserId', 'POST', { userId: uid, amount: amt.toString() });
  API.hideLoading();
  if (res && res.ok) API.toast('Yatırma Başarılı', 'success');
  else               API.toast('Hata', 'danger');
}

async function adminWithdraw() {
  const uid = document.getElementById('money-uid').value.trim();
  const amt = parseFloat(document.getElementById('money-amt').value);
  if (!uid || !amt) return;
  API.showLoading();
  const res = await API.call('/api/money-service/v1/accounts/withdrawByUserId', 'POST', { userId: uid, amount: amt.toString() });
  API.hideLoading();
  if (res && res.ok) API.toast('Çekim Başarılı', 'success');
  else               API.toast('Hata / Yetersiz Bakiye', 'danger');
}

// ═════════════════════════════════════════════════════
//  İŞLEM KAYITLARI (with transferStatus)
// ═════════════════════════════════════════════════════
async function loadUserTx() {
  const uid   = document.getElementById('tx-uid').value.trim();
  const tbody = document.getElementById('adm-tx-tbody');
  if (!uid) return;

  API.showLoading();
  const res = await API.call('/api/transaction-service/v1/transactions/gettransactionhistorywithid', 'POST', uid);
  API.hideLoading();

  if (res && res.ok) {
    const txs = await res.json();
    if (txs.length === 0) {
      tbody.innerHTML = `<tr><td colspan="8" style="text-align:center">İşlem bulunamadı.</td></tr>`;
      return;
    }

    tbody.innerHTML = '';
    txs.forEach(t => {
      let bClass = 'success';
      if (t.status === 'ERROR' || t.status === 'DENIED' || t.status === 'FAILED') bClass = 'danger';
      if (t.status === 'PROGRESS' || t.status === 'PROCESSED' || t.status === 'PENDING') bClass = 'warning';

      let typeText = t.transactionType || 'TRANSFER';
      if (typeText === 'DEPOSIT')  typeText = 'Yatırma';
      else if (typeText === 'WITHDRAW') typeText = 'Çekme';
      else typeText = 'Transfer';

      const transferStatus = t.transferStatus
        ? `<span class="badge badge-info">${t.transferStatus}</span>`
        : `<span style="color:var(--text-muted)">—</span>`;

      tbody.innerHTML += `
        <tr>
          <td style="font-size:12px">${API.formatDate(t.localDateTime)}</td>
          <td><span class="badge badge-primary">${typeText}</span></td>
          <td>${t.senderName || '-'}<div style="font-size:10px;color:#999">${t.senderIban || '-'}</div></td>
          <td>${t.receiverName || '-'}<div style="font-size:10px;color:#999">${t.receiverIban || '-'}</div></td>
          <td style="font-weight:600">${API.formatMoney(t.money)}</td>
          <td><span class="badge badge-${bClass}">${t.status || 'BİLİNMİYOR'}</span></td>
          <td>${transferStatus}</td>
          <td style="font-size:12px; max-width:140px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;"
              title="${t.description || ''}">${t.description || '-'}</td>
        </tr>`;
    });
  } else {
    tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; color:var(--danger)">Getirilemedi. ID'yi kontrol edin.</td></tr>`;
  }
}
