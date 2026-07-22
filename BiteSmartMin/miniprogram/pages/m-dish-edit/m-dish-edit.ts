import { getSafeArea } from '../../utils/safe-area'
import {
  createMerchantDish,
  updateMerchantDish,
  getMerchantDish,
  getMerchantCategories,
  type MerchantCategory
} from '../../api/merchant'

Page({
  data: {
    menuTop: 26,
    menuH: 32,
    editId: '' as number | string,
    // 表单（输入框均为字符串，提交时转数值）
    dishName: '',
    price: '',
    stock: '',
    calories: '',
    protein: '',
    fat: '',
    carbs: '',
    image: '',
    imgError: false,
    suitableFor: '',
    tagsText: '',
    aiComment: '',
    onSale: true,
    // 分类
    categories: [] as MerchantCategory[],
    catNames: [] as string[],
    catIndex: -1,
    err: '',
    submitting: false
  },

  /** 编辑模式的分类回显：分类接口与详情接口并行，谁先完成都尝试应用 */
  editCatId: '' as number | string,

  applyCatIndex() {
    if (!this.editCatId || !this.data.categories.length) return
    const idx = this.data.categories.findIndex((c) => String(c.id) === String(this.editCatId))
    if (idx >= 0) this.setData({ catIndex: idx })
  },

  onLoad(options: Record<string, string | undefined>) {
    const sa = getSafeArea()
    this.setData({ menuTop: sa.menuTop, menuH: sa.menuH })
    getMerchantCategories()
      .then((rows) => {
        const categories = rows || []
        this.setData({
          categories,
          catNames: categories.map((c) => c.categoryName || '分类'),
          catIndex: categories.length ? 0 : -1
        })
        this.applyCatIndex()
      })
      .catch((error: Error) => console.warn('[m-dish-edit] 分类加载失败：', error && error.message))

    // 编辑模式：?id=x 预填详情，提交走 PUT
    const id = (options && options.id) || ''
    if (!id) return
    this.setData({ editId: id })
    getMerchantDish(id)
      .then((dish) => {
        // tags 列是 JSON 数组字符串，编辑时还原为逗号分隔文本
        let tagsText = ''
        try {
          const arr = JSON.parse(dish.tags || '[]') as unknown
          if (Array.isArray(arr)) tagsText = arr.join('，')
        } catch (_) { /* ignore */ }
        this.setData({
          dishName: dish.dishName || '',
          price: dish.price != null ? String(dish.price) : '',
          stock: dish.stock != null ? String(dish.stock) : '',
          calories: dish.calories != null ? String(dish.calories) : '',
          protein: dish.protein != null ? String(dish.protein) : '',
          fat: dish.fat != null ? String(dish.fat) : '',
          carbs: dish.carbs != null ? String(dish.carbs) : '',
          image: dish.dishImage || '',
          suitableFor: dish.suitableFor || '',
          tagsText,
          aiComment: dish.aiComment || '',
          onSale: dish.status !== 20
        })
        this.editCatId = dish.categoryId != null ? dish.categoryId : ''
        this.applyCatIndex()
      })
      .catch((error: Error) => {
        console.warn('[m-dish-edit] 菜品详情加载失败：', error && error.message)
        wx.showToast({ title: '菜品详情加载失败', icon: 'none' })
      })
  },

  onInput(e: WechatMiniprogram.CustomEvent) {
    const field = e.currentTarget.dataset.field as string
    const patch: Record<string, unknown> = { [field]: e.detail.value, err: '' }
    if (field === 'image') patch.imgError = false
    this.setData(patch)
  },

  onCatChange(e: WechatMiniprogram.CustomEvent) {
    this.setData({ catIndex: Number(e.detail.value) })
  },

  onImgError() {
    this.setData({ imgError: true })
  },

  toggleSale() {
    this.setData({ onSale: !this.data.onSale })
  },

  /** 数字解析：空串 → undefined（不传）；非法 → NaN */
  num(v: string): number | undefined {
    const t = (v || '').trim()
    if (!t) return undefined
    return Number(t)
  },

  submit() {
    if (this.data.submitting) return
    const d = this.data
    const price = this.num(d.price)
    const stock = this.num(d.stock)
    const calories = this.num(d.calories)
    const protein = this.num(d.protein)
    const fat = this.num(d.fat)
    const carbs = this.num(d.carbs)

    if (!d.dishName.trim()) {
      this.setData({ err: '请填写菜品名称' })
      return
    }
    if (price === undefined || isNaN(price) || price <= 0 || price > 99999) {
      this.setData({ err: '请填写正确的价格（0 - 99999 元）' })
      return
    }
    if (calories !== undefined && (isNaN(calories) || calories < 0 || calories > 10000)) {
      this.setData({ err: '热量需为 0 - 10000 kcal' })
      return
    }
    const nutrs: Array<[string, number | undefined]> = [['蛋白质', protein], ['脂肪', fat], ['碳水', carbs]]
    for (const [label, v] of nutrs) {
      if (v !== undefined && (isNaN(v) || v < 0 || v > 1000)) {
        this.setData({ err: `${label}需为 0 - 1000 g` })
        return
      }
    }
    if (stock !== undefined && (isNaN(stock) || stock < 0 || Math.floor(stock) !== stock)) {
      this.setData({ err: '库存需为不小于 0 的整数' })
      return
    }

    // 口味标签：逗号/顿号分隔 → JSON 数组字符串（Dish.tags 列格式）
    const tags = d.tagsText.split(/[,，、]/).map((s) => s.trim()).filter((s) => !!s)
    const cat = d.catIndex >= 0 ? d.categories[d.catIndex] : undefined

    this.setData({ submitting: true })
    const payload = {
      dishName: d.dishName.trim(),
      price,
      stock: stock === undefined ? 0 : stock,
      calories,
      protein,
      fat,
      carbs,
      categoryId: cat && cat.id !== undefined ? (cat.id as number) : undefined,
      dishImage: d.image.trim() || undefined,
      suitableFor: d.suitableFor.trim() || undefined,
      tags: tags.length ? JSON.stringify(tags) : undefined,
      aiComment: d.aiComment.trim() || undefined,
      status: d.onSale ? 10 : 20
    }
    const req = d.editId
      ? updateMerchantDish(d.editId, payload)
      : createMerchantDish(payload)
    req
      .then(() => {
        wx.showToast({ title: d.editId ? '已保存' : '新增成功', icon: 'none' })
        setTimeout(() => wx.navigateBack({ fail: () => wx.redirectTo({ url: '/pages/m-dishes/m-dishes' }) }), 600)
      })
      .catch((error: Error) => {
        this.setData({ submitting: false })
        wx.showToast({ title: error.message || '保存失败', icon: 'none' })
      })
  },

  goBack() {
    wx.navigateBack({ fail: () => wx.redirectTo({ url: '/pages/m-dishes/m-dishes' }) })
  },

  noop() {}
})
