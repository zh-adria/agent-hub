<template>
  <div v-if="loading" class="boot-screen">正在加载...</div>

  <div v-else-if="!currentUser" class="login-screen">
    <section class="login-panel">
      <div>
        <h1>AgentHub</h1>
        <p>使用本地演示账号进入 Agent 管理与运营控制台。</p>
      </div>
      <form @submit.prevent="submitLogin">
        <label>租户</label>
        <input v-model="loginForm.tenantCode" autocomplete="organization" required />
        <label>用户名</label>
        <input v-model="loginForm.username" autocomplete="username" required />
        <label>密码</label>
        <input v-model="loginForm.password" type="password" autocomplete="current-password" required />
        <button type="submit" :disabled="loginBusy">{{ loginBusy ? '登录中...' : '登录' }}</button>
        <p v-if="loginError" class="error">{{ loginError }}</p>
      </form>
    </section>
  </div>

  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="brand">AgentHub</div>
      <button
        v-for="item in visibleNavItems"
        :key="item.key"
        :class="{ active: activeView === item.key }"
        @click="activeView = item.key"
      >
        {{ item.label }}
      </button>
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
  --bg: #f4f6f8;
  --surface: #ffffff;
  --surface-muted: #f8fafc;
  --border: #d9e0e8;
  --border-strong: #b8c4d2;
  --text: #17202a;
  --text-muted: #667085;
  --primary: #176b87;
  --primary-strong: #0f5369;
  --danger: #b42318;
  --success: #1f7a4d;
  --sidebar: #17202a;
  --sidebar-active: #263445;
}

body {
  margin: 0;
  font-family: "Microsoft YaHei", "PingFang SC", "Segoe UI", Arial, sans-serif;
  color: var(--text);
  background: var(--bg);
  font-size: 14px;
}

.boot-screen,
.login-screen {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: var(--bg);
}

.login-panel {
  width: min(440px, 100%);
  padding: 28px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface);
  box-shadow: 0 18px 44px rgba(23, 32, 42, 0.12);
}

.login-panel h1,
.login-panel p {
  margin: 0;
}

.login-panel p {
  margin-top: 8px;
  color: var(--text-muted);
}

.login-panel form {
  display: grid;
  gap: 9px;
  margin-top: 22px;
}

.login-panel label {
  font-weight: 700;
}

.login-panel input,
input,
textarea,
select {
  font: inherit;
}

.login-panel input {
  width: 100%;
  min-height: 38px;
  padding: 8px 10px;
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
  width: 236px;
  padding: 18px 14px;
  background: var(--sidebar);
  color: #fff;
}

.brand {
  margin-bottom: 22px;
  padding: 0 8px;
  font-size: 20px;
  font-weight: 700;
  line-height: 32px;
}

.sidebar > button {
  display: flex;
  align-items: center;
  width: 100%;
  min-height: 38px;
  margin-bottom: 6px;
  padding: 9px 10px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #dbe4ef;
  text-align: left;
  cursor: pointer;
  font-size: 14px;
}

.sidebar > button.active,
.sidebar > button:hover {
  background: var(--sidebar-active);
  color: #fff;
}

.account {
  margin-top: 28px;
  padding: 12px 8px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.14);
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

button,
input,
textarea,
select {
  font: inherit;
}

button:focus-visible,
input:focus-visible,
textarea:focus-visible,
select:focus-visible {
  outline: 2px solid rgba(23, 107, 135, 0.28);
  outline-offset: 1px;
}
</style>
