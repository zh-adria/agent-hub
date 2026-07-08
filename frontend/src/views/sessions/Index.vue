<template>
  <div class="session-manager">
    <h2>Session Manager</h2>

    <!-- Session List -->
    <div class="session-list">
      <div v-for="session in sessions" :key="session.id" class="session-card" @click="selectSession(session)">
        <h3>{{ session.agentName }}</h3>
        <p>Created: {{ formatDate(session.createdAt) }}</p>
        <span :class="['status', session.status]">{{ session.status }}</span>
      </div>
    </div>

    <!-- Chat Area -->
    <div v-if="currentSession" class="chat-area">
      <div class="chat-header">
        <h3>{{ currentSession.agentName }}</h3>
        <button @click="closeSession">Close</button>
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
          placeholder="Type a message..."
          :disabled="!isConnected"
        />
        <button @click="sendMessage" :disabled="!isConnected || !newMessage.trim()">
          Send
        </button>
      </div>
    </div>

    <div v-else class="no-session">
      <p>Select a session to start chatting</p>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      sessions: [],
      currentSession: null,
      newMessage: '',
      isConnected: false,
      ws: null
    };
  },
  async created() {
    await this.loadSessions();
  },
  beforeDestroy() {
    if (this.ws) {
      this.ws.close();
    }
  },
  methods: {
    async loadSessions() {
      // TODO: fetch from API
      this.sessions = [];
    },
    selectSession(session) {
      this.currentSession = session;
      this.connectWebSocket(session.id);
    },
    closeSession() {
      this.currentSession = null;
      if (this.ws) {
        this.ws.close();
        this.ws = null;
      }
    },
    connectWebSocket(sessionId) {
      // TODO: implement WebSocket connection
      this.isConnected = true;
    },
    sendMessage() {
      if (!this.newMessage.trim() || !this.isConnected) return;

      // TODO: send message via WebSocket
      this.newMessage = '';
    },
    formatDate(date) {
      return new Date(date).toLocaleString();
    },
    formatTime(date) {
      return new Date(date).toLocaleTimeString();
    }
  }
};
</script>

<style scoped>
.session-manager {
  display: flex;
  height: 100vh;
}
.session-list {
  width: 300px;
  border-right: 1px solid #ddd;
  overflow-y: auto;
}
.session-card {
  padding: 15px;
  border-bottom: 1px solid #eee;
  cursor: pointer;
}
.session-card:hover {
  background: #f5f5f5;
}
.session-card .status {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}
.session-card .status.active {
  background: #42b983;
  color: white;
}
.session-card .status.closed {
  background: #ccc;
}
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.chat-header {
  padding: 15px;
  border-bottom: 1px solid #ddd;
  display: flex;
  justify-content: space-between;
  align-items: center;
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
  border-radius: 10px;
  display: inline-block;
}
.message.user .message-content {
  background: #42b983;
  color: white;
}
.message.assistant .message-content {
  background: #f0f0f0;
}
.message-time {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
}
.message-input {
  padding: 15px;
  border-top: 1px solid #ddd;
  display: flex;
  gap: 10px;
}
.message-input input {
  flex: 1;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 5px;
}
.message-input button {
  padding: 10px 20px;
  background: #42b983;
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
  color: #999;
}
</style>
