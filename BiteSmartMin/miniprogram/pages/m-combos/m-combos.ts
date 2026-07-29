import { getSafeArea } from '../../utils/safe-area'
import { getMerchantCombos, updateMerchantCombo } from '../../api/merchant'
import type { Combo } from '../../api/catalog'
import { resolveFileUrl } from '../../api/file'

interface ComboVM {
  id: number | string
  name: string
  image: string
  price: string
  kcal: number
  protein: string
  sold: number
  onSale: boolean
}

const buildVM = (c: Combo): ComboVM => ({
  id: c.id || 0,
  name: c.comboName || '套餐',
  image: resolveFileUrl(c.comboImage),
  price: String(c.price != null ? c.price : 0),
  kcal: Number(c.totalCalories || 0),
  protein: c.totalProtein != null ? String(c.totalProtein) : '—',
  sold: Number(c.salesCount || 0),
  // Combo.status：10 上架 / 20 下架（同菜品口径）
  onSale: c.status !== 20
})

Page({
  data: {
    menuTop: 26,
    menuH: 32,
    list: [] as ComboVM[]
  },

  onLoad() {
    const sa = getSafeArea()
    this.setData({ menuTop: sa.menuTop, menuH: sa.menuH })
    this.loadCombos()
  },

  onShow() {
    this.loadCombos()
  },

  loadCombos() {
    getMerchantCombos()
      .then((combos) => this.setData({ list: (combos || []).map(buildVM) }))
      .catch((error: Error) => {
        console.warn('[m-combos] 套餐加载失败：', error && error.message)
        wx.showToast({ title: '套餐加载失败，请稍后重试', icon: 'none' })
      })
  },

  addCombo() {
    wx.navigateTo({ url: '/pages/m-combo-edit/m-combo-edit' })
  },

  editCombo(e: WechatMiniprogram.CustomEvent) {
    wx.navigateTo({ url: `/pages/m-combo-edit/m-combo-edit?id=${e.currentTarget.dataset.id}` })
  },

  /** 上下架：PUT /merchant/combos/{id} 仅传 { combo: { status } }（dishItems 省略，后端保留原菜品关联） */
  toggleCombo(e: WechatMiniprogram.CustomEvent) {
    const id = e.currentTarget.dataset.id as number | string
    const item = this.data.list.find((x) => String(x.id) === String(id))
    if (!item) return
    const next = !item.onSale
    updateMerchantCombo(id, { combo: { status: next ? 10 : 20 } })
      .then(() => {
        wx.showToast({ title: `${next ? '已上架' : '已下架'} · ${item.name}`, icon: 'none' })
        this.loadCombos()
      })
      .catch((error: Error) => wx.showToast({ title: error.message || '操作失败', icon: 'none' }))
  },

  goBack() {
    wx.navigateBack({ fail: () => wx.redirectTo({ url: '/pages/m-me/m-me' }) })
  },

  noop() {}
})
