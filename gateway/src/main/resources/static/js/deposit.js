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
  
      btn.disabled = true;
      btn.innerHTML = '<span class="spinner"></span> İşleniyor...';
      API.hideMsg(msg);
  
      try {
        const userId = API.getUserId();
        
        // 1. Transaction Service'e Deposit Ekle (Audit Logs + Kafka trigger for money-service)
        const dtData = {
          senderIban: document.getElementById('iban-display').innerText.replace('IBAN: ','').trim(),
          amount: amount,
          transactionType: 'DEPOSIT',
          description: description || 'Şubeden/ATMden Para Yatırma'
        };
  
        // Sadece Transaction Service'e istek atılır.
        // Backend'de bu servis Kafka event'i gönderir, Money Service bu event'i dinleyip bakiyeyi günceller.
        const res = await API.call('/api/transaction-service/v1/transactions/create', 'POST', dtData);
  
        if (res && res.ok) {
          form.reset();
          API.showMsg(msg, `✅ ${API.formatMoney(amount)} başarıyla işleme alındı.`, 'success');
          API.toast(`${API.formatMoney(amount)} yatırıldı`, 'success');
          
          // Biraz bekleyip bakiyeyi yenile (Kafka'nın işlemesi için)
          setTimeout(loadBalance, 1000);
        } else {
            const errBody = await res.text().catch(()=>'');
            API.showMsg(msg, `❌ Para yatırma işlemi başarısız oldu. ${errBody}`, 'danger');
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
    try {
      const res = await API.call('/api/money-service/v1/accounts/balance-info', 'GET');
      if (res && res.ok) {
        const data = await res.json();
        document.getElementById('balance-display').innerText = API.formatMoney(data.money);
        document.getElementById('iban-display').innerText = 'IBAN: ' + (data.userIban || '-');
      }
    } catch(e) {}
  }
