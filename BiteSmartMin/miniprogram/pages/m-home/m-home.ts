import { getSafeArea } from '../../utils/safe-area'
import {
  getMerchantShop,
  getMerchantTodayStats,
  getMerchantOrders,
  getMerchantDishes,
  acceptMerchantOrder,
  rejectMerchantOrder,
  finishMerchantOrder,
  updateMerchantShop,
  type MerchantShop
} from '../../api/merchant'
import { buildOrderVM, money, LOW_STOCK_THRESHOLD, type DishPriceMap, type MOrderVM } from '../../utils/merchant-vm'
import { uimg } from '../../mock/catalog'

const FALLBACK_LOGO = uimg('1543353071-873f17a7a088', 200)

Page({
  data: {
    padTop: 44,
    active: 'home',
    shopName: 'BiteSmart 门店',
    shopLogo: FALLBACK_LOGO,
    open: true,
    openText: '营业中',
    orderCount: 0,
    revenue: '0',
    pendingCount: 0,
    lowStockCount: 0,
    statusFilter: 'pending' as 'pending' | 'doing',
    queue: [] as MOrderVM[],
    doing: [] as MOrderVM[]
  },

  onLoad() {
    this.setData({ padTop: getSafeArea().padTop })
    this.loadAll()
  },

  onShow() {
    this.loadAll()
  },

  loadAll() {
    getMerchantShop()
      .then((shop: MerchantShop | null) => {
        if (!shop) return
        this.setData({
          shopName: shop.shopName || 'BiteSmart 门店',
          shopLogo: shop.shopLogo || FALLBACK_LOGO,
          // 营业状态：openStatus 10 营业中 / 20 打烊；后端并行开发中，缺省时按营业中兜底
          open: shop.openStatus !== 20,
          openText: shop.openStatus === 20 ? '已打烊' : '营业中'
        })
      })
      .catch((error: Error) => console.warn('[m-home] 店铺信息加载失败：', error && error.message))

    getMerchantTodayStats()
      .then((s) => {
        this.setData({
          orderCount: Number(s.orderCount || 0),
          revenue: money(Number(s.revenue || 0)),
          pendingCount: Number(s.pendingOrderCount || 0)
        })
      })
      .catch((error: Error) => console.warn('[m-home] 今日统计加载失败：', error && error.message))

    Promise.all([
      getMerchantOrders(),
      getMerchantDishes().catch((error: Error) => {
        console.warn('[m-home] 菜品目录加载失败：', error && error.message)
        return []
      })
    ])
      .then(([orders, dishes]) => {
        const dishPrices: DishPriceMap = {}
        ;(dishes || []).forEach((dish) => {
          const price = Number(dish.price)
          if (dish.id != null && Number.isFinite(price) && price > 0) dishPrices[String(dish.id)] = price
        })
        const list = (orders || []).map((order) => buildOrderVM(order, dishPrices))
        this.setData({
          lowStockCount: (dishes || []).filter((d) => Number(d.stock || 0) <= LOW_STOCK_THRESHOLD).length,
          queue: list.filter((o) => o.status === 20),
          doing: list.filter((o) => o.status === 30 || o.status === 40)
        })
      })
      .catch((error: Error) => {
        console.warn('[m-home] 订单加载失败：', error && error.message)
        wx.showToast({ title: '订单加载失败，请稍后重试', icon: 'none' })
      })
  },

  goDishes() {
    wx.redirectTo({ url: '/pages/m-dishes/m-dishes' })
  },

  /** 营业开关：PUT /merchant/shop { openStatus }（10 营业中 / 20 打烊），失败回滚 */
  toggleOpen() {
    const open = !this.data.open
    updateMerchantShop({ openStatus: open ? 10 : 20 })
      .then(() => {
        this.setData({ open, openText: open ? '营业中' : '已打烊' })
        wx.showToast({ title: open ? '已开店' : '已打烊', icon: 'none' })
      })
      .catch((error: Error) => wx.showToast({ title: error.message || '状态更新失败', icon: 'none' }))
  },

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
            wx.showToast({ title: '已接单，开始备餐', icon: 'none' })
            this.loadAll()
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
            wx.showToast({ title: '已拒单并退款', icon: 'none' })
            this.loadAll()
          })
          .catch((error: Error) => wx.showToast({ title: error.message || '拒单失败', icon: 'none' }))
      }
    })
  },

  done(e: WechatMiniprogram.CustomEvent) {
    const id = e.currentTarget.dataset.id as number | string
    finishMerchantOrder(id)
      .then(() => {
        wx.showToast({ title: '已出餐，骑手大厅可见', icon: 'none' })
        this.loadAll()
      })
      .catch((error: Error) => wx.showToast({ title: error.message || '操作失败', icon: 'none' }))
  },

  openOrderDetail(e: WechatMiniprogram.CustomEvent) {
    const id = e.currentTarget.dataset.id as number | string
    if (!id) return
    wx.navigateTo({ url: `/pages/m-orders/m-orders?orderId=${encodeURIComponent(String(id))}` })
  },

  toggleItems(e: WechatMiniprogram.CustomEvent) {
    const id = String(e.currentTarget.dataset.id)
    const update = (orders: MOrderVM[]): MOrderVM[] => orders.map((order) => (
      String(order.id) === id ? { ...order, itemsExpanded: !order.itemsExpanded } : order
    ))
    if (this.data.queue.some((order) => String(order.id) === id)) {
      this.setData({ queue: update(this.data.queue) })
      return
    }
    if (this.data.doing.some((order) => String(order.id) === id)) {
      this.setData({ doing: update(this.data.doing) })
    }
  },

  selectStatus(e: WechatMiniprogram.CustomEvent) {
    const status = e.currentTarget.dataset.status as 'pending' | 'doing'
    if (!status || status === this.data.statusFilter) return
    this.setData({ statusFilter: status })
  },

  goTab(e: WechatMiniprogram.CustomEvent) {
    const url = e.currentTarget.dataset.url as string
    const key = e.currentTarget.dataset.key as string
    if (!url || key === this.data.active) return
    wx.redirectTo({ url })
  },

  noop() {}
})
