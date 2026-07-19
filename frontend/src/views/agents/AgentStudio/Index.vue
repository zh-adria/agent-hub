<template>
  <div class="agent-studio">
    <div class="page-header">
      <div>
        <h1 class="page-title">Agent Studio</h1>
        <p class="page-subtitle">Configure agents, bind tools, and manage prompts.</p>
      </div>
      <button v-if="canCreate" class="btn btn-primary" @click="resetForm">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
          <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
        </svg>
        New Agent
      </button>
    </div>

    <div class="studio-layout">
      <!-- Agent List -->
      <section class="agent-list-panel">
        <div class="panel-header">
          <h3>Agents</h3>
          <span class="count">{{ agents.length }}</span>
        </div>

        <div v-if="agents.length === 0" class="empty-state">
          <div class="empty-state-icon">&#9733;</div>
          <p class="empty-state-title">No agents yet</p>
          <p class="empty-state-desc">Create your first agent to get started with AI workflows.</p>
          <button v-if="canCreate" class="btn btn-primary btn-sm" @click="resetForm">
            Create Agent
          </button>
        </div>

        <div v-else class="agent-cards">
          <div
            v-for="item in agents"
            :key="item.id"
            :class="['agent-card', { active: editingId === String(item.id) }]"
            @click="editAgent(item)"
          >
            <div class="agent-card-main">
              <div class="agent-card-header">
                <h4>{{ item.name }}</h4>
                <span class="badge badge-accent">{{ item.model || 'gpt-4o-mini' }}</span>
              </div>
              <p class="agent-card-desc">{{ item.description || 'No description configured' }}</p>
              <div class="agent-card-meta">
                <span v-if="item.provider" class="meta-chip">{{ item.provider }}</span>
                <span v-if="item.temperature" class="meta-chip">
                  Temp {{ item.temperature }}
                </span>
                <span v-if="item.functionIds && item.functionIds.length > 0" class="meta-chip">
                  {{ item.functionIds.length }} tools
                </span>
              </div>
            </div>
            <div class="agent-card-actions" @click.stop>
              <button v-if="canUpdate" class="btn btn-ghost btn-sm" @click="editAgent(item)" title="Edit">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                  <path d="M17 3a2.83 2.83 0 1 1 4 4L7.5 20.5 4 21l.5-3.5L17 3z" />
                </svg>
              </button>
              <button v-if="canDelete" class="btn btn-ghost btn-sm danger-hover" @click="deleteAgent(item)" title="Delete">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                  <polyline points="3 6 5 6 21 6" /><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6" />
                </svg>
              </button>
            </div>
          </div>
        </div>
      </section>

      <!-- Editor Panel -->
      <section v-if="canCreate || canUpdate" class="editor-panel">
        <div class="panel-header">
          <h3>{{ agent.id ? 'Edit Agent' : 'Create Agent' }}</h3>
          <span v-if="agent.id" class="meta-text">ID {{ shortId(agent.id) }}</span>
        </div>

        <form @submit.prevent="saveAgent" class="editor-form">
          <div class="form-section">
            <h4 class="form-section-title">Basic Info</h4>
            <div class="form-group">
              <label class="form-label">Name</label>
              <input v-model="agent.name" class="input" type="text" placeholder="My Agent" required />
            </div>
            <div class="form-group">
              <label class="form-label">Description</label>
              <textarea v-model="agent.description" class="textarea" placeholder="What does this agent do?" rows="2"></textarea>
            </div>
          </div>

          <div class="form-section">
            <h4 class="form-section-title">Model Configuration</h4>
            <div class="form-row-2">
              <div class="form-group">
                <label class="form-label">Provider</label>
                <input v-model="agent.provider" class="input" type="text" placeholder="openai" />
              </div>
              <div class="form-group">
                <label class="form-label">Model</label>
                <input v-model="agent.model" class="input" type="text" placeholder="gpt-4o-mini" required />
              </div>
            </div>
            <div class="form-row-2">
              <div class="form-group">
                <label class="form-label">Temperature</label>
                <input v-model.number="agent.temperature" class="input" type="number" min="0" max="2" step="0.1" />
              </div>
              <div class="form-group">
                <label class="form-label">Max Tokens</label>
                <input v-model.number="agent.maxTokens" class="input" type="number" min="1" placeholder="4096" />
              </div>
            </div>
          </div>

          <div class="form-section">
            <h4 class="form-section-title">System Prompt</h4>
            <div class="form-group">
              <textarea v-model="agent.prompt" class="textarea prompt-editor" placeholder="You are a helpful assistant..." rows="6" required></textarea>
            </div>
          </div>

          <div class="form-section">
            <h4 class="form-section-title">
              Available Functions
              <span v-if="agent.functionIds && agent.functionIds.length" class="badge badge-accent ml-auto">
                {{ agent.functionIds.length }}
              </span>
            </h4>
            <div class="form-group">
              <select v-model="agent.functionIds" class="select" multiple>
                <option v-for="func in availableFunctions" :key="func.id" :value="func.id">
                  {{ func.name }}
                </option>
              </select>
              <p class="form-hint">Hold Ctrl/Cmd to select multiple tools</p>
            </div>
          </div>

          <div class="form-actions">
            <button v-if="agent.id" type="button" class="btn btn-secondary" @click="resetForm">
              Cancel
            </button>
            <button type="submit" class="btn btn-primary">
              {{ agent.id ? 'Update Agent' : 'Create Agent' }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </div>
</template>

<script>
import { apiFetch, hasPermission, shortId } from '../../../api';

export default {
  props: {
    user: { type: Object, default: null }
  },
  data() {
    return {
      agents: [],
      editingId: null,
      agent: this.emptyAgent(),
      availableFunctions: []
    };
  },
  computed: {
    canCreate() {
      return hasPermission(this.user, 'agent:create');
    },
    canUpdate() {
      return hasPermission(this.user, 'agent:update');
    },
    canDelete() {
      return hasPermission(this.user, 'agent:delete');
    }
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
        provider: '',
        model: 'gpt-4o-mini',
        temperature: 0.7,
        maxTokens: null,
        maxIterations: null,
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
        alert('Failed to save agent');
        return;
      }
      this.resetForm();
      await this.loadAgents();
    },
    editAgent(agent) {
      this.editingId = String(agent.id);
      this.agent = {
        ...this.emptyAgent(),
        ...agent,
        functionIds: agent.functionIds || []
      };
    },
    async deleteAgent(agent) {
      if (!confirm(`Delete agent "${agent.name}"?`)) return;
      const response = await apiFetch(`/api/agents/${agent.id}`, { method: 'DELETE' });
      if (!response.ok) {
        alert('Failed to delete agent');
        return;
      }
      if (this.editingId === String(agent.id)) {
        this.resetForm();
      }
      await this.loadAgents();
    },
    resetForm() {
      this.editingId = null;
      this.agent = this.emptyAgent();
    },
    shortId
  }
};
</script>

<style scoped>
.agent-studio {
  padding: var(--sp-6);
  max-width: 1400px;
}

/* ── Layout ──────────────────────────────────────────────── */
.studio-layout {
  display: grid;
  grid-template-columns: minmax(340px, 420px) minmax(460px, 1fr);
  gap: var(--sp-5);
  align-items: start;
}

/* ── Agent List ──────────────────────────────────────────── */
.agent-list-panel {
  min-width: 0;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--sp-4);
}

.panel-header h3 {
  font-family: var(--font-display);
  font-size: var(--text-md);
  font-weight: 700;
  letter-spacing: -0.01em;
}

.count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 20px;
  padding: 0 7px;
  border-radius: var(--radius-full);
  background: var(--bg-overlay);
  color: var(--text-tertiary);
  font-size: var(--text-xs);
  font-weight: 600;
}

.agent-cards {
  display: flex;
  flex-direction: column;
  gap: var(--sp-2);
}

.agent-card {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--sp-3);
  padding: var(--sp-4);
  background: var(--bg-raised);
  border: 1px solid var(--border-base);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-out);
}

.agent-card:hover {
  border-color: var(--border-strong);
  background: var(--bg-overlay);
}

.agent-card.active {
  border-color: var(--accent);
  background: var(--accent-soft);
  box-shadow: 0 0 0 1px var(--accent-ring);
}

.agent-card-main {
  flex: 1;
  min-width: 0;
}

.agent-card-header {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  margin-bottom: var(--sp-1-5);
}

.agent-card-header h4 {
  font-family: var(--font-display);
  font-size: var(--text-md);
  font-weight: 700;
  letter-spacing: -0.01em;
  line-height: 1.3;
}

.agent-card-desc {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  line-height: var(--leading-normal);
  margin-bottom: var(--sp-2-5);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.agent-card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sp-1-5);
}

.meta-chip {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  background: var(--bg-hover);
  color: var(--text-tertiary);
  font-size: 11px;
  font-weight: 500;
  font-family: var(--font-mono);
  border: 1px solid var(--border-subtle);
}

.agent-card-actions {
  display: flex;
  gap: 2px;
  flex-shrink: 0;
  opacity: 0;
  transition: opacity var(--duration-fast);
}

.agent-card:hover .agent-card-actions {
  opacity: 1;
}

.danger-hover:hover {
  color: var(--danger) !important;
  background: var(--danger-soft) !important;
}

.meta-text {
  font-size: var(--text-xs);
  color: var(--text-muted);
  font-family: var(--font-mono);
}

/* ── Editor ──────────────────────────────────────────────── */
.editor-panel {
  background: var(--bg-raised);
  border: 1px solid var(--border-base);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.editor-panel .panel-header {
  padding: var(--sp-4) var(--sp-5);
  margin-bottom: 0;
  border-bottom: 1px solid var(--border-base);
}

.editor-form {
  padding: var(--sp-5);
}

.form-section {
  margin-bottom: var(--sp-6);
  padding-bottom: var(--sp-5);
  border-bottom: 1px solid var(--border-subtle);
}

.form-section:last-of-type {
  margin-bottom: var(--sp-4);
  padding-bottom: 0;
  border-bottom: none;
}

.form-section-title {
  display: flex;
  align-items: center;
  font-size: var(--text-xs);
  font-weight: 700;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  margin-bottom: var(--sp-4);
}

.form-row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--sp-3);
}

.form-hint {
  margin-top: var(--sp-1-5);
  font-size: var(--text-xs);
  color: var(--text-muted);
}

.prompt-editor {
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  line-height: var(--leading-relaxed);
  background: var(--bg-inset);
  border: 1px solid var(--border-base);
  border-radius: var(--radius-md);
  padding: var(--sp-3);
  color: var(--text-primary);
  tab-size: 2;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--sp-3);
  padding-top: var(--sp-4);
  border-top: 1px solid var(--border-base);
}

.ml-auto {
  margin-left: auto;
}

/* ── Responsive ──────────────────────────────────────────── */
@media (max-width: 1000px) {
  .studio-layout {
    grid-template-columns: 1fr;
  }
}
</style>
