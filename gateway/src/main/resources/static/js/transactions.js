/* ===== SpringBank - Transactions (Geçmiş) JS ===== */

document.addEventListener('DOMContentLoaded', () => {
    if (!API.checkAuth()) return;
  
    const user = API.getUserData();
    document.getElementById('user-name').innerText = user.name + ' ' + user.surname;
    if (user.roles?.includes('ADMIN')) {
      document.getElementById('admin-section').style.display = 'block';
      document.getElementById('admin-link').style.display = 'block';
      document.getElementById('user-role').innerText = 'Admin';
    }
  
    document.getElementById('logout-btn').addEventListener('click', () => API.logout());
  
    loadTransactions();
});
  
window.loadTransactions = async function() {
    const wrapper = document.getElementById('tx-wrapper');
    wrapper.innerHTML = `<div class="empty-state"><div class="spinner" style="border-top-color:var(--primary);margin-bottom:10px;"></div><div class="empty-state-text">Hareketler yükleniyor...</div></div>`;
  
    try {
        const userId = API.getUserId();
        
        const res = await API.call('/api/transaction-service/v1/transactions/gettransactionhistorywithid', 'POST', userId);
        
        if (res && res.ok) {
            const data = await res.json();
            
            if (!data || data.length === 0) {
                wrapper.innerHTML = `
                    <div class="empty-state">
                        <div class="empty-state-icon">📭</div>
                        <div class="empty-state-text">Henüz hiçbir hesap hareketiniz bulunmuyor.</div>
                    </div>
                `;
                return;
            }
            
            // Tarihe göre yeniden eskiye diz
            data.sort((a,b) => new Date(b.localDateTime) - new Date(a.localDateTime));
            
            let html = `
                <table>
                    <thead>
                        <tr>
                            <th>Tarih</th>
                            <th>İşlem Türü</th>
                            <th>Açıklama</th>
                            <th>Gönderen</th>
                            <th>Alıcı</th>
                            <th>Tutar</th>
                            <th>Durum</th>
                        </tr>
                    </thead>
                    <tbody>
            `;
            
            data.forEach(tx => {
                let statusBadge = '';
                if(tx.status === 'SUCCESS') statusBadge = '<span class="badge badge-success">Başarılı</span>';
                else if(tx.status === 'PROGRESS') statusBadge = '<span class="badge badge-warning">İşleniyor</span>';
                else statusBadge = '<span class="badge badge-danger">Hatalı</span>';

                let typeLabel = tx.transactionType;
                let isOutflow = tx.senderUserId === userId && typeLabel !== 'DEPOSIT';
                
                let amountSign = isOutflow ? '-' : '+';
                let amountColor = isOutflow ? 'var(--danger)' : 'var(--success)';
                if (typeLabel === 'DEPOSIT' || typeLabel === 'WITHDRAW') {
                    amountSign = typeLabel === 'WITHDRAW' ? '-' : '+';
                    amountColor = typeLabel === 'WITHDRAW' ? 'var(--danger)' : 'var(--success)';
                }

                html += `
                    <tr>
                        <td>${API.formatDate(tx.localDateTime)}</td>
                        <td><strong>${typeLabel || '-'}</strong></td>
                        <td><span style="color:var(--text-muted);font-size:12px;">${tx.description || '-'}</span></td>
                        <td>${tx.senderName ? tx.senderName + ' ' + (tx.senderSurname||'') : '-'}</td>
                        <td>${tx.receiverName ? tx.receiverName + ' ' + (tx.receiverSurname||'') : '-'}</td>
                        <td style="color:${amountColor}; font-weight:700;">${amountSign}${API.formatMoney(tx.money)}</td>
                        <td>${statusBadge}</td>
                    </tr>
                `;
            });
            
            html += `</tbody></table>`;
            wrapper.innerHTML = html;
        } else {
            wrapper.innerHTML = `<div class="empty-state"><div class="empty-state-icon">⚠️</div><div class="empty-state-text">Veriler alınamadı.</div></div>`;
        }
    } catch (e) {
        wrapper.innerHTML = `<div class="empty-state"><div class="empty-state-icon">🔌</div><div class="empty-state-text">Bağlantı hatası oluştu.</div></div>`;
    }
};
