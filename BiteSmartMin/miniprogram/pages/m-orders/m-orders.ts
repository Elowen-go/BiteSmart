import { getSafeArea } from '../../utils/safe-area'
import {
  getMerchantOrders,
  getMerchantOrderDetail,
  acceptMerchantOrder,
  rejectMerchantOrder,
  finishMerchantOrder,
  type MerchantOrderItem
} from '../../api/merchant'
import { buildOrderVM, money, type MOrderVM } from '../../utils/merchant-vm'
import { fmtDateTime } from '../../utils/json'

const TABS = [
  { k: '20', n: '待接单' },
  { k: '30', n: '备餐中' },
  { k: '40', n: '待取餐' },
  { k: 'all', n: '全部' }
]

/** 待取餐口径：orderStatus=40 且骑手未取餐（deliveryStatus<=10）；骑手取餐后只在「全部」可见 */
const inTab = (o: MOrderVM, k: string): boolean => {
  if (k === 'all') return true
  if (k === '40') return o.status === 40 && o.deliveryStatus <= 10
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
    tab: '20',
    counts: {} as Record<string, number>,
    list: [] as MOrderVM[],
    // 订单详情弹层
    sheetShow: false,
    dNo: '',
    dTime: '',
    dAddr: '',
    dRemark: '',
    dItems: [] as DetailItem[],
    dTotal: '',
    dStatus: 0,
    dId: '' as number | string
  },

  all: [] as MOrderVM[],

  onLoad() {
    const sa = getSafeArea()
    this.setData({ padTop: sa.padTop, menuTop: sa.menuTop, menuH: sa.menuH })
    this.loadOrders()
  },

  onShow() {
    this.loadOrders()
  },

  loadOrders() {
    getMerchantOrders()
      .then((orders) => {
        this.all = (orders || []).map(buildOrderVM)
        this.applyTab()
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
          dItems: items,
          dTotal: money(Number(order.payAmount != null ? order.payAmount : order.totalAmount) || 0),
          dStatus: order.orderStatus || 0
        })
      })
      .catch((error: Error) => wx.showToast({ title: error.message || '订单详情加载失败', icon: 'none' }))
  },

  closeSheet() {
    this.setData({ sheetShow: false })
  },

  /* ---------- 状态流转操作（20 接单/拒单 → 30 出餐） ---------- */

  accept(e: WechatMiniprogram.CustomEvent) {
    const id = e.currentTarget.dataset.id as number | string
    acceptMerchantOrder(id)
      .then(() => {
        this.setData({ sheetShow: false })
        wx.showToast({ title: '已接单，开始备餐', icon: 'none' })
        this.loadOrders()
      })
      .catch((error: Error) => wx.showToast({ title: error.message || '接单失败', icon: 'none' }))
  },

  reject(e: WechatMiniprogram.CustomEvent) {
    const id = e.currentTarget.dataset.id as number | string
    wx.showModal({
      title: '拒绝该订单？',
      content: '拒单后订单将取消并自动退款给用户',
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
