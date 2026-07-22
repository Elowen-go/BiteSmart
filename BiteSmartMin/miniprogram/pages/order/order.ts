import { getOrders, type Order } from '../../api/order'
import { getSafeArea } from '../../utils/safe-area'
import { requireUser } from '../../utils/user-route'

interface OrderCard extends Order {
  group: string
  statusText: string
  statusClass: string
  amountText: string
  timeText: string
}

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

const toCard = (order: Order): OrderCard => {
  const group = groupOf(order.orderStatus)
  return {
    ...order,
    group,
    statusText: textOf(order.orderStatus),
    statusClass: classOf(group),
    amountText: order.totalAmount != null ? String(order.totalAmount) : '--',
    timeText: order.createTime ? String(order.createTime).slice(0, 16) : ''
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
  rebuy() { wx.switchTab({ url: '/pages/food/food' }) }
})
