import { requireUser } from '../../utils/user-route'
Page({ onLoad() { requireUser() }, back() { wx.navigateBack() }, delivery() { wx.navigateTo({ url: '/pages/delivery/delivery' }) }, action(event: WechatMiniprogram.CustomEvent) { wx.showToast({ title: event.currentTarget.dataset.label, icon: 'none' }) } })
