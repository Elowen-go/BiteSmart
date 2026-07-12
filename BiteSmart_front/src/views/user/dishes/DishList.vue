<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDishList } from '../../../api/user/dishes'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const dishes = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const fetchDishes = async () => {
  loading.value = true
  try {
    const res = await getDishList({ page: currentPage.value, size: pageSize.value })
    dishes.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error('获取菜品列表失败', e)
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchDishes()
}

const handleViewDetail = (row: any) => {
  ElMessage.info(`查看菜品详情: ${row.dishName}`)
}

const handleAddToCart = (row: any) => {
  ElMessage.success(`已加入购物车: ${row.dishName}`)
}

onMounted(() => {
  fetchDishes()
})
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>菜品浏览</h3>
      </div>
      <div style="padding-top: 20px;">
        <el-table v-loading="loading" :data="dishes" border>
          <el-table-column prop="name" label="菜品名称" min-width="140" />
          <el-table-column prop="price" label="价格" width="120">
            <template #default="{ row }">
              <span>¥{{ row.price }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="calories" label="热量(千卡)" width="110" />
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="handleViewDetail(row)">查看详情</el-button>
              <el-button size="small" type="primary" @click="handleAddToCart(row)">加入购物车</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="handlePageChange"
          />
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
</style>
