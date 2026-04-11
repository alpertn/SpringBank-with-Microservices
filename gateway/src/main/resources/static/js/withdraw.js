/* ===== SpringBank - Withdraw (Para Çek) JS ===== */

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
    const form = document.getElementById('withdraw-form');
    const btn = document.getElementById('withdraw-btn');
    const msg = document.getElementById('withdraw-msg');
  
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      
      const amount = parseFloat(document.getElementById('amount').value);
      const description = document.getElementById('description').value;
  
      if (!amount || amount <= 0) {
        API.showMsg(msg, '❌ Geçerli bir tutar giriniz.', 'danger');
        return;
      }
  
      // IBAN kontrolü — bakiye yüklenmediyse işlem başlatılmaz
      const senderIban = window.userIbanStr || '';
      if (!senderIban) {
        API.showMsg(msg, '❌ Hesap bilgisi yüklenemedi. Lütfen sayfayı yenileyip tekrar deneyiniz.', 'danger');
        await loadBalance();
        return;
      }
  
      btn.disabled = true;
      btn.innerHTML = '<span class="spinner"></span> İşleniyor...';
      API.hideMsg(msg);
  
      try {
        // Transaction Service'e Withdraw Ekle
        const dtData = {
          senderIban: senderIban,
          amount: amount,
          transactionType: 'WITHDRAW',
          description: description || 'Şubeden/ATMden Para Çekme'
        };
  
        // API.call → ham Response objesi döner
        const res = await API.call('/api/transaction-service/v1/transactions/create', 'POST', dtData);
  
        if (res && res.ok) {
          form.reset();
          API.showMsg(msg, `✅ ${API.formatMoney(amount)} çekim talebi alındı.`, 'success');
          API.toast(`${API.formatMoney(amount)} çekildi`, 'success');
          setTimeout(loadBalance, 3000); // Kafka işlemesi için bekle
        } else if (res) {
          const errBody = await res.text().catch(() => '');
          const statusCode = res.status;
          if (statusCode === 400) {
            API.showMsg(msg, '❌ Geçersiz istek: Yetersiz bakiye veya hatalı bilgi.', 'danger');
          } else if (statusCode === 500) {
            API.showMsg(msg, `❌ Sunucu hatası: ${errBody || 'İşlem gerçekleştirilemedi.'}`, 'danger');
          } else {
            API.showMsg(msg, `❌ Para çekme işlemi başarısız. (${statusCode})`, 'danger');
          }
        } else {
          API.showMsg(msg, '❌ Oturum süresi doldu. Lütfen tekrar giriş yapın.', 'danger');
        }
      } catch (err) {
        console.error(err);
        API.showMsg(msg, '❌ Sistemsel bir bağlantı hatası oluştu.', 'danger');
      } finally {
        btn.disabled = false;
        btn.innerHTML = '🏧 Para Çek';
      }
    });
  });
  
  async function loadBalance() {
    const form = document.getElementById('withdraw-form');
    const btn = document.getElementById('withdraw-btn');
    const msg = document.getElementById('withdraw-msg');
    try {
      const res = await API.call('/api/money-service/v1/accounts/balance-info', 'GET');
      if (res && res.ok) {
        const data = await res.json();
        document.getElementById('balance-display').innerText = API.formatMoney(data.money);
        // API'de 'blockedMoney' veya 'blockedmoney' olabilir, her ikisini de dene
        const blocked = data.blockedMoney ?? data.blockedmoney ?? 0;
        document.getElementById('blocked-display').innerText = 'Blokeli Bakiye: ' + API.formatMoney(blocked);
        window.userIbanStr = data.userIban;
        // DÜZELTME: Bakiye başarıyla yüklendi, formu aktif et
        if (form) form.style.opacity = '1';
        if (btn) btn.disabled = false;
        if (msg) API.hideMsg(msg);
      } else {
        // DÜZELTME: API çağrısı başarısız — formu devre dışı bırak ve kullanıcıyı bilgilendir
        console.warn('[withdraw] Balance API returned non-ok:', res?.status);
        if (form) form.style.opacity = '0.5';
        if (btn) btn.disabled = true;
        API.showMsg(msg, '⚠️ Hesap bilgileri yüklenemedi. Hesabınız henüz oluşturulmamış olabilir veya sunucu hatası oluştu. Sayfayı yenileyip tekrar deneyin.', 'danger');
      }
    } catch(e) {
      console.warn('[withdraw] Balance yüklenemedi:', e?.message);
      // DÜZELTME: İstisna durumunda da formu devre dışı bırak
      if (form) form.style.opacity = '0.5';
      if (btn) btn.disabled = true;
      API.showMsg(msg, '⚠️ Hesap bilgileri alınamadı. Lütfen internet bağlantınızı kontrol edip sayfayı yenileyin.', 'danger');
    }
  }
