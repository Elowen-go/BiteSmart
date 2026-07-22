import { createReview } from '../../api/feedback'
import { getSafeArea } from '../../utils/safe-area'
import { requireUser } from '../../utils/user-route'

Page({
  data: {
    stars: [1, 2, 3, 4, 5],
    ratingFood: 5,
    ratingDelivery: 5,
    ratingService: 5,
    // 订单 id 为雪花字符串，原样透传禁止 Number() 强转
    orderId: '' as number | string,
    content: '',
    isAnonymous: false,
    submitting: false,
    menuTop: 0,
    menuH: 32
  },
  onLoad(options: Record<string, string>) {
    if (!requireUser()) return
    const { menuTop, menuH } = getSafeArea()
    this.setData({ menuTop, menuH })
    if (options && options.orderId) this.setData({ orderId: options.orderId })
  },
  back() { wx.navigateBack() },
  rate(event: WechatMiniprogram.CustomEvent) {
    const { key, index } = event.currentTarget.dataset
    this.setData({ [key]: Number(index) + 1 } as Record<string, number>)
  },
  onInput(event: WechatMiniprogram.Input) { this.setData({ content: event.detail.value }) },
  onAnonChange(event: WechatMiniprogram.SwitchChange) { this.setData({ isAnonymous: event.detail.value }) },
  submit() {
    if (this.data.submitting) return
    if (!this.data.orderId) {
      wx.showToast({ title: '缺少订单信息', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    const { ratingFood, ratingDelivery, ratingService } = this.data
    const overallRating = Math.round((ratingFood + ratingDelivery + ratingService) / 3)
    createReview(this.data.orderId, {
      ratingFood,
      ratingDelivery,
      ratingService,
      overallRating,
      content: this.data.content,
      isAnonymous: this.data.isAnonymous ? 1 : 0
    })
      .then(() => wx.showToast({ title: '评价已提交', icon: 'none' }))
      .then(() => wx.navigateBack())
      .catch((error: Error) => wx.showToast({ title: error.message || '提交失败', icon: 'none' }))
      .finally(() => this.setData({ submitting: false }))
  }
})
