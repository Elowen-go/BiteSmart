﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, View } from '@element-plus/icons-vue'
import { getOrderList, getOrderDetail, acceptOrder, rejectOrder, prepareOrder, doneOrder } from '../../../api/merchant/orders'
import type { MerchantOrder } from '../../../api/merchant/orders'

const loading = ref(false)
const orderList = ref<MerchantOrder[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const detailVisible = ref(false)
const detailData = ref<any>(null)
const activeStatus = ref<number | 'all'>('all')

const statusOptions = [
  { label: '全部', value: 'all' as const },
  { label: '待接单', value: 20 },
  { label: '备餐中', value: 30 },
  { label: '配送中', value: 40 },
  { label: '已完成', value: 50 },
  { label: '已取消', value: 60 }
]

const statusConfig: Record<number, { label: string; tag: 'success' | 'warning' | 'primary' | 'info' | 'danger'; className: string }> = {
  10: { label: '待支付', tag: 'info', className: 'muted' },
  20: { label: '待接单', tag: 'warning', className: 'urgent' },
  30: { label: '备餐中', tag: 'primary', className: 'working' },
  40: { label: '配送中', tag: 'primary', className: 'delivery' },
  50: { label: '已完成', tag: 'success', className: 'done' },
  60: { label: '已取消', tag: 'info', className: 'muted' }
}

const filteredOrderList = computed(() => {
  if (activeStatus.value === 'all') return orderList.value
  return orderList.value.filter((item) => item.orderStatus === activeStatus.value)
})

const orderStats = computed(() => {
  const countByStatus = (status: number) => orderList.value.filter((item) => item.orderStatus === status).length
  const amount = orderList.value
    .filter((item) => item.orderStatus === 50)
    .reduce((sum, item) => sum + Number(item.payAmount || 0), 0)

  return [
    { label: '待接单', value: countByStatus(20), status: 20, hint: '需要尽快确认' },
    { label: '备餐中', value: countByStatus(30), status: 30, hint: '后厨正在处理' },
    { label: '配送中', value: countByStatus(40), status: 40, hint: '关注配送进度' },
    { label: '已完成', value: countByStatus(50), status: 50, hint: `本页完成 ¥${amount.toFixed(2)}` }
  ]
})

const currentOrder = computed(() => detailData.value?.order || detailData.value)
const currentItems = computed(() => detailData.value?.items || [])

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
    confirmButtonText: '接单',
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

const handleStatusChange = (status: number | 'all') => {
  activeStatus.value = status
}

const getStatusTag = (status: number) => {
  return statusConfig[status]?.tag || 'info'
}

const getStatusLabel = (status: number) => {
  return statusConfig[status]?.label || '未知'
}

const getStatusClass = (status: number) => {
  return statusConfig[status]?.className || 'muted'
}

const formatAmount = (amount: number | string | undefined) => {
  return Number(amount || 0).toFixed(2)
}
const imageUrl = (value: string) => value ? (value.startsWith('http') ? value : `/api/files/download${value}`) : ''

onMounted(() => {
  fetchList()
})
</script>

<template>
  <div class="page-container">
    <div class="order-page">
      <div class="page-head">
        <div>
          <h2>订单处理</h2>
          <p>按接单、备餐、出餐顺序处理门店订单</p>
        </div>
        <el-button :icon="Refresh" :loading="loading" @click="fetchList">刷新</el-button>
      </div>

      <div class="summary-grid">
        <button
          v-for="item in orderStats"
          :key="item.label"
          class="summary-item"
          :class="[getStatusClass(item.status), { active: activeStatus === item.status }]"
          @click="handleStatusChange(item.status)"
        >
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <em>{{ item.hint }}</em>
        </button>
      </div>

      <div class="card-panel order-panel">
        <div class="toolbar">
          <el-segmented v-model="activeStatus" :options="statusOptions" />
          <span class="toolbar-count">当前 {{ filteredOrderList.length }} 单</span>
        </div>

        <el-table :data="filteredOrderList" v-loading="loading" class="order-table" empty-text="暂无符合条件的订单">
          <el-table-column prop="orderNo" label="订单信息" min-width="220">
            <template #default="{ row }">
              <div class="order-no">{{ row.orderNo }}</div>
              <div class="order-time">{{ row.createTime }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="receiverName" label="收货人" width="150">
            <template #default="{ row }">
              <div class="receiver-name">{{ row.receiverName || '未填写' }}</div>
              <div class="receiver-phone">{{ row.receiverPhone || '-' }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="deliveryAddress" label="配送地址" min-width="240" show-overflow-tooltip />
          <el-table-column prop="payAmount" label="实付金额" width="130" align="right">
            <template #default="{ row }">
              <span class="amount">¥{{ formatAmount(row.payAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="orderStatus" label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="getStatusTag(row.orderStatus)">{{ getStatusLabel(row.orderStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.remark || '无' }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="250" fixed="right" align="right">
            <template #default="{ row }">
              <el-button size="small" :icon="View" @click="handleViewDetail(row)">详情</el-button>
              <el-button v-if="row.orderStatus === 20" size="small" type="primary" @click="handleAccept(row)">接单</el-button>
              <el-button v-if="row.orderStatus === 20" size="small" type="danger" plain @click="handleReject(row)">拒绝</el-button>
              <el-button v-if="row.orderStatus === 30" size="small" @click="handlePrepare(row)">备餐</el-button>
              <el-button v-if="row.orderStatus === 40" size="small" type="success" @click="handleDone(row)">完成</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-row">
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

    <el-dialog v-model="detailVisible" title="订单详情" width="720px">
      <template v-if="currentOrder">
        <div class="party-strip"><span>买家：<strong>{{ detailData.buyer?.nickname || detailData.buyer?.username || currentOrder.userId }}</strong></span><span>商家：<strong>{{ detailData.merchant?.shopName || currentOrder.merchantId }}</strong></span></div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">{{ getStatusLabel(currentOrder.orderStatus) }}</el-descriptions-item>
          <el-descriptions-item label="支付金额">¥{{ formatAmount(currentOrder.payAmount) }}</el-descriptions-item>
          <el-descriptions-item label="支付方式">{{ currentOrder.payMethod === 1 ? '在线支付' : '货到付款' }}</el-descriptions-item>
          <el-descriptions-item label="收货人">{{ currentOrder.receiverName || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentOrder.receiverPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="配送地址" :span="2">{{ currentOrder.deliveryAddress }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ currentOrder.remark || '无' }}</el-descriptions-item>
          <el-descriptions-item label="下单时间" :span="2">{{ currentOrder.createTime }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="currentItems.length" class="detail-section">
          <h4>商品明细</h4>
          <el-table :data="currentItems" size="small">
            <el-table-column label="商品" min-width="220"><template #default="{ row }"><div class="order-item-cell"><img v-if="row.snapshotImage" :src="imageUrl(row.snapshotImage)" alt="商品图片" /><span v-else class="order-item-placeholder">餐</span><div><strong>{{ row.snapshotName || '订单商品' }}</strong><small>{{ row.itemType === 20 ? '套餐' : '菜品' }}</small></div></div></template></el-table-column>
            <el-table-column prop="quantity" label="数量" width="90" />
            <el-table-column prop="snapshotPrice" label="单价" width="110">
              <template #default="{ row }">¥{{ formatAmount(row.snapshotPrice) }}</template>
            </el-table-column>
            <el-table-column prop="subTotal" label="小计" width="110">
              <template #default="{ row }">¥{{ formatAmount(row.subTotal) }}</template>
            </el-table-column>
          </el-table>
        </div>
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

.order-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-head h2 {
  margin: 0;
  color: var(--bs-text-title);
  font-size: 20px;
  font-weight: 650;
}

.page-head p {
  margin-top: 4px;
  color: var(--bs-text-muted);
  font-size: 13px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.summary-item {
  min-height: 104px;
  padding: 16px;
  text-align: left;
  background: #fff;
  border: 1px solid var(--bs-border-light);
  border-radius: var(--bs-radius-md);
  box-shadow: var(--bs-card-shadow);
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.summary-item:hover,
.summary-item.active {
  border-color: rgba(27, 58, 47, 0.35);
  box-shadow: var(--bs-card-shadow-hover);
  transform: translateY(-1px);
}

.summary-item span,
.summary-item em {
  display: block;
  color: var(--bs-text-muted);
  font-size: 13px;
  font-style: normal;
}

.summary-item strong {
  display: block;
  margin: 6px 0 4px;
  color: var(--bs-text-title);
  font-size: 28px;
  line-height: 1.1;
}

.summary-item.urgent {
  border-left: 3px solid #b76e2a;
}

.summary-item.working,
.summary-item.delivery {
  border-left: 3px solid var(--bs-primary);
}

.summary-item.done {
  border-left: 3px solid #1b6b4a;
}

.card-panel {
  background: var(--bs-card-bg);
  border-radius: var(--bs-radius-md);
  box-shadow: var(--bs-card-shadow);
  padding: var(--bs-spacing-lg);
}

.order-panel {
  padding-top: 16px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.toolbar-count {
  color: var(--bs-text-muted);
  font-size: 13px;
  white-space: nowrap;
}

.order-table {
  width: 100%;
}

.order-no,
.receiver-name,
.amount {
  color: var(--bs-text-title);
  font-weight: 600;
}

.order-time,
.receiver-phone {
  margin-top: 4px;
  color: var(--bs-text-muted);
  font-size: 12px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.detail-section {
  margin-top: 18px;
}

.detail-section h4 {
  margin: 0 0 10px;
  color: var(--bs-text-title);
  font-size: 15px;
  font-weight: 600;
}

@media (max-width: 1100px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .page-head,
  .toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
<style scoped>
.party-strip{display:flex;gap:28px;margin-bottom:14px;padding:12px 14px;background:#f5f8f5;color:#87928a;font-size:12px}.party-strip strong{color:#1f2a24}.order-item-cell{display:flex;align-items:center;gap:10px}.order-item-cell img,.order-item-placeholder{width:42px;height:42px;flex:0 0 42px;object-fit:cover}.order-item-placeholder{display:grid;place-items:center;background:#eef4ef;color:#5e8069}.order-item-cell div{display:flex;flex-direction:column;gap:4px}.order-item-cell small{color:#87928a;font-size:11px}
</style>
