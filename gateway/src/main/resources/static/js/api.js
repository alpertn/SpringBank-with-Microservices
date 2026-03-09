/* ===== Mercury Bank - Shared API Utility ===== */

const API = {
    // Token Getir
    getToken: () => localStorage.getItem('token'),

    // Refresh Token Getir
    getRefreshToken: () => localStorage.getItem('refreshToken'),

    // Tokenleri Kaydet
    setTokens(accessToken, refreshToken) {
        localStorage.setItem('token', accessToken);
        if (refreshToken) localStorage.setItem('refreshToken', refreshToken);
    },

    // Tokenleri Sil (Oturum Kapatma)
    clearTokens() {
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
    },

    // Auth kontrolu - token yoksa login'e gonder
    checkAuth() {
        if (!this.getToken()) {
            window.location.href = '/login.html';
            return false;
        }
        return true;
    },

    // JWT İceriginden Kullanici Bilgisini Cikar (Parse)
    parseJwt() {
        try {
            const token = this.getToken();
            if (!token) return null;

            // Base64 cözümlemesi
            const base64Url = token.split('.')[1];
            const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
            const jsonPayload = decodeURIComponent(window.atob(base64).split('').map(c =>
                '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
            ).join(''));

            return JSON.parse(jsonPayload);
        } catch (e) {
            return null; // Parse hatasi
        }
    },

    // Genel API cagirici
    async call(url, method = 'GET', body = null) {
        const headers = { 'Content-Type': 'application/json' };
        const token = this.getToken();
        if (token) headers['Authorization'] = 'Bearer ' + token;

        const options = { method, headers };
        if (body) options.body = JSON.stringify(body);

        const response = await fetch(url, options);

        // 401 ise oturum bitmis
        if (response.status === 401) {
            this.clearTokens();
            window.location.href = '/login.html';
            return null;
        }
        return response;
    },

    // Cikis Yonlendirmesi ve Temizlik
    async logout() {
        const refreshToken = this.getRefreshToken();
        if (refreshToken) {
            try {
                await this.call('/api/auth/logout', 'POST', { refreshToken }); // Backend cikis
            } catch (e) { console.warn("Logout error ignores"); }
        }
        this.clearTokens();
        window.location.href = '/login.html';
    },

    // Ortak UI Mesaj Gosterici (id veya element kabul eder)
    showMsg(el, text, type) {
        if (typeof el === 'string') el = document.getElementById(el);
        if (!el) return;
        el.className = `alert alert-${type} mt-2 p-2`;
        el.innerHTML = `<strong>Sistem Mesajı:</strong> ${text}`;
        el.style.display = 'block';
    }
};
