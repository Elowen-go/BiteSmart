import { getSafeArea } from '../../utils/safe-area'
import { getMerchantReviews, replyMerchantReview, type MerchantReview } from '../../api/merchant'
import { fmtDateTime } from '../../utils/json'

const REPLY_MAX = 200

interface ReviewVM {
  id: number | string
  name: string
  time: string
  ratingText: string
  content: string
  reply: string
  replying: boolean
  draft: string
  submitting: boolean
}

const buildVM = (r: MerchantReview): ReviewVM => ({
  id: r.id || 0,
  name: r.isAnonymous === 1 ? '匿名用户' : (r.userNickname || r.username || '用户'),
  time: fmtDateTime(r.createTime),
  ratingText: `菜品 ${r.ratingFood != null ? r.ratingFood : '—'} 星 · 服务 ${r.ratingService != null ? r.ratingService : '—'} 星 · 配送 ${r.ratingDelivery != null ? r.ratingDelivery : '—'} 星`,
  content: r.content || '用户未填写文字评价',
  reply: r.merchantReply || '',
  replying: false,
  draft: '',
  submitting: false
})

Page({
  data: {
    menuTop: 26,
    menuH: 32,
    replyMax: REPLY_MAX,
    list: [] as ReviewVM[]
  },

  onLoad() {
    const sa = getSafeArea()
    this.setData({ menuTop: sa.menuTop, menuH: sa.menuH })
    this.loadReviews()
  },

  onShow() {
    this.loadReviews()
  },

  loadReviews() {
    getMerchantReviews()
      .then((rows) => this.setData({ list: (rows || []).map(buildVM) }))
      .catch((error: Error) => {
        console.warn('[m-reviews] 评价加载失败：', error && error.message)
        wx.showToast({ title: '评价加载失败，请稍后重试', icon: 'none' })
      })
  },

  findIndex(id: number | string): number {
    return this.data.list.findIndex((x) => String(x.id) === String(id))
  },

  /** 展开回复输入框 */
  openReply(e: WechatMiniprogram.CustomEvent) {
    const i = this.findIndex(e.currentTarget.dataset.id)
    if (i < 0) return
    this.setData({ [`list[${i}].replying`]: true } as Record<string, unknown>)
  },

  cancelReply(e: WechatMiniprogram.CustomEvent) {
    const i = this.findIndex(e.currentTarget.dataset.id)
    if (i < 0) return
    this.setData({
      [`list[${i}].replying`]: false,
      [`list[${i}].draft`]: ''
    } as Record<string, unknown>)
  },

  onReplyInput(e: WechatMiniprogram.CustomEvent) {
    const i = this.findIndex(e.currentTarget.dataset.id)
    if (i < 0) return
    this.setData({ [`list[${i}].draft`]: String(e.detail.value).slice(0, REPLY_MAX) } as Record<string, unknown>)
  },

  /** 提交回复：POST /merchant/reviews/{id}/reply（content 走 query） */
  submitReply(e: WechatMiniprogram.CustomEvent) {
    const id = e.currentTarget.dataset.id as number | string
    const i = this.findIndex(id)
    if (i < 0) return
    const item = this.data.list[i]
    if (item.submitting) return
    const content = (item.draft || '').trim()
    if (!content) {
      wx.showToast({ title: '请填写回复内容', icon: 'none' })
      return
    }
    this.setData({ [`list[${i}].submitting`]: true } as Record<string, unknown>)
    replyMerchantReview(id, content)
      .then(() => {
        wx.showToast({ title: '回复成功', icon: 'none' })
        this.loadReviews()
      })
      .catch((error: Error) => {
        this.setData({ [`list[${i}].submitting`]: false } as Record<string, unknown>)
        wx.showToast({ title: error.message || '回复失败', icon: 'none' })
      })
  },

  goBack() {
    wx.navigateBack({ fail: () => wx.redirectTo({ url: '/pages/m-me/m-me' }) })
  },

  noop() {}
})
