<script setup lang="ts">
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>系统设置</h3>
      </div>
      <div style="padding-top: 20px;">
        <el-form :model="{}" label-width="120px">
          <el-form-item label="系统名称">
            <el-input placeholder="请输入系统名称" />
          </el-form-item>
          <el-form-item label="AI API Key">
            <el-input type="password" placeholder="请输入API Key" />
          </el-form-item>
          <el-form-item label="配送范围">
            <el-input placeholder="请输入配送范围（公里）" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary">保存配置</el-button>
          </el-form-item>
        </el-form>
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