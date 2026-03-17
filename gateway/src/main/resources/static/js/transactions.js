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

// DUZELTME: TransactionStatus enum degerlerini Turkce karsiliklarina ve renk sinifina esler.
// Onceden sadece SUCCESS/PROGRESS/diger kontrolu vardi, gercek enum degerleri hic karsilastirilmiyordu.
const STATUS_MAP = {
    'CREATED':           { label: 'Oluşturuldu',        css: 'badge-info' },
    'VALIDATION_PENDING':{ label: 'Doğrulanıyor',       css: 'badge-warning' },
    'VALIDATION_FAILED': { label: 'Doğrulama Hatası',   css: 'badge-danger' },
    'INSUFFICIENT_FUNDS':{ label: 'Yetersiz Bakiye',    css: 'badge-danger' },
    'FRAUD_REVIEW':      { label: 'İncelemede',         css: 'badge-warning' },
    'FRAUD_REJECTED':    { label: 'Reddedildi',         css: 'badge-danger' },
    'FUNDS_BLOCKED':     { label: 'Bloke Edildi',       css: 'badge-warning' },
    'FUNDS_BLOCK_FAILED':{ label: 'Bloke Hatası',       css: 'badge-danger' },
    'PROCESSING':        { label: 'İşleniyor',          css: 'badge-warning' },
    'COMPLETED':         { label: 'Tamamlandı',         css: 'badge-success' },
    'FAILED':            { label: 'Başarısız',          css: 'badge-danger' },
    'KAFKA_ERROR':       { label: 'Sistem Hatası',      css: 'badge-danger' },
    'DECLINED':          { label: 'Reddedildi',         css: 'badge-danger' },
    'REVERSED':          { label: 'İptal Edildi',       css: 'badge-warning' },
};

// DUZELTME: islem tipi etiketlerini Turkce karsiliklar ile goster.
const TYPE_MAP = {
    'TRANSACTION': 'Transfer',
    'DEPOSIT':     'Para Yatırma',
    'WITHDRAW':    'Para Çekme',
};

function getStatusBadge(status, statusDescription) {
    const s = STATUS_MAP[status];
    if (s) {
        // statusDescription varsa tooltip olarak goster
        const title = statusDescription ? ` title="${statusDescription}"` : '';
        return `<span class="badge ${s.css}"${title}>${s.label}</span>`;
    }
    return `<span class="badge badge-info">${status || '-'}</span>`;
}

window.loadTransactions = async function () {
    const wrapper = document.getElementById('tx-wrapper');
    wrapper.innerHTML = `<div class="empty-state"><div class="spinner" style="border-top-color:var(--primary);margin-bottom:10px;"></div><div class="empty-state-text">Hareketler yükleniyor...</div></div>`;

    try {
        // DUZELTME: onceden POST + body ile userId gonderiliyordu. Endpoint GET + ?id=... olarak duzeltildi.
        const userId = API.getUserId();
        const res = await API.call(`/api/transaction-service/v1/transactions/gettransactionhistorywithid?id=${userId}`, 'GET');

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
            data.sort((a, b) => new Date(b.localDateTime) - new Date(a.localDateTime));

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
                            <th>Detay</th>
                        </tr>
                    </thead>
                    <tbody>
            `;

            data.forEach(tx => {
                const statusBadge = getStatusBadge(tx.status, tx.statusDescription);

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

                // DUZELTME: hata varsa satiri belirgin yap
                const rowStyle = tx.error ? 'background:rgba(var(--danger-rgb,220,53,69),0.06);' : '';

                // statusDescription detay satiri
                const detailText = tx.statusDescription || '-';

                html += `
                    <tr style="${rowStyle}">
                        <td>${API.formatDate(tx.localDateTime)}</td>
                        <td><strong>${typeLabel}</strong></td>
                        <td><span style="color:var(--text-muted);font-size:12px;">${tx.description || '-'}</span></td>
                        <td>${tx.senderName ? tx.senderName + ' ' + (tx.senderSurname || '') : '-'}</td>
                        <td>${tx.receiverName ? tx.receiverName + ' ' + (tx.receiverSurname || '') : '-'}</td>
                        <td style="color:${amountColor}; font-weight:700;">${amountSign}${API.formatMoney(tx.money)}</td>
                        <td>${statusBadge}</td>
                        <td style="font-size:11px; color:var(--text-muted); max-width:200px; white-space:normal;">${detailText}</td>
                    </tr>
                `;
            });

            html += `</tbody></table>`;
            wrapper.innerHTML = html;
        } else {
            wrapper.innerHTML = `<div class="empty-state"><div class="empty-state-icon">⚠️</div><div class="empty-state-text">Veriler alınamadı.</div></div>`;
        }
    } catch (e) {
        console.error('Transactions load error:', e);
        wrapper.innerHTML = `<div class="empty-state"><div class="empty-state-icon">🔌</div><div class="empty-state-text">Bağlantı hatası oluştu.</div></div>`;
    }
};