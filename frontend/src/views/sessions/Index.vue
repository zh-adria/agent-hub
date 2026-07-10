<template>
  <div class="session-manager">
    <!-- 会话列表 -->
    <div class="session-list">
      <h2>会话管理</h2>
      <form class="create-session" @submit.prevent="createSession">
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

    <!-- 聊天区域 -->
    <div v-if="currentSession" class="chat-area">
      <div class="chat-header">
        <h3>{{ currentSession.agentName }}</h3>
        <button @click="closeSession">关闭</button>
      </div>

      <div class="messages" ref="messagesContainer">
        <div v-for="msg in currentSession.messages" :key="msg.id" class="message" :class="msg.role">
          <div class="message-content">{{ msg.content }}</div>
          <div class="message-time">{{ formatTime(msg.timestamp) }}</div>
        </div>
      </div>

      <div class="message-input">
        <input
          v-model="newMessage"
          @keyup.enter="sendMessage"
          placeholder="输入消息..."
          :disabled="!isConnected"
        />
        <button @click="sendMessage" :disabled="!isConnected || !newMessage.trim()">
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
import { apiFetch } from '../../api';

export default {
  data() {
    return {
      agents: [],
      sessions: [],
      currentSession: null,
      newMessage: '',
      isConnected: false,
      ws: null,
      newSession: {
        agentId: '',
        userId: '1'
      }
    };
  },
  async created() {
    await this.loadAgents();
    await this.loadSessions();
  },
  beforeUnmount() {
    if (this.ws) {
      this.ws.close();
    }
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
      this.connectSession(session.id);
    },
    closeSession() {
      this.currentSession = null;
      if (this.ws) {
        this.ws.close();
        this.ws = null;
      }
    },
    connectSession(sessionId) {
      this.isConnected = true;
      apiFetch(`/api/sessions/${sessionId}/messages`)
        .then(response => response.ok ? response.json() : [])
        .then(messages => {
          this.currentSession.messages = messages;
        });
    },
    async sendMessage() {
      if (!this.newMessage.trim() || !this.isConnected) return;

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
  height: 100vh;
  background: var(--surface);
}
.session-list {
  width: 320px;
  border-right: 1px solid var(--border);
  background: var(--surface-muted);
  overflow-y: auto;
}
.session-list h2 {
  margin: 0;
  padding: 20px 16px 4px;
  font-size: 22px;
}
.create-session {
  padding: 15px;
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
}
.session-card {
  padding: 15px;
  border-bottom: 1px solid var(--border);
  cursor: pointer;
  background: var(--surface);
}
.session-card:hover {
  background: #eef7fa;
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
  border-radius: 8px;
  display: inline-block;
  text-align: left;
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
}
.message-input button:disabled {
  background: #ccc;
}
.no-session {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
}
</style>
