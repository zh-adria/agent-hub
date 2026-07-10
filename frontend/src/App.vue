<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">AgentHub</div>
      <button
        v-for="item in navItems"
        :key="item.key"
        :class="{ active: activeView === item.key }"
        @click="activeView = item.key"
      >
        {{ item.label }}
      </button>
    </aside>
    <main class="workspace">
      <div class="workspace-inner">
        <component :is="currentComponent" />
      </div>
    </main>
  </div>
</template>

<script>
import AgentStudio from './views/agents/AgentStudio/Index.vue';
import FunctionRegistry from './views/functions/FunctionRegistry/Index.vue';
import KnowledgeBase from './views/knowledge/Index.vue';
import SessionManager from './views/sessions/Index.vue';

export default {
  components: {
    AgentStudio,
    FunctionRegistry,
    KnowledgeBase,
    SessionManager
  },
  data() {
    return {
      activeView: 'agents',
      navItems: [
        { key: 'agents', label: 'Agent 工作台' },
        { key: 'functions', label: '函数注册中心' },
        { key: 'knowledge', label: '知识库' },
        { key: 'sessions', label: '会话管理' }
      ]
    };
  },
  computed: {
    currentComponent() {
      const map = {
        agents: 'AgentStudio',
        functions: 'FunctionRegistry',
        knowledge: 'KnowledgeBase',
        sessions: 'SessionManager'
      };
      return map[this.activeView];
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

.sidebar button {
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

.sidebar button.active,
.sidebar button:hover {
  background: var(--sidebar-active);
  color: #fff;
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
