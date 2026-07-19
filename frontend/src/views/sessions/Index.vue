<template>
  <div class="session-manager">
    <!-- Session List Sidebar -->
    <div class="session-sidebar">
      <div class="session-sidebar-header">
        <h3>Sessions</h3>
        <button v-if="canCreate" class="btn btn-primary btn-sm" @click="showNewSession = true">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
            <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
          </svg>
        </button>
      </div>

      <!-- New Session Form -->
      <div v-if="showNewSession" class="new-session-form">
        <select v-model="newAgentId" class="select" required>
          <option value="" disabled>Select Agent</option>
          <option v-for="agent in agents" :key="agent.id" :value="agent.id">
            {{ agent.name }}
          </option>
        </select>
        <div class="form-row-compact">
          <input v-model="newUserId" class="input" type="number" min="1" placeholder="User ID" />
          <button class="btn btn-primary btn-sm" @click="createSession" :disabled="!newAgentId">
            Create
          </button>
        </div>
        <button class="btn btn-ghost btn-sm" style="width: 100%;" @click="showNewSession = false">Cancel</button>
      </div>

      <!-- Session List -->
      <div class="session-list">
        <div
          v-for="session in sessions"
          :key="session.id"
          :class="['session-item', { active: currentSession && currentSession.id === session.id }]"
          @click="selectSession(session)"
        >
          <div class="session-item-info">
            <strong>{{ session.agentName || 'Session ' + session.id }}</strong>
            <span>{{ formatDate(session.createdAt) }}</span>
          </div>
          <span :class="['status-badge', session.status]">
            {{ formatStatus(session.status) }}
          </span>
        </div>

        <div v-if="sessions.length === 0" class="empty-state" style="padding: var(--sp-8) var(--sp-4);">
          <p class="empty-state-desc">No sessions yet</p>
        </div>
      </div>
    </div>

    <!-- Chat Area -->
    <div v-if="currentSession" class="chat-area">
      <div class="chat-header">
        <div class="chat-header-info">
          <h3>{{ currentSession.agentName || 'Session ' + currentSession.id }}</h3>
          <span class="chat-header-status">
            <span :class="['status-dot', currentSession.status]"></span>
            {{ formatStatus(currentSession.status) }}
          </span>
        </div>
        <button class="btn btn-ghost btn-sm" @click="closeSession">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
          </svg>
        </button>
      </div>

      <div class="messages-container" ref="messagesContainer">
        <div v-if="currentSession.messages.length === 0" class="empty-chat">
          <div class="empty-chat-icon">&#9993;</div>
          <p>Start a conversation</p>
        </div>

        <div v-for="msg in currentSession.messages" :key="msg.id" :class="['message', msg.role]">
          <div class="message-avatar">
            {{ msg.role === 'user' ? 'U' : 'AI' }}
          </div>
          <div class="message-body">
            <div class="message-content">{{ msg.content }}</div>
            <div class="message-meta">
              <span>{{ formatTime(msg.timestamp) }}</span>
              <span v-if="msg.traceId" class="meta-trace">Trace {{ shortId(msg.traceId) }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="chat-input-area">
        <div class="chat-input-wrap">
          <textarea
            v-model="newMessage"
            class="chat-input"
            placeholder="Type a message..."
            rows="1"
            @keydown.enter.exact.prevent="sendMessage"
          ></textarea>
          <button
            class="send-btn"
            @click="sendMessage"
            :disabled="!canMessage || !newMessage.trim()"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="22" y1="2" x2="11" y2="13" />
              <polygon points="22 2 15 22 11 13 2 9 22 2" />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- No Session Selected -->
    <div v-else class="no-session">
      <div class="no-session-content">
        <div class="no-session-icon">&#9993;</div>
        <h3>No Session Selected</h3>
        <p>Select an existing session or create a new one to start chatting.</p>
        <button v-if="canCreate" class="btn btn-primary" @click="showNewSession = true">
          New Session
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { apiFetch, formatDate, formatStatus, formatTime, hasPermission, shortId } from '../../api';

export default {
  props: {
    user: { type: Object, default: null }
  },
  data() {
    return {
      agents: [],
      sessions: [],
      currentSession: null,
      newMessage: '',
      newAgentId: '',
      newUserId: '1',
      showNewSession: false
    };
  },
  computed: {
    canCreate() {
      return hasPermission(this.user, 'session:create');
    },
    canMessage() {
      return hasPermission(this.user, 'session:message') && this.currentSession;
    }
  },
  async created() {
    await Promise.all([this.loadAgents(), this.loadSessions()]);
  },
  methods: {
    async loadAgents() {
      const response = await apiFetch('/api/agents');
      this.agents = response.ok ? await response.json() : [];
    },
    async loadSessions() {
      const response = await apiFetch('/api/sessions');
      this.sessions = response.ok ? await response.json() : [];
      this.sessions.forEach(session => {
        const agent = this.agents.find(item => String(item.id) === String(session.agentId));
        session.agentName = agent ? agent.name : (session.agentName || session.name || `Session ${session.id}`);
        session.messages = session.messages || [];
      });
    },
    async createSession() {
      if (!this.newAgentId) return;
      const response = await apiFetch('/api/sessions', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ agentId: this.newAgentId, userId: this.newUserId })
      });
      if (!response.ok) {
        alert('Failed to create session');
        return;
      }
      this.showNewSession = false;
      await this.loadSessions();
      const session = await response.json();
      const created = this.sessions.find(item => String(item.id) === String(session.id));
      if (created) this.selectSession(created);
    },
    selectSession(session) {
      this.currentSession = session;
      this.loadMessages(session.id);
    },
    closeSession() {
      this.currentSession = null;
    },
    async loadMessages(sessionId) {
      const response = await apiFetch(`/api/sessions/${sessionId}/messages`);
      this.currentSession.messages = response.ok ? await response.json() : [];
    },
    async sendMessage() {
      if (!this.newMessage.trim() || !this.canMessage) return;
      const content = this.newMessage;
      this.newMessage = '';

      // Optimistic user message
      const userMsg = {
        id: Date.now(),
        role: 'user',
        content,
        timestamp: new Date().toISOString()
      };
      this.currentSession.messages.push(userMsg);

      const response = await apiFetch(`/api/sessions/${this.currentSession.id}/messages`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ role: 'user', content })
      });

      if (response.ok) {
        const saved = await response.json();
        this.currentSession.messages.push(saved);
      } else {
        this.currentSession.messages.push({
          id: Date.now() + 1,
          role: 'assistant',
          content: 'Failed to send message.',
          timestamp: new Date().toISOString()
        });
      }
    },
    formatDate,
    formatTime,
    formatStatus,
    shortId
  }
};
</script>

<style scoped>
.session-manager {
  display: flex;
  height: calc(100vh - 0px);
  background: var(--bg-base);
}

/* ── Session Sidebar ─────────────────────────────────────── */
.session-sidebar {
  width: 300px;
  min-width: 300px;
  background: var(--bg-raised);
  border-right: 1px solid var(--border-base);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.session-sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--sp-4) var(--sp-4) var(--sp-3);
  border-bottom: 1px solid var(--border-base);
  flex-shrink: 0;
}

.session-sidebar-header h3 {
  font-family: var(--font-display);
  font-size: var(--text-md);
  font-weight: 700;
  letter-spacing: -0.01em;
}

.new-session-form {
  padding: var(--sp-3) var(--sp-4);
  border-bottom: 1px solid var(--border-base);
  display: grid;
  gap: var(--sp-2);
  flex-shrink: 0;
  animation: slideIn var(--duration-normal) var(--ease-out);
}

.form-row-compact {
  display: flex;
  gap: var(--sp-2);
}

.form-row-compact .input {
  flex: 1;
  min-height: 34px;
}

.session-list {
  flex: 1;
  overflow-y: auto;
}

.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-3);
  padding: var(--sp-3) var(--sp-4);
  border-bottom: 1px solid var(--border-subtle);
  cursor: pointer;
  transition: background var(--duration-fast);
  width: 100%;
  background: none;
  border-left: none;
  border-right: none;
  border-top: none;
  text-align: left;
  font-family: var(--font-sans);
}

.session-item:hover {
  background: var(--bg-hover);
}

.session-item.active {
  background: var(--accent-soft);
  border-bottom-color: transparent;
}

.session-item-info {
  flex: 1;
  min-width: 0;
}

.session-item-info strong {
  display: block;
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.3;
  margin-bottom: 2px;
}

.session-item-info span {
  font-size: 11px;
  color: var(--text-tertiary);
}

.session-item.active .session-item-info strong {
  color: var(--accent);
}

.status-badge {
  display: inline-flex;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}

.status-badge.ACTIVE,
.status-badge.active {
  background: var(--success-soft);
  color: var(--success);
}

.status-badge.ENDED,
.status-badge.ended,
.status-badge.closed {
  background: rgba(107, 122, 148, 0.1);
  color: var(--text-tertiary);
}

.status-badge.ERROR,
.status-badge.error {
  background: var(--danger-soft);
  color: var(--danger);
}

/* ── Chat Area ───────────────────────────────────────────── */
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--bg-base);
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--sp-4) var(--sp-6);
  border-bottom: 1px solid var(--border-base);
  background: var(--bg-raised);
  flex-shrink: 0;
}

.chat-header-info h3 {
  font-family: var(--font-display);
  font-size: var(--text-md);
  font-weight: 700;
  letter-spacing: -0.01em;
  margin-bottom: 2px;
}

.chat-header-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--text-xs);
  color: var(--text-tertiary);
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  display: inline-block;
}

.status-dot.ACTIVE,
.status-dot.active {
  background: var(--success);
  box-shadow: 0 0 8px var(--accent-ring);
}

.status-dot.ENDED,
.status-dot.ended {
  background: var(--text-muted);
}

/* Messages */
.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: var(--sp-6);
  display: flex;
  flex-direction: column;
  gap: var(--sp-4);
}

.empty-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--sp-3);
  color: var(--text-muted);
}

.empty-chat-icon {
  font-size: 36px;
  opacity: 0.3;
}

.empty-chat p {
  font-size: var(--text-sm);
}

.message {
  display: flex;
  gap: var(--sp-3);
  max-width: 75%;
  animation: slideUp var(--duration-normal) var(--ease-out);
}

.message.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message.assistant {
  align-self: flex-start;
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.message.user .message-avatar {
  background: var(--accent);
  color: var(--bg-base);
}

.message.assistant .message-avatar {
  background: var(--bg-overlay);
  color: var(--text-tertiary);
  border: 1px solid var(--border-base);
}

.message-body {
  flex: 1;
  min-width: 0;
}

.message-content {
  padding: var(--sp-3) var(--sp-4);
  border-radius: var(--radius-lg);
  font-size: var(--text-sm);
  line-height: var(--leading-relaxed);
  word-break: break-word;
}

.message.user .message-content {
  background: var(--accent);
  color: var(--bg-base);
  border-bottom-right-radius: var(--radius-sm);
}

.message.assistant .message-content {
  background: var(--bg-raised);
  color: var(--text-primary);
  border: 1px solid var(--border-base);
  border-bottom-left-radius: var(--radius-sm);
}

.message-meta {
  display: flex;
  gap: var(--sp-3);
  margin-top: var(--sp-1-5);
  font-size: 11px;
  color: var(--text-muted);
}

.message.user .message-meta {
  justify-content: flex-end;
}

.meta-trace {
  font-family: var(--font-mono);
  font-size: 10px;
}

/* ── Input Area ──────────────────────────────────────────── */
.chat-input-area {
  padding: var(--sp-4) var(--sp-6);
  border-top: 1px solid var(--border-base);
  background: var(--bg-raised);
  flex-shrink: 0;
}

.chat-input-wrap {
  display: flex;
  align-items: flex-end;
  gap: var(--sp-3);
  background: var(--bg-inset);
  border: 1px solid var(--border-base);
  border-radius: var(--radius-lg);
  padding: var(--sp-1-5) var(--sp-1-5) var(--sp-1-5) var(--sp-4);
  transition: border-color var(--duration-fast), box-shadow var(--duration-fast);
}

.chat-input-wrap:focus-within {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-ring);
}

.chat-input {
  flex: 1;
  padding: var(--sp-2) 0;
  background: none;
  border: none;
  color: var(--text-primary);
  font-family: var(--font-sans);
  font-size: var(--text-sm);
  line-height: var(--leading-normal);
  resize: none;
  max-height: 120px;
}

.chat-input:focus {
  outline: none;
}

.chat-input::placeholder {
  color: var(--text-muted);
}

.send-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: var(--radius-md);
  background: var(--accent);
  color: var(--bg-base);
  cursor: pointer;
  flex-shrink: 0;
  transition: all var(--duration-fast);
}

.send-btn:hover:not(:disabled) {
  background: var(--accent-hover);
  box-shadow: var(--shadow-glow);
}

.send-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

/* ── No Session State ────────────────────────────────────── */
.no-session {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.no-session-content {
  text-align: center;
  color: var(--text-tertiary);
}

.no-session-icon {
  font-size: 48px;
  opacity: 0.2;
  margin-bottom: var(--sp-4);
}

.no-session h3 {
  font-family: var(--font-display);
  font-size: var(--text-lg);
  font-weight: 700;
  color: var(--text-secondary);
  margin-bottom: var(--sp-2);
}

.no-session p {
  font-size: var(--text-sm);
  margin-bottom: var(--sp-5);
  max-width: 300px;
  line-height: var(--leading-relaxed);
}

/* ── Animations ──────────────────────────────────────────── */
@keyframes slideIn {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 768px) {
  .session-sidebar {
    width: 260px;
    min-width: 260px;
  }
  .messages-container {
    padding: var(--sp-4);
  }
  .message {
    max-width: 90%;
  }
}
</style>
