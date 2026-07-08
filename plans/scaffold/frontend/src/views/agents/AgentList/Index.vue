<template>
  <div class="agent-list">
    <div class="header">
      <h1>Agent 列表</h1>
      <el-button type="primary" @click="handleCreate">创建 Agent</el-button>
    </div>
    <el-table :data="agents" style="width: 100%">
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="description" label="描述" />
      <el-table-column prop="status" label="状态" />
      <el-table-column prop="creator" label="创建人" />
      <el-table-column prop="createdAt" label="创建时间" />
      <el-table-column label="操作">
        <template #default="scope">
          <el-button @click="handleEdit(scope.row)">编辑</el-button>
          <el-button type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAgentList } from '@/api/agent'

const agents = ref<any[]>([])

onMounted(async () => {
  // TODO: 加载 Agent 列表
})

function handleCreate() {
  console.log('创建 Agent')
}

function handleEdit(agent: any) {
  console.log('编辑 Agent', agent)
}

function handleDelete(agent: any) {
  console.log('删除 Agent', agent)
}
</script>

<style scoped>
.agent-list {
  padding: 20px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
</style>