<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, nextTick, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { use, init, type ECharts } from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getFinanceOverview, getFinanceLedgers, getFinanceSettlements } from '../../../api/merchant/finance'

use([BarChart, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

const loading = ref(false)
const error = ref('')
const account = ref<any>({})
const ledgers = ref<any[]>([])
const settlements = ref<any[]>([])

/** 金额：千分位 + 两位小数 */
const money = (value: unknown) =>
  `¥${Number(value || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`

/** 流水类型映射：10 支付收入 / 20 平台佣金 / 30 退款冲正 / 其他 结算变动 */
const ledgerTypeMap: Record<number, { label: string; tag: 'success' | 'warning' | 'danger' | 'info' }> = {
  10: { label: '支付收入', tag: 'success' },
  20: { label: '平台佣金', tag: 'warning' },
  30: { label: '退款冲正', tag: 'danger' }
}

const getLedgerType = (type: number) => ledgerTypeMap[type] || { label: '结算变动', tag: 'info' as const }

/** 结算状态映射：10 待审核 / 20 已审核 / 30 已结算 / 40 已驳回 */
const settlementStatusMap: Record<number, { label: string; tag: 'warning' | 'primary' | 'success' | 'danger' }> = {
  10: { label: '待审核', tag: 'warning' },
  20: { label: '已审核', tag: 'primary' },
  30: { label: '已结算', tag: 'success' },
  40: { label: '已驳回', tag: 'danger' }
}

const getSettlementStatus = (status: number) =>
  settlementStatusMap[status] || { label: '未知', tag: 'info' as const }

// TODO(口径): 累计净收入为前端试算（累计收入 − 平台佣金），后端暂无净收入字段
const netIncome = computed(() => Number(account.value.totalIncome || 0) - Number(account.value.totalCommission || 0))

const metricCards = computed(() => [
  { label: '可结算余额', value: money(account.value.availableBalance), hint: '可发起结算的余额', className: 'brand' },
  { label: '待结算余额', value: money(account.value.pendingBalance), hint: '订单完成前暂存', className: 'warning' },
  { label: '累计收入', value: money(account.value.totalIncome), hint: '支付成功订单收入', className: 'success' },
  { label: '累计净收入', value: money(netIncome.value), hint: '收入 − 平台佣金（试算）', className: 'muted' }
])

/** 近 7 日资金变动：按日期聚合流水（收入 direction=10 / 支出其他） */
const trendDays = computed(() => {
  const days: { date: string; income: number; expense: number }[] = []
  for (let i = 6; i >= 0; i -= 1) {
    const d = new Date()
    d.setDate(d.getDate() - i)
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    days.push({ date: key, income: 0, expense: 0 })
  }
  const byDate = new Map(days.map((item) => [item.date, item]))
  ledgers.value.forEach((ledger) => {
    const key = String(ledger.createTime || '').slice(0, 10)
    const bucket = byDate.get(key)
    if (!bucket) return
    if (ledger.direction === 10) bucket.income += Number(ledger.amount || 0)
    else bucket.expense += Number(ledger.amount || 0)
  })
  return days
})

const trendChartRef = ref<HTMLElement | null>(null)
let trendChart: ECharts | null = null

const renderTrendChart = () => {
  if (!trendChartRef.value || !ledgers.value.length) return
  trendChart?.dispose()
  trendChart = init(trendChartRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { bottom: 0, data: ['收入', '支出'], textStyle: { color: '#6B7A72' } },
    grid: { left: 60, right: 24, top: 20, bottom: 46 },
    xAxis: {
      type: 'category',
      data: trendDays.value.map((item) => item.date.slice(5)),
      axisLine: { lineStyle: { color: '#E4E8E3' } },
      axisTick: { show: false },
      axisLabel: { color: '#6B7A72' }
    },
    yAxis: {
      type: 'value',
      axisLabel: { formatter: '¥{value}', color: '#6B7A72' },
      splitLine: { lineStyle: { color: '#E4E8E3', type: 'dashed' } }
    },
    series: [
      {
        name: '收入',
        type: 'bar',
        barMaxWidth: 18,
        data: trendDays.value.map((item) => Number(item.income.toFixed(2))),
        itemStyle: { color: '#1E9E62', borderRadius: [3, 3, 0, 0] }
      },
      {
        name: '支出',
        type: 'bar',
        barMaxWidth: 18,
        data: trendDays.value.map((item) => Number(item.expense.toFixed(2))),
        itemStyle: { color: '#D97B2B', borderRadius: [3, 3, 0, 0] }
      }
    ]
  })
}

const resizeCharts = () => {
  trendChart?.resize()
}

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    const [overview, ledgerRes, settlementRes] = await Promise.all([
      getFinanceOverview(),
      getFinanceLedgers(),
      getFinanceSettlements()
    ])
    if (overview.code !== 200 || ledgerRes.code !== 200 || settlementRes.code !== 200) throw new Error()
    account.value = overview.data || {}
    ledgers.value = ledgerRes.data || []
    settlements.value = settlementRes.data || []
  } catch {
    error.value = '资金数据加载失败，请稍后重试'
    ElMessage.error(error.value)
  } finally {
    loading.value = false
  }
  nextTick(() => renderTrendChart())
}

onMounted(() => {
  load()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  trendChart?.dispose()
  trendChart = null
})
</script>

<template>
  <div class="page-container" v-loading="loading">
    <div class="finance-page">
      <div class="page-head">
        <div>
          <div class="en">FINANCE</div>
          <h2>资金中心</h2>
          <p>查看本店收入、余额和平台结算记录</p>
        </div>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </div>

      <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" />

      <div class="metric-grid">
        <div v-for="item in metricCards" :key="item.label" class="metric-card" :class="item.className">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <em>{{ item.hint }}</em>
        </div>
      </div>

      <div class="card-panel">
        <div class="section-head">
          <div>
            <div class="en">TREND</div>
            <h3>近 7 日资金变动</h3>
          </div>
          <span>按流水日期聚合</span>
        </div>
        <div v-if="ledgers.length" ref="trendChartRef" class="trend-chart"></div>
        <el-empty v-else description="暂无资金流水" />
      </div>

      <div class="content-grid">
        <section class="card-panel panel">
          <div class="section-head">
            <div>
              <div class="en">LEDGERS</div>
              <h3>资金流水</h3>
            </div>
            <span>{{ ledgers.length }} 条记录</span>
          </div>
          <el-table :data="ledgers" empty-text="暂无资金流水">
            <el-table-column prop="createTime" label="时间" min-width="170" />
            <el-table-column prop="ledgerType" label="类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getLedgerType(row.ledgerType).tag" effect="plain" size="small">
                  {{ getLedgerType(row.ledgerType).label }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="amount" label="金额" width="140" align="right">
              <template #default="{ row }">
                <span class="amount" :class="row.direction === 10 ? 'credit' : 'debit'">
                  {{ row.direction === 10 ? '+' : '-' }}{{ money(row.amount) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="说明" min-width="190" show-overflow-tooltip />
          </el-table>
        </section>

        <section class="card-panel panel">
          <div class="section-head">
            <div>
              <div class="en">SETTLEMENTS</div>
              <h3>结算记录</h3>
            </div>
            <span>{{ settlements.length }} 笔</span>
          </div>
          <el-table :data="settlements" empty-text="暂无结算记录">
            <el-table-column prop="settlementNo" label="结算单号" min-width="170" show-overflow-tooltip />
            <el-table-column prop="netAmount" label="金额" width="130" align="right">
              <template #default="{ row }">
                <span class="amount credit">{{ money(row.netAmount) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="settlementStatus" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getSettlementStatus(row.settlementStatus).tag" effect="plain" size="small">
                  {{ getSettlementStatus(row.settlementStatus).label }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" min-width="160" />
          </el-table>
        </section>
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

.finance-page {
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

.page-head .en {
  color: var(--faint);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 2px;
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
  font-size: 26px;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}

.metric-card.brand {
  border-left: 3px solid var(--green);
}

.metric-card.warning {
  border-left: 3px solid var(--bs-status-warning);
}

.metric-card.success {
  border-left: 3px solid var(--bs-status-success);
}

.metric-card.muted {
  border-left: 3px solid var(--bs-status-secondary);
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
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 14px;
}

.section-head h3 {
  margin: 0;
  color: var(--bs-text-title);
  font-size: var(--bs-font-size-lg);
  font-weight: 600;
}

.section-head .en {
  color: var(--faint);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 2px;
}

.section-head > span {
  color: var(--bs-text-muted);
  font-size: 12px;
  white-space: nowrap;
}

.trend-chart {
  height: 240px;
}

.content-grid {
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  gap: 16px;
}

.panel {
  min-width: 0;
}

.amount {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.amount.credit {
  color: var(--bs-status-success);
}

.amount.debit {
  color: var(--bs-status-danger);
}

@media (max-width: 1000px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }

  .page-head {
    align-items: flex-start;
    gap: 10px;
    flex-direction: column;
  }
}
</style>
