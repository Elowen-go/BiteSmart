import { getSafeArea } from '../../utils/safe-area'
import {
  getMerchantOrders,
  getMerchantDishes,
  getMerchantOrderDetail,
  acceptMerchantOrder,
  rejectMerchantOrder,
  finishMerchantOrder,
  type MerchantOrderItem
} from '../../api/merchant'
import { buildOrderVM, money, type DishPriceMap, type MOrderVM } from '../../utils/merchant-vm'
import { fmtDateTime } from '../../utils/json'

const TABS = [
  { k: 'all', n: '全部' },
  { k: '10', n: '待支付' },
  { k: '20', n: '待接单' },
  { k: '30', n: '备餐中' },
  { k: '40-pickup', n: '待取餐' },
  { k: '40-delivery', n: '配送中' },
  { k: '50', n: '已完成' },
  { k: '60', n: '已取消' },
  { k: '70', n: '退款中' },
  { k: '80', n: '已退款' }
]

/** 配送状态按 deliveryStatus 拆分，退款和终态订单也保留独立筛选项 */
const inTab = (o: MOrderVM, k: string): boolean => {
  if (k === 'all') return true
  if (k === '40-pickup') return o.status === 40 && o.deliveryStatus <= 10
  if (k === '40-delivery') return o.status === 40 && o.deliveryStatus > 10
  return o.status === Number(k)
}

interface DetailItem { name: string; qty: number; amount: string }

Page({
  data: {
    padTop: 44,
    menuTop: 26,
    menuH: 32,
    active: 'orders',
    tabs: TABS,
    tab: 'all',
    counts: {} as Record<string, number>,
    list: [] as MOrderVM[],
    // 订单详情弹层
    sheetShow: false,
    dNo: '',
    dTime: '',
    dAddr: '',
    dRemark: '',
    dPickupCode: '',
    dItems: [] as DetailItem[],
    dTotal: '',
    dStatus: 0,
    dId: '' as number | string
  },

  all: [] as MOrderVM[],

  pendingDetailId: '' as string,

  onLoad(options: Record<string, string | undefined>) {
    const sa = getSafeArea()
    this.setData({ padTop: sa.padTop, menuTop: sa.menuTop, menuH: sa.menuH })
    this.pendingDetailId = options && options.orderId ? decodeURIComponent(options.orderId) : ''
    this.loadOrders()
  },

  onShow() {
    this.loadOrders()
  },

  loadOrders() {
    Promise.all([
      getMerchantOrders(),
      getMerchantDishes().catch(() => [])
    ])
      .then(([orders, dishes]) => {
        const dishPrices: DishPriceMap = {}
        ;(dishes || []).forEach((dish) => {
          const price = Number(dish.price)
          if (dish.id != null && Number.isFinite(price) && price > 0) dishPrices[String(dish.id)] = price
        })
        this.all = (orders || []).map((order) => buildOrderVM(order, dishPrices))
        this.applyTab()
        if (this.pendingDetailId) {
          const id = this.pendingDetailId
          this.pendingDetailId = ''
          this.openDetailById(id)
        }
      })
      .catch((error: Error) => {
        console.warn('[m-orders] 订单加载失败：', error && error.message)
        wx.showToast({ title: '订单加载失败，请稍后重试', icon: 'none' })
      })
  },

  applyTab() {
    const counts: Record<string, number> = { all: this.all.length }
    this.data.tabs.forEach((t) => {
      if (t.k !== 'all') counts[t.k] = this.all.filter((o) => inTab(o, t.k)).length
    })
    this.setData({
      counts,
      list: this.all.filter((o) => inTab(o, this.data.tab))
    })
  },

  pickTab(e: WechatMiniprogram.CustomEvent) {
    this.setData({ tab: e.currentTarget.dataset.k as string })
    this.applyTab()
  },

  refresh() {
    this.loadOrders()
    wx.showToast({ title: '已刷新', icon: 'none' })
  },

  /* ---------- 订单详情弹层（复刻原型 mOrderSheet） ---------- */

  openDetail(e: WechatMiniprogram.CustomEvent) {
    const id = e.currentTarget.dataset.id as number | string
    this.openDetailById(id)
  },

  openDetailById(id: number | string) {
    getMerchantOrderDetail(id)
      .then((d) => {
        const order = d.order || {}
        const items: DetailItem[] = (d.items || order.items || []).map((it: MerchantOrderItem) => ({
          name: it.snapshotName || '商品',
          qty: it.quantity || 1,
          amount: money(Number(it.subTotal != null ? it.subTotal : (it.snapshotPrice || 0) * (it.quantity || 1)))
        }))
        this.setData({
          sheetShow: true,
          dId: id,
          dNo: order.orderNo || '—',
          dTime: fmtDateTime(order.createTime),
          dAddr: order.deliveryAddress || '到店自取',
          dRemark: order.remark || '无',
          dPickupCode: d.pickupCode || '',
          dItems: items,
          dTotal: money(Number(order.payAmount != null ? order.payAmount : order.totalAmount) || 0),
          dStatus: order.orderStatus || 0
        })
      })
      .catch((error: Error) => wx.showToast({ title: error.message || '订单详情加载失败', icon: 'none' }))
  },

  toggleItems(e: WechatMiniprogram.CustomEvent) {
    const id = String(e.currentTarget.dataset.id)
    this.all = this.all.map((order) => (
      String(order.id) === id ? { ...order, itemsExpanded: !order.itemsExpanded } : order
    ))
    this.applyTab()
  },

  closeSheet() {
    this.setData({ sheetShow: false })
  },

  /* ---------- 状态流转操作（20 接单/拒单 → 30 出餐） ---------- */

  accept(e: WechatMiniprogram.CustomEvent) {
    const id = e.currentTarget.dataset.id as number | string
    wx.showModal({
      title: '确认接单？',
      content: '确认后订单将进入备餐流程',
      confirmText: '确认接单',
      confirmColor: '#0F7A4A',
      success: (res) => {
        if (!res.confirm) return
        acceptMerchantOrder(id)
          .then(() => {
            this.setData({ sheetShow: false })
            wx.showToast({ title: '已接单，开始备餐', icon: 'none' })
            this.loadOrders()
          })
          .catch((error: Error) => wx.showToast({ title: error.message || '接单失败', icon: 'none' }))
      }
    })
  },

  reject(e: WechatMiniprogram.CustomEvent) {
    const id = e.currentTarget.dataset.id as number | string
    wx.showModal({
      title: '拒绝该订单？',
      content: '',
      confirmText: '确认拒单',
      confirmColor: '#C0563F',
      editable: true,
      placeholderText: '拒单原因（选填）',
      success: (res) => {
        if (!res.confirm) return
        rejectMerchantOrder(id, (res.content || '').trim() || '商家原因无法接单')
          .then(() => {
            this.setData({ sheetShow: false })
            wx.showToast({ title: '已拒单并退款', icon: 'none' })
            this.loadOrders()
          })
          .catch((error: Error) => wx.showToast({ title: error.message || '拒单失败', icon: 'none' }))
      }
    })
  },

  done(e: WechatMiniprogram.CustomEvent) {
    const id = e.currentTarget.dataset.id as number | string
    finishMerchantOrder(id)
      .then(() => {
        this.setData({ sheetShow: false })
        wx.showToast({ title: '已出餐，骑手大厅可见', icon: 'none' })
        this.loadOrders()
      })
      .catch((error: Error) => wx.showToast({ title: error.message || '操作失败', icon: 'none' }))
  },

  goTab(e: WechatMiniprogram.CustomEvent) {
    const url = e.currentTarget.dataset.url as string
    const key = e.currentTarget.dataset.key as string
    if (!url || key === this.data.active) return
    wx.redirectTo({ url })
  },

  noop() {}
})
