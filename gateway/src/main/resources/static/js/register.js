// Sisteme Yeni Kullanici Kaydi Istegi
document.getElementById('registerForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    // UI Elementleri
    const btn = document.getElementById('regBtn');
    const msgBox = document.getElementById('msg');

    // Yukleme durumu
    btn.disabled = true;
    btn.textContent = 'Kayıt yapılıyor...';
    msgBox.style.display = 'none';

    // Form verisi
    const payload = {
        Name: document.getElementById('firstName').value.trim(),
        surname: document.getElementById('lastName').value.trim(),
        email: document.getElementById('email').value.trim(),
        password: document.getElementById('password').value
    };

    try {
        // Kaydedici Backend cagirisi
        const response = await fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            API.showMsg(msgBox, 'Kayıt başarılı! Giriş sayfasına yönlendiriliyorsunuz...', 'success');
            // Basarilisa 2 saniye sonra logine geri don
            setTimeout(() => window.location.href = '/login.html', 2000);
        } else {
            const text = await response.text();
            API.showMsg(msgBox, 'Kayıt hatası: ' + text, 'danger');
            btn.disabled = false;
            btn.textContent = 'Kayıt Ol';
        }
    } catch (err) {
        API.showMsg(msgBox, 'Sunucuya bağlanılamadı!', 'danger');
        btn.disabled = false;
        btn.textContent = 'Kayıt Ol';
    }
});
