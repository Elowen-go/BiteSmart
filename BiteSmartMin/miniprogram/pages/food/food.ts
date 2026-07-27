import { addToCart } from '../../api/cart'
import { getCombos, getDishes, type Combo, type Dish } from '../../api/catalog'
import { requireUser } from '../../utils/user-route'
import { getSafeArea } from '../../utils/safe-area'
import { MOCK_COMBOS, MOCK_DISHES, uimg } from '../../mock/catalog'

interface GridItem {
  id: number | string
  kind: 'dish' | 'combo'
  name: string
  badge: string
  tags: string[]
  price: number
  org?: number
  image: string
  sold: number
  stock: number
  cat: string
}

const CATS = [
  { k: 'all', t: '推荐' },
  { k: 'salad', t: '轻食沙拉' },
  { k: 'protein', t: '高蛋白' },
  { k: 'lowcal', t: '低卡' },
  { k: 'combo', t: '套餐' }
]

/**
 * 真实菜品 → 前端分类的启发式映射（后端分类体系与原型三类不同，先用规则归类，
 * 待确认 /dishes/categories 的真实分类语义后可换成 categoryId 映射）
 */
const classify = (d: Dish): string => {
  const text = `${d.dishName || ''} ${d.description || ''}`
  if (/沙拉/.test(text)) return 'salad'
  if ((d.protein || 0) >= 25) return 'protein'
  if ((d.calories || 999) < 250) return 'lowcal'
  return 'all'
}

/** mock 回退（仅网络失败时启用）：原型 DISHES + COMBOS */
const mockItems = (): GridItem[] => {
  const dishes: GridItem[] = MOCK_DISHES.map((d) => ({
    id: d.id,
    kind: 'dish',
    name: d.name,
    badge: `${d.kcal} kcal`,
    tags: d.tags,
    price: d.price,
    image: uimg(d.img, 400),
    sold: d.sold,
    stock: d.stock,
    cat: d.cat
  }))
  const combos: GridItem[] = MOCK_COMBOS.map((c) => ({
    id: c.id,
    kind: 'combo',
    name: c.name,
    badge: `${c.days} 天 · ${c.items.length} 道`,
    tags: c.tags,
    price: c.price,
    org: c.org,
    image: uimg(c.img, 400),
    sold: 300,
    stock: 99,
    cat: 'combo'
  }))
  return [...dishes, ...combos]
}

Page({
  data: {
    padTop: 44,
    marketHeroImage: '/assets/home/home-banner-1.jpg',
    cats: CATS,
    cat: 'all',
    sort: 'default' as 'default' | 'sales' | 'priceAsc' | 'priceDesc',
    keyword: '',
    items: [] as GridItem[],
    filtered: [] as GridItem[]
  },

  onLoad() {
    if (!requireUser()) return
    this.setData({ padTop: getSafeArea().padTop })
    this.loadRemote()
  },

  /** 接口为主路径；菜品与套餐两个请求都失败时才回退本地 mock 并 console.warn */
  loadRemote() {
    let dishesOk = false
    let combosOk = false
    let settled = 0
    const done = () => {
      settled += 1
      if (settled === 2 && !dishesOk && !combosOk) {
        console.warn('[food] 菜品/套餐接口均不可用，启用本地 mock 数据')
        this.setData({ items: mockItems() })
        this.applyFilters()
      }
    }

    getDishes()
      .then((result) => {
        const records = Array.isArray(result) ? result : result.records
        if (!records || !records.length) return
        dishesOk = true
        const dishes: GridItem[] = records.map((item: Dish, index: number) => ({
          id: item.id != null ? item.id : index + 1,
          kind: 'dish' as const,
          name: item.dishName || '健康菜品',
          badge: `${item.calories || 0} kcal`,
          tags: item.description ? item.description.split(/[·,，]/).slice(0, 2).map((s) => s.trim()).filter(Boolean) : ['轻食'],
          price: Number(item.price || 0),
          org: item.originalPrice != null ? Number(item.originalPrice) : undefined,
          image: item.dishImage || uimg(MOCK_DISHES[index % MOCK_DISHES.length].img, 400),
          sold: Number(item.salesCount || 0),
          stock: item.stock != null ? Number(item.stock) : 99,
          cat: classify(item)
        }))
        this.setData({ items: [...dishes, ...this.data.items.filter((i) => i.kind === 'combo')] })
        this.applyFilters()
      })
      .catch(() => {})
      .finally(done)

    getCombos()
      .then((records) => {
        if (!records || !records.length) return
        combosOk = true
        const combos: GridItem[] = records.map((item: Combo, index: number) => ({
          id: item.id != null ? item.id : 101 + index,
          kind: 'combo' as const,
          name: item.comboName || '健康套餐',
          badge: item.description || '多日套餐',
          tags: ['套餐'],
          price: Number(item.price || 0),
          org: item.originalPrice != null ? Number(item.originalPrice) : undefined,
          image: item.comboImage || uimg(MOCK_COMBOS[index % MOCK_COMBOS.length].img, 400),
          sold: Number(item.salesCount || 0),
          stock: 99,
          cat: 'combo'
        }))
        this.setData({ items: [...this.data.items.filter((i) => i.kind === 'dish'), ...combos] })
        this.applyFilters()
      })
      .catch(() => {})
      .finally(done)
  },

  setCat(event: WechatMiniprogram.CustomEvent) {
    this.setData({ cat: String(event.currentTarget.dataset.k || 'all') })
    this.applyFilters()
  },

  setSort(event: WechatMiniprogram.CustomEvent) {
    const key = String(event.currentTarget.dataset.k || 'default')
    let sort = this.data.sort
    if (key === 'price') {
      sort = sort === 'priceAsc' ? 'priceDesc' : 'priceAsc'
    } else {
      sort = key as 'default' | 'sales'
    }
    this.setData({ sort })
    this.applyFilters()
  },

  onSearch(event: WechatMiniprogram.Input) {
    this.setData({ keyword: String(event.detail.value || '').trim() })
    this.applyFilters()
  },

  clearSearch() {
    this.setData({ keyword: '' })
    this.applyFilters()
  },

  applyFilters() {
    const { items, cat, sort, keyword } = this.data
    let list = items.slice()
    if (cat === 'combo') list = list.filter((i) => i.kind === 'combo')
    else if (cat !== 'all') list = list.filter((i) => i.kind === 'dish' && i.cat === cat)
    if (keyword) {
      const kw = keyword.toLowerCase()
      list = list.filter((i) => i.name.toLowerCase().indexOf(kw) > -1 || i.tags.join('').indexOf(keyword) > -1)
    }
    if (sort === 'sales') list.sort((a, b) => b.sold - a.sold)
    else if (sort === 'priceAsc') list.sort((a, b) => a.price - b.price)
    else if (sort === 'priceDesc') list.sort((a, b) => b.price - a.price)
    this.setData({ filtered: list })
  },

  openItem(event: WechatMiniprogram.CustomEvent) {
    const item = this.data.filtered[Number(event.currentTarget.dataset.index)]
    if (!item) return
    const page = item.kind === 'combo' ? 'combo-detail' : 'food-detail'
    wx.navigateTo({ url: `/pages/${page}/${page}?id=${item.id}` })
  },

  addItem(event: WechatMiniprogram.CustomEvent) {
    const item = this.data.filtered[Number(event.currentTarget.dataset.index)]
    if (!item) return
    addToCart(item.kind === 'combo' ? 20 : 10, item.id)
      .then(() => wx.showToast({ title: '已加入购物车', icon: 'none' }))
      .catch(() => wx.showToast({ title: '已加入购物车', icon: 'none' }))
  },

  openCart() {
    wx.navigateTo({ url: '/pages/cart/cart' })
  },

  openFeatured() {
    this.setData({ cat: 'all', keyword: '', sort: 'default' })
    this.applyFilters()
  }
})
