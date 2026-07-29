import { getSafeArea } from '../../utils/safe-area'
import { clearAuth, getUserInfo, setUserInfo } from '../../utils/auth'
import { getMerchantShop, getMerchantTodayStats, type MerchantShop } from '../../api/merchant'
import { money } from '../../utils/merchant-vm'
import { resolveFileUrl } from '../../api/file'
import { getCurrentUser } from '../../api/user'

const formatBusinessHours = (value: unknown): string => {
  if (!value) return ''
  const raw = String(value).trim()
  if (!raw) return ''

  try {
    const parsed = typeof value === 'object' ? value as Record<string, unknown> : JSON.parse(raw) as Record<string, unknown>
    const weekday = String(parsed.weekday || '').trim()
    const weekend = String(parsed.weekend || '').trim()
    if (weekday && weekend) return `工作日 ${weekday} · 周末 ${weekend}`
    if (weekday) return `工作日 ${weekday}`
    if (weekend) return `周末 ${weekend}`
  } catch {
    // 非 JSON 格式时直接显示原始营业时间文本
  }

  return raw.startsWith('{') ? '' : raw
}

Page({
  data: {
    padTop: 44,
    active: 'me',
    shopName: '',
    shopLogo: '',
    userAvatar: '',
    shopSub: '',
    rating: '—',
    orderCount: 0,
    revenue: '0'
  },

  onLoad() {
    this.setData({ padTop: getSafeArea().padTop })
    this.refreshAccount()
    this.loadAll()
  },

  onShow() {
    this.refreshAccount()
    this.loadAll()
  },

  refreshAccount() {
    const cached = getUserInfo()
    this.setData({ userAvatar: resolveFileUrl(cached && cached.avatar) })
    getCurrentUser()
      .then((user) => {
        setUserInfo(user)
        this.setData({ userAvatar: resolveFileUrl(user.avatar) })
      })
      .catch(() => {})
  },

  onAvatarError() {
    this.setData({ userAvatar: '' })
  },

  loadAll() {
    getMerchantShop()
      .then((shop: MerchantShop | null) => {
        if (!shop) return
        const hoursText = formatBusinessHours(shop.businessHours)
        const hours = hoursText ? ` · ${hoursText}` : ''
        this.setData({
          shopName: shop.shopName || '',
          shopLogo: resolveFileUrl(shop.shopLogo),
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

  goInventory() {
    wx.navigateTo({ url: '/pages/m-inventory/m-inventory' })
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
