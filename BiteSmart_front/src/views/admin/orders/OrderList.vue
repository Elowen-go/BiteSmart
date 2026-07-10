<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getOrderList, getOrderDetail } from '../../../api/admin/orders'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const detailDialogVisible = ref(false)
const detailData = ref<any>(null)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getOrderList({ pageNum: pageNum.value, pageSize: pageSize.value })
    if (res.code === 200) {
      tableData.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } catch (err) {
    console.error('获取订单列表失败', err)
  } finally {
    loading.value = false
  }
}

const orderStatusMap: Record<number, string> = { 10: '待付款', 20: '已付款', 30: '待接单', 40: '配送中', 50: '已完成', 60: '已取消' }
const orderStatusTypeMap: Record<number, 'success' | 'warning' | 'primary' | 'info' | ''> = { 10: 'warning', 20: 'success', 30: 'primary', 40: '', 50: 'success', 60: 'info' }

const handleDetail = async (row: any) => {
  try {
    const res = await getOrderDetail(row.id)
    if (res.code === 200) {
      detailData.value = res.data
      detailDialogVisible.value = true
    }
  } catch (err) {
    console.error('获取订单详情失败', err)
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>订单管理</h3>
      </div>
      <div style="padding-top: 20px;">
        <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
          <el-table-column prop="orderNo" label="订单号" min-width="180" />
          <el-table-column label="订单金额" width="120">
            <template #default="{ row }">
              ¥{{ row.totalAmount?.toFixed(2) }}
            </template>
          </el-table-column>
          <el-table-column label="实付金额" width="120">
            <template #default="{ row }">
              ¥{{ row.payAmount?.toFixed(2) }}
            </template>
          </el-table-column>
          <el-table-column label="订单状态" width="100">
            <template #default="{ row }">
              <el-tag :type="orderStatusTypeMap[row.orderStatus] || 'info'" size="small">
                {{ orderStatusMap[row.orderStatus] || '未知' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="deliveryAddress" label="配送地址" min-width="220" show-overflow-tooltip />
          <el-table-column label="创建时间" width="170">
            <template #default="{ row }">
              {{ row.createTime?.slice(0, 16) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" link @click="handleDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div style="display:flex;justify-content:flex-end;padding-top:16px;">
          <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="loadData"
            @size-change="loadData"
          />
        </div>
      </div>
    </div>

    <el-dialog v-model="detailDialogVisible" title="订单详情" width="600px">
      <div v-if="detailData" class="order-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ detailData.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="orderStatusTypeMap[detailData.orderStatus] || 'info'" size="small">
              {{ orderStatusMap[detailData.orderStatus] || '未知' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="订单金额">¥{{ detailData.totalAmount?.toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="实付金额">¥{{ detailData.payAmount?.toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="配送地址" :span="2">{{ detailData.deliveryAddress }}</el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">{{ detailData.createTime }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
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
</style>
