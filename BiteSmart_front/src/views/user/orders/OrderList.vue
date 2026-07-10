<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getOrderList, cancelOrder } from '../../../api/user/orders'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const orders = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const detailVisible = ref(false)
const currentDetail = ref<any>(null)

const orderStatusMap: Record<number, string> = {
  0: '待付款',
  1: '待配送',
  2: '配送中',
  3: '已完成',
  4: '已取消'
}

const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await getOrderList({ page: currentPage.value, size: pageSize.value })
    orders.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error('获取订单列表失败', e)
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchOrders()
}

const handleViewDetail = async (row: any) => {
  currentDetail.value = row
  detailVisible.value = true
}

const handleCancel = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定要取消该订单吗？', '提示')
    await cancelOrder(row.id)
    ElMessage.success('订单已取消')
    fetchOrders()
  } catch (e) {
    // 取消不做处理
  }
}

const getStatusTag = (status: number) => {
  const map: Record<number, string> = { 0: 'warning', 1: 'primary', 2: '', 3: 'success', 4: 'info' }
  return (map[status] || '') as 'success' | 'warning' | 'primary' | 'info' | 'danger'
}

onMounted(() => {
  fetchOrders()
})
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>我的订单</h3>
      </div>
      <div style="padding-top: 20px;">
        <el-table v-loading="loading" :data="orders" border>
          <el-table-column prop="orderNo" label="订单号" min-width="180" />
          <el-table-column prop="payAmount" label="金额" width="120">
            <template #default="{ row }">
              <span>¥{{ row.payAmount }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusTag(row.orderStatus)">
                {{ orderStatusMap[row.orderStatus] || '未知' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="时间" width="180" />
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="handleViewDetail(row)">查看详情</el-button>
              <el-button size="small" type="danger" @click="handleCancel(row)" v-if="row.orderStatus === 0">取消订单</el-button>
              <el-button size="small" type="primary" v-if="row.orderStatus === 3">去评价</el-button>
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

    <el-dialog v-model="detailVisible" title="订单详情" width="600px">
      <template v-if="currentDetail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ currentDetail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">{{ orderStatusMap[currentDetail.orderStatus] }}</el-descriptions-item>
          <el-descriptions-item label="总金额">¥{{ currentDetail.totalAmount }}</el-descriptions-item>
          <el-descriptions-item label="实付金额">¥{{ currentDetail.payAmount }}</el-descriptions-item>
          <el-descriptions-item label="收货地址" :span="2">{{ currentDetail.deliveryAddress }}</el-descriptions-item>
          <el-descriptions-item label="收件人">{{ currentDetail.receiverName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentDetail.receiverPhone }}</el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">{{ currentDetail.createTime }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ currentDetail.remark || '无' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>
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
