// Giris yapilmissa dogrudan Dashboard'a yonlendir
if (API.getToken()) window.location.href = '/dashboard.html';

// Giris Formu Gonderme Islemi
document.getElementById('loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    // UI Elementleri
    const btn = document.getElementById('loginBtn');
    const msgBox = document.getElementById('msg');
    const email = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;

    // Butonu kilitlerken yukleme efekti ver
    btn.disabled = true;
    btn.textContent = 'Giriş yapılıyor...';
    msgBox.style.display = 'none';

    try {
        // Auth istegini gonder
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });

        if (response.ok) {
            const data = await response.json();
            if (data.access_token) {
                // Tokenlari kaydet ve iceri al
                API.setTokens(data.access_token, data.refresh_token);
                window.location.href = '/dashboard.html';
            } else {
                API.showMsg(msgBox, 'Token alınamadı.', 'danger');
            }
        } else {
            API.showMsg(msgBox, 'E-posta veya şifre hatalı.', 'danger');
        }
    } catch (err) {
        API.showMsg(msgBox, 'Sunucuya bağlanılamadı!', 'danger');
    }

    // Islem bitince butonu eski haline getir
    btn.disabled = false;
    btn.textContent = 'Giriş Yap';
});
