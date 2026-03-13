/* ===== SpringBank - Login Sayfası JS ===== */

document.addEventListener('DOMContentLoaded', () => {
  // Zaten giriş yapıldıysa dashboard'a yönlendir
  if (API.getToken()) {
    window.location.href = '/dashboard.html';
    return;
  }

  const form = document.getElementById('login-form');
  const btn  = document.getElementById('login-btn');
  const msg  = document.getElementById('login-msg');

  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const email    = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;

    if (!email || !password) {
      API.showMsg(msg, '❌ E-posta ve şifre gereklidir.', 'danger');
      return;
    }

    // Buton yükleniyor durumu
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner"></span> Giriş yapılıyor...';
    API.hideMsg(msg);

    try {
      // POST /api/auth-service/v1/auth/login
      const res = await fetch('/api/auth-service/v1/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      });

      if (res.ok) {
        const data = await res.json();
        // Token'ları kaydet
        API.setTokens(data.access_token, data.refresh_token);

        // JWT'den kullanıcı bilgisi çek
        const parsed = API.parseJwt();
        if (parsed) {
          API.setUser({
            id:      parsed.sub,
            email:   parsed.email || email,
            name:    parsed.given_name || parsed.name || '',
            surname: parsed.family_name || '',
            roles:   parsed?.realm_access?.roles || []
          });
        }

        API.showMsg(msg, '✅ Giriş başarılı, yönlendiriliyorsunuz...', 'success');
        setTimeout(() => window.location.href = '/dashboard.html', 800);
      } else {
        const errText = await res.text().catch(() => '');
        API.showMsg(msg, `❌ Giriş başarısız (${res.status}). Email veya şifre hatalı.`, 'danger');
      }
    } catch (err) {
      API.showMsg(msg, '❌ Sunucuya bağlanılamadı. Lütfen tekrar deneyin.', 'danger');
      console.error('Login error:', err);
    } finally {
      btn.disabled = false;
      btn.innerHTML = 'Giriş Yap';
    }
  });
});
