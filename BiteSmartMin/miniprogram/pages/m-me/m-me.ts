import { getSafeArea } from '../../utils/safe-area'
import { clearAuth } from '../../utils/auth'
import { getMerchantShop, getMerchantTodayStats, type MerchantShop } from '../../api/merchant'
import { money } from '../../utils/merchant-vm'
import { uimg } from '../../mock/catalog'

const FALLBACK_LOGO = uimg('1543353071-873f17a7a088', 200)

Page({
  data: {
    padTop: 44,
    active: 'me',
    shopName: 'BiteSmart 门店',
    shopLogo: FALLBACK_LOGO,
    shopSub: '',
    rating: '—',
    orderCount: 0,
    revenue: '0'
  },

  onLoad() {
    this.setData({ padTop: getSafeArea().padTop })
    this.loadAll()
  },

  onShow() {
    this.loadAll()
  },

  loadAll() {
    getMerchantShop()
      .then((shop: MerchantShop | null) => {
        if (!shop) return
        const hours = shop.businessHours ? ` · ${shop.businessHours}` : ''
        this.setData({
          shopName: shop.shopName || 'BiteSmart 门店',
          shopLogo: shop.shopLogo || FALLBACK_LOGO,
          shopSub: `评分 ${shop.avgRating != null ? shop.avgRating : '—'}${hours}`,
          rating: shop.avgRating != null ? String(shop.avgRating) : '—'
        })
      })
      .catch((error: Error) => console.warn('[m-me] 店铺信息加载失败：', error && error.message))

    getMerchantTodayStats()
      .then((s) => {
        this.setData({
          orderCount: Number(s.orderCount || 0),
          revenue: money(Number(s.revenue || 0))
        })
      })
      .catch((error: Error) => console.warn('[m-me] 今日统计加载失败：', error && error.message))
  },

  goStats() {
    wx.navigateTo({ url: '/pages/m-stats/m-stats' })
  },

  goReviews() {
    wx.navigateTo({ url: '/pages/m-reviews/m-reviews' })
  },

  goCombos() {
    wx.navigateTo({ url: '/pages/m-combos/m-combos' })
  },

  shopInfo() {
    wx.navigateTo({ url: '/pages/m-shop-edit/m-shop-edit' })
  },

  logout() {
    wx.showModal({
      title: '退出商家端？',
      content: '退出后需要重新登录',
      confirmText: '退出',
      confirmColor: '#C0563F',
      success: (res) => {
        if (!res.confirm) return
        clearAuth()
        wx.reLaunch({ url: '/pages/login/login' })
      }
    })
  },

  goTab(e: WechatMiniprogram.CustomEvent) {
    const url = e.currentTarget.dataset.url as string
    const key = e.currentTarget.dataset.key as string
    if (!url || key === this.data.active) return
    wx.redirectTo({ url })
  },

  noop() {}
})
