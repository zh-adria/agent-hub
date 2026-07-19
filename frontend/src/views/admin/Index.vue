<template>
  <div class="admin-console">
    <div class="page-header">
      <div>
        <h1 class="page-title">Admin Console</h1>
        <p class="page-subtitle">Monitor health, traces, workflows, evaluations, and system audit.</p>
      </div>
      <div class="header-actions">
        <button class="btn btn-secondary" @click="refreshAll">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" style="display:inline;vertical-align:middle;margin-right:4px;">
            <polyline points="23 4 23 10 17 10" /><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10" />
          </svg>
          Refresh
        </button>
        <button class="btn btn-primary" @click="seedDemoData" :disabled="seedBusy">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" style="display:inline;vertical-align:middle;margin-right:4px;">
            <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83" />
          </svg>
          {{ seedBusy ? 'Generating...' : 'Seed Demo Data' }}
        </button>
      </div>
    </div>

    <!-- Metric Cards -->
    <div class="metrics-grid">
      <div class="metric-card">
        <div class="metric-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <path d="M22 12h-4l-3 9L9 3l-3 9H2" />
          </svg>
        </div>
        <div class="metric-body">
          <span class="metric-label">Service Status</span>
          <strong :class="['metric-value', statusClass(health.status)]">{{ health.status || '-' }}</strong>
        </div>
      </div>
      <div class="metric-card">
        <div class="metric-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
          </svg>
        </div>
        <div class="metric-body">
          <span class="metric-label">Readiness</span>
          <strong :class="['metric-value', statusClass(ready.status)]">{{ ready.status || '-' }}</strong>
        </div>
      </div>
      <div class="metric-card">
        <div class="metric-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
          </svg>
        </div>
        <div class="metric-body">
          <span class="metric-label">Traces</span>
          <strong class="metric-value">{{ summary.traceCount ?? '-' }}</strong>
          <span class="metric-detail">{{ summary.traceSucceededCount ?? 0 }} success / {{ summary.traceFailedCount ?? 0 }} failed</span>
        </div>
      </div>
      <div class="metric-card">
        <div class="metric-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <rect x="2" y="2" width="20" height="20" rx="2.18" ry="2.18" /><line x1="7" y1="2" x2="7" y2="22" /><line x1="17" y1="2" x2="17" y2="22" />
          </svg>
        </div>
        <div class="metric-body">
          <span class="metric-label">Step Records</span>
          <strong class="metric-value">{{ summary.stepRecordCount ?? '-' }}</strong>
          <span class="metric-detail">{{ summary.stepSucceededCount ?? 0 }} / {{ summary.stepFailedCount ?? 0 }}</span>
        </div>
      </div>
      <div class="metric-card">
        <div class="metric-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
          </svg>
        </div>
        <div class="metric-body">
          <span class="metric-label">LLM Usage</span>
          <strong class="metric-value">{{ summary.llmAuditRecordCount ?? '-' }}</strong>
          <span class="metric-detail">{{ summary.llmTotalTokens || 0 }} tokens</span>
        </div>
      </div>
      <div class="metric-card">
        <div class="metric-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" /><polyline points="22 4 12 14.01 9 11.01" />
          </svg>
        </div>
        <div class="metric-body">
          <span class="metric-label">Delivery Score</span>
          <strong class="metric-value">{{ readiness.readinessScore ?? '-' }}%</strong>
          <span class="metric-detail">{{ readiness.readyCount || 0 }}/{{ readiness.totalCount || 0 }} checks</span>
        </div>
      </div>
      <div class="metric-card">
        <div class="metric-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
          </svg>
        </div>
        <div class="metric-body">
          <span class="metric-label">Production Gaps</span>
          <strong :class="['metric-value', productionReadiness.productionReady ? 'ok' : 'bad']">
            {{ productionReadiness.blockingGapCount ?? '-' }}
          </strong>
          <span class="metric-detail">{{ productionReadiness.mvpReady ? 'MVP Ready' : 'MVP Pending' }}</span>
        </div>
      </div>
    </div>

    <!-- Tabs -->
    <div class="tab-bar-wrapper">
      <div class="tab-bar">
        <button
          v-for="tab in visibleTabs"
          :key="tab.key"
          :class="['tab-btn', { active: activeTab === tab.key }]"
          @click="activeTab = tab.key"
        >
          {{ tab.label }}
          <span v-if="tab.key === 'audit'" class="tab-count">{{ llmAudit.length }}</span>
        </button>
      </div>
    </div>

    <!-- Tab Content -->
    <div class="tab-content">
      <!-- Overview -->
      <section v-if="activeTab === 'overview'" class="content-section">
        <div class="section-grid">
          <div class="info-card">
            <h3>Integration Status</h3>
            <div class="info-rows">
              <div v-for="(value, key) in (health.integrations || {})" :key="key" class="info-row">
                <span class="info-key">{{ key }}</span>
                <span :class="['info-val', typeof value === 'boolean' ? (value ? 'ok' : 'bad') : '']">{{ value }}</span>
              </div>
              <div class="info-row">
                <span class="info-key">database</span>
                <span :class="['info-val', ready.database ? 'ok' : 'bad']">{{ ready.database }}</span>
              </div>
            </div>
          </div>
          <div class="info-card">
            <h3>Current Identity</h3>
            <div class="info-rows">
              <div class="info-row">
                <span class="info-key">User</span>
                <span class="info-val">{{ user?.displayName || user?.username || '-' }}</span>
              </div>
              <div class="info-row">
                <span class="info-key">Tenant</span>
                <span class="info-val">{{ user?.tenantId || '-' }}</span>
              </div>
              <div class="info-row">
                <span class="info-key">Roles</span>
                <span class="info-val">{{ joinList(user?.roles) }}</span>
              </div>
              <div class="info-row">
                <span class="info-key">Permissions</span>
                <span class="info-val clip">{{ joinList(user?.permissions) }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Readiness -->
      <section v-if="activeTab === 'readiness'" class="content-section">
        <div class="section-grid">
          <div class="info-card">
            <h3>Delivery Readiness</h3>
            <div v-if="readiness.checks && readiness.checks.length" class="readiness-table">
              <div v-for="item in readiness.checks" :key="item.domain" class="readiness-row">
                <span class="readiness-domain">{{ item.domain }}</span>
                <span :class="['pill', item.ready ? 'ready' : 'pending']">
                  {{ item.ready ? 'ready' : 'pending' }}
                </span>
                <span class="readiness-count">{{ item.count }}</span>
              </div>
            </div>
            <div v-else class="empty-state">
              <p class="empty-state-desc">No delivery readiness data</p>
            </div>
          </div>
          <div class="info-card">
            <h3>Next Actions</h3>
            <div v-if="readiness.nextActions?.length" class="action-list">
              <div v-for="(action, i) in readiness.nextActions" :key="i" class="action-item">
                <span class="action-num">{{ i + 1 }}</span>
                <span>{{ action }}</span>
              </div>
            </div>
            <div v-else class="empty-state">
              <p class="empty-state-desc">No pending actions</p>
            </div>
            <div v-if="seedMessage" class="notice notice-success">{{ seedMessage }}</div>
          </div>
        </div>
      </section>

      <!-- Production -->
      <section v-if="activeTab === 'production'" class="content-section">
        <div class="section-grid">
          <div class="info-card">
            <h3>Production Gaps</h3>
            <div v-if="productionReadiness.gaps?.length" class="table-wrapper">
              <table class="data-table">
                <thead>
                  <tr><th>Priority</th><th>Domain</th><th>Status</th><th>Task</th></tr>
                </thead>
                <tbody>
                  <tr v-for="gap in productionReadiness.gaps" :key="gap.domain + gap.task">
                    <td><span class="priority-badge" :class="'p-' + gap.priority?.toLowerCase()">{{ gap.priority }}</span></td>
                    <td>{{ gap.domain }}</td>
                    <td><span :class="['pill', gap.status === 'DONE' ? 'ready' : 'pending']">{{ gap.status }}</span></td>
                    <td>{{ gap.task }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-else class="empty-state">
              <p class="empty-state-desc">No production gap data</p>
            </div>
          </div>
          <div class="info-card">
            <h3>Acceptance Criteria</h3>
            <div v-if="productionReadiness.gaps?.length" class="action-list">
              <div v-for="(gap, i) in productionReadiness.gaps" :key="i" class="action-item">
                <strong>{{ gap.domain }}</strong>
                <span>{{ gap.acceptance }}</span>
              </div>
            </div>
            <h3 class="mt-4">Next Steps</h3>
            <div v-if="productionReadiness.nextActions?.length" class="action-list">
              <div v-for="(action, i) in productionReadiness.nextActions" :key="i" class="action-item">
                <span class="action-num">{{ i + 1 }}</span>
                <span>{{ action }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Evidence -->
      <section v-if="activeTab === 'evidence'" class="content-section">
        <div class="section-grid">
          <div class="info-card">
            <h3>Delivery Evidence Package</h3>
            <div class="info-rows">
              <div class="info-row"><span class="info-key">Tenant</span><span class="info-val">{{ deliveryEvidence.tenantId || '-' }}</span></div>
              <div class="info-row"><span class="info-key">Generated</span><span class="info-val">{{ deliveryEvidence.generatedAt || '-' }}</span></div>
              <div class="info-row"><span class="info-key">Scope</span><span class="info-val">{{ deliveryEvidence.scope || '-' }}</span></div>
              <div class="info-row">
                <span class="info-key">Delivery Ready</span>
                <span class="info-val">{{ deliveryEvidence.deliveryReadiness?.readyCount ?? 0 }}/{{ deliveryEvidence.deliveryReadiness?.totalCount ?? 0 }}</span>
              </div>
              <div class="info-row">
                <span class="info-key">Production Ready</span>
                <span :class="['info-val', deliveryEvidence.productionReadiness?.productionReady ? 'ok' : 'bad']">
                  {{ deliveryEvidence.productionReadiness?.productionReady ? 'Yes' : 'No' }}
                </span>
              </div>
            </div>
            <div v-if="deliveryEvidence.exportHint" class="notice notice-info mt-4">{{ deliveryEvidence.exportHint }}</div>
            <div class="btn-row mt-4">
              <button class="btn btn-primary btn-sm" @click="downloadEvidence('json')">Download JSON</button>
              <button class="btn btn-secondary btn-sm" @click="downloadEvidence('zip')">Export ZIP</button>
            </div>
          </div>
          <div class="info-card">
            <h3>Evidence Endpoints</h3>
            <div v-if="deliveryEvidence.evidence?.length" class="table-wrapper">
              <table class="data-table">
                <thead><tr><th>Name</th><th>Endpoint</th><th>Purpose</th></tr></thead>
                <tbody>
                  <tr v-for="item in deliveryEvidence.evidence" :key="item.endpoint">
                    <td>{{ item.name }}</td>
                    <td class="clip">{{ item.endpoint }}</td>
                    <td>{{ item.purpose }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-else class="empty-state">
              <p class="empty-state-desc">No evidence data</p>
            </div>
          </div>
        </div>
      </section>

      <!-- Dify Import -->
      <section v-if="activeTab === 'dify-import'" class="content-section">
        <div class="import-card">
          <h3>Dify Export Import</h3>
          <p class="section-hint">Paste a Dify export JSON, run preflight check, then import.</p>
          <form @submit.prevent="runPreflight">
            <label class="form-label">Dify Export JSON</label>
            <textarea v-model="difyPayload" class="textarea import-textarea" rows="12"
              placeholder='{"apps":[],"workflows":[],"tools":[],"knowledgeBases":[]}'></textarea>
            <div class="btn-row mt-3">
              <button type="button" class="btn btn-secondary" @click="runPreflight" :disabled="!difyPayload">
                Preflight Check
              </button>
              <button type="button" class="btn btn-primary" @click="runImport" :disabled="!difyPayload">
                Execute Import
              </button>
            </div>
          </form>
          <div v-if="preflightError" class="notice notice-danger mt-3">{{ preflightError }}</div>
          <div v-if="preflightReport" class="preflight-panel">
            <h4>Preflight Results</h4>
            <div class="preflight-summary">
              <div class="mini-metric">
                <span>Status</span>
                <strong :class="preflightReport.ready ? 'ok' : 'bad'">{{ preflightReport.ready ? 'Ready to import' : 'Blocked' }}</strong>
              </div>
              <div class="mini-metric"><span>Apps</span><strong>{{ preflightReport.summary?.apps ?? 0 }}</strong></div>
              <div class="mini-metric"><span>Workflows</span><strong>{{ preflightReport.summary?.workflows ?? 0 }}</strong></div>
              <div class="mini-metric"><span>Tools</span><strong>{{ preflightReport.summary?.tools ?? 0 }}</strong></div>
              <div class="mini-metric"><span>Knowledge Bases</span><strong>{{ preflightReport.summary?.knowledgeBases ?? 0 }}</strong></div>
              <div class="mini-metric"><span>Documents</span><strong>{{ preflightReport.summary?.documents ?? 0 }}</strong></div>
            </div>
            <div v-if="preflightReport.blockers?.length" class="alert-list">
              <h5>Blockers</h5>
              <div v-for="(b, i) in preflightReport.blockers" :key="i" class="alert-item bad">{{ b }}</div>
            </div>
            <div v-if="preflightReport.warnings?.length" class="alert-list">
              <h5>Warnings</h5>
              <div v-for="(w, i) in preflightReport.warnings" :key="i" class="alert-item warn">{{ w }}</div>
            </div>
            <div v-if="preflightReport.mappings?.length" class="alert-list">
              <h5>Mapping Preview</h5>
              <div class="table-wrapper">
                <table class="data-table compact">
                  <thead><tr><th>Source</th><th>Target</th><th>Status</th></tr></thead>
                  <tbody>
                    <tr v-for="item in preflightReport.mappings" :key="item.sourceName + item.targetType">
                      <td>{{ item.sourceName }}</td>
                      <td>{{ item.targetType }}</td>
                      <td><span :class="['pill', statusPillClass(item.status)]">{{ item.status }}</span></td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
          <div v-if="migrationError" class="notice notice-danger mt-3">{{ migrationError }}</div>
        </div>
      </section>

      <!-- Dify Results -->
      <section v-if="activeTab === 'dify-results'" class="content-section">
        <div class="info-card">
          <div class="section-header-row">
            <h3>Import Results</h3>
            <button class="btn btn-secondary btn-sm" @click="loadDifyResults">Refresh</button>
          </div>
          <div v-if="!importResults.length" class="empty-state">
            <p class="empty-state-desc">No import records. Run an import first.</p>
          </div>
          <div v-else>
            <div class="results-summary">
              <div class="mini-metric"><span>Total</span><strong>{{ importResults.length }}</strong></div>
              <div class="mini-metric"><span>Success</span><strong class="ok">{{ importResults.filter(r => r.status === 'SUCCEEDED').length }}</strong></div>
              <div class="mini-metric"><span>Failed</span><strong class="bad">{{ importResults.filter(r => r.status === 'FAILED').length }}</strong></div>
            </div>
            <div class="table-wrapper mt-3">
              <table class="data-table">
                <thead>
                  <tr><th>Source</th><th>Target</th><th>Source Type</th><th>Status</th><th>Error</th><th>Time</th></tr>
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
            </div>
          </div>
        </div>
      </section>

      <!-- Delivery Templates -->
      <section v-if="activeTab === 'delivery-templates'" class="content-section">
        <div class="info-card">
          <h3>Client Delivery Templates</h3>
          <p class="section-hint">Select version and format to download delivery templates.</p>
          <div v-if="templateError" class="notice notice-danger">{{ templateError }}</div>
          <div class="template-grid">
            <div v-for="tpl in deliveryTemplates" :key="tpl.id" class="template-card">
              <h4>{{ tpl.label }}</h4>
              <p>{{ tpl.description }}</p>
              <div class="template-actions">
                <button class="btn btn-secondary btn-sm" @click="downloadTemplate(tpl.id, 'json')">JSON</button>
                <button class="btn btn-primary btn-sm" @click="downloadTemplate(tpl.id, 'zip')">ZIP</button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Traces -->
      <section v-if="activeTab === 'traces'" class="content-section">
        <div class="split-layout">
          <div class="list-panel">
            <h3>Traces</h3>
            <div v-if="traces.length" class="list-items">
              <button
                v-for="trace in traces"
                :key="trace.id"
                :class="['list-item', { active: selectedTrace && selectedTrace.id === trace.id }]"
                @click="selectTrace(trace)"
              >
                <strong>{{ trace.name }}</strong>
                <span>{{ trace.status }} / {{ formatDate(trace.startedAt) }}</span>
              </button>
            </div>
            <div v-else class="empty-state">
              <p class="empty-state-desc">No traces</p>
            </div>
          </div>
          <div class="detail-panel">
            <h3>Step Records</h3>
            <div v-if="traceSteps.length" class="table-wrapper">
              <table class="data-table">
                <thead><tr><th>Step</th><th>Status</th><th>Agent</th><th>Output</th></tr></thead>
                <tbody>
                  <tr v-for="step in traceSteps" :key="step.id">
                    <td><code>{{ step.stepKey }}</code></td>
                    <td><span :class="['pill', step.status === 'SUCCEEDED' ? 'ready' : 'pending']">{{ step.status }}</span></td>
                    <td>{{ step.agentId || '-' }}</td>
                    <td class="clip">{{ step.error || step.output || '-' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-else class="empty-state">
              <p class="empty-state-desc">Select a trace to view steps</p>
            </div>
          </div>
        </div>
      </section>

      <!-- Workflows -->
      <section v-if="activeTab === 'workflows'" class="content-section">
        <div class="split-layout">
          <div class="list-panel">
            <h3>Workflows</h3>
            <div v-if="workflows.length" class="list-items">
              <button
                v-for="wf in workflows"
                :key="wf.id"
                :class="['list-item', { active: selectedWorkflow && selectedWorkflow.id === wf.id }]"
                @click="selectedWorkflow = wf"
              >
                <strong>{{ wf.name }}</strong>
                <span>{{ wf.description || 'No description' }}</span>
              </button>
            </div>
            <div v-else class="empty-state">
              <p class="empty-state-desc">No workflows</p>
            </div>
          </div>
          <div class="detail-panel">
            <h3>Execute</h3>
            <form @submit.prevent="executeWorkflow">
              <label class="form-label">Input (JSON)</label>
              <textarea v-model="workflowInput" class="textarea" rows="6" placeholder='{"key": "value"}'></textarea>
              <button class="btn btn-primary mt-3" type="submit" :disabled="!selectedWorkflow || !canWorkflowExecute">
                Execute Workflow
              </button>
            </form>
            <div v-if="workflowResult" class="result-block mt-3">
              <h4>Result</h4>
              <pre>{{ workflowResult }}</pre>
            </div>
          </div>
        </div>
      </section>

      <!-- Evaluations -->
      <section v-if="activeTab === 'evaluations'" class="content-section">
        <div class="info-card">
          <h3>Evaluation Runs</h3>
          <div v-if="evaluations.length" class="table-wrapper">
            <table class="data-table">
              <thead>
                <tr><th>ID</th><th>Name</th><th>Agent</th><th>Status</th><th>Score</th><th>Cases</th></tr>
              </thead>
              <tbody>
                <tr v-for="run in evaluations" :key="run.id">
                  <td><code>{{ run.id }}</code></td>
                  <td>{{ run.name }}</td>
                  <td>{{ run.agentId }}</td>
                  <td><span :class="['pill', run.status === 'SUCCEEDED' ? 'ready' : 'pending']">{{ run.status }}</span></td>
                  <td>{{ formatScore(run.score) }}</td>
                  <td>{{ run.passedCases }}/{{ run.totalCases }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="empty-state">
            <p class="empty-state-desc">No evaluation runs</p>
          </div>
        </div>
      </section>

      <!-- Bots -->
      <section v-if="activeTab === 'bots'" class="content-section">
        <div class="split-layout">
          <div class="list-panel">
            <h3>Enterprise Channel Bindings</h3>
            <div v-if="botBindings.length" class="list-items">
              <button
                v-for="binding in botBindings"
                :key="binding.id"
                :class="['list-item', { active: selectedBinding && selectedBinding.id === binding.id }]"
                @click="selectedBinding = binding"
              >
                <strong>{{ binding.channel }} / {{ binding.channelBotId }}</strong>
                <span>Agent {{ binding.agentId }} / {{ binding.status }}</span>
              </button>
            </div>
            <div v-else class="empty-state">
              <p class="empty-state-desc">No bot bindings</p>
            </div>
          </div>
          <div class="detail-panel">
            <h3>Secret Rotation</h3>
            <form @submit.prevent="rotateSecret">
              <label class="form-label">New Webhook Secret</label>
              <input v-model="newSecret" class="input" type="password" placeholder="New secret" />
              <button class="btn btn-primary mt-3" type="submit" :disabled="!selectedBinding || !newSecret || !canBotUpdate">
                Rotate Secret
              </button>
            </form>
            <div v-if="botMessage" class="notice notice-success mt-3">{{ botMessage }}</div>
          </div>
        </div>
      </section>

      <!-- Audit -->
      <section v-if="activeTab === 'audit'" class="content-section">
        <div class="info-card">
          <h3>LLM Usage Audit</h3>
          <form class="audit-filters" @submit.prevent="loadAudit">
            <div class="filter-grid">
              <div class="filter-group">
                <label class="filter-label">Agent</label>
                <input v-model="auditFilters.agentId" class="input" placeholder="Agent ID" />
              </div>
              <div class="filter-group">
                <label class="filter-label">Session</label>
                <input v-model="auditFilters.agentSessionId" class="input" placeholder="Session ID" />
              </div>
              <div class="filter-group">
                <label class="filter-label">Trace</label>
                <input v-model="auditFilters.traceId" class="input" placeholder="Trace ID" />
              </div>
              <div class="filter-group">
                <label class="filter-label">User</label>
                <input v-model="auditFilters.userId" class="input" placeholder="User ID" />
              </div>
            </div>
            <div class="filter-actions">
              <button type="submit" class="btn btn-primary btn-sm">Apply</button>
              <button type="button" class="btn btn-secondary btn-sm" @click="clearAuditFilters">Clear</button>
            </div>
          </form>
          <div class="audit-summary mt-3">
            <div class="mini-metric"><span>Records</span><strong>{{ llmAuditSummary.recordCount ?? llmAudit.length }}</strong></div>
            <div class="mini-metric"><span>Prompt Tokens</span><strong>{{ llmAuditSummary.promptTokens || 0 }}</strong></div>
            <div class="mini-metric"><span>Completion Tokens</span><strong>{{ llmAuditSummary.completionTokens || 0 }}</strong></div>
            <div class="mini-metric"><span>Total Cost</span><strong>{{ llmAuditSummary.totalCost || 0 }}</strong></div>
          </div>
          <div v-if="llmAudit.length" class="table-wrapper mt-3">
            <table class="data-table">
              <thead>
                <tr><th>Time</th><th>Agent</th><th>Step</th><th>Model</th><th>Tokens</th><th>Cost</th></tr>
              </thead>
              <tbody>
                <tr v-for="(item, i) in llmAudit" :key="i">
                  <td>{{ item.createdAt }}</td>
                  <td>{{ item.agentId || '-' }}</td>
                  <td>{{ item.agentStepType || '-' }}</td>
                  <td>{{ item.model || '-' }}</td>
                  <td>{{ item.totalTokens || 0 }}</td>
                  <td>{{ item.cost || 0 }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="empty-state">
            <p class="empty-state-desc">No LLM audit records</p>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import { apiFetch, hasPermission, formatScore, formatDate } from '../../api';

export default {
  props: {
    user: { type: Object, default: null }
  },
  data() {
    return {
      activeTab: 'overview',
      tabs: [
        { key: 'overview', label: 'Overview', permissions: [] },
        { key: 'readiness', label: 'Readiness', permissions: ['audit:read'] },
        { key: 'production', label: 'Production', permissions: ['audit:read'] },
        { key: 'evidence', label: 'Evidence', permissions: ['audit:read'] },
        { key: 'dify-import', label: 'Dify Import', permissions: ['migration:write'] },
        { key: 'dify-results', label: 'Import Results', permissions: ['migration:read'] },
        { key: 'delivery-templates', label: 'Templates', permissions: ['audit:read'] },
        { key: 'traces', label: 'Traces', permissions: ['trace:read'] },
        { key: 'workflows', label: 'Workflows', permissions: ['workflow:read'] },
        { key: 'evaluations', label: 'Evaluations', permissions: ['evaluation:read'] },
        { key: 'bots', label: 'Channels', permissions: ['bot:read'] },
        { key: 'audit', label: 'LLM Audit', permissions: ['audit:read'] }
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
      llmAuditSummary: {},
      importResults: [],
      migrationError: '',
      deliveryTemplates: [],
      templateError: '',
      auditFilters: { agentId: '', agentSessionId: '', traceId: '', userId: '' },
      preflightReport: null,
      preflightError: '',
      difyPayload: JSON.stringify({ apps: [], workflows: [], tools: [], knowledgeBases: [] }, null, 2)
    };
  },
  computed: {
    visibleTabs() {
      return this.tabs.filter(tab => tab.permissions.length === 0 || tab.permissions.some(p => this.can(p)));
    },
    canWorkflowExecute() {
      return this.can('workflow:execute');
    },
    canBotUpdate() {
      return this.can('bot:update');
    }
  },
  async created() {
    await this.refreshAll();
  },
  methods: {
    can(p) { return hasPermission(this.user, p); },
    async refreshAll() {
      await Promise.all([
        this.loadHealth(),
        this.can('audit:read') ? this.loadSummary() : 0,
        this.can('audit:read') ? this.loadReadiness() : 0,
        this.can('audit:read') ? this.loadProductionReadiness() : 0,
        this.can('audit:read') ? this.loadDeliveryEvidence() : 0,
        this.can('migration:read') ? this.loadDifyResults() : 0,
        this.can('audit:read') ? this.loadDeliveryTemplates() : 0,
        this.can('trace:read') ? this.loadTraces() : 0,
        this.can('workflow:read') ? this.loadWorkflows() : 0,
        this.can('evaluation:read') ? this.loadEvaluations() : 0,
        this.can('bot:read') ? this.loadBotBindings() : 0,
        this.can('audit:read') ? this.loadAudit() : 0
      ]);
    },
    async loadHealth() {
      const [h, r] = await Promise.all([apiFetch('/api/health'), apiFetch('/api/health/ready')]);
      this.health = h.ok ? await h.json() : {};
      this.ready = r.ok ? await r.json() : {};
    },
    async loadSummary() {
      const r = await apiFetch('/api/observability/summary');
      this.summary = r.ok ? await r.json() : {};
    },
    async loadReadiness() {
      const r = await apiFetch('/api/observability/delivery-readiness');
      this.readiness = r.ok ? await r.json() : {};
    },
    async loadProductionReadiness() {
      const r = await apiFetch('/api/observability/production-readiness');
      this.productionReadiness = r.ok ? await r.json() : {};
    },
    async loadDeliveryEvidence() {
      const r = await apiFetch('/api/observability/delivery-evidence');
      this.deliveryEvidence = r.ok ? await r.json() : {};
    },
    async loadTraces() {
      const r = await apiFetch('/api/traces');
      this.traces = r.ok ? await r.json() : [];
    },
    async selectTrace(trace) {
      this.selectedTrace = trace;
      const r = await apiFetch(`/api/traces/${trace.id}/steps`);
      this.traceSteps = r.ok ? await r.json() : [];
    },
    async loadWorkflows() {
      const r = await apiFetch('/api/workflows');
      this.workflows = r.ok ? await r.json() : [];
    },
    async executeWorkflow() {
      if (!this.selectedWorkflow || !this.canWorkflowExecute) return;
      const r = await apiFetch(`/api/workflows/${this.selectedWorkflow.id}/execute`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ input: this.workflowInput })
      });
      this.workflowResult = r.ok ? JSON.stringify(await r.json(), null, 2) : 'Failed';
      await Promise.all([this.loadTraces(), this.loadSummary()]);
    },
    async loadEvaluations() {
      const r = await apiFetch('/api/evaluations/runs');
      this.evaluations = r.ok ? await r.json() : [];
    },
    async loadBotBindings() {
      const r = await apiFetch('/api/bots/bindings');
      this.botBindings = r.ok ? await r.json() : [];
    },
    async rotateSecret() {
      if (!this.selectedBinding || !this.newSecret || !this.canBotUpdate) return;
      const r = await apiFetch(`/api/bots/bindings/${this.selectedBinding.id}/rotate-secret`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ newSecret: this.newSecret })
      });
      this.botMessage = r.ok ? 'Secret rotated' : 'Failed';
      this.newSecret = '';
      await this.loadBotBindings();
    },
    async loadAudit() {
      const q = this.auditQuery();
      const [records, summary] = await Promise.all([
        apiFetch(`/api/audit/llm-usage${q}`),
        apiFetch(`/api/audit/llm-usage/summary${q}`)
      ]);
      this.llmAudit = records.ok ? await records.json() : [];
      this.llmAuditSummary = summary.ok ? await summary.json() : {};
    },
    async loadDifyResults() {
      const r = await apiFetch('/api/migrations/dify/results');
      this.importResults = r.ok ? await r.json() : [];
    },
    async loadDeliveryTemplates() {
      const r = await apiFetch('/api/delivery/templates/list');
      this.deliveryTemplates = r.ok ? await r.json() : [];
      this.templateError = r.ok ? '' : 'Failed to load templates';
    },
    async runPreflight() {
      this.preflightError = '';
      try {
        const payload = JSON.parse(this.difyPayload);
        const r = await apiFetch('/api/migrations/dify/preflight', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
        if (!r.ok) throw new Error('Preflight failed');
        this.preflightReport = await r.json();
      } catch (e) {
        this.preflightError = e.message || 'Preflight failed';
      }
    },
    async runImport() {
      this.migrationError = '';
      try {
        const payload = JSON.parse(this.difyPayload);
        const r = await apiFetch('/api/migrations/dify/import', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
        if (!r.ok) throw new Error('Import failed');
        await this.loadDifyResults();
        this.difyPayload = JSON.stringify({ apps: [], workflows: [], tools: [], knowledgeBases: [] }, null, 2);
        this.activeTab = 'dify-results';
        await this.refreshAll();
      } catch (e) {
        this.migrationError = e.message || 'Import failed';
      }
    },
    async seedDemoData() {
      if (this.seedBusy) return;
      this.seedBusy = true;
      this.seedMessage = '';
      try {
        const base = import.meta.env.VITE_BACKEND_URL || 'http://127.0.0.1:8080';
        const headers = { 'Content-Type': 'application/json' };
        const agent = await this.postJson('/api/agents', {
          name: `demo-agent-${Date.now()}`, description: 'Demo agent', prompt: 'You are a helpful assistant.',
          model: 'gpt-4o-mini', temperature: 0.2
        });
        await this.postJson('/api/functions', {
          name: `health_check_${Date.now()}`, description: 'Health check',
          endpoint: `${base}/api/health`, method: 'GET', parameters: { type: 'object' }
        });
        await this.postJson('/api/functions/${func.id}/invoke', { input: {} });
        const kb = await this.postJson('/api/knowledge-bases', {
          name: `demo-kb-${Date.now()}`, description: 'Demo KB'
        });
        const doc = await this.postJson(`/api/knowledge-bases/${kb.id}/documents`, {
          title: 'Demo doc', sourceUri: 'demo://', mimeType: 'text/plain'
        });
        await this.postJson(`/api/knowledge-bases/${kb.id}/documents/${doc.id}/chunks`, {
          content: 'Demo chunk content.', chunkIndex: 0
        });
        await this.postJson('/api/workflows', {
          name: `demo-wf-${Date.now()}`, description: 'Demo workflow',
          definition: { nodes: [{ id: 'n1', agentId: String(agent.id) }] }
        });
        await this.postJson('/api/bots/bindings', {
          channel: 'webhook', channelBotId: `demo-bot-${Date.now()}`,
          agentId: String(agent.id), secret: 'demo-secret'
        });
        this.seedMessage = 'Demo data generated.';
        await this.refreshAll();
        this.activeTab = 'evidence';
      } catch (e) {
        this.seedMessage = e.message || 'Failed to seed';
      } finally {
        this.seedBusy = false;
      }
    },
    async postJson(url, payload) {
      const r = await apiFetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
      if (!r.ok) throw new Error(`${url} failed`);
      return r.json();
    },
    downloadEvidence(format) {
      const isZip = format === 'zip';
      const url = isZip ? '/api/observability/delivery-evidence/export' : '/api/observability/delivery-evidence';
      apiFetch(url).then(async r => {
        if (!r.ok) throw new Error('Download failed');
        const blob = await r.blob();
        const suffix = isZip ? 'zip' : 'json';
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `agenthub-evidence-${new Date().toISOString().replace(/[:.]/g, '-')}.${suffix}`;
        link.click();
        URL.revokeObjectURL(link.href);
      }).catch(e => { /* silent */ });
    },
    downloadTemplate(id, format) {
      const url = `/api/delivery/templates?edition=${id}&format=${format}`;
      apiFetch(url).then(async r => {
        if (!r.ok) throw new Error('Download failed');
        const blob = await r.blob();
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `agenthub-template-${id}-${new Date().toISOString().replace(/[:.]/g, '-')}.${format === 'zip' ? 'zip' : 'json'}`;
        link.click();
        URL.revokeObjectURL(link.href);
      }).catch(e => { this.templateError = e.message; });
    },
    auditQuery() {
      const p = new URLSearchParams();
      Object.entries(this.auditFilters).forEach(([k, v]) => { const t = String(v || '').trim(); if (t) p.set(k, t); });
      const q = p.toString();
      return q ? `?${q}` : '';
    },
    clearAuditFilters() {
      this.auditFilters = { agentId: '', agentSessionId: '', traceId: '', userId: '' };
      this.loadAudit();
    },
    statusClass(status) { return status === 'UP' ? 'ok' : 'bad'; },
    statusPillClass(s) {
      const u = String(s || '').toUpperCase();
      return ['SUCCEEDED', 'READY', 'DONE'].includes(u) ? 'ready' : 'pending';
    },
    joinList(v) { return v?.length ? v.join(', ') : '-'; },
    formatScore
  }
};
</script>

<style scoped>
.admin-console {
  padding: var(--sp-6);
  max-width: 1600px;
}

/* ── Metrics Grid ────────────────────────────────────────── */
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: var(--sp-4);
  margin-bottom: var(--sp-5);
}

.metric-card {
  display: flex;
  align-items: flex-start;
  gap: var(--sp-3);
  padding: var(--sp-4);
  background: var(--bg-raised);
  border: 1px solid var(--border-base);
  border-radius: var(--radius-lg);
  transition: border-color var(--duration-normal);
}

.metric-card:hover {
  border-color: var(--border-strong);
}

.metric-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  background: var(--accent-soft);
  color: var(--accent);
  flex-shrink: 0;
}

.metric-body {
  flex: 1;
  min-width: 0;
}

.metric-label {
  display: block;
  font-size: var(--text-xs);
  font-weight: 500;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-bottom: var(--sp-1);
}

.metric-value {
  display: block;
  font-family: var(--font-display);
  font-size: var(--text-xl);
  font-weight: 700;
  line-height: 1.2;
  color: var(--text-primary);
}

.metric-value.ok { color: var(--success); }
.metric-value.bad { color: var(--danger); }

.metric-detail {
  display: block;
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  margin-top: 2px;
}

/* ── Tabs ────────────────────────────────────────────────── */
.tab-bar-wrapper {
  margin-bottom: var(--sp-5);
  overflow-x: auto;
}

.tab-bar {
  display: flex;
  gap: 2px;
  padding: 3px;
  background: var(--bg-raised);
  border: 1px solid var(--border-base);
  border-radius: var(--radius-md);
  width: fit-content;
  min-width: 100%;
}

.tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-tertiary);
  font-family: var(--font-sans);
  font-size: var(--text-sm);
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: all var(--duration-fast) var(--ease-out);
}

.tab-btn:hover {
  color: var(--text-secondary);
  background: var(--bg-hover);
}

.tab-btn.active {
  background: var(--bg-active);
  color: var(--text-primary);
  font-weight: 600;
  box-shadow: var(--shadow-sm);
}

.tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 16px;
  padding: 0 5px;
  border-radius: var(--radius-full);
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 10px;
  font-weight: 700;
}

/* ── Tab Content ─────────────────────────────────────────── */
.tab-content {
  animation: fadeIn var(--duration-normal) var(--ease-out);
}

.content-section {
  background: var(--bg-raised);
  border: 1px solid var(--border-base);
  border-radius: var(--radius-lg);
  padding: var(--sp-5);
}

.section-grid {
  display: grid;
  grid-template-columns: minmax(300px, 1fr) minmax(300px, 1fr);
  gap: var(--sp-5);
}

.info-card h3 {
  font-family: var(--font-display);
  font-size: var(--text-md);
  font-weight: 700;
  letter-spacing: -0.01em;
  margin-bottom: var(--sp-4);
}

.info-rows {
  display: flex;
  flex-direction: column;
  gap: var(--sp-2);
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--sp-2-5) 0;
  border-bottom: 1px solid var(--border-subtle);
}

.info-row:last-child {
  border-bottom: none;
}

.info-key {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
}

.info-val {
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--text-primary);
  text-align: right;
  max-width: 60%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.info-val.ok { color: var(--success); }
.info-val.bad { color: var(--danger); }
.clip { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* ── Readiness ───────────────────────────────────────────── */
.readiness-table {
  display: flex;
  flex-direction: column;
  gap: var(--sp-2);
}

.readiness-row {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  padding: var(--sp-2-5) var(--sp-3);
  background: var(--bg-overlay);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
}

.readiness-domain {
  flex: 1;
  font-weight: 500;
}

.readiness-count {
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  color: var(--text-tertiary);
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: var(--sp-2);
}

.action-item {
  display: flex;
  align-items: flex-start;
  gap: var(--sp-3);
  padding: var(--sp-3);
  background: var(--bg-overlay);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
}

.action-item strong {
  display: block;
  font-weight: 600;
  margin-bottom: 2px;
}

.action-item span {
  color: var(--text-tertiary);
}

.action-num {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: var(--radius-full);
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 11px;
  font-weight: 700;
  flex-shrink: 0;
}

/* ── Shared Elements ─────────────────────────────────────── */
.pill {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  font-size: var(--text-xs);
  font-weight: 600;
}

.pill.ready {
  background: var(--success-soft);
  color: var(--success);
}

.pill.pending {
  background: var(--danger-soft);
  color: var(--danger);
}

.btn-row {
  display: flex;
  gap: var(--sp-3);
}

.mt-3 { margin-top: var(--sp-3); }
.mt-4 { margin-top: var(--sp-4); }

.section-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--sp-4);
}

.section-hint {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  margin-bottom: var(--sp-4);
}

.import-textarea {
  font-family: var(--font-mono);
  font-size: var(--text-sm);
}

.preflight-panel {
  margin-top: var(--sp-4);
  padding: var(--sp-4);
  background: var(--bg-overlay);
  border: 1px solid var(--border-base);
  border-radius: var(--radius-lg);
}

.preflight-panel h4 {
  font-family: var(--font-display);
  font-size: var(--text-md);
  font-weight: 700;
  margin-bottom: var(--sp-3);
}

.preflight-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(100px, 1fr));
  gap: var(--sp-2);
  margin-bottom: var(--sp-4);
}

.mini-metric {
  padding: var(--sp-2-5) var(--sp-3);
  background: var(--bg-raised);
  border: 1px solid var(--border-base);
  border-radius: var(--radius-md);
}

.mini-metric span {
  display: block;
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  margin-bottom: 2px;
}

.mini-metric strong {
  display: block;
  font-size: var(--text-lg);
  font-weight: 700;
  font-family: var(--font-display);
}

.alert-list {
  margin-top: var(--sp-3);
}

.alert-list h5 {
  font-size: var(--text-xs);
  font-weight: 700;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-bottom: var(--sp-2);
}

.alert-item {
  padding: var(--sp-2) var(--sp-3);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  margin-bottom: var(--sp-1-5);
}

.alert-item.bad {
  background: var(--danger-soft);
  color: var(--danger);
  border: 1px solid rgba(248, 113, 113, 0.15);
}

.alert-item.warn {
  background: var(--warning-soft);
  color: var(--warning);
  border: 1px solid rgba(251, 191, 36, 0.15);
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: var(--sp-4);
  margin-top: var(--sp-4);
}

.template-card {
  padding: var(--sp-4);
  background: var(--bg-overlay);
  border: 1px solid var(--border-base);
  border-radius: var(--radius-lg);
}

.template-card h4 {
  font-family: var(--font-display);
  font-size: var(--text-md);
  font-weight: 700;
  margin-bottom: var(--sp-2);
}

.template-card p {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  line-height: var(--leading-normal);
  margin-bottom: var(--sp-3);
}

.template-actions {
  display: flex;
  gap: var(--sp-2);
}

.results-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(80px, 1fr));
  gap: var(--sp-2);
  margin-bottom: var(--sp-3);
}

/* ── Split Layout ────────────────────────────────────────── */
.split-layout {
  display: grid;
  grid-template-columns: minmax(280px, 340px) 1fr;
  gap: var(--sp-5);
}

.list-panel h3,
.detail-panel h3 {
  font-family: var(--font-display);
  font-size: var(--text-md);
  font-weight: 700;
  margin-bottom: var(--sp-3);
}

.list-items {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.list-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 100%;
  padding: var(--sp-3);
  border: 1px solid var(--border-base);
  border-radius: var(--radius-md);
  background: var(--bg-raised);
  text-align: left;
  font-family: var(--font-sans);
  cursor: pointer;
  transition: all var(--duration-fast);
}

.list-item:hover {
  border-color: var(--border-strong);
  background: var(--bg-hover);
}

.list-item.active {
  border-color: var(--accent);
  background: var(--accent-soft);
}

.list-item strong {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--text-primary);
}

.list-item span {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

.result-block {
  margin-top: var(--sp-4);
}

.result-block h4 {
  font-size: var(--text-sm);
  font-weight: 600;
  margin-bottom: var(--sp-2);
}

.result-block pre {
  padding: var(--sp-4);
  background: var(--bg-inset);
  border: 1px solid var(--border-base);
  border-radius: var(--radius-lg);
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  color: var(--accent);
  overflow-x: auto;
  max-height: 400px;
}

/* ── Audit Filters ───────────────────────────────────────── */
.audit-filters {
  display: flex;
  flex-direction: column;
  gap: var(--sp-3);
  padding-bottom: var(--sp-4);
  border-bottom: 1px solid var(--border-base);
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(150px, 1fr));
  gap: var(--sp-3);
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: var(--sp-1);
}

.filter-label {
  font-size: var(--text-xs);
  font-weight: 600;
  color: var(--text-tertiary);
}

.filter-actions {
  display: flex;
  gap: var(--sp-2);
}

.audit-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(100px, 1fr));
  gap: var(--sp-2);
}

.notice {
  padding: var(--sp-3) var(--sp-4);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
}

.notice-info { background: var(--info-soft); border: 1px solid rgba(96, 165, 250, 0.15); color: var(--info); }
.notice-danger { background: var(--danger-soft); border: 1px solid rgba(248, 113, 113, 0.15); color: var(--danger); }
.notice-success { background: var(--success-soft); border: 1px solid rgba(45, 212, 168, 0.15); color: var(--success); }

.priority-badge {
  display: inline-flex;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  font-size: 10px;
  font-weight: 700;
}

.priority-badge.p-high {
  background: var(--danger-soft);
  color: var(--danger);
}

.priority-badge.p-medium {
  background: var(--warning-soft);
  color: var(--warning);
}

.priority-badge.p-low {
  background: var(--info-soft);
  color: var(--info);
}

.form-label {
  display: block;
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: var(--sp-1-5);
}

.danger-hover:hover {
  color: var(--danger) !important;
  background: var(--danger-soft) !important;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@media (max-width: 1100px) {
  .metrics-grid {
    grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  }
  .section-grid,
  .split-layout {
    grid-template-columns: 1fr;
  }
  .filter-grid {
    grid-template-columns: repeat(2, minmax(150px, 1fr));
  }
  .audit-summary {
    grid-template-columns: repeat(2, minmax(100px, 1fr));
  }
}
</style>
