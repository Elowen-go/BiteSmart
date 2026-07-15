<script setup lang="ts">
import { ref, computed, nextTick, onBeforeUnmount, onMounted } from 'vue'
import { use, init, type ECharts } from 'echarts/core'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import {
  Money,
  ShoppingCart,
  User,
  Shop,
  List,
  Bell,
  ChatDotRound,
  ArrowUp,
  UserFilled
} from '@element-plus/icons-vue'
import StatCard from '../../components/common/StatCard.vue'
import { getOverview, getTrend, getDashboardStats } from '../../api/admin/statistics'
import { listNotices } from '../../api/admin/notices'
import { getMerchantList } from '../../api/admin/merchants'
import { getReviewList } from '../../api/admin/reviews'
import { getDriverList } from '../../api/admin/drivers'

use([LineChart, BarChart, PieChart, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

const loading = ref(true)
const stats = ref({
  totalRevenue: 0,
  orderCount: 0,
  userCount: 0,
  merchantCount: 0,
  pendingRefundCount: 0,
  pendingComplaintCount: 0,
  activeMembershipCount: 0,
  aiConversationCount: 0,
  lowStockDishCount: 0
})

const recentOrders = ref<any[]>([])
const notifications = ref<any[]>([])
const trends = ref<any[]>([])
const orderSummary = ref({ pending: 0, delivering: 0, completed: 0 })
const managementTodos = ref({ merchantAudit: 0, reviewPending: 0, driverFrozen: 0, orderException: 0 })
const workQueue = computed(() => [
  { label: '待审核商家', value: managementTodos.value.merchantAudit, hint: '商家审核', tone: 'warning' },
  { label: '待处理评价', value: managementTodos.value.reviewPending, hint: '评价管理', tone: 'warning' },
  { label: '冻结配送员', value: managementTodos.value.driverFrozen, hint: '配送员复核', tone: 'warning' },
  { label: '异常订单', value: managementTodos.value.orderException, hint: '订单跟进', tone: 'danger' },
  { label: '待审核退款', value: stats.value.pendingRefundCount, hint: '退款工单', tone: 'warning' },
  { label: '待处理投诉', value: stats.value.pendingComplaintCount, hint: '投诉工单', tone: 'warning' },
  { label: '库存预警', value: stats.value.lowStockDishCount, hint: '库存补货', tone: 'warning' }
])
const maxTrendRevenue = computed(() => Math.max(...trends.value.map((item) => Number(item.revenue || 0)), 1))
const trendBarHeight = (value: number) => `${Math.max((Number(value || 0) / maxTrendRevenue.value) * 100, 5)}%`
const orderTotal = computed(() => orderSummary.value.pending + orderSummary.value.delivering + orderSummary.value.completed)
const orderDonutStyle = computed(() => {
  if (!orderTotal.value) return { background: '#e8efea' }
  const pending = orderSummary.value.pending / orderTotal.value * 100
  const delivering = orderSummary.value.delivering / orderTotal.value * 100
  return { background: `conic-gradient(#e5a33d 0 ${pending}%, #4d9b78 ${pending}% ${pending + delivering}%, #2c634b ${pending + delivering}% 100%)` }
})
const trendChartRef = ref<HTMLElement | null>(null)
const orderChartRef = ref<HTMLElement | null>(null)
const scaleChartRef = ref<HTMLElement | null>(null)
let trendChart: ECharts | null = null
let orderChart: ECharts | null = null
let scaleChart: ECharts | null = null

const renderCharts = () => {
  if (trendChartRef.value) {
    trendChart?.dispose()
    trendChart = init(trendChartRef.value)
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { bottom: 0, data: ['收入', '订单'] },
      grid: { left: 45, right: 48, top: 16, bottom: 32 },
      xAxis: { type: 'category', data: trends.value.map((item) => item.type === 'day' ? '近1天' : item.type === 'week' ? '近7天' : '近1月') },
      yAxis: [{ type: 'value', name: '收入', axisLabel: { formatter: '¥{value}' } }, { type: 'value', name: '订单', minInterval: 1 }],
      series: [
        { name: '收入', type: 'line', smooth: true, yAxisIndex: 0, data: trends.value.map((item) => Number(item.revenue || 0)), lineStyle: { color: '#2c634b', width: 3 }, itemStyle: { color: '#2c634b' }, areaStyle: { color: 'rgba(44,99,75,.12)' } },
        { name: '订单', type: 'bar', yAxisIndex: 1, barMaxWidth: 26, data: trends.value.map((item) => Number(item.orderCount || 0)), itemStyle: { color: '#e5a33d', borderRadius: [4, 4, 0, 0] } }
      ]
    })
  }
  if (orderChartRef.value) {
    orderChart?.dispose()
    orderChart = init(orderChartRef.value)
    orderChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0, left: 'center' },
      series: [{ type: 'pie', radius: ['44%', '70%'], center: ['50%', '45%'], label: { formatter: '{b}\n{c} 单' }, data: [{ value: orderSummary.value.pending, name: '待处理', itemStyle: { color: '#e5a33d' } }, { value: orderSummary.value.delivering, name: '配送中', itemStyle: { color: '#4d9b78' } }, { value: orderSummary.value.completed, name: '已完成', itemStyle: { color: '#2c634b' } }] }]
    })
  }
  if (scaleChartRef.value) {
    scaleChart?.dispose()
    scaleChart = init(scaleChartRef.value)
    scaleChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: 48, right: 20, top: 20, bottom: 28 },
      xAxis: { type: 'category', data: ['注册用户', '商家', '订单'] },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{ type: 'bar', barMaxWidth: 48, data: [{ value: stats.value.userCount, itemStyle: { color: '#4d9b78' } }, { value: stats.value.merchantCount, itemStyle: { color: '#e5a33d' } }, { value: stats.value.orderCount, itemStyle: { color: '#2c634b' } }], label: { show: true, position: 'top' }, itemStyle: { borderRadius: [5, 5, 0, 0] } }]
    })
  }
}
const resizeCharts = () => { trendChart?.resize(); orderChart?.resize(); scaleChart?.resize() }

const noticeTypeMap: Record<number, any> = {
  10: { icon: ChatDotRound, label: '系统公告' },
  20: { icon: UserFilled, label: '健康知识' },
  30: { icon: Bell, label: '活动信息' },
  40: { icon: ArrowUp, label: '升级通知' },
  0: { icon: Bell, label: '通知' }
}

const formatTime = (createTime: string) => {
  const now = new Date()
  const time = new Date(createTime)
  const diffMinutes = Math.floor((now.getTime() - time.getTime()) / (1000 * 60))
  if (diffMinutes < 1) return '刚刚'
  if (diffMinutes < 60) return `${diffMinutes}分钟前`
  if (diffMinutes < 1440) return `${Math.floor(diffMinutes / 60)}小时前`
  if (diffMinutes < 43200) return `${Math.floor(diffMinutes / 1440)}天前`
  return createTime.slice(0, 10)
}

onMounted(async () => {
  loading.value = true
  try {
    const overviewRes = await getOverview()
    if (overviewRes.code === 200) {
      const d = overviewRes.data
      stats.value.totalRevenue = d.totalRevenue || 0
      stats.value.orderCount = d.orderCount || 0
      stats.value.userCount = d.userCount || 0
      stats.value.merchantCount = d.merchantCount || 0
    }

    const trendResults = await Promise.all([getTrend('day'), getTrend('week'), getTrend('month')])
    trends.value = trendResults.filter((res) => res.code === 200).map((res) => res.data)

    const [merchantAuditRes, reviewRes, driverRes] = await Promise.all([
      getMerchantList({ pageNum: 1, pageSize: 1, status: 10 }),
      getReviewList({ pageNum: 1, pageSize: 100 }),
      getDriverList({ pageNum: 1, pageSize: 1, status: 40 })
    ])
    managementTodos.value.merchantAudit = merchantAuditRes.code === 200 ? Number(merchantAuditRes.data?.total || 0) : 0
    managementTodos.value.reviewPending = reviewRes.code === 200 ? (reviewRes.data?.list || []).filter((item: any) => item.status === 10).length : 0
    managementTodos.value.driverFrozen = driverRes.code === 200 ? Number(driverRes.data?.total || 0) : 0

    const { getOrderList } = await import('../../api/admin/orders')
    const orderRes = await getOrderList({ pageNum: 1, pageSize: 5 })
    if (orderRes.code === 200) {
      const list = orderRes.data?.list || []
      recentOrders.value = list.map((o: any) => ({
        orderNo: o.orderNo,
        customer: o.deliveryAddress?.slice(0, 8) + '…' || '未知',
        amount: o.payAmount || o.totalAmount,
        status: o.orderStatus >= 50 ? 'completed' : o.orderStatus >= 40 ? 'delivering' : 'pending',
        time: o.createTime?.slice(0, 16) || ''
      }))
      orderSummary.value = list.reduce((summary: any, order: any) => {
        if (order.orderStatus >= 50) summary.completed += 1
        else if (order.orderStatus >= 40) summary.delivering += 1
        else summary.pending += 1
        return summary
      }, { pending: 0, delivering: 0, completed: 0 })
    }
    const exceptionRes = await getOrderList({ pageNum: 1, pageSize: 1, orderStatus: 60 })
    managementTodos.value.orderException = exceptionRes.code === 200 ? Number(exceptionRes.data?.total || 0) : 0
    await nextTick()
    renderCharts()

    const noticeRes = await listNotices(1, 3)
    if (noticeRes.code === 200) {
      const list = noticeRes.data?.list || noticeRes.data || []
      notifications.value = list.map((n: any) => ({
        icon: noticeTypeMap[n.noticeType]?.icon || noticeTypeMap[0].icon,
        title: n.title,
        desc: n.content?.slice(0, 20) + '…' || '',
        time: formatTime(n.createTime || '')
      }))
    }
    const dashboardRes = await getDashboardStats()
    if (dashboardRes.code === 200 && dashboardRes.data) {
      const dashboard = dashboardRes.data
      if (dashboard.overview) stats.value = { ...stats.value, ...dashboard.overview }
      stats.value.pendingRefundCount = Number(dashboard.pendingRefundCount || 0)
      stats.value.pendingComplaintCount = Number(dashboard.pendingComplaintCount || 0)
      stats.value.activeMembershipCount = Number(dashboard.activeMembershipCount || 0)
      stats.value.aiConversationCount = Number(dashboard.aiConversationCount || 0)
      stats.value.lowStockDishCount = Number(dashboard.lowStockDishCount || 0)
      trends.value = (dashboard.dailyTrend || []).map((item: any) => ({ type: item.stat_date || item.statDate, revenue: item.revenue, orderCount: item.order_count || item.orderCount }))
      orderSummary.value = (dashboard.orderStatus || []).reduce((summary: any, item: any) => {
        if (Number(item.status) >= 50) summary.completed += Number(item.count || 0)
        else if (Number(item.status) >= 40) summary.delivering += Number(item.count || 0)
        else summary.pending += Number(item.count || 0)
        return summary
      }, { pending: 0, delivering: 0, completed: 0 })
    }
    await nextTick()
    renderCharts()
  } catch (err) {
    console.error('获取Dashboard数据失败', err)
  } finally {
    loading.value = false
  }
})
onMounted(() => window.addEventListener('resize', resizeCharts))
onBeforeUnmount(() => { window.removeEventListener('resize', resizeCharts); trendChart?.dispose(); orderChart?.dispose(); scaleChart?.dispose() })

const getStatusBadge = (status: string) => {
  switch (status) {
    case 'completed':
      return { class: 'success', text: '已完成' }
    case 'delivering':
      return { class: 'warning', text: '配送中' }
    case 'pending':
      return { class: 'secondary', text: '待接单' }
    default:
      return { class: 'secondary', text: status }
  }
}
</script>

<template>
  <div class="dashboard">
    <div v-loading="loading" class="stats-row">
      <StatCard
        :icon="Money"
        label="总销售额"
        :value="'¥' + Number(stats.totalRevenue).toLocaleString()"
      />
      <StatCard
        :icon="ShoppingCart"
        label="订单总数"
        :value="stats.orderCount"
      />
      <StatCard
        :icon="User"
        label="注册用户"
        :value="stats.userCount"
      />
      <StatCard
        :icon="Shop"
        label="商家总数"
        :value="stats.merchantCount"
      />
    </div>

    <div class="work-queue card-panel">
      <div class="card-header">
        <div><h3>待处理事项</h3><p>需要管理员关注的运营任务</p></div>
        <span class="queue-summary">{{ workQueue.filter(item => item.value > 0).length }} 项待跟进</span>
      </div>
      <div class="queue-grid">
        <div v-for="item in workQueue" :key="item.label" :class="['queue-item', item.tone, { active: item.value > 0 }]">
          <span class="queue-label">{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.hint }}</small>
        </div>
      </div>
    </div>
    
    <div class="dashboard-insights">
      <div class="card-panel trend-panel">
        <div class="card-header"><div><h3>经营趋势</h3><p>按时间范围查看订单与收入</p></div><span class="insight-label">实时汇总</span></div>
        <div ref="trendChartRef" class="echart trend-chart"></div>
      </div>
      <div class="card-panel summary-panel">
        <div class="card-header"><div><h3>订单状态</h3><p>当前列表中的订单分布</p></div></div>
        <div ref="orderChartRef" class="echart order-chart"></div>
      </div>
    </div>

    <div class="card-panel platform-chart-panel">
      <div class="card-header"><div><h3>平台规模</h3><p>用户、商家与订单总量对比</p></div></div>
      <div ref="scaleChartRef" class="echart scale-chart"></div>
    </div>

    <div class="grid-2col">
      <div class="card-panel">
        <div class="card-header">
          <h3>
            <List style="width: 18px; height: 18px; margin-right: 8px; color: var(--bs-primary);" />
            最近订单
          </h3>
          <button class="btn btn-secondary btn-sm">查看更多</button>
        </div>
        <div class="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>订单号</th>
                <th>客户</th>
                <th>金额</th>
                <th>状态</th>
                <th>时间</th>
              </tr>
            </thead>
            <tbody v-if="recentOrders.length > 0">
              <tr v-for="order in recentOrders" :key="order.orderNo">
                <td>{{ order.orderNo }}</td>
                <td>{{ order.customer }}</td>
                <td>¥{{ Number(order.amount).toFixed(2) }}</td>
                <td>
                  <span :class="['status-badge', getStatusBadge(order.status).class]">
                    {{ getStatusBadge(order.status).text }}
                  </span>
                </td>
                <td>{{ order.time }}</td>
              </tr>
            </tbody>
            <tbody v-else>
              <tr><td colspan="5" style="text-align:center;color:var(--bs-text-muted);padding:32px 0;">暂无订单</td></tr>
            </tbody>
          </table>
        </div>
      </div>
      
      <div class="card-panel">
        <div class="card-header">
          <h3>
            <Bell style="width: 18px; height: 18px; margin-right: 8px; color: var(--bs-primary);" />
            最新通知
          </h3>
          <span style="font-size: 13px; color: var(--bs-text-muted);">{{ notifications.length }}条通知</span>
        </div>
        <div v-if="notifications.length" class="notifications-list">
          <div v-for="(item, index) in notifications" :key="index" class="notification-item">
            <component :is="item.icon" style="color: var(--bs-primary); width: 20px;" />
            <div class="notification-content">
              <div style="font-weight: 500; color: var(--bs-text-title);">{{ item.title }}</div>
              <div style="font-size: 13px; color: var(--bs-text-muted);">{{ item.desc }}</div>
            </div>
            <span style="font-size: 12px; color: var(--bs-text-muted);">{{ item.time }}</span>
          </div>
        </div>
        <div v-else class="empty-notifications">暂无通知</div>
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
  grid-template-columns: repeat(4, 1fr);
  gap: var(--bs-spacing-lg);
  margin-bottom: var(--bs-spacing-lg);
}
.work-queue { margin-bottom: var(--bs-spacing-lg); }
.work-queue .card-header { margin-bottom: 14px; }
.queue-summary { color: var(--bs-text-muted); font-size: 13px; }
.queue-grid { display: grid; grid-template-columns: repeat(4, 1fr); column-gap: 28px; }
.queue-item { display: grid; grid-template-columns: 1fr auto; gap: 4px 12px; align-items: center; min-height: 62px; padding: 10px 0; border-top: 1px solid #e8efea; color: var(--bs-text-muted); }
.queue-item strong { grid-column: 2; grid-row: 1 / span 2; align-self: center; color: var(--bs-text-title); font-size: 22px; font-variant-numeric: tabular-nums; }
.queue-item small { grid-column: 1; color: var(--bs-text-muted); font-size: 11px; }
.queue-item.active.warning .queue-label::before, .queue-item.active.danger .queue-label::before { display: inline-block; width: 6px; height: 6px; margin: 0 7px 2px 0; border-radius: 50%; background: #e5a33d; content: ''; }
.queue-item.active.danger .queue-label::before { background: #c95c5c; }

.grid-2col {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: var(--bs-spacing-lg);
}

.dashboard-insights {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: var(--bs-spacing-lg);
  margin-bottom: var(--bs-spacing-lg);
}

.trend-panel, .summary-panel { min-height: 188px; }
.card-header p { margin: 5px 0 0; color: var(--bs-text-muted); font-size: 12px; }
.insight-label { color: var(--bs-primary); font-size: 12px; }
.trend-chart { height: 150px; display: flex; gap: 10px; }
.echart { width: 100%; }
.order-chart { height: 160px; }
.scale-chart { height: 250px; }
.platform-chart-panel { margin-bottom: var(--bs-spacing-lg); }
.chart-axis { width: 54px; display: flex; flex-direction: column; justify-content: space-between; color: var(--bs-text-muted); font-size: 11px; }
.bars { flex: 1; display: flex; align-items: flex-end; justify-content: space-around; gap: 18px; border-bottom: 1px solid #dfe9e2; padding: 0 12px; }
.bar-column { height: 100%; flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: flex-end; gap: 5px; color: var(--bs-text-muted); font-size: 11px; }
.bar-column small { font-size: 10px; }
.bar-value { color: var(--bs-text-title); font-size: 11px; }
.bar { width: min(44px, 70%); min-height: 7px; background: linear-gradient(180deg, #6db18f, #2c634b); border-radius: 5px 5px 0 0; transition: height .3s ease; }
.donut-layout { display: flex; align-items: center; gap: 25px; padding-top: 10px; }
.donut { width: 128px; height: 128px; border-radius: 50%; display: grid; place-items: center; flex: 0 0 128px; }
.donut > div { width: 78px; height: 78px; border-radius: 50%; background: var(--bs-card-bg); display: flex; flex-direction: column; align-items: center; justify-content: center; }
.donut strong { color: var(--bs-text-title); font-size: 24px; }.donut span { color: var(--bs-text-muted); font-size: 11px; }
.legend { flex: 1; display: flex; flex-direction: column; gap: 11px; }.legend div { display: grid; grid-template-columns: 10px 1fr auto; gap: 8px; align-items: center; font-size: 12px; }.legend i { width: 8px; height: 8px; border-radius: 50%; }.legend .pending { background: #e5a33d; }.legend .delivering { background: #4d9b78; }.legend .completed { background: #2c634b; }.legend span { color: var(--bs-text-muted); }.legend strong { color: var(--bs-text-title); }
.empty-insight, .empty-notifications { padding: 28px 0; text-align: center; color: var(--bs-text-muted); font-size: 13px; }

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
  text-decoration: none;
}

.btn-primary {
  background: var(--bs-primary);
  color: #FFFFFF;
}

.btn-primary:hover {
  background: var(--bs-primary-hover);
}

.btn-secondary {
  background: transparent;
  border-color: var(--bs-border-light);
  color: var(--bs-text-body);
}

.btn-secondary:hover {
  background: var(--bs-bg-hover);
}

.btn-sm {
  padding: var(--bs-spacing-xs) var(--bs-spacing-md);
  font-size: var(--bs-font-size-sm);
}

.table-wrapper {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--bs-font-size-base);
}

table th {
  text-align: left;
  padding: 12px 12px 12px 0;
  font-weight: 600;
  color: var(--bs-text-muted);
  border-bottom: 1px solid var(--bs-border-light);
}

table td {
  padding: 14px 12px 14px 0;
  border-bottom: 1px solid var(--bs-border-light);
  color: var(--bs-text-body);
}

table tr:last-child td {
  border-bottom: none;
}

table tr:hover td {
  background: var(--bs-bg-hover);
}

.status-badge {
  display: inline-block;
  padding: 2px 12px;
  border-radius: 20px;
  font-size: var(--bs-font-size-xs);
  font-weight: 500;
}

.status-badge.success {
  background: var(--bs-status-success-bg);
  color: var(--bs-status-success);
}

.status-badge.warning {
  background: var(--bs-status-warning-bg);
  color: var(--bs-status-warning);
}

.status-badge.secondary {
  background: var(--bs-status-secondary-bg);
  color: var(--bs-status-secondary);
}

.notifications-list {
  display: flex;
  flex-direction: column;
  gap: var(--bs-spacing-md);
}

.notification-item {
  display: flex;
  gap: var(--bs-spacing-md);
  align-items: flex-start;
  padding-bottom: var(--bs-spacing-sm);
  border-bottom: 1px solid var(--bs-border-light);
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-content {
  flex: 1;
}

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .queue-grid { grid-template-columns: repeat(2, 1fr); column-gap: 24px; }
  .grid-2col {
    grid-template-columns: 1fr;
  }
  .dashboard-insights { grid-template-columns: 1fr; }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
  .queue-grid { grid-template-columns: 1fr; }
  .trend-chart { height: 145px; }
  .donut-layout { justify-content: center; }
}
</style>
