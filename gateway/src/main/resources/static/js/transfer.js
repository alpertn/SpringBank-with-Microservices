// Sayfa acilirken oturum kontrolu
API.checkAuth();

// Para Transferi Istegi
document.getElementById('transferForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    // UI Elementleri
    const btn = document.getElementById('transferBtn');
    const msgBox = document.getElementById('msg');

    // Yukleme Durumu
    btn.disabled = true;
    btn.textContent = 'Gönderiliyor...';
    msgBox.style.display = 'none';

    // Istek verisi
    const payload = {
        senderIban: document.getElementById('senderIban').value.trim(),
        receiverIban: document.getElementById('receiverIban').value.trim(),
        receiverName: document.getElementById('receiverName').value.trim(),
        receiverSurname: document.getElementById('receiverSurname').value.trim(),
        money: parseFloat(document.getElementById('amount').value),
        description: document.getElementById('description').value.trim()
    };

    try {
        // Backend e API Call
        const response = await API.call('/api/transactions/create', 'POST', payload);

        if (response && response.ok) {
            API.showMsg(msgBox, 'Transfer başarıyla gerçekleştirildi!', 'success');
            document.getElementById('transferForm').reset(); // Formu sifirla
        } else {
            const text = response ? await response.text() : 'Bilinmeyen hata';
            API.showMsg(msgBox, 'Transfer hatası: ' + text, 'danger');
        }
    } catch (err) {
        API.showMsg(msgBox, 'Bağlantı hatası: ' + err.message, 'danger');
    }

    // Islem Sonu
    btn.disabled = false;
    btn.textContent = 'Gönder';
});
