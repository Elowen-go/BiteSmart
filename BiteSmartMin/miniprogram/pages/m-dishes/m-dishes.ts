import { getSafeArea } from '../../utils/safe-area'
import { getMerchantDishes, updateMerchantDish } from '../../api/merchant'
import type { Dish } from '../../api/catalog'
import { uimg } from '../../mock/catalog'
import { LOW_STOCK_THRESHOLD } from '../../utils/merchant-vm'

const FALLBACK_IMG = uimg('1512621776951-a57141f2eefd', 200)

interface DishVM {
  id: number | string
  name: string
  image: string
  price: string
  kcal: number
  sold: number
  stock: number
  low: boolean
  onSale: boolean
}

const buildVM = (d: Dish): DishVM => {
  const stock = Number(d.stock || 0)
  return {
    id: d.id || 0,
    name: d.dishName || '菜品',
    image: d.dishImage || FALLBACK_IMG,
    price: String(d.price != null ? d.price : 0),
    kcal: Number(d.calories || 0),
    sold: Number(d.salesCount || 0),
    stock,
    low: stock <= LOW_STOCK_THRESHOLD,
    // Dish.status：10 上架 / 20 下架 / 30 售罄（售罄视为在售但库存为 0）
    onSale: d.status !== 20
  }
}

Page({
  data: {
    menuTop: 26,
    menuH: 32,
    active: 'dishes',
    list: [] as DishVM[]
  },

  onLoad() {
    const sa = getSafeArea()
    this.setData({ menuTop: sa.menuTop, menuH: sa.menuH })
    this.loadDishes()
  },

  onShow() {
    this.loadDishes()
  },

  loadDishes() {
    getMerchantDishes()
      .then((dishes) => this.setData({ list: (dishes || []).map(buildVM) }))
      .catch((error: Error) => {
        console.warn('[m-dishes] 菜品加载失败：', error && error.message)
        wx.showToast({ title: '菜品加载失败，请稍后重试', icon: 'none' })
      })
  },

  addDish() {
    wx.navigateTo({ url: '/pages/m-dish-edit/m-dish-edit' })
  },

  /** 上下架：PUT /merchant/dishes/{id} { status }（动态更新只改状态字段） */
  toggleDish(e: WechatMiniprogram.CustomEvent) {
    const id = e.currentTarget.dataset.id as number | string
    const item = this.data.list.find((x) => String(x.id) === String(id))
    if (!item) return
    const next = !item.onSale
    updateMerchantDish(id, { status: next ? 10 : 20 })
      .then(() => {
        wx.showToast({ title: `${next ? '已上架' : '已下架'} · ${item.name}`, icon: 'none' })
        this.loadDishes()
      })
      .catch((error: Error) => wx.showToast({ title: error.message || '操作失败', icon: 'none' }))
  },

  /** 库存编辑（失焦提交）：PUT { stock } */
  onStockBlur(e: WechatMiniprogram.CustomEvent) {
    const id = e.currentTarget.dataset.id as number | string
    const item = this.data.list.find((x) => String(x.id) === String(id))
    if (!item) return
    const v = parseInt(String(e.detail.value), 10)
    if (isNaN(v) || v < 0) {
      wx.showToast({ title: '库存需为不小于 0 的整数', icon: 'none' })
      this.loadDishes() // 回显旧值
      return
    }
    if (v === item.stock) return
    updateMerchantDish(id, { stock: v })
      .then(() => {
        wx.showToast({ title: '库存已更新', icon: 'none' })
        this.loadDishes()
      })
      .catch((error: Error) => {
        wx.showToast({ title: error.message || '库存更新失败', icon: 'none' })
        this.loadDishes()
      })
  },

  /** 编辑：进入 m-dish-edit 编辑模式（?id=x，提交走 PUT） */
  editDish(e: WechatMiniprogram.CustomEvent) {
    wx.navigateTo({ url: `/pages/m-dish-edit/m-dish-edit?id=${e.currentTarget.dataset.id}` })
  },

  goTab(e: WechatMiniprogram.CustomEvent) {
    const url = e.currentTarget.dataset.url as string
    const key = e.currentTarget.dataset.key as string
    if (!url || key === this.data.active) return
    wx.redirectTo({ url })
  },

  noop() {}
})
