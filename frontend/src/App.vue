<template>
  <!-- Boot Screen -->
  <div v-if="loading" class="boot-screen">
    <div class="boot-logo">
      <span class="boot-mark">AH</span>
      <div class="boot-ring"></div>
    </div>
    <p class="boot-text">Initializing AgentHub...</p>
  </div>

  <!-- Login -->
  <Login v-else-if="!currentUser" @login="handleLogin" />

  <!-- App Shell -->
  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="sidebar-inner">
        <!-- Brand -->
        <div class="sidebar-brand">
          <span class="brand-mark">AH</span>
          <div class="brand-text">
            <strong>AgentHub</strong>
            <small>Enterprise AgentOps</small>
          </div>
        </div>

        <!-- Navigation -->
        <nav class="sidebar-nav">
          <button
            v-for="item in visibleNavItems"
            :key="item.key"
            :class="['nav-item', { active: activeView === item.key }]"
            @click="activeView = item.key"
            :title="item.label"
          >
            <span class="nav-icon">{{ item.icon }}</span>
            <span class="nav-label">{{ item.label }}</span>
            <span v-if="item.badge" class="nav-badge">{{ item.badge }}</span>
          </button>
        </nav>

        <!-- Bottom Section -->
        <div class="sidebar-bottom">
          <!-- User -->
          <div class="user-card">
            <div class="user-avatar">
              {{ userInitial }}
            </div>
            <div class="user-info">
              <strong class="user-name">{{ currentUser.displayName || currentUser.username }}</strong>
              <span class="user-tenant">{{ currentUser.tenantId }}</span>
            </div>
          </div>

          <!-- Logout -->
          <button class="logout-btn" @click="submitLogout" title="Sign out">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
              <polyline points="16 17 21 12 16 7" />
              <line x1="21" y1="12" x2="9" y2="12" />
            </svg>
          </button>
        </div>
      </div>

      <!-- Active Indicator Line -->
      <div class="sidebar-indicator" :style="indicatorStyle"></div>
    </aside>

    <!-- Main Content -->
    <main class="main-content">
      <div class="main-scroll">
        <component :is="currentComponent" :user="currentUser" />
      </div>
    </main>
  </div>
</template>

<script>
import Login from './views/Login.vue';
import AgentStudio from './views/agents/AgentStudio/Index.vue';
import FunctionRegistry from './views/functions/FunctionRegistry/Index.vue';
import KnowledgeBase from './views/knowledge/Index.vue';
import SessionManager from './views/sessions/Index.vue';
import AdminConsole from './views/admin/Index.vue';
import {
  clearAuthSession,
  getAccessToken,
  hasAnyPermission,
  loadCurrentUser,
  login,
  logout
} from './api';

export default {
  components: {
    Login,
    AgentStudio,
    FunctionRegistry,
    KnowledgeBase,
    SessionManager,
    AdminConsole
  },
  data() {
    return {
      loading: true,
      currentUser: null,
      activeView: 'agents',
      navItems: [
        { key: 'agents', label: 'Agent Studio', icon: '&#9733;', permissions: ['agent:read'] },
        { key: 'functions', label: 'Functions', icon: '&#9830;', permissions: ['function:read'] },
        { key: 'knowledge', label: 'Knowledge', icon: '&#9670;', permissions: ['knowledge:read', 'knowledge:search'] },
        { key: 'sessions', label: 'Sessions', icon: '&#9679;', permissions: ['session:read'] },
        {
          key: 'admin',
          label: 'Admin',
          icon: '&#9881;',
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
    },
    userInitial() {
      const name = this.currentUser?.displayName || this.currentUser?.username || '?';
      return name.charAt(0).toUpperCase();
    },
    indicatorStyle() {
      const idx = this.visibleNavItems.findIndex(item => item.key === this.activeView);
      if (idx < 0) return { opacity: '0' };
      return {
        transform: `translateY(${idx * 44}px)`
      };
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
    handleLogin(user) {
      this.currentUser = user;
    },
    async submitLogout() {
      await logout();
      this.currentUser = null;
      this.activeView = 'agents';
    }
  }
};
</script>

<style scoped>
/* ── Boot Screen ─────────────────────────────────────────── */
.boot-screen {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  gap: var(--sp-6);
  background: var(--bg-base);
}

.boot-logo {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.boot-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, var(--accent), var(--accent-strong));
  color: var(--bg-base);
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 800;
  position: relative;
  z-index: 1;
}

.boot-ring {
  position: absolute;
  inset: -6px;
  border-radius: var(--radius-xl);
  border: 2px solid var(--accent);
  opacity: 0.3;
  animation: bootPulse 1.5s ease-in-out infinite;
}

@keyframes bootPulse {
  0%, 100% { transform: scale(1); opacity: 0.3; }
  50% { transform: scale(1.15); opacity: 0.1; }
}

.boot-text {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  animation: pulseSoft 1.5s ease-in-out infinite;
}

@keyframes pulseSoft {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

/* ── App Shell ───────────────────────────────────────────── */
.app-shell {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* ── Sidebar ─────────────────────────────────────────────── */
.sidebar {
  position: relative;
  width: var(--sidebar-width);
  height: 100vh;
  display: flex;
  flex-shrink: 0;
  background: var(--bg-raised);
  border-right: 1px solid var(--border-base);
  overflow: hidden;
}

.sidebar-inner {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  padding: var(--sp-4);
}

.sidebar-indicator {
  position: absolute;
  left: 0;
  top: 68px;
  width: 3px;
  height: 36px;
  background: var(--accent);
  border-radius: 0 var(--radius-full) var(--radius-full) 0;
  transition: transform 0.3s var(--ease-out);
  box-shadow: 0 0 12px var(--accent-ring);
}

/* Brand */
.sidebar-brand {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  padding: var(--sp-2) var(--sp-2) var(--sp-4);
  margin-bottom: var(--sp-2);
}

.brand-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--accent), var(--accent-strong));
  color: var(--bg-base);
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 800;
  flex-shrink: 0;
}

.brand-text strong {
  display: block;
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--text-primary);
  line-height: 1.2;
}

.brand-text small {
  display: block;
  font-size: 10px;
  color: var(--text-muted);
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

/* Navigation */
.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  width: 100%;
  min-height: 40px;
  padding: 0 var(--sp-3);
  border: none;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--text-tertiary);
  font-family: var(--font-sans);
  font-size: var(--text-sm);
  font-weight: 500;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
  text-align: left;
  position: relative;
}

.nav-item:hover {
  background: var(--bg-hover);
  color: var(--text-secondary);
}

.nav-item.active {
  background: var(--accent-soft);
  color: var(--accent);
  font-weight: 600;
}

.nav-item.active .nav-icon {
  color: var(--accent);
}

.nav-icon {
  font-size: 16px;
  width: 20px;
  text-align: center;
  flex-shrink: 0;
  opacity: 0.7;
  transition: opacity var(--duration-fast);
}

.nav-item.active .nav-icon {
  opacity: 1;
}

.nav-label {
  flex: 1;
}

.nav-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 18px;
  padding: 0 6px;
  border-radius: var(--radius-full);
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 10px;
  font-weight: 700;
}

/* Bottom */
.sidebar-bottom {
  margin-top: auto;
  padding-top: var(--sp-3);
  border-top: 1px solid var(--border-base);
  display: flex;
  align-items: center;
  gap: var(--sp-2);
}

.user-card {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  flex: 1;
  min-width: 0;
  padding: var(--sp-1-5) var(--sp-2);
  border-radius: var(--radius-md);
  transition: background var(--duration-fast);
}

.user-card:hover {
  background: var(--bg-hover);
}

.user-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--accent-strong), var(--accent));
  color: var(--bg-base);
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}

.user-info {
  min-width: 0;
  flex: 1;
}

.user-name {
  display: block;
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-tenant {
  display: block;
  font-size: 11px;
  color: var(--text-muted);
  line-height: 1.3;
}

.logout-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--text-tertiary);
  cursor: pointer;
  transition: all var(--duration-fast);
  flex-shrink: 0;
}

.logout-btn:hover {
  background: var(--danger-soft);
  color: var(--danger);
}

/* ── Main Content ────────────────────────────────────────── */
.main-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: var(--bg-base);
  position: relative;
}

.main-scroll {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}

/* ── Responsive ──────────────────────────────────────────── */
@media (max-width: 860px) {
  .sidebar {
    width: var(--sidebar-collapsed);
  }

  .sidebar-inner {
    padding: var(--sp-3) var(--sp-2);
    align-items: center;
  }

  .sidebar-brand {
    justify-content: center;
    padding: var(--sp-1) var(--sp-1) var(--sp-3);
  }

  .brand-text {
    display: none;
  }

  .nav-label {
    display: none;
  }

  .nav-item {
    justify-content: center;
    padding: 0;
    min-height: 44px;
  }

  .nav-icon {
    font-size: 18px;
    width: auto;
  }

  .nav-badge {
    position: absolute;
    top: 4px;
    right: 4px;
    min-width: 16px;
    height: 14px;
    font-size: 9px;
  }

  .user-info {
    display: none;
  }

  .logout-btn {
    width: 38px;
    height: 38px;
  }

  .sidebar-bottom {
    justify-content: center;
  }

  .sidebar-indicator {
    display: none;
  }
}
</style>
