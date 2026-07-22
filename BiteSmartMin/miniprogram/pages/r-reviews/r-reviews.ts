import { getSafeArea } from '../../utils/safe-area'
import {
  getDriverReviews,
  getDriverReviewStats
} from '../../api/delivery'
import { fmtDateTime } from '../../utils/json'

interface ReviewVM {
  id: number | string
  name: string
  ratingText: string
  content: string
  time: string
}

Page({
  data: {
    menuTop: 26,
    menuH: 32,
    rating: '—',
    list: [] as ReviewVM[]
  },

  onLoad() {
    const sa = getSafeArea()
    this.setData({ menuTop: sa.menuTop, menuH: sa.menuH })
    this.loadAll()
  },

  loadAll() {
    getDriverReviewStats()
      .then((s) => this.setData({ rating: s && s.averageRating != null ? String(s.averageRating) : '—' }))
      .catch((error: Error) => console.warn('[r-reviews] 评分加载失败：', error && error.message))

    getDriverReviews()
      .then((rows) => {
        this.setData({
          list: (rows || []).map((r) => ({
            id: r.id || '',
            name: r.isAnonymous === 1 ? '匿名用户' : (r.userNickname || r.username || '用户'),
            ratingText: `配送 ${r.ratingDelivery != null ? r.ratingDelivery : '—'} 星`,
            content: r.content || '用户未填写文字评价',
            time: fmtDateTime(r.createTime)
          }))
        })
      })
      .catch((error: Error) => {
        console.warn('[r-reviews] 评价加载失败：', error && error.message)
        wx.showToast({ title: error.message || '评价加载失败', icon: 'none' })
      })
  },

  goBack() {
    wx.navigateBack({ fail: () => wx.redirectTo({ url: '/pages/r-me/r-me' }) })
  },

  noop() {}
})
