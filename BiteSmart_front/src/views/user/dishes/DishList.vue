<script setup lang="ts">
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>菜品浏览</h3>
      </div>
      <div style="padding-top: 20px;">
        <el-table :data="[]" border>
          <el-table-column prop="name" label="菜品名称" />
          <el-table-column prop="category" label="分类" />
          <el-table-column prop="price" label="价格" />
          <el-table-column prop="rating" label="评分" />
          <el-table-column label="操作">
            <template #default>
              <el-button size="small">查看详情</el-button>
              <el-button size="small" type="primary">加入购物车</el-button>
            </template>
          </el-table-column>
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