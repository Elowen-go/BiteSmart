<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { createComplaint } from '../../api/user/complaints'
import { getOrderList } from '../../api/user/orders'
import { tail, merchantLabel } from '../../utils/display'

const loading = ref(false)
const submitLoading = ref(false)
const orders = ref<any[]>([])
// orderId / merchantId / driverId 都是雪花 ID，全程 string 透传，禁止 Number() 强转
const selectedOrderId = ref<string>()
const targetType = ref(10)
const complaintReason = ref('')
const complaintDesc = ref('')

const selectedOrder = computed(() => orders.value.find(o => String(o.id) === String(selectedOrderId.value)))

const itemSummary = (order: any) => {
  if (order.itemSummary || order.summary) return order.itemSummary || order.summary
  const items = order.items || order.orderItems || []
  const names = items.map((item: any) => item.snapshotName || item.name || item.dishName || item.comboName).filter(Boolean)
  if (names.length) {
    const visible = names.slice(0, 2).join('、')
    return names.length > 2 ? `${visible} 等${names.length}项` : visible
  }
  const count = Number(order.itemCount || order.totalQuantity || order.quantity || 0)
  return count ? `${count} 件商品` : '订单商品'
}

// 下拉选项：订单号后 6 位 + 菜品摘要 + 金额，绝不展示完整雪花 ID
const orderOptionLabel = (order: any) =>
  `···${tail(order.orderNo)} · ${itemSummary(order)} · ¥${order.payAmount ?? 0}`

// targetId 从订单自动带出：商家取 merchantId，配送员取 driverId（无则提示）
const targetHint = computed(() => {
  const order = selectedOrder.value
  if (!order) return ''
  if (targetType.value === 10) return merchantLabel(order.shopName, order.merchantId)
  return order.driverId ? `配送员 ···${tail(order.driverId)}` : '该订单暂无配送员信息'
})

const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await getOrderList({ page: 1, size: 100 })
    orders.value = res.data?.list || []
  } catch {
    ElMessage.error('加载订单失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const submit = async () => {
  const order = selectedOrder.value
  if (!order) return ElMessage.warning('请选择要投诉的订单')
  const targetId = targetType.value === 10 ? order.merchantId : order.driverId
  if (!targetId) return ElMessage.warning('该订单暂无配送员信息，请改选投诉商家')
  if (!complaintReason.value.trim()) return ElMessage.warning('请填写投诉原因')
  submitLoading.value = true
  try {
    const res: any = await createComplaint({
      orderId: String(order.id),
      targetType: targetType.value,
      targetId: String(targetId),
      complaintReason: complaintReason.value.trim(),
      complaintDesc: complaintDesc.value.trim()
    } as any)
    if (res.code === 200) {
      ElMessage.success('投诉已提交，我们会尽快处理')
      selectedOrderId.value = undefined
      targetType.value = 10
      complaintReason.value = ''
      complaintDesc.value = ''
    }
  } finally {
    submitLoading.value = false
  }
}

onMounted(fetchOrders)
</script>

<template>
  <div class="page-container">
    <div class="panel">
      <div class="panel-head">
        <h3>提交投诉</h3>
        <p>选择需要投诉的订单，投诉对象会随订单自动带出。</p>
      </div>
      <el-empty v-if="!loading && !orders.length" description="暂无可投诉的订单" />
      <el-form v-else v-loading="loading" label-width="100px">
        <el-form-item label="选择订单">
          <el-select v-model="selectedOrderId" placeholder="从我的订单中选择" filterable style="width: 100%">
            <el-option
              v-for="order in orders"
              :key="String(order.id)"
              :label="orderOptionLabel(order)"
              :value="String(order.id)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="投诉对象">
          <el-radio-group v-model="targetType">
            <el-radio :label="10">商家</el-radio>
            <el-radio :label="20">配送员</el-radio>
          </el-radio-group>
          <span v-if="targetHint" class="target-hint">{{ targetHint }}</span>
        </el-form-item>
        <el-form-item label="投诉原因">
          <el-input v-model="complaintReason" maxlength="100" placeholder="例如：餐品撒漏、配送超时、服务态度" />
        </el-form-item>
        <el-form-item label="详细说明">
          <el-input v-model="complaintDesc" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="补充事情经过，方便我们核实处理" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitLoading" @click="submit">提交投诉</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  max-width: 760px;
}

.panel {
  background: var(--bs-card-bg);
  border: 1px solid var(--bs-border-light);
  border-radius: 8px;
  padding: 24px;
}

.panel-head {
  margin-bottom: 24px;
}

.panel-head h3 {
  margin: 0;
  color: var(--bs-text-title);
}

.panel-head p {
  margin: 6px 0 0;
  color: var(--bs-text-muted);
  font-size: 13px;
}

.target-hint {
  margin-left: 12px;
  color: var(--bs-text-muted);
  font-size: 13px;
}
</style>
