document.addEventListener('DOMContentLoaded', () => {
  API.checkAdmin();
  const ud = API.getUserData();
  document.getElementById('user-name').innerText = (ud.Name || 'Admin') + ' ' + (ud.surname || '');

  document.getElementById('logout-btn').addEventListener('click', () => API.logout());

  // Tab switching
  const tabs = document.querySelectorAll('.admin-tab');
  const panels = document.querySelectorAll('.admin-panel');

  tabs.forEach(t => {
    t.addEventListener('click', () => {
      tabs.forEach(btn => btn.classList.remove('active'));
      panels.forEach(p => p.classList.remove('active'));
      
      t.classList.add('active');
      document.getElementById(t.getAttribute('data-target')).classList.add('active');
    });
  });

  loadStats();
});

async function loadStats() {
  const res = await API.call('/api/user-service/v1/admin/stats/total');
  if(res && res.ok) {
    const total = await res.text();
    document.getElementById('stat-users').innerText = total;
  }
}

// USER SEARCH
async function searchUser() {
  const q = document.getElementById('search-query').value.trim();
  const resDiv = document.getElementById('user-search-res');
  if(!q) return;

  API.showLoading();
  let url = '/api/user-service/v1/admin/search?query=' + encodeURIComponent(q);
  if(q.includes('@')) url = '/api/user-service/v1/admin/findbyemail?email=' + encodeURIComponent(q);

  const res = await API.call(url);
  API.hideLoading();

  if(res && res.ok) {
    const data = await res.json();
    const rows = Array.isArray(data) ? data : [data];
    if(rows.length === 0) { resDiv.innerHTML = 'Kayıt bulunamadı.'; return; }
    
    let html = `<table style="width:100%; text-align:left; border-collapse:collapse;">
      <tr style="border-bottom:1px solid var(--border)"><th>ID</th><th>Email</th><th>İsim</th><th>Rol</th></tr>`;
    rows.forEach(r => {
      html += `<tr>
        <td style="padding:8px;font-family:monospace;font-size:11px">${r.id}</td>
        <td>${r.email}</td>
        <td>${r.name} ${r.surname}</td>
        <td><span class="badge badge-${r.role==='ADMIN'?'danger':'primary'}">${r.role}</span></td>
      </tr>`;
    });
    html += `</table>`;
    resDiv.innerHTML = html;
  } else {
    resDiv.innerHTML = '<span style="color:var(--danger)">Arama başarısız.</span>';
  }
}

// ROLE UPDATE
async function updateRole() {
  const uid = document.getElementById('role-uid').value.trim();
  const role = document.getElementById('role-sel').value;
  if(!uid) return;

  API.showLoading();
  const res = await API.call(`/api/user-service/v1/admin/updaterole/${uid}?role=${role}`, 'PATCH');
  API.hideLoading();
  
  if(res && res.ok) {
    API.toast('Yetki güncellendi', 'success');
  } else {
    API.toast('Yetki güncelleme başarısız', 'danger');
  }
}

// WALLET LOOKUP
async function checkWallet() {
  const uid = document.getElementById('wallet-uid').value.trim();
  const resDiv = document.getElementById('wallet-res');
  if(!uid) return;

  API.showLoading();
  const res = await API.call('/api/money-service/v1/accounts/getUserIbanWithUserId', 'POST', { userId: uid });
  API.hideLoading();

  if(res && res.ok) {
    const data = await res.json();
    resDiv.innerHTML = `
      <div style="padding:16px; background:var(--bg2); border-radius:8px;">
        <div><strong>IBAN:</strong> ${data.iban}</div>
        <div style="font-size:18px; margin-top:8px; color:var(--success)"><strong>Bakiye:</strong> ${API.formatMoney(data.money)}</div>
        <div style="color:var(--warning)">Blokeli: ${API.formatMoney(data.blockedMoney)}</div>
      </div>
    `;
  } else {
    resDiv.innerHTML = '<span style="color:var(--danger)">Cüzdan bilgisi bulunamadı.</span>';
  }
}

// ADMIN DEPOSIT/WITHDRAW (calls transaction service logic to bypass or via admin endpoints)
async function adminDeposit() {
  const uid = document.getElementById('money-uid').value.trim();
  const amt = parseFloat(document.getElementById('money-amt').value);
  if(!uid || !amt) return;
  API.showLoading();
  // Using money service directly to force update since it's an admin bypass
  const res = await API.call('/api/money-service/v1/accounts/depositByUserId', 'POST', { userId: uid, amount: amt.toString() });
  API.hideLoading();
  if(res && res.ok) API.toast('Yatırma Başarılı', 'success');
  else API.toast('Hata', 'danger');
}

async function adminWithdraw() {
  const uid = document.getElementById('money-uid').value.trim();
  const amt = parseFloat(document.getElementById('money-amt').value);
  if(!uid || !amt) return;
  API.showLoading();
  // Using money service directly to force update since it's an admin bypass
  const res = await API.call('/api/money-service/v1/accounts/withdrawByUserId', 'POST', { userId: uid, amount: amt.toString() });
  API.hideLoading();
  if(res && res.ok) API.toast('Çekim Başarılı', 'success');
  else API.toast('Hata/Yetersiz Bakiye', 'danger');
}

// ADMIN TRANSACTIONS
async function loadUserTx() {
  const uid = document.getElementById('tx-uid').value.trim();
  const tbody = document.getElementById('adm-tx-tbody');
  if(!uid) return;

  API.showLoading();
  const res = await API.call('/api/transaction-service/v1/transactions/gettransactionhistorywithid', 'POST', uid);
  API.hideLoading();

  if(res && res.ok) {
    const txs = await res.json();
    if(txs.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align:center">İşlem bulunamadı.</td></tr>`;
      return;
    }
    
    tbody.innerHTML = '';
    txs.forEach(t => {
      let bClass = 'success';
      if(t.status === 'ERROR' || t.status === 'DENIED') bClass = 'danger';
      if(t.status === 'PROGRESS' || t.status === 'PROCESSED') bClass = 'warning';
      
      let typeText = t.transactionType || 'TRANSFER';
      if(typeText === 'DEPOSIT') typeText = 'Yatırma';
      else if(typeText === 'WITHDRAW') typeText = 'Çekme';

      tbody.innerHTML += `
        <tr>
          <td style="font-size:12px">${API.formatDate(t.localDateTime)}</td>
          <td><span class="badge badge-primary">${typeText}</span></td>
          <td>${t.senderName || '-'} <div style="font-size:10px;color:#999">${t.senderIban||'-'}</div></td>
          <td>${t.receiverName || '-'} <div style="font-size:10px;color:#999">${t.receiverIban||'-'}</div></td>
          <td style="font-weight:600">${API.formatMoney(t.money)}</td>
          <td><span class="badge badge-${bClass}">${t.status||'BILINMIYOR'}</span></td>
          <td style="font-size:12px; max-width:150px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;" title="${t.description||''}">${t.description || '-'}</td>
        </tr>
      `;
    });
  } else {
    tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color:var(--danger)">Getirilemedi. ID'yi kontrol edin.</td></tr>`;
  }
}
