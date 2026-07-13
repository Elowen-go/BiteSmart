<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Money,
  ShoppingCart,
  UserFilled,
  Coin,
  Clock,
  Warning,
  Message,
  Star,
  DataBoard,
  PieChart,
  Box,
  Bowl
} from '@element-plus/icons-vue'
import { getTodayStats, getTopDishes, getDailyStats, getCategoryRevenue } from '../../api/merchant/statistics'

const router = useRouter()
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
  const date = new Date()
  date.setDate(date.getDate() - daysAgo)
  return date.toISOString().slice(0, 10)
}

const formatShortDate = (dateText: string) => {
  if (!dateText) {
    return ''
  }
  const date = new Date(dateText)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

const formatCurrency = (value: number) => {
  return `¥${Number(value || 0).toLocaleString()}`
}

const weekStats = computed(() => {
  if (!periodStats.value.length) {
    return Array.from({ length: 7 }, (_, index) => ({
      label: formatShortDate(getDateStr(6 - index)),
      revenue: 0,
      orderCount: 0
    }))
  }

  return periodStats.value.map((item: any, index: number) => ({
    label: formatShortDate(item.date || item.statDate || getDateStr(6 - index)),
    revenue: Number(item.revenue || item.periodSales || 0),
    orderCount: Number(item.orderCount || 0)
  }))
})

const maxRevenue = computed(() => {
  return Math.max(...weekStats.value.map((item) => item.revenue), 1)
})

const maxOrderCount = computed(() => {
  return Math.max(...weekStats.value.map((item) => item.orderCount), 1)
})

const lineChartPoints = computed(() => {
  if (weekStats.value.length === 1) {
    const singleStat = weekStats.value[0]
    return singleStat ? [{ x: 50, y: 50, value: singleStat.orderCount }] : []
  }

  const width = 100 / Math.max(weekStats.value.length - 1, 1)
  return weekStats.value.map((item, index) => ({
    x: index * width,
    y: 100 - (item.orderCount / maxOrderCount.value) * 100,
    value: item.orderCount
  }))
})

const linePath = computed(() => {
  if (lineChartPoints.value.length < 2) {
    return ''
  }

  return lineChartPoints.value
    .map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x} ${point.y}`)
    .join(' ')
})

const areaPath = computed(() => {
  if (lineChartPoints.value.length < 2) {
    return ''
  }

  const lastPoint = lineChartPoints.value[lineChartPoints.value.length - 1]
  if (!lastPoint) {
    return ''
  }
  return `${linePath.value} L ${lastPoint.x} 100 L 0 100 Z`
})

const categoryColors = ['#1b5e4a', '#2d7a61', '#4b9b72', '#80b88a', '#d3934a', '#da5f52']

const totalCategoryRevenue = computed(() => {
  return categoryRevenueStats.value.reduce((sum, item) => sum + Number(item.revenue || 0), 0)
})

const categorySegments = computed(() => {
  const total = totalCategoryRevenue.value || 1
  let start = 0

  return categoryRevenueStats.value.map((item, index) => {
    const revenue = Number(item.revenue || 0)
    const percent = revenue / total
    const end = start + percent * 360
    const segment = {
      ...item,
      revenue,
      percent,
      color: categoryColors[index % categoryColors.length],
      start,
      end
    }
    start = end
    return segment
  })
})

const donutStyle = computed(() => {
  if (!categorySegments.value.length) {
    return {
      background: 'conic-gradient(#dfe8e3 0deg 360deg)'
    }
  }

  return {
    background: `conic-gradient(${categorySegments.value
      .map((segment) => `${segment.color} ${segment.start}deg ${segment.end}deg`)
      .join(', ')})`
  }
})

const comboRevenue = computed(() => {
  return categoryRevenueStats.value.reduce((sum, item) => {
    const name = String(item.categoryName || '')
    if (name.includes('套餐') || name.includes('组合')) {
      return sum + Number(item.revenue || 0)
    }
    return sum
  }, 0)
})

const comboRevenueRate = computed(() => {
  if (!totalCategoryRevenue.value) {
    return 0
  }
  return Math.round((comboRevenue.value / totalCategoryRevenue.value) * 100)
})

const reviewHealth = computed(() => {
  if (todayStats.value.reviewCount === 0) {
    return '今日评价较少，适合主动引导用户反馈口味和包装体验。'
  }
  if (todayStats.value.reviewCount >= 8) {
    return '评价活跃度不错，建议优先处理新评价，保持口碑滚动。'
  }
  return '评价数量稳定，适合把高频好评菜品做成首页推荐。'
})

const priorityList = computed(() => [
  {
    title: '待处理订单',
    value: `${todayStats.value.pendingOrderCount || 0} 单`,
    hint: todayStats.value.pendingOrderCount ? '优先确认出餐节奏' : '当前出餐压力平稳',
    tone: 'warning'
  },
  {
    title: '库存预警',
    value: `${todayStats.value.stockAlertCount || 0} 项`,
    hint: todayStats.value.stockAlertCount ? '建议尽快补货或调整套餐' : '库存状态健康',
    tone: todayStats.value.stockAlertCount ? 'danger' : 'success'
  },
  {
    title: '今日评价',
    value: `${todayStats.value.reviewCount || 0} 条`,
    hint: todayStats.value.reviewCount ? '记得及时回复用户反馈' : '可引导首批订单用户评价',
    tone: 'neutral'
  }
])

const coreMetrics = computed(() => [
  {
    label: '今日营收',
    value: formatCurrency(todayStats.value.revenue),
    note: '经营结果',
    icon: Money,
    tone: 'emerald'
  },
  {
    label: '今日订单',
    value: `${todayStats.value.orderCount || 0}`,
    note: '交易规模',
    icon: ShoppingCart,
    tone: 'forest'
  },
  {
    label: '新用户数',
    value: `${todayStats.value.newUserCount || 0}`,
    note: '拉新表现',
    icon: UserFilled,
    tone: 'mint'
  },
  {
    label: '平均客单价',
    value: formatCurrency(todayStats.value.avgOrderAmount),
    note: '客单结构',
    icon: Coin,
    tone: 'gold'
  }
])

const businessSignals = computed(() => [
  {
    title: '套餐营收占比',
    value: `${comboRevenueRate.value}%`,
    detail: comboRevenue.value ? `${formatCurrency(comboRevenue.value)} 来自套餐销售` : '暂无套餐类营收数据',
    icon: Box
  },
  {
    title: '菜品分类活跃度',
    value: `${categoryRevenueStats.value.length || 0}`,
    detail: categoryRevenueStats.value.length ? '有成交的分类数量' : '暂无分类成交数据',
    icon: PieChart
  },
  {
    title: '热销菜品覆盖',
    value: `${topDishes.value.length || 0}`,
    detail: topDishes.value.length ? `榜首菜品：${topDishes.value[0]?.dishName || '暂无'}` : '今日还没有形成热销榜',
    icon: Bowl
  }
])

const topDishSummary = computed(() => {
  if (!topDishes.value.length) {
    return null
  }

  const topDish = topDishes.value[0]
  return {
    name: topDish.dishName || '招牌菜品',
    quantity: Number(topDish.totalQuantity || 0),
    revenue: Number(topDish.revenue || 0)
  }
})

const topCategory = computed(() => {
  if (!categorySegments.value.length) {
    return null
  }
  return [...categorySegments.value].sort((a, b) => b.revenue - a.revenue)[0]
})

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
      topDishes.value = topRes.data.list || topRes.data || []
    }

    if (dailyRes.code === 200) {
      periodStats.value = dailyRes.data || []
    }

    if (categoryRes.code === 200) {
      categoryRevenueStats.value = categoryRes.data || []
    }
  } catch (error) {
    console.error('获取统计数据失败', error)
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
    <section class="hero-board">
      <div class="hero-main">
        <p class="hero-eyebrow">轻食经营总览</p>
        <h2>把出餐、库存和套餐表现放在同一张工作台上。</h2>
        <p class="hero-copy">
          今天的首页优先关注待处理订单、库存异常和套餐销售占比，让商家先看到最该处理的事。
        </p>
        <div class="hero-actions">
          <button class="hero-btn primary" @click="router.push('/merchant/orders')">
            处理订单
          </button>
          <button class="hero-btn secondary" @click="router.push('/merchant/combos')">
            调整套餐
          </button>
        </div>
        <div class="hero-tags">
          <span class="hero-tag">今日订单 {{ todayStats.orderCount || 0 }}</span>
          <span class="hero-tag">库存预警 {{ todayStats.stockAlertCount || 0 }}</span>
          <span class="hero-tag">新客 {{ todayStats.newUserCount || 0 }}</span>
        </div>
      </div>

      <div class="hero-side">
        <div
          v-for="item in priorityList"
          :key="item.title"
          class="priority-card"
          :class="item.tone"
        >
          <div class="priority-label">{{ item.title }}</div>
          <div class="priority-value">{{ item.value }}</div>
          <div class="priority-hint">{{ item.hint }}</div>
        </div>
      </div>
    </section>

    <section class="metric-grid">
      <article
        v-for="metric in coreMetrics"
        :key="metric.label"
        class="metric-card"
        :class="metric.tone"
      >
        <div class="metric-top">
          <div class="metric-icon">
            <component :is="metric.icon" />
          </div>
          <span class="metric-note">{{ metric.note }}</span>
        </div>
        <div class="metric-label">{{ metric.label }}</div>
        <div class="metric-value">{{ metric.value }}</div>
      </article>
    </section>

    <section class="insight-grid">
      <article class="panel">
        <div class="panel-header">
          <div class="panel-title">
            <Clock />
            <h3>经营信号</h3>
          </div>
        </div>
        <div class="signal-list">
          <div v-for="item in businessSignals" :key="item.title" class="signal-item">
            <div class="signal-icon">
              <component :is="item.icon" />
            </div>
            <div class="signal-body">
              <div class="signal-top">
                <span>{{ item.title }}</span>
                <strong>{{ item.value }}</strong>
              </div>
              <p>{{ item.detail }}</p>
            </div>
          </div>
        </div>
      </article>

      <article class="panel spotlight">
        <div class="panel-header">
          <div class="panel-title">
            <Star />
            <h3>今日主推建议</h3>
          </div>
        </div>
        <div class="spotlight-content">
          <div class="spotlight-block">
            <span class="spotlight-label">口碑反馈</span>
            <p>{{ reviewHealth }}</p>
          </div>
          <div class="spotlight-block" v-if="topDishSummary">
            <span class="spotlight-label">热销菜品</span>
            <p>{{ topDishSummary.name }} 今日售出 {{ topDishSummary.quantity }} 份，带来 {{ formatCurrency(topDishSummary.revenue) }}。</p>
          </div>
          <div class="spotlight-block" v-else>
            <span class="spotlight-label">热销菜品</span>
            <p>今天还没有形成热销榜，可以优先把招牌套餐放到更显眼的位置。</p>
          </div>
          <div class="spotlight-block" v-if="topCategory">
            <span class="spotlight-label">分类表现</span>
            <p>{{ topCategory.categoryName }} 当前贡献最高，占分类营收 {{ Math.round(topCategory.percent * 100) }}%。</p>
          </div>
        </div>
      </article>
    </section>

    <section class="visual-grid">
      <article class="panel">
        <div class="panel-header">
          <div class="panel-title">
            <Money />
            <h3>近 7 日营收走势</h3>
          </div>
          <span class="panel-meta">{{ formatCurrency(todayStats.revenue) }} / 今日</span>
        </div>
        <div class="bar-chart" v-if="weekStats.length">
          <div v-for="item in weekStats" :key="item.label" class="bar-item">
            <div class="bar-track">
              <div class="bar-fill" :style="{ height: `${(item.revenue / maxRevenue) * 100}%` }"></div>
            </div>
            <span class="bar-label">{{ item.label }}</span>
            <strong class="bar-value">{{ formatCurrency(item.revenue) }}</strong>
          </div>
        </div>
      </article>

      <article class="panel">
        <div class="panel-header">
          <div class="panel-title">
            <PieChart />
            <h3>分类营收结构</h3>
          </div>
          <span class="panel-meta">{{ formatCurrency(totalCategoryRevenue) }}</span>
        </div>
        <div class="structure-panel">
          <div class="donut-wrapper">
            <div class="donut" :style="donutStyle"></div>
            <div class="donut-center">
              <strong>{{ formatCurrency(totalCategoryRevenue) }}</strong>
              <span>分类总营收</span>
            </div>
          </div>
          <div class="legend-list" v-if="categorySegments.length">
            <div v-for="item in categorySegments" :key="item.categoryName" class="legend-item">
              <span class="legend-dot" :style="{ background: item.color }"></span>
              <span class="legend-name">{{ item.categoryName }}</span>
              <span class="legend-percent">{{ Math.round(item.percent * 100) }}%</span>
            </div>
          </div>
          <div v-else class="empty-state compact">
            <p>还没有分类营收数据</p>
            <span>有成交后会自动显示分类结构</span>
          </div>
        </div>
      </article>
    </section>

    <section class="visual-grid lower">
      <article class="panel">
        <div class="panel-header">
          <div class="panel-title">
            <DataBoard />
            <h3>近 7 日订单趋势</h3>
          </div>
          <span class="panel-meta">{{ todayStats.pendingOrderCount || 0 }} 单待处理</span>
        </div>
        <div class="line-chart">
          <svg viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">
            <defs>
              <linearGradient id="merchant-line-gradient" x1="0%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stop-color="#2d7a61" stop-opacity="0.35" />
                <stop offset="100%" stop-color="#2d7a61" stop-opacity="0" />
              </linearGradient>
            </defs>
            <path v-if="areaPath" :d="areaPath" fill="url(#merchant-line-gradient)" />
            <path v-if="linePath" :d="linePath" fill="none" stroke="#1b5e4a" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round" />
            <circle
              v-for="(point, index) in lineChartPoints"
              :key="index"
              :cx="point.x"
              :cy="point.y"
              r="1.8"
              fill="#1b5e4a"
            />
          </svg>
          <div class="line-axis">
            <div v-for="item in weekStats" :key="item.label" class="axis-item">
              <span>{{ item.label }}</span>
              <strong>{{ item.orderCount }}</strong>
            </div>
          </div>
        </div>
      </article>

      <article class="panel">
        <div class="panel-header">
          <div class="panel-title">
            <Message />
            <h3>热销菜品排行</h3>
          </div>
          <span class="panel-meta">{{ topDishes.length }} 道上榜</span>
        </div>
        <div v-if="topDishes.length" class="top-dish-list">
          <div v-for="(item, index) in topDishes.slice(0, 5)" :key="item.dishName || index" class="top-dish-item">
            <div class="dish-rank">{{ index + 1 }}</div>
            <div class="dish-body">
              <div class="dish-name">{{ item.dishName }}</div>
              <div class="dish-meta">
                <span>销量 {{ item.totalQuantity || 0 }}</span>
                <span>{{ formatCurrency(item.revenue || 0) }}</span>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="empty-state">
          <p>今天还没有热销菜品数据</p>
          <span>首批订单产生后，这里会自动给出销量排行</span>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 24px;
  animation: fadeIn 0.28s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.hero-board {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(320px, 0.9fr);
  gap: 20px;
}

.hero-main,
.hero-side,
.panel,
.metric-card {
  border-radius: 8px;
}

.hero-main {
  background:
    radial-gradient(circle at top left, rgba(122, 177, 141, 0.28), transparent 40%),
    linear-gradient(135deg, #173c30 0%, #204e3e 60%, #2d6b52 100%);
  color: #fff;
  padding: 28px 30px;
  min-height: 240px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-shadow: 0 14px 30px rgba(27, 58, 47, 0.18);
}

.hero-eyebrow {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: rgba(255, 255, 255, 0.72);
  margin-bottom: 12px;
}

.hero-main h2 {
  font-size: 30px;
  line-height: 1.22;
  margin: 0;
  max-width: 14ch;
}

.hero-copy {
  max-width: 60ch;
  margin: 14px 0 0;
  color: rgba(255, 255, 255, 0.82);
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.hero-btn {
  border: none;
  border-radius: 999px;
  height: 40px;
  padding: 0 18px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.18s ease, opacity 0.18s ease, background 0.18s ease;
}

.hero-btn:hover {
  transform: translateY(-1px);
}

.hero-btn.primary {
  background: #f3fbf6;
  color: #173c30;
}

.hero-btn.secondary {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.18);
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 22px;
}

.hero-tag {
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.14);
  color: rgba(255, 255, 255, 0.86);
  padding: 7px 12px;
  border-radius: 999px;
  font-size: 13px;
}

.hero-side {
  display: grid;
  gap: 14px;
}

.priority-card {
  background: #fff;
  padding: 18px 18px 16px;
  border: 1px solid #edf1ee;
  box-shadow: 0 10px 24px rgba(31, 54, 44, 0.05);
}

.priority-card.warning {
  border-left: 4px solid #d3934a;
}

.priority-card.danger {
  border-left: 4px solid #da5f52;
}

.priority-card.success {
  border-left: 4px solid #2d7a61;
}

.priority-card.neutral {
  border-left: 4px solid #88a89b;
}

.priority-label {
  color: var(--bs-text-muted);
  font-size: 13px;
}

.priority-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 700;
  color: var(--bs-text-title);
}

.priority-hint {
  margin-top: 8px;
  line-height: 1.6;
  color: var(--bs-text-body);
  font-size: 13px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.metric-card {
  background: #fff;
  padding: 20px;
  border: 1px solid #edf1ee;
  box-shadow: 0 8px 24px rgba(28, 46, 39, 0.05);
}

.metric-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.metric-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.metric-icon :deep(svg) {
  width: 20px;
  height: 20px;
}

.metric-card.emerald .metric-icon {
  background: #ebf7f1;
  color: #1b5e4a;
}

.metric-card.forest .metric-icon {
  background: #edf5f0;
  color: #2d7a61;
}

.metric-card.mint .metric-icon {
  background: #f0faf5;
  color: #4b9b72;
}

.metric-card.gold .metric-icon {
  background: #fff4e6;
  color: #d3934a;
}

.metric-note {
  font-size: 12px;
  color: var(--bs-text-muted);
}

.metric-label {
  margin-top: 18px;
  font-size: 14px;
  color: var(--bs-text-body);
}

.metric-value {
  margin-top: 8px;
  font-size: 34px;
  line-height: 1.1;
  font-weight: 700;
  color: var(--bs-text-title);
}

.insight-grid,
.visual-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.panel {
  background: #fff;
  padding: 22px 24px;
  border: 1px solid #edf1ee;
  box-shadow: 0 8px 24px rgba(28, 46, 39, 0.05);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.panel-title :deep(svg) {
  width: 18px;
  height: 18px;
  color: #1b5e4a;
}

.panel-title h3 {
  margin: 0;
  font-size: 18px;
  color: var(--bs-text-title);
}

.panel-meta {
  font-size: 12px;
  color: var(--bs-text-muted);
}

.signal-list {
  display: grid;
  gap: 14px;
}

.signal-item {
  display: flex;
  gap: 14px;
  padding: 14px;
  border-radius: 8px;
  background: #f7faf8;
}

.signal-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: #e6f2ec;
  color: #1b5e4a;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.signal-icon :deep(svg) {
  width: 18px;
  height: 18px;
}

.signal-body {
  min-width: 0;
  flex: 1;
}

.signal-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 14px;
  color: var(--bs-text-title);
}

.signal-top strong {
  font-size: 18px;
}

.signal-body p {
  margin: 6px 0 0;
  color: var(--bs-text-muted);
  line-height: 1.6;
  font-size: 13px;
}

.spotlight {
  background:
    linear-gradient(180deg, rgba(232, 245, 236, 0.9), #ffffff 48%);
}

.spotlight-content {
  display: grid;
  gap: 16px;
}

.spotlight-block {
  padding: 16px 18px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid #e7efe9;
}

.spotlight-label {
  display: inline-block;
  margin-bottom: 8px;
  font-size: 12px;
  color: #2d7a61;
  font-weight: 600;
}

.spotlight-block p {
  margin: 0;
  color: var(--bs-text-body);
  line-height: 1.7;
}

.bar-chart {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 12px;
  align-items: end;
  min-height: 240px;
}

.bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.bar-track {
  width: 100%;
  max-width: 34px;
  height: 150px;
  border-radius: 999px;
  background: #edf3ef;
  display: flex;
  align-items: end;
  overflow: hidden;
}

.bar-fill {
  width: 100%;
  min-height: 6px;
  border-radius: 999px;
  background: linear-gradient(180deg, #5fb386 0%, #1b5e4a 100%);
}

.bar-label,
.bar-value {
  font-size: 12px;
}

.bar-label {
  color: var(--bs-text-muted);
}

.bar-value {
  color: var(--bs-text-title);
}

.structure-panel {
  min-height: 240px;
  display: flex;
  align-items: center;
  gap: 24px;
}

.donut-wrapper {
  position: relative;
  width: 170px;
  height: 170px;
  flex-shrink: 0;
}

.donut {
  width: 100%;
  height: 100%;
  border-radius: 50%;
}

.donut-center {
  position: absolute;
  inset: 22px;
  background: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  text-align: center;
  box-shadow: inset 0 0 0 1px #eef2ef;
}

.donut-center strong {
  font-size: 20px;
  color: var(--bs-text-title);
}

.donut-center span {
  margin-top: 4px;
  font-size: 12px;
  color: var(--bs-text-muted);
}

.legend-list {
  display: grid;
  gap: 12px;
  width: 100%;
}

.legend-item {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.legend-name,
.legend-percent {
  font-size: 13px;
}

.legend-name {
  color: var(--bs-text-body);
}

.legend-percent {
  color: var(--bs-text-title);
  font-weight: 600;
}

.line-chart {
  min-height: 240px;
  display: flex;
  flex-direction: column;
}

.line-chart svg {
  width: 100%;
  height: 170px;
}

.line-axis {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
  margin-top: 10px;
}

.axis-item {
  text-align: center;
}

.axis-item span {
  display: block;
  font-size: 12px;
  color: var(--bs-text-muted);
}

.axis-item strong {
  display: block;
  margin-top: 6px;
  color: var(--bs-text-title);
}

.top-dish-list {
  display: grid;
  gap: 12px;
  min-height: 240px;
}

.top-dish-item {
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 14px 16px;
  border-radius: 8px;
  background: #f8fbf9;
}

.dish-rank {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #1b5e4a;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  flex-shrink: 0;
}

.dish-body {
  min-width: 0;
  flex: 1;
}

.dish-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--bs-text-title);
}

.dish-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 6px;
  font-size: 13px;
  color: var(--bs-text-muted);
}

.empty-state {
  min-height: 240px;
  border: 1px dashed #d6e3da;
  border-radius: 8px;
  background: #f9fcfa;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  text-align: center;
  padding: 20px;
}

.empty-state.compact {
  min-height: 180px;
  flex: 1;
}

.empty-state p {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--bs-text-title);
}

.empty-state span {
  margin-top: 8px;
  font-size: 13px;
  color: var(--bs-text-muted);
  line-height: 1.6;
}

@media (max-width: 1440px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1200px) {
  .hero-board,
  .insight-grid,
  .visual-grid {
    grid-template-columns: 1fr;
  }

  .hero-main h2 {
    max-width: none;
  }
}

@media (max-width: 768px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }

  .hero-main,
  .panel,
  .metric-card {
    padding: 18px;
  }

  .hero-actions,
  .hero-tags {
    flex-direction: column;
    align-items: stretch;
  }

  .bar-chart,
  .line-axis {
    gap: 6px;
  }

  .structure-panel {
    flex-direction: column;
    align-items: flex-start;
  }

  .donut-wrapper {
    width: 148px;
    height: 148px;
  }
}
</style>
