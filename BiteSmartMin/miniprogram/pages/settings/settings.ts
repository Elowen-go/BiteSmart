import { clearAuth } from '../../utils/auth'
import { getSafeArea } from '../../utils/safe-area'
import { requireUser } from '../../utils/user-route'

Page({
  data: { menuTop: 0, menuH: 32 },
  onLoad() {
    if (!requireUser()) return
    const { menuTop, menuH } = getSafeArea()
    this.setData({ menuTop, menuH })
  },
  back() { wx.navigateBack() },
  action(event: WechatMiniprogram.CustomEvent) {
    wx.showToast({ title: event.currentTarget.dataset.label, icon: 'none' })
  },
  logout() {
    wx.showModal({
      title: '退出登录？',
      content: '退出后将返回登录页',
      confirmText: '退出',
      confirmColor: '#C0563F',
      success: (result) => {
        if (result.confirm) {
          clearAuth()
          wx.reLaunch({ url: '/pages/login/login' })
        }
      }
    })
  }
})
