import { getSafeArea } from '../../utils/safe-area'
import { getDriverProfile, updateDriverProfile } from '../../api/delivery'

Page({
  data: {
    menuTop: 26,
    menuH: 32,
    realName: '',
    phone: '',
    err: '',
    submitting: false
  },

  onLoad() {
    const sa = getSafeArea()
    this.setData({ menuTop: sa.menuTop, menuH: sa.menuH })
    // 预填：GET /api/driver/profile（后端已实现；失败静默，表单仍可填写提交）
    getDriverProfile()
      .then((p) => {
        if (!p) return
        this.setData({ realName: p.realName || '', phone: p.phone || '' })
      })
      .catch((error: Error) => console.warn('[r-profile] 资料加载失败：', error && error.message))
  },

  onInput(e: WechatMiniprogram.CustomEvent) {
    const field = e.currentTarget.dataset.field as string
    this.setData({ [field]: e.detail.value, err: '' } as Record<string, string>)
  },

  submit() {
    if (this.data.submitting) return
    const realName = this.data.realName.trim()
    const phone = this.data.phone.trim()
    if (!realName) {
      this.setData({ err: '请填写姓名' })
      return
    }
    if (phone && !/^1\d{10}$/.test(phone)) {
      this.setData({ err: '请填写正确的 11 位手机号' })
      return
    }
    this.setData({ submitting: true })
    updateDriverProfile({ realName, phone })
      .then(() => {
        wx.showToast({ title: '已保存', icon: 'none' })
        setTimeout(() => wx.navigateBack({ fail: () => wx.redirectTo({ url: '/pages/r-me/r-me' }) }), 600)
      })
      .catch((error: Error) => {
        this.setData({ submitting: false })
        wx.showToast({ title: error.message || '保存失败', icon: 'none' })
      })
  },

  goBack() {
    wx.navigateBack({ fail: () => wx.redirectTo({ url: '/pages/r-me/r-me' }) })
  },

  noop() {}
})
