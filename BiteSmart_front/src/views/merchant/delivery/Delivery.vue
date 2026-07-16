﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getDeliveryTasks } from '../../../api/merchant/delivery'
import type { DeliveryTask } from '../../../api/merchant/delivery'

const loading = ref(false)
const taskList = ref<DeliveryTask[]>([])
const activeStatus = ref<number | 'all'>('all')

const statusOptions = [
  { label: '全部', value: 'all' as const },
  { label: '待接单', value: 10 },
  { label: '待取餐', value: 20 },
  { label: '已取餐', value: 30 },
  { label: '配送中', value: 40 },
  { label: '已送达', value: 50 },
  { label: '异常', value: 60 },
  { label: '已取消', value: 70 }
]

const statusConfig: Record<number, { label: string; tag: 'success' | 'warning' | 'primary' | 'info' | 'danger'; className: string }> = {
  10: { label: '待接单', tag: 'info', className: 'muted' },
  20: { label: '待取餐', tag: 'primary', className: 'pickup' },
  30: { label: '已取餐', tag: 'primary', className: 'pickup' },
  40: { label: '配送中', tag: 'warning', className: 'delivery' },
  50: { label: '已送达', tag: 'success', className: 'done' },
  60: { label: '异常', tag: 'danger', className: 'danger' },
  70: { label: '已取消', tag: 'info', className: 'muted' }
}

const filteredTaskList = computed(() => {
  if (activeStatus.value === 'all') return taskList.value
  return taskList.value.filter((item) => item.taskStatus === activeStatus.value)
})

const taskStats = computed(() => {
  const countByStatus = (status: number) => taskList.value.filter((item) => item.taskStatus === status).length
  return [
    { label: '待取餐', value: countByStatus(20), status: 20, hint: '出餐后等待骑手' },
    { label: '配送中', value: countByStatus(40), status: 40, hint: '关注送达进度' },
    { label: '异常', value: countByStatus(60), status: 60, hint: '需要及时处理' },
    { label: '已送达', value: countByStatus(50), status: 50, hint: '今日完成配送' }
  ]
})

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
  return statusConfig[status]?.tag || 'info'
}

const getTaskStatusLabel = (status: number) => {
  return statusConfig[status]?.label || '未知'
}

const getStatusClass = (status: number) => {
  return statusConfig[status]?.className || 'muted'
}

const handleStatusChange = (status: number | 'all') => {
  activeStatus.value = status
}

const formatText = (value?: string) => {
  return value || '-'
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="page-container">
    <div class="delivery-page">
      <div class="page-head">
        <div>
          <h2>配送管理</h2>
          <p>查看出餐后的取餐、配送、送达和异常状态</p>
        </div>
        <el-button :icon="Refresh" :loading="loading" @click="fetchData">刷新</el-button>
      </div>

      <div class="summary-grid">
        <button
          v-for="item in taskStats"
          :key="item.label"
          class="summary-item"
          :class="[getStatusClass(item.status), { active: activeStatus === item.status }]"
          @click="handleStatusChange(item.status)"
        >
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <em>{{ item.hint }}</em>
        </button>
      </div>

      <div class="card-panel delivery-panel">
        <div class="toolbar">
          <el-segmented v-model="activeStatus" :options="statusOptions" />
          <span class="toolbar-count">当前 {{ filteredTaskList.length }} 条配送任务</span>
        </div>

        <el-table :data="filteredTaskList" v-loading="loading" class="delivery-table" empty-text="暂无符合条件的配送任务">
          <el-table-column prop="orderNo" label="订单信息" min-width="220">
            <template #default="{ row }">
              <div class="order-no">{{ row.orderNo }}</div>
              <div class="sub-text">创建 {{ formatText(row.createTime) }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="driverName" label="配送员" min-width="150">
            <template #default="{ row }">
              <div class="strong-text">{{ formatText(row.driverName) }}</div>
              <div class="sub-text">{{ formatText(row.driverPhone) }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="taskStatus" label="配送状态" width="120">
            <template #default="{ row }">
              <el-tag :type="getTaskStatusTag(row.taskStatus)">{{ getTaskStatusLabel(row.taskStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="pickupCode" label="取餐码" width="120" />
          <el-table-column prop="estimatedDeliveryTime" label="预计送达" min-width="170">
            <template #default="{ row }">{{ formatText(row.estimatedDeliveryTime) }}</template>
          </el-table-column>
          <el-table-column prop="pickupTime" label="取餐时间" min-width="170">
            <template #default="{ row }">{{ formatText(row.pickupTime) }}</template>
          </el-table-column>
          <el-table-column prop="deliverTime" label="送达时间" min-width="170">
            <template #default="{ row }">{{ formatText(row.deliverTime) }}</template>
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

.delivery-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-head h2 {
  margin: 0;
  color: var(--bs-text-title);
  font-size: 20px;
  font-weight: 650;
}

.page-head p {
  margin-top: 4px;
  color: var(--bs-text-muted);
  font-size: 13px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.summary-item {
  min-height: 104px;
  padding: 16px;
  text-align: left;
  background: #fff;
  border: 1px solid var(--bs-border-light);
  border-radius: var(--bs-radius-md);
  box-shadow: var(--bs-card-shadow);
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.summary-item:hover,
.summary-item.active {
  border-color: rgba(27, 58, 47, 0.35);
  box-shadow: var(--bs-card-shadow-hover);
  transform: translateY(-1px);
}

.summary-item span,
.summary-item em {
  display: block;
  color: var(--bs-text-muted);
  font-size: 13px;
  font-style: normal;
}

.summary-item strong {
  display: block;
  margin: 6px 0 4px;
  color: var(--bs-text-title);
  font-size: 28px;
  line-height: 1.1;
}

.summary-item.pickup {
  border-left: 3px solid var(--bs-primary);
}

.summary-item.delivery {
  border-left: 3px solid #b76e2a;
}

.summary-item.danger {
  border-left: 3px solid var(--bs-status-danger);
}

.summary-item.done {
  border-left: 3px solid #1b6b4a;
}

.card-panel {
  background: var(--bs-card-bg);
  border-radius: var(--bs-radius-md);
  box-shadow: var(--bs-card-shadow);
  padding: var(--bs-spacing-lg);
}

.delivery-panel {
  padding-top: 16px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.toolbar-count {
  color: var(--bs-text-muted);
  font-size: 13px;
  white-space: nowrap;
}

.delivery-table {
  width: 100%;
}

.order-no,
.strong-text {
  color: var(--bs-text-title);
  font-weight: 600;
}

.sub-text {
  margin-top: 4px;
  color: var(--bs-text-muted);
  font-size: 12px;
}

@media (max-width: 1100px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .page-head,
  .toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
