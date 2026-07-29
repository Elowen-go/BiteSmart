import { getSafeArea } from '../../utils/safe-area'
import {
  getDriverSettlements,
  getSettlementStats,
  getDriverReviewStats,
  type DriverSettlement
} from '../../api/delivery'
import { money } from '../../utils/merchant-vm'

const WEEK_CN = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

interface BarVM { label: string; value: string; height: number; hot: boolean }

const pad = (n: number): string => (n < 10 ? '0' + n : '' + n)
const dateStr = (d: Date): string => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`

/** 结算记录归属日期：优先结算时间，其次创建时间（yyyy-MM-dd） */
const settleDate = (s: DriverSettlement): string => String(s.settlementTime || s.createTime || '').slice(0, 10)

Page({
  data: {
    padTop: 44,
    active: 'stats',
    todayAmount: '0',
    todayCount: 0,
    rating: '—',
    weekAmount: '0',
    monthAmount: '0',
    monthCount: 0,
    pendingAmount: '0',
    settledAmount: '0',
    totalAmount: '0',
    bars: [] as BarVM[],
    hasChartData: false
  },

  onLoad() {
    this.setData({ padTop: getSafeArea().padTop })
    this.loadAll()
  },

  onShow() {
    this.loadAll()
  },

  loadAll() {
    const now = new Date()
    const today = dateStr(now)
    const month = today.slice(0, 7)
    // 本周周一（日历周）
    const monday = new Date(now)
    monday.setDate(now.getDate() - ((now.getDay() + 6) % 7))
    const weekStart = dateStr(monday)
    // 近 7 日（含今日）
    const days: Date[] = []
    for (let i = 6; i >= 0; i--) {
      const d = new Date()
      d.setDate(d.getDate() - i)
      days.push(d)
    }

    // 收入统计：送达后生成的结算记录，按结算/创建日期聚合
    Promise.all([getDriverSettlements(), getSettlementStats()])
      .then(([rows, stats]) => {
        const byDate: Record<string, number> = {}
        let todayAmount = 0
        let todayCount = 0
        let weekAmount = 0
        let monthAmount = 0
        let monthCount = 0
        ;(rows || []).forEach((s) => {
          const d = settleDate(s)
          if (!d) return
          const amt = Number(s.settlementAmount || 0)
          byDate[d] = (byDate[d] || 0) + amt
          if (d === today) { todayAmount += amt; todayCount += 1 }
          if (d >= weekStart && d <= today) weekAmount += amt
          if (d.slice(0, 7) === month) { monthAmount += amt; monthCount += 1 }
        })
        const values = days.map((d) => byDate[dateStr(d)] || 0)
        const max = Math.max.apply(null, values.concat([1]))
        const bars: BarVM[] = days.map((d, i) => ({
          label: i === 6 ? '今日' : WEEK_CN[d.getDay()],
          value: money(values[i]),
          height: Math.max(8, Math.round((values[i] / max) * 156)),
          hot: i === 6
        }))
        this.setData({
          todayAmount: money(todayAmount),
          todayCount,
          weekAmount: money(weekAmount),
          monthAmount: money(monthAmount),
          monthCount,
          pendingAmount: money(stats.pendingAmount),
          settledAmount: money(stats.settledAmount),
          totalAmount: money(stats.totalAmount),
          bars,
          hasChartData: values.some((value) => value > 0)
        })
      })
      .catch((error: Error) => {
        console.warn('[r-stats] 结算数据加载失败：', error && error.message)
        wx.showToast({ title: '收入统计加载失败', icon: 'none' })
      })

    getDriverReviewStats()
      .then((s) => this.setData({ rating: s && s.averageRating != null ? String(s.averageRating) : '—' }))
      .catch((error: Error) => console.warn('[r-stats] 评分加载失败：', error && error.message))
  },

  goTab(e: WechatMiniprogram.CustomEvent) {
    const url = e.currentTarget.dataset.url as string
    const key = e.currentTarget.dataset.key as string
    if (!url || key === this.data.active) return
    wx.redirectTo({ url })
  },

  noop() {}
})
