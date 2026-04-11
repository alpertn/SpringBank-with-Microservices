/* ===== SpringBank - Para Transferi JS ===== */

document.addEventListener('DOMContentLoaded', () => {
    if (!API.checkAuth()) return;
  
    // Sidebar Kullanıcı Bilgisi
    const user = API.getUserData();
    document.getElementById('user-name').innerText = user.name + ' ' + user.surname;
    if (user.roles?.includes('ADMIN')) {
      document.getElementById('admin-section').style.display = 'block';
      document.getElementById('admin-link').style.display = 'block';
      document.getElementById('user-role').innerText = 'Admin';
    }
  
    // Çıkış Butonu
    document.getElementById('logout-btn').addEventListener('click', () => API.logout());
  
    // Sayfa açıldığında bakiye getir
    loadBalance();
  
    // Form İşlemi
    const form = document.getElementById('transfer-form');
    const btn = document.getElementById('transfer-btn');
    const msg = document.getElementById('transfer-msg');
  
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      
      const receiverIban = document.getElementById('receiver-iban').value.trim();
      const receiverName = document.getElementById('receiver-name').value.trim();
      const receiverSurname = document.getElementById('receiver-surname').value.trim();
      const amount = parseFloat(document.getElementById('amount').value);
      const description = document.getElementById('description').value.trim();
  
      if (!receiverIban || !receiverName || !receiverSurname || !amount || amount <= 0) {
        API.showMsg(msg, '❌ Lütfen tüm zorunlu alanları eksiksiz giriniz.', 'danger');
        return;
      }

      // IBAN kontrolü — bakiye yüklenmediyse işlem başlatılmaz
      const senderIban = window.userIbanStr || '';
      if (!senderIban) {
        API.showMsg(msg, '❌ Hesap bilgisi yüklenemedi. Lütfen sayfayı yenileyip tekrar deneyiniz.', 'danger');
        await loadBalance();
        return;
      }

      // Kendi IBAN'ına transfer engeli
      if (senderIban === receiverIban) {
        API.showMsg(msg, '❌ Kendi hesabınıza transfer yapamazsınız.', 'danger');
        return;
      }
  
      btn.disabled = true;
      btn.innerHTML = '<span class="spinner"></span> İşleniyor...';
      API.hideMsg(msg);
  
      try {
        // Transaction Service'e Transfer İsteği At
        const dtData = {
          senderIban: senderIban,
          receiverIban: receiverIban,
          receiverName: receiverName,
          receiverSurname: receiverSurname,
          amount: amount,
          transactionType: 'TRANSFER',
          description: description || 'Para Transferi'
        };
  
        // API.call → ham Response objesi döner
        const res = await API.call('/api/transaction-service/v1/transactions/create', 'POST', dtData);
  
        if (res && res.ok) {
          form.reset();
          API.showMsg(msg, `✅ ${API.formatMoney(amount)} transfer talebi başarıyla alındı.`, 'success');
          API.toast(`Transfer talebi gönderildi`, 'success');
          setTimeout(loadBalance, 3000); // Kafka işlemesi için bekle
        } else if (res) {
          const errBody = await res.text().catch(() => '');
          const statusCode = res.status;
          if (statusCode === 400) {
            API.showMsg(msg, '❌ Yetersiz bakiye, yanlış IBAN veya geçersiz bilgi.', 'danger');
          } else if (statusCode === 500) {
            API.showMsg(msg, `❌ Sunucu hatası: ${errBody || 'Transfer gerçekleştirilemedi.'}`, 'danger');
          } else {
            API.showMsg(msg, `❌ Transfer işlemi reddedildi. (${statusCode})`, 'danger');
          }
        } else {
          API.showMsg(msg, '❌ Oturum süresi doldu. Lütfen tekrar giriş yapın.', 'danger');
        }
      } catch (err) {
        console.error(err);
        API.showMsg(msg, '❌ Sistemsel bir bağlantı hatası oluştu.', 'danger');
      } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="ph ph-paper-plane-right"></i> Transferi Başlat';
      }
    });
  });
  
  async function loadBalance(retryCount = 0) {
    const form = document.getElementById('transfer-form');
    const btn = document.getElementById('transfer-btn');
    const msg = document.getElementById('transfer-msg');
    try {
      const res = await API.call('/api/money-service/v1/accounts/balance-info', 'GET');
      if (res && res.ok) {
        const data = await res.json();
        document.getElementById('balance-display').innerText = API.formatMoney(data.money);
        document.getElementById('iban-display').innerText = 'IBAN: ' + data.userIban;
        window.userIbanStr = data.userIban;
        if (form) form.style.opacity = '1';
        if (btn) btn.disabled = false;
        if (msg) API.hideMsg(msg);
      } else if (res && res.status === 404) {
        console.warn('[transfer] Hesap bulunamadı (404)');
        if (form) form.style.opacity = '0.5';
        if (btn) btn.disabled = true;
        API.showMsg(msg, '⚠️ Hesabınız henüz oluşturulmamış. Lütfen önce Dashboard\'dan hesap oluşturun.', 'danger');
      } else if (res && res.status === 401) {
        console.warn('[transfer] Unauthorized (401)');
        API.showMsg(msg, '⚠️ Oturum süresi doldu. Lütfen tekrar giriş yapın.', 'danger');
      } else {
        console.warn('[transfer] Balance API returned non-ok:', res?.status);
        if (form) form.style.opacity = '0.5';
        if (btn) btn.disabled = true;
        API.showMsg(msg, '⚠️ Hesap bilgileri yüklenemedi. Sayfayı yenileyip tekrar deneyin.', 'danger');
      }
    } catch(e) {
      console.warn('[transfer] Balance yüklenemedi:', e?.message);
      if (form) form.style.opacity = '0.5';
      if (btn) btn.disabled = true;
      if (retryCount < 2) {
        API.showMsg(msg, '⏳ Hesap bilgileri yüklenemiyor, tekrar deneniyor...', 'danger');
        setTimeout(() => loadBalance(retryCount + 1), 2000);
      } else {
        API.showMsg(msg, '⚠️ Hesap bilgileri alınamadı. Sunucu yanıt vermiyor olabilir. Lütfen sayfayı yenileyin.', 'danger');
      }
    }
  }
