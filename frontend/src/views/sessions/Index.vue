<template>
  <div class="session-manager">
    <div class="session-list">
      <div class="session-title">
        <h2>会话管理</h2>
        <p>创建会话并验证 Agent 运行链路。</p>
      </div>
      <form v-if="canCreate" class="create-session" @submit.prevent="createSession">
        <label>Agent</label>
        <select v-model="newSession.agentId" required>
          <option value="" disabled>选择 Agent</option>
          <option v-for="agent in agents" :key="agent.id" :value="agent.id">
            {{ agent.name }}
          </option>
        </select>
        <label>用户 ID</label>
        <input v-model="newSession.userId" type="number" min="1" required />
        <button type="submit">新建会话</button>
      </form>

      <div v-for="session in sessions" :key="session.id" class="session-card" @click="selectSession(session)">
        <h3>{{ session.agentName }}</h3>
        <p>创建时间：{{ formatDate(session.createdAt) }}</p>
        <span :class="['status', session.status]">{{ formatStatus(session.status) }}</span>
      </div>
    </div>

    <div v-if="currentSession" class="chat-area">
      <div class="chat-header">
        <h3>{{ currentSession.agentName }}</h3>
        <button @click="closeSession">关闭</button>
      </div>

      <div class="messages" ref="messagesContainer">
        <div v-for="msg in currentSession.messages" :key="msg.id" class="message" :class="msg.role">
          <div class="message-content">{{ msg.content }}</div>
          <div class="message-time">
            {{ formatTime(msg.timestamp) }}
            <span v-if="msg.traceId">Trace {{ shortId(msg.traceId) }}</span>
            <span v-if="msg.stepRecordId">Step {{ msg.stepRecordId }}</span>
          </div>
        </div>
      </div>

      <div class="message-input">
        <input
          v-model="newMessage"
          @keyup.enter="sendMessage"
          placeholder="输入消息..."
          :disabled="!canMessage"
        />
        <button @click="sendMessage" :disabled="!canMessage || !newMessage.trim()">
          发送
        </button>
      </div>
    </div>

    <div v-else class="no-session">
      <p>选择或新建会话开始对话</p>
    </div>
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
      agents: [],
      sessions: [],
      currentSession: null,
      newMessage: '',
      newSession: {
        agentId: '',
        userId: '1'
      }
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
    await this.loadAgents();
    await this.loadSessions();
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
        session.agentName = agent ? agent.name : (session.agentName || session.name || `会话 ${session.id}`);
        session.messages = session.messages || [];
      });
    },
    async createSession() {
      const response = await apiFetch('/api/sessions', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(this.newSession)
      });
      if (!response.ok) {
        alert('会话创建失败');
        return;
      }
      const session = await response.json();
      await this.loadSessions();
      const created = this.sessions.find(item => String(item.id) === String(session.id));
      if (created) {
        this.selectSession(created);
      }
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

      const message = {
        id: Date.now(),
        role: 'user',
        content: this.newMessage,
        timestamp: new Date().toISOString()
      };
      const content = this.newMessage;
      this.newMessage = '';
      this.currentSession.messages.push(message);
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
          content: '消息发送失败',
          timestamp: new Date().toISOString()
        });
      }
    },
    formatDate(date) {
      return new Date(date).toLocaleString();
    },
    formatTime(date) {
      return new Date(date).toLocaleTimeString();
    },
    shortId(value) {
      const text = String(value || '');
      return text.length > 10 ? `${text.slice(0, 10)}...` : text;
    },
    formatStatus(status) {
      const map = {
        ACTIVE: '进行中',
        active: '进行中',
        ENDED: '已结束',
        ended: '已结束',
        ERROR: '异常',
        error: '异常',
        closed: '已关闭'
      };
      return map[status] || status || '未知';
    }
  }
};
</script>

<style scoped>
.session-manager {
  display: flex;
  min-height: 100vh;
  background: var(--bg);
}
.session-list {
  width: 340px;
  border-right: 1px solid var(--border);
  background: var(--surface);
  overflow-y: auto;
}
.session-title {
  padding: 20px 16px 10px;
  border-bottom: 1px solid var(--border);
}
.session-title h2,
.session-title p {
  margin: 0;
}
.session-title h2 {
  font-size: 22px;
  line-height: 28px;
}
.session-title p {
  margin-top: 4px;
  color: var(--text-muted);
}
.create-session {
  padding: 14px 16px;
  border-bottom: 1px solid var(--border);
  display: grid;
  gap: 8px;
}
.create-session label {
  font-size: 12px;
  font-weight: bold;
  color: var(--text-muted);
}
.create-session input,
.create-session select {
  width: 100%;
  padding: 8px;
  border: 1px solid var(--border);
  border-radius: 5px;
  background: var(--surface);
}
.create-session button {
  padding: 9px 12px;
  background: var(--primary);
  color: #fff;
  border: 0;
  border-radius: 5px;
  cursor: pointer;
  font-weight: 700;
}
.session-card {
  padding: 14px 16px;
  border-bottom: 1px solid var(--border);
  cursor: pointer;
  background: var(--surface);
}
.session-card:hover {
  background: var(--primary-soft);
}
.session-card h3,
.session-card p {
  margin: 0;
}
.session-card p {
  margin-top: 5px;
  color: var(--text-muted);
  font-size: 12px;
}
.session-card .status {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}
.session-card .status.active,
.session-card .status.ACTIVE {
  background: var(--success);
  color: white;
}
.session-card .status.closed,
.session-card .status.ENDED {
  background: #ccc;
}
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--surface);
}
.chat-header {
  padding: 15px;
  border-bottom: 1px solid var(--border);
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.chat-header h3 {
  margin: 0;
  font-size: 16px;
}
.chat-header button {
  min-height: 32px;
  padding: 6px 12px;
  border: 0;
  border-radius: 5px;
  background: #e8edf3;
  cursor: pointer;
}
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: var(--bg);
}
.message {
  margin-bottom: 15px;
  max-width: 70%;
}
.message.user {
  margin-left: auto;
  text-align: right;
}
.message.assistant {
  margin-right: auto;
}
.message-content {
  padding: 10px 15px;
  border-radius: 7px;
  display: inline-block;
  text-align: left;
  box-shadow: var(--shadow-sm);
}
.message.user .message-content {
  background: var(--primary);
  color: white;
}
.message.assistant .message-content {
  background: var(--surface-muted);
  border: 1px solid var(--border);
}
.message-time {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
}
.message-input {
  padding: 15px;
  border-top: 1px solid var(--border);
  display: flex;
  gap: 10px;
  background: var(--surface);
}
.message-input input {
  flex: 1;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: 5px;
}
.message-input button {
  padding: 10px 20px;
  background: var(--primary);
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-weight: 700;
}
.message-input button:disabled {
  background: #ccc;
  cursor: not-allowed;
}
.no-session {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
}
</style>
