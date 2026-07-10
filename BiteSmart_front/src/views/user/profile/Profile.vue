<script setup lang="ts">
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>个人中心</h3>
        <button class="btn btn-primary">编辑信息</button>
      </div>
      <div style="padding-top: 20px;">
        <el-form :model="{}" label-width="120px">
          <el-form-item label="昵称">
            <el-input placeholder="请输入昵称" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input placeholder="请输入邮箱" />
          </el-form-item>
          <el-form-item label="健康档案">
            <el-button size="small">查看健康档案</el-button>
          </el-form-item>
          <el-form-item label="会员中心">
            <el-button size="small">查看会员权益</el-button>
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

.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: var(--bs-spacing-sm) 20px;
  border-radius: var(--bs-radius-md);
  font-size: var(--bs-font-size-base);
  font-weight: 500;
  border: 1px solid transparent;
  cursor: pointer;
  transition: 0.15s;
}

.btn-primary {
  background: var(--bs-primary);
  color: #FFFFFF;
}

.btn-primary:hover {
  background: var(--bs-primary-hover);
}
</style>