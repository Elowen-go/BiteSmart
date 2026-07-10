<script setup lang="ts">
import { ref } from 'vue'
import { Star, Sell, Tickets, Clock } from '@element-plus/icons-vue'
import StatCard from '../../components/common/StatCard.vue'

const stats = ref({
  recommendDish: '今日推荐',
  hotCombo: '热销套餐',
  newDish: '新品上市',
  discount: '限时优惠'
})
</script>

<template>
  <div class="home-page">
    <div class="stats-row">
      <StatCard
        :icon="Star"
        label="今日推荐"
        :value="stats.recommendDish"
      />
      <StatCard
        :icon="Sell"
        label="热销套餐"
        :value="stats.hotCombo"
      />
      <StatCard
        :icon="Tickets"
        label="新品上市"
        :value="stats.newDish"
      />
      <StatCard
        :icon="Clock"
        label="限时优惠"
        :value="stats.discount"
      />
    </div>
    
    <div class="card-panel">
      <div class="card-header">
        <h3>推荐菜品</h3>
      </div>
      <div style="padding-top: 20px;">
        <el-table :data="[]" border>
          <el-table-column prop="name" label="菜品名称" />
          <el-table-column prop="price" label="价格" />
          <el-table-column prop="rating" label="评分" />
          <el-table-column label="操作">
            <template #default>
              <el-button size="small">查看详情</el-button>
              <el-button size="small" type="primary">加入购物车</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-page {
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

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>