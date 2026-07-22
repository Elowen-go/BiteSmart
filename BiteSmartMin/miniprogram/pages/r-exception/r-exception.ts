import { getSafeArea } from '../../utils/safe-area'
import { reportDriverException } from '../../api/delivery'

const EX_TYPES = ['联系不上用户', '商家出餐延迟', '地址错误', '交通异常', '商品损坏']

Page({
  data: {
    menuTop: 26,
    menuH: 32,
    id: '' as number | string,
    types: EX_TYPES,
    typeIndex: 0,
    text: '',
    err: false,
    saving: false
  },

  onLoad(options: Record<string, string | undefined>) {
    const sa = getSafeArea()
    this.setData({ menuTop: sa.menuTop, menuH: sa.menuH, id: (options && options.id) || '' })
  },

  pickType(e: WechatMiniprogram.CustomEvent) {
    this.setData({ typeIndex: Number(e.detail.value) })
  },

  onText(e: WechatMiniprogram.CustomEvent) {
    this.setData({ text: e.detail.value as string, err: false })
  },

  /** 提交异常：POST /driver/tasks/{id}/exception（任务 → 60 异常，客服介入） */
  submit() {
    if (this.data.saving) return
    const text = this.data.text.trim()
    if (!text) {
      this.setData({ err: true })
      return
    }
    this.setData({ saving: true })
    const reason = `${this.data.types[this.data.typeIndex]}：${text}`
    reportDriverException(this.data.id, reason)
      .then(() => {
        wx.showToast({ title: '异常已上报，客服将介入', icon: 'none' })
        setTimeout(() => wx.navigateBack({ fail: () => wx.redirectTo({ url: '/pages/r-tasks/r-tasks' }) }), 600)
      })
      .catch((error: Error) => wx.showToast({ title: error.message || '上报失败', icon: 'none' }))
      .finally(() => this.setData({ saving: false }))
  },

  goBack() {
    wx.navigateBack({ fail: () => wx.redirectTo({ url: '/pages/r-tasks/r-tasks' }) })
  },

  noop() {}
})
