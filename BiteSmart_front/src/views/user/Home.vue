<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Star, Sell, Tickets, Clock } from '@element-plus/icons-vue'
import StatCard from '../../components/common/StatCard.vue'
import { getDishList } from '../../api/user/dishes'
import { getComboList } from '../../api/user/combos'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const dishes = ref<any[]>([])
const combos = ref<any[]>([])

const fetchData = async () => {
  loading.value = true
  try {
    const [dishRes, comboRes] = await Promise.all([
      getDishList({ page: 1, size: 10 }),
      getComboList({ page: 1, size: 4 })
    ])
    dishes.value = dishRes.data?.list || []
    combos.value = comboRes.data?.list || []
  } catch (e) {
    console.error('获取数据失败', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="home-page">
    <div class="stats-row">
      <StatCard
        :icon="Star"
        label="今日推荐"
        :value="dishes.length > 0 ? dishes[0]?.name : '暂无推荐'"
      />
      <StatCard
        :icon="Sell"
        label="热销套餐"
        :value="combos.length > 0 ? combos[0]?.name : '暂无套餐'"
      />
      <StatCard
        :icon="Tickets"
        label="菜品数量"
        :value="`共 ${dishes.length} 款`"
      />
      <StatCard
        :icon="Clock"
        label="限时优惠"
        :value="combos.length > 1 ? combos[1]?.name : '暂无优惠'"
      />
    </div>
    
    <div class="card-panel">
      <div class="card-header">
        <h3>推荐菜品</h3>
      </div>
      <div style="padding-top: 20px;">
        <el-table v-loading="loading" :data="dishes" border>
          <el-table-column prop="name" label="菜品名称" />
          <el-table-column prop="price" label="价格" width="120">
            <template #default="{ row }">
              <span>¥{{ row.price }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="calories" label="热量(千卡)" width="120" />
          <el-table-column label="操作" width="200">
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