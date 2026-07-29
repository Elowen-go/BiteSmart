import { getSafeArea } from '../../utils/safe-area'
import { getDriverProfile, updateDriverProfile } from '../../api/delivery'
import { getCurrentUser, updateCurrentUser } from '../../api/user'
import { getUserInfo, setUserInfo } from '../../utils/auth'
import { resolveFileUrl, uploadFile } from '../../api/file'

Page({
  data: {
    menuTop: 26,
    menuH: 32,
    avatar: '',
    avatarUploading: false,
    realName: '',
    phone: '',
    serviceArea: '',
    vehicleTypes: ['请选择车辆', '电动车', '自行车', '汽车'],
    vehicleIndex: 0,
    err: '',
    submitting: false
  },

  onLoad() {
    const sa = getSafeArea()
    this.setData({ menuTop: sa.menuTop, menuH: sa.menuH })
    const user = getUserInfo()
    this.setData({ avatar: resolveFileUrl(user && user.avatar) })
    getCurrentUser().then((currentUser) => {
      setUserInfo(currentUser)
      this.setData({ avatar: resolveFileUrl(currentUser.avatar) })
    }).catch(() => {})
    // 预填：GET /api/driver/profile（后端已实现；失败静默，表单仍可填写提交）
    getDriverProfile()
      .then((p) => {
        if (!p) return
        const vehicleMap: Record<number, number> = { 10: 1, 20: 2, 30: 3 }
        this.setData({
          realName: p.realName || '',
          phone: p.phone || '',
          serviceArea: p.serviceArea || '',
          vehicleIndex: vehicleMap[Number(p.vehicleType)] || 0
        })
      })
      .catch((error: Error) => console.warn('[r-profile] 资料加载失败：', error && error.message))
  },

  onInput(e: WechatMiniprogram.CustomEvent) {
    const field = e.currentTarget.dataset.field as string
    this.setData({ [field]: e.detail.value, err: '' } as Record<string, string>)
  },

  chooseAvatar() {
    if (this.data.avatarUploading) return
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (result) => {
        const filePath = result.tempFilePaths && result.tempFilePaths[0]
        if (!filePath) return
        this.setData({ avatarUploading: true })
        uploadFile(filePath, 'avatar')
          .then((url) => updateCurrentUser({ avatar: url }))
          .then((currentUser) => {
            setUserInfo(currentUser)
            this.setData({ avatar: resolveFileUrl(currentUser.avatar) })
            wx.showToast({ title: '澶村儚宸叉洿鎹?', icon: 'success' })
          })
          .catch((error: Error) => wx.showToast({ title: error.message || '澶村儚涓婁紶澶辫触', icon: 'none' }))
          .finally(() => this.setData({ avatarUploading: false }))
      }
    })
  },

  onAvatarError() {
    this.setData({ avatar: '' })
  },

  pickVehicle(e: WechatMiniprogram.CustomEvent) {
    this.setData({ vehicleIndex: Number(e.detail.value), err: '' })
  },

  submit() {
    if (this.data.submitting) return
    const realName = this.data.realName.trim()
    const phone = this.data.phone.trim()
    const serviceArea = this.data.serviceArea.trim()
    if (!realName) {
      this.setData({ err: '请填写姓名' })
      return
    }
    if (phone && !/^1\d{10}$/.test(phone)) {
      this.setData({ err: '请填写正确的 11 位手机号' })
      return
    }
    if (this.data.vehicleIndex === 0) {
      this.setData({ err: '请选择车辆类型' })
      return
    }
    this.setData({ submitting: true })
    const vehicleTypes = [0, 10, 20, 30]
    updateDriverProfile({ realName, phone, serviceArea, vehicleType: vehicleTypes[this.data.vehicleIndex] })
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
