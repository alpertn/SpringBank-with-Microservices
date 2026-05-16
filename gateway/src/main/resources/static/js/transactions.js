/* ===== SpringBank - Transactions JS ===== */

document.addEventListener('DOMContentLoaded', () => {
    if (!API.checkAuth()) return;

    const user = API.getUserData();
    document.getElementById('user-name').innerText = `${user.name || ''} ${user.surname || ''}`.trim();
    if (user.roles?.includes('ADMIN')) {
        document.getElementById('admin-section').style.display = 'block';
        document.getElementById('admin-link').style.display = 'block';
        document.getElementById('user-role').innerText = 'Admin';
    }

    document.getElementById('logout-btn').addEventListener('click', () => API.logout());
    loadTransactions();
});

const STATUS_MAP = {
    CREATED: { label: 'Olusturuldu', css: 'badge-info' },
    VALIDATION_PENDING: { label: 'Dogrulaniyor', css: 'badge-warning' },
    VALIDATION_FAILED: { label: 'Dogrulama Hatasi', css: 'badge-danger' },
    INSUFFICIENT_FUNDS: { label: 'Yetersiz Bakiye', css: 'badge-danger' },
    FRAUD_REVIEW: { label: 'Incelemede', css: 'badge-warning' },
    FRAUD_REJECTED: { label: 'Reddedildi', css: 'badge-danger' },
    BLOCK_MONEY: { label: 'Tutar Bloke Edildi', css: 'badge-warning' },
    FUNDS_BLOCKED: { label: 'Bloke Edildi', css: 'badge-warning' },
    BLOCK_MONEY_FAILED: { label: 'Bloke Hatasi', css: 'badge-danger' },
    FUNDS_BLOCK_FAILED: { label: 'Bloke Hatasi', css: 'badge-danger' },
    PROCESSING: { label: 'Isleniyor', css: 'badge-warning' },
    COMPLETED: { label: 'Tamamlandi', css: 'badge-success' },
    FAILED: { label: 'Basarisiz', css: 'badge-danger' },
    DEPOSIT_FAILED: { label: 'Yatirma Basarisiz', css: 'badge-danger' },
    WITHDRAW_FAILED: { label: 'Cekme Basarisiz', css: 'badge-danger' },
    KAFKA_ERROR: { label: 'Sistem Hatasi', css: 'badge-danger' },
    DECLINED: { label: 'Reddedildi', css: 'badge-danger' },
    REVERSED: { label: 'Geri Alindi', css: 'badge-warning' },
    CANCELLED: { label: 'Geri Cekildi', css: 'badge-warning' },
};

const TYPE_MAP = {
    TRANSFER: 'Transfer',
    DEPOSIT: 'Para Yatirma',
    WITHDRAW: 'Para Cekme',
};

function getStatusBadge(status, statusDescription) {
    const s = STATUS_MAP[status];
    if (s) {
        const title = statusDescription ? ` title="${statusDescription}"` : '';
        return `<span class="badge ${s.css}"${title}>${s.label}</span>`;
    }
    return `<span class="badge badge-info">${status || '-'}</span>`;
}

window.loadTransactions = async function () {
    const wrapper = document.getElementById('transactions-wrapper');
    wrapper.innerHTML = `<div class="empty-state"><div class="spinner" style="border-top-color:var(--primary);margin-bottom:10px;"></div><div class="empty-state-text">Hareketler yukleniyor...</div></div>`;

    try {
        const userId = API.getUserId();
        const res = await API.call(`/api/transaction-service/v1/transactions/gettransactionhistorywithid?id=${userId}`, 'GET');

        if (!res || !res.ok) {
            wrapper.innerHTML = `<div class="empty-state"><div class="empty-state-icon">!</div><div class="empty-state-text">Veriler alinamadi.</div></div>`;
            return;
        }

        const data = await res.json();
        if (!data || data.length === 0) {
            wrapper.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">-</div>
                    <div class="empty-state-text">Henuz hicbir hesap hareketiniz bulunmuyor.</div>
                </div>
            `;
            return;
        }

        data.sort((a, b) => new Date(b.localDateTime) - new Date(a.localDateTime));

        let html = `
            <table>
                <thead>
                    <tr>
                        <th>Tarih</th>
                        <th>Islem Turu</th>
                        <th>Aciklama</th>
                        <th>Gonderen</th>
                        <th>Alici</th>
                        <th>Tutar</th>
                        <th>Durum</th>
                        <th>Detay</th>
                        <th>Islem</th>
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

            const rowStyle = tx.error ? 'background:rgba(var(--danger-rgb,220,53,69),0.06);' : '';
            const detailText = tx.statusDescription || '-';
            const canCancel = tx.eventId && !['FAILED', 'CANCELLED', 'REVERSED'].includes(tx.status);
            const cancelAction = canCancel
                ? `<button class="btn btn-sm btn-outline" onclick="cancelTransaction('${tx.eventId}')">Geri Cek</button>`
                : `<span style="font-size:12px;color:var(--text-muted);">-</span>`;

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
                    <td>${cancelAction}</td>
                </tr>
            `;
        });

        html += `</tbody></table>`;
        wrapper.innerHTML = html;
    } catch (e) {
        console.error('Transactions load error:', e);
        wrapper.innerHTML = `<div class="empty-state"><div class="empty-state-icon">!</div><div class="empty-state-text">Baglanti hatasi olustu.</div></div>`;
    }
};

window.cancelTransaction = async function (eventId) {
    if (!eventId) return;
    if (!confirm('Bu islemi geri cekmek istediginize emin misiniz?')) return;
    API.showLoading('Islem geri cekiliyor...');
    try {
        const res = await API.call(`/api/transaction-service/v1/transactions/cancel?eventUUID=${encodeURIComponent(eventId)}`, 'POST');
        if (res && res.ok) {
            API.toast('Islem geri cekildi', 'success');
            await loadTransactions();
        } else {
            const text = res ? await res.text().catch(() => '') : '';
            API.toast(`Geri cekme basarisiz: ${text || 'Bilinmeyen hata'}`, 'danger');
        }
    } catch (e) {
        console.error('Cancel transaction error:', e);
        API.toast('Geri cekme sirasinda baglanti hatasi olustu', 'danger');
    } finally {
        API.hideLoading();
    }
};
