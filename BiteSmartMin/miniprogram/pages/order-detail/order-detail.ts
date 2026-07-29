import { applyRefund as applyRefundApi, cancelOrder, getOrderDetail, payOrder, type OrderDetail, type RefundApplication } from '../../api/order'
import { getSafeArea } from '../../utils/safe-area'
import { requireUser } from '../../utils/user-route'

interface ItemView { name: string; qty: number; price: string; image: string }
interface TlView { text: string; time: string; cls: string }

interface DetailView {
  group: string
  statusText: string
  statusClass: string
  hint: string
  orderNo: string
  amountText: string
  timeText: string
  receiver: string
  phone: string
  address: string
  items: ItemView[]
  timeline: TlView[]
  refundState: string
  refundActionText: string
  canRefund: boolean
}

const readStr = (obj: Record<string, unknown>, keys: string[]): string => {
  for (const key of keys) {
    const v = obj[key]
    if (v != null && v !== '') return String(v)
  }
  return ''
}

const groupOf = (status?: number): string => {
  if (status === 10) return 'unpaid'
  if (status === 50) return 'done'
  if (status != null && status >= 60) return 'cancelled'
  return 'doing'
}

/** 后端 orders.order_status 真实枚举 */
const STATUS_TEXT: Record<number, string> = { 10: '待支付', 20: '待接单', 30: '备餐中', 40: '配送中', 50: '已完成', 60: '已取消', 70: '退款中', 80: '已退款' }
const HINTS: Record<string, string> = {
  unpaid: '请尽快完成支付，美味不等人',
  doing: '你的健康餐正在路上，请稍作等待',
  done: '订单已完成，欢迎再次光临',
  cancelled: '订单已取消'
}

const refundViewOf = (orderStatus?: number, application?: RefundApplication | null) => {
  if (orderStatus === 80 || application?.auditStatus === 40) {
    return { refundState: 'refunded', refundActionText: '已退款', canRefund: false }
  }
  if (orderStatus === 70 || application?.auditStatus === 10 || application?.auditStatus === 20) {
    return { refundState: 'processing', refundActionText: '退款处理中', canRefund: false }
  }
  if (application?.auditStatus === 30) {
    return { refundState: 'rejected', refundActionText: '重新申请退款', canRefund: true }
  }
  const canRefund = orderStatus === 20 || orderStatus === 30 || orderStatus === 40
  return { refundState: canRefund ? 'available' : 'none', refundActionText: '申请退款', canRefund }
}

const buildView = (detail: OrderDetail): DetailView => {
  const order = detail.order || {}
  const group = groupOf(order.orderStatus)
  const refund = refundViewOf(order.orderStatus, detail.refundApplication)
  const rawItems = Array.isArray(detail.items) ? detail.items : []
  // 后端 OrderItem 快照字段为 snapshotName/snapshotPrice/snapshotImage（order_item 表），其余键为兼容兜底
  const items: ItemView[] = rawItems.map((entry) => ({
    name: readStr(entry, ['snapshotName', 'dishName', 'comboName', 'name', 'itemName']) || '健康餐',
    qty: Number(readStr(entry, ['quantity', 'qty', 'num']) || 1),
    price: readStr(entry, ['snapshotPrice', 'price', 'amount']) || '--',
    image: readStr(entry, ['snapshotImage', 'dishImage', 'comboImage', 'image'])
  }))
  const rawTl = Array.isArray(detail.statusTimeline) ? detail.statusTimeline : []
  // 后端 OrderStatusLog 字段为 fromStatus/toStatus/reason/createTime：按 toStatus 映射状态文案，reason 作备注
  const timeline: TlView[] = rawTl.map((entry, index) => {
    const toStatus = Number(readStr(entry, ['toStatus']) || 0)
    const fromStatus = Number(readStr(entry, ['fromStatus']) || 0)
    const reason = readStr(entry, ['reason'])
    const toText = (toStatus && STATUS_TEXT[toStatus]) || ''
    const fromText = (fromStatus && STATUS_TEXT[fromStatus]) || ''
    let text = toText || readStr(entry, ['text', 'title', 'status', 'description', 'name']) || '状态更新'
    if (toText && fromText && fromText !== toText) text = `${fromText} → ${toText}`
    if (reason) text += `（${reason}）`
    return {
      text,
      time: readStr(entry, ['createTime', 'time', 'timeText']),
      cls: group === 'done' || group === 'cancelled' || index < rawTl.length - 1 ? 'done' : 'now'
    }
  })
  return {
    group,
    statusText: refund.refundState === 'processing' ? '退款处理中' : refund.refundState === 'refunded' ? '已退款' : (order.orderStatus != null && STATUS_TEXT[order.orderStatus]) || '处理中',
    statusClass: refund.refundState === 'processing' || refund.refundState === 'refunded' ? 's3' : group === 'unpaid' ? 's1' : group === 'doing' ? 's2' : 's3',
    hint: refund.refundState === 'processing' ? '退款申请已提交，请等待平台审核' : refund.refundState === 'refunded' ? '款项将按原支付方式退回' : HINTS[group],
    orderNo: order.orderNo || `#${order.id || ''}`,
    amountText: order.totalAmount != null ? String(order.totalAmount) : '--',
    timeText: order.createTime ? String(order.createTime).slice(0, 16) : '',
    receiver: order.receiverName || '',
    phone: order.receiverPhone || '',
    address: order.deliveryAddress || '',
    items,
    timeline,
    ...refund
  }
}

Page({
  data: { detail: null as OrderDetail | null, view: null as DetailView | null, loading: false, menuTop: 0, menuH: 32 },
  onLoad(options: Record<string, string>) {
    if (!requireUser()) return
    const { menuTop, menuH } = getSafeArea()
    this.setData({ menuTop, menuH })
    // 订单 id 为雪花字符串，原样透传禁止 Number() 强转
    const id = (options && options.id) || ''
    if (!id) return
    this.load(id)
  },
  load(id: number | string) {
    this.setData({ loading: true })
    getOrderDetail(id)
      .then((detail) => this.setData({ detail, view: buildView(detail) }))
      .catch((error: Error) => wx.showToast({ title: error.message || '订单加载失败', icon: 'none' }))
      .finally(() => this.setData({ loading: false }))
  },
  back() { wx.navigateBack() },
  pay() {
    const id = this.data.detail && this.data.detail.order.id
    if (!id) return
    payOrder(id, 20)
      .then(() => { wx.showToast({ title: '支付成功', icon: 'none' }); return getOrderDetail(id) })
      .then((detail) => this.setData({ detail, view: buildView(detail) }))
      .catch((error: Error) => wx.showToast({ title: error.message || '支付失败', icon: 'none' }))
  },
  delivery() {
    const id = this.data.detail && this.data.detail.order.id
    wx.navigateTo({ url: `/pages/delivery/delivery?orderId=${id || ''}` })
  },
  goReview() {
    const id = this.data.detail && this.data.detail.order.id
    wx.navigateTo({ url: `/pages/review/review?orderId=${id || ''}` })
  },
  cancel() {
    const id = this.data.detail && this.data.detail.order.id
    if (!id) return
    cancelOrder(id, '用户主动取消')
      .then(() => wx.showToast({ title: '订单已取消', icon: 'none' }))
      .then(() => wx.navigateBack())
      .catch((error: Error) => wx.showToast({ title: error.message || '取消失败', icon: 'none' }))
  },
  applyRefund() {
    const id = this.data.detail && this.data.detail.order.id
    if (!id) return
    wx.showModal({
      title: '申请退款',
      content: '提交后将由平台审核，确认申请整单退款吗？',
      editable: true,
      placeholderText: '请输入退款原因（选填）',
      confirmText: '提交申请',
      success: (result) => {
        if (!result.confirm) return
        applyRefundApi(id, (result.content || '').trim() || '其他原因')
          .then(() => {
            wx.showToast({ title: '退款申请已提交', icon: 'none' })
            return getOrderDetail(id)
          })
          .then((detail) => this.setData({ detail, view: buildView(detail) }))
          .catch((error: Error) => wx.showToast({ title: error.message || '申请退款失败', icon: 'none' }))
      }
    })
  },
  rebuy() { wx.switchTab({ url: '/pages/food/food' }) }
})
