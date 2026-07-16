import { requireUser } from '../../utils/user-route'
Page({ onLoad() { requireUser() }, back() { wx.navigateBack() } })
