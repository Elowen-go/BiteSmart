<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMembershipPlans, getMembershipStatus, buyMembership } from '../../../api/user/membership'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star } from '@element-plus/icons-vue'

const loading = ref(false)
const plans = ref<any[]>([])
const membershipStatus = ref<any>(null)

const fetchData = async () => {
  loading.value = true
  try {
    const [plansRes, statusRes] = await Promise.all([
      getMembershipPlans(),
      getMembershipStatus()
    ])
    plans.value = plansRes.data || []
    membershipStatus.value = statusRes.data || null
  } catch (e) {
    console.error('获取会员信息失败', e)
  } finally {
    loading.value = false
  }
}

const handleBuy = async (plan: any) => {
  try {
    await ElMessageBox.confirm(
      `确定购买「${plan.name}」？价格：¥${plan.price}`,
      '购买确认'
    )
    await buyMembership(plan.id)
    ElMessage.success('购买成功！')
    fetchData()
  } catch (e) {
    // 取消不做处理
  }
}

const getPlanIcon = (name: string) => {
  if (!name) return '⭐'
  if (name.includes('月')) return '📅'
  if (name.includes('季')) return '🌸'
  if (name.includes('年')) return '🌟'
  return '⭐'
}

const getMembershipStatusText = (status: number) => {
  const map: Record<number, string> = { 0: '已过期', 1: '正常', 2: '即将过期' }
  return map[status] || '未知'
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="page-container">
    <!-- 当前会员状态 -->
    <div class="card-panel status-card" v-if="membershipStatus">
      <div class="card-header">
        <h3>当前会员</h3>
      </div>
      <div class="status-body">
        <div class="status-info">
          <el-tag :type="membershipStatus.status === 1 ? 'success' : membershipStatus.status === 2 ? 'warning' : 'info'" size="large">
            {{ getMembershipStatusText(membershipStatus.status) }}
          </el-tag>
          <span class="plan-name">{{ membershipStatus.planName }}</span>
        </div>
        <div class="status-time">
          <span>有效期：{{ membershipStatus.startTime }} ~ {{ membershipStatus.endTime }}</span>
        </div>
      </div>
    </div>

    <!-- 会员套餐 -->
    <div class="card-panel">
      <div class="card-header">
        <h3>会员套餐</h3>
      </div>
      <div v-loading="loading" style="padding-top: 20px;">
        <div class="plans-grid">
          <div v-for="plan in plans" :key="plan.id" class="plan-card">
            <div class="plan-icon">{{ getPlanIcon(plan.name) }}</div>
            <h4 class="plan-name">{{ plan.name }}</h4>
            <p class="plan-desc">{{ plan.description }}</p>
            <div class="plan-price">
              <span class="price-symbol">¥</span>
              <span class="price-value">{{ plan.price }}</span>
            </div>
            <p class="plan-duration">{{ plan.durationDays }}天有效期</p>
            <el-button
              type="primary"
              size="large"
              style="width: 100%;"
              :disabled="membershipStatus?.status === 1"
              @click="handleBuy(plan)"
            >
              {{ membershipStatus?.status === 1 ? '已订阅' : '立即购买' }}
            </el-button>
          </div>
          <div v-if="!loading && plans.length === 0" class="empty-state">
            <el-empty description="暂无会员套餐" />
          </div>
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

.card-panel {
  background: var(--bs-card-bg);
  border-radius: var(--bs-radius-md);
  box-shadow: var(--bs-card-shadow);
  padding: var(--bs-spacing-lg);
  margin-bottom: var(--bs-spacing-lg);
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

.status-card {
  margin-bottom: var(--bs-spacing-lg);
}

.status-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
}

.status-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.status-info .plan-name {
  font-size: var(--bs-font-size-lg);
  font-weight: 600;
  color: var(--bs-text-title);
}

.status-time {
  font-size: var(--bs-font-size-sm);
  color: var(--bs-text-muted);
}

.plans-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--bs-spacing-lg);
}

.plan-card {
  border: 1px solid var(--bs-border-color);
  border-radius: var(--bs-radius-md);
  padding: 32px 24px;
  text-align: center;
  transition: all 0.3s;
}

.plan-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.1);
  border-color: var(--bs-primary);
}

.plan-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.plan-name {
  font-size: var(--bs-font-size-xl);
  font-weight: 600;
  color: var(--bs-text-title);
  margin-bottom: 8px;
}

.plan-desc {
  font-size: var(--bs-font-size-sm);
  color: var(--bs-text-muted);
  margin-bottom: 16px;
}

.plan-price {
  margin-bottom: 8px;
}

.price-symbol {
  font-size: var(--bs-font-size-lg);
  color: var(--bs-text-muted);
  vertical-align: super;
}

.price-value {
  font-size: 36px;
  font-weight: 700;
  color: var(--bs-primary);
}

.plan-duration {
  font-size: var(--bs-font-size-sm);
  color: var(--bs-text-muted);
  margin-bottom: 24px;
}

.empty-state {
  grid-column: 1 / -1;
  padding: 60px 0;
}

@media (max-width: 768px) {
  .status-body {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .plans-grid {
    grid-template-columns: 1fr;
  }
}
</style>
