'use strict';

(() => {
  const OPS = '/api/admin-service/v1';
  const state = {
    users: [],
    accounts: [],
    transactions: [],
    lastSearch: [],
    report: null,
    queryCatalog: null,
    kafkaTopics: [],
    currentDbResult: null,
    currentKafkaResult: null,
    queryHistory: [],
    activeHistoryRequestId: null
  };

  const tokenValue = () => window.API?.getToken?.() || localStorage.getItem('sb_token') || '';
  const headers = () => ({ 'Content-Type': 'application/json', 'Authorization': `Bearer ${tokenValue()}` });
  const safeEsc = value => (typeof escHtml === 'function'
    ? escHtml(value)
    : String(value ?? '').replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char])));
  const money = value => (typeof formatMoney === 'function' ? formatMoney(value) : `${value ?? '-'} TRY`);
  const notify = (message, type = 'info') => (typeof toast === 'function' ? toast(message, type) : console.log(message));
  const activity = (type, text) => { try { window.addActivity?.(type, text); } catch (_) { } };

  async function extApi(method, path, body = null) {
    const options = { method, headers: headers() };
    if (body !== null) options.body = JSON.stringify(body);
    const response = await fetch(path, options);
    if (response.status === 401) {
      window.location.href = '/login.html';
      return null;
    }
    const contentType = response.headers.get('content-type') || '';
    const payload = contentType.includes('application/json') ? await response.json() : await response.text();
    if (!response.ok) {
      throw new Error(typeof payload === 'string' ? payload : JSON.stringify(payload));
    }
    return payload;
  }

  function setText(id, data) {
    const element = document.getElementById(id);
    if (element) element.textContent = typeof data === 'string' ? data : JSON.stringify(data, null, 2);
  }

  function setHtml(id, html) {
    const element = document.getElementById(id);
    if (element) element.innerHTML = html;
  }

  function toApiDateTime(date) {
    const pad = value => String(value).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
  }

  function downloadText(filename, text, type = 'text/plain') {
    const blob = new Blob([text], { type });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = filename;
    link.click();
    URL.revokeObjectURL(link.href);
  }

  function injectStyles() {
    if (document.getElementById('adminExtendedStyles')) return;
    const style = document.createElement('style');
    style.id = 'adminExtendedStyles';
    style.textContent = `
      .ops-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(320px,1fr));gap:16px;margin-bottom:18px}
      .ops-card,.query-card{background:var(--surface);border:1px solid var(--border);border-radius:8px;padding:16px;box-shadow:var(--shadow)}
      .ops-card h3,.query-card h3{font-size:15px;margin:0 0 10px;display:flex;align-items:center;gap:8px}
      .ops-card p,.query-card p{font-size:12px;color:var(--text-muted);margin:0 0 12px}
      .ops-form{display:grid;grid-template-columns:repeat(auto-fit,minmax(140px,1fr));gap:10px;align-items:end}
      .ops-form label,.query-form label{font-size:11px;font-weight:700;color:var(--text-muted);text-transform:uppercase;display:grid;gap:6px}
      .ops-pre,.query-pre{margin-top:12px;min-height:150px;max-height:460px;overflow:auto;background:#0f172a;color:#e2e8f0;border-radius:8px;padding:12px;font-size:12px;line-height:1.5;white-space:pre-wrap}
      .search-panel,.query-shell{background:var(--surface);border:1px solid var(--border);border-radius:8px;padding:16px;margin-bottom:16px}
      .search-filters{display:grid;grid-template-columns:repeat(auto-fit,minmax(170px,1fr));gap:10px}
      .search-result-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:12px;margin:14px 0}
      .result-chip{background:var(--surface2);border:1px solid var(--border);border-radius:8px;padding:12px}
      .result-chip strong{display:block;font-size:20px}
      .mini-table{width:100%;border-collapse:collapse;font-size:12px}
      .mini-table th,.mini-table td{border-bottom:1px solid var(--border);padding:8px;text-align:left;vertical-align:top}
      .mini-table th{color:var(--text-muted);font-weight:700;background:var(--surface2)}
      .rbac-table td:first-child,.rbac-table th:first-child{position:sticky;left:0;background:var(--surface);z-index:1}
      .chart-row{display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:16px}
      .chart-box{height:280px}
      .query-band{display:grid;grid-template-columns:minmax(0,1.6fr) minmax(320px,1fr);gap:16px}
      .query-toolbar{display:flex;gap:8px;flex-wrap:wrap;margin-bottom:12px}
      .query-form{display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:10px}
      .query-editor{width:100%;min-height:220px;background:linear-gradient(180deg,#0b1220,#101a2f);color:#e5eefc;border:1px solid rgba(122,149,189,.35);border-radius:8px;padding:14px;font:500 12px/1.6 'Consolas','Courier New',monospace;resize:vertical}
      .query-tabs{display:flex;gap:8px;flex-wrap:wrap;margin-bottom:14px}
      .query-tab-btn,.query-view-btn,.query-chip,.topic-item{border:1px solid var(--border);background:var(--surface2);color:var(--text);border-radius:999px;padding:8px 12px;font-size:12px;font-weight:600;cursor:pointer;transition:var(--transition)}
      .query-tab-btn.active,.query-view-btn.active,.query-chip.active,.topic-item.active{background:var(--primary);color:#fff;border-color:var(--primary)}
      .query-chip{border-radius:8px}
      .query-chip-wrap{display:flex;gap:8px;flex-wrap:wrap;margin-top:10px}
      .query-panel{display:none}
      .query-panel.active{display:block}
      .query-result-switch{display:flex;gap:8px;flex-wrap:wrap;margin:14px 0 10px}
      .query-result-panel{display:none}
      .query-result-panel.active{display:block}
      .query-meta{display:flex;gap:10px;flex-wrap:wrap;margin-top:12px}
      .query-meta .badge{font-size:11px}
      .topic-grid{display:grid;grid-template-columns:300px minmax(0,1fr);gap:16px}
      .topic-list{display:flex;flex-direction:column;gap:8px;max-height:640px;overflow:auto;padding-right:6px}
      .topic-item,.history-item{border-radius:10px;padding:12px 14px;text-align:left}
      .topic-item small{display:block;color:inherit;opacity:.75;font-weight:500}
      .history-list{display:flex;flex-direction:column;gap:8px;max-height:420px;overflow:auto}
      .history-item{border:1px solid var(--border);background:var(--surface2);cursor:pointer}
      .history-item.active{border-color:var(--primary);box-shadow:inset 0 0 0 1px var(--primary)}
      .history-head{display:flex;justify-content:space-between;gap:12px;font-size:12px;font-weight:700;margin-bottom:6px}
      .history-meta{display:flex;gap:8px;flex-wrap:wrap;font-size:11px;color:var(--text-muted)}
      .message-card{border:1px solid var(--border);background:var(--surface2);border-radius:8px;padding:12px;margin-bottom:10px}
      .message-head{display:flex;gap:10px;flex-wrap:wrap;margin-bottom:8px;font-size:11px;color:var(--text-muted);text-transform:uppercase;font-weight:700}
      .message-json{margin:0;background:#0f172a;color:#e2e8f0;border-radius:8px;padding:10px;font-size:12px;white-space:pre-wrap;overflow:auto}
      .message-raw{font-family:'Consolas','Courier New',monospace;font-size:12px}
      .spotlight{background:linear-gradient(135deg,rgba(2,108,182,.12),rgba(12,154,93,.08));border:1px solid rgba(2,108,182,.16);border-radius:10px;padding:16px;margin-bottom:16px}
      @media (max-width: 1100px){.query-band,.topic-grid{grid-template-columns:1fr}}
    `;
    document.head.appendChild(style);
  }

  function injectPages() {
    const content = document.querySelector('.main-content');
    const activityPage = document.getElementById('page-activity');
    if (!content || document.getElementById('page-queries')) return;

    const advanced = document.createElement('div');
    advanced.className = 'page-wrap';
    advanced.id = 'page-advanced-search';
    advanced.innerHTML = `
      <div class="section-header">
        <div><div class="section-title">Detayli Arama</div><div class="section-sub">Kullanici, hesap ve islem verisini tek yerde filtrele</div></div>
        <div class="section-actions">
          <button class="btn btn-outline btn-sm" id="btnSearchLoad"><i class="ph ph-arrows-clockwise"></i> Veriyi Yukle</button>
          <button class="btn btn-outline btn-sm" id="btnSearchExport"><i class="ph ph-download-simple"></i> CSV</button>
        </div>
      </div>
      <div class="search-panel">
        <div class="search-filters">
          <label>Genel arama<input class="form-control" id="advQ" placeholder="email, ad, userId, iban, tx id"></label>
          <label>Rol<select class="form-control" id="advRole"><option value="">Hepsi</option><option>USER</option><option>ADMIN</option></select></label>
          <label>Islem tipi<select class="form-control" id="advTxType"><option value="">Hepsi</option><option>DEPOSIT</option><option>WITHDRAW</option><option>TRANSFER</option><option>EFT</option></select></label>
          <label>Durum<input class="form-control" id="advStatus" placeholder="COMPLETED, FAILED"></label>
          <label>Min tutar<input class="form-control" id="advMin" type="number" step="0.01"></label>
          <label>Max tutar<input class="form-control" id="advMax" type="number" step="0.01"></label>
          <label>Baslangic<input class="form-control" id="advStart" type="date"></label>
          <label>Bitis<input class="form-control" id="advEnd" type="date"></label>
          <label>Hesap<input class="form-control" id="advAccountMode" list="accountModes" placeholder="var/yok/blokeli/fark"></label>
          <datalist id="accountModes"><option value="var"><option value="yok"><option value="blokeli"><option value="fark"></datalist>
        </div>
        <div class="query-toolbar" style="margin-top:12px">
          <button class="btn btn-primary btn-sm" id="btnRunAdvancedSearch"><i class="ph ph-magnifying-glass"></i> Ara</button>
          <button class="btn btn-outline btn-sm" id="btnSaveAdvancedFilter"><i class="ph ph-floppy-disk"></i> Filtreyi Sakla</button>
          <button class="btn btn-outline btn-sm" id="btnClearAdvancedFilter"><i class="ph ph-eraser"></i> Temizle</button>
          <select class="role-select" id="savedAdvancedFilters" style="min-width:220px"></select>
        </div>
      </div>
      <div class="search-result-grid" id="advancedSearchSummary"></div>
      <div class="table-card">
        <div class="table-toolbar"><div class="table-toolbar-title"><i class="ph ph-list-magnifying-glass"></i> Sonuclar</div></div>
        <div style="overflow:auto;max-height:62vh">
          <table class="mini-table">
            <thead><tr><th>Tip</th><th>Kimlik</th><th>Ad/IBAN</th><th>Durum</th><th>Tutar</th><th>Eslesme</th></tr></thead>
            <tbody id="advancedSearchRows"></tbody>
          </table>
        </div>
      </div>
    `;

    const queries = document.createElement('div');
    queries.className = 'page-wrap';
    queries.id = 'page-queries';
    queries.innerHTML = `
      <div class="section-header">
        <div><div class="section-title">Tum Sorgular</div><div class="section-sub">Kafka topicleri ve tum veritabanlari icin tek merkez query workbench</div></div>
        <div class="section-actions">
          <label style="display:grid;gap:4px;font-size:11px;font-weight:700;color:var(--text-muted);text-transform:uppercase;">Transport<select class="form-control" id="queryTransport"><option value="grpc">gRPC</option><option value="kafka">Kafka Topic</option></select></label>
          <button class="btn btn-primary btn-sm" id="btnRefreshQueryCatalog"><i class="ph ph-arrows-clockwise"></i> Kaynaklari Yenile</button>
        </div>
      </div>
      <div class="spotlight">
        <strong style="display:block;font-size:14px;margin-bottom:6px;">Query Workbench</strong>
        <span style="font-size:12px;color:var(--text-muted);">Postgres icin SQL, Mongo icin mongosh query, Elasticsearch icin path + JSON body, Kafka icin topic browse ve mesaj JSON gorunumu.</span>
      </div>
      <div class="query-tabs">
        <button class="query-tab-btn active" data-query-panel="database">Database</button>
        <button class="query-tab-btn" data-query-panel="kafka">Kafka Topicleri</button>
      </div>

      <div class="query-panel active" id="query-panel-database">
        <div class="query-band">
          <div class="query-shell">
            <div class="query-form">
              <label>Engine<select class="form-control" id="queryEngine"></select></label>
              <label>Database<select class="form-control" id="queryDatabase"></select></label>
              <label>Hazir sorgu<select class="form-control" id="queryTemplateSelect"></select></label>
              <label id="queryMethodWrap" style="display:none;">Method<select class="form-control" id="queryMethod"><option>GET</option><option>POST</option></select></label>
              <label id="queryPathWrap" style="display:none;">Path<input class="form-control" id="queryPath" placeholder="/money-accounts/_search"></label>
            </div>
            <div class="query-chip-wrap" id="queryTemplateChips"></div>
            <textarea class="query-editor" id="queryEditor" spellcheck="false"></textarea>
            <div class="query-toolbar" style="margin-top:12px">
              <button class="btn btn-primary btn-sm" id="btnRunDbQuery"><i class="ph ph-play"></i> Sorguyu Calistir</button>
              <button class="btn btn-outline btn-sm" id="btnLoadDbTemplate"><i class="ph ph-lightning"></i> Hazir Sorguyu Yukle</button>
              <button class="btn btn-outline btn-sm" id="btnCopyDbQuery"><i class="ph ph-copy"></i> Kopyala</button>
              <button class="btn btn-outline btn-sm" id="btnClearDbQuery"><i class="ph ph-eraser"></i> Temizle</button>
            </div>
            <div class="query-meta" id="queryResultMeta"></div>
          </div>
          <div style="display:grid;gap:16px">
            <div class="query-card">
              <h3><i class="ph ph-sparkle"></i> Hazir Sorgular</h3>
              <p>Secili database icin hizli komutlar. Butona bastiginda editore yuklenir ve istersen duzenleyip aninda calistirirsin.</p>
              <div id="queryTemplatePreview" class="query-pre" style="min-height:180px;">Template secilmedi.</div>
            </div>
            <div class="query-card">
              <h3><i class="ph ph-clock-counter-clockwise"></i> Eski Sorgular</h3>
              <p>Admin emaili, transport, sorgu tipi, ne zaman gonderildigi, veri alip almadigi ve gelen cikti burada tutulur.</p>
              <div class="query-toolbar">
                <input class="form-control" id="historyKeyword" placeholder="email, request type, target">
                <button class="btn btn-outline btn-sm" id="btnRefreshHistory"><i class="ph ph-arrows-clockwise"></i> Gecmisi Yenile</button>
              </div>
              <div class="history-list" id="queryHistoryList"></div>
              <pre class="query-pre" id="queryHistoryDetail" style="min-height:180px;">Sorgu gecmisi detayi burada gorunecek.</pre>
            </div>
          </div>
        </div>
        <div class="query-card" style="margin-top:16px">
          <h3><i class="ph ph-database"></i> Sonuc</h3>
          <div class="query-result-switch">
            <button class="query-view-btn active" data-query-view="table">Tablo</button>
            <button class="query-view-btn" data-query-view="json">JSON</button>
            <button class="query-view-btn" data-query-view="raw">Ham Cikti</button>
          </div>
          <div class="query-result-panel active" id="query-view-table">
            <div style="overflow:auto;max-height:520px"><table class="mini-table"><thead id="dbQueryTableHead"></thead><tbody id="dbQueryTableBody"></tbody></table></div>
          </div>
          <div class="query-result-panel" id="query-view-json"><pre class="query-pre" id="dbQueryJson">JSON sonucu burada gorunecek.</pre></div>
          <div class="query-result-panel" id="query-view-raw"><pre class="query-pre" id="dbQueryRaw">Ham sonuc burada gorunecek.</pre></div>
        </div>
      </div>

      <div class="query-panel" id="query-panel-kafka">
        <div class="topic-grid">
          <div class="query-card">
            <h3><i class="ph ph-queue"></i> Topic Listesi</h3>
            <p>Topicleri yenile, sec ve dogrudan icerigini incele.</p>
            <div class="query-toolbar">
              <button class="btn btn-outline btn-sm" id="btnRefreshKafkaTopics"><i class="ph ph-arrows-clockwise"></i> Topicleri Yenile</button>
            </div>
            <div class="topic-list" id="kafkaTopicList"></div>
          </div>
          <div class="query-shell">
            <div class="query-form">
              <label>Secili topic<input class="form-control" id="selectedTopicName" readonly></label>
              <label>Mesaj sayisi<input class="form-control" id="kafkaMessageCount" type="number" min="1" max="100" value="20"></label>
              <label>Baslangictan oku<select class="form-control" id="kafkaFromBeginning"><option value="true">Evet</option><option value="false">Hayir</option></select></label>
            </div>
            <div class="query-toolbar" style="margin-top:12px">
              <button class="btn btn-primary btn-sm" id="btnOpenKafkaTopic"><i class="ph ph-play"></i> Topic Ac</button>
            </div>
            <div class="query-meta" id="kafkaMeta"></div>
            <pre class="query-pre" id="kafkaTopicDescribe">Topic acildiginda describe bilgisi burada gorunur.</pre>
            <div id="kafkaMessageCards" style="margin-top:12px"></div>
            <pre class="query-pre" id="kafkaTopicRaw">Ham Kafka ciktilari burada gorunecek.</pre>
          </div>
        </div>
      </div>
    `;

    const flow = document.createElement('div');
    flow.className = 'page-wrap';
    flow.id = 'page-flow';
    flow.innerHTML = `
      <div class="section-header">
        <div><div class="section-title">Saga, Kafka ve CQRS</div><div class="section-sub">Event akisi, projection senkronu ve consumer lag</div></div>
        <div class="section-actions"><button class="btn btn-primary btn-sm" id="btnRefreshFlow"><i class="ph ph-arrows-clockwise"></i> Yenile</button></div>
      </div>
      <div class="ops-grid">
        <div class="ops-card"><h3><i class="ph ph-git-branch"></i> Saga Timeline</h3><p>Transaction saga endpointleri, takilan islemler ve retry/recover sinyalleri.</p><button class="btn btn-outline btn-sm" id="btnLoadSaga">Saga Oku</button><pre class="ops-pre" id="sagaOut">Hazir.</pre></div>
        <div class="ops-card"><h3><i class="ph ph-queue"></i> Kafka Topic/Lag</h3><p>Topic listesi, consumer group lag, offset ve son event operasyonlari.</p><button class="btn btn-outline btn-sm" id="btnLoadKafka">Kafka Oku</button><pre class="ops-pre" id="kafkaOut">Hazir.</pre></div>
        <div class="ops-card"><h3><i class="ph ph-database"></i> CQRS Projection</h3><p>money-service-query, MongoDB ve Elasticsearch read-model durumu.</p><button class="btn btn-outline btn-sm" id="btnLoadCqrs">CQRS Oku</button><pre class="ops-pre" id="cqrsOut">Hazir.</pre></div>
      </div>
    `;

    const ops = document.createElement('div');
    ops.className = 'page-wrap';
    ops.id = 'page-ops';
    ops.innerHTML = `
      <div class="section-header">
        <div><div class="section-title">Admin Operasyon Merkezi</div><div class="section-sub">Reconciliation, raporlama, backup, Kubernetes, audit ve proje komutlari</div></div>
        <div class="section-actions"><button class="btn btn-primary btn-sm" id="btnRunEverything"><i class="ph ph-play"></i> Her Seyi Test Et</button><button class="btn btn-outline btn-sm" id="btnShowAllLogs"><i class="ph ph-terminal-window"></i> Tum Loglar</button></div>
      </div>
      <div class="ops-grid">
        <div class="ops-card"><h3><i class="ph ph-scales"></i> Veri Tutarliligi</h3><p>Postgres bakiye, transaction ledger, Mongo projection ve Elasticsearch sayimlarini karsilastirir.</p><button class="btn btn-outline btn-sm" id="btnReconcile">Kontrol Et</button><pre class="ops-pre" id="reconcileOut">Hazir.</pre></div>
        <div class="ops-card"><h3><i class="ph ph-sliders"></i> Sistem Konfigurasyonu</h3><p>Kafka topic adlari, servis URL'leri, fraud limitleri ve deployment env degerleri.</p><button class="btn btn-outline btn-sm" id="btnConfig">Konfig Oku</button><pre class="ops-pre" id="configOut">Hazir.</pre></div>
        <div class="ops-card"><h3><i class="ph ph-chart-line-up"></i> Raporlama</h3><p>Gunluk, haftalik ve aylik finansal ozet; grafik, CSV, PDF ve Excel export.</p><div class="ops-form"><label>Periyot<select class="form-control" id="reportPeriod"><option value="daily">Gunluk</option><option value="weekly">Haftalik</option><option value="monthly">Aylik</option></select></label><button class="btn btn-outline btn-sm" id="btnBuildReport">Raporla</button><button class="btn btn-outline btn-sm" id="btnExportReportCsv">CSV</button><button class="btn btn-outline btn-sm" id="btnExportReportXls">Excel</button><button class="btn btn-outline btn-sm" id="btnExportReportPdf">PDF</button></div><div class="chart-row" id="reportCharts"></div><pre class="ops-pre" id="reportOut">Hazir.</pre></div>
        <div class="ops-card"><h3><i class="ph ph-lock-key"></i> Hesap Dondurma / Bloke</h3><p>UserId, Keycloak UUID veya IBAN ile manuel bloke koyup kaldirir.</p><div class="ops-form"><label>Kullanici / IBAN<input class="form-control" id="blockUserId"></label><label>Tutar<input class="form-control" id="blockAmount" type="number" step="0.01"></label><label>Freeze<select class="form-control" id="blockFreeze"><option value="false">Sadece tutar</option><option value="true">Tum bakiye</option></select></label><button class="btn btn-danger btn-sm" id="btnBlockAccount">Bloke Koy</button><button class="btn btn-outline btn-sm" id="btnUnblockAccount">Bloke Kaldir</button></div><pre class="ops-pre" id="blockOut">Hazir.</pre></div>
        <div class="ops-card"><h3><i class="ph ph-archive-box"></i> Backup / Export / Restore</h3><p>Postgres snapshot/export dosyalarini uretir ve son backup icerigini okur.</p><div class="ops-form"><label>Database<select class="form-control" id="backupDb"><option>banking_transactions</option><option>banking_money</option><option>banking_money_command</option><option>banking_users</option><option>banking_fraud</option><option>banking_keycloak</option></select></label><button class="btn btn-outline btn-sm" id="btnBackupExport">Export</button><button class="btn btn-outline btn-sm" id="btnBackupList">Listele</button></div><select class="role-select" id="backupList" style="margin-top:10px;min-width:260px"></select><button class="btn btn-outline btn-sm" id="btnBackupRead" style="margin-top:10px">Oku</button><pre class="ops-pre" id="backupOut">Hazir.</pre></div>
        <div class="ops-card"><h3><i class="ph ph-cube"></i> Kubernetes Operasyon</h3><p>Deployment durumu, rollout history, pod restart, HPA/CPU/memory ve scale.</p><div class="ops-form"><label>Deployment<input class="form-control" id="k8sDeployment" value="gateway" list="deployments"></label><datalist id="deployments"><option value="gateway"><option value="admin-service"><option value="admin-service-command"><option value="admin-service-query"><option value="user-service"><option value="money-service"><option value="money-service-command"><option value="money-service-query"><option value="transaction-service"><option value="fraud-service"></datalist><label>Replica<input class="form-control" id="k8sReplicas" type="number" value="1" min="0" max="5"></label><button class="btn btn-outline btn-sm" id="btnK8sOverview">Durum</button><button class="btn btn-outline btn-sm" id="btnK8sHistory">History</button><button class="btn btn-warning btn-sm" id="btnK8sRestart">Restart</button><button class="btn btn-outline btn-sm" id="btnK8sScale">Scale</button></div><pre class="ops-pre" id="k8sOut">Hazir.</pre></div>
        <div class="ops-card"><h3><i class="ph ph-user-plus"></i> Admin'den Kullanici Olusturma</h3><p>Tek kullanici veya CSV toplu import. CSV: email,name,surname,password</p><div class="ops-form"><label>Email<input class="form-control" id="newUserEmail"></label><label>Ad<input class="form-control" id="newUserName"></label><label>Soyad<input class="form-control" id="newUserSurname"></label><label>Sifre<input class="form-control" id="newUserPassword" type="password"></label><button class="btn btn-primary btn-sm" id="btnCreateUser">Olustur</button></div><textarea class="form-control" id="bulkUsers" rows="4" style="margin-top:10px" placeholder="email,name,surname,password"></textarea><button class="btn btn-outline btn-sm" id="btnBulkImport" style="margin-top:10px">Bulk Import</button><pre class="ops-pre" id="userCreateOut">Hazir.</pre></div>
        <div class="ops-card"><h3><i class="ph ph-shield-check"></i> Yetki Matrisi</h3><p>Permission bazli RBAC modelini admin bakisindan gorunur yapar.</p><div style="overflow:auto"><table class="mini-table rbac-table" id="rbacTable"></table></div></div>
        <div class="ops-card"><h3><i class="ph ph-clipboard-text"></i> Kalici Audit Log</h3><p>Backend'e yazilan admin audit trail; operasyonlar filtrelenebilir log olarak akar.</p><button class="btn btn-outline btn-sm" id="btnAuditLoad">Audit Oku</button><pre class="ops-pre" id="auditOut">Hazir.</pre></div>
        <div class="ops-card"><h3><i class="ph ph-play-circle"></i> Proje Build/Run/Test</h3><p>Kubernetes restart/status ve admin smoke test calisir. Host .bat sadece host ortaminda calisabildiginde tetiklenir.</p><div class="query-toolbar"><button class="btn btn-outline btn-sm" data-run-action="restart-project">Projeyi Yeniden Baslat</button><button class="btn btn-outline btn-sm" data-run-action="k8s-status">K8s Status</button><button class="btn btn-primary btn-sm" data-run-action="test-everything">Admin Test Run</button><button class="btn btn-outline btn-sm" data-run-action="host-test-bat">test-everything.bat</button><button class="btn btn-outline btn-sm" data-run-action="host-build">Build .bat</button><button class="btn btn-outline btn-sm" data-run-action="host-full-run">Her Seyi Yap .bat</button></div><pre class="ops-pre" id="runOut">Hazir.</pre></div>
      </div>
    `;

    content.insertBefore(advanced, activityPage);
    content.insertBefore(queries, activityPage);
    content.insertBefore(flow, activityPage);
    content.insertBefore(ops, activityPage);
  }

  function values() {
    return {
      q: document.getElementById('advQ')?.value.trim().toLowerCase() || '',
      role: document.getElementById('advRole')?.value || '',
      type: document.getElementById('advTxType')?.value || '',
      status: document.getElementById('advStatus')?.value.trim().toUpperCase() || '',
      min: Number(document.getElementById('advMin')?.value || Number.NEGATIVE_INFINITY),
      max: Number(document.getElementById('advMax')?.value || Number.POSITIVE_INFINITY),
      start: document.getElementById('advStart')?.value || '',
      end: document.getElementById('advEnd')?.value || '',
      accountMode: document.getElementById('advAccountMode')?.value.trim().toLowerCase() || ''
    };
  }

  function userRole(user) {
    return user.role || user.roles?.[0] || user.realmRole || '';
  }

  function accountFor(user) {
    return state.accounts.find(account =>
      String(account.userId) === String(user.id)
      || String(account.keycloakUserUUID) === String(user.id)
      || String(account.userId) === String(user.userId)
      || String(account.keycloakUserUUID) === String(user.keycloakUUID));
  }

  function searchable(item) {
    return JSON.stringify(item ?? {}).toLowerCase();
  }

  async function loadSearchData() {
    const end = new Date();
    const start = new Date();
    start.setDate(start.getDate() - 120);
    const [usersResult, accountsResult, transactionsResult] = await Promise.allSettled([
      extApi('GET', '/api/user-service/v1/admin/allusers'),
      extApi('GET', '/api/money-service/v1/admin/accounts?page=0&size=1000'),
      extApi('GET', `/api/transaction-service/v1/transactions/daterange?startDate=${encodeURIComponent(toApiDateTime(start))}&endDate=${encodeURIComponent(toApiDateTime(end))}`)
    ]);
    state.users = usersResult.status === 'fulfilled' && Array.isArray(usersResult.value) ? usersResult.value : [];
    state.accounts = accountsResult.status === 'fulfilled' && Array.isArray(accountsResult.value) ? accountsResult.value : [];
    state.transactions = transactionsResult.status === 'fulfilled' && Array.isArray(transactionsResult.value) ? transactionsResult.value : [];
    const failures = [usersResult, accountsResult, transactionsResult]
      .filter(item => item.status === 'rejected')
      .map(item => item.reason?.message || 'endpoint hatasi');
    notify(`Arama verisi yuklendi: ${state.users.length} kullanici, ${state.accounts.length} hesap, ${state.transactions.length} islem${failures.length ? ` | eksik: ${failures.join(' / ')}` : ''}`, failures.length ? 'warning' : 'success');
    runAdvancedSearch();
  }

  function runAdvancedSearch() {
    const filters = values();
    const rows = [];

    for (const user of state.users) {
      const account = accountFor(user);
      const role = userRole(user);
      const hasBlocked = Number(account?.blockedMoney || 0) > 0;
      const mismatch = !account;
      if (filters.role && role !== filters.role) continue;
      if (filters.accountMode === 'var' && !account) continue;
      if (filters.accountMode === 'yok' && account) continue;
      if (filters.accountMode === 'blokeli' && !hasBlocked) continue;
      if (filters.accountMode === 'fark' && !mismatch) continue;
      const haystack = `${searchable(user)} ${searchable(account)}`;
      if (filters.q && !haystack.includes(filters.q)) continue;
      rows.push({
        type: 'Kullanici',
        id: user.id || user.userId || user.keycloakUUID || user.mail || user.email,
        name: `${user.name || ''} ${user.surname || ''}`.trim() || user.mail || user.email || account?.userIban,
        status: role || '-',
        amount: account ? `${money(account.money)} / bloke ${money(account.blockedMoney)}` : '-',
        match: account ? 'Hesap var' : 'Hesap yok'
      });
    }

    for (const transaction of state.transactions) {
      const amount = Number(transaction.money ?? transaction.amount ?? 0);
      const created = transaction.createdAt || transaction.date || transaction.createdDate || '';
      if (filters.type && String(transaction.transactionType || transaction.type || '').toUpperCase() !== filters.type) continue;
      if (filters.status && !String(transaction.status || '').toUpperCase().includes(filters.status)) continue;
      if (amount < filters.min || amount > filters.max) continue;
      if (filters.start && created && created.slice(0, 10) < filters.start) continue;
      if (filters.end && created && created.slice(0, 10) > filters.end) continue;
      if (filters.q && !searchable(transaction).includes(filters.q)) continue;
      rows.push({
        type: 'Islem',
        id: transaction.id || transaction.transactionId || transaction.eventUUID,
        name: transaction.senderIban || transaction.receiverIban || transaction.userId || '-',
        status: transaction.status || '-',
        amount: money(amount),
        match: `${transaction.transactionType || transaction.type || '-'} ${created}`
      });
    }

    state.lastSearch = rows;
    renderSearch(rows);
    activity('info', `Detayli arama: ${rows.length} sonuc`);
  }

  function renderSearch(rows) {
    const summary = document.getElementById('advancedSearchSummary');
    const body = document.getElementById('advancedSearchRows');
    if (!summary || !body) return;
    const userCount = rows.filter(row => row.type === 'Kullanici').length;
    const transactionCount = rows.filter(row => row.type === 'Islem').length;
    const blocked = state.accounts.filter(account => Number(account.blockedMoney || 0) > 0).length;
    const mismatch = state.users.filter(user => !accountFor(user)).length;
    summary.innerHTML = [
      ['Toplam sonuc', rows.length],
      ['Kullanici', userCount],
      ['Islem', transactionCount],
      ['Blokeli hesap', blocked],
      ['Hesap farki', mismatch]
    ].map(([label, value]) => `<div class="result-chip"><span>${safeEsc(label)}</span><strong>${safeEsc(value)}</strong></div>`).join('');
    body.innerHTML = rows.slice(0, 500).map(row => `
      <tr>
        <td>${safeEsc(row.type)}</td>
        <td class="mono">${safeEsc(row.id)}</td>
        <td>${safeEsc(row.name)}</td>
        <td>${typeof badge === 'function' ? badge(row.status) : safeEsc(row.status)}</td>
        <td>${safeEsc(row.amount)}</td>
        <td>${safeEsc(row.match)}</td>
      </tr>
    `).join('') || '<tr><td colspan="6" class="empty-msg">Sonuc yok.</td></tr>';
  }

  function refreshSavedFilters() {
    const select = document.getElementById('savedAdvancedFilters');
    if (!select) return;
    const items = JSON.parse(localStorage.getItem('sb_admin_saved_filters') || '[]');
    select.innerHTML = '<option value="">Kayitli filtre sec</option>' + items.map((item, index) => `<option value="${index}">${safeEsc(item.name)}</option>`).join('');
    select.onchange = () => {
      const current = items[Number(select.value)];
      if (!current) return;
      const idMap = {
        q: 'advQ',
        role: 'advRole',
        type: 'advTxType',
        status: 'advStatus',
        min: 'advMin',
        max: 'advMax',
        start: 'advStart',
        end: 'advEnd',
        accountMode: 'advAccountMode'
      };
      Object.entries(current.values || {}).forEach(([key, value]) => {
        const input = document.getElementById(idMap[key]);
        if (input) input.value = value;
      });
      runAdvancedSearch();
    };
  }

  window.initAdvancedSearch = function initAdvancedSearch() {
    if (!document.getElementById('btnSearchLoad')) return;
    document.getElementById('btnSearchLoad').onclick = () => loadSearchData().catch(error => notify(error.message, 'error'));
    document.getElementById('btnRunAdvancedSearch').onclick = runAdvancedSearch;
    document.getElementById('btnSearchExport').onclick = () => downloadText(
      'springbank-detayli-arama.csv',
      ['tip,id,ad,status,tutar,eslesme', ...state.lastSearch.map(row => [row.type, row.id, row.name, row.status, row.amount, row.match].map(value => `"${String(value ?? '').replace(/"/g, '""')}"`).join(','))].join('\n'),
      'text/csv'
    );
    document.getElementById('btnClearAdvancedFilter').onclick = () => {
      ['advQ', 'advRole', 'advTxType', 'advStatus', 'advMin', 'advMax', 'advStart', 'advEnd', 'advAccountMode'].forEach(id => {
        const input = document.getElementById(id);
        if (input) input.value = '';
      });
      runAdvancedSearch();
    };
    document.getElementById('btnSaveAdvancedFilter').onclick = () => {
      const filters = JSON.parse(localStorage.getItem('sb_admin_saved_filters') || '[]');
      filters.push({ name: `Filtre ${new Date().toLocaleString('tr-TR')}`, values: values() });
      localStorage.setItem('sb_admin_saved_filters', JSON.stringify(filters.slice(-20)));
      refreshSavedFilters();
      notify('Filtre saklandi', 'success');
    };
    document.querySelectorAll('#page-advanced-search input,#page-advanced-search select').forEach(element => {
      if (element.id !== 'savedAdvancedFilters') {
        element.oninput = () => { if (state.users.length || state.transactions.length) runAdvancedSearch(); };
      }
    });
    refreshSavedFilters();
    if (!state.users.length && !state.transactions.length) {
      loadSearchData().catch(error => notify(error.message, 'error'));
    } else {
      renderSearch(state.lastSearch);
    }
  };

  async function loadFlow(kind, outId) {
    setText(outId, 'Yukleniyor...');
    try {
      const data = await extApi('GET', `${OPS}/${kind}`);
      setText(outId, data);
      activity('info', `${kind} monitor acildi`);
    } catch (error) {
      setText(outId, error.message);
      notify(error.message, 'error');
    }
  }

  window.initFlowMonitor = function initFlowMonitor() {
    if (!document.getElementById('btnLoadSaga')) return;
    document.getElementById('btnLoadSaga').onclick = () => loadFlow('saga', 'sagaOut');
    document.getElementById('btnLoadKafka').onclick = () => loadFlow('kafka', 'kafkaOut');
    document.getElementById('btnLoadCqrs').onclick = () => loadFlow('cqrs', 'cqrsOut');
    document.getElementById('btnRefreshFlow').onclick = () => {
      loadFlow('saga', 'sagaOut');
      loadFlow('kafka', 'kafkaOut');
      loadFlow('cqrs', 'cqrsOut');
    };
  };

  async function buildReport() {
    setText('reportOut', 'Rapor hazirlaniyor...');
    const [summary, distribution, transactions] = await Promise.all([
      extApi('GET', '/api/money-service/v1/admin/stats/summary'),
      extApi('GET', '/api/money-service/v1/admin/stats/distribution'),
      extApi('GET', `/api/transaction-service/v1/transactions/daterange?startDate=${encodeURIComponent(toApiDateTime(new Date(Date.now() - 31 * 86400000)))}&endDate=${encodeURIComponent(toApiDateTime(new Date()))}`)
    ]);
    const byType = {};
    for (const transaction of (Array.isArray(transactions) ? transactions : [])) {
      const key = transaction.transactionType || transaction.type || 'UNKNOWN';
      byType[key] = (byType[key] || 0) + Number(transaction.money || transaction.amount || 0);
    }
    state.report = {
      period: document.getElementById('reportPeriod').value,
      generatedAt: new Date().toISOString(),
      summary,
      distribution,
      byType,
      transactionCount: Array.isArray(transactions) ? transactions.length : 0
    };
    setText('reportOut', state.report);
    renderReportCharts(state.report);
  }

  function renderReportCharts(report) {
    const charts = document.getElementById('reportCharts');
    if (!charts || !window.Chart) return;
    charts.innerHTML = '<div class="ops-card chart-box"><canvas id="balanceChart"></canvas></div><div class="ops-card chart-box"><canvas id="txTypeChart"></canvas></div>';
    new Chart(document.getElementById('balanceChart'), {
      type: 'doughnut',
      data: { labels: Object.keys(report.distribution || {}), datasets: [{ data: Object.values(report.distribution || {}) }] },
      options: { maintainAspectRatio: false }
    });
    new Chart(document.getElementById('txTypeChart'), {
      type: 'bar',
      data: { labels: Object.keys(report.byType || {}), datasets: [{ label: 'TRY', data: Object.values(report.byType || {}) }] },
      options: { maintainAspectRatio: false }
    });
  }

  function renderRbac() {
    const permissions = ['view_dashboard', 'view_users', 'create_user', 'bulk_import', 'reset_password', 'change_role', 'delete_user', 'view_transactions', 'reverse_transaction', 'freeze_account', 'unblock_account', 'view_reconciliation', 'run_k8s_restart', 'scale_deployment', 'read_all_logs', 'run_tests', 'export_backup', 'view_audit'];
    const roles = {
      USER: ['view_dashboard'],
      SUPPORT: ['view_dashboard', 'view_users', 'view_transactions', 'read_all_logs'],
      OPS: ['view_dashboard', 'view_reconciliation', 'run_k8s_restart', 'scale_deployment', 'read_all_logs', 'run_tests', 'view_audit'],
      ADMIN: permissions
    };
    const table = document.getElementById('rbacTable');
    if (!table) return;
    table.innerHTML = `
      <thead><tr><th>Permission</th>${Object.keys(roles).map(role => `<th>${role}</th>`).join('')}</tr></thead>
      <tbody>${permissions.map(permission => `
        <tr>
          <td class="mono">${permission}</td>
          ${Object.values(roles).map(current => `<td>${current.includes(permission) ? '<i class="ph-fill ph-check-circle" style="color:var(--success)"></i>' : '<i class="ph ph-minus" style="color:var(--text-muted)"></i>'}</td>`).join('')}
        </tr>
      `).join('')}</tbody>
    `;
  }

  async function runOp(action) {
    setText('runOut', `${action} calisiyor...`);
    try {
      const data = await extApi('POST', `${OPS}/run`, { action });
      setText('runOut', data);
      if (action === 'test-everything') notify('Admin test loglari olustu, Test Loglari ekranindan kontrol edebilirsin.', 'success');
    } catch (error) {
      setText('runOut', error.message);
      notify(error.message, 'error');
    }
  }

  window.initOpsCenter = function initOpsCenter() {
    if (!document.getElementById('btnRunEverything')) return;
    renderRbac();
    document.getElementById('btnRunEverything').onclick = () => runOp('test-everything');
    document.getElementById('btnShowAllLogs').onclick = () => {
      if (typeof navigateTo === 'function') navigateTo('logs');
      setTimeout(() => document.querySelector('[data-svc="all"]')?.click(), 300);
    };
    document.getElementById('btnReconcile').onclick = async () => setText('reconcileOut', await extApi('GET', `${OPS}/reconcile`));
    document.getElementById('btnConfig').onclick = async () => setText('configOut', await extApi('GET', `${OPS}/config`));
    document.getElementById('btnBuildReport').onclick = () => buildReport().catch(error => setText('reportOut', error.message));
    document.getElementById('btnExportReportCsv').onclick = () => state.report && downloadText('springbank-report.csv', Object.entries(state.report.summary || {}).map(([key, value]) => `${key},${value}`).join('\n'), 'text/csv');
    document.getElementById('btnExportReportXls').onclick = () => state.report && downloadText('springbank-report.xls', `<table>${Object.entries(state.report.summary || {}).map(([key, value]) => `<tr><td>${safeEsc(key)}</td><td>${safeEsc(value)}</td></tr>`).join('')}</table>`, 'application/vnd.ms-excel');
    document.getElementById('btnExportReportPdf').onclick = () => window.print();
    document.getElementById('btnBlockAccount').onclick = async () => setText('blockOut', await extApi('POST', '/api/money-service/v1/admin/account/block', { userId: document.getElementById('blockUserId').value, amount: document.getElementById('blockAmount').value, freeze: document.getElementById('blockFreeze').value === 'true' }));
    document.getElementById('btnUnblockAccount').onclick = async () => setText('blockOut', await extApi('POST', '/api/money-service/v1/admin/account/unblock', { userId: document.getElementById('blockUserId').value }));
    document.getElementById('btnBackupExport').onclick = async () => setText('backupOut', await extApi('POST', `${OPS}/backup/export`, { database: document.getElementById('backupDb').value }));
    document.getElementById('btnBackupList').onclick = async () => {
      const list = await extApi('GET', `${OPS}/backup/list`);
      document.getElementById('backupList').innerHTML = (list || []).map(item => `<option value="${safeEsc(item.name)}">${safeEsc(item.name)}</option>`).join('');
      setText('backupOut', list);
    };
    document.getElementById('btnBackupRead').onclick = async () => {
      const selected = document.getElementById('backupList').value;
      setText('backupOut', await extApi('GET', `${OPS}/backup/${encodeURIComponent(selected)}?lines=600`));
    };
    document.getElementById('btnK8sOverview').onclick = async () => setText('k8sOut', await extApi('GET', `${OPS}/overview`));
    document.getElementById('btnK8sHistory').onclick = async () => setText('k8sOut', await extApi('GET', `${OPS}/kubernetes/history?deployment=${encodeURIComponent(document.getElementById('k8sDeployment').value)}`));
    document.getElementById('btnK8sRestart').onclick = async () => setText('k8sOut', await extApi('POST', `${OPS}/kubernetes/restart`, { deployment: document.getElementById('k8sDeployment').value }));
    document.getElementById('btnK8sScale').onclick = async () => setText('k8sOut', await extApi('POST', `${OPS}/kubernetes/scale`, { deployment: document.getElementById('k8sDeployment').value, replicas: Number(document.getElementById('k8sReplicas').value) }));
    document.getElementById('btnCreateUser').onclick = async () => setText('userCreateOut', await extApi('POST', '/api/user-service/v1/auth/register', { email: document.getElementById('newUserEmail').value, name: document.getElementById('newUserName').value, surname: document.getElementById('newUserSurname').value, password: document.getElementById('newUserPassword').value }));
    document.getElementById('btnBulkImport').onclick = async () => {
      const results = [];
      for (const line of document.getElementById('bulkUsers').value.split(/\r?\n/).filter(Boolean)) {
        const [email, name, surname, password] = line.split(',').map(value => value.trim());
        try {
          results.push({ email, result: await extApi('POST', '/api/user-service/v1/auth/register', { email, name, surname, password }) });
        } catch (error) {
          results.push({ email, error: error.message });
        }
      }
      setText('userCreateOut', results);
    };
    document.getElementById('btnAuditLoad').onclick = async () => setText('auditOut', (await extApi('GET', `${OPS}/audit?lines=500`)).join('\n'));
    document.querySelectorAll('[data-run-action]').forEach(button => {
      button.onclick = () => runOp(button.dataset.runAction);
    });
  };

  function setActiveQueryPanel(panel) {
    document.querySelectorAll('.query-tab-btn').forEach(button => button.classList.toggle('active', button.dataset.queryPanel === panel));
    document.querySelectorAll('.query-panel').forEach(item => item.classList.toggle('active', item.id === `query-panel-${panel}`));
  }

  function setActiveQueryResultView(view) {
    document.querySelectorAll('.query-view-btn').forEach(button => button.classList.toggle('active', button.dataset.queryView === view));
    document.querySelectorAll('.query-result-panel').forEach(panel => panel.classList.toggle('active', panel.id === `query-view-${view}`));
  }

  function syncQueryDatabaseOptions() {
    const engine = document.getElementById('queryEngine')?.value || 'postgres';
    const databaseSelect = document.getElementById('queryDatabase');
    const templateSelect = document.getElementById('queryTemplateSelect');
    const methodWrap = document.getElementById('queryMethodWrap');
    const pathWrap = document.getElementById('queryPathWrap');
    if (!databaseSelect || !templateSelect || !state.queryCatalog) return;

    const databases = (state.queryCatalog.databases || []).filter(item => item.engine === engine);
    databaseSelect.innerHTML = databases.map(item => `<option value="${safeEsc(item.database)}">${safeEsc(item.label)}</option>`).join('');
    methodWrap.style.display = engine === 'elasticsearch' ? '' : 'none';
    pathWrap.style.display = engine === 'elasticsearch' ? '' : 'none';
    refreshQueryTemplates();
  }

  function refreshQueryTemplates() {
    const engine = document.getElementById('queryEngine')?.value || 'postgres';
    const database = document.getElementById('queryDatabase')?.value || '';
    const templateSelect = document.getElementById('queryTemplateSelect');
    const chipWrap = document.getElementById('queryTemplateChips');
    const preview = document.getElementById('queryTemplatePreview');
    if (!templateSelect || !chipWrap || !preview || !state.queryCatalog) return;

    const templates = (state.queryCatalog.templates || []).filter(item => item.engine === engine && item.database === database);
    templateSelect.innerHTML = templates.length
      ? templates.map((template, index) => `<option value="${index}">${safeEsc(template.label)}</option>`).join('')
      : '<option value="">Hazir sorgu yok</option>';
    chipWrap.innerHTML = templates.map((template, index) => `<button class="query-chip${index === 0 ? ' active' : ''}" data-template-index="${index}">${safeEsc(template.label)}</button>`).join('');
    if (templates.length) {
      preview.textContent = templates[0].query || (engine === 'elasticsearch' ? 'Bu template secili target icin default path kullanir.' : '');
    } else {
      preview.textContent = 'Hazir sorgu bulunmuyor.';
    }

    chipWrap.querySelectorAll('[data-template-index]').forEach(button => {
      button.onclick = () => {
        chipWrap.querySelectorAll('.query-chip').forEach(item => item.classList.remove('active'));
        button.classList.add('active');
        templateSelect.value = button.dataset.templateIndex;
        preview.textContent = templates[Number(button.dataset.templateIndex)]?.query || '';
      };
    });

    templateSelect.onchange = () => {
      const current = templates[Number(templateSelect.value)];
      preview.textContent = current?.query || 'Hazir sorgu secilmedi.';
      chipWrap.querySelectorAll('.query-chip').forEach(item => item.classList.toggle('active', item.dataset.templateIndex === templateSelect.value));
    };
  }

  function loadSelectedTemplate() {
    const engine = document.getElementById('queryEngine')?.value || 'postgres';
    const database = document.getElementById('queryDatabase')?.value || '';
    const select = document.getElementById('queryTemplateSelect');
    const editor = document.getElementById('queryEditor');
    if (!select || !editor || !state.queryCatalog) return;

    const templates = (state.queryCatalog.templates || []).filter(item => item.engine === engine && item.database === database);
    const current = templates[Number(select.value)];
    if (!current) return;
    editor.value = current.query || '';

    if (engine === 'elasticsearch') {
      const pathInput = document.getElementById('queryPath');
      const methodInput = document.getElementById('queryMethod');
      if (pathInput && !pathInput.value) {
        pathInput.value = database === '_cluster' ? '/_cluster/health' : database === '_cat' ? '/_cat/indices?v' : `/${database}/_search`;
      }
      if (methodInput) methodInput.value = current.query ? 'POST' : 'GET';
    }
  }

  async function loadQueryCatalog() {
    const data = await extApi('GET', `${OPS}/query/catalog`);
    state.queryCatalog = data;
    state.kafkaTopics = data.kafkaTopics || [];
    const engineSelect = document.getElementById('queryEngine');
    if (engineSelect) {
      const engines = [...new Set((data.databases || []).map(item => item.engine))];
      engineSelect.innerHTML = engines.map(engine => `<option value="${safeEsc(engine)}">${safeEsc(engine)}</option>`).join('');
      if (!engineSelect.value) engineSelect.value = engines[0] || 'postgres';
    }
    syncQueryDatabaseOptions();
    renderKafkaTopicList(state.kafkaTopics);
    await loadHistory();
  }

  function parsePayloadText(text) {
    if (!text) return null;
    try { return JSON.parse(text); } catch (_) { return null; }
  }

  function prettyPayload(text) {
    const parsed = parsePayloadText(text);
    return parsed ? JSON.stringify(parsed, null, 2) : (text || '');
  }

  async function loadHistory() {
    const keyword = document.getElementById('historyKeyword')?.value || '';
    state.queryHistory = await extApi('GET', `${OPS}/history?limit=40&keyword=${encodeURIComponent(keyword)}`);
    renderHistoryList();
  }

  function renderHistoryList() {
    const list = document.getElementById('queryHistoryList');
    if (!list) return;
    list.innerHTML = state.queryHistory.length
      ? state.queryHistory.map(item => `
        <button class="history-item${state.activeHistoryRequestId === item.requestId ? ' active' : ''}" data-request-id="${safeEsc(item.requestId)}">
          <div class="history-head"><span>${safeEsc(item.requestType || 'QUERY')}</span><span>${safeEsc(item.status || '-')}</span></div>
          <div class="history-meta">
            <span>${safeEsc(item.transport || '-')}</span>
            <span>${safeEsc(item.adminEmail || '-')}</span>
            <span>${safeEsc(item.targetName || item.topicName || '-')}</span>
            <span>${safeEsc(item.requestedAt || '-')}</span>
          </div>
        </button>
      `).join('')
      : '<div class="empty-msg">Eski sorgu bulunamadi.</div>';
    list.querySelectorAll('[data-request-id]').forEach(button => {
      button.onclick = () => openHistoryItem(button.dataset.requestId).catch(error => notify(error.message, 'error'));
    });
  }

  async function openHistoryItem(requestId) {
    state.activeHistoryRequestId = requestId;
    renderHistoryList();
    const item = await extApi('GET', `${OPS}/history/${encodeURIComponent(requestId)}`);
    setText('queryHistoryDetail', {
      requestId: item.requestId,
      adminEmail: item.adminEmail,
      adminPasswordMasked: item.adminPasswordMasked,
      transport: item.transport,
      requestType: item.requestType,
      targetType: item.targetType,
      targetName: item.targetName,
      topicName: item.topicName,
      status: item.status,
      responseReceived: item.responseReceived,
      responseType: item.responseType,
      requestedAt: item.requestedAt,
      receivedAt: item.receivedAt,
      queryText: item.queryText,
      requestPayload: prettyPayload(item.requestPayload),
      responsePayload: prettyPayload(item.responsePayload),
      errorMessage: item.errorMessage
    });
  }

  async function pollHistoryUntilDone(requestId, renderer) {
    state.activeHistoryRequestId = requestId;
    renderHistoryList();
    for (let attempt = 0; attempt < 30; attempt += 1) {
      const item = await extApi('GET', `${OPS}/history/${encodeURIComponent(requestId)}`);
      if (item.status === 'COMPLETED' || item.status === 'FAILED') {
        await loadHistory();
        await openHistoryItem(requestId);
        const parsed = parsePayloadText(item.responsePayload);
        if (parsed) renderer(parsed);
        if (item.status === 'FAILED') notify(item.errorMessage || 'Asenkron sorgu basarisiz', 'error');
        return;
      }
      await new Promise(resolve => setTimeout(resolve, 2000));
    }
    notify('Asenkron sorgu zamani doldu', 'warning');
  }

  function renderDbResult(result) {
    state.currentDbResult = result;
    const columns = Array.isArray(result.columns) ? result.columns : [];
    const rows = Array.isArray(result.rows) ? result.rows : [];
    setHtml('queryResultMeta', [
      result.requestId ? `<span class="badge badge-muted">${safeEsc(result.requestId)}</span>` : '',
      result.transport ? `<span class="badge badge-info">${safeEsc(result.transport)}</span>` : '',
      `<span class="badge badge-info">${safeEsc(result.engine || '-')}</span>`,
      `<span class="badge badge-muted">${safeEsc(result.database || '-')}</span>`,
      `<span class="badge ${result.raw?.exitCode === 0 ? 'badge-success' : 'badge-danger'}">exit ${safeEsc(result.raw?.exitCode ?? '-')}</span>`,
      rows.length ? `<span class="badge badge-success">${safeEsc(rows.length)} satir</span>` : ''
    ].filter(Boolean).join(''));
    setHtml('dbQueryTableHead', columns.length ? `<tr>${columns.map(column => `<th>${safeEsc(column)}</th>`).join('')}</tr>` : '<tr><th>Sonuc</th></tr>');
    setHtml('dbQueryTableBody', rows.length
      ? rows.map(row => `<tr>${columns.map(column => `<td>${safeEsc(row[column])}</td>`).join('')}</tr>`).join('')
      : `<tr><td>${safeEsc(result.pretty || result.json || result.raw?.output || 'Tablo sonucu yok')}</td></tr>`);
    setText('dbQueryJson', result.json || result.pretty || result.raw?.output || 'JSON sonucu yok.');
    setText('dbQueryRaw', result.raw?.output || 'Ham sonuc yok.');
    setActiveQueryResultView(columns.length ? 'table' : (result.json ? 'json' : 'raw'));
  }

  async function executeDbQuery() {
    const engine = document.getElementById('queryEngine')?.value || 'postgres';
    const database = document.getElementById('queryDatabase')?.value || '';
    const query = document.getElementById('queryEditor')?.value || '';
    const payload = { engine, database, query, transport: document.getElementById('queryTransport')?.value || 'grpc' };
    if (engine === 'elasticsearch') {
      payload.method = document.getElementById('queryMethod')?.value || 'GET';
      payload.path = document.getElementById('queryPath')?.value || '';
      payload.requestBody = query;
    }
    renderDbResult({ engine, database, raw: { output: 'Sorgu calisiyor...' } });
    const result = await extApi('POST', `${OPS}/query/database`, payload);
    if (result.status === 'accepted' && result.requestId) {
      renderDbResult({ engine, database, requestId: result.requestId, transport: result.transport, raw: { output: result.message || 'Asenkron sorgu kabul edildi.' } });
      await loadHistory();
      await pollHistoryUntilDone(result.requestId, renderDbResult);
      return;
    }
    renderDbResult(result);
    await loadHistory();
    if (result.requestId) await openHistoryItem(result.requestId);
    activity('info', `Query calisti: ${engine}/${database}`);
  }

  function renderKafkaTopicList(topics) {
    const list = document.getElementById('kafkaTopicList');
    if (!list) return;
    list.innerHTML = topics.length
      ? topics.map((topic, index) => `<button class="topic-item${index === 0 ? ' active' : ''}" data-topic="${safeEsc(topic)}"><strong>${safeEsc(topic)}</strong><small>Ac ve JSON olarak incele</small></button>`).join('')
      : '<div class="empty-msg">Topic bulunamadi.</div>';
    list.querySelectorAll('[data-topic]').forEach(button => {
      button.onclick = () => {
        list.querySelectorAll('.topic-item').forEach(item => item.classList.remove('active'));
        button.classList.add('active');
        document.getElementById('selectedTopicName').value = button.dataset.topic;
      };
    });
    if (topics[0]) {
      document.getElementById('selectedTopicName').value = topics[0];
    }
  }

  function renderKafkaResult(result) {
    state.currentKafkaResult = result;
    setHtml('kafkaMeta', [
      result.requestId ? `<span class="badge badge-muted">${safeEsc(result.requestId)}</span>` : '',
      result.transport ? `<span class="badge badge-info">${safeEsc(result.transport)}</span>` : '',
      `<span class="badge badge-info">${safeEsc(result.topic || '-')}</span>`,
      `<span class="badge badge-success">${safeEsc((result.messages || []).length)} mesaj</span>`,
      `<span class="badge ${result.raw?.exitCode === 0 ? 'badge-success' : 'badge-danger'}">exit ${safeEsc(result.raw?.exitCode ?? '-')}</span>`
    ].join(''));
    setText('kafkaTopicDescribe', result.describe?.output || 'Describe sonucu yok.');
    setText('kafkaTopicRaw', result.raw?.output || 'Ham Kafka ciktilari yok.');
    setHtml('kafkaMessageCards', (result.messages || []).map(message => `
      <div class="message-card">
        <div class="message-head">
          <span>partition ${safeEsc(message.partition ?? '-')}</span>
          <span>offset ${safeEsc(message.offset ?? '-')}</span>
          <span>${safeEsc(message.timestamp ?? '-')}</span>
        </div>
        <div class="message-raw"><strong>Key:</strong> ${safeEsc(message.key || '-')}</div>
        <pre class="message-json">${safeEsc(message.json || message.value || '')}</pre>
      </div>
    `).join('') || '<div class="empty-msg">Mesaj bulunamadi.</div>');
  }

  async function openKafkaTopic() {
    const topic = document.getElementById('selectedTopicName')?.value || '';
    if (!topic) {
      notify('Once bir topic sec', 'warning');
      return;
    }
    renderKafkaResult({ topic, raw: { output: 'Topic aciliyor...' }, messages: [] });
    const transport = document.getElementById('queryTransport')?.value || 'grpc';
    const result = await extApi('GET', `${OPS}/query/kafka/topic?topic=${encodeURIComponent(topic)}&maxMessages=${encodeURIComponent(document.getElementById('kafkaMessageCount').value || '20')}&fromBeginning=${encodeURIComponent(document.getElementById('kafkaFromBeginning').value || 'true')}&transport=${encodeURIComponent(transport)}`);
    if (result.status === 'accepted' && result.requestId) {
      renderKafkaResult({ topic, requestId: result.requestId, transport: result.transport, raw: { output: result.message || 'Asenkron topic okuma kabul edildi.' }, messages: [] });
      await loadHistory();
      await pollHistoryUntilDone(result.requestId, renderKafkaResult);
      return;
    }
    renderKafkaResult(result);
    await loadHistory();
    if (result.requestId) await openHistoryItem(result.requestId);
    activity('info', `Kafka topic acildi: ${topic}`);
  }

  async function loadKafkaTopicCatalog() {
    const data = await extApi('GET', `${OPS}/query/kafka/topics`);
    state.kafkaTopics = data.topics || [];
    renderKafkaTopicList(state.kafkaTopics);
  }

  window.initQueryWorkbench = function initQueryWorkbench() {
    if (!document.getElementById('btnRefreshQueryCatalog')) return;
    document.querySelectorAll('.query-tab-btn').forEach(button => {
      button.onclick = () => setActiveQueryPanel(button.dataset.queryPanel);
    });
    document.querySelectorAll('.query-view-btn').forEach(button => {
      button.onclick = () => setActiveQueryResultView(button.dataset.queryView);
    });
    document.getElementById('btnRefreshQueryCatalog').onclick = () => loadQueryCatalog().then(() => notify('Query kaynaklari guncellendi', 'success')).catch(error => notify(error.message, 'error'));
    document.getElementById('queryEngine').onchange = syncQueryDatabaseOptions;
    document.getElementById('queryDatabase').onchange = refreshQueryTemplates;
    document.getElementById('btnRefreshHistory').onclick = () => loadHistory().catch(error => notify(error.message, 'error'));
    document.getElementById('btnLoadDbTemplate').onclick = loadSelectedTemplate;
    document.getElementById('btnRunDbQuery').onclick = () => executeDbQuery().catch(error => {
      setText('dbQueryRaw', error.message);
      notify(error.message, 'error');
    });
    document.getElementById('btnCopyDbQuery').onclick = async () => {
      try {
        await navigator.clipboard.writeText(document.getElementById('queryEditor').value || '');
        notify('Sorgu kopyalandi', 'success');
      } catch (_) {
        notify('Kopyalama basarisiz', 'warning');
      }
    };
    document.getElementById('btnClearDbQuery').onclick = () => { document.getElementById('queryEditor').value = ''; };
    document.getElementById('btnRefreshKafkaTopics').onclick = () => loadKafkaTopicCatalog().catch(error => notify(error.message, 'error'));
    document.getElementById('btnOpenKafkaTopic').onclick = () => openKafkaTopic().catch(error => {
      setText('kafkaTopicRaw', error.message);
      notify(error.message, 'error');
    });

    if (!state.queryCatalog) {
      loadQueryCatalog().catch(error => notify(error.message, 'error'));
    } else {
      syncQueryDatabaseOptions();
      renderKafkaTopicList(state.kafkaTopics);
    }
  };

  document.addEventListener('DOMContentLoaded', () => {
    injectStyles();
    injectPages();
  });
})();
