import { requireUser } from '../../utils/user-route'
Page({ data: { image: 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=900&q=80' }, onLoad() { requireUser() }, back() { wx.navigateBack() }, openChat() { wx.navigateTo({ url: '/pages/chat/chat' }) }, action(event: WechatMiniprogram.CustomEvent) { wx.showToast({ title: event.currentTarget.dataset.label, icon: 'none' }) } })
