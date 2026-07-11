﻿<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getTodayStats, getPeriodStats, getTopDishes } from '../../../api/merchant/statistics'

const loading = ref(false)
const todayStats = ref({
  orderCount: 0,
  revenue: 0,
  newUserCount: 0,
  avgOrderAmount: 0
})
const periodStats = ref<any[]>([])
const topDishes = ref<any[]>([])

const getDateStr = (daysAgo: number) => {
  const d = new Date()
  d.setDate(d.getDate() - daysAgo)
  return d.toISOString().slice(0, 10)
}

const fetchData = async () => {
  loading.value = true
  try {
    const [todayRes, periodRes, topRes] = await Promise.all([
      getTodayStats(),
      getPeriodStats({ startDate: getDateStr(7), endDate: getDateStr(0) }),
      getTopDishes({ limit: 10 })
    ])
    if (todayRes.code === 200) {
      todayStats.value = todayRes.data
    }
    if (periodRes.code === 200) {
      periodStats.value = periodRes.data.list || periodRes.data || []
    }
    if (topRes.code === 200) {
      topDishes.value = topRes.data.list || topRes.data || []
    }
  } catch (e) {
    console.error('获取统计数据失败', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="page-container" v-loading="loading">
    <div class="card-panel">
      <div class="card-header">
        <h3>今日概况</h3>
      </div>
      <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: var(--bs-spacing-lg); padding-top: 20px;">
        <div class="stat-item">
          <div class="stat-label">今日营收</div>
          <div class="stat-value">¥{{ todayStats.revenue }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">今日订单</div>
          <div class="stat-value">{{ todayStats.orderCount }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">新增用户</div>
          <div class="stat-value">{{ todayStats.newUserCount }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">平均客单价</div>
          <div class="stat-value">¥{{ todayStats.avgOrderAmount }}</div>
        </div>
      </div>
    </div>

    <div class="card-panel" style="margin-top: var(--bs-spacing-lg);">
      <div class="card-header">
        <h3>期间统计</h3>
      </div>
      <div style="padding-top: 20px;">
        <el-table :data="periodStats" border>
          <el-table-column prop="date" label="日期" />
          <el-table-column prop="revenue" label="销售额">
            <template #default="{ row }">
              ¥{{ row.revenue }}
            </template>
          </el-table-column>
          <el-table-column prop="orderCount" label="订单数" />
        </el-table>
      </div>
    </div>

    <div class="card-panel" style="margin-top: var(--bs-spacing-lg);">
      <div class="card-header">
        <h3>热销菜品排行</h3>
      </div>
      <div style="padding-top: 20px;">
        <el-table :data="topDishes" border>
          <el-table-column type="index" label="排名" width="80" />
          <el-table-column prop="dishName" label="菜品名称" />
          <el-table-column prop="soldCount" label="销量" />
          <el-table-column prop="revenue" label="营收">
            <template #default="{ row }">
              ¥{{ row.revenue }}
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

.stat-item {
  background: var(--bs-card-bg);
  border: 1px solid var(--bs-border-color, #e4e7ed);
  border-radius: var(--bs-radius-md);
  padding: var(--bs-spacing-lg);
  text-align: center;
}

.stat-label {
  font-size: var(--bs-font-size-sm);
  color: var(--bs-text-muted);
  margin-bottom: var(--bs-spacing-sm);
}

.stat-value {
  font-size: var(--bs-font-size-2xl);
  font-weight: 600;
  color: var(--bs-text-title);
}
</style>

