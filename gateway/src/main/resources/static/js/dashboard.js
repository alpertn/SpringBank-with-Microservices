/* ===== SpringBank - Dashboard Sayfası JS ===== */

document.addEventListener('DOMContentLoaded', () => {
    if (!API.checkAuth()) return;

    const user = API.getUserData();
    document.getElementById('user-name').innerText = user.name + ' ' + user.surname;
    document.getElementById('greeting').innerText = 'Hoş geldiniz, ' + user.name + ' 👋';

    if (user.roles?.includes('ADMIN')) {
        document.getElementById('admin-section').style.display = 'block';
        document.getElementById('admin-link').style.display = 'block';
        document.getElementById('user-role').innerText = 'Admin';
    }

    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', () => API.logout());
    }

    window.loadData();
});

window.copyIban = function () {
    const ibanText = document.getElementById('iban').innerText;
    if (ibanText && ibanText !== 'Yükleniyor...' && ibanText !== '-') {
        navigator.clipboard.writeText(ibanText);
        API.toast('IBAN kopyalandı', 'success', 2000);
    }
};

window.loadData = function () {
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
            // Hesap var — hesap oluştur butonunu gizle, bakiye kartlarını göster
            document.getElementById('no-account-banner').style.display = 'none';
            document.getElementById('balance-cards').style.display = '';
            document.getElementById('quick-action-create-account').style.display = 'none';
        } else {
            // Hesap yok — bakiye kartlarını gizle, oluştur butonunu göster
            document.getElementById('balance-cards').style.display = 'none';
            document.getElementById('no-account-banner').style.display = '';
            document.getElementById('quick-action-create-account').style.display = '';
        }
    } catch (e) {
        console.error('Balance load error:', e);
    }
}

window.createAccount = async function () {
    const btn = document.getElementById('btn-create-account');
    btn.disabled = true;
    btn.innerHTML = '<i class="ph ph-spinner"></i> Oluşturuluyor...';

    try {
        const userId = API.getUserId();
        const res = await API.call('/api/money-service/v1/accounts/createusermoney', 'POST', { id: userId });
        if (res && res.ok) {
            API.toast('Hesabınız başarıyla oluşturuldu!', 'success');
            window.loadData();
        } else {
            const err = await res.json().catch(() => ({}));
            API.toast('Hesap oluşturulamadı: ' + (err.message || 'Hata'), 'danger');
            btn.disabled = false;
            btn.innerHTML = '<i class="ph ph-plus"></i> Hesap Oluştur';
        }
    } catch (e) {
        API.toast('Bağlantı hatası', 'danger');
        btn.disabled = false;
        btn.innerHTML = '<i class="ph ph-plus"></i> Hesap Oluştur';
    }
};

// DUZELTME: STATUS_MAP tum TransactionStatus enum degerlerini kapsayacak sekilde guncellendi.
// Onceden sadece SUCCESS/PROGRESS/diger kontrolu vardi ve gercek enum degerleriyle eslesmiyordu.
const STATUS_MAP = {
    'CREATED':           { label: 'Oluşturuldu',        css: 'badge-info' },
    'VALIDATION_PENDING':{ label: 'Doğrulanıyor',       css: 'badge-warning' },
    'VALIDATION_FAILED': { label: 'Doğrulama Hatası',   css: 'badge-danger' },
    'INSUFFICIENT_FUNDS':{ label: 'Yetersiz Bakiye',    css: 'badge-danger' },
    'FRAUD_REVIEW':      { label: 'İncelemede',         css: 'badge-warning' },
    'FRAUD_REJECTED':    { label: 'Reddedildi',         css: 'badge-danger' },
    'FUNDS_BLOCKED':     { label: 'Bloke Edildi',       css: 'badge-warning' },
    'FUNDS_BLOCK_FAILED':{ label: 'Bloke Hatası',       css: 'badge-danger' },
    'BLOCK_MONEY':       { label: 'Para Blokede',       css: 'badge-warning' },
    'BLOCK_MONEY_FAILED':{ label: 'Bloke Hatası',       css: 'badge-danger' },
    'DEPOSIT_FAILED':    { label: 'Yatırma Hatası',     css: 'badge-danger' },
    'WITHDRAW_FAILED':   { label: 'Çekme Hatası',       css: 'badge-danger' },
    'PROCESSING':        { label: 'İşleniyor',          css: 'badge-warning' },
    'COMPLETED':         { label: 'Tamamlandı',         css: 'badge-success' },
    'FAILED':            { label: 'Başarısız',          css: 'badge-danger' },
    'KAFKA_ERROR':       { label: 'Sistem Hatası',      css: 'badge-danger' },
    'DECLINED':          { label: 'Reddedildi',         css: 'badge-danger' },
    'REVERSED':          { label: 'İptal Edildi',       css: 'badge-warning' },
};

const TYPE_MAP = {
    'TRANSFER':    'Transfer',
    'DEPOSIT':     'Para Yatırma',
    'WITHDRAW':    'Para Çekme',
};


function getStatusBadge(status, statusDescription, small = false) {
    const s = STATUS_MAP[status];
    const sizeStyle = small ? 'font-size:11px;' : '';
    if (s) {
        const title = statusDescription ? ` title="${statusDescription}"` : '';
        return `<span class="badge ${s.css}" style="${sizeStyle}"${title}>${s.label}</span>`;
    }
    return `<span class="badge badge-info" style="${sizeStyle}">${status || '-'}</span>`;
}

async function loadTransactions() {
    const wrapper = document.getElementById('transactions-wrapper');
    wrapper.innerHTML = `<div class="empty-state"><div class="spinner"></div><div class="empty-state-text" style="margin-top:12px">Hareketler yükleniyor...</div></div>`;

    try {
        // DUZELTME: onceden POST + body ile userId gonderiliyordu. Endpoint GET + ?id=... olarak duzeltildi.
        const userId = API.getUserId();
        const res = await API.call(`/api/transaction-service/v1/transactions/gettransactionhistorywithid?id=${userId}`, 'GET');

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

            data.sort((a, b) => new Date(b.localDateTime) - new Date(a.localDateTime));
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
                // DUZELTME: statusBadge tum enum degerlerini karsilayacak sekilde guncellendi.
                // statusDescription tooltip olarak badge'e eklendi.
                const statusBadge = getStatusBadge(tx.status, tx.statusDescription, true);

                const typeLabel = TYPE_MAP[tx.transactionType] || tx.transactionType || '-';

                let isOutflow = tx.senderUserId === userId && tx.transactionType !== 'DEPOSIT';
                let amountSign = isOutflow ? '-' : '+';
                let amountColor = isOutflow ? 'var(--danger)' : 'var(--success)';
                if (tx.transactionType === 'DEPOSIT') {
                    amountSign = '+';
                    amountColor = 'var(--success)';
                } else if (tx.transactionType === 'WITHDRAW') {
                    amountSign = '-';
                    amountColor = 'var(--danger)';
                }

                // DUZELTME: aciklama kisaltmasi 25 karakter'den 35'e cikarildi.
                const desc = tx.description
                    ? (tx.description.length > 35 ? tx.description.substring(0, 35) + '...' : tx.description)
                    : '-';

                // DUZELTME: islem nerede oldugu bilgisi - statusDescription satir alti olarak eklendi.
                const statusDesc = tx.statusDescription
                    ? `<div style="font-size:10px; color:var(--text-muted); margin-top:2px;">${tx.statusDescription}</div>`
                    : '';

                html += `
                    <tr>
                        <td style="font-size:13px;">${API.formatDate(tx.localDateTime)}</td>
                        <td style="font-weight:600; font-size:13px;">${typeLabel}</td>
                        <td style="color:var(--text-muted); font-size:13px;">${desc}</td>
                        <td style="color:${amountColor}; font-weight:700;">${amountSign}${API.formatMoney(tx.money)}</td>
                        <td>${statusBadge}${statusDesc}</td>
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