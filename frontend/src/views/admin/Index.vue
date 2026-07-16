<template>
  <div class="admin-console">
    <div class="page-header">
      <div>
        <h2>管理控制台</h2>
        <p>查看运行状态、链路追踪、工作流、评估、企业通道和用量审计。</p>
      </div>
      <button type="button" @click="refreshAll">刷新</button>
    </div>

    <div class="status-grid">
      <section class="metric">
        <span>服务状态</span>
        <strong :class="statusClass(health.status)">{{ health.status || '-' }}</strong>
      </section>
      <section class="metric">
        <span>就绪状态</span>
        <strong :class="statusClass(ready.status)">{{ ready.status || '-' }}</strong>
      </section>
      <section class="metric">
        <span>链路数</span>
        <strong>{{ summary.traceCount ?? '-' }}</strong>
        <small>成功 {{ summary.traceSucceededCount ?? 0 }} / 失败 {{ summary.traceFailedCount ?? 0 }}</small>
      </section>
      <section class="metric">
        <span>步骤记录</span>
        <strong>{{ summary.stepRecordCount ?? '-' }}</strong>
        <small>成功 {{ summary.stepSucceededCount ?? 0 }} / 失败 {{ summary.stepFailedCount ?? 0 }}</small>
      </section>
      <section class="metric">
        <span>用量审计</span>
        <strong>{{ summary.llmAuditRecordCount ?? '-' }}</strong>
        <small>{{ summary.llmTotalTokens || 0 }} tokens</small>
      </section>
    </div>

    <div class="tabs">
      <button
        v-for="tab in visibleTabs"
        :key="tab.key"
        type="button"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
      </button>
    </div>

    <section v-if="activeTab === 'overview'" class="section">
      <div class="section-grid">
        <div>
          <h3>集成状态</h3>
          <table>
            <tbody>
              <tr v-for="(value, key) in health.integrations || {}" :key="key">
                <th>{{ key }}</th>
                <td>{{ value }}</td>
              </tr>
              <tr>
                <th>database</th>
                <td>{{ ready.database }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div>
          <h3>当前身份</h3>
          <table>
            <tbody>
              <tr>
                <th>用户</th>
                <td>{{ user?.displayName || user?.username || '-' }}</td>
              </tr>
              <tr>
                <th>租户</th>
                <td>{{ user?.tenantId || '-' }}</td>
              </tr>
              <tr>
                <th>角色</th>
                <td>{{ joinList(user?.roles) }}</td>
              </tr>
              <tr>
                <th>权限</th>
                <td class="clip">{{ joinList(user?.permissions) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>

    <section v-if="activeTab === 'traces'" class="section">
      <div class="split">
        <div>
          <h3>链路追踪</h3>
          <button
            v-for="trace in traces"
            :key="trace.id"
            type="button"
            class="list-item"
            :class="{ active: selectedTrace && selectedTrace.id === trace.id }"
            @click="selectTrace(trace)"
          >
            <strong>{{ trace.name }}</strong>
            <span>{{ trace.status }} / {{ trace.startedAt }}</span>
          </button>
          <div v-if="traces.length === 0" class="empty">暂无链路</div>
        </div>
        <div>
          <h3>步骤记录</h3>
          <table v-if="traceSteps.length > 0">
            <thead>
              <tr>
                <th>步骤</th>
                <th>状态</th>
                <th>Agent</th>
                <th>输出</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="step in traceSteps" :key="step.id">
                <td>{{ step.stepKey }}</td>
                <td>{{ step.status }}</td>
                <td>{{ step.agentId || '-' }}</td>
                <td class="clip">{{ step.error || step.output || '-' }}</td>
              </tr>
            </tbody>
          </table>
          <div v-else class="empty">选择链路后查看步骤</div>
        </div>
      </div>
    </section>

    <section v-if="activeTab === 'workflows'" class="section">
      <div class="split">
        <div>
          <h3>工作流</h3>
          <button
            v-for="workflow in workflows"
            :key="workflow.id"
            type="button"
            class="list-item"
            :class="{ active: selectedWorkflow && selectedWorkflow.id === workflow.id }"
            @click="selectedWorkflow = workflow"
          >
            <strong>{{ workflow.name }}</strong>
            <span>{{ workflow.description || '无描述' }}</span>
          </button>
          <div v-if="workflows.length === 0" class="empty">暂无工作流</div>
        </div>
        <div>
          <h3>执行</h3>
          <form class="settings-form" @submit.prevent="executeWorkflow">
            <label>输入</label>
            <textarea v-model="workflowInput" placeholder="工作流输入"></textarea>
            <button type="submit" :disabled="!selectedWorkflow || !can('workflow:execute')">执行工作流</button>
          </form>
          <pre v-if="workflowResult">{{ workflowResult }}</pre>
        </div>
      </div>
    </section>

    <section v-if="activeTab === 'evaluations'" class="section">
      <h3>评估批次</h3>
      <table v-if="evaluations.length > 0">
        <thead>
          <tr>
            <th>ID</th>
            <th>名称</th>
            <th>Agent</th>
            <th>状态</th>
            <th>得分</th>
            <th>用例</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="run in evaluations" :key="run.id">
            <td>{{ run.id }}</td>
            <td>{{ run.name }}</td>
            <td>{{ run.agentId }}</td>
            <td>{{ run.status }}</td>
            <td>{{ formatScore(run.score) }}</td>
            <td>{{ run.passedCases }}/{{ run.totalCases }}</td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty">暂无评估批次</div>
    </section>

    <section v-if="activeTab === 'bots'" class="section">
      <div class="split">
        <div>
          <h3>企业通道绑定</h3>
          <button
            v-for="binding in botBindings"
            :key="binding.id"
            type="button"
            class="list-item"
            :class="{ active: selectedBinding && selectedBinding.id === binding.id }"
            @click="selectedBinding = binding"
          >
            <strong>{{ binding.channel }} / {{ binding.channelBotId }}</strong>
            <span>Agent {{ binding.agentId }} / {{ binding.status }}</span>
          </button>
          <div v-if="botBindings.length === 0" class="empty">暂无企业通道绑定</div>
        </div>
        <div>
          <h3>密钥轮换</h3>
          <form class="settings-form" @submit.prevent="rotateSecret">
            <label>新密钥</label>
            <input v-model="newSecret" type="password" placeholder="新的 Webhook 密钥" />
            <button type="submit" :disabled="!selectedBinding || !newSecret || !can('bot:update')">轮换密钥</button>
          </form>
          <div v-if="botMessage" class="notice">{{ botMessage }}</div>
        </div>
      </div>
    </section>

    <section v-if="activeTab === 'audit'" class="section">
      <h3>LLM 用量审计</h3>
      <table v-if="llmAudit.length > 0">
        <thead>
          <tr>
            <th>时间</th>
            <th>Agent</th>
            <th>步骤</th>
            <th>模型</th>
            <th>Token</th>
            <th>成本</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, index) in llmAudit" :key="index">
            <td>{{ item.createdAt }}</td>
            <td>{{ item.agentId || '-' }}</td>
            <td>{{ item.agentStepType || '-' }}</td>
            <td>{{ item.model || '-' }}</td>
            <td>{{ item.totalTokens || 0 }}</td>
            <td>{{ item.cost || 0 }}</td>
          </tr>
        </tbody>
      </table>
      <div v-else class="empty">暂无 LLM 用量记录</div>
    </section>
  </div>
</template>

<script>
import { apiFetch, hasPermission } from '../../api';

export default {
  props: {
    user: { type: Object, default: null }
  },
  data() {
    return {
      activeTab: 'overview',
      tabs: [
        { key: 'overview', label: '概览', permissions: [] },
        { key: 'traces', label: '链路追踪', permissions: ['trace:read'] },
        { key: 'workflows', label: '工作流', permissions: ['workflow:read'] },
        { key: 'evaluations', label: '评估', permissions: ['evaluation:read'] },
        { key: 'bots', label: '企业通道', permissions: ['bot:read'] },
        { key: 'audit', label: '用量审计', permissions: ['audit:read'] }
      ],
      health: {},
      ready: {},
      summary: {},
      traces: [],
      selectedTrace: null,
      traceSteps: [],
      workflows: [],
      selectedWorkflow: null,
      workflowInput: '',
      workflowResult: '',
      evaluations: [],
      botBindings: [],
      selectedBinding: null,
      newSecret: '',
      botMessage: '',
      llmAudit: []
    };
  },
  computed: {
    visibleTabs() {
      return this.tabs.filter(tab => tab.permissions.length === 0 || tab.permissions.some(permission => this.can(permission)));
    }
  },
  async created() {
    await this.refreshAll();
  },
  methods: {
    can(permission) {
      return hasPermission(this.user, permission);
    },
    async refreshAll() {
      await Promise.all([
        this.loadHealth(),
        this.can('audit:read') ? this.loadSummary() : Promise.resolve(),
        this.can('trace:read') ? this.loadTraces() : Promise.resolve(),
        this.can('workflow:read') ? this.loadWorkflows() : Promise.resolve(),
        this.can('evaluation:read') ? this.loadEvaluations() : Promise.resolve(),
        this.can('bot:read') ? this.loadBotBindings() : Promise.resolve(),
        this.can('audit:read') ? this.loadAudit() : Promise.resolve()
      ]);
    },
    async loadHealth() {
      const [health, ready] = await Promise.all([
        apiFetch('/api/health'),
        apiFetch('/api/health/ready')
      ]);
      this.health = health.ok ? await health.json() : {};
      this.ready = ready.ok ? await ready.json() : {};
    },
    async loadSummary() {
      const response = await apiFetch('/api/observability/summary');
      this.summary = response.ok ? await response.json() : {};
    },
    async loadTraces() {
      const response = await apiFetch('/api/traces');
      this.traces = response.ok ? await response.json() : [];
    },
    async selectTrace(trace) {
      this.selectedTrace = trace;
      const response = await apiFetch(`/api/traces/${trace.id}/steps`);
      this.traceSteps = response.ok ? await response.json() : [];
    },
    async loadWorkflows() {
      const response = await apiFetch('/api/workflows');
      this.workflows = response.ok ? await response.json() : [];
    },
    async executeWorkflow() {
      if (!this.selectedWorkflow || !this.can('workflow:execute')) return;
      const response = await apiFetch(`/api/workflows/${this.selectedWorkflow.id}/execute`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ input: this.workflowInput })
      });
      this.workflowResult = response.ok
        ? JSON.stringify(await response.json(), null, 2)
        : '工作流执行失败';
      await Promise.all([this.loadTraces(), this.loadSummary()]);
    },
    async loadEvaluations() {
      const response = await apiFetch('/api/evaluations/runs');
      this.evaluations = response.ok ? await response.json() : [];
    },
    async loadBotBindings() {
      const response = await apiFetch('/api/bots/bindings');
      this.botBindings = response.ok ? await response.json() : [];
    },
    async rotateSecret() {
      if (!this.selectedBinding || !this.newSecret || !this.can('bot:update')) return;
      const response = await apiFetch(`/api/bots/bindings/${this.selectedBinding.id}/rotate-secret`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ newSecret: this.newSecret })
      });
      this.botMessage = response.ok ? '密钥已轮换' : '密钥轮换失败';
      this.newSecret = '';
      await this.loadBotBindings();
    },
    async loadAudit() {
      const response = await apiFetch('/api/audit/llm-usage');
      this.llmAudit = response.ok ? await response.json() : [];
    },
    statusClass(status) {
      return status === 'UP' ? 'ok' : 'bad';
    },
    formatScore(score) {
      return Number(score || 0).toFixed(3);
    },
    joinList(values) {
      return values && values.length > 0 ? values.join(', ') : '-';
    }
  }
};
</script>

<style scoped>
.admin-console {
  padding: 24px;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}
.page-header h2,
.page-header p {
  margin: 0;
}
.page-header h2 {
  font-size: 22px;
}
.page-header p {
  margin-top: 5px;
  color: var(--text-muted);
}
.status-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(120px, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}
.metric {
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--surface);
  box-shadow: var(--shadow-sm);
}
.metric span,
.metric strong,
.metric small {
  display: block;
}
.metric span {
  color: var(--text-muted);
  font-size: 12px;
}
.metric strong {
  margin-top: 6px;
  font-size: 22px;
  line-height: 28px;
}
.metric small {
  margin-top: 4px;
  color: var(--text-soft);
  font-size: 12px;
}
.metric strong.ok {
  color: var(--success);
}
.metric strong.bad {
  color: var(--danger);
}
.tabs {
  display: flex;
  gap: 6px;
  margin-bottom: 16px;
  overflow-x: auto;
  padding: 4px;
  border: 1px solid var(--border);
  border-radius: 7px;
  background: var(--surface);
}
button {
  min-height: 36px;
  padding: 8px 12px;
  border: 0;
  border-radius: 5px;
  background: var(--primary);
  color: #fff;
  cursor: pointer;
  font-weight: 700;
}
button.secondary,
.tabs button {
  background: transparent;
  color: var(--text);
}
.tabs button.active,
button:hover {
  background: var(--primary-strong);
  color: #fff;
}
button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}
.section {
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--surface);
  box-shadow: var(--shadow-sm);
}
.section h3 {
  margin: 0 0 12px;
  font-size: 15px;
}
.section-grid,
.split {
  display: grid;
  grid-template-columns: minmax(280px, 420px) minmax(420px, 1fr);
  gap: 18px;
}
table {
  width: 100%;
  border-collapse: collapse;
}
th,
td {
  padding: 9px 8px;
  border-bottom: 1px solid var(--border);
  text-align: left;
  vertical-align: top;
}
th {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
}
.settings-form {
  display: grid;
  gap: 8px;
}
label {
  font-weight: 700;
}
input,
textarea {
  width: 100%;
  min-height: 36px;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: 5px;
  background: var(--surface);
}
textarea {
  min-height: 120px;
  resize: vertical;
}
.list-item {
  display: block;
  width: 100%;
  margin-bottom: 8px;
  padding: 12px;
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--text);
  text-align: left;
  border-radius: 6px;
  box-shadow: var(--shadow-sm);
}
.list-item.active {
  border-color: var(--primary);
  background: var(--primary-soft);
}
.list-item strong,
.list-item span {
  display: block;
}
.list-item span {
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 13px;
}
.empty,
.notice {
  padding: 14px;
  border: 1px dashed var(--border-strong);
  border-radius: 6px;
  background: var(--surface-muted);
  color: var(--text-muted);
}
.notice {
  margin-top: 12px;
  border-style: solid;
}
.clip {
  max-width: 420px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
pre {
  max-height: 360px;
  overflow: auto;
  padding: 12px;
  border-radius: 6px;
  background: #101820;
  color: #edf2f7;
}
@media (max-width: 1100px) {
  .status-grid,
  .section-grid,
  .split {
    grid-template-columns: 1fr;
  }
}
</style>
