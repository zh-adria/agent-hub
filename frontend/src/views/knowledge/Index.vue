<template>
  <div class="knowledge-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">Knowledge Base</h1>
        <p class="page-subtitle">Manage knowledge bases, documents, and RAG retrieval.</p>
      </div>
    </div>

    <div class="kb-layout">
      <!-- Panel 1: Knowledge Bases -->
      <section class="kb-panel">
        <div class="panel-header">
          <div class="panel-title-group">
            <span class="panel-icon">&#9632;</span>
            <h3>Knowledge Bases</h3>
          </div>
          <button v-if="canCreate" class="btn btn-primary btn-sm" @click="createKnowledgeBase">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
              <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
            </svg>
          </button>
        </div>

        <div v-if="knowledgeBases.length === 0" class="empty-state">
          <p class="empty-state-desc">No knowledge bases yet</p>
        </div>

        <div class="kb-list">
          <button
            v-for="item in knowledgeBases"
            :key="item.id"
            :class="['kb-item', { active: selectedKB && selectedKB.id === item.id }]"
            @click="selectKB(item)"
          >
            <div class="kb-item-icon">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
                <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
              </svg>
            </div>
            <div class="kb-item-info">
              <strong>{{ item.name }}</strong>
              <span>{{ item.description || 'No description' }}</span>
            </div>
          </button>
        </div>

        <div v-if="selectedKB && canDelete" class="panel-footer">
          <button class="btn btn-danger btn-sm" style="width: 100%;" @click="deleteKB">
            Delete Knowledge Base
          </button>
        </div>
      </section>

      <!-- Panel 2: Documents -->
      <section class="kb-panel">
        <div class="panel-header">
          <div class="panel-title-group">
            <span class="panel-icon">&#9635;</span>
            <h3>Documents</h3>
          </div>
          <button v-if="selectedKB && canCreate" class="btn btn-primary btn-sm" @click="openDocForm">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
              <line x1="12" y1="5" x2="12" y2="19" /><line x1="5" y1="12" x2="19" y2="12" />
            </svg>
          </button>
        </div>

        <div v-if="!selectedKB" class="empty-state">
          <p class="empty-state-desc">Select a knowledge base first</p>
        </div>

        <template v-else>
          <div v-if="documents.length === 0" class="empty-state">
            <p class="empty-state-desc">No documents yet</p>
          </div>

          <div class="kb-list">
            <button
              v-for="doc in documents"
              :key="doc.id"
              :class="['kb-item', { active: selectedDoc && selectedDoc.id === doc.id }]"
              @click="selectDoc(doc)"
            >
              <div class="kb-item-icon">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                  <polyline points="14 2 14 8 20 8" />
                </svg>
              </div>
              <div class="kb-item-info">
                <strong>{{ doc.title }}</strong>
                <span>{{ doc.mimeType || 'text/plain' }} &middot; {{ doc.sourceUri || 'no source' }}</span>
              </div>
            </button>
          </div>

          <div v-if="selectedDoc && canDelete" class="panel-footer">
            <button class="btn btn-danger btn-sm" style="width: 100%;" @click="deleteDoc">
              Delete Document
            </button>
          </div>
        </template>
      </section>

      <!-- Panel 3: Chunks & Search -->
      <section class="kb-panel">
        <div class="panel-header">
          <div class="panel-title-group">
            <span class="panel-icon">&#9638;</span>
            <h3>Chunks & Search</h3>
          </div>
        </div>

        <!-- Search -->
        <div v-if="selectedKB && canSearch" class="search-bar">
          <div class="search-input-wrap">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.4">
              <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
            <input
              v-model="searchQuery"
              class="search-input"
              type="text"
              placeholder="Search knowledge base..."
              @keyup.enter="runSearch"
            />
          </div>
          <button class="btn btn-primary btn-sm" @click="runSearch" :disabled="!searchQuery.trim()">
            Search
          </button>
        </div>

        <!-- Search Results -->
        <div v-if="searchResults.length > 0" class="search-results">
          <div class="results-header">
            <span>{{ searchResults.length }} results</span>
          </div>
          <div v-for="result in searchResults" :key="result.chunkId" class="chunk-card search-result">
            <div class="chunk-scores">
              <span class="score-chip" :class="scoreClass(result.score)">
                Hybrid {{ formatScore(result.score) }}
              </span>
              <span class="score-chip score-vector">
                Vec {{ formatScore(result.vectorScore) }}
              </span>
              <span class="score-chip score-keyword">
                KW {{ formatScore(result.keywordScore) }}
              </span>
            </div>
            <p class="chunk-content">{{ result.content }}</p>
          </div>
        </div>

        <!-- Add Chunk -->
        <div v-if="selectedDoc && canCreate" class="chunk-add">
          <textarea
            v-model="chunkContent"
            class="textarea chunk-textarea"
            placeholder="Add a new chunk to this document..."
            rows="3"
          ></textarea>
          <button
            class="btn btn-primary btn-sm"
            @click="addChunk"
            :disabled="!chunkContent.trim()"
          >
            Add Chunk
          </button>
        </div>

        <!-- Chunks List -->
        <div v-if="!selectedDoc" class="empty-state">
          <p class="empty-state-desc">Select a document first</p>
        </div>
        <div v-else-if="chunks.length === 0 && searchResults.length === 0" class="empty-state">
          <p class="empty-state-desc">No chunks yet</p>
        </div>

        <div v-if="chunks.length > 0 && searchResults.length === 0" class="chunk-list">
          <div v-for="chunk in chunks" :key="chunk.id" class="chunk-card">
            <div class="chunk-header">
              <span class="chunk-index">#{{ chunk.chunkIndex }}</span>
              <button v-if="canDelete" class="btn btn-ghost btn-sm danger-hover" @click="deleteChunk(chunk)">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                  <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </div>
            <p class="chunk-content">{{ chunk.content }}</p>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import { apiFetch, hasPermission, formatScore } from '../../api';

export default {
  props: {
    user: { type: Object, default: null }
  },
  data() {
    return {
      knowledgeBases: [],
      documents: [],
      chunks: [],
      selectedKB: null,
      selectedDoc: null,
      searchQuery: '',
      searchResults: [],
      chunkContent: '',
      knowledgeBaseForm: { name: '', description: '' },
      documentForm: { title: '', sourceUri: '', mimeType: 'text/plain' }
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
    await this.loadKBs();
  },
  methods: {
    async loadKBs() {
      const response = await apiFetch('/api/knowledge-bases');
      this.knowledgeBases = response.ok ? await response.json() : [];
    },
    async createKnowledgeBase() {
      const name = prompt('Knowledge base name:');
      if (!name) return;
      const desc = prompt('Description (optional):') || '';
      const response = await apiFetch('/api/knowledge-bases', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, description: desc })
      });
      if (!response.ok) {
        alert('Failed to create knowledge base');
        return;
      }
      await this.loadKBs();
    },
    async selectKB(item) {
      this.selectedKB = item;
      this.selectedDoc = null;
      this.chunks = [];
      this.searchResults = [];
      await this.loadDocuments();
    },
    async loadDocuments() {
      if (!this.selectedKB) return;
      const response = await apiFetch(`/api/knowledge-bases/${this.selectedKB.id}/documents`);
      this.documents = response.ok ? await response.json() : [];
    },
    async deleteKB() {
      if (!confirm(`Delete knowledge base "${this.selectedKB.name}"?`)) return;
      const response = await apiFetch(`/api/knowledge-bases/${this.selectedKB.id}`, { method: 'DELETE' });
      if (!response.ok) {
        alert('Failed to delete');
        return;
      }
      this.selectedKB = null;
      this.selectedDoc = null;
      this.documents = [];
      this.chunks = [];
      this.searchResults = [];
      await this.loadKBs();
    },
    async selectDoc(doc) {
      this.selectedDoc = doc;
      this.searchResults = [];
      await this.loadChunks();
    },
    async loadChunks() {
      if (!this.selectedKB || !this.selectedDoc) return;
      const response = await apiFetch(`/api/knowledge-bases/${this.selectedKB.id}/documents/${this.selectedDoc.id}/chunks`);
      this.chunks = response.ok ? await response.json() : [];
    },
    async openDocForm() {
      const title = prompt('Document title:');
      if (!title) return;
      const source = prompt('Source URI (optional):') || '';
      const response = await apiFetch(`/api/knowledge-bases/${this.selectedKB.id}/documents`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ title, sourceUri: source, mimeType: 'text/plain' })
      });
      if (!response.ok) {
        alert('Failed to create document');
        return;
      }
      await this.loadDocuments();
    },
    async deleteDoc() {
      if (!confirm(`Delete document "${this.selectedDoc.title}"?`)) return;
      const response = await apiFetch(`/api/knowledge-bases/${this.selectedKB.id}/documents/${this.selectedDoc.id}`, { method: 'DELETE' });
      if (!response.ok) {
        alert('Failed to delete');
        return;
      }
      this.selectedDoc = null;
      this.chunks = [];
      this.searchResults = [];
      await this.loadDocuments();
    },
    async runSearch() {
      if (!this.searchQuery.trim()) return;
      const response = await apiFetch(`/api/knowledge-bases/${this.selectedKB.id}/search`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ query: this.searchQuery, topK: 5 })
      });
      if (!response.ok) {
        alert('Search failed');
        return;
      }
      this.searchResults = await response.json();
    },
    async addChunk() {
      if (!this.chunkContent.trim()) return;
      const response = await apiFetch(`/api/knowledge-bases/${this.selectedKB.id}/documents/${this.selectedDoc.id}/chunks`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content: this.chunkContent, chunkIndex: this.chunks.length })
      });
      if (!response.ok) {
        alert('Failed to add chunk');
        return;
      }
      this.chunkContent = '';
      await this.loadChunks();
    },
    async deleteChunk(chunk) {
      if (!confirm('Delete this chunk?')) return;
      const response = await apiFetch(`/api/knowledge-bases/${this.selectedKB.id}/documents/${this.selectedDoc.id}/chunks/${chunk.id}`, { method: 'DELETE' });
      if (!response.ok) {
        alert('Failed to delete');
        return;
      }
      await this.loadChunks();
    },
    formatScore,
    scoreClass(score) {
      const s = Number(score || 0);
      if (s >= 0.8) return 'score-high';
      if (s >= 0.5) return 'score-mid';
      return 'score-low';
    }
  }
};
</script>

<style scoped>
.knowledge-page {
  padding: var(--sp-6);
  max-width: 1600px;
}

/* ── 3-Panel Layout ──────────────────────────────────────── */
.kb-layout {
  display: grid;
  grid-template-columns: minmax(240px, 280px) minmax(260px, 320px) minmax(360px, 1fr);
  gap: var(--sp-4);
  align-items: start;
}

.kb-panel {
  background: var(--bg-raised);
  border: 1px solid var(--border-base);
  border-radius: var(--radius-lg);
  display: flex;
  flex-direction: column;
  min-height: 600px;
  max-height: calc(100vh - 140px);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--sp-4) var(--sp-4) var(--sp-3);
  border-bottom: 1px solid var(--border-base);
  flex-shrink: 0;
}

.panel-title-group {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
}

.panel-icon {
  color: var(--accent);
  font-size: 14px;
}

.panel-title-group h3 {
  font-family: var(--font-display);
  font-size: var(--text-sm);
  font-weight: 700;
  letter-spacing: -0.01em;
}

.panel-footer {
  margin-top: auto;
  padding: var(--sp-3) var(--sp-4);
  border-top: 1px solid var(--border-base);
  flex-shrink: 0;
}

/* ── KB / Doc Lists ──────────────────────────────────────── */
.kb-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--sp-2);
}

.kb-item {
  display: flex;
  align-items: flex-start;
  gap: var(--sp-3);
  width: 100%;
  padding: var(--sp-3);
  border: none;
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--text-secondary);
  font-family: var(--font-sans);
  text-align: left;
  cursor: pointer;
  transition: all var(--duration-fast);
  margin-bottom: 2px;
}

.kb-item:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.kb-item.active {
  background: var(--accent-soft);
  color: var(--accent);
}

.kb-item-icon {
  flex-shrink: 0;
  margin-top: 2px;
  opacity: 0.5;
}

.kb-item.active .kb-item-icon {
  opacity: 1;
}

.kb-item-info {
  flex: 1;
  min-width: 0;
}

.kb-item-info strong {
  display: block;
  font-size: var(--text-sm);
  font-weight: 600;
  line-height: 1.3;
  margin-bottom: 2px;
}

.kb-item-info span {
  display: block;
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kb-item.active .kb-item-info span {
  color: var(--accent-strong);
}

/* ── Search ──────────────────────────────────────────────── */
.search-bar {
  display: flex;
  gap: var(--sp-2);
  padding: var(--sp-3) var(--sp-4);
  border-bottom: 1px solid var(--border-base);
  flex-shrink: 0;
}

.search-input-wrap {
  flex: 1;
  position: relative;
}

.search-input-wrap svg {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  pointer-events: none;
}

.search-input {
  width: 100%;
  padding: 7px 10px 7px 32px;
  background: var(--bg-inset);
  border: 1px solid var(--border-base);
  border-radius: var(--radius-md);
  color: var(--text-primary);
  font-family: var(--font-sans);
  font-size: var(--text-sm);
}

.search-input:focus {
  outline: none;
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-ring);
}

.search-input::placeholder {
  color: var(--text-muted);
}

/* ── Chunk / Content Area ────────────────────────────────── */
.chunk-add {
  padding: var(--sp-3) var(--sp-4);
  border-top: 1px solid var(--border-base);
  flex-shrink: 0;
}

.chunk-textarea {
  margin-bottom: var(--sp-2);
  font-size: var(--text-sm);
  min-height: 60px;
}

.chunk-list,
.search-results {
  flex: 1;
  overflow-y: auto;
  padding: var(--sp-3) var(--sp-4);
}

.results-header {
  font-size: var(--text-xs);
  font-weight: 600;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-bottom: var(--sp-3);
}

.chunk-card {
  padding: var(--sp-3);
  margin-bottom: var(--sp-2);
  background: var(--bg-overlay);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
}

.chunk-card.search-result {
  border-color: rgba(45, 212, 168, 0.15);
}

.chunk-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--sp-2);
}

.chunk-index {
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  font-weight: 600;
}

.chunk-content {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  line-height: var(--leading-relaxed);
  white-space: pre-wrap;
  word-break: break-word;
}

.chunk-scores {
  display: flex;
  gap: var(--sp-1-5);
  margin-bottom: var(--sp-2);
}

.score-chip {
  display: inline-flex;
  align-items: center;
  padding: 1px 7px;
  border-radius: var(--radius-full);
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
}

.score-high {
  background: var(--success-soft);
  color: var(--success);
}

.score-mid {
  background: var(--warning-soft);
  color: var(--warning);
}

.score-low {
  background: var(--danger-soft);
  color: var(--danger);
}

.score-vector {
  background: rgba(96, 165, 250, 0.1);
  color: var(--info);
}

.score-keyword {
  background: rgba(168, 85, 247, 0.1);
  color: #a855f7;
}

.danger-hover:hover {
  color: var(--danger) !important;
  background: var(--danger-soft) !important;
}

@media (max-width: 1200px) {
  .kb-layout {
    grid-template-columns: 1fr;
  }
  .kb-panel {
    min-height: auto;
    max-height: none;
  }
}
</style>
