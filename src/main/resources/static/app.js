/* =================================================================
   SecureBank — app.js
   Auth: Standard email + password (JWT) — no Clerk, no OTP
   Features: Register, Login, Admin Login, Dashboard, Deposit, Withdraw, Transfer
   ================================================================= */

'use strict';

// ── Auth State (persisted in localStorage) ──────────────────────
let jwtToken  = localStorage.getItem('bank_jwt');
let userRole  = localStorage.getItem('bank_role') || 'USER';
let userEmail = localStorage.getItem('bank_email') || '';

// =================================================================
//  ROUTER — data-page attribute based SPA routing
// =================================================================
const PAGES = ['home', 'login', 'signup', 'about', 'customer', 'admin'];

function navigateTo(pageId) {
  // If already logged in and trying to go to login/signup, redirect to dashboard
  if (jwtToken && (pageId === 'login' || pageId === 'signup')) {
    pageId = (userRole === 'ADMIN') ? 'admin' : 'customer';
  }

  // Hide all pages
  PAGES.forEach(p => {
    const el = document.querySelector(`[data-page="${p}"]`);
    if (el) { el.classList.remove('active-page'); el.style.display = 'none'; }
  });

  // Show target page
  const target = document.querySelector(`[data-page="${pageId}"]`);
  if (target) { target.classList.add('active-page'); target.style.display = 'block'; }

  // Update nav active state
  document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
  if (pageId === 'home')  document.getElementById('navHome')?.classList.add('active');
  if (pageId === 'about') document.getElementById('navAbout')?.classList.add('active');

  // Update header
  renderHeader();

  // Page-specific init
  if (pageId === 'customer') loadCustomerDashboard();
  if (pageId === 'admin')    loadAdminDashboard();

  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function scrollToSection(id) {
  navigateTo('home');
  setTimeout(() => {
    const el = document.getElementById(id);
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }, 200);
}

// ── Dashboard sub-sections ───────────────────────────────────────
function showDashSection(section) {
  ['overview', 'deposit', 'withdraw', 'transfer', 'history'].forEach(s => {
    const el  = document.getElementById(`dashSection-${s}`);
    const nav = document.getElementById(`nav-${s}`);
    if (el)  el.style.display = (s === section) ? 'block' : 'none';
    if (nav) nav.classList.toggle('active', s === section);
  });
  if (section === 'history') loadFullHistory();
}

// =================================================================
//  AUTH — Register (POST /api/auth/register)
// =================================================================
async function handleRegister(e) {
  e.preventDefault();
  const name        = document.getElementById('regName').value.trim();
  const email       = document.getElementById('regEmail').value.trim().toLowerCase();
  const password    = document.getElementById('regPassword').value;
  const phoneNumber = document.getElementById('regPhone').value.trim();
  const btn         = document.getElementById('regBtn');
  const errEl       = document.getElementById('regError');

  // Clear previous error
  errEl.style.display = 'none';
  errEl.textContent = '';
  btn.disabled = true;
  btn.innerHTML = '<span class="loading-spinner"></span> Creating account…';

  try {
    const res = await fetch('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, email, password, phoneNumber }),
    });

    if (res.status === 409) {
      throw new Error('An account with this email or phone number already exists.');
    }
    if (!res.ok) {
      const data = await res.json().catch(() => ({}));
      throw new Error(data.message || `Registration failed (${res.status}). Check your details.`);
    }

    const data = await res.json();
    saveSession(data.token, 'USER', data.email);
    showToast(`Welcome to SecureBank, ${name}! 🎉`, 'success');
    renderHeader();
    navigateTo('customer');
  } catch (err) {
    errEl.textContent = err.message;
    errEl.style.display = 'block';
  } finally {
    btn.disabled = false;
    btn.textContent = 'Open Free Account →';
  }
}

// =================================================================
//  AUTH — Login (POST /api/auth/login)
// =================================================================
async function handleLogin(e) {
  e.preventDefault();
  const email    = document.getElementById('loginEmail').value.trim().toLowerCase();
  const password = document.getElementById('loginPassword').value;
  const btn      = document.getElementById('loginBtn');
  const errEl    = document.getElementById('loginError');

  errEl.style.display = 'none';
  errEl.textContent = '';
  btn.disabled = true;
  btn.innerHTML = '<span class="loading-spinner"></span> Signing in…';

  try {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    });

    if (res.status === 401) {
      throw new Error('Invalid email or password. Please try again.');
    }
    if (!res.ok) throw new Error(`Login failed (${res.status}). Please try again.`);

    const data = await res.json();
    saveSession(data.token, 'USER', data.email);
    showToast(`Welcome back, ${email.split('@')[0]}! 👋`, 'success');
    renderHeader();
    navigateTo('customer');
  } catch (err) {
    errEl.textContent = err.message;
    errEl.style.display = 'block';
  } finally {
    btn.disabled = false;
    btn.textContent = 'Sign In →';
  }
}

// =================================================================
//  AUTH — Admin Login (POST /api/auth/admin/login)
// =================================================================
function showAdminLogin() {
  document.getElementById('adminLoginForm').style.display = 'block';
  document.getElementById('btnShowAdmin').style.display = 'none';
}
function hideAdminLogin() {
  document.getElementById('adminLoginForm').style.display = 'none';
  document.getElementById('btnShowAdmin').style.display = '';
}

async function handleAdminLogin(e) {
  e.preventDefault();
  const email    = document.getElementById('adminEmail').value.trim();
  const password = document.getElementById('adminPassword').value;
  const btn      = document.getElementById('adminLoginBtn');
  const errEl    = document.getElementById('adminLoginError');

  errEl.style.display = 'none';
  btn.disabled = true;
  btn.innerHTML = '<span class="loading-spinner"></span> Signing in…';

  try {
    const res = await fetch('/api/auth/admin/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    });
    if (!res.ok) throw new Error('Invalid admin credentials. Check email and password.');
    const data = await res.json();
    saveSession(data.token, 'ADMIN', data.email);
    showToast('Welcome, Administrator! 🔐', 'success');
    renderHeader();
    navigateTo('admin');
  } catch (err) {
    errEl.textContent = err.message;
    errEl.style.display = 'block';
  } finally {
    btn.disabled = false;
    btn.textContent = 'Sign In as Admin';
  }
}

// =================================================================
//  SESSION
// =================================================================
function saveSession(token, role, email) {
  jwtToken = token; userRole = role; userEmail = email;
  localStorage.setItem('bank_jwt', token);
  localStorage.setItem('bank_role', role);
  localStorage.setItem('bank_email', email);
}
function clearSession() {
  jwtToken = null; userRole = 'USER'; userEmail = '';
  localStorage.removeItem('bank_jwt');
  localStorage.removeItem('bank_role');
  localStorage.removeItem('bank_email');
}
function handleSignOut() {
  clearSession();
  showToast('Signed out successfully.', 'success');
  renderHeader();
  navigateTo('home');
}

// =================================================================
//  HEADER RENDER
// =================================================================
function renderHeader() {
  const mainNav      = document.getElementById('mainNav');
  const headerStatus = document.getElementById('headerAuthStatus');
  if (!headerStatus) return;
  headerStatus.innerHTML = '';

  if (!jwtToken) {
    if (mainNav) mainNav.style.display = 'flex';
    return;
  }

  if (mainNav) mainNav.style.display = 'none';

  const badge = document.createElement('span');
  badge.className = 'user-badge';
  badge.textContent = userRole === 'ADMIN' ? '🔐 Admin' : '👤 Customer';

  const emailEl = document.createElement('span');
  emailEl.className = 'user-email';
  emailEl.textContent = userEmail;

  const dashBtn = document.createElement('button');
  dashBtn.className = 'btn btn-secondary btn-sm';
  dashBtn.textContent = userRole === 'ADMIN' ? '⚙️ Dashboard' : '📊 My Account';
  dashBtn.onclick = () => navigateTo(userRole === 'ADMIN' ? 'admin' : 'customer');

  const signOutBtn = document.createElement('button');
  signOutBtn.className = 'btn btn-ghost btn-sm';
  signOutBtn.textContent = 'Sign Out';
  signOutBtn.onclick = handleSignOut;

  headerStatus.append(badge, emailEl, dashBtn, signOutBtn);
}

// =================================================================
//  CUSTOMER DASHBOARD
// =================================================================
const fmt = (n) => `₹${parseFloat(n || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

async function loadCustomerDashboard() {
  const name = userEmail ? userEmail.split('@')[0] : 'User';
  const cap  = name.charAt(0).toUpperCase() + name.slice(1);

  const avatar = document.getElementById('sidebarAvatar');
  if (avatar) avatar.textContent = cap.charAt(0).toUpperCase();
  const sName = document.getElementById('sidebarName');
  if (sName) sName.textContent = cap;
  const sEmail = document.getElementById('sidebarEmail');
  if (sEmail) sEmail.textContent = userEmail;
  const greet = document.getElementById('customerGreeting');
  if (greet) greet.textContent = `Welcome back, ${cap}! 👋`;

  try {
    const res = await fetch('/dashboard', {
      headers: { 'Authorization': `Bearer ${jwtToken}` },
    });
    if (res.status === 401 || res.status === 403) { handleSignOut(); return; }
    if (!res.ok) throw new Error(`Dashboard error (${res.status})`);

    const data = await res.json();

    const balEl    = document.getElementById('custBalance');
    const depEl    = document.getElementById('custTotalDeposits');
    const witEl    = document.getElementById('custTotalWithdrawals');
    const holderEl = document.getElementById('custHolderName');
    const phoneEl  = document.getElementById('custPhoneNumber');
    const acnoEl   = document.getElementById('custAccountIdChip');
    const statusEl = document.getElementById('custAccountStatus');

    if (balEl)    balEl.textContent    = fmt(data.accountBalance);
    if (depEl)    depEl.textContent    = fmt(data.totalDeposits);
    if (witEl)    witEl.textContent    = fmt(data.totalWithdrawals);
    if (holderEl) holderEl.textContent = data.accountHolderName || cap;
    if (phoneEl)  phoneEl.textContent  = data.phoneNumber || '—';
    if (acnoEl)   acnoEl.textContent   = `ACC #${data.accountId || '—'}`;
    if (statusEl) statusEl.textContent = data.accountStatus || 'ACTIVE';

    renderTransactionTable('custTxBody', data.recentTransactions);

  } catch (err) {
    showToast(`Error loading dashboard: ${err.message}`, 'error');
  }
}

function renderTransactionTable(tbodyId, txns) {
  const tbody = document.getElementById(tbodyId);
  if (!tbody) return;
  tbody.innerHTML = '';

  if (!txns || txns.length === 0) {
    tbody.innerHTML = `<tr><td colspan="5"><div class="empty-state"><div class="empty-state-icon">📭</div><p>No transactions yet — deposit to get started!</p></div></td></tr>`;
    return;
  }

  txns.forEach(tx => {
    const typeClass = tx.transactionType === 'DEPOSIT' ? 'tx-deposit' : tx.transactionType === 'WITHDRAW' ? 'tx-withdraw' : 'tx-transfer';
    const badge     = tx.status === 'SUCCESS'
      ? '<span class="badge badge-green">✓ Success</span>'
      : '<span class="badge badge-red">✕ Failed</span>';
    const prefix  = tx.transactionType === 'DEPOSIT' ? '+' : '-';
    const dateStr = tx.timestamp
      ? new Date(tx.timestamp).toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })
      : '—';
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td><strong class="${typeClass}">${tx.transactionType}</strong></td>
      <td><strong>${prefix}${fmt(tx.amount)}</strong></td>
      <td>${badge}</td>
      <td style="color:var(--gray-500);white-space:nowrap">${dateStr}</td>
      <td style="color:var(--gray-500)">${tx.remarks || '—'}</td>
    `;
    tbody.appendChild(tr);
  });
}

async function loadFullHistory() {
  try {
    const res = await fetch('/transactions/history?size=50', {
      headers: { 'Authorization': `Bearer ${jwtToken}` },
    });
    if (!res.ok) throw new Error('Failed to load history');
    const data = await res.json();
    renderTransactionTable('historyTxBody', data.content || []);
  } catch (err) {
    showToast(`History error: ${err.message}`, 'error');
  }
}

// Quick amount setter
function setAmount(inputId, val) {
  const el = document.getElementById(inputId);
  if (el) { el.value = val; el.focus(); }
}

// ── Deposit ──────────────────────────────────────────────────────
async function handleQuickDeposit(e) {
  e.preventDefault();
  const amount = parseFloat(document.getElementById('depositAmount').value);
  const btn    = document.getElementById('depositBtn');
  btn.disabled = true; btn.innerHTML = '<span class="loading-spinner"></span>';
  try {
    const res = await fetch('/api/accounts/my/deposit', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${jwtToken}` },
      body: JSON.stringify({ amount }),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Deposit failed.');
    }
    showToast(`${fmt(amount)} deposited successfully! 💰`, 'success');
    document.getElementById('depositAmount').value = '';
    await loadCustomerDashboard();
    showDashSection('overview');
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    btn.disabled = false; btn.textContent = '💰 Deposit Now';
  }
}

// ── Withdraw ─────────────────────────────────────────────────────
async function handleQuickWithdraw(e) {
  e.preventDefault();
  const amount = parseFloat(document.getElementById('withdrawAmount').value);
  const btn    = document.getElementById('withdrawBtn');
  btn.disabled = true; btn.innerHTML = '<span class="loading-spinner"></span>';
  try {
    const res = await fetch('/api/accounts/my/withdraw', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${jwtToken}` },
      body: JSON.stringify({ amount }),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Insufficient balance or withdrawal failed.');
    }
    showToast(`${fmt(amount)} withdrawn successfully! 🏧`, 'success');
    document.getElementById('withdrawAmount').value = '';
    await loadCustomerDashboard();
    showDashSection('overview');
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    btn.disabled = false; btn.textContent = '🏧 Withdraw Now';
  }
}

// ── Transfer ─────────────────────────────────────────────────────
async function handleTransfer(e) {
  e.preventDefault();
  const phone   = document.getElementById('receiverPhone').value.trim();
  const amount  = parseFloat(document.getElementById('transferAmount').value);
  const remarks = document.getElementById('transferRemarks').value.trim();
  const btn     = document.getElementById('transferBtn');
  btn.disabled = true; btn.innerHTML = '<span class="loading-spinner"></span> Processing…';
  try {
    const res = await fetch('/transfer', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${jwtToken}` },
      body: JSON.stringify({ receiverPhoneNumber: phone, amount, remarks }),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Transfer failed. Check the phone number and your balance.');
    }
    showToast(`${fmt(amount)} sent to ${phone}! ⚡`, 'success');
    document.getElementById('receiverPhone').value  = '';
    document.getElementById('transferAmount').value  = '';
    document.getElementById('transferRemarks').value = '';
    await loadCustomerDashboard();
    showDashSection('overview');
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    btn.disabled = false; btn.textContent = '⚡ Transfer Instantly';
  }
}

// =================================================================
//  ADMIN DASHBOARD
// =================================================================
async function loadAdminDashboard() {
  try {
    const res = await fetch('/admin/dashboard', { headers: { 'Authorization': `Bearer ${jwtToken}` } });
    if (res.status === 401 || res.status === 403) { handleSignOut(); return; }
    if (!res.ok) throw new Error('Failed to load stats.');
    const s = await res.json();

    document.getElementById('adminTotalMoney').textContent       = fmt(s.totalMoneyInSystem);
    document.getElementById('adminTotalUsers').textContent        = s.totalUsersCount ?? 0;
    document.getElementById('adminTotalAccounts').textContent     = s.totalAccountsCount ?? 0;
    document.getElementById('adminTotalTransactions').textContent = s.totalTransactionsCount ?? 0;

    const topBody = document.getElementById('adminTopHoldersBody');
    topBody.innerHTML = '';
    if (s.topAccountHolders?.length) {
      s.topAccountHolders.forEach((acc, i) => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
          <td><strong>#${i+1}</strong></td>
          <td><strong>${acc.accountHolderName||'—'}</strong></td>
          <td style="color:var(--gray-500)">${acc.user?.email||'—'}</td>
          <td style="color:var(--gray-500); font-family:monospace">${acc.user?.phoneNumber||'—'}</td>
          <td><strong>${fmt(acc.balance)}</strong></td>
        `;
        topBody.appendChild(tr);
      });
    } else {
      topBody.innerHTML = `<tr><td colspan="5"><div class="empty-state"><p>No accounts yet</p></div></td></tr>`;
    }
  } catch (err) { showToast(`Stats error: ${err.message}`, 'error'); }

  try {
    const res = await fetch('/admin/accounts?size=100', { headers: { 'Authorization': `Bearer ${jwtToken}` } });
    if (!res.ok) throw new Error('Failed to load accounts.');
    const data = await res.json();
    const body = document.getElementById('adminAccountsBody');
    body.innerHTML = '';

    if (!data.content?.length) {
      body.innerHTML = `<tr><td colspan="7"><div class="empty-state"><p>No accounts found</p></div></td></tr>`;
      return;
    }

    data.content.forEach(acc => {
      const statusBadge = acc.accountStatus === 'ACTIVE'
        ? '<span class="badge badge-green">Active</span>'
        : acc.accountStatus === 'BLOCKED'
          ? '<span class="badge badge-red">Blocked</span>'
          : '<span class="badge badge-gray">Closed</span>';

      let actions = '';
      if (acc.accountStatus === 'ACTIVE') {
        actions = `<button class="btn btn-danger btn-sm" onclick="adminUpdateStatus(${acc.id},'block')">Block</button>
                   <button class="btn btn-secondary btn-sm" onclick="adminUpdateStatus(${acc.id},'close')">Close</button>`;
      } else if (acc.accountStatus === 'BLOCKED') {
        actions = `<button class="btn btn-success btn-sm" onclick="adminUpdateStatus(${acc.id},'unblock')">Unblock</button>
                   <button class="btn btn-secondary btn-sm" onclick="adminUpdateStatus(${acc.id},'close')">Close</button>`;
      } else {
        actions = '<span style="color:var(--gray-400);font-size:.8rem;">—</span>';
      }

      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td style="color:var(--gray-400); font-family:monospace">#${acc.id}</td>
        <td><strong>${acc.accountHolderName||'—'}</strong></td>
        <td style="color:var(--gray-500)">${acc.user?.email||'—'}</td>
        <td style="color:var(--gray-500); font-family:monospace">${acc.user?.phoneNumber||'—'}</td>
        <td><strong>${fmt(acc.balance)}</strong></td>
        <td>${statusBadge}</td>
        <td><div class="actions-cell">${actions}</div></td>
      `;
      body.appendChild(tr);
    });
  } catch (err) { showToast(`Accounts error: ${err.message}`, 'error'); }
}

async function adminUpdateStatus(id, action) {
  try {
    const res = await fetch(`/admin/${action}-account/${id}`, {
      method: 'PUT',
      headers: { 'Authorization': `Bearer ${jwtToken}` },
    });
    if (!res.ok) throw new Error(`Failed to ${action} account.`);
    showToast(`Account #${id} ${action}ed successfully.`, 'success');
    await loadAdminDashboard();
  } catch (err) { showToast(err.message, 'error'); }
}

// =================================================================
//  NOTIFY ME (SIP/MF coming soon features)
// =================================================================
function notifyMe(type) {
  const emailId = type === 'sip' ? 'sipEmail' : 'mfEmail';
  const email   = document.getElementById(emailId)?.value.trim();
  if (!email || !email.includes('@')) { showToast('Enter a valid email address.', 'error'); return; }
  showToast(`You'll be notified at ${email} when ${type === 'sip' ? 'SIP' : 'Mutual Funds'} launches! 🚀`, 'success');
  document.getElementById(emailId).value = '';
}

// =================================================================
//  TOAST NOTIFICATIONS
// =================================================================
function showToast(message, type = 'info') {
  const container = document.getElementById('toastContainer');
  if (!container) return;
  const icon = type === 'success' ? '✅' : type === 'error' ? '❌' : 'ℹ️';
  const t = document.createElement('div');
  t.className = `toast ${type}`;
  t.innerHTML = `<div class="toast-icon">${icon}</div><span>${message}</span>`;
  container.appendChild(t);
  setTimeout(() => {
    t.style.animation = 'toastIn .3s ease reverse forwards';
    setTimeout(() => t.remove(), 320);
  }, 4500);
}

// =================================================================
//  INIT
// =================================================================
window.addEventListener('DOMContentLoaded', () => {
  renderHeader();
  // Restore session: go to dashboard if already logged in, otherwise home
  if (jwtToken) {
    navigateTo(userRole === 'ADMIN' ? 'admin' : 'customer');
  } else {
    navigateTo('home');
  }
});
