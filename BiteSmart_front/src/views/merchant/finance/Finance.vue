<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getFinanceOverview, getFinanceLedgers, getFinanceSettlements } from '../../../api/merchant/finance'

const loading = ref(false)
const error = ref('')
const account = ref<any>({})
const ledgers = ref<any[]>([])
const settlements = ref<any[]>([])
const money = (value: unknown) => `¥${Number(value || 0).toFixed(2)}`
const statusText: Record<number, string> = { 10: '待审核', 20: '已审核', 30: '已结算', 40: '已驳回' }
const netIncome = computed(() => Number(account.value.totalIncome || 0) - Number(account.value.totalCommission || 0))

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    const [overview, ledgerRes, settlementRes] = await Promise.all([getFinanceOverview(), getFinanceLedgers(), getFinanceSettlements()])
    if (overview.code !== 200 || ledgerRes.code !== 200 || settlementRes.code !== 200) throw new Error()
    account.value = overview.data || {}
    ledgers.value = ledgerRes.data || []
    settlements.value = settlementRes.data || []
  } catch {
    error.value = '资金数据加载失败，请稍后重试'
    ElMessage.error(error.value)
  } finally { loading.value = false }
}
onMounted(load)
</script>

<template>
  <div class="page-container" v-loading="loading"><div class="finance-page">
    <div class="page-head"><div><h2>资金中心</h2><p>查看本店收入、余额和平台结算记录</p></div><el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button></div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" />
    <div class="metric-grid">
      <div class="metric-card primary"><span>可结算余额</span><strong>{{ money(account.availableBalance) }}</strong><em>管理员可为此余额创建结算</em></div>
      <div class="metric-card"><span>待结算余额</span><strong>{{ money(account.pendingBalance) }}</strong><em>订单完成前暂存</em></div>
      <div class="metric-card"><span>累计收入</span><strong>{{ money(account.totalIncome) }}</strong><em>支付成功订单收入</em></div>
      <div class="metric-card"><span>累计净收入</span><strong>{{ money(netIncome) }}</strong><em>收入扣除平台佣金</em></div>
    </div>
    <div class="content-grid">
      <section class="panel"><div class="section-head"><h3>资金流水</h3><span>{{ ledgers.length }} 条记录</span></div><el-table :data="ledgers" empty-text="暂无资金流水"><el-table-column prop="createTime" label="时间" min-width="170"/><el-table-column prop="ledgerType" label="类型" width="120"><template #default="{row}">{{ row.ledgerType === 10 ? '支付收入' : row.ledgerType === 20 ? '平台佣金' : row.ledgerType === 30 ? '退款冲正' : '结算变动' }}</template></el-table-column><el-table-column prop="amount" label="金额" width="120"><template #default="{row}"><span :class="row.direction === 10 ? 'credit' : 'debit'">{{ row.direction === 10 ? '+' : '-' }}{{ money(row.amount) }}</span></template></el-table-column><el-table-column prop="remark" label="说明" min-width="190" show-overflow-tooltip/></el-table></section>
      <section class="panel"><div class="section-head"><h3>结算记录</h3><span>{{ settlements.length }} 笔</span></div><el-table :data="settlements" empty-text="暂无结算记录"><el-table-column prop="settlementNo" label="结算单号" min-width="170" show-overflow-tooltip/><el-table-column prop="netAmount" label="金额" width="110"><template #default="{row}">{{ money(row.netAmount) }}</template></el-table-column><el-table-column prop="settlementStatus" label="状态" width="90"><template #default="{row}">{{ statusText[row.settlementStatus] || '-' }}</template></el-table-column><el-table-column prop="createTime" label="创建时间" min-width="160"/></el-table></section>
    </div>
  </div></div>
</template>

<style scoped>.finance-page{display:flex;flex-direction:column;gap:16px}.page-head{display:flex;align-items:center;justify-content:space-between}.page-head h2{margin:0;color:var(--bs-text-title);font-size:20px}.page-head p{margin:5px 0 0;color:var(--bs-text-muted);font-size:13px}.metric-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:12px}.metric-card,.panel{background:#fff;border:1px solid var(--bs-border-light);border-radius:8px;box-shadow:var(--bs-card-shadow)}.metric-card{padding:16px;border-left:3px solid #9eb8a9}.metric-card.primary{border-left-color:var(--bs-primary)}.metric-card span,.metric-card em{display:block;color:var(--bs-text-muted);font-size:13px;font-style:normal}.metric-card strong{display:block;margin:9px 0 5px;color:var(--bs-text-title);font-size:26px}.content-grid{display:grid;grid-template-columns:1.15fr 1fr;gap:16px}.panel{padding:18px;min-width:0}.section-head{display:flex;justify-content:space-between;align-items:center;margin-bottom:14px}.section-head h3{margin:0;color:var(--bs-text-title);font-size:16px}.section-head span{color:var(--bs-text-muted);font-size:12px}.credit{color:#2f8f63;font-weight:600}.debit{color:#c06b5a;font-weight:600}@media(max-width:1000px){.metric-grid{grid-template-columns:repeat(2,1fr)}.content-grid{grid-template-columns:1fr}}@media(max-width:600px){.metric-grid{grid-template-columns:1fr}.page-head{align-items:flex-start;gap:10px;flex-direction:column}}</style>
