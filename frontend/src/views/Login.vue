<template>
  <div class="login-page">
    <!-- Background Effects -->
    <div class="login-bg">
      <div class="grid-pattern"></div>
      <div class="glow glow-1"></div>
      <div class="glow glow-2"></div>
    </div>

    <!-- Left Branding Panel -->
    <div class="login-brand">
      <div class="brand-content">
        <div class="brand-logo">
          <span class="logo-mark">AH</span>
          <div class="logo-glow"></div>
        </div>
        <h1 class="brand-title">AgentHub</h1>
        <p class="brand-tagline">Enterprise Agent Operations Platform</p>
        <div class="brand-divider"></div>
        <p class="brand-desc">
          Configure, deploy, and monitor AI agents at scale.
          Multi-tenant architecture with full observability.
        </p>
        <div class="brand-features">
          <div class="feature">
            <span class="feature-icon">&#9679;</span>
            <span>Multi-tenant Isolation</span>
          </div>
          <div class="feature">
            <span class="feature-icon">&#9679;</span>
            <span>RAG & Hybrid Search</span>
          </div>
          <div class="feature">
            <span class="feature-icon">&#9679;</span>
            <span>Full Trace Observability</span>
          </div>
          <div class="feature">
            <span class="feature-icon">&#9679;</span>
            <span>Enterprise Bot Channels</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Right Form Panel -->
    <div class="login-form-area">
      <div class="form-wrapper">
        <div class="form-header">
          <h2>Welcome back</h2>
          <p>Sign in to your workspace</p>
        </div>

        <form @submit.prevent="submitLogin" class="login-form">
          <div class="input-group">
            <label class="input-label" for="tenant">Tenant</label>
            <div class="input-wrapper">
              <span class="input-icon">&#9635;</span>
              <input
                id="tenant"
                v-model="loginForm.tenantCode"
                class="input"
                type="text"
                placeholder="your-tenant"
                autocomplete="organization"
                required
              />
            </div>
          </div>

          <div class="input-group">
            <label class="input-label" for="username">Username</label>
            <div class="input-wrapper">
              <span class="input-icon">&#9786;</span>
              <input
                id="username"
                v-model="loginForm.username"
                class="input"
                type="text"
                placeholder="admin"
                autocomplete="username"
                required
              />
            </div>
          </div>

          <div class="input-group">
            <label class="input-label" for="password">Password</label>
            <div class="input-wrapper">
              <span class="input-icon">&#9679;</span>
              <input
                id="password"
                v-model="loginForm.password"
                class="input"
                type="password"
                placeholder="Enter password"
                autocomplete="current-password"
                required
              />
            </div>
          </div>

          <button
            type="submit"
            class="btn btn-primary btn-lg login-btn"
            :disabled="loginBusy"
          >
            <span v-if="!loginBusy">Sign In</span>
            <span v-else class="spinner"></span>
          </button>

          <p v-if="loginError" class="login-error">{{ loginError }}</p>
        </form>

        <p class="login-footer">
          Demo credentials pre-filled &middot; Contact your admin
        </p>
      </div>
    </div>
  </div>
</template>

<script>
import { login, loadCurrentUser } from '../api';

export default {
  data() {
    return {
      loginBusy: false,
      loginError: '',
      loginForm: {
        tenantCode: localStorage.getItem('tenantId') || 'tenant-001',
        username: 'admin',
        password: ''
      }
    };
  },
  methods: {
    async submitLogin() {
      this.loginBusy = true;
      this.loginError = '';
      try {
        await login(this.loginForm);
        await loadCurrentUser();
      } catch (error) {
        this.loginError = error.message || 'Login failed';
      } finally {
        this.loginBusy = false;
      }
    }
  }
};
</script>

<style scoped>
.login-page {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  position: relative;
}

/* ── Background ──────────────────────────────────────────── */
.login-bg {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

.grid-pattern {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(45, 212, 168, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(45, 212, 168, 0.03) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: radial-gradient(ellipse at center, black 30%, transparent 70%);
}

.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(120px);
  opacity: 0.15;
}

.glow-1 {
  width: 600px;
  height: 600px;
  background: var(--accent);
  top: -200px;
  left: -100px;
}

.glow-2 {
  width: 400px;
  height: 400px;
  background: #6366f1;
  bottom: -150px;
  right: -50px;
  opacity: 0.08;
}

/* ── Brand Panel ─────────────────────────────────────────── */
.login-brand {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--sp-12);
  position: relative;
  z-index: 1;
}

.brand-content {
  max-width: 420px;
  animation: slideIn 0.6s var(--ease-out);
}

.brand-logo {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--sp-6);
}

.logo-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, var(--accent), var(--accent-strong));
  color: var(--bg-base);
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.02em;
  position: relative;
  z-index: 1;
}

.logo-glow {
  position: absolute;
  inset: -8px;
  border-radius: var(--radius-xl);
  background: var(--accent);
  opacity: 0.2;
  filter: blur(16px);
  z-index: 0;
}

.brand-title {
  font-family: var(--font-display);
  font-size: var(--text-3xl);
  font-weight: 800;
  letter-spacing: -0.03em;
  color: var(--text-primary);
  line-height: 1.1;
}

.brand-tagline {
  margin-top: var(--sp-2);
  font-size: var(--text-md);
  color: var(--accent);
  font-weight: 500;
  letter-spacing: 0.02em;
}

.brand-divider {
  width: 48px;
  height: 3px;
  background: linear-gradient(90deg, var(--accent), transparent);
  border-radius: var(--radius-full);
  margin: var(--sp-6) 0;
}

.brand-desc {
  font-size: var(--text-md);
  color: var(--text-tertiary);
  line-height: var(--leading-relaxed);
  margin-bottom: var(--sp-8);
}

.brand-features {
  display: grid;
  gap: var(--sp-3);
}

.feature {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

.feature-icon {
  color: var(--accent);
  font-size: 8px;
}

/* ── Form Panel ──────────────────────────────────────────── */
.login-form-area {
  width: 440px;
  min-width: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--sp-8);
  position: relative;
  z-index: 1;
  background: linear-gradient(180deg, transparent, rgba(11, 14, 18, 0.4));
}

.form-wrapper {
  width: 100%;
  max-width: 360px;
  animation: slideUp 0.6s var(--ease-out);
}

.form-header {
  margin-bottom: var(--sp-8);
}

.form-header h2 {
  font-family: var(--font-display);
  font-size: var(--text-2xl);
  font-weight: 700;
  letter-spacing: -0.02em;
  color: var(--text-primary);
}

.form-header p {
  margin-top: var(--sp-1);
  font-size: var(--text-sm);
  color: var(--text-tertiary);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: var(--sp-5);
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: var(--sp-1-5);
}

.input-label {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--text-secondary);
}

.input-wrapper {
  position: relative;
}

.input-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-muted);
  font-size: 14px;
  pointer-events: none;
  transition: color var(--duration-fast);
}

.input-wrapper .input {
  padding-left: 36px;
}

.input-wrapper:focus-within .input-icon {
  color: var(--accent);
}

.input::placeholder {
  color: var(--text-muted);
}

.login-btn {
  margin-top: var(--sp-2);
  position: relative;
  overflow: hidden;
}

.login-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, transparent 40%, rgba(255, 255, 255, 0.15) 50%, transparent 60%);
  transform: translateX(-100%);
  transition: transform 0.6s var(--ease-out);
}

.login-btn:hover::before {
  transform: translateX(100%);
}

.spinner {
  display: inline-flex;
  width: 18px;
  height: 18px;
  border: 2px solid rgba(11, 14, 18, 0.3);
  border-top-color: var(--bg-base);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.login-error {
  text-align: center;
  font-size: var(--text-sm);
  color: var(--danger);
  padding: var(--sp-2) var(--sp-3);
  background: var(--danger-soft);
  border-radius: var(--radius-md);
  border: 1px solid rgba(248, 113, 113, 0.15);
}

.login-footer {
  margin-top: var(--sp-6);
  text-align: center;
  font-size: var(--text-xs);
  color: var(--text-muted);
}

/* ── Responsive ──────────────────────────────────────────── */
@media (max-width: 900px) {
  .login-page {
    flex-direction: column;
  }

  .login-brand {
    flex: none;
    padding: var(--sp-8) var(--sp-6) var(--sp-4);
    min-height: auto;
  }

  .brand-content {
    text-align: center;
    max-width: 100%;
  }

  .brand-features {
    display: none;
  }

  .brand-divider {
    margin: var(--sp-4) auto;
  }

  .login-form-area {
    width: 100%;
    min-width: auto;
    flex: 1;
    padding: var(--sp-6);
    background: var(--bg-base);
  }

  .form-wrapper {
    max-width: 100%;
  }
}
</style>
