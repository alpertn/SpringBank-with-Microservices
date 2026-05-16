/* ===== SpringBank - Register Sayfası JS ===== */

document.addEventListener('DOMContentLoaded', () => {
  // Zaten giriş yapıldıysa dashboard'a yönlendir
  if (API.getToken()) {
    window.location.href = '/dashboard.html';
    return;
  }

  const form = document.getElementById('register-form');
  const btn  = document.getElementById('register-btn');
  const msg  = document.getElementById('register-msg');

  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const name     = document.getElementById('name').value.trim();
    const surname  = document.getElementById('surname').value.trim();
    const email    = document.getElementById('email').value.trim();
    const password = document.getElementById('password').value;
    const password2= document.getElementById('password2').value;

    // Validasyon
    if (!name || !surname || !email || !password) {
      API.showMsg(msg, '❌ Tüm alanları doldurunuz.', 'danger');
      return;
    }
    if (password !== password2) {
      API.showMsg(msg, '❌ Şifreler eşleşmiyor.', 'danger');
      return;
    }
    if (password.length < 6) {
      API.showMsg(msg, '❌ Şifre en az 6 karakter olmalıdır.', 'danger');
      return;
    }

    btn.disabled = true;
    btn.innerHTML = '<span class="spinner"></span> Hesap oluşturuluyor...';
    API.hideMsg(msg);

    try {
      // POST /api/user-service/v1/auth/register
      // RegisterDto: email, Name (büyük N - @JsonProperty), surname, password
      const res = await fetch('/api/user-service/v1/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, name: name, surname, password })
      });

      if (res.ok || res.status === 201) {
        API.showMsg(msg, '✅ Hesabınız oluşturuldu! Giriş sayfasına yönlendiriliyorsunuz...', 'success');
        setTimeout(() => window.location.href = '/login.html', 2000);
      } else {
        let errMsg = `Kayıt başarısız (${res.status}).`;
        try {
          const errData = await res.json();
          if (errData.message) errMsg = errData.message;
        } catch { /* yoksay */ }
        API.showMsg(msg, `❌ ${errMsg}`, 'danger');
      }
    } catch (err) {
      API.showMsg(msg, '❌ Sunucuya bağlanılamadı.', 'danger');
      console.error('Register error:', err);
    } finally {
      btn.disabled = false;
      btn.innerHTML = 'Hesap Oluştur';
    }
  });
});
