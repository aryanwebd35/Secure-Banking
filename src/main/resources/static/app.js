/* =================================================================
   SecureBank — app.js  (v2)
   Auth: Clerk v5 sign-in / sign-up (OTP) + Admin + Demo guest
   Features: Dashboard, Deposit, Withdraw, Transfer, Admin CRUD
   ================================================================= */

'use strict';

// ── Auth State ──────────────────────────────────────────────────
let jwtToken  = localStorage.getItem('bank_jwt');
let userRole  = localStorage.getItem('bank_role') || 'USER';
let userEmail = localStorage.getItem('bank_email') || '';

// ── Clerk instance (loaded async) ──────────────────────────────
let clerk = null;

// ── PUBLISHABLE KEY ─────────────────────────────────────────────
const CLERK_PUBLISHABLE_KEY = 'pk_test_bWVhc3VyZWQtYXNwLTY3LmNsZXJrLmFjY291bnRzLmRldiQ';

// =================================================================
//  ROUTER — data-page attribute based
// =================================================================
const PAGES = ['home', 'login', 'signup', 'about', 'customer', 'admin'];

function navigateTo(pageId) {
  // If already logged in and trying to go to login/signup, redirect
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

  // Nav active state
  document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
  if (pageId === 'home') document.getElementById('navHome')?.classList.add('active');
  if (pageId === 'about') document.getElementById('navAbout')?.classList.add('active');

  // Show/hide main nav vs auth header
  renderHeader();

  // Trigger page-specific init
  if (pageId === 'customer') { loadCustomerDashboard(); }
  if (pageId === 'admin')    { loadAdminDashboard(); }
  if (pageId === 'login')    { mountClerkSignIn(); }
  if (pageId === 'signup')   { mountClerkSignUp(); }

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
    const el = document.getElementById(`dashSection-${s}`);
    if (el) el.style.display = (s === section) ? 'block' : 'none';
    const nav = document.getElementById(`nav-${s}`);
    if (nav) { nav.classList.toggle('active', s === section); }
  });
  if (section === 'history') loadFullHistory();
}

// =================================================================
//  CLERK v5 — Sign-In and Sign-Up mount
// =================================================================
async function initClerk() {
  try {
    // Wait for Clerk global to be available (loaded async)
    const clerkInstance = await waitForClerk();
    if (!clerkInstance) {
      console.warn('Clerk SDK not available — OTP login will not work. Email demo login is still available.');
      updateClerkMountFallback();
      return;
    }

    clerk = clerkInstance;

    // Load Clerk with publishable key
    await clerk.load({ publishableKey: CLERK_PUBLISHABLE_KEY });
    console.info('Clerk v5 loaded successfully');

    // If user already has an active Clerk session, sync with backend
    if (clerk.session && !jwtToken) {
      console.info('Existing Clerk session found — syncing with backend');
      await syncClerkSession();
    }

    // Listen for session changes (sign-in / sign-out via Clerk)
    clerk.addListener(async ({ session }) => {
      if (session && !jwtToken) {
        await syncClerkSession();
      }
    });

  } catch (err) {
    console.warn('Clerk init error:', err.message);
    updateClerkMountFallback();
  }
}

function waitForClerk(timeoutMs = 8000) {
  return new Promise((resolve) => {
    if (window.Clerk) return resolve(window.Clerk);
    const start = Date.now();
    const check = setInterval(() => {
      if (window.Clerk) { clearInterval(check); resolve(window.Clerk); }
      else if (Date.now() - start > timeoutMs) { clearInterval(check); resolve(null); }
    }, 200);
  });
}

function updateClerkMountFallback() {
  ['clerkSignInMount', 'clerkSignUpMount'].forEach(id => {
    const el = document.getElementById(id);
    if (el) {
      el.innerHTML = `
        <div style="text-align:center; padding:1rem; color:var(--gray-500); font-size:.85rem;">
          <div style="font-size:1.25rem; margin-bottom:.4rem;">⚠️</div>
          Clerk authentication unavailable in this environment.<br>
          <strong>Use Demo Access or Admin Login below.</strong>
        </div>`;
    }
  });
}

// Mount Clerk's embedded sign-in widget into the login page
function mountClerkSignIn() {
  if (!clerk) {
    // Clerk might not have loaded yet — try again after a short delay
    setTimeout(() => { if (clerk) mountClerkSignIn(); else updateClerkMountFallback(); }, 1000);
    return;
  }
  const el = document.getElementById('clerkSignInMount');
  if (!el) return;
  el.innerHTML = ''; // Clear placeholder
  try {
    clerk.mountSignIn(el, {
      routing: 'virtual',
      afterSignInUrl: '/',
      afterSignUpUrl: '/',
    });
  } catch (err) {
    console.warn('Clerk mountSignIn error:', err.message);
    updateClerkMountFallback();
  }
}

// Mount Clerk's embedded sign-up widget into the signup page
function mountClerkSignUp() {
  if (!clerk) {
    setTimeout(() => { if (clerk) mountClerkSignUp(); else updateClerkMountFallback(); }, 1000);
    return;
  }
  const el = document.getElementById('clerkSignUpMount');
  if (!el) return;
  el.innerHTML = '';
  try {
    clerk.mountSignUp(el, {
      routing: 'virtual',
      afterSignUpUrl: '/',
      afterSignInUrl: '/',
    });
  } catch (err) {
    console.warn('Clerk mountSignUp error:', err.message);
    updateClerkMountFallback();
  }
}

// After Clerk auth completes, exchange Clerk token for backend JWT
async function syncClerkSession() {
  try {
    if (!clerk || !clerk.session) return;
    const clerkToken = await clerk.session.getToken();
    if (!clerkToken) return;

    const res = await fetch('/api/auth/clerk-login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ clerkToken }),
    });

    if (!res.ok) {
      showToast('Authentication failed — please try again.', 'error');
      return;
    }

    const data = await res.json();
    saveSession(data.token, 'USER', data.email);
    renderHeader();

    if (data.needsPhoneSetup) {
      // Show phone setup modal before navigating to dashboard
      showPhoneSetupModal();
    } else {
      showToast(`Welcome back, ${data.email.split('@')[0]}! 🎉`, 'success');
      navigateTo('customer');
    }
  } catch (err) {
    console.error('Clerk session sync error:', err);
    showToast('Failed to sync authentication. Please try again.', 'error');
  }
}

// =================================================================
//  PHONE SETUP MODAL
// =================================================================
function showPhoneSetupModal() {
  const modal = document.getElementById('phoneSetupModal');
  if (modal) modal.style.display = 'flex';
}
function hidePhoneSetupModal() {
  const modal = document.getElementById('phoneSetupModal');
  if (modal) modal.style.display = 'none';
}

async function handlePhoneSetup(e) {
  e.preventDefault();
  const phone = document.getElementById('setupPhone').value.trim();
  const btn   = document.getElementById('phoneSetupBtn');
  const errEl = document.getElementById('phoneSetupError');

  if (!/^\d{10}$/.test(phone)) {
    errEl.textContent = 'Please enter exactly 10 digits.';
    errEl.style.display = 'block';
    return;
  }

  btn.disabled = true;
  btn.innerHTML = '<span class="loading-spinner"></span> Setting up account…';
  errEl.style.display = 'none';

  try {
    const res = await fetch('/api/auth/setup-phone', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${jwtToken}`,
      },
      body: JSON.stringify({ phoneNumber: phone }),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'That phone number may already be in use. Try a different one.');
    }

    const data = await res.json();
    saveSession(data.token, 'USER', data.email);
    hidePhoneSetupModal();
    showToast('Account activated! Welcome to SecureBank 🎉', 'success');
    navigateTo('customer');
  } catch (err) {
    errEl.textContent = err.message;
    errEl.style.display = 'block';
  } finally {
    btn.disabled = false;
    btn.textContent = 'Confirm & Open My Account →';
  }
}

// =================================================================
//  ADMIN LOGIN
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
    showToast(err.message, 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = 'Sign In as Admin';
  }
}

// =================================================================
//  DEMO LOGIN
// =================================================================
function showDemoLogin() {
  document.getElementById('demoLoginForm').style.display = 'block';
  document.getElementById('btnShowDemo').style.display = 'none';
}
function hideDemoLogin() {
  document.getElementById('demoLoginForm').style.display = 'none';
  document.getElementById('btnShowDemo').style.display = '';
}

async function handleDemoLogin(e) {
  e.preventDefault();
  const name  = document.getElementById('demoName').value.trim();
  const email = document.getElementById('demoEmail').value.trim().toLowerCase();
  const btn   = document.getElementById('demoLoginBtn');
  btn.disabled = true;
  btn.innerHTML = '<span class="loading-spinner"></span> Entering demo…';

  try {
    const res = await fetch('/api/auth/simulated-login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, name }),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Demo login failed. Please try again.');
    }
    const data = await res.json();
    saveSession(data.token, 'USER', data.email);
    showToast(`Welcome, ${name}! (Demo mode) 🎭`, 'success');
    renderHeader();
    navigateTo('customer');
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = 'Enter Demo Mode';
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
  if (clerk && clerk.session) clerk.signOut();
  showToast('Signed out successfully.', 'success');
  renderHeader();
  navigateTo('home');
}

// =================================================================
//  HEADER RENDER
// =================================================================
function renderHeader() {
  const mainNav       = document.getElementById('mainNav');
  const headerStatus  = document.getElementById('headerAuthStatus');
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
  // Update sidebar info
  const name = userEmail ? userEmail.split('@')[0] : 'User';
  const cap = name.charAt(0).toUpperCase() + name.slice(1);

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

    // Balance card
    const balEl = document.getElementById('custBalance');
    if (balEl) balEl.textContent = fmt(data.accountBalance);

    const depEl = document.getElementById('custTotalDeposits');
    const witEl = document.getElementById('custTotalWithdrawals');
    if (depEl) depEl.textContent = fmt(data.totalDeposits);
    if (witEl) witEl.textContent = fmt(data.totalWithdrawals);

    const holderEl = document.getElementById('custHolderName');
    const phoneEl  = document.getElementById('custPhoneNumber');
    const acnoEl   = document.getElementById('custAccountIdChip');
    const statusEl = document.getElementById('custAccountStatus');

    if (holderEl) holderEl.textContent = data.accountHolderName || cap;
    if (phoneEl)  phoneEl.textContent  = data.phoneNumber || '—';
    if (acnoEl)   acnoEl.textContent   = `ACC #${data.accountId || '—'}`;
    if (statusEl) statusEl.textContent = data.accountStatus || 'ACTIVE';

    // Recent transactions table
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
    tbody.innerHTML = `<tr><td colspan="5"><div class="empty-state"><div class="empty-state-icon">📭</div><p>No transactions yet — make your first deposit!</p></div></td></tr>`;
    return;
  }

  txns.forEach(tx => {
    const typeClass = tx.transactionType === 'DEPOSIT' ? 'tx-deposit' : tx.transactionType === 'WITHDRAW' ? 'tx-withdraw' : 'tx-transfer';
    const badge     = tx.status === 'SUCCESS'
      ? '<span class="badge badge-green">✓ Success</span>'
      : '<span class="badge badge-red">✕ Failed</span>';
    const prefix    = tx.transactionType === 'DEPOSIT' ? '+' : '-';
    const dateStr   = tx.timestamp
      ? new Date(tx.timestamp).toLocaleString('en-IN', { day:'2-digit', month:'short', year:'numeric', hour:'2-digit', minute:'2-digit' })
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
    // Refresh overview
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
//  NOTIFY ME (SIP/MF)
// =================================================================
function notifyMe(type) {
  const emailId = type === 'sip' ? 'sipEmail' : 'mfEmail';
  const email   = document.getElementById(emailId)?.value.trim();
  if (!email || !email.includes('@')) {
    showToast('Enter a valid email address.', 'error');
    return;
  }
  showToast(`You'll be notified at ${email} when ${type === 'sip' ? 'SIP' : 'Mutual Funds'} launches! 🚀`, 'success');
  document.getElementById(emailId).value = '';
}

// =================================================================
//  TOAST
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

  // Determine initial page
  if (jwtToken) {
    navigateTo(userRole === 'ADMIN' ? 'admin' : 'customer');
  } else {
    navigateTo('home');
  }

  // Init Clerk in background (non-blocking)
  initClerk();
});
