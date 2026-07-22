import { getDish, type Dish } from '../../api/catalog'
import { addToCart, getCart } from '../../api/cart'
import { MOCK_DISHES, uimg, type MockDish } from '../../mock/catalog'
import { getSafeArea } from '../../utils/safe-area'
import { requireUser } from '../../utils/user-route'
import { parseJsonList } from '../../utils/json'

interface DishView {
  id: number | string
  name: string
  desc: string
  price: string
  kcal: number
  image: string
  protein: number
  carbs: number
  fat: number
  sold: number
  ai: string
  fit: string[]
  caution: string[]
  wKcal: number
  wProtein: number
  wCarbs: number
  wFat: number
}

const clamp = (n: number): number => Math.max(0, Math.min(100, Math.round(n)))

const widths = (kcal: number, protein: number, carbs: number, fat: number) => ({
  wKcal: clamp(kcal / 9),
  wProtein: clamp(protein * 2.2),
  wCarbs: clamp(carbs * 1.3),
  wFat: clamp(fat * 2.6)
})

const buildFromMock = (mock: MockDish): DishView => ({
  id: mock.id,
  name: mock.name,
  desc: mock.desc,
  price: String(mock.price),
  kcal: mock.kcal,
  image: uimg(mock.img, 1200),
  protein: mock.protein,
  carbs: mock.carbs,
  fat: mock.fat,
  sold: mock.sold,
  ai: mock.ai,
  fit: mock.fit,
  caution: mock.caution,
  ...widths(mock.kcal, mock.protein, mock.carbs, mock.fat)
})

/**
 * 真实菜品 → 视图。B 类字段已上线：aiComment / fitScenes / cautions（后两者为 JSON 数组字符串），
 * 优先用后端数据；字段为空时回退 mock 同 id 内容，再为空则隐藏对应区块（wxml wx:if 防御）。
 */
const buildFromDish = (dish: Dish, id: number | string): DishView => {
  const mock = MOCK_DISHES.find((d) => String(d.id) === String(id))
  const kcal = Number(dish.calories || (mock && mock.kcal) || 0)
  const protein = Number(dish.protein || (mock && mock.protein) || 0)
  const carbs = Number(dish.carbs || (mock && mock.carbs) || 0)
  const fat = Number(dish.fat || (mock && mock.fat) || 0)
  const fitScenes = parseJsonList(dish.fitScenes)
  const cautions = parseJsonList(dish.cautions)
  return {
    id,
    name: dish.dishName || (mock && mock.name) || '健康菜品',
    desc: dish.description || (mock && mock.desc) || '',
    price: String(dish.price != null ? dish.price : (mock && mock.price) || 0),
    kcal,
    image: dish.dishImage || (mock ? uimg(mock.img, 1200) : ''),
    protein,
    carbs,
    fat,
    sold: Number(dish.salesCount || (mock && mock.sold) || 0),
    ai: dish.aiComment || (mock ? mock.ai : ''),
    fit: fitScenes.length ? fitScenes : (mock ? mock.fit : []),
    caution: cautions.length ? cautions : (mock ? mock.caution : []),
    ...widths(kcal, protein, carbs, fat)
  }
}

Page({
  data: { item: null as DishView | null, menuTop: 0, cartCount: 0 },
  onLoad(options: Record<string, string>) {
    if (!requireUser()) return
    const { menuTop } = getSafeArea()
    this.setData({ menuTop })
    // 菜品 id 为雪花字符串，原样透传禁止 Number() 强转；mock 为小数字 id，比较时统一 String 口径
    const id = (options && options.id) || ''
    const mock = MOCK_DISHES.find((d) => String(d.id) === id)
    // mock 同 id 命中则先渲染保证秒开；未命中等待接口（失败后再兜底）
    if (mock) this.setData({ item: buildFromMock(mock) })
    this.refreshCartCount()
    if (!id) {
      if (!this.data.item) this.setData({ item: buildFromMock(MOCK_DISHES[0]) })
      return
    }
    getDish(id)
      .then((dish: Dish) => this.setData({ item: buildFromDish(dish, id) }))
      .catch((error: Error) => {
        console.warn('[food-detail] 菜品接口不可用，使用本地 mock：', error && error.message)
        if (!this.data.item) this.setData({ item: buildFromMock(MOCK_DISHES[0]) })
      })
  },
  onShow() { this.refreshCartCount() },
  refreshCartCount() {
    getCart()
      .then((items) => this.setData({ cartCount: items.reduce((sum, entry) => sum + entry.quantity, 0) }))
      .catch(() => {})
  },
  back() { wx.navigateBack() },
  goCart() { wx.navigateTo({ url: '/pages/cart/cart' }) },
  add() {
    const item = this.data.item
    if (!item) return
    addToCart(10, item.id)
      .then(() => {
        wx.showToast({ title: '已加入购物车', icon: 'none' })
        this.refreshCartCount()
      })
      .catch((error: Error) => wx.showToast({ title: error.message || '加入失败', icon: 'none' }))
  },
  onShareAppMessage() {
    const item = this.data.item
    return { title: `${item ? item.name : '健康餐'} · BiteSmart`, path: `/pages/food-detail/food-detail?id=${item ? item.id : ''}` }
  }
})
