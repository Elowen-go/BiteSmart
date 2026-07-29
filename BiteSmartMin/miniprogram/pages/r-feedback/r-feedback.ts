import { submitDriverFeedback } from '../../api/driver-feedback'
import { getSafeArea } from '../../utils/safe-area'
import { getUserInfo } from '../../utils/auth'

const CATEGORIES = ['配送任务', '商家协作', '结算收入', '平台服务', '其他建议']

Page({
  data: {
    orderId: '' as number | string,
    category: CATEGORIES[0],
    categories: CATEGORIES,
    content: '',
    submitting: false,
    menuTop: 0,
    menuH: 32
  },

  onLoad(options: Record<string, string>) {
    if (!getUserInfo()) {
      wx.reLaunch({ url: '/pages/login/login' })
      return
    }
    const { menuTop, menuH } = getSafeArea()
    this.setData({ menuTop, menuH, orderId: (options && options.orderId) || '' })
  },

  back() {
    wx.navigateBack()
  },

  setCategory(event: WechatMiniprogram.CustomEvent) {
    this.setData({ category: String(event.currentTarget.dataset.value) })
  },

  onInput(event: WechatMiniprogram.Input) {
    this.setData({ content: event.detail.value })
  },

  submit() {
    if (this.data.submitting) return
    if (!this.data.orderId) {
      wx.showToast({ title: '缺少配送订单信息', icon: 'none' })
      return
    }
    const content = this.data.content.trim()
    if (!content) {
      wx.showToast({ title: '请填写反馈内容', icon: 'none' })
      return
    }

    this.setData({ submitting: true })
    submitDriverFeedback({
      orderId: this.data.orderId,
      category: this.data.category,
      content
    })
      .then(() => {
        wx.showToast({ title: '反馈已提交', icon: 'none' })
        setTimeout(() => wx.navigateBack(), 600)
      })
      .catch((error: Error) => wx.showToast({ title: error.message || '提交失败', icon: 'none' }))
      .finally(() => this.setData({ submitting: false }))
  }
})
