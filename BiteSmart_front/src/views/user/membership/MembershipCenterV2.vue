<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMembershipHistory, getMembershipPlans, getMembershipStatus, buyMembership } from '../../../api/user/membership'

const loading = ref(false)
const plans = ref<any[]>([])
const current = ref<any>(null)
const history = ref<any[]>([])

const planNames: Record<number, string> = { 10: '月卡会员', 20: '季卡会员', 30: '年卡会员' }
const benefitLabels: Record<string, string> = {
  discount: '专属折扣',
  ai_advanced: 'AI 高级食谱',
  exclusive_combo: '会员专属套餐',
  priority_delivery: '优先配送',
  free_delivery: '免配送费'
}

const formatDateTime = (value?: string) => value ? value.replace('T', ' ').slice(0, 16) : '--'
const getPlanName = (type?: number) => planNames[type || 0] || '会员'
const getBenefits = (value: any) => {
  if (!value) return ['享受平台会员权益']
  let benefits = value
  if (typeof value === 'string') {
    try { benefits = JSON.parse(value) } catch { return [value] }
  }
  if (!benefits || typeof benefits !== 'object') return ['享受平台会员权益']
  return Object.entries(benefits).filter(([, enabled]) => enabled).map(([key, enabled]) => {
    if (key === 'discount') return `${Math.round((Number(enabled) || 1) * 100)} 折专属优惠`
    return benefitLabels[key] || key
  })
}

const currentPlan = computed(() => current.value ? plans.value.find(plan => plan.id === current.value.planId) : null)
const currentName = computed(() => currentPlan.value?.planName || getPlanName(current.value?.membershipType))
const hasActiveMembership = computed(() => current.value?.status === 10)

const load = async () => {
  loading.value = true
  try {
    const [planResponse, statusResponse, historyResponse] = await Promise.all([getMembershipPlans(), getMembershipStatus(), getMembershipHistory()])
    plans.value = planResponse.data || []
    current.value = statusResponse.data
    history.value = historyResponse.data || []
  } catch {
    ElMessage.error('加载会员信息失败')
  } finally {
    loading.value = false
  }
}

const buy = async (plan: any) => {
  try {
    // 后端 buyMembership 支持续期：已有有效会员时在当前有效期基础上顺延；当前无支付环节（测试环境）
    const renewTip = hasActiveMembership.value ? '，有效期将在当前基础上顺延' : ''
    await ElMessageBox.confirm(`确认购买${plan.planName}吗？价格 ¥${plan.price}${renewTip}。当前为免支付开通（测试环境），不会产生真实扣款。`, '购买确认')
    await buyMembership(plan.id)
    ElMessage.success(hasActiveMembership.value ? '续费成功，有效期已顺延' : '会员购买成功')
    await load()
  } catch { /* 用户取消购买 */ }
}

onMounted(load)
</script>

<template>
  <div class="membership-page">
    <section class="membership-hero">
      <div><span class="eyebrow">BITESMART MEMBERSHIP</span><h1>会员中心</h1><p>用更省心的方式，获得更完整的健康饮食服务。</p></div>
    </section>

    <section v-if="current" class="active-membership">
      <div><span class="section-kicker">CURRENT PLAN</span><h2>{{ currentName }}</h2><p>{{ formatDateTime(current.startTime) }} 至 {{ formatDateTime(current.endTime) }}</p></div>
      <el-tag type="success" effect="dark">{{ current.status === 10 ? '有效会员' : '已过期' }}</el-tag>
    </section>

    <section class="plans-section">
      <div class="section-heading"><div><span class="section-kicker">CHOOSE YOUR PLAN</span><h2>选择适合你的会员方案</h2></div><span class="heading-note">当前为免支付开通（测试环境），不会产生真实扣款</span></div>
      <div v-loading="loading" class="plans-grid">
        <article v-for="(plan, index) in plans" :key="plan.id" class="plan-card" :class="{ featured: index === 1 }">
          <span v-if="index === 1" class="featured-label">更受欢迎</span>
          <span class="plan-type">{{ getPlanName(plan.planType) }}</span>
          <h3>{{ plan.planName }}</h3>
          <div class="plan-price"><small>¥</small>{{ plan.price }}</div>
          <p class="plan-duration">{{ plan.validDays }} 天有效</p>
          <ul><li v-for="benefit in getBenefits(plan.benefits)" :key="benefit">{{ benefit }}</li></ul>
          <el-button class="plan-button" type="primary" @click="buy(plan)">{{ hasActiveMembership ? '续费延长有效期' : '立即开通' }}</el-button>
        </article>
      </div>
      <el-empty v-if="!loading && !plans.length" description="暂无可购买的会员方案" />
    </section>

    <section class="history-section">
      <div class="section-heading"><div><span class="section-kicker">MEMBERSHIP HISTORY</span><h2>购买记录</h2></div></div>
      <el-table :data="history" class="history-table" empty-text="暂无购买记录">
        <el-table-column label="会员方案" min-width="160"><template #default="{ row }">{{ getPlanName(row.membershipType) }}</template></el-table-column>
        <el-table-column label="金额" width="140"><template #default="{ row }">¥{{ row.payAmount }}</template></el-table-column>
        <el-table-column label="有效期" min-width="300"><template #default="{ row }">{{ formatDateTime(row.startTime) }} 至 {{ formatDateTime(row.endTime) }}</template></el-table-column>
        <el-table-column label="状态" width="120"><template #default="{ row }"><el-tag :type="row.status === 10 ? 'success' : 'info'" effect="plain">{{ row.status === 10 ? '有效' : row.status === 20 ? '已过期' : '已退款' }}</el-tag></template></el-table-column>
      </el-table>
    </section>
  </div>
</template>

<style scoped>
.membership-page{max-width:1180px;margin:0 auto;padding:42px 24px 80px;color:#253129}.membership-hero{display:flex;align-items:flex-end;justify-content:space-between;padding:8px 0 28px;border-bottom:1px solid #dfe7df}.eyebrow,.section-kicker{color:var(--orange);font-size:10px;font-weight:800;letter-spacing:.16em}.membership-hero h1{margin:9px 0 7px;font-family:"Source Han Serif SC","Songti SC",serif;font-size:32px;line-height:1.1}.membership-hero p{margin:0;color:#718078;font-size:13px}.active-membership{display:flex;align-items:center;justify-content:space-between;gap:24px;margin-top:24px;padding:24px 28px;background:var(--green-ink);color:#fff}.active-membership .section-kicker{color:var(--orange)}.active-membership h2{margin:10px 0 6px;font-family:"Source Han Serif SC","Songti SC",serif;font-size:26px}.active-membership p{margin:0;color:#d7e6d9;font-size:12px}.plans-section,.history-section{margin-top:24px;padding:24px 28px;background:#fff;border:1px solid #e1e9e2}.section-heading{display:flex;align-items:flex-end;justify-content:space-between;gap:20px;margin-bottom:22px}.section-heading h2{margin:7px 0 0;font-family:"Source Han Serif SC","Songti SC",serif;font-size:22px}.heading-note{color:#8a968e;font-size:11px}.plans-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:16px}.plan-card{position:relative;display:flex;flex-direction:column;min-height:360px;padding:23px 22px;border:1px solid #dfe7df;background:#fbfdfb}.plan-card.featured{border:2px solid var(--green);padding:22px 21px}.featured-label{position:absolute;top:-1px;right:20px;padding:5px 10px;background:var(--green);color:#fff;font-size:10px}.plan-type{color:var(--orange);font-size:10px;font-weight:800;letter-spacing:.12em}.plan-card h3{margin:12px 0 0;font-family:"Source Han Serif SC","Songti SC",serif;font-size:20px}.plan-price{margin-top:22px;color:var(--green);font-family:Georgia,serif;font-size:38px;font-weight:700}.plan-price small{margin-right:4px;font-family:inherit;font-size:16px}.plan-duration{margin:4px 0 18px;color:#8a968e;font-size:12px}.plan-card ul{display:grid;gap:10px;margin:0 0 22px;padding:16px 0;border-top:1px solid #edf1ec;list-style:none;color:#56655c;font-size:12px}.plan-card li::before{content:'✓';margin-right:8px;color:var(--orange);font-weight:700}.plan-button{width:100%;margin-top:auto}.history-table{--el-table-border-color:#e5ece6;--el-table-header-bg-color:#f4f8f3}.history-table :deep(th.el-table__cell){height:46px;background:#f4f8f3;color:#718078;font-size:12px}.history-table :deep(td.el-table__cell){height:56px;color:#253129;font-size:13px}@media(max-width:800px){.plans-grid{grid-template-columns:1fr 1fr}.membership-hero{align-items:flex-start;gap:20px;flex-direction:column}}@media(max-width:600px){.membership-page{padding:30px 16px 58px}.active-membership{align-items:flex-start;flex-direction:column}.plans-section,.history-section{padding:18px}.plans-grid{grid-template-columns:1fr}.section-heading{align-items:flex-start;flex-direction:column;gap:8px}.history-table{font-size:12px}}
</style>
