<template>
  <div class="admin-console">
    <div class="page-header">
      <div>
        <h2>管理控制台</h2>
        <p>查看运行状态、链路追踪、工作流、评估、企业通道和用量审计。</p>
      </div>
      <div class="header-actions">
        <button type="button" class="secondary" @click="seedDemoData" :disabled="seedBusy">
          {{ seedBusy ? '生成中...' : '生成演示数据' }}
        </button>
        <button type="button" @click="refreshAll">刷新</button>
      </div>
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
      <section class="metric">
        <span>交付就绪</span>
        <strong>{{ readiness.readinessScore ?? '-' }}%</strong>
        <small>{{ readiness.readyCount || 0 }}/{{ readiness.totalCount || 0 }} checks</small>
      </section>
      <section class="metric">
        <span>生产化缺口</span>
        <strong :class="productionReadiness.productionReady ? 'ok' : 'bad'">
          {{ productionReadiness.blockingGapCount ?? '-' }}
        </strong>
        <small>{{ productionReadiness.mvpReady ? 'MVP ready' : 'MVP pending' }}</small>
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

    <section v-if="activeTab === 'readiness'" class="section">
      <div class="section-grid">
        <div>
          <h3>Dify 替代交付就绪度</h3>
          <table v-if="readiness.checks && readiness.checks.length > 0">
            <thead>
              <tr>
                <th>产品域</th>
                <th>状态</th>
                <th>数量</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in readiness.checks" :key="item.domain">
                <td>{{ item.domain }}</td>
                <td>
                  <span :class="['pill', item.ready ? 'ready' : 'pending']">
                    {{ item.ready ? 'ready' : 'pending' }}
                  </span>
                </td>
                <td>{{ item.count }}</td>
              </tr>
            </tbody>
          </table>
          <div v-else class="empty">暂无交付就绪数据</div>
        </div>
        <div>
          <h3>下一步</h3>
          <div
            v-for="action in readiness.nextActions || []"
            :key="action"
            class="readiness-action"
          >
            {{ action }}
          </div>
          <div v-if="!readiness.nextActions || readiness.nextActions.length === 0" class="empty">
            暂无待办
          </div>
          <div v-if="seedMessage" class="notice">{{ seedMessage }}</div>
        </div>
      </div>
    </section>

    <section v-if="activeTab === 'production'" class="section">
      <div class="section-grid">
        <div>
          <h3>生产化缺口</h3>
          <table v-if="productionReadiness.gaps && productionReadiness.gaps.length > 0">
            <thead>
              <tr>
                <th>优先级</th>
                <th>产品域</th>
                <th>状态</th>
                <th>任务</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="gap in productionReadiness.gaps" :key="gap.domain + gap.task">
                <td>{{ gap.priority }}</td>
                <td>{{ gap.domain }}</td>
                <td>
                  <span :class="['pill', gap.status === 'DONE' ? 'ready' : 'pending']">
                    {{ gap.status }}
                  </span>
                </td>
                <td>{{ gap.task }}</td>
              </tr>
            </tbody>
          </table>
          <div v-else class="empty">暂无生产化缺口数据</div>
        </div>
        <div>
          <h3>验收口径</h3>
          <div
            v-for="gap in productionReadiness.gaps || []"
            :key="gap.task"
            class="readiness-action"
          >
            <strong>{{ gap.domain }}</strong>
            <span>{{ gap.acceptance }}</span>
          </div>
          <h3>下一步</h3>
          <div
            v-for="action in productionReadiness.nextActions || []"
            :key="action"
            class="readiness-action"
          >
            {{ action }}
          </div>
        </div>
      </div>
    </section>

    <section v-if="activeTab === 'evidence'" class="section">
      <div class="section-grid">
        <div>
          <h3>交付验收证据包</h3>
          <table>
            <tbody>
              <tr>
                <th>租户</th>
                <td>{{ deliveryEvidence.tenantId || '-' }}</td>
              </tr>
              <tr>
                <th>生成时间</th>
                <td>{{ deliveryEvidence.generatedAt || '-' }}</td>
              </tr>
              <tr>
                <th>范围</th>
                <td>{{ deliveryEvidence.scope || '-' }}</td>
              </tr>
              <tr>
                <th>交付就绪</th>
                <td>
                  {{ deliveryEvidence.deliveryReadiness?.readyCount ?? 0 }}/{{
                    deliveryEvidence.deliveryReadiness?.totalCount ?? 0
                  }}
                </td>
              </tr>
              <tr>
                <th>生产就绪</th>
                <td>{{ deliveryEvidence.productionReadiness?.productionReady ? 'ready' : 'pending' }}</td>
              </tr>
            </tbody>
          </table>
          <div v-if="deliveryEvidence.exportHint" class="notice">{{ deliveryEvidence.exportHint }}</div>
          <div class="evidence-actions">
            <button type="button" class="evidence-download" @click="downloadDeliveryEvidence('json')">
              下载 JSON
            </button>
            <button type="button" class="evidence-download secondary" @click="downloadDeliveryEvidence('zip')">
              导出归档 (ZIP)
            </button>
          </div>
        </div>
        <div>
          <h3>证据端点</h3>
          <table v-if="deliveryEvidence.evidence && deliveryEvidence.evidence.length > 0">
            <thead>
              <tr>
                <th>名称</th>
                <th>端点</th>
                <th>用途</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in deliveryEvidence.evidence" :key="item.endpoint">
                <td>{{ item.name }}</td>
                <td class="clip">{{ item.endpoint }}</td>
                <td>{{ item.purpose }}</td>
              </tr>
            </tbody>
          </table>
          <div v-else class="empty">暂无证据包数据</div>
        </div>
      </div>
    </section>

    <section v-if="activeTab === 'dify-import'" class="section">
      <h3>Dify 导出物导入</h3>
      <p class="hint">粘贴 Dify 导出 JSON，点击"前置检查"验证兼容性，确认后执行导入。</p>
      <form class="settings-form" @submit.prevent="runPreflight">
        <label>Dify 导出 JSON</label>
        <textarea v-model="difyPayload" rows="12" placeholder='{"apps":[],"workflows":[],"tools":[],"knowledgeBases":[]}'></textarea>
        <div class="dify-actions">
          <button type="button" class="secondary" @click="runPreflight" :disabled="!difyPayload">前置检查</button>
          <button type="button" @click="runDifyImport" :disabled="!difyPayload">执行导入</button>
        </div>
        <div v-if="preflightError" class="notice bad">{{ preflightError }}</div>
        <div v-if="preflightReport" class="preflight-result">
          <h4>检查结果</h4>
          <div class="preflight-summary">
            <section class="mini-metric">
              <span>状态</span>
              <strong :class="preflightReport.ready ? 'ok' : 'bad'">
                {{ preflightReport.ready ? '可导入' : '有阻塞' }}
              </strong>
            </section>
            <section class="mini-metric">
              <span>Apps</span>
              <strong>{{ preflightReport.summary?.apps ?? 0 }}</strong>
            </section>
            <section class="mini-metric">
              <span>Workflows</span>
              <strong>{{ preflightReport.summary?.workflows ?? 0 }}</strong>
            </section>
            <section class="mini-metric">
              <span>Tools</span>
              <strong>{{ preflightReport.summary?.tools ?? 0 }}</strong>
            </section>
            <section class="mini-metric">
              <span>Knowledge Bases</span>
              <strong>{{ preflightReport.summary?.knowledgeBases ?? 0 }}</strong>
            </section>
            <section class="mini-metric">
              <span>Documents</span>
              <strong>{{ preflightReport.summary?.documents ?? 0 }}</strong>
            </section>
          </div>
          <div v-if="preflightReport.blockers && preflightReport.blockers.length > 0" class="alert-list">
            <h4>阻塞项</h4>
            <div v-for="(blocker, index) in preflightReport.blockers" :key="index" class="alert-item bad">
              {{ blocker }}
            </div>
          </div>
          <div v-if="preflightReport.warnings && preflightReport.warnings.length > 0" class="alert-list">
            <h4>警告</h4>
            <div v-for="(warning, index) in preflightReport.warnings" :key="index" class="alert-item warn">
              {{ warning }}
            </div>
          </div>
          <div v-if="preflightReport.mappings && preflightReport.mappings.length > 0" class="alert-list">
            <h4>映射预览</h4>
            <table class="compact">
              <thead>
                <tr><th>源名称</th><th>目标类型</th><th>状态</th></tr>
              </thead>
              <tbody>
                <tr v-for="item in preflightReport.mappings" :key="item.sourceName + item.targetType">
                  <td>{{ item.sourceName }}</td>
                  <td>{{ item.targetType }}</td>
                  <td><span :class="['pill', statusPillClass(item.status)]">{{ item.status }}</span></td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-if="preflightReport.risks && preflightReport.risks.length > 0" class="alert-list">
            <h4>风险项</h4>
            <div v-for="(risk, index) in preflightReport.risks" :key="'risk-' + index" class="alert-item" :class="risk.level === 'HIGH' ? 'bad' : 'warn'">
              <strong>[{{ risk.level }}] {{ risk.target }}:</strong> {{ risk.description }}
            </div>
          </div>
          <div v-if="preflightReport.compatibilityIssues && preflightReport.compatibilityIssues.length > 0" class="alert-list">
            <h4>兼容性问题</h4>
            <div v-for="(issue, index) in preflightReport.compatibilityIssues" :key="'compat-' + index" class="alert-item warn">
              <strong>{{ issue.target }}:</strong> {{ issue.issue }}
            </div>
          </div>
        </div>
        <div v-if="migrationError" class="notice bad">{{ migrationError }}</div>
      </form>
    </section>

    <section v-if="activeTab === 'dify-results'" class="section">
      <div class="section-header">
        <h3>Dify 导入结果</h3>
        <button type="button" class="secondary" @click="loadDifyResults">刷新</button>
      </div>
      <div v-if="importResults.length === 0" class="empty">暂无导入记录，请先在"Dify 导入"页签执行导入。</div>
      <div v-else class="results-summary">
        <section class="mini-metric">
          <span>总数</span>
          <strong>{{ importResults.length }}</strong>
        </section>
        <section class="mini-metric">
          <span>成功</span>
          <strong class="ok">{{ importResults.filter(r => r.status === 'SUCCEEDED').length }}</strong>
        </section>
        <section class="mini-metric">
          <span>失败</span>
          <strong class="bad">{{ importResults.filter(r => r.status === 'FAILED').length }}</strong>
        </section>
      </div>
      <table v-if="importResults.length > 0">
        <thead>
          <tr>
            <th>源名称</th>
            <th>目标类型</th>
            <th>源类型</th>
            <th>状态</th>
            <th>错误</th>
            <th>时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in importResults" :key="item.id">
            <td>{{ item.sourceName }}</td>
            <td>{{ item.targetType }}</td>
            <td>{{ item.sourceType }}</td>
            <td><span :class="['pill', statusPillClass(item.status)]">{{ item.status }}</span></td>
            <td class="clip">{{ item.errorMessage || '-' }}</td>
            <td>{{ item.createdAt }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section v-if="activeTab === 'delivery-templates'" class="section">
      <h3>客户交付模板包</h3>
      <p class="hint">选择版本和格式，下载对应交付模板。ZIP 格式包含部署清单、验收标准和运维手册。</p>
      <div v-if="templateError" class="notice bad">{{ templateError }}</div>
      <div class="template-grid">
        <div v-for="tpl in deliveryTemplates" :key="tpl.id" class="template-card">
          <h4>{{ tpl.label }}</h4>
          <p>{{ tpl.description }}</p>
          <div class="template-actions">
            <button type="button" class="secondary" @click="downloadTemplate(tpl.id, 'json')">JSON</button>
            <button type="button" @click="downloadTemplate(tpl.id, 'zip')">ZIP 归档</button>
          </div>
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
      <form class="audit-filter" @submit.prevent="loadAudit">
        <label>
          <span>Agent</span>
          <input v-model="auditFilters.agentId" placeholder="agentId" />
        </label>
        <label>
          <span>Session</span>
          <input v-model="auditFilters.agentSessionId" placeholder="agentSessionId" />
        </label>
        <label>
          <span>Trace</span>
          <input v-model="auditFilters.traceId" placeholder="traceId" />
        </label>
        <label>
          <span>User</span>
          <input v-model="auditFilters.userId" placeholder="userId" />
        </label>
        <div class="filter-actions">
          <button type="submit">应用过滤</button>
          <button type="button" class="secondary" @click="clearAuditFilters">清空</button>
        </div>
      </form>
      <div class="audit-summary">
        <section class="mini-metric">
          <span>记录数</span>
          <strong>{{ llmAuditSummary.recordCount ?? llmAudit.length }}</strong>
        </section>
        <section class="mini-metric">
          <span>Prompt Token</span>
          <strong>{{ llmAuditSummary.promptTokens || 0 }}</strong>
        </section>
        <section class="mini-metric">
          <span>Completion Token</span>
          <strong>{{ llmAuditSummary.completionTokens || 0 }}</strong>
        </section>
        <section class="mini-metric">
          <span>总成本</span>
          <strong>{{ llmAuditSummary.totalCost || 0 }}</strong>
        </section>
      </div>
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
import { apiFetch, hasPermission, formatScore } from '../../api';

export default {
  props: {
    user: { type: Object, default: null }
  },
  data() {
    return {
      activeTab: 'overview',
      tabs: [
        { key: 'overview', label: '概览', permissions: [] },
        { key: 'readiness', label: '交付就绪', permissions: ['audit:read'] },
        { key: 'production', label: '生产化', permissions: ['audit:read'] },
        { key: 'evidence', label: '证据包', permissions: ['audit:read'] },
        { key: 'dify-import', label: 'Dify 导入', permissions: ['migration:write'] },
        { key: 'dify-results', label: '导入结果', permissions: ['migration:read'] },
        { key: 'preflight', label: '迁移检查', permissions: ['migration:write'] },
        { key: 'delivery-templates', label: '交付模板', permissions: ['audit:read'] },
        { key: 'traces', label: '链路追踪', permissions: ['trace:read'] },
        { key: 'workflows', label: '工作流', permissions: ['workflow:read'] },
        { key: 'evaluations', label: '评估', permissions: ['evaluation:read'] },
        { key: 'bots', label: '企业通道', permissions: ['bot:read'] },
        { key: 'audit', label: '用量审计', permissions: ['audit:read'] }
      ],
      health: {},
      ready: {},
      summary: {},
      readiness: {},
      productionReadiness: {},
      deliveryEvidence: {},
      seedBusy: false,
      seedMessage: '',
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
      llmAudit: [],
      importResults: [],
      migrationError: '',
      deliveryTemplates: [],
      templateError: '',
      selectedTemplate: 'standard',
      templateFormat: 'json'
      preflightReport: null,
      preflightError: ''
        agentId: '',
        agentSessionId: '',
        traceId: '',
        userId: ''
      }
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
        this.can('audit:read') ? this.loadReadiness() : Promise.resolve(),
        this.can('audit:read') ? this.loadProductionReadiness() : Promise.resolve(),
        this.can('audit:read') ? this.loadDeliveryEvidence() : Promise.resolve(),
        this.can('migration:read') ? this.loadDifyResults() : Promise.resolve(),
        this.can('audit:read') ? this.loadDeliveryTemplates() : Promise.resolve(),
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
    async loadReadiness() {
      const response = await apiFetch('/api/observability/delivery-readiness');
      this.readiness = response.ok ? await response.json() : {};
    },
    async loadProductionReadiness() {
      const response = await apiFetch('/api/observability/production-readiness');
      this.productionReadiness = response.ok ? await response.json() : {};
    },
    async loadDeliveryEvidence() {
      const response = await apiFetch('/api/observability/delivery-evidence');
      this.deliveryEvidence = response.ok ? await response.json() : {};
    },
    downloadDeliveryEvidence(format) {
      const isZip = format === 'zip';
      const endpoint = isZip
        ? '/api/observability/delivery-evidence/export'
        : '/api/observability/delivery-evidence';
      const suffix = isZip ? 'zip' : 'json';
      const mimeType = isZip ? 'application/zip' : 'application/json';

      apiFetch(endpoint).then(async (response) => {
        if (!response.ok) {
          throw new Error(isZip ? '归档导出失败' : 'JSON 下载失败');
        }
        const blob = await response.blob();
        const generatedAt = String(this.deliveryEvidence.generatedAt || new Date().toISOString())
          .replace(/[:.]/g, '-');
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `agenthub-delivery-evidence-${generatedAt}.${suffix}`;
        document.body.appendChild(link);
        link.click();
        URL.revokeObjectURL(link.href);
        document.body.removeChild(link);
      }).catch((error) => {
        // fallback: download JSON from cached data when ZIP endpoint is unavailable
        if (!isZip) return;
        const payload = JSON.stringify(this.deliveryEvidence || {}, null, 2);
        const blob = new Blob([payload], { type: 'application/json' });
        const generatedAt = String(this.deliveryEvidence.generatedAt || new Date().toISOString())
          .replace(/[:.]/g, '-');
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `agenthub-delivery-evidence-${generatedAt}.json`;
        document.body.appendChild(link);
        link.click();
        URL.revokeObjectURL(link.href);
        document.body.removeChild(link);
      });
    },
    async seedDemoData() {
      if (this.seedBusy) return;
      this.seedBusy = true;
      this.seedMessage = '';
      try {
        const suffix = Date.now();
        const agent = await this.postJson('/api/agents', {
          name: `dify-demo-agent-${suffix}`,
          description: 'Dify 替代迁移演示 Agent',
          prompt: '你是企业知识库和工具编排助手。',
          model: 'gpt-4o-mini',
          temperature: 0.2
        });
        const func = await this.postJson('/api/functions', {
          name: `health_check_${suffix}`,
          description: '演示工具：读取 AgentHub 健康状态',
          endpoint: `${this.backendBaseUrl()}/api/health`,
          method: 'GET',
          parameters: { type: 'object' }
        });
        await this.postJson(`/api/functions/${func.id}/invoke`, { input: {} });
        const kb = await this.postJson('/api/knowledge-bases', {
          name: `dify-demo-kb-${suffix}`,
          description: 'Dify 替代演示知识库'
        });
        const doc = await this.postJson(`/api/knowledge-bases/${kb.id}/documents`, {
          title: '迁移验收说明',
          sourceUri: 'demo://dify-replacement',
          mimeType: 'text/plain'
        });
        await this.postJson(`/api/knowledge-bases/${kb.id}/documents/${doc.id}/chunks`, {
          content: 'AgentHub 交付验收关注多租户隔离、工具调用审计、RAG 检索和 Trace 追踪。',
          chunkIndex: 0
        });
        await this.postJson('/api/workflows', {
          name: `dify-demo-workflow-${suffix}`,
          description: 'Dify 替代演示工作流',
          definition: { nodes: [{ id: 'agent-summary', agentId: String(agent.id), timeoutMs: 30000 }] }
        });
        await this.postJson('/api/bots/bindings', {
          channel: 'webhook',
          channelBotId: `demo-bot-${suffix}`,
          agentId: String(agent.id),
          secret: 'demo-secret'
        });
        this.seedMessage = '演示数据已生成，交付就绪度已刷新。';
        await this.refreshAll();
        this.activeTab = 'evidence';
      } catch (error) {
        this.seedMessage = error.message || '演示数据生成失败';
      } finally {
        this.seedBusy = false;
      }
    },
    async postJson(url, payload) {
      const response = await apiFetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || `${url} failed`);
      }
      return response.json();
    },
    backendBaseUrl() {
      return import.meta.env.VITE_BACKEND_URL || 'http://127.0.0.1:8080';
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
      const query = this.auditQuery();
      const [records, summary] = await Promise.all([
        apiFetch(`/api/audit/llm-usage${query}`),
        apiFetch(`/api/audit/llm-usage/summary${query}`)
      ]);
      this.llmAudit = records.ok ? await records.json() : [];
      this.llmAuditSummary = summary.ok ? await summary.json() : {};
    },
    async loadDifyResults() {
      const response = await apiFetch('/api/migrations/dify/results');
      this.importResults = response.ok ? await response.json() : [];
    },
    async runDifyImport() {
      this.migrationError = '';
      try {
        const payload = JSON.parse(this.difyPayload);
        const response = await apiFetch('/api/migrations/dify/import', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
        if (!response.ok) {
          const error = await response.json().catch(() => ({}));
          throw new Error(error.message || 'Dify 导入失败');
        }
        const result = await response.json();
        this.migrationError = '';
        this.difyPayload = JSON.stringify({ apps: [], workflows: [], tools: [], knowledgeBases: [] }, null, 2);
        await this.loadDifyResults();
        await this.refreshAll();
        this.activeTab = 'dify-results';
      } catch (error) {
        this.migrationError = error.message || 'Dify 导入失败';
      }
    },
    async runPreflight() {
      this.preflightError = '';
      try {
        const payload = JSON.parse(this.difyPayload);
        const response = await apiFetch('/api/migrations/dify/preflight', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
        if (!response.ok) {
          const error = await response.json().catch(() => ({}));
          throw new Error(error.message || '前置检查失败');
        }
        this.preflightReport = await response.json();
        this.preflightError = '';
      } catch (error) {
        this.preflightError = error.message || '前置检查失败';
      }
    },
    statusPillClass(status) {
      if (!status) return 'pending';
      const upper = String(status).toUpperCase();
      if (upper === 'SUCCEEDED' || upper === 'READY' || upper === 'DONE') return 'ready';
      if (upper === 'FAILED' || upper === 'BLOCKED' || upper === 'PENDING') return 'pending';
      return 'pending';
    },
    async loadDeliveryTemplates() {
      const response = await apiFetch('/api/delivery/templates/list');
      this.deliveryTemplates = response.ok ? await response.json() : [];
    },
    downloadTemplate(edition, format) {
      const url = `/api/delivery/templates?edition=${edition}&format=${format}`;
      apiFetch(url).then(async (response) => {
        if (!response.ok) {
          throw new Error('模板下载失败');
        }
        const blob = await response.blob();
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `agenthub-delivery-${edition}-${new Date().toISOString().replace(/[:.]/g, '-')}.${format === 'zip' ? 'zip' : 'json'}`;
        document.body.appendChild(link);
        link.click();
        URL.revokeObjectURL(link.href);
        document.body.removeChild(link);
      }).catch((error) => {
        this.templateError = error.message || '模板下载失败';
      });
    }
    clearAuditFilters() {
      this.auditFilters = {
        agentId: '',
        agentSessionId: '',
        traceId: '',
        userId: ''
      };
      return this.loadAudit();
    },
    auditQuery() {
      const params = new URLSearchParams();
      Object.entries(this.auditFilters).forEach(([key, value]) => {
        const text = String(value || '').trim();
        if (text) {
          params.set(key, text);
        }
      });
      const query = params.toString();
      return query ? `?${query}` : '';
    },
    statusClass(status) {
      return status === 'UP' ? 'ok' : 'bad';
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
.header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}
.status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
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
button.secondary {
  border: 1px solid var(--border);
  background: var(--surface);
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
.audit-filter {
  display: grid;
  grid-template-columns: repeat(4, minmax(150px, 1fr)) auto;
  gap: 10px;
  align-items: end;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border);
}
.audit-filter label {
  display: grid;
  gap: 5px;
}
.audit-filter span {
  color: var(--text-muted);
  font-size: 12px;
}
.filter-actions {
  display: flex;
  gap: 8px;
}
.audit-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}
.mini-metric {
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--surface-muted);
}
.mini-metric span,
.mini-metric strong {
  display: block;
}
.mini-metric span {
  color: var(--text-muted);
  font-size: 12px;
}
.mini-metric strong {
  margin-top: 4px;
  font-size: 18px;
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
.pill {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}
.pill.ready {
  background: var(--success-soft);
  color: var(--success);
}
.pill.pending {
  background: var(--danger-soft);
  color: var(--danger);
}
.readiness-action {
  margin-bottom: 8px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--surface-muted);
  color: var(--text);
}
.readiness-action strong,
.readiness-action span {
  display: block;
}
.readiness-action span {
  margin-top: 4px;
  color: var(--text-muted);
}
.notice {
  margin-top: 12px;
  border-style: solid;
}
.evidence-download {
  margin-top: 12px;
}
.evidence-actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.dify-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.hint {
  color: var(--text-muted);
  font-size: 13px;
  margin-bottom: 10px;
}
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.preflight-result {
  margin-top: 12px;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--surface-muted);
}
.preflight-result h4 {
  margin: 0 0 10px;
  font-size: 13px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 1px;
}
.preflight-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(100px, 1fr));
  gap: 8px;
  margin-bottom: 12px;
}
.alert-list {
  margin-top: 10px;
}
.alert-list h4 {
  margin: 0 0 6px;
  font-size: 12px;
  color: var(--text-muted);
}
.alert-item {
  padding: 8px 10px;
  border-radius: 4px;
  margin-bottom: 4px;
  font-size: 13px;
}
.alert-item.bad {
  background: #fff1f0;
  color: #cf1322;
  border: 1px solid #ffa39e;
}
.alert-item.warn {
  background: #fffbe6;
  color: #d48806;
  border: 1px solid #ffe58f;
}
.results-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(80px, 1fr));
  gap: 8px;
  margin-bottom: 12px;
}
.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
  margin-top: 12px;
}
.template-card {
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--surface);
}
.template-card h4 {
  margin: 0 0 8px;
  font-size: 15px;
}
.template-card p {
  margin: 0 0 12px;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.5;
}
.template-actions {
  display: flex;
  gap: 8px;
}
table.compact {
  font-size: 12px;
}
table.compact th,
table.compact td {
  padding: 5px 6px;
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
  .audit-filter,
  .audit-summary,
  .section-grid,
  .split {
    grid-template-columns: 1fr;
  }
}
</style>
