﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrderList, getOrderDetail, acceptOrder, rejectOrder, prepareOrder, doneOrder } from '../../../api/merchant/orders'
import type { MerchantOrder } from '../../../api/merchant/orders'

const loading = ref(false)
const orderList = ref<MerchantOrder[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const detailVisible = ref(false)
const detailData = ref<any>(null)

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getOrderList({ page: page.value, size: size.value })
    if (res.code === 200) {
      orderList.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    console.error('获取订单列表失败', e)
  } finally {
    loading.value = false
  }
}

const handleViewDetail = async (row: any) => {
  try {
    const res = await getOrderDetail(row.id)
    if (res.code === 200) {
      detailData.value = res.data
      detailVisible.value = true
    }
  } catch (e) {
    ElMessage.error('获取订单详情失败')
  }
}

const handleAccept = (row: any) => {
  ElMessageBox.confirm(`确定要接单「${row.orderNo}」吗？`, '接单确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  }).then(async () => {
    try {
      const res = await acceptOrder(row.id)
      if (res.code === 200) {
        ElMessage.success('已接单')
        fetchList()
      } else {
        ElMessage.error(res.message || '操作失败')
      }
    } catch (e) {
      ElMessage.error('操作失败')
    }
  }).catch(() => {})
}

const handleReject = (row: any) => {
  ElMessageBox.prompt('请输入拒绝原因', '拒绝订单', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPlaceholder: '请输入拒绝原因'
  }).then(async ({ value }) => {
    try {
      const res = await rejectOrder(row.id, value)
      if (res.code === 200) {
        ElMessage.success('已拒绝')
        fetchList()
      } else {
        ElMessage.error(res.message || '操作失败')
      }
    } catch (e) {
      ElMessage.error('操作失败')
    }
  }).catch(() => {})
}

const handlePrepare = async (row: any) => {
  try {
    const res = await prepareOrder(row.id)
    if (res.code === 200) {
      ElMessage.success('已开始备餐')
      fetchList()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const handleDone = async (row: any) => {
  try {
    const res = await doneOrder(row.id)
    if (res.code === 200) {
      ElMessage.success('已完成')
      fetchList()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const handlePageChange = (val: number) => {
  page.value = val
  fetchList()
}

const handleSizeChange = (val: number) => {
  size.value = val
  page.value = 1
  fetchList()
}

const getStatusTag = (status: number): 'success' | 'warning' | 'primary' | 'info' | 'danger' => {
  const map: Record<number, 'success' | 'warning' | 'primary' | 'info' | 'danger'> = { 30: 'warning', 40: 'primary', 50: 'success' }
  return map[status] || 'info'
}

const getStatusLabel = (status: number) => {
  const map: Record<number, string> = { 30: '待接单', 40: '配送中', 50: '已完成' }
  return map[status] || '未知'
}

onMounted(() => {
  fetchList()
})
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>订单处理</h3>
      </div>
      <div style="padding-top: 20px;">
        <el-table :data="orderList" border v-loading="loading">
          <el-table-column prop="orderNo" label="订单号" width="200" />
          <el-table-column prop="payAmount" label="支付金额" width="120">
            <template #default="{ row }">
              ¥{{ row.payAmount }}
            </template>
          </el-table-column>
          <el-table-column prop="deliveryAddress" label="配送地址" min-width="200" show-overflow-tooltip />
          <el-table-column prop="orderStatus" label="订单状态" width="120">
            <template #default="{ row }">
              <el-tag :type="getStatusTag(row.orderStatus)">{{ getStatusLabel(row.orderStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="下单时间" width="180" />
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="handleViewDetail(row)">详情</el-button>
              <el-button v-if="row.orderStatus === 30" size="small" type="primary" @click="handleAccept(row)">接单</el-button>
              <el-button v-if="row.orderStatus === 30" size="small" type="danger" @click="handleReject(row)">拒绝</el-button>
              <el-button v-if="row.orderStatus === 30" size="small" @click="handlePrepare(row)">备餐</el-button>
              <el-button v-if="row.orderStatus === 40" size="small" type="success" @click="handleDone(row)">完成</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div style="display: flex; justify-content: flex-end; margin-top: 20px;">
          <el-pagination
            v-if="total > 0"
            :current-page="page"
            :page-size="size"
            :total="total"
            :page-sizes="[5, 10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="订单详情" width="600px">
      <template v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ detailData.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">{{ getStatusLabel(detailData.orderStatus) }}</el-descriptions-item>
          <el-descriptions-item label="支付金额">¥{{ detailData.payAmount }}</el-descriptions-item>
          <el-descriptions-item label="支付方式">{{ detailData.payMethod === 1 ? '在线支付' : '货到付款' }}</el-descriptions-item>
          <el-descriptions-item label="收货人">{{ detailData.receiverName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detailData.receiverPhone }}</el-descriptions-item>
          <el-descriptions-item label="配送地址" :span="2">{{ detailData.deliveryAddress }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detailData.remark || '无' }}</el-descriptions-item>
          <el-descriptions-item label="下单时间" :span="2">{{ detailData.createTime }}</el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
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

