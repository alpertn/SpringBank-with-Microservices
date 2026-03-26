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
  
      btn.disabled = true;
      btn.innerHTML = '<span class="spinner"></span> İşleniyor...';
      API.hideMsg(msg);
  
      try {
        const userId = API.getUserId();
        
        // 1. Transaction Service'e İstek At (Saga veya Event Driven Kafka Süreci)
        const dtData = {
          senderIban: window.userIbanStr || '',
          receiverIban: receiverIban,
          receiverName: receiverName,
          receiverSurname: receiverSurname,
          amount: amount,
          transactionType: 'TRANSFER',
          description: description || 'Para Transferi'
        };
  
        const res = await API.call('/api/transaction-service/v1/transactions/create', 'POST', dtData);
  
        if (res && res.ok) {
            form.reset();
            API.showMsg(msg, `✅ ${API.formatMoney(amount)} transfer talebi başarıyla alındı.`, 'success');
            API.toast(`Transfer talebi gönderildi`, 'success');
            setTimeout(loadBalance, 1000); // Kafka event işlemi sonrası yenile
        } else {
            const text = await res.text().catch(()=>'');
            if(res.status === 400 || res.status === 500) {
                API.showMsg(msg, '❌ Yetersiz bakiye, yanlış IBAN veya işlem hatası.', 'danger');
            } else {
                API.showMsg(msg, '❌ İşlem reddedildi.', 'danger');
            }
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
  
  async function loadBalance() {
    try {
      const res = await API.call('/api/money-service/v1/accounts/balance-info', 'GET');
      if (res && res.ok) {
        const data = await res.json();
        document.getElementById('balance-display').innerText = API.formatMoney(data.money);
        document.getElementById('iban-display').innerText = 'IBAN: ' + data.userIban;
        window.userIbanStr = data.userIban;
      }
    } catch(e) {}
  }
