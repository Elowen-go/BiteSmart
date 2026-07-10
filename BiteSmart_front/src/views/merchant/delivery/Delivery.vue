<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDeliveryTasks } from '../../../api/merchant/delivery'
import type { DeliveryTask } from '../../../api/merchant/delivery'

const loading = ref(false)
const taskList = ref<DeliveryTask[]>([])

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getDeliveryTasks()
    if (res.code === 200) {
      taskList.value = res.data.list || res.data || []
    }
  } catch (e) {
    console.error('获取配送任务失败', e)
  } finally {
    loading.value = false
  }
}

const getTaskStatusTag = (status: number): 'success' | 'warning' | 'primary' | 'info' | 'danger' => {
  const map: Record<number, 'success' | 'warning' | 'primary' | 'info' | 'danger'> = { 0: 'info', 1: 'primary', 2: 'warning', 3: 'success', 4: 'danger' }
  return map[status] || 'info'
}

const getTaskStatusLabel = (status: number) => {
  const map: Record<number, string> = { 0: '待分配', 1: '待取餐', 2: '配送中', 3: '已送达', 4: '异常' }
  return map[status] || '未知'
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>配送管理</h3>
      </div>
      <div style="padding-top: 20px;">
        <el-table :data="taskList" border v-loading="loading">
          <el-table-column prop="orderNo" label="订单号" width="200" />
          <el-table-column prop="driverName" label="配送员" width="120" />
          <el-table-column prop="driverPhone" label="联系方式" width="140" />
          <el-table-column prop="taskStatus" label="配送状态" width="120">
            <template #default="{ row }">
              <el-tag :type="getTaskStatusTag(row.taskStatus)">{{ getTaskStatusLabel(row.taskStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="pickupCode" label="取餐码" width="120" />
          <el-table-column prop="estimatedDeliveryTime" label="预计送达" width="180" />
          <el-table-column prop="createTime" label="创建时间" width="180" />
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

