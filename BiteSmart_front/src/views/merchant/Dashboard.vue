<script setup lang="ts">
import { ref } from 'vue'
import { Money, ShoppingCart, Bowl, Star } from '@element-plus/icons-vue'
import StatCard from '../../components/common/StatCard.vue'

const stats = ref({
  todaySales: 2450,
  todayOrders: 32,
  hotDish: '鸡胸肉沙拉',
  rating: 4.9
})
</script>

<template>
  <div class="dashboard">
    <div class="stats-row">
      <StatCard
        :icon="Money"
        label="今日销售额"
        :value="'¥' + stats.todaySales"
      />
      <StatCard
        :icon="ShoppingCart"
        label="今日订单"
        :value="stats.todayOrders"
      />
      <StatCard
        :icon="Bowl"
        label="热销菜品"
        :value="stats.hotDish"
      />
      <StatCard
        :icon="Star"
        label="店铺评分"
        :value="stats.rating"
      />
    </div>
    
    <div class="card-panel">
      <div class="card-header">
        <h3>待处理订单</h3>
        <button class="btn btn-primary btn-sm">查看全部</button>
      </div>
      <div style="padding-top: 20px;">
        <el-table :data="[]" border>
          <el-table-column prop="orderNo" label="订单号" />
          <el-table-column prop="items" label="菜品" />
          <el-table-column prop="amount" label="金额" />
          <el-table-column prop="time" label="下单时间" />
          <el-table-column label="操作">
            <template #default>
              <el-button size="small" type="primary">接单</el-button>
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