﻿<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getTodayStats, getDailyStats, getTopDishes } from '../../../api/merchant/statistics'

const loading = ref(false)
const todayStats = ref({
  orderCount: 0,
  revenue: 0,
  newUserCount: 0,
  avgOrderAmount: 0
})
const periodStats = ref<any[]>([])
const topDishes = ref<any[]>([])

const totalRevenue = computed(() => {
  return periodStats.value.reduce((sum, item) => sum + Number(item.revenue || 0), 0)
})

const totalOrders = computed(() => {
  return periodStats.value.reduce((sum, item) => sum + Number(item.orderCount || 0), 0)
})

const avgDailyRevenue = computed(() => {
  if (!periodStats.value.length) return 0
  return totalRevenue.value / periodStats.value.length
})

const maxRevenue = computed(() => {
  return Math.max(...periodStats.value.map((item) => Number(item.revenue || 0)), 0)
})

const metricCards = computed(() => [
  { label: '今日营收', value: `¥${formatAmount(todayStats.value.revenue)}`, hint: '当天已完成销售额' },
  { label: '今日订单', value: todayStats.value.orderCount, hint: '当天订单量' },
  { label: '平均客单价', value: `¥${formatAmount(todayStats.value.avgOrderAmount)}`, hint: '营收 / 订单数' },
  { label: '新增用户', value: todayStats.value.newUserCount, hint: '今日新增下单用户' }
])

const getDateStr = (daysAgo: number) => {
  const d = new Date()
  d.setDate(d.getDate() - daysAgo)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const formatAmount = (amount: number | string | undefined) => {
  return Number(amount || 0).toFixed(2)
}

const getBarHeight = (revenue: number | string | undefined) => {
  if (!maxRevenue.value) return '8%'
  return `${Math.max((Number(revenue || 0) / maxRevenue.value) * 100, 8)}%`
}

const fetchData = async () => {
  loading.value = true
  try {
    const [todayRes, dailyRes, topRes] = await Promise.all([
      getTodayStats(),
      getDailyStats({ startDate: getDateStr(6), endDate: getDateStr(0) }),
      getTopDishes({ limit: 10 })
    ])
    if (todayRes.code === 200) {
      todayStats.value = todayRes.data
    }
    if (dailyRes.code === 200) {
      periodStats.value = dailyRes.data || []
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
    <div class="statistics-page">
      <div class="page-head">
        <div>
          <h2>销售统计</h2>
          <p>查看今日经营、近 7 日销售和热销菜品表现</p>
        </div>
        <el-button :icon="Refresh" :loading="loading" @click="fetchData">刷新</el-button>
      </div>

      <div class="metric-grid">
        <div v-for="item in metricCards" :key="item.label" class="metric-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <em>{{ item.hint }}</em>
        </div>
      </div>

      <div class="content-grid">
        <div class="card-panel trend-panel">
          <div class="section-head">
            <div>
              <h3>近 7 日销售</h3>
              <p>合计 ¥{{ formatAmount(totalRevenue) }}，{{ totalOrders }} 单，日均 ¥{{ formatAmount(avgDailyRevenue) }}</p>
            </div>
          </div>

          <div class="bars" v-if="periodStats.length">
            <div v-for="item in periodStats" :key="item.date" class="bar-item">
              <div class="bar-track">
                <div class="bar-fill" :style="{ height: getBarHeight(item.revenue) }"></div>
              </div>
              <span>{{ item.date?.slice(5) }}</span>
              <em>¥{{ formatAmount(item.revenue) }}</em>
            </div>
          </div>
          <el-empty v-else description="暂无销售数据" />

          <el-table :data="periodStats" class="period-table" empty-text="暂无期间统计">
            <el-table-column prop="date" label="日期" />
            <el-table-column prop="revenue" label="销售额">
              <template #default="{ row }">
                <span class="amount">¥{{ formatAmount(row.revenue) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="orderCount" label="订单数" />
          </el-table>
        </div>

        <div class="card-panel ranking-panel">
          <div class="section-head">
            <div>
              <h3>热销菜品排行</h3>
              <p>按销量展示当前门店高频菜品</p>
            </div>
          </div>

          <div class="ranking-list" v-if="topDishes.length">
            <div v-for="(item, index) in topDishes" :key="item.dishId || item.dishName" class="ranking-item">
              <span class="rank">{{ index + 1 }}</span>
              <div class="ranking-main">
                <strong>{{ item.dishName }}</strong>
                <em>{{ item.soldCount }} 份</em>
              </div>
              <span class="amount">¥{{ formatAmount(item.revenue) }}</span>
            </div>
          </div>
          <el-empty v-else description="暂无热销数据" />
        </div>
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

.statistics-page {
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

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.metric-card {
  min-height: 104px;
  padding: 16px;
  background: #fff;
  border: 1px solid var(--bs-border-light);
  border-left: 3px solid var(--bs-primary);
  border-radius: var(--bs-radius-md);
  box-shadow: var(--bs-card-shadow);
}

.metric-card span,
.metric-card em {
  display: block;
  color: var(--bs-text-muted);
  font-size: 13px;
  font-style: normal;
}

.metric-card strong {
  display: block;
  margin: 6px 0 4px;
  color: var(--bs-text-title);
  font-size: 28px;
  line-height: 1.1;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(320px, 0.9fr);
  gap: 16px;
}

.card-panel {
  background: var(--bs-card-bg);
  border-radius: var(--bs-radius-md);
  box-shadow: var(--bs-card-shadow);
  padding: var(--bs-spacing-lg);
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-head h3 {
  margin: 0;
  font-size: var(--bs-font-size-lg);
  font-weight: 600;
  color: var(--bs-text-title);
}

.section-head p {
  margin-top: 4px;
  color: var(--bs-text-muted);
  font-size: 13px;
}

.bars {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 12px;
  height: 220px;
  padding: 12px 4px 18px;
  border-bottom: 1px solid var(--bs-border-light);
}

.bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.bar-track {
  display: flex;
  align-items: flex-end;
  width: 100%;
  height: 145px;
  background: var(--bs-bg-hover);
  border-radius: var(--bs-radius-sm);
  overflow: hidden;
}

.bar-fill {
  width: 100%;
  min-height: 8px;
  background: var(--bs-primary);
  border-radius: var(--bs-radius-sm) var(--bs-radius-sm) 0 0;
}

.bar-item span,
.bar-item em {
  color: var(--bs-text-muted);
  font-size: 12px;
  font-style: normal;
  white-space: nowrap;
}

.period-table {
  margin-top: 18px;
}

.amount {
  color: var(--bs-text-title);
  font-weight: 600;
}

.ranking-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ranking-item {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--bs-border-light);
  border-radius: var(--bs-radius-md);
}

.rank {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  color: #fff;
  background: var(--bs-primary);
  border-radius: 50%;
  font-size: 13px;
  font-weight: 600;
}

.ranking-main {
  min-width: 0;
}

.ranking-main strong,
.ranking-main em {
  display: block;
}

.ranking-main strong {
  color: var(--bs-text-title);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ranking-main em {
  margin-top: 4px;
  color: var(--bs-text-muted);
  font-size: 12px;
  font-style: normal;
}

@media (max-width: 1180px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .page-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }

  .bars {
    overflow-x: auto;
    grid-template-columns: repeat(7, 72px);
  }
}
</style>
