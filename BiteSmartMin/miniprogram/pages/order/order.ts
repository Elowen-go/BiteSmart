import { applyRefund as applyRefundApi, getOrderDetail, getOrders, type Order, type OrderDetail, type RefundApplication } from '../../api/order'
import { getSafeArea } from '../../utils/safe-area'
import { requireUser } from '../../utils/user-route'

interface OrderCard extends Order {
  group: string
  statusText: string
  statusClass: string
  amountText: string
  timeText: string
  firstItem: OrderPreview | null
  refundState: string
  refundActionText: string
  canRefund: boolean
}

interface OrderPreview { name: string; qty: number; image: string }

interface TabItem { key: string; label: string; count: number }

const TABS: TabItem[] = [
  { key: 'all', label: '全部', count: 0 },
  { key: 'unpaid', label: '待支付', count: 0 },
  { key: 'doing', label: '进行中', count: 0 },
  { key: 'done', label: '已完成', count: 0 },
  { key: 'cancelled', label: '已取消', count: 0 }
]

/** 后端 orders.order_status 真实枚举：10 待支付 / 20 待接单 / 30 备餐中 / 40 配送中 / 50 已完成 / 60 已取消 / 70 退款中 / 80 已退款 */
const groupOf = (status?: number): string => {
  if (status === 10) return 'unpaid'
  if (status === 50) return 'done'
  if (status != null && status >= 60) return 'cancelled'
  return 'doing'
}

const STATUS_TEXT: Record<number, string> = { 10: '待支付', 20: '待接单', 30: '备餐中', 40: '配送中', 50: '已完成', 60: '已取消', 70: '退款中', 80: '已退款' }
const textOf = (status?: number): string => (status != null && STATUS_TEXT[status]) || '处理中'
const classOf = (group: string): string => (group === 'unpaid' ? 's1' : group === 'doing' ? 's2' : 's3')

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

const readStr = (obj: Record<string, unknown>, keys: string[]): string => {
  for (const key of keys) {
    const value = obj[key]
    if (value != null && value !== '') return String(value)
  }
  return ''
}

const firstItemOf = (detail: OrderDetail): OrderPreview | null => {
  const raw = Array.isArray(detail.items) ? detail.items[0] : null
  if (!raw) return null
  return {
    name: readStr(raw, ['snapshotName', 'dishName', 'comboName', 'name', 'itemName']) || '健康餐品',
    qty: Number(readStr(raw, ['quantity', 'qty', 'num']) || 1),
    image: readStr(raw, ['snapshotImage', 'dishImage', 'comboImage', 'image'])
  }
}

const toCard = (order: Order): OrderCard => {
  const group = groupOf(order.orderStatus)
  return {
    ...order,
    group,
    statusText: textOf(order.orderStatus),
    statusClass: classOf(group),
    amountText: order.totalAmount != null ? String(order.totalAmount) : '--',
    timeText: order.createTime ? String(order.createTime).slice(0, 16) : '',
    firstItem: null,
    ...refundViewOf(order.orderStatus)
  }
}

const decorateCard = (card: OrderCard, application?: RefundApplication | null): OrderCard => {
  const refund = refundViewOf(card.orderStatus, application)
  return {
    ...card,
    statusText: refund.refundState === 'processing' ? '退款处理中' : refund.refundState === 'refunded' ? '已退款' : card.statusText,
    statusClass: refund.refundState === 'processing' || refund.refundState === 'refunded' ? 's3' : card.statusClass,
    ...refund
  }
}

Page({
  data: {
    orders: [] as OrderCard[],
    filtered: [] as OrderCard[],
    tabs: TABS,
    tab: 'all',
    loading: false,
    menuTop: 0,
    menuH: 32
  },
  onLoad() {
    if (!requireUser()) return
    const { menuTop, menuH } = getSafeArea()
    this.setData({ menuTop, menuH })
  },
  onShow() { this.load() },
  load() {
    this.setData({ loading: true })
    getOrders()
      .then((result) => {
        const orders = (Array.isArray(result) ? result : result.records).map(toCard)
        this.setData({ orders })
        this.applyFilter()
        return Promise.all(orders.map((order) => getOrderDetail(order.id)
          .then((detail) => ({ id: order.id, firstItem: firstItemOf(detail), refundApplication: detail.refundApplication || null }))
          .catch(() => ({ id: order.id, firstItem: null, refundApplication: null }))))
      })
      .then((previews) => {
        const previewMap = new Map(previews.map((item) => [String(item.id), item]))
        const orders = this.data.orders.map((order) => {
          const extra = previewMap.get(String(order.id))
          return decorateCard({ ...order, firstItem: extra ? extra.firstItem : null }, extra && extra.refundApplication)
        })
        this.setData({ orders })
        this.applyFilter()
      })
      .catch(() => {})
      .finally(() => this.setData({ loading: false }))
  },
  applyFilter() {
    const { orders, tab } = this.data
    const tabs = TABS.map((t) => ({
      ...t,
      count: t.key === 'all' ? orders.length : orders.filter((o) => o.group === t.key).length
    }))
    const filtered = tab === 'all' ? orders : orders.filter((o) => o.group === tab)
    this.setData({ tabs, filtered })
  },
  setTab(event: WechatMiniprogram.CustomEvent) {
    this.setData({ tab: String(event.currentTarget.dataset.key) })
    this.applyFilter()
  },
  back() { wx.navigateBack() },
  openDetail(event: WechatMiniprogram.CustomEvent) {
    wx.navigateTo({ url: `/pages/order-detail/order-detail?id=${event.currentTarget.dataset.id}` })
  },
  goDelivery(event: WechatMiniprogram.CustomEvent) {
    wx.navigateTo({ url: `/pages/delivery/delivery?orderId=${event.currentTarget.dataset.id}` })
  },
  goReview(event: WechatMiniprogram.CustomEvent) {
    wx.navigateTo({ url: `/pages/review/review?orderId=${event.currentTarget.dataset.id}` })
  },
  applyRefund(event: WechatMiniprogram.CustomEvent) {
    const id = event.currentTarget.dataset.id as number | string
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
            this.load()
          })
          .catch((error: Error) => wx.showToast({ title: error.message || '申请退款失败', icon: 'none' }))
      }
    })
  },
  rebuy() { wx.switchTab({ url: '/pages/food/food' }) }
})
