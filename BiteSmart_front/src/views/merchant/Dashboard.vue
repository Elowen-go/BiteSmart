<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { 
  Money, ShoppingCart, UserFilled, Coin, 
  Clock, Warning, Message, Star, DataBoard, PieChart 
} from '@element-plus/icons-vue'
import StatCard from '../../components/common/StatCard.vue'
import { getTodayStats, getTopDishes, getDailyStats, getCategoryRevenue } from '../../api/merchant/statistics'

const loading = ref(false)
const todayStats = ref({
  orderCount: 0,
  revenue: 0,
  newUserCount: 0,
  avgOrderAmount: 0,
  pendingOrderCount: 0,
  stockAlertCount: 0,
  reviewCount: 0
})
const topDishes = ref<any[]>([])
const periodStats = ref<any[]>([])
const categoryRevenueStats = ref<any[]>([])

const getDateStr = (daysAgo: number) => {
  const d = new Date()
  d.setDate(d.getDate() - daysAgo)
  return d.toISOString().slice(0, 10)
}

const getBarHeight = (value: number): number => {
  const maxValue = Math.max(...periodStats.value.map((item: any) => item.revenue || item.periodSales || 0), 1)
  return (value / maxValue) * 100
}

const formatDate = (index: number): string => {
  const d = new Date()
  d.setDate(d.getDate() - (6 - index))
  return `${d.getMonth() + 1}/${d.getDate()}`
}

const getRankClass = (index: number): string => {
  if (index === 0) return 'rank-1'
  if (index === 1) return 'rank-2'
  if (index === 2) return 'rank-3'
  return ''
}

const pieStyle = computed(() => {
  const total = todayStats.value.orderCount || 1
  const completed = Math.round((total * 0.6) * 3.6)
  const delivering = Math.round((total * 0.2) * 3.6)
  const pending = Math.round((total * 0.15) * 3.6)
  const cancelled = Math.round((total * 0.05) * 3.6)
  return {
    background: `conic-gradient(#1B3A2F 0deg ${completed}deg, #2D5A45 ${completed}deg ${completed + delivering}deg, #8B5A2B ${completed + delivering}deg ${completed + delivering + pending}deg, #D9534F ${completed + delivering + pending}deg ${completed + delivering + pending + cancelled}deg)`
  }
})

const lineChartPoints = computed(() => {
  const maxOrderCount = Math.max(...periodStats.value.map((item: any) => item.orderCount || 0), 1)
  const width = 100 / (periodStats.value.length - 1)
  return periodStats.value.map((item: any, index: number) => ({
    x: index * width,
    y: 100 - ((item.orderCount || 0) / maxOrderCount) * 100,
    value: item.orderCount || 0
  }))
})

const linePath = computed(() => {
  if (lineChartPoints.value.length < 2) return ''
  return lineChartPoints.value.map((point, index) => 
    `${index === 0 ? 'M' : 'L'} ${point.x} ${point.y}`
  ).join(' ')
})

const areaPath = computed(() => {
  if (lineChartPoints.value.length < 2) return ''
  const points = lineChartPoints.value
  const lastPoint = points[points.length - 1]
  return `${linePath.value} L ${lastPoint?.x ?? 0} 100 L 0 100 Z`
})

const donutStyle = computed(() => {
  const totalRevenue = categoryRevenueStats.value.reduce((sum, item) => sum + (Number(item.revenue) || 0), 0) || 1
  const colors = ['#1B3A2F', '#2D5A45', '#3D7A5A', '#4D9A6A', '#8B5A2B', '#A06B36']
  let gradient = ''
  let lastAngle = 0
  categoryRevenueStats.value.forEach((item, index) => {
    const percent = (Number(item.revenue) || 0) / totalRevenue
    const angle = Math.round(percent * 360)
    const color = colors[index % colors.length]
    gradient += `${color} ${lastAngle}deg ${lastAngle + angle}deg${index < categoryRevenueStats.value.length - 1 ? ', ' : ''}`
    lastAngle += angle
  })
  return {
    background: `conic-gradient(${gradient})`
  }
})

const totalRevenue = computed(() => {
  return categoryRevenueStats.value.reduce((sum, item) => sum + (Number(item.revenue) || 0), 0)
})

const categoryColors = ['#1B3A2F', '#2D5A45', '#3D7A5A', '#4D9A6A', '#8B5A2B', '#A06B36']

const fetchData = async () => {
  loading.value = true
  try {
    const [todayRes, topRes, dailyRes, categoryRes] = await Promise.all([
      getTodayStats(),
      getTopDishes({ limit: 10 }),
      getDailyStats({ startDate: getDateStr(6), endDate: getDateStr(0) }),
      getCategoryRevenue()
    ])
    if (todayRes.code === 200) {
      todayStats.value = todayRes.data
    }
    if (topRes.code === 200) {
      topDishes.value = topRes.data.list || topRes.data
    }
    if (dailyRes.code === 200) {
      periodStats.value = dailyRes.data || []
    }
    if (categoryRes.code === 200) {
      categoryRevenueStats.value = categoryRes.data || []
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
  <div class="dashboard" v-loading="loading">
    <div class="stats-row">
      <StatCard
        :icon="Money"
        label="今日营收"
        :value="'¥' + (todayStats.revenue || 0)"
        color="#1B3A2F"
      />
      <StatCard
        :icon="ShoppingCart"
        label="今日订单"
        :value="todayStats.orderCount || 0"
        color="#2D5A45"
      />
      <StatCard
        :icon="UserFilled"
        label="新用户数"
        :value="todayStats.newUserCount || 0"
        color="#3D7A5A"
      />
      <StatCard
        :icon="Coin"
        label="平均订单金额"
        :value="'¥' + (todayStats.avgOrderAmount || 0)"
        color="#4D9A6A"
      />
      <StatCard
        :icon="Clock"
        label="待处理订单"
        :value="todayStats.pendingOrderCount || 0"
        color="#8B5A2B"
      />
      <StatCard
        :icon="Warning"
        label="库存预警"
        :value="todayStats.stockAlertCount || 0"
        color="#D9534F"
      />
      <StatCard
        :icon="Message"
        label="今日评价"
        :value="todayStats.reviewCount || 0"
        color="#2D8F5C"
      />
    </div>

    <div class="card-panel-row">
      <div class="card-panel chart-panel">
        <div class="card-header">
          <div class="header-left">
            <Star style="width: 18px; height: 18px; color: var(--bs-primary);" />
            <h3>近7日营收趋势</h3>
          </div>
        </div>
        <div class="chart-container">
          <div class="bar-chart">
            <div 
              v-for="(item, index) in periodStats" 
              :key="index" 
              class="bar-item"
            >
              <div class="bar-wrapper">
                <div 
                  class="bar" 
                  :style="{ height: getBarHeight(item.revenue || item.periodSales || 0) + '%' }"
                ></div>
              </div>
              <div class="bar-label">{{ formatDate(index) }}</div>
              <div class="bar-value">¥{{ item.revenue || item.periodSales || 0 }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="card-panel chart-panel">
        <div class="card-header">
          <h3>订单状态分布</h3>
        </div>
        <div class="chart-container">
          <div class="pie-chart">
            <div class="pie-wrapper">
              <div class="pie" :style="pieStyle"></div>
              <div class="pie-center">
                <div class="pie-total">{{ todayStats.orderCount || 0 }}</div>
                <div class="pie-label">今日订单</div>
              </div>
            </div>
            <div class="pie-legend">
              <div class="legend-item">
                <span class="legend-dot" style="background: #1B3A2F;"></span>
                <span>已完成</span>
              </div>
              <div class="legend-item">
                <span class="legend-dot" style="background: #2D5A45;"></span>
                <span>配送中</span>
              </div>
              <div class="legend-item">
                <span class="legend-dot" style="background: #8B5A2B;"></span>
                <span>待处理</span>
              </div>
              <div class="legend-item">
                <span class="legend-dot" style="background: #D9534F;"></span>
                <span>已取消</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="card-panel chart-panel">
        <div class="card-header">
          <div class="header-left">
            <DataBoard style="width: 18px; height: 18px; color: var(--bs-primary);" />
            <h3>近7日订单量趋势</h3>
          </div>
        </div>
        <div class="chart-container">
          <div class="line-chart">
            <svg viewBox="0 0 100 100" preserveAspectRatio="none">
              <defs>
                <linearGradient id="lineGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                  <stop offset="0%" style="stop-color: var(--bs-primary); stop-opacity: 0.3" />
                  <stop offset="100%" style="stop-color: var(--bs-primary); stop-opacity: 0" />
                </linearGradient>
              </defs>
              <path :d="areaPath" fill="url(#lineGradient)" />
              <path :d="linePath" fill="none" stroke="var(--bs-primary)" stroke-width="0.8" stroke-linecap="round" stroke-linejoin="round" />
              <circle 
                v-for="(point, index) in lineChartPoints" 
                :key="index"
                :cx="point.x" 
                :cy="point.y" 
                r="1.5" 
                fill="var(--bs-primary)"
                class="line-point"
              />
            </svg>
            <div class="line-labels">
              <span v-for="(item, index) in periodStats" :key="index" class="line-label">{{ formatDate(index) }}</span>
            </div>
            <div class="line-values">
              <span v-for="(point, index) in lineChartPoints" :key="index" class="line-value">{{ point.value }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="card-panel chart-panel">
        <div class="card-header">
          <div class="header-left">
            <PieChart style="width: 18px; height: 18px; color: var(--bs-primary);" />
            <h3>菜品分类销售占比</h3>
          </div>
        </div>
        <div class="chart-container">
          <div class="pie-chart">
            <div class="pie-wrapper donut-wrapper">
              <div class="donut" :style="donutStyle"></div>
              <div class="donut-center">
                <div class="donut-total">¥{{ totalRevenue }}</div>
                <div class="donut-label">总营收</div>
              </div>
            </div>
            <div class="pie-legend">
              <div v-for="(item, index) in categoryRevenueStats" :key="index" class="legend-item">
                <span class="legend-dot" :style="{ background: categoryColors[index % categoryColors.length] }"></span>
                <span>{{ item.categoryName }}</span>
                <span class="legend-value">¥{{ item.revenue }}</span>
              </div>
              <div v-if="categoryRevenueStats.length === 0" class="legend-item">
                <span>暂无数据</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="card-panel" style="margin-top: var(--bs-spacing-lg);">
      <div class="card-header">
        <h3>热销菜品排行</h3>
      </div>
      <div style="padding-top: 20px;">
        <el-table :data="topDishes" border>
          <el-table-column type="index" label="排名" width="80">
            <template #default="{ $index }">
              <span :class="getRankClass($index)">{{ $index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="dishName" label="菜品名称" />
          <el-table-column prop="totalQuantity" label="销量" />
          <el-table-column prop="revenue" label="营收">
            <template #default="{ row }">
              ¥{{ row.revenue || 0 }}
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: var(--bs-spacing-lg);
  margin-bottom: var(--bs-spacing-lg);
}

.card-panel-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--bs-spacing-lg);
  margin-bottom: var(--bs-spacing-lg);
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

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-header h3 {
  font-size: var(--bs-font-size-lg);
  font-weight: 600;
  color: var(--bs-text-title);
}

.chart-container {
  height: 200px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.bar-chart {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  width: 100%;
  height: 100%;
  padding-top: 40px;
}

.bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}

.bar-wrapper {
  width: 20px;
  height: 140px;
  background: var(--bs-bg-hover);
  border-radius: 4px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.bar {
  width: 16px;
  background: linear-gradient(180deg, var(--bs-primary) 0%, #2D5A45 100%);
  border-radius: 4px 4px 0 0;
  transition: height 0.5s ease;
}

.bar-label {
  font-size: var(--bs-font-size-xs);
  color: var(--bs-text-muted);
  margin-top: 8px;
}

.bar-value {
  font-size: var(--bs-font-size-xs);
  color: var(--bs-text-title);
  font-weight: 500;
  margin-top: 4px;
}

.line-chart {
  position: relative;
  width: 100%;
  height: 100%;
  padding-top: 30px;
}

.line-chart svg {
  width: 100%;
  height: 140px;
}

.line-point {
  transition: r 0.2s;
}

.line-point:hover {
  r: 2.5;
}

.line-labels {
  display: flex;
  justify-content: space-between;
  padding: 0 5px;
  margin-top: 8px;
}

.line-label {
  font-size: var(--bs-font-size-xs);
  color: var(--bs-text-muted);
}

.line-values {
  display: flex;
  justify-content: space-between;
  padding: 0 5px;
  margin-top: 4px;
}

.line-value {
  font-size: var(--bs-font-size-xs);
  color: var(--bs-text-title);
  font-weight: 500;
}

.pie-chart {
  display: flex;
  align-items: center;
  gap: 20px;
  width: 100%;
  height: 100%;
}

.pie-wrapper {
  position: relative;
  width: 120px;
  height: 120px;
  flex-shrink: 0;
}

.donut-wrapper {
  width: 130px;
  height: 130px;
}

.pie {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  transition: transform 0.5s ease;
}

.donut {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  transition: transform 0.5s ease;
}

.pie-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 70px;
  height: 70px;
  background: var(--bs-card-bg);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.donut-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 75px;
  height: 75px;
  background: var(--bs-card-bg);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.pie-total {
  font-size: var(--bs-font-size-xl);
  font-weight: 700;
  color: var(--bs-text-title);
}

.donut-total {
  font-size: var(--bs-font-size-lg);
  font-weight: 700;
  color: var(--bs-text-title);
}

.pie-label {
  font-size: var(--bs-font-size-xs);
  color: var(--bs-text-muted);
}

.donut-label {
  font-size: var(--bs-font-size-xs);
  color: var(--bs-text-muted);
}

.pie-legend {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.legend-value {
  margin-left: auto;
  font-size: var(--bs-font-size-xs);
  color: var(--bs-text-title);
  font-weight: 500;
}

.rank-1 {
  color: #D9534F;
  font-weight: 700;
}

.rank-2 {
  color: #8B5A2B;
  font-weight: 700;
}

.rank-3 {
  color: #2D5A45;
  font-weight: 700;
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

.btn-sm {
  padding: var(--bs-spacing-xs) var(--bs-spacing-md);
  font-size: var(--bs-font-size-sm);
}

@media (max-width: 1400px) {
  .stats-row {
    grid-template-columns: repeat(4, 1fr);
  }
  .card-panel-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(3, 1fr);
  }
  .card-panel-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>