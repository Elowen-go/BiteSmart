import { getOrders, type Order } from '../../api/order'
import { requireUser } from '../../utils/user-route'

Page({
  data: { orders: [] as Order[], loading: false },
  onLoad() { if (!requireUser()) return; this.setData({ loading: true }); getOrders().then((result) => this.setData({ orders: Array.isArray(result) ? result : result.records })).catch(() => {}).finally(() => this.setData({ loading: false })) },
  back() { wx.navigateBack() },
  openDetail() { wx.navigateTo({ url: '/pages/order-detail/order-detail' }) }
})
