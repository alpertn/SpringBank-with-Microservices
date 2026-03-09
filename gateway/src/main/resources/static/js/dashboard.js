// Auth kontrolu ve JWT Parse
API.checkAuth();
const currentUser = API.parseJwt();
const currentUserId = currentUser ? currentUser.sub : null;
const currentUserName = currentUser ? `${currentUser.given_name} ${currentUser.family_name}` : '-';

// Sayfa yuklendiginde
document.addEventListener('DOMContentLoaded', async () => {
    document.getElementById('userNameSurname').textContent = currentUserName;
    await loadMyAccount(); // Kullanici hesabini cek
});

// Oturum acmis kullanicinin hesap bilgilerini getir
async function loadMyAccount() {
    try {
        const response = await API.call('/api/accounts/getUserIbanWithUserId', 'POST', { userId: currentUserId });

        if (response && response.ok) {
            const data = await response.json();

            // Hesap verilerini UI'a yansit
            document.getElementById('balance').textContent = `₺${data.money || '0.00'}`;
            document.getElementById('userIban').textContent = data.userIban || '-';
            document.getElementById('blockedMoney').textContent = `₺${data.blockedmoney || data.blockedMoney || '0.00'}`;

            // Islem alanini ac, hesap acma alanini gizle
            document.getElementById('transactionArea').style.display = 'flex';
            document.getElementById('createAccountArea').style.display = 'none';

            // Hesap varsa islem gecmisini de getir
            loadTransactionHistory();
        } else {
            // Hesap yoksa hesap acma ekranini goster
            document.getElementById('transactionArea').style.display = 'none';
            document.getElementById('createAccountArea').style.display = 'block';
            document.getElementById('transactionHistoryArea').style.display = 'none';
        }
    } catch (err) {
        console.error('Hesap yuklenirken hata:', err);
    }
}

// Yeni Banka Hesabi Olustur
async function createAccount() {
    const msgBox = document.getElementById('createAccountMsg');
    msgBox.style.display = 'none';

    try {
        const response = await API.call('/api/accounts/createusermoney', 'POST', { id: currentUserId });
        if (response && response.ok) {
            API.showMsg(msgBox, 'Hesabınız başarıyla oluşturuldu! Sayfa yenileniyor...', 'success');
            setTimeout(() => window.location.reload(), 1500);
        } else {
            API.showMsg(msgBox, 'Hesap oluşturulurken bir hata meydana geldi.', 'danger');
        }
    } catch (err) {
        API.showMsg(msgBox, 'Sunucu bağlantı hatası.', 'danger');
    }
}

// Para Yatir (Kendi Hesabina)
async function deposit() {
    const amount = document.getElementById('depositAmount').value;
    const msgBox = document.getElementById('depositMsg');

    if (!amount) return API.showMsg(msgBox, 'Lütfen tutar alanını doldurunuz.', 'warning');

    try {
        const response = await API.call('/api/accounts/depositByUserId', 'POST', { userId: currentUserId, amount });

        if (response && response.ok) {
            API.showMsg(msgBox, 'İşlem Başarılı: Hesabınıza para yatırıldı.', 'success');
            document.getElementById('depositAmount').value = '';
            addNotification(`Hesabınıza ₺${amount} yatırıldı.`);
            loadMyAccount(); // Guncel bakiyeyi yenile
        } else {
            const text = response ? await response.text() : 'Bilinmeyen Hata';
            API.showMsg(msgBox, `İşlem Başarısız: ${text}`, 'danger');
        }
    } catch (err) {
        API.showMsg(msgBox, 'Sunucu bağlantı hatası.', 'danger');
    }
}

// Para Cek (Kendi Hesabindan)
async function withdraw() {
    const amount = document.getElementById('withdrawAmount').value;
    const msgBox = document.getElementById('withdrawMsg');

    if (!amount) return API.showMsg(msgBox, 'Lütfen tutar alanını doldurunuz.', 'warning');

    try {
        const response = await API.call('/api/accounts/withdrawByUserId', 'POST', { userId: currentUserId, amount });

        if (response && response.ok) {
            API.showMsg(msgBox, 'İşlem Başarılı: Hesabınızdan para çekildi.', 'success');
            document.getElementById('withdrawAmount').value = '';
            addNotification(`Hesabınızdan ₺${amount} çekildi.`);
            loadMyAccount(); // Guncel bakiyeyi yenile
        } else {
            const text = response ? await response.text() : 'Bilinmeyen Hata';
            API.showMsg(msgBox, `İşlem Başarısız: Yetersiz bakiye veya sistem hatası. (${text})`, 'danger');
        }
    } catch (err) {
        API.showMsg(msgBox, 'Sunucu bağlantı hatası.', 'danger');
    }
}

// Baska Hesap Sorgula (Mock - Devre Disi)
function queryAccount() {
    const iban = document.getElementById('queryIban').value.trim();
    if (!iban) return;

    const resultDiv = document.getElementById('queryResult');
    resultDiv.style.display = 'block';

    // Eski endpoint ID almaya programlandigi icin mock veriyoruz
    resultDiv.innerHTML = '<div class="alert alert-info" style="font-size:13px">Hızlı IBAN sorgulama özelliği geçici olarak devre dışıdır.</div>';
}

// Islemler Gecmisini Getir
async function loadTransactionHistory() {
    try {
        const response = await API.call('/api/transactions/gettransactionhistorywithid', 'POST', currentUserId);

        if (response && response.ok) {
            const data = await response.json();
            const tbody = document.getElementById('transactionHistoryTableBody');

            if (!data || data.length === 0) {
                tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-3">Henüz bir işleminiz bulunmuyor.</td></tr>';
            } else {
                tbody.innerHTML = '';
                data.forEach(txn => {
                    const tr = document.createElement('tr');

                    // Tarihi formatla
                    const dateObj = new Date(txn.localDateTime);
                    const formattedDate = dateObj.toLocaleString('tr-TR', {
                        year: 'numeric', month: '2-digit', day: '2-digit',
                        hour: '2-digit', minute: '2-digit'
                    });

                    // Tutar formatla (Gelen mi, giden mi belirlemek icin)
                    let amountHtml = `₺${txn.money}`;
                    if (txn.senderUserId === currentUserId && txn.receiverUserId !== currentUserId) {
                        amountHtml = `<span class="text-danger">-₺${txn.money}</span>`;
                    } else if (txn.receiverUserId === currentUserId && txn.senderUserId !== currentUserId) {
                        amountHtml = `<span class="text-success">+₺${txn.money}</span>`;
                    }

                    // Durumu formatla
                    let statusBadge = '';
                    if (txn.status === 'SUCCESS' || txn.status === 'COMPLETED' || txn.status === 'SUCCESSFUL') {
                        statusBadge = '<span class="badge badge-success">Tamamlandı</span>';
                    } else if (txn.status === 'ERROR' || txn.status === 'FAILED') {
                        statusBadge = '<span class="badge badge-danger">Hata</span>';
                    } else {
                        statusBadge = `<span class="badge badge-warning">${txn.status}</span>`;
                    }

                    tr.innerHTML = `
                        <td class="align-middle">${formattedDate}</td>
                        <td class="align-middle">${txn.transactionType || '-'}</td>
                        <td class="align-middle" title="${txn.senderIban}">${txn.senderUserId === currentUserId ? 'Siz' : (txn.senderIban || '-')}</td>
                        <td class="align-middle" title="${txn.receiverIban}">${txn.receiverUserId === currentUserId ? 'Siz' : (txn.receiverName + ' ' + txn.receiverSurname)}</td>
                        <td class="align-middle font-weight-bold">${amountHtml}</td>
                        <td class="align-middle">${statusBadge}</td>
                    `;
                    tbody.appendChild(tr);
                });
            }
            document.getElementById('transactionHistoryArea').style.display = 'block';
        } else {
            console.warn('Islem gecmisi alinamadi.');
        }
    } catch (err) {
        console.error('Islem gecmisi yuklenirken hata:', err);
    }
}

// Sag Ust Bildirim Ekleme
function addNotification(msg) {
    const list = document.getElementById('notificationList');

    const newItem = document.createElement('a');
    newItem.className = 'dropdown-item py-2 border-bottom text-success font-weight-bold';
    newItem.style.fontSize = '13px';
    newItem.href = '#';
    newItem.textContent = msg;

    list.insertBefore(newItem, list.firstChild);

    const badge = document.getElementById('notifBadge');
    badge.textContent = parseInt(badge.textContent || 0) + 1;
}
