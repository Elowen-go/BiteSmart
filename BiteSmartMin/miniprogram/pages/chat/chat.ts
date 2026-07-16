import { chat } from '../../api/ai'
import { requireUser } from '../../utils/user-route'

Page({
  data: { question: '', loading: false },
  onLoad() { requireUser() },
  back() { wx.navigateBack() },
  onInput(event: WechatMiniprogram.Input) { this.setData({ question: event.detail.value }) },
  send() { if (!this.data.question || this.data.loading) return; this.setData({ loading: true }); chat(this.data.question).then(() => this.setData({ question: '' })).catch(() => wx.showToast({ title: 'AI 暂时没有回应', icon: 'none' })).finally(() => this.setData({ loading: false })) },
  action(event: WechatMiniprogram.CustomEvent) { wx.showToast({ title: event.currentTarget.dataset.label, icon: 'none' }) }
})
