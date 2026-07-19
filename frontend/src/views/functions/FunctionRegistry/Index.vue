<template>
  <div class="function-registry">
    <div class="page-header">
      <div>
        <h1 class="page-title">Function Registry</h1>
        <p class="page-subtitle">Register and test HTTP / MCP tools for your agents.</p>
      </div>
      <button v-if="canCreate" class="btn btn-primary" @click="openCreateForm">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
          <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
        </svg>
        Register Function
      </button>
    </div>

    <!-- Stats Row -->
    <div class="stats-row">
      <div class="stat-card">
        <span class="stat-value">{{ functions.length }}</span>
        <span class="stat-label">Total Functions</span>
      </div>
      <div class="stat-card">
        <span class="stat-value">{{ methodCount('GET') }}</span>
        <span class="stat-label">GET Endpoints</span>
      </div>
      <div class="stat-card">
        <span class="stat-value">{{ methodCount('POST') }}</span>
        <span class="stat-label">POST Endpoints</span>
      </div>
    </div>

    <!-- Function Grid -->
    <div v-if="functions.length === 0" class="empty-state">
      <div class="empty-state-icon">&#9830;</div>
      <p class="empty-state-title">No functions registered</p>
      <p class="empty-state-desc">Register your first tool to enable agent capabilities.</p>
      <button v-if="canCreate" class="btn btn-primary btn-sm" @click="openCreateForm">
        Register Function
      </button>
    </div>

    <div v-else class="function-grid">
      <div v-for="func in functions" :key="func.id" class="function-card">
        <div class="function-card-header">
          <div class="function-info">
            <h4>{{ func.name }}</h4>
            <p>{{ func.description || 'No description' }}</p>
          </div>
          <span :class="['method-badge', 'method-' + (func.method || 'GET').toLowerCase()]">
            {{ func.method || 'GET' }}
          </span>
        </div>

        <div class="function-endpoint">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.5">
            <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" />
            <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" />
          </svg>
          <code class="endpoint-text">{{ func.endpoint }}</code>
        </div>

        <div class="function-card-actions">
          <button v-if="canInvoke" class="btn btn-secondary btn-sm" @click="testFunction(func)">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
              <polygon points="5 3 19 12 5 21 5 3" />
            </svg>
            Test
          </button>
          <button v-if="canUpdate" class="btn btn-ghost btn-sm" @click="editFunction(func)">Edit</button>
          <button v-if="canDelete" class="btn btn-ghost btn-sm danger-hover" @click="deleteFunction(func)">Delete</button>
        </div>
      </div>
    </div>

    <!-- Modal -->
    <div v-if="showForm" class="modal-overlay" @click.self="closeForm">
      <div class="modal-panel">
        <div class="modal-header">
          <h3>{{ editingFunction ? 'Edit Function' : 'Register Function' }}</h3>
          <button class="modal-close" @click="closeForm">&times;</button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="saveFunction">
            <div class="form-group">
              <label class="form-label">Name</label>
              <input v-model="functionForm.name" class="input" type="text" placeholder="get_weather" required />
            </div>
            <div class="form-group">
              <label class="form-label">Description</label>
              <textarea v-model="functionForm.description" class="textarea" placeholder="What does this function do?" rows="2"></textarea>
            </div>
            <div class="form-group">
              <label class="form-label">Endpoint URL</label>
              <input v-model="functionForm.endpoint" class="input font-mono" type="text" placeholder="https://api.example.com/weather" required />
            </div>
            <div class="form-group">
              <label class="form-label">HTTP Method</label>
              <select v-model="functionForm.method" class="select">
                <option value="GET">GET</option>
                <option value="POST">POST</option>
                <option value="PUT">PUT</option>
                <option value="DELETE">DELETE</option>
                <option value="PATCH">PATCH</option>
              </select>
            </div>
            <div class="form-actions">
              <button type="button" class="btn btn-secondary" @click="closeForm">Cancel</button>
              <button type="submit" class="btn btn-primary">
                {{ editingFunction ? 'Update' : 'Register' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { apiFetch, hasPermission } from '../../../api';

export default {
  props: {
    user: { type: Object, default: null }
  },
  data() {
    return {
      functions: [],
      showForm: false,
      editingFunction: null,
      functionForm: {
        name: '',
        description: '',
        endpoint: '',
        method: 'GET'
      }
    };
  },
  computed: {
    canCreate() {
      return hasPermission(this.user, 'function:create');
    },
    canUpdate() {
      return hasPermission(this.user, 'function:update');
    },
    canDelete() {
      return hasPermission(this.user, 'function:delete');
    },
    canInvoke() {
      return hasPermission(this.user, 'function:invoke');
    }
  },
  async created() {
    await this.loadFunctions();
  },
  methods: {
    async loadFunctions() {
      const response = await apiFetch('/api/functions');
      this.functions = response.ok ? await response.json() : [];
    },
    openCreateForm() {
      this.editingFunction = null;
      this.functionForm = { name: '', description: '', endpoint: '', method: 'GET' };
      this.showForm = true;
    },
    async saveFunction() {
      const method = this.editingFunction ? 'PUT' : 'POST';
      const url = this.editingFunction
        ? `/api/functions/${this.editingFunction.id}`
        : '/api/functions';
      const response = await apiFetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(this.functionForm)
      });
      if (!response.ok) {
        alert('Failed to save function');
        return;
      }
      this.closeForm();
      await this.loadFunctions();
    },
    editFunction(func) {
      this.editingFunction = func;
      this.functionForm = { ...func };
      this.showForm = true;
    },
    async testFunction(func) {
      const response = await apiFetch(`/api/functions/${func.id}/invoke`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({})
      });
      const result = response.ok ? await response.json() : { error: 'Test failed' };
      alert(JSON.stringify(result, null, 2));
    },
    async deleteFunction(func) {
      if (!confirm(`Delete function "${func.name}"?`)) return;
      const response = await apiFetch(`/api/functions/${func.id}`, { method: 'DELETE' });
      if (!response.ok) {
        alert('Failed to delete function');
        return;
      }
      await this.loadFunctions();
    },
    closeForm() {
      this.showForm = false;
      this.editingFunction = null;
      this.functionForm = { name: '', description: '', endpoint: '', method: 'GET' };
    },
    methodCount(method) {
      return this.functions.filter(f => (f.method || 'GET').toUpperCase() === method).length;
    }
  }
};
</script>

<style scoped>
.function-registry {
  padding: var(--sp-6);
  max-width: 1400px;
}

/* ── Stats Row ───────────────────────────────────────────── */
.stats-row {
  display: flex;
  gap: var(--sp-4);
  margin-bottom: var(--sp-6);
}

.stat-card {
  display: flex;
  align-items: baseline;
  gap: var(--sp-3);
  padding: var(--sp-3) var(--sp-4);
  background: var(--bg-raised);
  border: 1px solid var(--border-base);
  border-radius: var(--radius-lg);
}

.stat-value {
  font-family: var(--font-display);
  font-size: var(--text-xl);
  font-weight: 700;
  color: var(--text-primary);
}

.stat-label {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
}

/* ── Function Grid ───────────────────────────────────────── */
.function-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: var(--sp-4);
}

.function-card {
  background: var(--bg-raised);
  border: 1px solid var(--border-base);
  border-radius: var(--radius-lg);
  padding: var(--sp-5);
  display: flex;
  flex-direction: column;
  gap: var(--sp-4);
  transition: all var(--duration-normal) var(--ease-out);
}

.function-card:hover {
  border-color: var(--border-strong);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.function-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--sp-3);
}

.function-info h4 {
  font-family: var(--font-display);
  font-size: var(--text-md);
  font-weight: 700;
  letter-spacing: -0.01em;
  margin-bottom: var(--sp-1);
}

.function-info p {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  line-height: var(--leading-normal);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.method-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 3px 10px;
  border-radius: var(--radius-sm);
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.04em;
  flex-shrink: 0;
}

.method-get {
  background: var(--success-soft);
  color: var(--success);
  border: 1px solid rgba(45, 212, 168, 0.2);
}

.method-post {
  background: var(--info-soft);
  color: var(--info);
  border: 1px solid rgba(96, 165, 250, 0.2);
}

.method-put {
  background: var(--warning-soft);
  color: var(--warning);
  border: 1px solid rgba(251, 191, 36, 0.2);
}

.method-delete {
  background: var(--danger-soft);
  color: var(--danger);
  border: 1px solid rgba(248, 113, 113, 0.2);
}

.method-patch {
  background: rgba(168, 85, 247, 0.1);
  color: #a855f7;
  border: 1px solid rgba(168, 85, 247, 0.2);
}

.function-endpoint {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  padding: var(--sp-2-5) var(--sp-3);
  background: var(--bg-inset);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
}

.endpoint-text {
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.function-card-actions {
  display: flex;
  gap: var(--sp-2);
  padding-top: var(--sp-1);
}

.danger-hover:hover {
  color: var(--danger) !important;
  background: var(--danger-soft) !important;
}

/* ── Modal ───────────────────────────────────────────────── */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--sp-6);
  background: rgba(0, 0, 0, 0.65);
  backdrop-filter: blur(4px);
  animation: fadeIn var(--duration-normal) var(--ease-out);
}

.modal-panel {
  width: min(520px, 100%);
  background: var(--bg-raised);
  border: 1px solid var(--border-base);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  animation: slideUp var(--duration-slow) var(--ease-out);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--sp-5) var(--sp-5) 0;
}

.modal-header h3 {
  font-family: var(--font-display);
  font-size: var(--text-lg);
  font-weight: 700;
  letter-spacing: -0.01em;
}

.modal-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--text-tertiary);
  font-size: 20px;
  cursor: pointer;
  transition: all var(--duration-fast);
}

.modal-close:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.modal-body {
  padding: var(--sp-4) var(--sp-5) var(--sp-5);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--sp-3);
  margin-top: var(--sp-5);
  padding-top: var(--sp-4);
  border-top: 1px solid var(--border-base);
}

/* ── Animations ──────────────────────────────────────────── */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(12px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

.font-mono {
  font-family: var(--font-mono);
  font-size: var(--text-sm);
}

@media (max-width: 768px) {
  .function-grid {
    grid-template-columns: 1fr;
  }
  .stats-row {
    flex-wrap: wrap;
  }
}
</style>
