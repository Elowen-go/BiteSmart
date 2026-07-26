import { clearAuth, getUserInfo } from '../../utils/auth'
import { getSafeArea } from '../../utils/safe-area'
import { getWeightRecords, type WeightRecord } from '../../api/health'
import { getProfile } from '../../api/user'

interface MenuItem {
  key: string
  label: string
  icon: string
  url: string
  hint?: string
}

Page({
  data: {
    padTop: 44,
    nickname: '王硕',
    initial: '王',
    goal: '健康生活方式',
    isVip: true,
    stats: [
      { num: '12', unit: '', label: '连续记录 / 天' },
      { num: '--', unit: ' kg', label: '当前体重' },
      { num: '--', unit: ' kg', label: '本月变化' }
    ],
    quicks: [
      { key: 'orders', label: '订单', url: '/pages/order/order' },
      { key: 'cart', label: '购物车', url: '/pages/cart/cart' },
      { key: 'plan', label: '食谱', url: '/pages/plan/plan' },
      { key: 'review', label: '评价', url: '/pages/review/review' }
    ],
    menus: [
      { key: 'assessment', label: '健康档案', icon: 'ic-doc', url: '/pages/assessment/assessment', hint: '未定制' },
      { key: 'health', label: '健康记录', icon: 'ic-chart', url: '/pages/health/health' },
      { key: 'member', label: '会员中心', icon: 'ic-crown', url: '/pages/member/member', hint: 'PRO' },
      { key: 'address', label: '收货地址', icon: 'ic-pin', url: '/pages/address/address' },
      { key: 'notice', label: '系统公告', icon: 'ic-note', url: '/pages/notice/notice' },
      { key: 'settings', label: '设置', icon: 'ic-gear', url: '/pages/settings/settings' },
      { key: 'complaint', label: '投诉建议', icon: 'ic-chat', url: '/pages/complaint/complaint' }
    ] as MenuItem[]
  },

  onLoad() {
    const user = getUserInfo()
    const nickname = (user && (user.nickname || user.username)) || '王硕'
    this.setData({
      padTop: getSafeArea().padTop,
      nickname,
      initial: nickname.slice(0, 1)
    })
  },

  onShow() {
    this.loadStats()
  },

  /** 当前体重 / 本月变化：来自真实体重记录；目标文案：健康档案 healthGoal */
  loadStats() {
    getWeightRecords()
      .then((result) => {
        const list: WeightRecord[] = Array.isArray(result) ? result : result.list || []
        if (!list || !list.length) return
        const sorted = list.slice().sort((a, b) => String(a.recordDate || '').localeCompare(String(b.recordDate || '')))
        const latest = sorted[sorted.length - 1]
        const cur = latest.weight || 0
        const monthPrefix = String(latest.recordDate || '').slice(0, 7)
        const monthFirst = sorted.find((x) => String(x.recordDate || '').slice(0, 7) === monthPrefix) || sorted[0]
        const delta = Math.round((cur - (monthFirst.weight || cur)) * 10) / 10
        const stats = this.data.stats.slice()
        stats[1] = { ...stats[1], num: String(cur) }
        stats[2] = { ...stats[2], num: (delta > 0 ? '+' : '') + delta }
        this.setData({ stats })
      })
      .catch(() => {})
    getProfile()
      .then((profile) => {
        if (profile && profile.healthGoal) {
          this.setData({ goal: `${profile.healthGoal} · 每日 ${profile.dailyCalorieTarget || 2000} kcal` })
        }
      })
      .catch(() => {})
    // TODO(B 类)：连续记录天数暂无后端统计接口，保持静态展示，等 /api/health/stats 或前端按 diet 记录连算
  },

  goMember() {
    wx.navigateTo({ url: '/pages/member/member' })
  },

  openMenu(event: WechatMiniprogram.CustomEvent) {
    const url = String(event.currentTarget.dataset.url || '')
    if (!url) return
    wx.navigateTo({ url })
  },

  logout() {
    wx.showModal({
      title: '退出登录',
      content: '确定要退出当前账号吗？',
      confirmColor: '#1E9E62',
      success: (res) => {
        if (!res.confirm) return
        clearAuth()
        wx.reLaunch({ url: '/pages/login/login' })
      }
    })
  }
})
