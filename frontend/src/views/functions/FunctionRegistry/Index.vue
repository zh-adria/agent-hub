<template>
  <div class="function-registry">
    <div class="page-header">
      <h2>函数注册中心</h2>
      <button v-if="canCreate" @click="showCreateForm = true">注册函数</button>
    </div>

    <div class="function-list">
      <div v-if="functions.length === 0" class="empty">暂无函数</div>
      <div v-for="func in functions" :key="func.id" class="function-card">
        <h3>{{ func.name }}</h3>
        <p>{{ func.description || '暂无描述' }}</p>
        <span>{{ func.method }} {{ func.endpoint }}</span>
        <div class="actions">
          <button v-if="canInvoke" @click="testFunction(func)">测试</button>
          <button v-if="canUpdate" @click="editFunction(func)">编辑</button>
          <button v-if="canDelete" class="danger" @click="deleteFunction(func)">删除</button>
        </div>
      </div>
    </div>

    <div v-if="showCreateForm" class="modal">
      <div class="modal-content">
        <h3>{{ editingFunction ? '编辑函数' : '注册函数' }}</h3>
        <form @submit.prevent="saveFunction">
          <div class="form-group">
            <label>名称</label>
            <input v-model="functionForm.name" type="text" required />
          </div>
          <div class="form-group">
            <label>描述</label>
            <textarea v-model="functionForm.description"></textarea>
          </div>
          <div class="form-group">
            <label>端点地址</label>
            <input v-model="functionForm.endpoint" type="text" required />
          </div>
          <div class="form-group">
            <label>请求方法</label>
            <select v-model="functionForm.method">
              <option value="GET">GET</option>
              <option value="POST">POST</option>
              <option value="PUT">PUT</option>
              <option value="DELETE">DELETE</option>
            </select>
          </div>
          <div class="actions">
            <button type="submit">保存</button>
            <button type="button" @click="closeForm">取消</button>
          </div>
        </form>
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
    async saveFunction() {
      const method = this.editingFunction ? 'PUT' : 'POST';
      const url = this.editingFunction ? `/api/functions/${this.editingFunction.id}` : '/api/functions';
      const response = await apiFetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(this.functionForm)
      });
      if (!response.ok) {
        alert('函数保存失败');
        return;
      }
      this.closeForm();
      await this.loadFunctions();
    },
    editFunction(func) {
      this.editingFunction = func;
      this.functionForm = { ...func };
      this.showCreateForm = true;
    },
    async testFunction(func) {
      const response = await apiFetch(`/api/functions/${func.id}/invoke`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({})
      });
      const result = response.ok ? await response.json() : { error: '函数测试失败' };
      alert(JSON.stringify(result, null, 2));
    },
    async deleteFunction(func) {
      const response = await apiFetch(`/api/functions/${func.id}`, { method: 'DELETE' });
      if (!response.ok) {
        alert('函数删除失败');
        return;
      }
      await this.loadFunctions();
    },
    closeForm() {
      this.showCreateForm = false;
      this.editingFunction = null;
      this.functionForm = { name: '', description: '', endpoint: '', method: 'GET' };
    }
  }
};
</script>

<style scoped>
.function-registry {
  padding: 24px;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}
.page-header h2 {
  margin: 0;
  font-size: 22px;
}
.page-header button {
  min-height: 36px;
  padding: 8px 14px;
  border: 0;
  border-radius: 5px;
  background: var(--primary);
  color: #fff;
  cursor: pointer;
}
.function-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 12px;
}
.function-card {
  border: 1px solid var(--border);
  padding: 14px;
  border-radius: 6px;
  background: var(--surface);
}
.function-card h3 {
  margin: 0 0 6px;
  font-size: 15px;
}
.function-card p {
  margin: 0 0 8px;
  color: var(--text-muted);
}
.function-card span {
  display: block;
  color: var(--text-muted);
  font-size: 12px;
  word-break: break-all;
}
.actions {
  margin-top: 12px;
}
.actions button {
  margin-right: 8px;
  min-height: 32px;
  padding: 6px 10px;
  border: 0;
  border-radius: 5px;
  background: var(--primary);
  color: #fff;
  cursor: pointer;
}
.actions button.danger {
  background: var(--danger);
  color: #fff;
  border: 0;
}
.empty {
  padding: 16px;
  border: 1px dashed var(--border-strong);
  border-radius: 6px;
  background: var(--surface-muted);
  color: var(--text-muted);
}
.modal {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
}
.modal-content {
  background: var(--surface);
  padding: 20px;
  border-radius: 6px;
  width: 500px;
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.24);
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
  min-height: 36px;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: 5px;
  background: var(--surface);
  box-sizing: border-box;
}
textarea {
  min-height: 90px;
}
button[type="submit"] {
  min-height: 36px;
  padding: 8px 14px;
  border-radius: 5px;
  background: var(--primary);
  color: white;
  border: none;
  cursor: pointer;
  margin-right: 10px;
}
button[type="button"] {
  min-height: 36px;
  padding: 8px 14px;
  border-radius: 5px;
  background: #e8edf3;
  border: none;
  cursor: pointer;
}
</style>
