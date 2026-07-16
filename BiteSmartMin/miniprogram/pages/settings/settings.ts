import { clearAuth } from '../../utils/auth'
import { requireUser } from '../../utils/user-route'
Page({ onLoad() { requireUser() }, back() { wx.navigateBack() }, action(event: WechatMiniprogram.CustomEvent) { wx.showToast({ title: event.currentTarget.dataset.label, icon: 'none' }) }, logout() { clearAuth(); wx.reLaunch({ url: '/pages/login/login' }) } })
