<template>
  <div class="agent-studio">
    <div class="page-header">
      <h2>Agent 工作台</h2>
      <button type="button" @click="resetForm">新建 Agent</button>
    </div>
    <div class="agent-grid">
      <section>
        <div v-if="agents.length === 0" class="empty">暂无 Agent</div>
        <div v-for="item in agents" :key="item.id" class="agent-card">
          <div>
            <h3>{{ item.name }}</h3>
            <p>{{ item.description || '暂无描述' }}</p>
            <span>{{ item.model }}</span>
          </div>
          <div class="actions">
            <button type="button" @click="editAgent(item)">编辑</button>
            <button type="button" class="danger" @click="deleteAgent(item)">删除</button>
          </div>
        </div>
      </section>

      <section>
        <h3>{{ agent.id ? '编辑 Agent' : '创建 Agent' }}</h3>
    <form @submit.prevent="saveAgent">
      <div class="form-group">
        <label>Agent 名称</label>
        <input v-model="agent.name" type="text" required />
      </div>
      <div class="form-group">
        <label>描述</label>
        <textarea v-model="agent.description"></textarea>
      </div>
      <div class="form-group">
        <label>系统提示词</label>
        <textarea v-model="agent.prompt" required></textarea>
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>模型</label>
          <input v-model="agent.model" type="text" required />
        </div>
        <div class="form-group">
          <label>温度</label>
          <input v-model.number="agent.temperature" type="number" min="0" max="2" step="0.1" />
        </div>
      </div>
      <div class="form-group">
        <label>可用函数</label>
        <select multiple v-model="agent.functionIds">
          <option v-for="func in availableFunctions" :key="func.id" :value="func.id">
            {{ func.name }}
          </option>
        </select>
      </div>
      <button type="submit">{{ agent.id ? '更新 Agent' : '保存 Agent' }}</button>
    </form>
      </section>
    </div>
  </div>
</template>

<script>
import { apiFetch } from '../../../api';

export default {
  data() {
    return {
      agents: [],
      agent: {
        id: null,
        name: '',
        description: '',
        prompt: '',
        model: 'gpt-4o-mini',
        temperature: 0.7,
        functionIds: []
      },
      availableFunctions: []
    };
  },
  async created() {
    await Promise.all([this.loadAgents(), this.loadFunctions()]);
  },
  methods: {
    emptyAgent() {
      return {
        id: null,
        name: '',
        description: '',
        prompt: '',
        model: 'gpt-4o-mini',
        temperature: 0.7,
        functionIds: []
      };
    },
    async loadAgents() {
      const response = await apiFetch('/api/agents');
      this.agents = response.ok ? await response.json() : [];
    },
    async loadFunctions() {
      const response = await apiFetch('/api/functions');
      this.availableFunctions = response.ok ? await response.json() : [];
    },
    async saveAgent() {
      const url = this.agent.id ? `/api/agents/${this.agent.id}` : '/api/agents';
      const response = await apiFetch(url, {
        method: this.agent.id ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(this.agent)
      });
      if (!response.ok) {
        alert('Agent 保存失败');
        return;
      }
      this.resetForm();
      await this.loadAgents();
    },
    editAgent(agent) {
      this.agent = {
        ...this.emptyAgent(),
        ...agent,
        functionIds: agent.functionIds || []
      };
    },
    async deleteAgent(agent) {
      const response = await apiFetch(`/api/agents/${agent.id}`, { method: 'DELETE' });
      if (!response.ok) {
        alert('Agent 删除失败');
        return;
      }
      this.resetForm();
      await this.loadAgents();
    },
    resetForm() {
      this.agent = this.emptyAgent();
    }
  }
};
</script>

<style scoped>
.agent-studio {
  padding: 24px;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}
.page-header h2 {
  margin: 0;
  font-size: 22px;
}
.agent-grid {
  display: grid;
  grid-template-columns: minmax(300px, 420px) minmax(420px, 1fr);
  gap: 18px;
}
.agent-card {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
  margin-bottom: 10px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--surface);
}
.agent-card h3 {
  margin: 0 0 6px;
  font-size: 15px;
}
.agent-card p {
  margin: 0 0 6px;
  color: var(--text-muted);
}
.agent-card span {
  font-size: 12px;
  color: var(--text-muted);
}
.actions {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}
.empty {
  color: var(--text-muted);
  padding: 16px;
  border: 1px dashed var(--border-strong);
  border-radius: 6px;
  background: var(--surface-muted);
}
.form-group {
  margin-bottom: 15px;
}
.form-row {
  display: grid;
  grid-template-columns: 1fr 160px;
  gap: 12px;
}
label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
}
input, textarea, select {
  width: 100%;
  min-height: 36px;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: 5px;
  background: var(--surface);
  box-sizing: border-box;
}
textarea,
select[multiple] {
  min-height: 96px;
}
button {
  min-height: 36px;
  padding: 8px 14px;
  border-radius: 5px;
  background: var(--primary);
  color: white;
  border: none;
  cursor: pointer;
}
button:hover {
  background: var(--primary-strong);
}
button.danger {
  background: var(--danger);
}
@media (max-width: 900px) {
  .agent-grid,
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
