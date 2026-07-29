import { deleteCartItem, getCart, selectCartItem, updateCartQuantity, type CartItem } from '../../api/cart'
import { MOCK_COMBOS, MOCK_DISHES } from '../../mock/catalog'
import { API_ORIGIN } from '../../utils/request'
import { getSafeArea } from '../../utils/safe-area'
import { requireUser } from '../../utils/user-route'

interface CartViewItem extends CartItem {
  swipeX: number
}

const normalizeImage = (value?: string): string => {
  const image = String(value || '').trim()
  if (!image) return ''
  if (/^(https?:\/\/|data:image\/)/i.test(image)) return image
  if (image.startsWith('/uploads/')) return `${API_ORIGIN}/api/files/download/${image.slice('/uploads/'.length)}`
  if (image.startsWith('/')) return `${API_ORIGIN}${image}`
  return `${API_ORIGIN}/${image}`
}

/** 购物车条目无热量字段，用本地 mock 目录按 dishId/comboId 估算合计热量
 *  TODO(B 类)：后端 dish 已有 calories，但 CartItem 未携带；等后端把热量挂到购物车条目或提供批量菜品查询后改为真实值 */
const estimateKcal = (item: CartItem): number => {
  if (item.itemType === 10) {
    const dish = MOCK_DISHES.find((d) => String(d.id) === String(item.dishId))
    return dish ? dish.kcal * item.quantity : 0
  }
  const combo = MOCK_COMBOS.find((c) => String(c.id) === String(item.comboId))
  return combo ? combo.kcal * item.quantity : 0
}

Page({
  data: {
    items: [] as CartViewItem[],
    total: 0,
    totalKcal: 0,
    selectedCount: 0,
    allSelected: false,
    menuTop: 0,
    menuH: 32,
    swipeStartX: 0,
    swipeStartY: 0,
    swipeStartId: ''
  },
  onLoad() {
    if (!requireUser()) return
    const { menuTop, menuH } = getSafeArea()
    this.setData({ menuTop, menuH })
    this.load()
  },
  onShow() { this.load() },
  load() {
    getCart().then((items) => {
      const normalized = items.map((item) => ({
        ...item,
        dishImage: normalizeImage(item.dishImage),
        comboImage: normalizeImage(item.comboImage),
        swipeX: 0
      }))
      this.setData({ items: normalized })
      this.refreshTotal()
    }).catch(() => {})
  },
  back() { wx.navigateBack() },
  swipeStart(event: WechatMiniprogram.TouchEvent) {
    const touch = event.touches[0]
    if (!touch) return
    this.setData({
      swipeStartX: touch.clientX,
      swipeStartY: touch.clientY,
      swipeStartId: String(event.currentTarget.dataset.id)
    })
  },
  swipeEnd(event: WechatMiniprogram.TouchEvent) {
    const touch = event.changedTouches[0]
    const id = String(event.currentTarget.dataset.id)
    if (!touch || id !== this.data.swipeStartId) return
    const deltaX = touch.clientX - this.data.swipeStartX
    const deltaY = touch.clientY - this.data.swipeStartY
    if (Math.abs(deltaX) <= Math.abs(deltaY) || Math.abs(deltaX) < 36) return
    const target = deltaX < 0 ? -116 : 0
    this.setData({ items: this.data.items.map((item) => ({ ...item, swipeX: String(item.id) === id ? target : 0 })) })
  },
  refreshTotal() {
    const selected = this.data.items.filter((item) => item.selected === 1)
    const total = selected.reduce((sum, item) => sum + Number(item.price || 0) * item.quantity, 0)
    const totalKcal = selected.reduce((sum, item) => sum + estimateKcal(item), 0)
    this.setData({
      total,
      totalKcal,
      selectedCount: selected.length,
      allSelected: this.data.items.length > 0 && selected.length === this.data.items.length
    })
  },
  // 条目 id 为雪花字符串，全程禁止 Number() 强转；与本地数据比较时统一 String 口径
  changeQuantity(event: WechatMiniprogram.CustomEvent) {
    const id = String(event.currentTarget.dataset.id)
    const delta = Number(event.currentTarget.dataset.delta)
    const item = this.data.items.find((entry) => String(entry.id) === id)
    if (!item || item.quantity + delta < 1) return
    const quantity = item.quantity + delta
    updateCartQuantity(id, quantity).then(() => {
      this.setData({ items: this.data.items.map((entry) => String(entry.id) === id ? { ...entry, quantity, swipeX: 0 } : entry) })
      this.refreshTotal()
    }).catch((error: Error) => wx.showToast({ title: error.message || '修改数量失败', icon: 'none' }))
  },
  toggleSelected(event: WechatMiniprogram.CustomEvent) {
    const id = String(event.currentTarget.dataset.id)
    const item = this.data.items.find((entry) => String(entry.id) === id)
    if (!item) return
    const selected = item.selected === 1 ? 0 : 1
    selectCartItem(id, selected).then(() => {
      this.setData({ items: this.data.items.map((entry) => String(entry.id) === id ? { ...entry, selected, swipeX: 0 } : entry) })
      this.refreshTotal()
    }).catch((error: Error) => wx.showToast({ title: error.message || '修改选择失败', icon: 'none' }))
  },
  toggleAll() {
    const target = this.data.allSelected ? 0 : 1
    Promise.all(this.data.items.map((item) => selectCartItem(item.id, target)))
      .then(() => {
        this.setData({ items: this.data.items.map((entry) => ({ ...entry, selected: target, swipeX: 0 })) })
        this.refreshTotal()
      })
      .catch((error: Error) => wx.showToast({ title: error.message || '操作失败', icon: 'none' }))
  },
  removeItem(event: WechatMiniprogram.CustomEvent) {
    const id = String(event.currentTarget.dataset.id)
    deleteCartItem(id).then(() => {
      this.setData({ items: this.data.items.filter((item) => String(item.id) !== id) })
      this.refreshTotal()
    }).catch((error: Error) => wx.showToast({ title: error.message || '删除失败', icon: 'none' }))
  },
  goHealth() { wx.navigateTo({ url: '/pages/health/health' }) },
  goFood() { wx.switchTab({ url: '/pages/food/food' }) },
  checkout() {
    if (!this.data.items.some((item) => item.selected === 1)) {
      wx.showToast({ title: '请先选择要结算的商品', icon: 'none' })
      return
    }
    wx.navigateTo({ url: '/pages/checkout/checkout' })
  }
})
