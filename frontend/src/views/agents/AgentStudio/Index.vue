<template>
  <div class="agent-studio">
    <h2>Agent Studio</h2>
    <form @submit.prevent="saveAgent">
      <div class="form-group">
        <label>Agent Name</label>
        <input v-model="agent.name" type="text" required />
      </div>
      <div class="form-group">
        <label>Description</label>
        <textarea v-model="agent.description"></textarea>
      </div>
      <div class="form-group">
        <label>Functions</label>
        <select multiple v-model="agent.functions">
          <option v-for="func in availableFunctions" :key="func.id" :value="func.id">
            {{ func.name }}
          </option>
        </select>
      </div>
      <button type="submit">Save Agent</button>
    </form>
  </div>
</template>

<script>
export default {
  data() {
    return {
      agent: { name: '', description: '', functions: [] },
      availableFunctions: []
    };
  },
  async created() {
    await this.loadFunctions();
  },
  methods: {
    async loadFunctions() {
      // TODO: fetch from API
      this.availableFunctions = [];
    },
    async saveAgent() {
      // TODO: POST /api/agents
      alert('Agent saved');
    }
  }
};
</script>

<style scoped>
.agent-studio {
  padding: 20px;
}
.form-group {
  margin-bottom: 15px;
}
label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
}
input, textarea, select {
  width: 100%;
  padding: 8px;
  box-sizing: border-box;
}
button {
  padding: 10px 20px;
  background: #42b983;
  color: white;
  border: none;
  cursor: pointer;
}
</style>
