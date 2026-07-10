<script setup lang="ts">
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>操作日志</h3>
      </div>
      <div style="padding-top: 20px;">
        <el-table :data="[]" border>
          <el-table-column prop="user" label="操作人" />
          <el-table-column prop="action" label="操作" />
          <el-table-column prop="module" label="模块" />
          <el-table-column prop="time" label="时间" />
          <el-table-column prop="ip" label="IP" />
        </el-table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.card-panel {
  background: var(--bs-card-bg);
  border-radius: var(--bs-radius-md);
  box-shadow: var(--bs-card-shadow);
  padding: var(--bs-spacing-lg);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--bs-spacing-lg);
}

.card-header h3 {
  font-size: var(--bs-font-size-lg);
  font-weight: 600;
  color: var(--bs-text-title);
}
</style>