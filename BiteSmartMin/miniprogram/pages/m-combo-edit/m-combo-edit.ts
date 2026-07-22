import { getSafeArea } from '../../utils/safe-area'
import {
  createMerchantCombo,
  updateMerchantCombo,
  getMerchantComboDetail,
  getMerchantDishes
} from '../../api/merchant'
import type { Dish } from '../../api/catalog'

interface DishPick {
  id: number | string
  name: string
  kcal: number
  price: string
  selected: boolean
}

Page({
  data: {
    menuTop: 26,
    menuH: 32,
    editId: '' as number | string,
    // 表单
    comboName: '',
    price: '',
    originalPrice: '',
    description: '',
    image: '',
    imgError: false,
    suitableFor: '',
    onSale: true,
    dishes: [] as DishPick[],
    selectedCount: 0,
    err: '',
    submitting: false
  },

  onLoad(options: Record<string, string | undefined>) {
    const sa = getSafeArea()
    this.setData({ menuTop: sa.menuTop, menuH: sa.menuH })
    const id = (options && options.id) || ''
    this.setData({ editId: id })

    // 菜品池
    const dishesP = getMerchantDishes()
      .then((rows) => (rows || []).map((d: Dish) => ({
        id: d.id || 0,
        name: d.dishName || '菜品',
        kcal: Number(d.calories || 0),
        price: String(d.price != null ? d.price : 0),
        selected: false
      })))
      .catch((error: Error) => {
        console.warn('[m-combo-edit] 菜品列表加载失败：', error && error.message)
        return [] as DishPick[]
      })

    if (id) {
      // 编辑模式：预填套餐信息 + 已选菜品
      Promise.all([dishesP, getMerchantComboDetail(id)])
        .then(([dishesRaw, detail]) => {
          const combo = (detail && detail.combo) || {}
          const chosen = new Set(((detail && detail.dishItems) || []).map((it) => String(it.dishId)))
          const dishes = dishesRaw.map((d) => ({ ...d, selected: chosen.has(String(d.id)) }))
          this.setData({
            dishes,
            selectedCount: dishes.filter((d) => d.selected).length,
            comboName: combo.comboName || '',
            price: combo.price != null ? String(combo.price) : '',
            originalPrice: combo.originalPrice != null ? String(combo.originalPrice) : '',
            description: combo.description || '',
            image: combo.comboImage || '',
            suitableFor: combo.suitableFor || '',
            onSale: combo.status !== 20
          })
        })
        .catch((error: Error) => {
          console.warn('[m-combo-edit] 套餐详情加载失败：', error && error.message)
          wx.showToast({ title: '套餐详情加载失败', icon: 'none' })
        })
    } else {
      dishesP.then((dishes) => this.setData({ dishes }))
    }
  },

  onInput(e: WechatMiniprogram.CustomEvent) {
    const field = e.currentTarget.dataset.field as string
    const patch: Record<string, unknown> = { [field]: e.detail.value, err: '' }
    if (field === 'image') patch.imgError = false
    this.setData(patch)
  },

  onImgError() {
    this.setData({ imgError: true })
  },

  toggleSale() {
    this.setData({ onSale: !this.data.onSale })
  },

  toggleDish(e: WechatMiniprogram.CustomEvent) {
    const id = e.currentTarget.dataset.id as number | string
    const dishes = this.data.dishes.map((d) =>
      String(d.id) === String(id) ? { ...d, selected: !d.selected } : d)
    this.setData({ dishes, selectedCount: dishes.filter((d) => d.selected).length, err: '' })
  },

  num(v: string): number | undefined {
    const t = (v || '').trim()
    if (!t) return undefined
    return Number(t)
  },

  submit() {
    if (this.data.submitting) return
    const d = this.data
    const price = this.num(d.price)
    const originalPrice = this.num(d.originalPrice)
    if (!d.comboName.trim()) {
      this.setData({ err: '请填写套餐名称' })
      return
    }
    if (price === undefined || isNaN(price) || price <= 0 || price > 99999) {
      this.setData({ err: '请填写正确的价格（0 - 99999 元）' })
      return
    }
    if (originalPrice !== undefined && (isNaN(originalPrice) || originalPrice <= 0)) {
      this.setData({ err: '原价需为大于 0 的数字' })
      return
    }
    const dishItems = d.dishes
      .filter((x) => x.selected)
      .map((x) => ({ dishId: x.id, quantity: 1, isFixed: 0 }))
    if (!dishItems.length) {
      this.setData({ err: '请至少选择 1 道组成菜品' })
      return
    }

    this.setData({ submitting: true })
    const payload = {
      combo: {
        comboName: d.comboName.trim(),
        price,
        originalPrice,
        description: d.description.trim() || undefined,
        comboImage: d.image.trim() || undefined,
        suitableFor: d.suitableFor.trim() || undefined,
        status: d.onSale ? 10 : 20
      },
      dishItems
    }
    const req = d.editId
      ? updateMerchantCombo(d.editId, payload)
      : createMerchantCombo(payload)
    req
      .then(() => {
        wx.showToast({ title: d.editId ? '已保存' : '新增成功', icon: 'none' })
        setTimeout(() => wx.navigateBack({ fail: () => wx.redirectTo({ url: '/pages/m-combos/m-combos' }) }), 600)
      })
      .catch((error: Error) => {
        this.setData({ submitting: false })
        wx.showToast({ title: error.message || '保存失败', icon: 'none' })
      })
  },

  goBack() {
    wx.navigateBack({ fail: () => wx.redirectTo({ url: '/pages/m-combos/m-combos' }) })
  },

  noop() {}
})
