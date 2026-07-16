<template>
  <div v-if="loading" class="boot-screen">正在加载...</div>

  <div v-else-if="!currentUser" class="login-screen">
    <section class="login-panel">
      <div>
        <h1>AgentHub</h1>
        <p>使用本地演示账号进入 Agent 管理与运营控制台。</p>
      </div>
      <form @submit.prevent="submitLogin">
        <label>
          <span>租户</span>
          <input v-model="loginForm.tenantCode" autocomplete="organization" required />
        </label>
        <label>
          <span>用户名</span>
          <input v-model="loginForm.username" autocomplete="username" required />
        </label>
        <label>
          <span>密码</span>
          <input v-model="loginForm.password" type="password" autocomplete="current-password" required />
        </label>
        <button type="submit" :disabled="loginBusy">{{ loginBusy ? '登录中...' : '登录' }}</button>
        <p v-if="loginError" class="error">{{ loginError }}</p>
      </form>
    </section>
  </div>

  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark">AH</span>
        <div>
          <strong>AgentHub</strong>
          <small>Enterprise AgentOps</small>
        </div>
      </div>
      <nav class="nav-stack">
        <button
          v-for="item in visibleNavItems"
          :key="item.key"
          :class="{ active: activeView === item.key }"
          @click="activeView = item.key"
        >
          <span>{{ item.label }}</span>
        </button>
      </nav>
      <div class="account">
        <strong>{{ currentUser.displayName || currentUser.username }}</strong>
        <span>{{ currentUser.tenantId }}</span>
        <button type="button" @click="submitLogout">退出登录</button>
      </div>
    </aside>
    <main class="workspace">
      <div class="workspace-inner">
        <component :is="currentComponent" :user="currentUser" />
      </div>
    </main>
  </div>
</template>

<script>
import AgentStudio from './views/agents/AgentStudio/Index.vue';
import FunctionRegistry from './views/functions/FunctionRegistry/Index.vue';
import KnowledgeBase from './views/knowledge/Index.vue';
import SessionManager from './views/sessions/Index.vue';
import AdminConsole from './views/admin/Index.vue';
import { clearAuthSession, getAccessToken, hasAnyPermission, loadCurrentUser, login, logout } from './api';

export default {
  components: {
    AgentStudio,
    FunctionRegistry,
    KnowledgeBase,
    SessionManager,
    AdminConsole
  },
  data() {
    return {
      loading: true,
      loginBusy: false,
      loginError: '',
      currentUser: null,
      activeView: 'agents',
      loginForm: {
        tenantCode: localStorage.getItem('tenantId') || 'tenant-001',
        username: 'admin',
        password: ''
      },
      navItems: [
        { key: 'agents', label: 'Agent 工作台', permissions: ['agent:read'] },
        { key: 'functions', label: '函数注册中心', permissions: ['function:read'] },
        { key: 'knowledge', label: '知识库', permissions: ['knowledge:read', 'knowledge:search'] },
        { key: 'sessions', label: '会话管理', permissions: ['session:read'] },
        {
          key: 'admin',
          label: '管理控制台',
          permissions: ['audit:read', 'trace:read', 'workflow:read', 'evaluation:read', 'bot:read']
        }
      ]
    };
  },
  computed: {
    visibleNavItems() {
      return this.navItems.filter(item => hasAnyPermission(this.currentUser, item.permissions));
    },
    currentComponent() {
      const map = {
        agents: 'AgentStudio',
        functions: 'FunctionRegistry',
        knowledge: 'KnowledgeBase',
        sessions: 'SessionManager',
        admin: 'AdminConsole'
      };
      return map[this.activeView] || map[this.visibleNavItems[0]?.key] || 'AdminConsole';
    }
  },
  watch: {
    visibleNavItems: {
      handler(items) {
        if (items.length > 0 && !items.some(item => item.key === this.activeView)) {
          this.activeView = items[0].key;
        }
      },
      immediate: true
    }
  },
  async created() {
    if (!getAccessToken()) {
      this.loading = false;
      return;
    }
    try {
      this.currentUser = await loadCurrentUser();
    } catch (error) {
      clearAuthSession();
    } finally {
      this.loading = false;
    }
  },
  methods: {
    async submitLogin() {
      this.loginBusy = true;
      this.loginError = '';
      try {
        await login(this.loginForm);
        this.currentUser = await loadCurrentUser();
      } catch (error) {
        this.loginError = error.message || '登录失败';
      } finally {
        this.loginBusy = false;
      }
    },
    async submitLogout() {
      await logout();
      this.currentUser = null;
    }
  }
};
</script>

<style>
* {
  box-sizing: border-box;
}

:root {
  --bg: #f3f5f7;
  --surface: #ffffff;
  --surface-muted: #f8fafb;
  --border: #d8dee6;
  --border-strong: #aeb8c5;
  --text: #18202b;
  --text-muted: #687385;
  --text-soft: #8a95a5;
  --primary: #175c62;
  --primary-strong: #0d4349;
  --primary-soft: #e7f2f1;
  --danger: #b42318;
  --danger-soft: #fff1f0;
  --success: #1f7a4d;
  --success-soft: #eaf6ef;
  --sidebar: #111820;
  --sidebar-line: #26313d;
  --sidebar-active: #1f3a40;
  --shadow-sm: 0 1px 2px rgba(16, 24, 40, 0.06);
  --shadow-md: 0 16px 36px rgba(16, 24, 40, 0.12);
}

body {
  margin: 0;
  font-family: "Microsoft YaHei", "PingFang SC", "Segoe UI", Arial, sans-serif;
  color: var(--text);
  background: var(--bg);
  font-size: 14px;
  line-height: 1.45;
}

.boot-screen,
.login-screen {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.76), rgba(243, 245, 247, 0.96)), var(--bg);
}

.login-panel {
  width: min(420px, 100%);
  padding: 30px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface);
  box-shadow: var(--shadow-md);
}

.login-panel h1,
.login-panel p {
  margin: 0;
}

.login-panel h1 {
  font-size: 28px;
  line-height: 34px;
}

.login-panel p {
  margin-top: 8px;
  color: var(--text-muted);
}

.login-panel form {
  display: grid;
  gap: 12px;
  margin-top: 22px;
}

.login-panel label {
  display: grid;
  gap: 6px;
  font-weight: 700;
}

.login-panel label span {
  font-size: 13px;
}

button,
input,
textarea,
select {
  font: inherit;
}

input,
textarea,
select {
  color: var(--text);
}

.login-panel input {
  width: 100%;
  min-height: 38px;
  padding: 8px 11px;
  border: 1px solid var(--border);
  border-radius: 5px;
}

.login-panel button,
.sidebar button {
  font: inherit;
}

.login-panel button {
  min-height: 38px;
  margin-top: 8px;
  border: 0;
  border-radius: 5px;
  background: var(--primary);
  color: #fff;
  cursor: pointer;
  font-weight: 700;
}

.login-panel button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error {
  color: var(--danger);
}

.app-shell {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  position: sticky;
  top: 0;
  width: 248px;
  height: 100vh;
  padding: 18px 14px;
  background: var(--sidebar);
  color: #fff;
  display: flex;
  flex-direction: column;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  padding: 0 8px 16px;
  border-bottom: 1px solid var(--sidebar-line);
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 7px;
  background: #d9ece9;
  color: #0f3f44;
  font-size: 13px;
  font-weight: 700;
}

.brand strong,
.brand small {
  display: block;
}

.brand strong {
  font-size: 17px;
  line-height: 20px;
}

.brand small {
  margin-top: 2px;
  color: #9eabb8;
  font-size: 11px;
}

.nav-stack {
  display: grid;
  gap: 5px;
}

.nav-stack button {
  display: flex;
  align-items: center;
  width: 100%;
  min-height: 40px;
  padding: 9px 11px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: #dbe4ef;
  text-align: left;
  cursor: pointer;
  font-size: 14px;
}

.nav-stack button.active,
.nav-stack button:hover {
  background: var(--sidebar-active);
  color: #fff;
}

.account {
  margin-top: auto;
  padding: 12px 8px 0;
  border-top: 1px solid var(--sidebar-line);
}

.account strong,
.account span {
  display: block;
}

.account span {
  margin-top: 4px;
  color: #b9c6d6;
  font-size: 12px;
}

.account button {
  width: 100%;
  min-height: 34px;
  margin-top: 10px;
  border: 0;
  border-radius: 5px;
  background: #e8eef4;
  color: var(--text);
  cursor: pointer;
}

.workspace {
  flex: 1;
  min-width: 0;
  background: var(--bg);
}

.workspace-inner {
  min-height: 100vh;
}

button {
  transition: background-color 0.15s ease, border-color 0.15s ease, color 0.15s ease;
}

button:focus-visible,
input:focus-visible,
textarea:focus-visible,
select:focus-visible {
  outline: 2px solid rgba(23, 92, 98, 0.28);
  outline-offset: 1px;
}

@media (max-width: 860px) {
  .app-shell {
    flex-direction: column;
  }

  .sidebar {
    position: static;
    width: 100%;
    height: auto;
  }

  .nav-stack {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
