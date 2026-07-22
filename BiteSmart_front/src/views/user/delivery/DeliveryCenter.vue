<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOrderList } from '../../../api/user/orders'
import { ElMessage } from 'element-plus'
import { ArrowRight, Van } from '@element-plus/icons-vue'
import { orderLabel } from '../../../utils/display'

const router = useRouter()
const loading = ref(false)
const orders = ref<any[]>([])

const statusText: Record<number, string> = { 30: '备餐中', 40: '配送中', 50: '已送达' }
const statusTone: Record<number, string> = { 30: 'processing', 40: 'shipping', 50: 'completed' }
const formatDate = (value: any) => value ? String(value).replace('T', ' ').slice(0, 16) : '--'

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

const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await getOrderList({ page: 1, size: 50 })
    const list = Array.isArray(res.data) ? res.data : (res.data?.list || [])
    // 备餐中 / 配送中 / 已送达：覆盖"还要多久"的完整等待链路
    orders.value = list.filter((o: any) => [30, 40, 50].includes(Number(o.orderStatus)))
  } catch (error) {
    console.error('获取配送订单失败', error)
    ElMessage.error('获取配送订单失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const openTracking = (row: any) => router.push(`/user/delivery/${row.id}`)

onMounted(fetchOrders)
</script>

<template>
  <div class="delivery-page">
    <header class="delivery-intro">
      <div>
        <span class="eyebrow">DELIVERY</span>
        <h1>配送跟踪</h1>
        <p>从备餐到送达，每一份餐食的进度都在这里。</p>
      </div>
      <el-button @click="fetchOrders">刷新</el-button>
    </header>

    <div v-loading="loading" class="delivery-list">
      <article v-for="order in orders" :key="order.id" class="delivery-card">
        <div class="card-head">
          <div class="order-meta">
            <span class="status-dot" :class="statusTone[Number(order.orderStatus)]"></span>
            <strong>{{ statusText[Number(order.orderStatus)] || '进行中' }}</strong>
            <span>{{ formatDate(order.updateTime) }} 更新</span>
          </div>
          <span class="order-number">{{ orderLabel(order.orderNo) }}</span>
        </div>
        <div class="card-body">
          <div class="order-summary">
            <span class="summary-mark"><Van /></span>
            <div>
              <strong>{{ itemSummary(order) }}</strong>
              <p>{{ order.receiverName || '收货人' }} · {{ order.deliveryAddress || '地址待完善' }}</p>
            </div>
          </div>
          <el-button type="primary" :plain="Number(order.orderStatus) !== 40" @click="openTracking(order)">
            查看跟踪 <ArrowRight />
          </el-button>
        </div>
      </article>

      <div v-if="!loading && !orders.length" class="delivery-empty">
        <el-empty description="暂无备餐或配送中的订单">
          <el-button type="primary" @click="router.push('/user/dishes')">去逛逛菜品 <ArrowRight /></el-button>
        </el-empty>
      </div>
    </div>
  </div>
</template>

<style scoped>
.delivery-page {
  max-width: 1050px;
  margin: 0 auto;
  padding: 66px 24px 90px;
  color: #1f2a24;
}

.delivery-intro {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 38px;
}

.eyebrow {
  display: block;
  color: var(--orange);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .14em;
}

.delivery-intro h1 {
  margin: 12px 0 8px;
  font-family: "Source Han Serif SC", "Noto Serif CJK SC", "Songti SC", serif;
  font-size: 38px;
  font-weight: 600;
  line-height: 1.2;
}

.delivery-intro p {
  margin: 0;
  color: #718078;
  font-size: 13px;
}

.delivery-list {
  min-height: 260px;
}

.delivery-card {
  margin-bottom: 18px;
  border: 1px solid #e1e9e2;
  background: #fff;
  transition: border-color .2s, box-shadow .2s;
}

.delivery-card:hover {
  border-color: #bfd2c2;
  box-shadow: 0 8px 22px rgba(30, 158, 98, .06);
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 14px 18px;
  border-bottom: 1px solid #edf1ec;
}

.order-meta {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 12px;
}

.order-meta strong {
  font-size: 13px;
}

.order-meta>span:last-child {
  color: #8a968e;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #9da8a0;
}

.status-dot.processing {
  background: var(--green);
}

.status-dot.shipping {
  background: var(--green-deep);
}

.status-dot.completed {
  background: var(--green-ink);
}

.order-number {
  color: #9aa49d;
  font-size: 11px;
}

.card-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 30px;
  padding: 22px 18px;
}

.order-summary {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 13px;
}

.summary-mark {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  background: #f2f7f2;
  color: var(--green);
}

.summary-mark svg {
  width: 18px;
}

.order-summary strong {
  display: block;
  overflow: hidden;
  color: #1f2a24;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-summary p {
  margin: 5px 0 0;
  overflow: hidden;
  color: #8a968e;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 720px) {
  .card-body {
    align-items: flex-start;
    flex-direction: column;
    gap: 16px;
  }
}
</style>
