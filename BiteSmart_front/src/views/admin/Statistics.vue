<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getOverview, getTrend } from '../../api/admin/statistics'

const loading = ref(true)
const trendLoading = ref(false)
const range = ref<'day' | 'week' | 'month'>('day')
const overview = ref({ userCount: 0, merchantCount: 0, orderCount: 0, totalRevenue: 0 })
const trend = ref({ orderCount: 0, revenue: 0, startTime: '', endTime: '' })

const cards = computed(() => [
  { label: '平台用户', value: overview.value.userCount, suffix: '人', tone: 'green' },
  { label: '入驻商家', value: overview.value.merchantCount, suffix: '家', tone: 'blue' },
  { label: '累计订单', value: overview.value.orderCount, suffix: '笔', tone: 'orange' },
  { label: '累计营收', value: Number(overview.value.totalRevenue || 0).toFixed(2), suffix: '元', tone: 'red' }
])

const rangeLabel = computed(() => ({ day: '近24小时', week: '近7天', month: '近30天' }[range.value]))

const loadOverview = async () => {
  const response = await getOverview()
  if (response.code === 200 && response.data) overview.value = response.data
}

const loadTrend = async () => {
  trendLoading.value = true
  try {
    const response = await getTrend(range.value)
    if (response.code === 200 && response.data) trend.value = response.data
  } finally {
    trendLoading.value = false
  }
}

const loadData = async () => {
  loading.value = true
  try {
    await Promise.all([loadOverview(), loadTrend()])
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-container" v-loading="loading">
    <div class="stats-grid">
      <div v-for="card in cards" :key="card.label" class="stat-card" :class="card.tone">
        <span class="stat-label">{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
        <span class="stat-suffix">{{ card.suffix }}</span>
      </div>
    </div>

    <section class="panel trend-panel" v-loading="trendLoading">
      <div class="panel-header">
        <div>
          <h3>平台经营趋势</h3>
          <p>{{ rangeLabel }}订单和营收概览</p>
        </div>
        <el-radio-group v-model="range" size="small" @change="loadTrend">
          <el-radio-button label="day">日</el-radio-button>
          <el-radio-button label="week">周</el-radio-button>
          <el-radio-button label="month">月</el-radio-button>
        </el-radio-group>
      </div>

      <div class="trend-summary">
        <div>
          <span>订单量</span>
          <strong>{{ trend.orderCount }} <small>笔</small></strong>
        </div>
        <div>
          <span>营收</span>
          <strong>¥{{ Number(trend.revenue || 0).toFixed(2) }}</strong>
        </div>
        <div class="period">
          {{ trend.startTime?.slice(0, 10) || '-' }} 至 {{ trend.endTime?.slice(0, 10) || '-' }}
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.page-container { animation: fadeIn .25s ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
.stats-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; margin-bottom: 16px; }
.stat-card { min-height: 112px; padding: 20px; border: 1px solid var(--bs-border-light); border-left: 4px solid var(--tone); border-radius: 8px; background: #fff; box-shadow: var(--bs-card-shadow); }
.stat-card.green { --tone: #4d9b72; } .stat-card.blue { --tone: #5d86b3; } .stat-card.orange { --tone: #c58a4a; } .stat-card.red { --tone: #bd6b67; }
.stat-label { display: block; color: var(--bs-text-muted); font-size: 13px; margin-bottom: 14px; }
.stat-card strong { color: var(--bs-text-title); font-size: 28px; line-height: 1; }
.stat-suffix { color: var(--bs-text-muted); margin-left: 6px; font-size: 13px; }
.panel { background: #fff; border: 1px solid var(--bs-border-light); border-radius: 8px; padding: 20px; box-shadow: var(--bs-card-shadow); }
.panel-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.panel-header h3 { margin: 0; color: var(--bs-text-title); font-size: 17px; } .panel-header p { margin: 6px 0 0; color: var(--bs-text-muted); font-size: 13px; }
.trend-summary { display: flex; align-items: flex-end; gap: 64px; margin-top: 40px; padding-top: 18px; border-top: 1px solid var(--bs-border-light); }
.trend-summary span { display: block; color: var(--bs-text-muted); font-size: 13px; margin-bottom: 8px; }
.trend-summary strong { color: var(--bs-text-title); font-size: 26px; } .trend-summary small { font-size: 13px; font-weight: 400; }
.trend-summary .period { margin-left: auto; color: var(--bs-text-muted); font-size: 13px; }
@media (max-width: 900px) { .stats-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 560px) { .stats-grid { grid-template-columns: 1fr; } .trend-summary { flex-wrap: wrap; gap: 24px; } .trend-summary .period { margin-left: 0; width: 100%; } }
</style>
