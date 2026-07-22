<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { use, init, type ECharts } from 'echarts/core'
import { LineChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getTodayStats, getDailyStats, getTopDishes } from '../../api/merchant/statistics'
import { getOrderList, getOrderDetail, acceptOrder, rejectOrder } from '../../api/merchant/orders'
import { getDishList } from '../../api/merchant/dishes'
import { getReviewList } from '../../api/merchant/reviews'
import { LOW_STOCK_THRESHOLD } from '../../constants/merchant'

use([LineChart, BarChart, GridComponent, TooltipComponent, CanvasRenderer])

const router = useRouter()

/** 待接单 / 出餐中（备餐中）订单状态 */
const STATUS_PENDING = 20
const STATUS_PREPARING = 30
/** 待办中心拉取的订单页大小（前端按状态过滤，无状态筛选参数） */
const ORDER_SCAN_SIZE = 50

const loading = ref(false)
const todayStats = ref<any>(null)
const dailyStats = ref<any[]>([])
const pendingOrders = ref<any[]>([])
const preparingCount = ref<number | null>(null)
const lowStocks = ref<any[]>([])
const latestReviews = ref<any[]>([])
const topDishes = ref<any[]>([])
const opLoading = ref<Record<string, boolean>>({})

/* ---------------- 格式化工具 ---------------- */
const getDateStr = (daysAgo: number) => {
  const d = new Date()
  d.setDate(d.getDate() - daysAgo)
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${month}-${day}`
}

const fmtMoney = (value: any, digits = 2) =>
  Number(value || 0).toLocaleString('en-US', { minimumFractionDigits: digits, maximumFractionDigits: digits })

const todayLabel = computed(() => {
  const now = new Date()
  const week = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][now.getDay()]
  return `${String(now.getMonth() + 1).padStart(2, '0')} / ${String(now.getDate()).padStart(2, '0')} ${week}`
})

const parseTime = (value?: string) => {
  if (!value) return null
  const t = new Date(String(value).replace(' ', 'T')).getTime()
  return Number.isNaN(t) ? null : t
}

const fromNow = (value?: string) => {
  const t = parseTime(value)
  if (!t) return '-'
  const diff = Math.max(0, Date.now() - t)
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} 小时前`
  return `${Math.floor(hours / 24)} 天前`
}

const maskName = (name?: string) => {
  if (!name) return '顾客'
  return `${name[0]}**`
}

const shortOrderNo = (orderNo?: string) => {
  if (!orderNo) return '#----'
  return `#${String(orderNo).slice(-4)}`
}

/* ---------------- 经营概览主卡 ---------------- */
const trendData = computed(() => {
  const list = dailyStats.value || []
  return list.map((item: any, index: number) => ({
    label: formatShortDate(item.date || item.statDate || getDateStr(list.length - 1 - index)),
    revenue: Number(item.revenue || 0),
    orderCount: Number(item.orderCount || 0)
  }))
})

const formatShortDate = (dateText: string) => {
  if (!dateText) return ''
  const parts = String(dateText).split('-')
  if (parts.length >= 3) return `${Number(parts[1])}/${Number(parts[2])}`
  return dateText
}

const todayRevenue = computed(() => (todayStats.value ? Number(todayStats.value.revenue || 0) : null))

const yesterdayRevenue = computed(() => {
  const list = dailyStats.value || []
  if (!list.length) return null
  const yesterday = getDateStr(1)
  const hit = list.find((item: any) => String(item.date || item.statDate || '') === yesterday)
  if (hit) return Number(hit.revenue || 0)
  // 接口未覆盖昨天时，退化为倒数第二条
  return list.length >= 2 ? Number(list[list.length - 2]?.revenue || 0) : null
})

const weekRevenue = computed(() => {
  const list = dailyStats.value || []
  if (!list.length) return null
  return list.reduce((sum: number, item: any) => sum + Number(item.revenue || 0), 0)
})

// 环比：今日营收 vs 昨日营收；昨日缺失或为 0 时不展示（防御）
const revenueDelta = computed(() => {
  if (todayRevenue.value === null || !yesterdayRevenue.value) return null
  return ((todayRevenue.value - yesterdayRevenue.value) / yesterdayRevenue.value) * 100
})

/* ---------------- 指标带 ---------------- */
const metricItems = computed(() => [
  { en: '今日订单 ORDERS', value: todayStats.value ? String(todayStats.value.orderCount ?? 0) : '-', unit: '单' },
  { en: '出餐中 PREPARING', value: preparingCount.value === null ? '-' : String(preparingCount.value), unit: '单' },
  { en: '新客 NEW CUSTOMERS', value: todayStats.value ? String(todayStats.value.newUserCount ?? 0) : '-', unit: '人' },
  { en: '客单价 AVG. ORDER', value: todayStats.value ? `¥${fmtMoney(todayStats.value.avgOrderAmount, 1)}` : '-', unit: '' }
])

/* ---------------- 待办中心 ---------------- */
const todoList = computed(() =>
  pendingOrders.value.map((order: any) => ({
    raw: order,
    no: shortOrderNo(order.orderNo),
    time: fromNow(order.createTime),
    summary: order.itemsSummary || '订单商品',
    buyer: maskName(order.buyerName || order.receiverName),
    address: order.deliveryAddress || '',
    remark: order.remark || '',
    amount: fmtMoney(order.payAmount ?? order.totalAmount)
  }))
)

const enrichAndSetPending = async (list: any[]) => {
  const pending = list
    .filter((item: any) => item.orderStatus === STATUS_PENDING)
    .slice(0, 3)
  const enriched = await Promise.all(
    pending.map(async (order: any) => {
      try {
        const res = await getOrderDetail(order.id)
        const items = res.data?.items || []
        const buyer = res.data?.buyer
        return {
          ...order,
          itemsSummary: items.length
            ? items.map((it: any) => `${it.snapshotName || '商品'} ×${it.quantity || 1}`).join(' · ')
            : '',
          buyerName: buyer?.nickname || buyer?.username || order.receiverName
        }
      } catch {
        return { ...order }
      }
    })
  )
  pendingOrders.value = enriched
}

const handleAccept = async (order: any) => {
  const key = String(order.id)
  if (opLoading.value[key]) return
  opLoading.value[key] = true
  try {
    // 雪花 id 字符串透传，禁止 Number() 强转
    const res = await acceptOrder(order.id)
    if (res.code === 200) {
      ElMessage.success(`已接单 ${order.orderNo || ''}`)
      await refreshAfterAction()
    } else {
      ElMessage.error(res.message || '接单失败')
    }
  } catch {
    ElMessage.error('接单失败，请稍后重试')
  } finally {
    opLoading.value[key] = false
  }
}

const handleReject = (order: any) => {
  ElMessageBox.prompt('请输入拒绝原因', `拒绝订单 ${order.orderNo || ''}`, {
    confirmButtonText: '确定拒单',
    cancelButtonText: '取消',
    inputPlaceholder: '例如：食材售罄 / 超出配送范围'
  }).then(async ({ value }) => {
    const key = String(order.id)
    opLoading.value[key] = true
    try {
      const res = await rejectOrder(order.id, value || '')
      if (res.code === 200) {
        ElMessage.success('已拒单')
        await refreshAfterAction()
      } else {
        ElMessage.error(res.message || '拒单失败')
      }
    } catch {
      ElMessage.error('拒单失败，请稍后重试')
    } finally {
      opLoading.value[key] = false
    }
  }).catch(() => {})
}

const refreshAfterAction = async () => {
  await Promise.allSettled([loadOrders(), loadTodayStats()])
}

/* ---------------- 库存预警 ---------------- */
// stock ≤ LOW_STOCK_THRESHOLD 升序取前 3
const stockList = computed(() =>
  lowStocks.value.map((dish: any) => {
    const stock = Number(dish.stock ?? 0)
    return {
      name: dish.dishName || '未命名菜品',
      sub: dish.salesReal !== undefined && dish.salesReal !== null
        ? `累计已售 ${dish.salesReal} 份`
        : `库存不高于 ${LOW_STOCK_THRESHOLD} 份`,
      stock,
      width: `${Math.max(6, Math.min(100, Math.round((stock / LOW_STOCK_THRESHOLD) * 100)))}%`
    }
  })
)

/* ---------------- 最新评价 ---------------- */
const reviewList = computed(() =>
  latestReviews.value.map((review: any) => {
    const rating = Math.round(Number(review.overallRating ?? review.rating ?? 0))
    const safeRating = Math.max(0, Math.min(5, rating))
    const name = review.isAnonymous === 1
      ? '匿名用户'
      : review.userNickname || review.username || '用户'
    return {
      letter: (name[0] || '评').slice(0, 1),
      name,
      stars: '★★★★★'.slice(0, safeRating) + '☆☆☆☆☆'.slice(0, 5 - safeRating),
      content: review.content || '用户未填写评价内容'
    }
  })
)

/* ---------------- 数据加载 ---------------- */
const loadTodayStats = async () => {
  const res = await getTodayStats()
  if (res.code === 200) todayStats.value = res.data
}

const loadOrders = async () => {
  const res = await getOrderList({ page: 1, size: ORDER_SCAN_SIZE })
  if (res.code !== 200) return
  const list = res.data?.list || res.data || []
  preparingCount.value = list.filter((item: any) => item.orderStatus === STATUS_PREPARING).length
  await enrichAndSetPending(list)
}

const fetchAll = async () => {
  loading.value = true
  try {
    const results = await Promise.allSettled([
      loadTodayStats(),
      getDailyStats({ startDate: getDateStr(6), endDate: getDateStr(0) }).then((res) => {
        if (res.code === 200) dailyStats.value = res.data || []
      }),
      getTopDishes({ limit: 5 }).then((res) => {
        if (res.code === 200) topDishes.value = res.data?.list || res.data || []
      }),
      loadOrders(),
      getDishList({ page: 1, size: 100 }).then((res) => {
        if (res.code !== 200) return
        const list = res.data?.list || res.data || []
        lowStocks.value = list
          .filter((item: any) => Number(item.stock ?? 0) <= LOW_STOCK_THRESHOLD)
          .sort((a: any, b: any) => Number(a.stock ?? 0) - Number(b.stock ?? 0))
          .slice(0, 3)
      }),
      getReviewList({ page: 1, size: 2 }).then((res) => {
        if (res.code === 200) latestReviews.value = res.data?.list || res.data || []
      })
    ])
    results.forEach((result) => {
      if (result.status === 'rejected') console.error('工作台数据加载失败', result.reason)
    })
  } finally {
    loading.value = false
    await nextTick()
    renderCharts()
  }
}

/* ---------------- 图表 ---------------- */
const sparkRef = ref<HTMLElement | null>(null)
const trendRef = ref<HTMLElement | null>(null)
const top5Ref = ref<HTMLElement | null>(null)
let sparkChart: ECharts | null = null
let trendChart: ECharts | null = null
let top5Chart: ECharts | null = null

const GREEN = '#1E9E62'
const SOFT_GREEN = '#A8D9BF'
const HAIR = '#E4E8E3'
const SUB = '#6B7A72'

const renderCharts = () => {
  const data = trendData.value
  const labels = data.map((item) => item.label)
  const revenues = data.map((item) => item.revenue)
  const orders = data.map((item) => item.orderCount)

  if (sparkRef.value) {
    sparkChart?.dispose()
    sparkChart = init(sparkRef.value)
    // 订单量按营收量级等比缩放，共用隐藏轴（同设计蓝本）
    const maxRevenue = Math.max(...revenues, 1)
    const maxOrders = Math.max(...orders, 1)
    const scale = maxRevenue / maxOrders
    sparkChart.setOption({
      grid: { left: 0, right: 0, top: 10, bottom: 0 },
      xAxis: { type: 'category', show: false, data: labels },
      yAxis: { type: 'value', show: false },
      series: [
        {
          type: 'line', smooth: true, symbol: 'none', data: revenues,
          lineStyle: { width: 2, color: GREEN },
          areaStyle: {
            color: {
              type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(30,158,98,.18)' },
                { offset: 1, color: 'rgba(30,158,98,0)' }
              ]
            }
          }
        },
        {
          type: 'line', smooth: true, symbol: 'none',
          data: orders.map((v) => v * scale),
          lineStyle: { width: 1.5, color: SOFT_GREEN, type: 'dashed' }
        }
      ]
    })
  }

  if (trendRef.value) {
    trendChart?.dispose()
    trendChart = init(trendRef.value)
    trendChart.setOption({
      grid: { left: 50, right: 44, top: 24, bottom: 28 },
      tooltip: { trigger: 'axis', axisPointer: { type: 'line', lineStyle: { color: HAIR } } },
      xAxis: {
        type: 'category', data: labels,
        axisLine: { lineStyle: { color: HAIR } },
        axisTick: { show: false },
        axisLabel: { color: SUB, fontSize: 11 }
      },
      yAxis: [
        {
          type: 'value', name: '营收', nameTextStyle: { color: SUB, fontSize: 10 },
          splitLine: { lineStyle: { color: HAIR, type: 'dashed' } },
          axisLabel: { color: SUB, fontSize: 11 }
        },
        {
          type: 'value', name: '订单', nameTextStyle: { color: SUB, fontSize: 10 },
          splitLine: { show: false },
          axisLabel: { color: SUB, fontSize: 11 },
          minInterval: 1
        }
      ],
      series: [
        {
          name: '营收', type: 'line', smooth: true, symbol: 'circle', symbolSize: 5,
          data: revenues,
          lineStyle: { width: 2.5, color: GREEN }, itemStyle: { color: GREEN },
          areaStyle: {
            color: {
              type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(30,158,98,.14)' },
                { offset: 1, color: 'rgba(30,158,98,0)' }
              ]
            }
          }
        },
        {
          name: '订单量', type: 'bar', yAxisIndex: 1, data: orders, barWidth: 12,
          itemStyle: { color: '#DCE9DF', borderRadius: [3, 3, 0, 0] }
        }
      ]
    })
  }

  if (top5Ref.value) {
    top5Chart?.dispose()
    top5Chart = init(top5Ref.value)
    const names = topDishes.value.map((item: any) => item.dishName || '未命名菜品')
    const values = topDishes.value.map((item: any) => Number(item.totalQuantity ?? item.soldCount ?? 0))
    top5Chart.setOption({
      grid: { left: 8, right: 40, top: 10, bottom: 10, containLabel: true },
      xAxis: { type: 'value', show: false },
      yAxis: {
        type: 'category', inverse: true, data: names,
        axisLine: { show: false }, axisTick: { show: false },
        axisLabel: { color: '#17251F', fontSize: 12 }
      },
      series: [{
        type: 'bar', data: values, barWidth: 14,
        itemStyle: {
          color: (p: any) => (p.dataIndex === 0 ? GREEN : '#BFE3CD'),
          borderRadius: [0, 4, 4, 0]
        },
        label: { show: true, position: 'right', color: SUB, fontSize: 11, fontWeight: 700, formatter: '{c} 份' }
      }]
    })
  }
}

const resizeCharts = () => {
  sparkChart?.resize()
  trendChart?.resize()
  top5Chart?.resize()
}

onMounted(() => {
  fetchAll()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  sparkChart?.dispose()
  trendChart?.dispose()
  top5Chart?.dispose()
})
</script>

<template>
  <div class="db" v-loading="loading">
    <!-- 经营概览主卡 -->
    <div class="card hero">
      <div class="hero-left">
        <div class="hero-lab">
          <span class="en">TODAY'S REVENUE</span>
          <span class="hero-date">{{ todayLabel }}</span>
        </div>
        <div class="hero-big num">
          <small>¥</small> {{ todayRevenue === null ? '-' : fmtMoney(todayRevenue) }}
        </div>
        <span v-if="revenueDelta !== null" class="hero-delta" :class="{ down: revenueDelta < 0 }">
          {{ revenueDelta >= 0 ? '↑' : '↓' }} {{ Math.abs(revenueDelta).toFixed(1) }}%
        </span>
        <div v-if="yesterdayRevenue !== null || weekRevenue !== null" class="hero-vs">
          <template v-if="yesterdayRevenue !== null">昨日营收 ¥{{ fmtMoney(yesterdayRevenue) }}</template>
          <template v-if="weekRevenue !== null">{{ yesterdayRevenue !== null ? ' · ' : '' }}近 7 日累计 ¥{{ fmtMoney(weekRevenue, 0) }}</template>
        </div>
      </div>
      <div class="hero-right">
        <div class="hero-chart-head">
          <span class="en">7-DAY TREND</span>
          <div class="legend">
            <span><i style="background:#1E9E62"></i>营收</span>
            <span><i style="background:#A8D9BF"></i>订单量</span>
          </div>
        </div>
        <div ref="sparkRef" class="spark-chart"></div>
      </div>
    </div>

    <!-- 指标带 -->
    <div class="card metrics-band">
      <div class="metrics-grid">
        <div v-for="item in metricItems" :key="item.en" class="metric-cell">
          <span class="en">{{ item.en }}</span>
          <div class="metric-value num">{{ item.value }}<em v-if="item.unit && item.value !== '-'"> {{ item.unit }}</em></div>
        </div>
      </div>
    </div>

    <!-- 待办 + 预警口碑 -->
    <div class="grid">
      <div class="card panel">
        <div class="panel-head">
          <span class="panel-title">待办中心</span>
          <span class="en panel-en">PENDING</span>
          <span class="panel-link" @click="router.push('/merchant/orders')">查看全部订单 →</span>
        </div>
        <template v-if="todoList.length">
          <div v-for="todo in todoList" :key="String(todo.raw.id)" class="todo">
            <div class="todo-no"><b>{{ todo.no }}</b>{{ todo.time }}</div>
            <div class="todo-what">
              <b>{{ todo.summary }}</b>
              <span>{{ todo.buyer }}<template v-if="todo.address"> · 送到 {{ todo.address }}</template><template v-if="todo.remark"> · 备注：{{ todo.remark }}</template></span>
            </div>
            <div class="todo-amt num">¥{{ todo.amount }}</div>
            <div class="todo-ops">
              <span class="todo-rej" :class="{ disabled: opLoading[String(todo.raw.id)] }" @click="handleReject(todo.raw)">拒单</span>
              <span class="btn-green" :class="{ disabled: opLoading[String(todo.raw.id)] }" @click="handleAccept(todo.raw)">接单</span>
            </div>
          </div>
        </template>
        <div v-else class="empty-line">暂无待接单订单</div>
      </div>

      <div class="side-col">
        <div class="card panel">
          <div class="panel-head">
            <span class="panel-title">库存预警</span>
            <span class="en panel-en">LOW STOCK</span>
            <span class="panel-link" @click="router.push('/merchant/dishes')">去补货 →</span>
          </div>
          <template v-if="stockList.length">
            <div v-for="dish in stockList" :key="dish.name" class="stock">
              <div class="stock-nm">{{ dish.name }}<span>{{ dish.sub }}</span></div>
              <div class="stock-bar"><i :style="{ width: dish.width }"></i></div>
              <div class="stock-ct num">剩 {{ dish.stock }}</div>
            </div>
          </template>
          <div v-else class="empty-line">库存状态健康，暂无预警菜品</div>
        </div>

        <div class="card panel">
          <div class="panel-head">
            <span class="panel-title">最新评价</span>
            <span class="en panel-en">REVIEWS</span>
            <span class="panel-link" @click="router.push('/merchant/reviews')">全部 →</span>
          </div>
          <template v-if="reviewList.length">
            <div v-for="(review, index) in reviewList" :key="index" class="review">
              <div class="review-head">
                <div class="review-ava">{{ review.letter }}</div>
                <span class="review-name">{{ review.name }}</span>
                <span class="review-stars">{{ review.stars }}</span>
              </div>
              <p>{{ review.content }}</p>
            </div>
          </template>
          <div v-else class="empty-line">暂无评价</div>
        </div>
      </div>
    </div>

    <!-- 图表区 -->
    <div class="charts">
      <div class="card chart-card">
        <div class="panel-head">
          <span class="panel-title">经营趋势</span>
          <span class="en panel-en">PERFORMANCE</span>
          <div class="legend">
            <span><i style="background:#1E9E62"></i>营收（元）</span>
            <span><i style="background:#A8D9BF"></i>订单量（单）</span>
          </div>
        </div>
        <div ref="trendRef" class="trend-chart"></div>
      </div>
      <div class="card chart-card">
        <div class="panel-head">
          <span class="panel-title">热销 TOP 5</span>
          <span class="en panel-en">BEST SELLERS</span>
        </div>
        <div ref="top5Ref" class="top5-chart"></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.db {
  /* 设计令牌已下沉至 src/styles/tokens.scss（--green/--green-ink/--hair 等全局可用） */
  max-width: 1280px;
  color: var(--ink);
  font-size: 14px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.num { font-variant-numeric: tabular-nums; }

.en {
  font-size: 10px;
  letter-spacing: 2px;
  color: var(--faint);
  font-weight: 700;
}

.card {
  background: #FFFFFF;
  border: 1px solid var(--hair);
  border-radius: 10px;
}

.btn-green {
  display: inline-flex;
  align-items: center;
  background: var(--green);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  border-radius: 8px;
  padding: 5px 12px;
  cursor: pointer;
  user-select: none;
}

.btn-green.disabled,
.todo-rej.disabled {
  opacity: 0.5;
  pointer-events: none;
}

.legend {
  display: flex;
  gap: 14px;
  font-size: 11px;
  color: var(--sub);
}

.legend i {
  width: 8px;
  height: 8px;
  border-radius: 2px;
  display: inline-block;
  margin-right: 5px;
}

/* ---------- 概览主卡 ---------- */
.hero {
  display: flex;
  margin-bottom: 20px;
}

.hero-left {
  padding: 26px 28px;
  width: 340px;
  flex: none;
  border-right: 1px solid var(--hair);
}

.hero-lab {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}

.hero-date {
  font-size: 11px;
  color: var(--faint);
}

.hero-big {
  font-size: 40px;
  font-weight: 800;
  margin-top: 14px;
  letter-spacing: -1px;
}

.hero-big small {
  font-size: 16px;
  font-weight: 700;
  color: var(--sub);
  letter-spacing: 0;
}

.hero-delta {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 10px;
  font-size: 12px;
  font-weight: 700;
  color: var(--green-deep);
  background: var(--green-soft);
  border-radius: 6px;
  padding: 4px 8px;
}

.hero-delta.down {
  color: var(--orange);
  background: var(--orange-soft);
}

.hero-vs {
  margin-top: 8px;
  font-size: 11px;
  color: var(--faint);
}

.hero-right {
  flex: 1;
  min-width: 0;
  padding: 20px 24px 12px;
  display: flex;
  flex-direction: column;
}

.hero-chart-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 4px;
}

.spark-chart {
  flex: 1;
  min-height: 110px;
}

/* ---------- 指标带 ---------- */
.metrics-band {
  margin-bottom: 20px;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
}

.metric-cell {
  padding: 16px 28px;
  border-right: 1px solid var(--hair);
}

.metric-cell:last-child {
  border-right: 0;
}

.metric-value {
  font-size: 22px;
  font-weight: 800;
  margin-top: 6px;
}

.metric-value em {
  font-style: normal;
  font-size: 11px;
  color: var(--faint);
  font-weight: 500;
  margin-left: 2px;
}

/* ---------- 三栏区 ---------- */
.grid {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.side-col {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

.panel {
  padding: 20px 22px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 14px;
}

.panel-title {
  font-size: 15px;
  font-weight: 800;
}

.panel-en {
  flex: 1;
  margin-left: 10px;
}

.panel-link {
  font-size: 11px;
  color: var(--green-deep);
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}

.todo {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 13px 0;
  border-top: 1px solid var(--hair);
}

.todo:first-of-type {
  border-top: 0;
}

.todo-no {
  font-size: 10px;
  color: var(--faint);
  width: 64px;
  flex: none;
}

.todo-no b {
  display: block;
  font-size: 12px;
  color: var(--ink);
  font-weight: 700;
  margin-bottom: 2px;
}

.todo-what {
  flex: 1;
  min-width: 0;
}

.todo-what b {
  font-size: 13px;
  font-weight: 700;
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.todo-what span {
  font-size: 11px;
  color: var(--sub);
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.todo-amt {
  font-size: 14px;
  font-weight: 800;
  width: 72px;
  text-align: right;
  flex: none;
}

.todo-ops {
  display: flex;
  gap: 6px;
  flex: none;
}

.todo-rej {
  font-size: 11px;
  color: var(--sub);
  border: 1px solid var(--hair);
  border-radius: 6px;
  padding: 5px 10px;
  cursor: pointer;
  background: #fff;
  user-select: none;
}

.empty-line {
  padding: 18px 0 6px;
  font-size: 12px;
  color: var(--faint);
  text-align: center;
}

.stock {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 0;
  border-top: 1px solid var(--hair);
}

.stock:first-of-type {
  border-top: 0;
}

.stock-nm {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  font-weight: 600;
}

.stock-nm span {
  display: block;
  font-size: 10px;
  color: var(--faint);
  margin-top: 2px;
  font-weight: 400;
}

.stock-bar {
  width: 110px;
  height: 5px;
  background: #F0F2EE;
  border-radius: 3px;
  overflow: hidden;
  flex: none;
}

.stock-bar i {
  display: block;
  height: 100%;
  background: var(--orange);
  border-radius: 3px;
}

.stock-ct {
  font-size: 12px;
  font-weight: 800;
  color: var(--orange);
  width: 52px;
  text-align: right;
  flex: none;
}

.review {
  padding: 12px 0;
  border-top: 1px solid var(--hair);
}

.review:first-of-type {
  border-top: 0;
}

.review-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.review-ava {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--green-soft);
  color: var(--green-deep);
  font-size: 10px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
}

.review-name {
  font-size: 12px;
  font-weight: 700;
  flex: 1;
}

.review-stars {
  color: var(--green);
  font-size: 11px;
  letter-spacing: 2px;
}

.review p {
  font-size: 12px;
  color: var(--sub);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ---------- 图表区 ---------- */
.charts {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 20px;
}

.chart-card {
  padding: 20px 22px;
  min-width: 0;
}

.trend-chart,
.top5-chart {
  height: 260px;
}

/* ---------- 窄屏适配 ---------- */
@media (max-width: 1100px) {
  .grid,
  .charts {
    grid-template-columns: 1fr;
  }

  .hero {
    flex-direction: column;
  }

  .hero-left {
    width: 100%;
    border-right: 0;
    border-bottom: 1px solid var(--hair);
  }

  .metrics-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .metric-cell:nth-child(2) {
    border-right: 0;
  }

  .metric-cell:nth-child(1),
  .metric-cell:nth-child(2) {
    border-bottom: 1px solid var(--hair);
  }
}
</style>
