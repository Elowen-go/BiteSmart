import { clearAuth } from '../../utils/auth'
import { bindWechat, getWechatBindingStatus } from '../../api/auth'
import { getSafeArea } from '../../utils/safe-area'
import { requireUser } from '../../utils/user-route'

Page({
  data: { menuTop: 0, menuH: 32, bindingWechat: false, wechatBound: false },
  onLoad() {
    if (!requireUser()) return
    const { menuTop, menuH } = getSafeArea()
    this.setData({ menuTop, menuH })
    getWechatBindingStatus()
      .then((bound) => this.setData({ wechatBound: bound }))
      .catch(() => undefined)
  },
  back() { wx.navigateBack() },
  action(event: WechatMiniprogram.CustomEvent) {
    wx.showToast({ title: event.currentTarget.dataset.label, icon: 'none' })
  },
  bindWechat() {
    if (this.data.bindingWechat) return
    wx.showModal({
      title: '绑定微信',
      content: '绑定后可使用微信一键登录当前账号',
      success: (result) => {
        if (!result.confirm) return
        this.setData({ bindingWechat: true })
        wx.login({
          success: (loginResult) => {
            bindWechat(loginResult.code)
              .then(() => {
                this.setData({ wechatBound: true })
                wx.showToast({ title: '绑定成功', icon: 'success' })
              })
              .catch((error: Error) => wx.showToast({ title: error.message || '绑定失败', icon: 'none' }))
              .finally(() => this.setData({ bindingWechat: false }))
          },
          fail: () => {
            this.setData({ bindingWechat: false })
            wx.showToast({ title: '无法获取微信登录凭证', icon: 'none' })
          }
        })
      }
    })
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
