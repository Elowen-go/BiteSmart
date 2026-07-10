<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOrderList } from '../../../api/user/orders'

const router = useRouter()
const loading = ref(false)
const orders = ref<any[]>([])

const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await getOrderList()
    if (res.code === 200) {
      orders.value = (res.data.list || res.data || []).filter(
        (order: any) => order.status === 30 || order.status === 40
      )
    }
  } catch (e) {
    console.error('获取配送订单失败', e)
  } finally {
    loading.value = false
  }
}

const goTracking = (orderId: number) => {
  router.push(`/user/delivery/${orderId}`)
}

const statusMap: Record<number, string> = {
  30: '配送中',
  40: '已送达'
}

onMounted(() => {
  fetchOrders()
})
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>配送追踪</h3>
      </div>
      <el-table :data="orders" border v-loading="loading" @row-click="(row) => goTracking(row.id)">
        <el-table-column prop="orderNo" label="订单编号" width="200" />
        <el-table-column prop="receiverName" label="收货人" width="120" />
        <el-table-column prop="receiverPhone" label="联系电话" width="130" />
        <el-table-column prop="address" label="配送地址" show-overflow-tooltip />
        <el-table-column label="配送状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 30 ? 'warning' : 'success'">
              {{ statusMap[row.status] || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="180" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click.stop="goTracking(row.id)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!loading && orders.length === 0" style="text-align: center; padding: 40px; color: var(--bs-text-muted);">
        暂无配送中或已送达的订单
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