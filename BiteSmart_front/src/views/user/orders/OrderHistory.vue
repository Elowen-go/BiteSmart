<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getOrderList, getOrderDetail, cancelOrder, payOrder } from '../../../api/user/orders'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight, Close, Location, Van, View } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { resolveFileUrl } from '../../../utils/fileUrl'

const router = useRouter()
const loading = ref(false)
const orders = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const detailVisible = ref(false)
const detail = ref<any>(null)
const statuses: Record<number, string> = { 10: '待支付', 20: '待接单', 30: '备餐中', 40: '配送中', 50: '已完成', 60: '已取消', 70: '退款中', 80: '已退款' }
const statusClass: Record<number, string> = { 10: 'pending', 20: 'processing', 30: 'processing', 40: 'shipping', 50: 'completed', 60: 'muted', 70: 'pending', 80: 'muted' }
const formatDate = (value: any) => value ? String(value).replace('T', ' ').slice(0, 16) : '时间未知'
const orderStatus = (order: any) => statuses[Number(order.orderStatus)] || '状态未知'
const statusTone = (order: any) => statusClass[Number(order.orderStatus)] || 'muted'
const itemCount = (order: any) => Number(order.itemCount || order.totalQuantity || order.quantity || 0)
const itemSummary = (order: any) => {
  if (order.itemSummary || order.summary) return order.itemSummary || order.summary
  const items = order.items || order.orderItems || []
  const names = items
    .map((item: any) => item.snapshotName || item.name || item.dishName || item.comboName)
    .filter(Boolean)
  if (names.length) {
    const visibleNames = names.slice(0, 2).join('、')
    return names.length > 2 ? `${visibleNames} 等${names.length}项` : visibleNames
  }
  return itemCount(order) ? `${itemCount(order)} 件商品` : '订单商品'
}
const detailItems = computed(() => detail.value?.items || detail.value?.orderItems || [])

const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await getOrderList({ page: currentPage.value, size: pageSize.value })
    orders.value = res.data?.list || []
    total.value = Number(res.data?.total || 0)
  } catch { ElMessage.error('获取订单失败') } finally { loading.value = false }
}
const showDetail = async (row: any) => {
  try {
    const res = await getOrderDetail(row.id)
    const payload = res.data || row
    detail.value = payload.order ? { ...payload.order, items: payload.items || [] } : payload
    detailVisible.value = true
  } catch { ElMessage.error('获取订单详情失败') }
}
const cancel = async (row: any) => {
  try {
    await ElMessageBox.confirm('未接单订单可以取消，确定继续吗？', '取消订单', { type: 'warning', confirmButtonText: '确认取消', cancelButtonText: '再想想' })
    await cancelOrder(row.id)
    ElMessage.success('订单已取消')
    fetchOrders()
  } catch {}
}
const openDelivery = (row: any) => router.push(`/user/delivery/${row.id}`)
const pay = async (row: any) => {
  const paymentWindow = window.open('', '_blank')
  try {
    const res = await payOrder(row.id, 10)
    const payload = res.data || {}
    if (payload.paymentMode === 'alipay-sandbox' && payload.form) {
      if (!paymentWindow) {
        ElMessage.warning('请允许浏览器打开支付页面')
        return
      }
      paymentWindow.document.write(payload.form)
      paymentWindow.document.close()
    } else {
      paymentWindow?.close()
      ElMessage.success(payload.message || '支付成功')
      await fetchOrders()
    }
  } catch {
    paymentWindow?.close()
    ElMessage.error('发起支付失败，请稍后重试')
  }
}
onMounted(fetchOrders)
</script>

<template>
  <div class="orders-page">
    <header class="orders-intro"><div><span class="eyebrow">订单记录</span><h1>我的订单</h1><p>查看每一份餐食的状态，安心等待美味送达。</p></div><div class="order-count"><strong>{{ total }}</strong><span>笔订单</span></div></header>
    <div v-loading="loading" class="order-list">
        <article v-for="order in orders" :key="order.id" class="order-card">
        <div class="order-card-head"><div class="order-meta"><span class="status-dot" :class="statusTone(order)"></span><strong>{{ orderStatus(order) }}</strong><span>{{ formatDate(order.createTime) }}</span></div><span class="order-number">订单号 {{ order.orderNo }}</span></div>
        <div class="order-card-body"><div class="order-summary"><div class="summary-mark"><Location /></div><div><strong>{{ itemSummary(order) }}</strong><p>{{ order.receiverName || '收货人未设置' }} · {{ order.deliveryAddress || '收货地址未设置' }}</p></div></div><div class="order-amount"><span>实付金额</span><strong>¥{{ order.payAmount || order.totalAmount || 0 }}</strong></div></div>
        <div class="order-card-foot"><span class="order-hint">{{ Number(order.orderStatus) === 50 ? '感谢你的每一次选择' : '订单状态会随配送进度更新' }}</span><div class="order-actions"><el-button v-if="Number(order.orderStatus) === 10" text type="primary" @click="pay(order)">去支付</el-button><el-button text @click="showDetail(order)"><View />查看详情</el-button><el-button v-if="Number(order.orderStatus) === 40" text type="primary" @click="openDelivery(order)"><Van />配送跟踪</el-button><el-button v-if="Number(order.orderStatus) === 10 || Number(order.orderStatus) === 20" text type="danger" @click="cancel(order)"><Close />取消订单</el-button></div></div>
      </article>
      <div v-if="!loading && !orders.length" class="order-empty"><div class="empty-mark"><Van /></div><h2>还没有订单</h2><p>去挑选一份喜欢的餐食，开启第一笔订单。</p><el-button type="primary" @click="router.push('/user/dishes')">去逛逛菜品 <ArrowRight /></el-button></div>
    </div>
    <div v-if="total" class="pager"><el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @current-change="fetchOrders" @size-change="fetchOrders" /></div>
    <el-dialog v-model="detailVisible" class="order-detail-dialog" width="700px" :show-close="true">
      <template #header><div class="detail-dialog-head"><div><span class="eyebrow">订单详情</span><h2>{{ detail?.orderNo }}</h2></div><span class="detail-status" :class="statusTone(detail || {})">{{ detail ? orderStatus(detail) : '' }}</span></div></template>
      <template v-if="detail"><div class="detail-section"><div class="detail-section-title"><span>订单商品</span><small>{{ detailItems.length }} 项</small></div><div v-if="detailItems.length" class="detail-items"><div v-for="item in detailItems" :key="item.id" class="detail-item"><div class="item-image"><img v-if="item.snapshotImage || item.dishImage || item.comboImage" :src="resolveFileUrl(item.snapshotImage || item.dishImage || item.comboImage)" alt="" /><span v-else>餐</span></div><div class="item-copy"><strong>{{ item.snapshotName || item.name || item.dishName || item.comboName || '订单商品' }}</strong><span>数量 × {{ item.quantity || 1 }}</span></div><strong class="item-price">¥{{ item.snapshotPrice || item.price || 0 }}</strong></div></div><p v-else class="detail-empty">商品明细暂未返回</p></div><div class="detail-section"><div class="detail-section-title"><span>配送信息</span></div><div class="delivery-info"><div><Location /><span>{{ detail.receiverName || '收货人未设置' }} {{ detail.receiverPhone || '' }}</span></div><p>{{ detail.deliveryAddress || '收货地址未设置' }}</p></div></div><div class="detail-total"><span>实付金额</span><strong>¥{{ detail.payAmount || detail.totalAmount || 0 }}</strong></div><div class="detail-note"><span>下单时间</span><strong>{{ formatDate(detail.createTime) }}</strong><span>备注</span><strong>{{ detail.remark || '无备注' }}</strong></div><div v-if="detail.statusTimeline?.length" class="detail-section"><div class="detail-section-title"><span>状态流转记录</span></div><el-timeline><el-timeline-item v-for="(event, index) in detail.statusTimeline" :key="index" :timestamp="formatDate(event.createTime || event.time)">{{ statuses[event.toStatus] || "状态变更" }}<span v-if="event.reason">：{{ event.reason }}</span></el-timeline-item></el-timeline></div><div class="detail-note"><span>订单金额</span><strong>¥{{ detail.totalAmount || 0 }}</strong><span>优惠金额</span><strong>¥{{ detail.discountAmount || 0 }}</strong><span>支付时间</span><strong>{{ formatDate(detail.payTime) }}</strong><span>支付方式</span><strong>{{ detail.payMethod === 10 ? "支付宝" : detail.payMethod === 20 ? "微信" : "未支付" }}</strong><span>下单渠道</span><strong>{{ detail.channel || "PC" }}</strong><span>完成时间</span><strong>{{ formatDate(detail.finishTime) }}</strong></div></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.orders-page{max-width:1050px;margin:0 auto;padding:66px 24px 90px;color:#1f2a24}.orders-intro{display:flex;align-items:flex-end;justify-content:space-between;margin-bottom:38px}.eyebrow{display:block;color:#cf704f;font-size:11px;font-weight:700;letter-spacing:.14em}.orders-intro h1{margin:12px 0 8px;font-family:"Source Han Serif SC","Noto Serif CJK SC","Songti SC",serif;font-size:38px;font-weight:600;line-height:1.2}.orders-intro p{margin:0;color:#718078;font-size:13px}.order-count{display:flex;align-items:baseline;gap:8px;padding-bottom:4px;color:#8a968e}.order-count strong{color:#1f4d3a;font-family:Georgia,serif;font-size:32px;font-weight:500}.order-count span{font-size:12px}.order-list{min-height:260px}.order-card{margin-bottom:18px;border:1px solid #e1e9e2;background:#fff;transition:border-color .2s,box-shadow .2s}.order-card:hover{border-color:#bfd2c2;box-shadow:0 8px 22px rgba(31,77,58,.06)}.order-card-head,.order-card-foot{display:flex;align-items:center;justify-content:space-between;gap:18px;padding:14px 18px}.order-card-head{border-bottom:1px solid #edf1ec}.order-meta{display:flex;align-items:center;gap:9px;font-size:12px}.order-meta strong{font-size:13px}.order-meta>span:last-child{color:#8a968e}.status-dot{width:7px;height:7px;border-radius:50%;background:#9da8a0}.status-dot.pending{background:#cf704f}.status-dot.processing{background:#568b70}.status-dot.shipping{background:#d49b4e}.status-dot.completed{background:#1f4d3a}.order-number{color:#9aa49d;font-size:11px}.order-card-body{display:flex;align-items:center;justify-content:space-between;gap:30px;padding:22px 18px}.order-summary{display:flex;align-items:center;min-width:0;gap:13px}.summary-mark{display:grid;place-items:center;width:42px;height:42px;flex:0 0 42px;background:#f2f7f2;color:#1f4d3a}.summary-mark svg{width:18px}.order-summary strong{display:block;overflow:hidden;color:#1f2a24;font-size:14px;text-overflow:ellipsis;white-space:nowrap}.order-summary p{max-width:600px;margin:7px 0 0;overflow:hidden;color:#89958d;font-size:11px;text-overflow:ellipsis;white-space:nowrap}.order-amount{display:flex;flex-direction:column;align-items:flex-end;flex:0 0 auto;gap:5px}.order-amount span{color:#89958d;font-size:11px}.order-amount strong{color:#1f4d3a;font-size:21px;font-weight:650}.order-card-foot{border-top:1px solid #edf1ec}.order-hint{color:#a0aaa3;font-size:11px}.order-actions{display:flex;align-items:center;gap:4px}.order-actions :deep(.el-button){margin:0;font-size:12px}.order-actions :deep(svg){width:14px;margin-right:4px;vertical-align:-2px}.order-empty{display:flex;flex-direction:column;align-items:center;justify-content:center;min-height:340px;background:#f1f5f1;text-align:center}.empty-mark{display:grid;place-items:center;width:54px;height:54px;margin-bottom:17px;background:#dfece1;color:#1f4d3a}.empty-mark svg{width:24px}.order-empty h2{margin:0 0 8px;font-family:"Source Han Serif SC","Noto Serif CJK SC","Songti SC",serif;font-size:20px}.order-empty p{margin:0 0 18px;color:#78857d;font-size:12px}.order-empty :deep(.el-button){background:#1f4d3a;border-color:#1f4d3a}.order-empty :deep(.el-button svg){width:14px;margin-left:5px;vertical-align:-2px}.pager{display:flex;justify-content:center;margin-top:32px}.order-detail-dialog :deep(.el-dialog__header){margin:0;padding:24px 28px 18px;border-bottom:1px solid #edf1ec}.order-detail-dialog :deep(.el-dialog__body){padding:20px 28px 24px}.detail-dialog-head{display:flex;align-items:flex-end;justify-content:space-between;gap:15px}.detail-dialog-head h2{margin:9px 0 0;color:#1f2a24;font-size:20px}.detail-status{padding:5px 9px;background:#f2f7f2;color:#1f4d3a;font-size:12px}.detail-status.pending{background:#fff4ed;color:#b55e43}.detail-status.shipping{background:#fff8ea;color:#aa762e}.detail-status.muted{background:#f0f3f0;color:#77847b}.detail-section{margin-bottom:22px}.detail-section-title{display:flex;align-items:baseline;justify-content:space-between;margin-bottom:10px;color:#1f2a24;font-size:14px;font-weight:700}.detail-section-title small{color:#8a968e;font-size:11px;font-weight:400}.detail-items{border-top:1px solid #e1e9e2}.detail-item{display:flex;align-items:center;gap:11px;padding:11px 0;border-bottom:1px solid #edf1ec}.item-image{display:grid;place-items:center;width:40px;height:40px;flex:0 0 40px;background:#eff4ef;color:#74907a;font-size:12px}.item-image img{width:100%;height:100%;object-fit:cover}.item-copy{display:flex;flex:1;flex-direction:column;gap:5px}.item-copy strong{color:#1f2a24;font-size:13px}.item-copy span{color:#8a968e;font-size:11px}.item-price{color:#1f4d3a;font-size:13px}.detail-empty{margin:0;padding:14px 0;color:#8a968e;font-size:12px}.delivery-info{padding:14px 15px;background:#f7faf7}.delivery-info div{display:flex;align-items:center;gap:8px;color:#1f2a24;font-size:13px}.delivery-info svg{width:16px;color:#1f4d3a}.delivery-info p{margin:9px 0 0 24px;color:#78857d;font-size:12px;line-height:1.6}.detail-total{display:flex;align-items:center;justify-content:space-between;padding:16px 0;border-top:1px solid #e1e9e2;border-bottom:1px solid #e1e9e2}.detail-total span{color:#78857d;font-size:12px}.detail-total strong{color:#1f4d3a;font-size:22px}.detail-note{display:grid;grid-template-columns:auto 1fr auto 1fr;gap:8px 13px;margin-top:15px;color:#8a968e;font-size:11px}.detail-note strong{color:#4e5e54;font-weight:500;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}@media(max-width:650px){.orders-page{padding:44px 18px 68px}.orders-intro{align-items:flex-start;flex-direction:column;gap:20px}.orders-intro h1{font-size:31px}.order-card-head,.order-card-foot{align-items:flex-start;flex-direction:column;gap:10px}.order-card-body{align-items:flex-start;flex-direction:column;gap:18px}.order-summary{width:100%}.order-summary p{max-width:calc(100vw - 110px)}.order-amount{align-items:flex-start}.order-actions{flex-wrap:wrap}.order-detail-dialog{width:calc(100% - 28px)!important;margin-top:8vh!important}.order-detail-dialog :deep(.el-dialog__header),.order-detail-dialog :deep(.el-dialog__body){padding-left:20px;padding-right:20px}.detail-note{grid-template-columns:auto 1fr}.detail-note span:nth-of-type(2){grid-column:1}.detail-note strong:nth-of-type(2){grid-column:2}}
</style>
<style scoped>
.summary-mark :deep(svg){width:18px;height:18px}
.empty-mark :deep(svg){width:24px;height:24px}
</style>
