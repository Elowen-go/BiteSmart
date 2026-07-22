import { getSafeArea } from '../../utils/safe-area'
import { getMerchantDailyStats, getMerchantTopDishes } from '../../api/merchant'
import { money } from '../../utils/merchant-vm'

const WEEK_CN = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

interface BarVM { label: string; value: string; height: number; hot: boolean }
interface TopVM { rank: number; name: string; qty: number }

const pad = (n: number): string => (n < 10 ? '0' + n : '' + n)
const dateStr = (d: Date): string => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`

Page({
  data: {
    menuTop: 26,
    menuH: 32,
    bars: [] as BarVM[],
    top: [] as TopVM[]
  },

  onLoad() {
    const sa = getSafeArea()
    this.setData({ menuTop: sa.menuTop, menuH: sa.menuH })

    // 近 7 日营收柱状图（含今日）；接口按日期合并，缺失日期补 0
    const days: Date[] = []
    for (let i = 6; i >= 0; i--) {
      const d = new Date()
      d.setDate(d.getDate() - i)
      days.push(d)
    }
    getMerchantDailyStats(dateStr(days[0]), dateStr(days[6]))
      .then((rows) => {
        const byDate: Record<string, number> = {}
        ;(rows || []).forEach((r) => { byDate[String(r.date || '')] = Number(r.revenue || 0) })
        const values = days.map((d) => byDate[dateStr(d)] || 0)
        const max = Math.max.apply(null, values.concat([1]))
        const bars: BarVM[] = days.map((d, i) => ({
          label: i === 6 ? '今日' : WEEK_CN[d.getDay()],
          value: money(values[i]),
          height: Math.max(8, Math.round((values[i] / max) * 156)),
          hot: i === 6
        }))
        this.setData({ bars })
      })
      .catch((error: Error) => {
        console.warn('[m-stats] 营收统计加载失败：', error && error.message)
        wx.showToast({ title: '营收统计加载失败', icon: 'none' })
      })

    getMerchantTopDishes(5)
      .then((rows) => {
        this.setData({
          top: (rows || []).map((r, i) => ({
            rank: i + 1,
            name: r.dishName || '菜品',
            qty: Number(r.totalQuantity || 0)
          }))
        })
      })
      .catch((error: Error) => console.warn('[m-stats] 热销排行加载失败：', error && error.message))
  },

  goBack() {
    wx.navigateBack({ fail: () => wx.redirectTo({ url: '/pages/m-me/m-me' }) })
  },

  noop() {}
})
