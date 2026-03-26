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
  
      btn.disabled = true;
      btn.innerHTML = '<span class="spinner"></span> İşleniyor...';
      API.hideMsg(msg);
  
      try {
        const userId = API.getUserId();
        
        // 1. Transaction Service'e Withdraw Ekle (Audit Logs + Kafka trigger for money-service)
        const dtData = {
          senderIban: window.userIbanStr || '',
          amount: amount,
          transactionType: 'WITHDRAW',
          description: description || 'Şubeden/ATMden Para Çekme'
        };
  
        // Sadece Transaction Service'e istek atılır.
        const res = await API.call('/api/transaction-service/v1/transactions/create', 'POST', dtData);
  
        if (res && res.ok) {
            form.reset();
            API.showMsg(msg, `✅ ${API.formatMoney(amount)} çekim talebi alındı.`, 'success');
            API.toast(`${API.formatMoney(amount)} çekildi`, 'success');
            setTimeout(loadBalance, 1000); // Kafka event işlemi sonrası yenile
        } else {
            // Hata veya Yetersiz Bakiye
            const text = await res.text().catch(()=>'');
            if(res.status === 400 || res.status === 500) {
                API.showMsg(msg, '❌ Yetersiz bakiye veya işlem hatası.', 'danger');
            } else {
                API.showMsg(msg, '❌ İşlem reddedildi.', 'danger');
            }
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
    try {
      const res = await API.call('/api/money-service/v1/accounts/balance-info', 'GET');
      if (res && res.ok) {
        const data = await res.json();
        document.getElementById('balance-display').innerText = API.formatMoney(data.money);
        document.getElementById('blocked-display').innerText = 'Blokeli Bakiye: ' + API.formatMoney(data.blockedmoney);
        window.userIbanStr = data.userIban;
      }
    } catch(e) {}
  }
