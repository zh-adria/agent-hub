<template>
  <div class="dashboard">
    <h1>AgentHub 控制台</h1>
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card>
          <template #header>Agent 总数</template>
          <div class="stat-value">12</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <template #header>Function 总数</template>
          <div class="stat-value">48</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <template #header>Session 总数</template>
          <div class="stat-value">156</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <template #header>今日调用</template>
          <div class="stat-value">1,234</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.dashboard {
  padding: 20px;
}
.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #409eff;
  text-align: center;
}
</style>