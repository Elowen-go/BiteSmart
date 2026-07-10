<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Money, ShoppingCart, UserFilled, Coin } from '@element-plus/icons-vue'
import StatCard from '../../components/common/StatCard.vue'
import { getTodayStats, getTopDishes } from '../../api/merchant/statistics'

const loading = ref(false)
const todayStats = ref({
  orderCount: 0,
  revenue: 0,
  newUserCount: 0,
  avgOrderAmount: 0
})
const topDishes = ref<any[]>([])
const pendingOrders = ref<any[]>([])

const fetchData = async () => {
  loading.value = true
  try {
    const [todayRes, topRes] = await Promise.all([
      getTodayStats(),
      getTopDishes({ limit: 10 })
    ])
    if (todayRes.code === 200) {
      todayStats.value = todayRes.data
    }
    if (topRes.code === 200) {
      topDishes.value = topRes.data.list || topRes.data
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
        :value="'¥' + todayStats.revenue"
      />
      <StatCard
        :icon="ShoppingCart"
        label="今日订单"
        :value="todayStats.orderCount"
      />
      <StatCard
        :icon="UserFilled"
        label="新用户数"
        :value="todayStats.newUserCount"
      />
      <StatCard
        :icon="Coin"
        label="平均订单金额"
        :value="'¥' + todayStats.avgOrderAmount"
      />
    </div>

    <div class="card-panel" style="margin-top: var(--bs-spacing-lg);">
      <div class="card-header">
        <h3>热销菜品排行</h3>
      </div>
      <div style="padding-top: 20px;">
        <el-table :data="topDishes" border>
          <el-table-column type="index" label="排名" width="80" />
          <el-table-column prop="dishName" label="菜品名称" />
          <el-table-column prop="soldCount" label="销量" />
          <el-table-column prop="revenue" label="营收">
            <template #default="{ row }">
              ¥{{ row.revenue }}
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

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
