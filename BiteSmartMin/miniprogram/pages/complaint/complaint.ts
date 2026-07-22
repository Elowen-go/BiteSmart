import { createComplaint } from '../../api/feedback'
import { getSafeArea } from '../../utils/safe-area'
import { requireUser } from '../../utils/user-route'

const TARGETS = [
  { v: 10, label: '商家' },
  { v: 20, label: '配送骑手' },
  { v: 30, label: '平台服务' }
]
const REASONS = ['餐品质量问题', '配送超时', '少送 / 错送', '服务态度', '其他']

Page({
  data: {
    // 订单 id 为雪花字符串，原样透传禁止 Number() 强转
    orderId: '' as number | string,
    targetType: 10,
    reason: REASONS[0],
    desc: '',
    targets: TARGETS,
    reasons: REASONS,
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
  setTarget(event: WechatMiniprogram.CustomEvent) { this.setData({ targetType: Number(event.currentTarget.dataset.v) }) },
  setReason(event: WechatMiniprogram.CustomEvent) { this.setData({ reason: String(event.currentTarget.dataset.v) }) },
  onInput(event: WechatMiniprogram.Input) { this.setData({ desc: event.detail.value }) },
  submit() {
    if (this.data.submitting) return
    if (!this.data.orderId) {
      wx.showToast({ title: '缺少订单信息', icon: 'none' })
      return
    }
    if (!this.data.desc.trim()) {
      wx.showToast({ title: '请填写问题描述', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    createComplaint({
      orderId: this.data.orderId,
      targetType: this.data.targetType,
      targetId: this.data.orderId,
      complaintReason: this.data.reason,
      complaintDesc: this.data.desc.trim()
    })
      .then(() => {
        wx.showToast({ title: '反馈已提交', icon: 'none' })
        setTimeout(() => wx.navigateBack(), 600)
      })
      .catch((error: Error) => wx.showToast({ title: error.message || '提交失败', icon: 'none' }))
      .finally(() => this.setData({ submitting: false }))
  }
})
