<template>
  <div class="knowledge-page">
    <div class="page-header">
      <h2>知识库</h2>
    </div>

    <div class="layout">
      <section class="panel">
        <h3>知识库列表</h3>
        <form v-if="canCreate" class="inline-form" @submit.prevent="createKnowledgeBase">
          <input v-model="knowledgeBaseForm.name" placeholder="知识库名称" required />
          <input v-model="knowledgeBaseForm.description" placeholder="描述" />
          <button type="submit">创建</button>
        </form>
        <div v-if="knowledgeBases.length === 0" class="empty">暂无知识库</div>
        <button v-if="selectedKnowledgeBase && canDelete" type="button" class="danger-button" @click="deleteKnowledgeBase">
          删除当前知识库
        </button>
        <button
          v-for="item in knowledgeBases"
          :key="item.id"
          type="button"
          class="list-item"
          :class="{ active: selectedKnowledgeBase && selectedKnowledgeBase.id === item.id }"
          @click="selectKnowledgeBase(item)"
        >
          <strong>{{ item.name }}</strong>
          <span>{{ item.description || '暂无描述' }}</span>
        </button>
      </section>

      <section class="panel">
        <h3>文档</h3>
        <form v-if="selectedKnowledgeBase && canCreate" class="inline-form" @submit.prevent="createDocument">
          <input v-model="documentForm.title" placeholder="文档标题" required />
          <input v-model="documentForm.sourceUri" placeholder="来源 URI" />
          <button type="submit">添加文档</button>
        </form>
        <div v-if="!selectedKnowledgeBase" class="empty">先选择知识库</div>
        <div v-else-if="documents.length === 0" class="empty">暂无文档</div>
        <button v-if="selectedDocument && canDelete" type="button" class="danger-button" @click="deleteDocument">
          删除当前文档
        </button>
        <button
          v-for="doc in documents"
          :key="doc.id"
          type="button"
          class="list-item"
          :class="{ active: selectedDocument && selectedDocument.id === doc.id }"
          @click="selectDocument(doc)"
        >
          <strong>{{ doc.title }}</strong>
          <span>{{ doc.sourceUri || '无来源' }}</span>
        </button>
      </section>

      <section class="panel">
        <h3>分块与检索</h3>
        <form v-if="selectedKnowledgeBase && canSearch" class="inline-form" @submit.prevent="searchKnowledgeBase">
          <input v-model="searchForm.query" placeholder="检索知识库" required />
          <button type="submit">检索</button>
        </form>
        <div v-if="searchResults.length > 0" class="search-results">
          <div v-for="result in searchResults" :key="result.chunkId" class="chunk">
            <div class="chunk-index">
              综合 {{ formatScore(result.score) }}
              / 向量 {{ formatScore(result.vectorScore) }}
              / 关键词 {{ formatScore(result.keywordScore) }}
            </div>
            <p>{{ result.content }}</p>
          </div>
        </div>
        <form v-if="selectedDocument && canCreate" class="chunk-form" @submit.prevent="createChunk">
          <textarea v-model="chunkForm.content" placeholder="分块内容" required></textarea>
          <button type="submit">添加分块</button>
        </form>
        <div v-if="!selectedDocument" class="empty">先选择文档</div>
        <div v-else-if="chunks.length === 0" class="empty">暂无分块</div>
        <div v-for="chunk in chunks" :key="chunk.id" class="chunk">
          <div class="chunk-header">
            <div class="chunk-index">#{{ chunk.chunkIndex }}</div>
            <button v-if="canDelete" type="button" @click="deleteChunk(chunk)">删除</button>
          </div>
          <p>{{ chunk.content }}</p>
        </div>
      </section>
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
      knowledgeBases: [],
      documents: [],
      chunks: [],
      selectedKnowledgeBase: null,
      selectedDocument: null,
      knowledgeBaseForm: {
        name: '',
        description: ''
      },
      documentForm: {
        title: '',
        sourceUri: '',
        mimeType: 'text/plain'
      },
      chunkForm: {
        content: ''
      },
      searchForm: {
        query: ''
      },
      searchResults: []
    };
  },
  computed: {
    canCreate() {
      return hasPermission(this.user, 'knowledge:create');
    },
    canDelete() {
      return hasPermission(this.user, 'knowledge:delete');
    },
    canSearch() {
      return hasPermission(this.user, 'knowledge:search');
    }
  },
  async created() {
    await this.loadKnowledgeBases();
  },
  methods: {
    async loadKnowledgeBases() {
      const response = await apiFetch('/api/knowledge-bases');
      this.knowledgeBases = response.ok ? await response.json() : [];
    },
    async createKnowledgeBase() {
      const response = await apiFetch('/api/knowledge-bases', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(this.knowledgeBaseForm)
      });
      if (!response.ok) {
        alert('知识库创建失败');
        return;
      }
      this.knowledgeBaseForm = { name: '', description: '' };
      await this.loadKnowledgeBases();
    },
    async selectKnowledgeBase(item) {
      this.selectedKnowledgeBase = item;
      this.selectedDocument = null;
      this.chunks = [];
      this.searchResults = [];
      await this.loadDocuments();
    },
    async loadDocuments() {
      const response = await apiFetch(`/api/knowledge-bases/${this.selectedKnowledgeBase.id}/documents`);
      this.documents = response.ok ? await response.json() : [];
    },
    async deleteKnowledgeBase() {
      const response = await apiFetch(`/api/knowledge-bases/${this.selectedKnowledgeBase.id}`, { method: 'DELETE' });
      if (!response.ok) {
        alert('知识库删除失败');
        return;
      }
      this.selectedKnowledgeBase = null;
      this.selectedDocument = null;
      this.documents = [];
      this.chunks = [];
      this.searchResults = [];
      await this.loadKnowledgeBases();
    },
    async createDocument() {
      const response = await apiFetch(`/api/knowledge-bases/${this.selectedKnowledgeBase.id}/documents`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(this.documentForm)
      });
      if (!response.ok) {
        alert('文档创建失败');
        return;
      }
      this.documentForm = { title: '', sourceUri: '', mimeType: 'text/plain' };
      await this.loadDocuments();
    },
    async deleteDocument() {
      const response = await apiFetch(`/api/knowledge-bases/${this.selectedKnowledgeBase.id}/documents/${this.selectedDocument.id}`, { method: 'DELETE' });
      if (!response.ok) {
        alert('文档删除失败');
        return;
      }
      this.selectedDocument = null;
      this.chunks = [];
      this.searchResults = [];
      await this.loadDocuments();
    },
    async selectDocument(doc) {
      this.selectedDocument = doc;
      this.searchResults = [];
      await this.loadChunks();
    },
    async loadChunks() {
      const response = await apiFetch(`/api/knowledge-bases/${this.selectedKnowledgeBase.id}/documents/${this.selectedDocument.id}/chunks`);
      this.chunks = response.ok ? await response.json() : [];
    },
    async createChunk() {
      const response = await apiFetch(`/api/knowledge-bases/${this.selectedKnowledgeBase.id}/documents/${this.selectedDocument.id}/chunks`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          content: this.chunkForm.content,
          chunkIndex: this.chunks.length
        })
      });
      if (!response.ok) {
        alert('分块创建失败');
        return;
      }
      this.chunkForm = { content: '' };
      await this.loadChunks();
    },
    async deleteChunk(chunk) {
      const response = await apiFetch(`/api/knowledge-bases/${this.selectedKnowledgeBase.id}/documents/${this.selectedDocument.id}/chunks/${chunk.id}`, { method: 'DELETE' });
      if (!response.ok) {
        alert('分块删除失败');
        return;
      }
      this.searchResults = [];
      await this.loadChunks();
    },
    async searchKnowledgeBase() {
      const response = await apiFetch(`/api/knowledge-bases/${this.selectedKnowledgeBase.id}/search`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          query: this.searchForm.query,
          topK: 5
        })
      });
      if (!response.ok) {
        alert('知识库检索失败');
        return;
      }
      this.searchResults = await response.json();
    },
    formatScore(score) {
      return Number(score || 0).toFixed(3);
    }
  }
};
</script>

<style scoped>
.knowledge-page {
  padding: 24px;
}
.page-header {
  margin-bottom: 18px;
}
.page-header h2 {
  margin: 0;
  font-size: 22px;
}
.layout {
  display: grid;
  grid-template-columns: minmax(260px, 320px) minmax(280px, 360px) minmax(360px, 1fr);
  gap: 18px;
}
.panel {
  min-width: 0;
}
.panel h3 {
  margin: 0 0 12px;
  font-size: 15px;
}
.inline-form,
.chunk-form {
  display: grid;
  gap: 8px;
  margin-bottom: 12px;
}
input,
textarea {
  width: 100%;
  min-height: 36px;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: 5px;
  background: var(--surface);
}
textarea {
  min-height: 120px;
  resize: vertical;
}
button {
  border: 0;
  border-radius: 5px;
  cursor: pointer;
}
.inline-form button,
.chunk-form button {
  min-height: 36px;
  padding: 8px 12px;
  background: var(--primary);
  color: #fff;
}
.list-item {
  display: block;
  width: 100%;
  padding: 12px;
  margin-bottom: 8px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--surface);
  text-align: left;
}
.list-item.active {
  border-color: var(--primary);
  background: #eef7fa;
}
.danger-button {
  width: 100%;
  min-height: 34px;
  margin-bottom: 10px;
  padding: 7px 10px;
  background: var(--danger);
  color: #fff;
}
.list-item strong,
.list-item span {
  display: block;
}
.list-item span {
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 13px;
}
.empty {
  padding: 14px;
  border: 1px dashed var(--border-strong);
  border-radius: 6px;
  background: var(--surface-muted);
  color: var(--text-muted);
}
.chunk {
  padding: 12px;
  margin-bottom: 10px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--surface);
}
.search-results {
  margin-bottom: 16px;
}
.chunk-index {
  color: var(--text-muted);
  font-size: 12px;
}
.chunk-header {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}
.chunk-header button {
  padding: 4px 8px;
  background: #eef2f6;
  color: var(--danger);
}
.chunk p {
  margin: 6px 0 0;
  white-space: pre-wrap;
}
@media (max-width: 1100px) {
  .layout {
    grid-template-columns: 1fr;
  }
}
</style>
