import { addToCart } from '../../api/cart'
import { getCombo, getDish, type Combo, type ComboDishRel } from '../../api/catalog'
import { MOCK_COMBOS, MOCK_DISHES, uimg, type MockCombo, type MockDish } from '../../mock/catalog'
import { getSafeArea } from '../../utils/safe-area'
import { requireUser } from '../../utils/user-route'

interface ComboView {
  id: number | string
  name: string
  en: string
  desc: string
  price: string
  org: string
  days: number
  meals: number
  kcal: string
  protein: string
  image: string
  ai: string
}

interface MealItem { dishId: number | string; slot: string; name: string; kcal: number; price: number; protein: number; img: string }
interface DayMeals { day: string; items: MealItem[] }

/** 换菜记录 → 订单备注的 storage key（checkout 页读取预填 remark；addToCart 不支持备注字段，走订单备注是简单可靠方案） */
const COMBO_SWAP_NOTE_KEY = 'bitesmart_combo_swap_note'

const SLOTS = ['早餐', '午餐', '晚餐']

const comboDishes = (mock: MockCombo): MockDish[] =>
  mock.items.map((id) => MOCK_DISHES.find((d) => d.id === id)).filter(Boolean) as MockDish[]

/** 由 mock 套餐的菜品池轮换生成「days 天 × 三餐」餐单（无后端 dishRels 时的展示层视图模型） */
const buildDays = (mock: MockCombo): DayMeals[] => {
  const dishes = comboDishes(mock)
  if (!dishes.length) return []
  const count = Math.min(mock.days, 7)
  const list: DayMeals[] = []
  for (let d = 0; d < count; d += 1) {
    const items: MealItem[] = SLOTS.map((slot, s) => {
      const dish = dishes[(d * SLOTS.length + s) % dishes.length]
      return { dishId: dish.id, slot, name: dish.name, kcal: dish.kcal, price: dish.price, protein: dish.protein, img: uimg(dish.img, 200) }
    })
    list.push({ day: `DAY ${d + 1}`, items })
  }
  return list
}

const buildView = (mock: MockCombo): ComboView => {
  const dishes = comboDishes(mock)
  const protein = dishes.length ? Math.round(dishes.reduce((s, d) => s + d.protein, 0) / dishes.length) * 3 : 0
  return {
    id: mock.id,
    name: mock.name,
    en: mock.en,
    desc: mock.desc,
    price: String(mock.price),
    org: String(mock.org),
    days: mock.days,
    meals: mock.days * 3,
    kcal: String(mock.kcal),
    protein: String(protein),
    image: uimg(mock.img, 1200),
    ai: `本套餐日均 ${mock.kcal} kcal、蛋白质约 ${protein}g，${mock.days} 天 ${mock.days * 3} 餐已由营养师排好，适合${mock.tags.join('、')}人群。餐单每天不重样，任意一餐都可随时更换。`
  }
}

Page({
  data: {
    combo: null as ComboView | null,
    dayMeals: [] as DayMeals[],
    featuredMeals: [] as MealItem[],
    menuTop: 0,
    swapVisible: false,
    swapDay: 0,
    swapSlot: 0,
    swapCandidates: [] as MealItem[],
    swapSummary: ''
  },
  /** 换菜基准（首次换菜前的总价/总热量/总蛋白），用于差异展示 */
  swapBaseline: null as { price: number; kcal: number; protein: number } | null,
  /** 换菜记录（从 → 到），加购时写入 storage 供 checkout 附到订单备注 */
  swapNotes: [] as string[],
  onLoad(options: Record<string, string>) {
    if (!requireUser()) return
    const { menuTop } = getSafeArea()
    // 套餐 id 为雪花字符串，原样透传禁止 Number() 强转；mock 为小数字 id，比较时统一 String 口径
    const id = (options && options.id) || String(MOCK_COMBOS[0].id)
    const mock = MOCK_COMBOS.find((c) => String(c.id) === id)
    // mock 命中先渲染保证秒开；未命中等待接口
    if (mock) {
      const days = buildDays(mock)
      this.setData({ menuTop, combo: buildView(mock), dayMeals: days, featuredMeals: days.length ? days[0].items : [] })
    }
    else this.setData({ menuTop })
    getCombo(id)
      .then((detail) => this.applyRemote(id, detail.combo, detail.dishRels))
      .catch((error: Error) => {
        console.warn('[combo-detail] 套餐接口不可用，使用本地 mock：', error && error.message)
        if (!this.data.combo) {
          const days = buildDays(MOCK_COMBOS[0])
          this.setData({ combo: buildView(MOCK_COMBOS[0]), dayMeals: days, featuredMeals: days.length ? days[0].items : [] })
        }
      })
  },

  /** 真实套餐数据：基本信息直接映射；餐单优先用 dishRels（逐菜品拉详情），缺失时保留 mock 餐单 */
  applyRemote(id: number | string, combo: Combo | undefined, dishRels?: ComboDishRel[]) {
    const current = this.data.combo
    if (!combo) return
    const totalKcal = Number(combo.totalCalories || 0)
    const totalProtein = Number(combo.totalProtein || 0)
    this.setData({
      combo: {
        id,
        name: String(combo.comboName || (current && current.name) || '健康套餐'),
        en: (current && current.en) || '',
        desc: String(combo.description || (current && current.desc) || ''),
        price: String(combo.price != null ? combo.price : (current && current.price) || 0),
        org: combo.originalPrice != null ? String(combo.originalPrice) : '',
        days: (current && current.days) || 1,
        meals: (current && current.meals) || 3,
        kcal: totalKcal ? String(totalKcal) : (current && current.kcal) || '--',
        protein: totalProtein ? String(totalProtein) : (current && current.protein) || '--',
        image: String(combo.comboImage || (current && current.image) || ''),
        ai: (current && current.ai) || ''
      }
    })
    if (dishRels && dishRels.length) this.buildRemoteMeals(dishRels)
  },

  /** dishRels → 每日餐单（按三餐/天切分；菜品详情逐个拉取，失败条目静默跳过） */
  buildRemoteMeals(rels: ComboDishRel[]) {
    Promise.all(
      rels.map((rel) =>
        // 菜品 id 为雪花字符串，原样透传禁止 Number() 强转
        getDish(rel.dishId || '')
          .then((dish) => ({
            dishId: rel.dishId || '',
            slot: '',
            name: dish.dishName || '健康菜品',
            kcal: Number(dish.calories || 0) * (rel.quantity || 1),
            price: Number(dish.price || 0) * (rel.quantity || 1),
            protein: Number(dish.protein || 0) * (rel.quantity || 1),
            img: dish.dishImage || ''
          }))
          .catch(() => null)
      )
    ).then((results) => {
      const meals = results.filter(Boolean) as MealItem[]
      if (!meals.length) return
      const dayMeals: DayMeals[] = []
      for (let i = 0; i < meals.length; i += 3) {
        const items = meals.slice(i, i + 3).map((m, s) => ({ ...m, slot: SLOTS[s % 3] }))
        dayMeals.push({ day: `DAY ${dayMeals.length + 1}`, items })
      }
      this.setData({
        dayMeals,
        featuredMeals: dayMeals.length ? dayMeals[0].items : [],
        combo: this.data.combo ? { ...this.data.combo, days: dayMeals.length, meals: meals.length } : this.data.combo
      })
    })
  },

  back() { wx.navigateBack() },
  /** 加购前把换菜记录写入 storage（addToCart 结构不支持备注，由 checkout 读取附到订单 remark） */
  persistSwapNote() {
    if (!this.swapNotes.length) return
    try {
      wx.setStorageSync(COMBO_SWAP_NOTE_KEY, `已更换：${this.swapNotes.join('；')}`)
    } catch (_) { /* ignore */ }
  },
  add() {
    const combo = this.data.combo
    if (!combo) return
    this.persistSwapNote()
    addToCart(20, combo.id)
      .then(() => wx.showToast({ title: '套餐已加入购物车', icon: 'none' }))
      .catch((error: Error) => wx.showToast({ title: error.message || '加入失败', icon: 'none' }))
  },
  buyNow() {
    const combo = this.data.combo
    if (!combo) return
    this.persistSwapNote()
    addToCart(20, combo.id)
      .then(() => wx.navigateTo({ url: '/pages/checkout/checkout' }))
      .catch((error: Error) => wx.showToast({ title: error.message || '操作失败', icon: 'none' }))
  },
  /** TODO：后端已有 POST /combos/{id}/replace 换菜接口（自动重算营养），
   *  语义待确认（是否影响套餐定义/是否需要登录身份），本次换菜保持本地状态 */
  openSwap(event: WechatMiniprogram.CustomEvent) {
    const { day, slot } = event.currentTarget.dataset
    const candidates: MealItem[] = MOCK_DISHES.map((d) => ({ dishId: d.id, slot: '', name: d.name, kcal: d.kcal, price: d.price, protein: d.protein, img: uimg(d.img, 200) }))
    this.setData({ swapVisible: true, swapDay: Number(day), swapSlot: Number(slot), swapCandidates: candidates })
  },
  closeSwap() { this.setData({ swapVisible: false }) },

  /** 本地实时重算：换菜后立即重算套餐总价/总热量/总蛋白并展示差异（如 换后 ¥96（+¥4）· 412kcal（-38）） */
  doSwap(event: WechatMiniprogram.CustomEvent) {
    const { day, slot, id } = event.currentTarget.dataset
    const dish = MOCK_DISHES.find((d) => String(d.id) === String(id))
    if (!dish) return
    const totals = (days: DayMeals[]) => days.reduce((acc, entry) => {
      entry.items.forEach((m) => {
        acc.price += m.price
        acc.kcal += m.kcal
        acc.protein += m.protein
      })
      return acc
    }, { price: 0, kcal: 0, protein: 0 })
    // 首次换菜时以当前餐单为基准
    if (!this.swapBaseline) this.swapBaseline = totals(this.data.dayMeals)
    const oldMeal = this.data.dayMeals[Number(day)] && this.data.dayMeals[Number(day)].items[Number(slot)]
    const dayMeals = this.data.dayMeals.map((entry, di) => di !== Number(day) ? entry : {
      ...entry,
      items: entry.items.map((meal, mi) => mi !== Number(slot) ? meal : { ...meal, dishId: dish.id, name: dish.name, kcal: dish.kcal, price: dish.price, protein: dish.protein, img: uimg(dish.img, 200) })
    })
    if (oldMeal && oldMeal.name !== dish.name) this.swapNotes.push(`${oldMeal.name}→${dish.name}`)
    const now = totals(dayMeals)
    const base = this.swapBaseline
    const dPrice = Math.round((now.price - base.price) * 100) / 100
    const fmtDiff = (v: number, unit: string): string => `${v > 0 ? '+' : ''}${unit === '¥' ? '¥' : ''}${v}`
    this.setData({
      dayMeals,
      swapVisible: false,
      swapSummary: `换后 ¥${now.price}（${fmtDiff(dPrice, '¥')}）· ${now.kcal}kcal（${fmtDiff(now.kcal - base.kcal, '')}）· 蛋白质 ${now.protein}g（${fmtDiff(now.protein - base.protein, '')}）`
    })
    wx.showToast({ title: '已更换', icon: 'none' })
  }
})
