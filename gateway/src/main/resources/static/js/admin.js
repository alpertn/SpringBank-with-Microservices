// Auth Kontrolu ve Yetki Sinamasi
API.checkAuth();

// Sayfa yuklendiginde otomatik istatistikleri cek
window.addEventListener('DOMContentLoaded', () => loadStats());

// Toplam Kullanici Sayisini Getir
async function loadStats() {
    try {
        const response = await API.call('/api/user/admin/stats/total', 'GET');
        document.getElementById('totalUsers').textContent = (response && response.ok) ? await response.text() : '?';
    } catch (err) {
        document.getElementById('totalUsers').textContent = '?';
    }
}

// Isimle Kullanici Ara
async function searchByName() {
    const query = document.getElementById('searchInput').value.trim();
    if (!query) return;

    hideMsg('searchMsg');

    try {
        const response = await API.call(`/api/user/admin/search?query=${encodeURIComponent(query)}`, 'GET');
        if (response && response.ok) {
            renderUsers(await response.json());
        } else {
            API.showMsg('searchMsg', 'Arama başarısız.', 'danger');
        }
    } catch (err) {
        API.showMsg('searchMsg', 'Bağlantı hatası.', 'danger');
    }
}

// E-posta ile Kullanici Ara
async function searchByEmail() {
    const email = document.getElementById('searchInput').value.trim();
    if (!email) return;

    hideMsg('searchMsg');

    try {
        const response = await API.call(`/api/user/admin/search-by-email?email=${encodeURIComponent(email)}`, 'GET');
        if (response && response.ok) {
            renderUsers(await response.json());
        } else {
            API.showMsg('searchMsg', 'Arama başarısız.', 'danger');
        }
    } catch (err) {
        API.showMsg('searchMsg', 'Bağlantı hatası.', 'danger');
    }
}

// Arama Sonuclarini Tabloya Ciz
function renderUsers(users) {
    const table = document.getElementById('usersTable');
    const tbody = document.getElementById('usersBody');
    tbody.innerHTML = '';

    if (!users || users.length === 0) {
        table.style.display = 'none';
        return API.showMsg('searchMsg', 'Kullanıcı bulunamadı.', 'warning');
    }

    table.style.display = 'table';
    users.forEach(user => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td style="font-size:12px;word-break:break-all">${user.id || '-'}</td>
            <td>${user.name || '-'} ${user.surname || ''}</td>
            <td>${user.email || '-'}</td>
            <td><span class="badge badge-primary">${user.role || '-'}</span></td>
            <td><button class="btn btn-sm btn-outline-primary" onclick="fillRoleUpdate('${user.id}')">Rol Değiştir</button></td>
        `;
        tbody.appendChild(tr);
    });
}

// Rol Degistirme Formulune ID'yi Aktar
function fillRoleUpdate(userId) {
    document.getElementById('roleUserId').value = userId;
    document.getElementById('roleUserId').scrollIntoView({ behavior: 'smooth' });
}

// ID ile Kullanici Detayini Bul
async function findById() {
    const userId = document.getElementById('userIdInput').value.trim();
    if (!userId) return;

    const detailDiv = document.getElementById('userDetail');

    try {
        const response = await API.call(`/api/user/admin/finduserbyid/${encodeURIComponent(userId)}`, 'GET');
        if (response && response.ok) {
            const user = await response.json();
            detailDiv.style.display = 'block';
            detailDiv.innerHTML = `
                <div class="mt-2">
                    <div class="info-row"><span class="label">ID</span><span class="value" style="font-size:12px;word-break:break-all">${user.id || '-'}</span></div>
                    <div class="info-row"><span class="label">Ad Soyad</span><span class="value">${user.name || '-'} ${user.surname || ''}</span></div>
                    <div class="info-row"><span class="label">E-posta</span><span class="value">${user.email || '-'}</span></div>
                    <div class="info-row"><span class="label">Rol</span><span class="value">${user.role || '-'}</span></div>
                </div>`;
        } else {
            detailDiv.style.display = 'block';
            detailDiv.innerHTML = '<div class="alert alert-warning mt-2">Kullanıcı bulunamadı.</div>';
        }
    } catch (err) {
        detailDiv.style.display = 'block';
        detailDiv.innerHTML = '<div class="alert alert-danger mt-2">Bağlantı hatası.</div>';
    }
}

// Kullanici Rolunu Guncelle
async function updateRole() {
    const userId = document.getElementById('roleUserId').value.trim();
    const role = document.getElementById('roleSelect').value;

    if (!userId) return API.showMsg('roleMsg', 'Kullanıcı ID giriniz.', 'warning');
    hideMsg('roleMsg');

    try {
        const response = await API.call(`/api/user/admin/updaterole/${encodeURIComponent(userId)}?role=${role}`, 'PATCH');
        if (response && response.ok) {
            API.showMsg('roleMsg', 'Rol başarıyla güncellendi!', 'success');
            loadStats(); // Istatistikleri yenile
        } else {
            const text = response ? await response.text() : 'Bilinmeyen hata';
            API.showMsg('roleMsg', `Güncelleme hatası: ${text}`, 'danger');
        }
    } catch (err) {
        API.showMsg('roleMsg', 'Bağlantı hatası.', 'danger');
    }
}

// Mesaj Kutucugunu Gizle
function hideMsg(id) {
    const el = document.getElementById(id);
    if (el) el.style.display = 'none';
}
