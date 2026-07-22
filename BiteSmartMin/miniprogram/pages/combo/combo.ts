import { getCombos, type Combo } from '../../api/catalog'
import { MOCK_COMBOS, uimg } from '../../mock/catalog'
import { getSafeArea } from '../../utils/safe-area'
import { requireUser } from '../../utils/user-route'

interface ComboCard {
  id: number | string
  name: string
  en: string
  desc: string
  price: string
  org: string
  days: string
  kcal: string
  tags: string[]
  image: string
}

const fallback: ComboCard[] = MOCK_COMBOS.map((c) => ({
  id: c.id,
  name: c.name,
  en: c.en,
  desc: c.desc,
  price: String(c.price),
  org: String(c.org),
  days: String(c.days),
  kcal: `${c.kcal} kcal/天`,
  tags: c.tags,
  image: uimg(c.img, 900)
}))

Page({
  data: { items: [] as ComboCard[], menuTop: 0, menuH: 32 },
  onLoad() {
    if (!requireUser()) return
    const { menuTop, menuH } = getSafeArea()
    this.setData({ menuTop, menuH })
    getCombos().then((items) => {
      if (!items.length) return
      this.setData({
        items: items.map((item: Combo) => ({
          id: item.id || 0,
          name: item.comboName || '健康套餐',
          en: '',
          desc: item.description || '营养搭配 · 目标定制',
          price: String(item.price != null ? item.price : 0),
          org: item.originalPrice != null ? String(item.originalPrice) : '',
          days: '',
          kcal: item.totalCalories ? `${item.totalCalories} kcal/天` : '',
          tags: [],
          image: item.comboImage || fallback[0].image
        }))
      })
    }).catch((error: Error) => {
      // 仅网络失败时启用本地 mock
      console.warn('[combo] 套餐接口不可用，启用本地 mock：', error && error.message)
      this.setData({ items: fallback })
    })
  },
  back() { wx.navigateBack() },
  openDetail(event: WechatMiniprogram.CustomEvent) {
    wx.navigateTo({ url: `/pages/combo-detail/combo-detail?id=${event.currentTarget.dataset.id}` })
  }
})
