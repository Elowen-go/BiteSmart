import { requireUser } from '../../utils/user-route'
Page({ onLoad() { requireUser() }, viewOrder() { wx.redirectTo({ url: '/pages/order/order' }) }, backHome() { wx.reLaunch({ url: '/pages/index/index' }) } })
