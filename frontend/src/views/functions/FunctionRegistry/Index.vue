<template>
  <div class="function-registry">
    <h2>Function Registry</h2>
    <div class="toolbar">
      <button @click="showCreateForm = true">Register Function</button>
    </div>

    <!-- Function List -->
    <div class="function-list">
      <div v-for="func in functions" :key="func.id" class="function-card">
        <h3>{{ func.name }}</h3>
        <p>{{ func.description }}</p>
        <div class="actions">
          <button @click="testFunction(func)">Test</button>
          <button @click="editFunction(func)">Edit</button>
        </div>
      </div>
    </div>

    <!-- Create/Edit Form -->
    <div v-if="showCreateForm" class="modal">
      <div class="modal-content">
        <h3>{{ editingFunction ? 'Edit Function' : 'Register Function' }}</h3>
        <form @submit.prevent="saveFunction">
          <div class="form-group">
            <label>Name</label>
            <input v-model="functionForm.name" type="text" required />
          </div>
          <div class="form-group">
            <label>Description</label>
            <textarea v-model="functionForm.description"></textarea>
          </div>
          <div class="form-group">
            <label>Endpoint</label>
            <input v-model="functionForm.endpoint" type="text" required />
          </div>
          <div class="form-group">
            <label>Method</label>
            <select v-model="functionForm.method">
              <option value="GET">GET</option>
              <option value="POST">POST</option>
              <option value="PUT">PUT</option>
              <option value="DELETE">DELETE</option>
            </select>
          </div>
          <div class="actions">
            <button type="submit">Save</button>
            <button type="button" @click="showCreateForm = false">Cancel</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      functions: [],
      showCreateForm: false,
      editingFunction: null,
      functionForm: {
        name: '',
        description: '',
        endpoint: '',
        method: 'GET'
      }
    };
  },
  async created() {
    await this.loadFunctions();
  },
  methods: {
    async loadFunctions() {
      // TODO: fetch from API
      this.functions = [];
    },
    async saveFunction() {
      // TODO: POST /api/functions
      this.showCreateForm = false;
      await this.loadFunctions();
    },
    editFunction(func) {
      this.editingFunction = func;
      this.functionForm = { ...func };
      this.showCreateForm = true;
    },
    async testFunction(func) {
      // TODO: POST /api/functions/{id}/invoke
      alert(`Testing ${func.name}`);
    }
  }
};
</script>

<style scoped>
.function-registry {
  padding: 20px;
}
.toolbar {
  margin-bottom: 20px;
}
.function-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 15px;
}
.function-card {
  border: 1px solid #ddd;
  padding: 15px;
  border-radius: 5px;
}
.actions {
  margin-top: 10px;
}
.actions button {
  margin-right: 10px;
  padding: 5px 10px;
  cursor: pointer;
}
.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0,0,0,0.5);
  display: flex;
  justify-content: center;
  align-items: center;
}
.modal-content {
  background: white;
  padding: 20px;
  border-radius: 5px;
  width: 500px;
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
button[type="submit"] {
  padding: 10px 20px;
  background: #42b983;
  color: white;
  border: none;
  cursor: pointer;
  margin-right: 10px;
}
button[type="button"] {
  padding: 10px 20px;
  background: #ccc;
  border: none;
  cursor: pointer;
}
</style>
