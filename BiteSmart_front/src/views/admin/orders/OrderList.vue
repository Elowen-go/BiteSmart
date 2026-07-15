<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getOrderList, getOrderDetail, cancelOrder } from '../../../api/admin/orders'
import ListState from '../../../components/common/ListState.vue'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const orderStatus = ref<number | undefined>()
const userId = ref<number | undefined>()
const merchantId = ref<number | undefined>()
const paymentStatus = ref<number | undefined>()
const deliveryStatus = ref<number | undefined>()
const loadError = ref('')

const detailDialogVisible = ref(false)
const detailData = ref<any>(null)
const currentOrder = computed(() => detailData.value?.order || detailData.value)
const currentItems = computed(() => detailData.value?.items || [])
const cancelDialogVisible = ref(false)
const cancelReason = ref('')
const cancelTarget = ref<any>(null)

const loadData = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const res = await getOrderList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      orderStatus: orderStatus.value,
      userId: userId.value,
      merchantId: merchantId.value,
      paymentStatus: paymentStatus.value,
      deliveryStatus: deliveryStatus.value
    })
    if (res.code === 200) {
      tableData.value = res.data.list || []
      total.value = res.data.total || 0
    } else {
      loadError.value = res.message || '订单列表暂时无法获取'
    }
  } catch (err) {
    loadError.value = '请检查网络连接后重试'
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

const handleFilterChange = () => {
  pageNum.value = 1
  loadData()
}

const clearFilters = () => {
  userId.value = undefined
  merchantId.value = undefined
  orderStatus.value = undefined
  paymentStatus.value = undefined
  deliveryStatus.value = undefined
  handleFilterChange()
}

const openCancelDialog = (row: any) => {
  cancelTarget.value = row
  cancelReason.value = ''
  cancelDialogVisible.value = true
}

const submitCancel = async () => {
  if (!cancelTarget.value) return
  const res = await cancelOrder(cancelTarget.value.id, cancelReason.value || '管理员取消')
  if (res.code === 200) {
    ElMessage.success('订单已取消')
    cancelDialogVisible.value = false
    loadData()
  }
}

const formatAmount = (value: any) => Number(value || 0).toFixed(2)
const imageUrl = (value: string) => value ? (value.startsWith('http') ? value : `/api/files/download${value}`) : ''

onMounted(loadData)
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header order-toolbar">
        <h3>订单管理</h3>
        <div class="filter-group">
          <el-input v-model.number="userId" clearable placeholder="用户 ID" size="small" class="filter-id" @keyup.enter="handleFilterChange" />
          <el-input v-model.number="merchantId" clearable placeholder="商家 ID" size="small" class="filter-id" @keyup.enter="handleFilterChange" />
          <el-select v-model="paymentStatus" clearable placeholder="支付状态" size="small" class="filter-select" @change="handleFilterChange">
            <el-option label="未支付" :value="0" />
            <el-option label="已支付" :value="10" />
          </el-select>
          <el-select v-model="deliveryStatus" clearable placeholder="配送状态" size="small" class="filter-select" @change="handleFilterChange">
            <el-option label="未配送" :value="0" />
            <el-option label="待取餐" :value="10" />
            <el-option label="已取餐" :value="20" />
            <el-option label="配送中" :value="30" />
            <el-option label="已送达" :value="40" />
            <el-option label="配送异常" :value="60" />
          </el-select>
          <el-select v-model="orderStatus" clearable placeholder="订单状态" size="small" class="filter-select" @change="handleFilterChange">
            <el-option label="待支付" :value="10" />
            <el-option label="待接单" :value="20" />
            <el-option label="备餐中" :value="30" />
            <el-option label="配送中" :value="40" />
            <el-option label="已完成" :value="50" />
            <el-option label="已取消" :value="60" />
          </el-select>
          <el-button size="small" @click="clearFilters">重置</el-button>
        </div>
      </div>
      <div style="padding-top: 20px;">
        <ListState :loading="loading" :error="loadError" :empty="!tableData.length" empty-text="暂无订单记录" @retry="loadData">
        <el-table :data="tableData" border stripe style="width: 100%">
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
              <el-button v-if="row.orderStatus === 10 || row.orderStatus === 20" size="small" type="danger" link @click="openCancelDialog(row)">取消</el-button>
            </template>
          </el-table-column>
        </el-table>
        </ListState>
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

    <el-dialog v-model="detailDialogVisible" title="订单详情" width="760px">
      <div v-if="currentOrder" class="order-detail">
        <div class="party-grid">
          <div><span>买家</span><strong>{{ detailData.buyer?.nickname || detailData.buyer?.username || currentOrder.userId }}</strong><small>{{ detailData.buyer?.phone || '未填写手机号' }}</small></div>
          <div><span>商家</span><strong>{{ detailData.merchant?.shopName || currentOrder.merchantId }}</strong><small>{{ detailData.merchant?.contactPhone || '未填写联系电话' }}</small></div>
        </div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="订单状态"><el-tag :type="orderStatusTypeMap[currentOrder.orderStatus] || 'info'" size="small">{{ orderStatusMap[currentOrder.orderStatus] || '未知' }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="订单金额">¥{{ formatAmount(currentOrder.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="实付金额">¥{{ formatAmount(currentOrder.payAmount) }}</el-descriptions-item>
          <el-descriptions-item label="收货人">{{ currentOrder.receiverName || '未填写' }} {{ currentOrder.receiverPhone || '' }}</el-descriptions-item>
          <el-descriptions-item label="配送地址" :span="2">{{ currentOrder.deliveryAddress }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ currentOrder.remark || '无' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">{{ currentOrder.createTime }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="currentItems.length" class="detail-section"><h4>商品明细</h4><div v-for="item in currentItems" :key="item.id" class="item-row"><div class="item-image"><img v-if="item.snapshotImage" :src="imageUrl(item.snapshotImage)" alt="商品图片" /><span v-else>餐</span></div><div class="item-copy"><strong>{{ item.snapshotName || '订单商品' }}</strong><small>数量 × {{ item.quantity || 1 }} · 单价 ¥{{ formatAmount(item.snapshotPrice) }}</small></div><b>¥{{ formatAmount(item.subTotal) }}</b></div></div>
        <div class="detail-section"><h4>配送信息</h4><p>配送状态：{{ ({ 0: '未配送', 10: '待取餐', 20: '已取餐', 30: '配送中', 40: '已送达', 60: '异常' } as Record<number, string>)[currentOrder.deliveryTask?.taskStatus] || '未分配' }} · 配送员：{{ currentOrder.deliveryTask?.driverId || '未分配' }}</p><p v-if="currentOrder.deliveryTask?.exceptionReason" class="exception-text">异常原因：{{ currentOrder.deliveryTask.exceptionReason }}</p></div>
        <div v-if="detailData.statusTimeline?.length" class="detail-section"><h4>状态流转记录</h4><el-timeline><el-timeline-item v-for="(event, index) in detailData.statusTimeline" :key="index" :timestamp="event.time" :type="event.label.includes('异常') ? 'danger' : 'primary'">{{ event.label }}<span v-if="event.reason">：{{ event.reason }}</span></el-timeline-item></el-timeline></div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="cancelDialogVisible" title="取消订单" width="420px">
      <el-input v-model="cancelReason" type="textarea" :rows="3" placeholder="请输入取消原因" />
      <template #footer>
        <el-button @click="cancelDialogVisible = false">关闭</el-button>
        <el-button type="danger" @click="submitCancel">确认取消</el-button>
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

.order-toolbar {
  gap: 16px;
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
  flex: 1;
}

.filter-id {
  width: 130px;
}

.filter-select {
  width: 125px;
}
</style>
<style scoped>
.party-grid{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:16px}.party-grid>div{display:flex;flex-direction:column;gap:5px;padding:13px 15px;background:#f5f8f5;border:1px solid #e2e9e2}.party-grid span,.party-grid small{color:#849088;font-size:11px}.party-grid strong{color:#1f2a24;font-size:14px}.detail-section{margin-top:20px}.detail-section h4{margin:0 0 10px;color:#1f2a24;font-size:14px}.item-row{display:flex;align-items:center;gap:12px;padding:10px 0;border-bottom:1px solid #edf1ec}.item-image{display:grid;place-items:center;width:48px;height:48px;flex:0 0 48px;background:#eef4ef;color:#6d8874}.item-image img{width:100%;height:100%;object-fit:cover}.item-copy{display:flex;flex:1;flex-direction:column;gap:5px}.item-copy strong{font-size:13px}.item-copy small{color:#87928a;font-size:11px}.item-row b{color:#1f4d3a}@media(max-width:600px){.party-grid{grid-template-columns:1fr}}
</style>
