import { getCart, type CartItem } from '../../api/cart'
import { requireUser } from '../../utils/user-route'

Page({
  data: { items: [] as CartItem[], total: 54, topStyle: '' },
  onLoad() {
    if (!requireUser()) return
    const systemInfo = wx.getSystemInfoSync()
    const menuButton = wx.getMenuButtonBoundingClientRect()
    const topHeight = Math.max(menuButton.bottom + 12, systemInfo.statusBarHeight + 56)
    this.setData({ topStyle: `padding-top:${topHeight}px;` })
    getCart().then((items) => this.setData({ items, total: items.reduce((sum, item) => sum + Number(item.price || 0) * item.quantity, 0) })).catch(() => {})
  },
  back() { wx.navigateBack() },
  checkout() {
    wx.navigateTo({ url: '/pages/checkout/checkout' })
  }
})
