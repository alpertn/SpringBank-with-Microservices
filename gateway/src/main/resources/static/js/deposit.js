/* ===== SpringBank - Deposit (Para Yatır) JS ===== */

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
    const form = document.getElementById('deposit-form');
    const btn = document.getElementById('deposit-btn');
    const msg = document.getElementById('deposit-msg');
  
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      
      const amount = parseFloat(document.getElementById('amount').value);
      const description = document.getElementById('description').value;
  
      if (!amount || amount <= 0) {
        API.showMsg(msg, '❌ Geçerli bir tutar giriniz.', 'danger');
        return;
      }
  
      // DÜZELTME: IBAN artık DOM text'inden değil, window.userIbanStr'den alınıyor (withdraw.js ile tutarlı)
      const senderIban = window.userIbanStr || '';
      if (!senderIban) {
        API.showMsg(msg, '❌ Hesap IBAN bilgisi yüklenemedi. Lütfen sayfayı yenileyiniz.', 'danger');
        await loadBalance();
        return;
      }
  
      btn.disabled = true;
      btn.innerHTML = '<span class="spinner"></span> İşleniyor...';
      API.hideMsg(msg);
  
      try {
        // 1. Transaction Service'e Deposit Ekle (Audit Logs + Kafka trigger for money-service)
        const dtData = {
          senderIban: senderIban,
          amount: amount,
          transactionType: 'DEPOSIT',
          description: description || 'Şubeden/ATMden Para Yatırma'
        };
  
        // API.call → ham Response objesi döner
        const res = await API.call('/api/transaction-service/v1/transactions/create', 'POST', dtData);
  
        if (res && res.ok) {
          form.reset();
          API.showMsg(msg, `✅ ${API.formatMoney(amount)} başarıyla işleme alındı.`, 'success');
          API.toast(`${API.formatMoney(amount)} yatırıldı`, 'success');
          setTimeout(loadBalance, 3000); // Kafka işlemesi için bekle
        } else if (res) {
          const errBody = await res.text().catch(() => '');
          const statusCode = res.status;
          if (statusCode === 400) {
            API.showMsg(msg, '❌ Geçersiz istek: IBAN veya tutar hatalı olabilir.', 'danger');
          } else if (statusCode === 500) {
            API.showMsg(msg, `❌ Sunucu hatası: ${errBody || 'İşlem gerçekleştirilemedi.'}`, 'danger');
          } else {
            API.showMsg(msg, `❌ Para yatırma işlemi başarısız oldu. (${statusCode})`, 'danger');
          }
        } else {
          API.showMsg(msg, '❌ Oturum süresi doldu. Lütfen tekrar giriş yapın.', 'danger');
        }
      } catch (err) {
        console.error(err);
        API.showMsg(msg, '❌ Sistemsel bir bağlantı hatası oluştu.', 'danger');
      } finally {
        btn.disabled = false;
        btn.innerHTML = '💰 Hesabıma Yatır';
      }
    });
  });
  
  async function loadBalance() {
    const form = document.getElementById('deposit-form');
    const btn = document.getElementById('deposit-btn');
    const msg = document.getElementById('deposit-msg');
    try {
      const res = await API.call('/api/money-service/v1/accounts/balance-info', 'GET');
      if (res && res.ok) {
        const data = await res.json();
        document.getElementById('balance-display').innerText = API.formatMoney(data.money);
        document.getElementById('iban-display').innerText = 'IBAN: ' + (data.userIban || '-');
        // DÜZELTME: window.userIbanStr'e de kaydet (senderIban için kullanılıyor)
        window.userIbanStr = data.userIban;
        if (form) form.style.opacity = '1';
        if (btn) btn.disabled = false;
      } else {
        console.warn('[deposit] Balance API returned non-ok:', res?.status);
        if (form) form.style.opacity = '0.5';
        if (btn) btn.disabled = true;
        API.showMsg(msg, '⚠️ Hesap bilgileri yüklenemedi. Sayfayı yenileyip tekrar deneyin.', 'danger');
      }
    } catch(e) {
      console.warn('[deposit] Balance yüklenemedi:', e?.message);
      if (form) form.style.opacity = '0.5';
      if (btn) btn.disabled = true;
      API.showMsg(msg, '⚠️ Hesap bilgileri alınamadı. Lütfen internet bağlantınızı kontrol edin.', 'danger');
    }
  }
