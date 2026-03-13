/* ===== SpringBank - Dashboard Sayfası JS ===== */

document.addEventListener('DOMContentLoaded', () => {
    // Auth kontrolü - Yoksa api.js login sayfasına yönlendirir
    if (!API.checkAuth()) return;

    // Kullanıcı bilgilerini ekrana doldur
    const user = API.getUserData();
    document.getElementById('user-name').innerText = user.name + ' ' + user.surname;
    document.getElementById('greeting').innerText = 'Hoş geldiniz, ' + user.name + ' 👋';
    
    // Admin ise admin menülerini göster
    if (user.roles?.includes('ADMIN')) {
        document.getElementById('admin-section').style.display = 'block';
        document.getElementById('admin-link').style.display = 'block';
        document.getElementById('user-role').innerText = 'Admin';
    }

    // Çıkış Butonu
    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', () => API.logout());
    }

    // Sayfa açıldığında verileri yükle
    window.loadData();
});

window.copyIban = function() {
    const ibanText = document.getElementById('iban').innerText;
    if (ibanText && ibanText !== 'Yükleniyor...' && ibanText !== '-') {
        navigator.clipboard.writeText(ibanText);
        API.toast('IBAN kopyalandı', 'success', 2000);
    }
};

window.loadData = function() {
    loadBalance();
    loadTransactions();
};

async function loadBalance() {
    try {
        const res = await API.call('/api/money-service/v1/accounts/balance-info', 'GET');
        if (res && res.ok) {
            const data = await res.json();
            document.getElementById('balance').innerText = API.formatMoney(data.money);
            document.getElementById('blocked').innerText = API.formatMoney(data.blockedmoney || 0);
            document.getElementById('iban').innerText = data.userIban || '-';
        } else {
            document.getElementById('balance').innerText = '-';
            document.getElementById('blocked').innerText = '-';
            document.getElementById('iban').innerText = '-';
        }
    } catch(e) {
        console.error('Balance load error:', e);
    }
}

async function loadTransactions() {
    const wrapper = document.getElementById('transactions-wrapper');
    wrapper.innerHTML = `<div class="empty-state"><div class="spinner"></div><div class="empty-state-text" style="margin-top:12px">Hareketler yükleniyor...</div></div>`;

    try {
        const userId = API.getUserId();
        const res = await API.call('/api/transaction-service/v1/transactions/gettransactionhistorywithid', 'POST', userId);

        if (res && res.ok) {
            const data = await res.json();
            
            if (!data || data.length === 0) {
                wrapper.innerHTML = `
                    <div class="empty-state">
                        <div style="font-size:32px; margin-bottom:12px;">📭</div>
                        <div class="empty-state-text">Henüz hiçbir hesap hareketiniz bulunmuyor.</div>
                    </div>
                `;
                return;
            }
            
            // Tarihe göre yeniden eskiye diz ve sadece ilk 10'u göster
            data.sort((a,b) => new Date(b.localDateTime) - new Date(a.localDateTime));
            const recentData = data.slice(0, 10);
            
            let html = `
                <table>
                    <thead>
                        <tr>
                            <th>Tarih</th>
                            <th>İşlem</th>
                            <th>Açıklama</th>
                            <th>Tutar</th>
                            <th>Durum</th>
                        </tr>
                    </thead>
                    <tbody>
            `;
            
            recentData.forEach(tx => {
                let statusBadge = '';
                if(tx.status === 'SUCCESS') statusBadge = '<span class="badge badge-success" style="font-size:11px;">Başarılı</span>';
                else if(tx.status === 'PROGRESS') statusBadge = '<span class="badge badge-warning" style="font-size:11px;">İşleniyor</span>';
                else statusBadge = '<span class="badge badge-danger" style="font-size:11px;">Hatalı</span>';

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
                        <td style="font-size:13px;">${API.formatDate(tx.localDateTime)}</td>
                        <td style="font-weight:600; font-size:13px;">${typeLabel || '-'}</td>
                        <td style="color:var(--text-muted); font-size:13px;">${tx.description ? (tx.description.length>25 ? tx.description.substring(0,25)+'...' : tx.description) : '-'}</td>
                        <td style="color:${amountColor}; font-weight:700;">${amountSign}${API.formatMoney(tx.money)}</td>
                        <td>${statusBadge}</td>
                    </tr>
                `;
            });
            
            html += `</tbody></table>`;
            wrapper.innerHTML = html;
        } else {
            wrapper.innerHTML = `<div class="empty-state"><div style="font-size:32px; margin-bottom:12px;">⚠️</div><div class="empty-state-text">Veriler alınamadı.</div></div>`;
        }
    } catch (e) {
        console.error('Transactions load error:', e);
        wrapper.innerHTML = `<div class="empty-state"><div style="font-size:32px; margin-bottom:12px;">🔌</div><div class="empty-state-text">Bağlantı hatası.</div></div>`;
    }
}
