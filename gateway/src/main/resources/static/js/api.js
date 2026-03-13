/* ===================================================
   SpringBank - Paylaşılan API Yardımcı Modülü
   Tüm sayfalarda kullanılan ortak fonksiyonlar
   =================================================== */

const API = {
  // API base URL (gateway üzerinden yönlendirme)
  BASE: '',

  // Token işlemleri
  getToken:        () => localStorage.getItem('sb_token'),
  getRefreshToken: () => localStorage.getItem('sb_refresh'),
  getUserData:     () => { try { return JSON.parse(localStorage.getItem('sb_user') || '{}'); } catch { return {}; } },

  setTokens(access, refresh) {
    localStorage.setItem('sb_token', access);
    if (refresh) localStorage.setItem('sb_refresh', refresh);
  },

  setUser(data) {
    localStorage.setItem('sb_user', JSON.stringify(data));
  },

  clearSession() {
    ['sb_token','sb_refresh','sb_user'].forEach(k => localStorage.removeItem(k));
  },

  // JWT parse - Keycloak token içeriğini çözer
  parseJwt() {
    try {
      const t = this.getToken();
      if (!t) return null;
      const b = t.split('.')[1].replace(/-/g,'+').replace(/_/g,'/');
      return JSON.parse(decodeURIComponent(atob(b).split('').map(c =>
        '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
      ).join('')));
    } catch { return null; }
  },

  // Kullanıcı ID'sini JWT'den çeker (Keycloak sub claim)
  getUserId() {
    const p = this.parseJwt();
    return p?.sub || null;
  },

  // Kullanıcı rollerini çeker
  getRoles() {
    const p = this.parseJwt();
    return p?.realm_access?.roles || [];
  },

  isAdmin() {
    return this.getRoles().includes('ADMIN') || this.getRoles().includes('admin');
  },

  // Auth kontrolü - token yoksa login'e yönlendir
  checkAuth() {
    if (!this.getToken()) {
      window.location.href = '/login.html';
      return false;
    }
    return true;
  },

  // Admin kontrolü
  checkAdmin() {
    if (!this.checkAuth()) return false;
    if (!this.isAdmin()) {
      window.location.href = '/dashboard.html';
      return false;
    }
    return true;
  },

  // Genel fetch wrapper
  async call(url, method = 'GET', body = null) {
    const headers = { 'Content-Type': 'application/json' };
    const token = this.getToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;

    const opts = { method, headers };
    if (body !== null) opts.body = JSON.stringify(body);

    const res = await fetch(this.BASE + url, opts);

    // Token süresi dolmuş
    if (res.status === 401) {
      const refreshed = await this.tryRefresh();
      if (!refreshed) {
        this.clearSession();
        window.location.href = '/login.html';
        return null;
      }
      // Tekrar dene
      headers['Authorization'] = 'Bearer ' + this.getToken();
      return fetch(this.BASE + url, { ...opts, headers });
    }

    return res;
  },

  // Refresh token ile yenileme
  async tryRefresh() {
    const rToken = this.getRefreshToken();
    if (!rToken) return false;
    try {
      const res = await fetch('/api/auth-service/v1/auth/refresh', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: rToken })
      });
      if (!res.ok) return false;
      const data = await res.json();
      console.log('[tryRefresh] response:', JSON.stringify(data));
      // FIX: access_token null gelirse undefined localStorage'a yazılıp
      // Bearer undefined olarak gönderiliyordu. Null kontrolü eklendi.
      if (!data.access_token) {
        console.error('[tryRefresh] access_token yok! Gelen response:', data);
        return false;
      }
      this.setTokens(data.access_token, data.refresh_token);
      return true;
    } catch (e) {
      console.error('[tryRefresh] hata:', e);
      return false;
    }
  },

  // Oturum kapatma
  async logout() {
    const rToken = this.getRefreshToken();
    if (rToken) {
      try {
        await fetch('/api/auth-service/v1/auth/logout', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + this.getToken() },
          body: JSON.stringify({ refreshToken: rToken })
        });
      } catch { /* hata yoksay */ }
    }
    this.clearSession();
    window.location.href = '/login.html';
  },

  // Toast bildirimi göster
  toast(msg, type = 'info', duration = 4000) {
    let container = document.getElementById('toast-container');
    if (!container) {
      container = document.createElement('div');
      container.id = 'toast-container';
      container.className = 'toast-container';
      document.body.appendChild(container);
    }

    const icons = {
      success: '<i class="ph-fill ph-check-circle" style="color:var(--success); font-size:20px;"></i>',
      danger: '<i class="ph-fill ph-x-circle" style="color:var(--danger); font-size:20px;"></i>',
      info: '<i class="ph-fill ph-info" style="color:var(--info); font-size:20px;"></i>',
      warning: '<i class="ph-fill ph-warning-circle" style="color:var(--warning); font-size:20px;"></i>'
    };
    const t = document.createElement('div');
    t.className = `toast toast-${type}`;
    t.innerHTML = `<span class="toast-icon" style="display:flex;align-items:center;">${icons[type] || icons.info}</span><span class="toast-msg">${msg}</span>`;
    container.appendChild(t);

    setTimeout(() => {
      t.style.animation = 'fadeOut 0.3s ease forwards';
      setTimeout(() => t.remove(), 300);
    }, duration);
  },

  // Alert mesajı göster (form altı)
  showMsg(el, text, type) {
    if (typeof el === 'string') el = document.getElementById(el);
    if (!el) return;
    el.className = `alert alert-${type}`;
    el.innerHTML = text;
    el.style.display = 'block';
    el.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  },

  hideMsg(el) {
    if (typeof el === 'string') el = document.getElementById(el);
    if (el) el.style.display = 'none';
  },

  // Para formatı
  formatMoney(n) {
    return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(n || 0);
  },

  // Tarih formatı
  formatDate(d) {
    if (!d) return '-';
    return new Date(d).toLocaleString('tr-TR', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  },

  // Loading overlay
  showLoading(msg = 'Yükleniyor...') {
    let overlay = document.getElementById('loading-overlay');
    if (!overlay) {
      overlay = document.createElement('div');
      overlay.id = 'loading-overlay';
      overlay.className = 'loading-overlay';
      overlay.innerHTML = `<div class="spinner" style="width:36px;height:36px;border-color:rgba(14,165,233,0.3);border-top-color:var(--primary);"></div><div>${msg}</div>`;
      document.body.appendChild(overlay);
    }
    overlay.classList.add('show');
  },

  hideLoading() {
    const overlay = document.getElementById('loading-overlay');
    if (overlay) overlay.classList.remove('show');
  }
};