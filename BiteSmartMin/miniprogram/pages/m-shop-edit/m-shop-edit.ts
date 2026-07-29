import { getSafeArea } from '../../utils/safe-area'
import { getMerchantShop, updateMerchantShop } from '../../api/merchant'
import { resolveFileUrl, uploadFile } from '../../api/file'
import { getCurrentUser, updateCurrentUser } from '../../api/user'
import { getUserInfo, setUserInfo } from '../../utils/auth'

Page({
  data: {
    menuTop: 26,
    menuH: 32,
    shopName: '',
    shopLogo: '',
    shopLogoUrl: '',
    userAvatarUrl: '',
    contactPhone: '',
    shopAddress: '',
    shopLat: 0,
    shopLng: 0,
    shopNotice: '',
    openKnown: false,
    open: true,
    loaded: false,
    err: '',
    submitting: false
  },

  onLoad() {
    const sa = getSafeArea()
    this.setData({ menuTop: sa.menuTop, menuH: sa.menuH })
    const cachedUser = getUserInfo()
    this.setData({ userAvatarUrl: resolveFileUrl(cachedUser && cachedUser.avatar) })
    getCurrentUser().then((user) => {
      setUserInfo(user)
      this.setData({ userAvatarUrl: resolveFileUrl(user.avatar) })
    }).catch(() => {})
    // 预填：以 GET /merchant/shop 实际返回字段为准
    getMerchantShop()
      .then((shop) => {
        if (!shop) return
        this.setData({
          shopName: shop.shopName || '',
          shopLogo: shop.shopLogo || '',
          shopLogoUrl: resolveFileUrl(shop.shopLogo),
          contactPhone: shop.contactPhone || '',
          shopAddress: shop.shopAddress || '',
          shopLat: Number(shop.shopLat || 0),
          shopLng: Number(shop.shopLng || 0),
          shopNotice: shop.shopNotice || '',
          openKnown: shop.openStatus === 10 || shop.openStatus === 20,
          open: shop.openStatus === 10,
          loaded: true
        })
      })
      .catch((error: Error) => {
        console.warn('[m-shop-edit] 店铺信息加载失败：', error && error.message)
        wx.showToast({ title: '店铺信息加载失败', icon: 'none' })
      })
  },

  onInput(e: WechatMiniprogram.CustomEvent) {
    const field = e.currentTarget.dataset.field as string
    this.setData({ [field]: e.detail.value, err: '' } as Record<string, string>)
  },

  toggleOpen() {
    if (!this.data.openKnown) return
    this.setData({ open: !this.data.open })
  },

  chooseShopLogo() {
    if (this.data.submitting) return
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (result) => {
        const filePath = result.tempFilePaths && result.tempFilePaths[0]
        if (!filePath) return
        this.setData({ submitting: true, err: '' })
        uploadFile(filePath, 'shop_logo')
          .then((url) => {
            this.setData({ shopLogo: url, shopLogoUrl: resolveFileUrl(url) })
            wx.showToast({ title: '头像上传成功', icon: 'success' })
          })
          .catch((error: Error) => {
            this.setData({ err: error.message || '头像上传失败' })
            wx.showToast({ title: error.message || '头像上传失败', icon: 'none' })
          })
          .finally(() => this.setData({ submitting: false }))
      }
    })
  },

  chooseUserAvatar() {
    if (this.data.submitting) return
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (result) => {
        const filePath = result.tempFilePaths && result.tempFilePaths[0]
        if (!filePath) return
        this.setData({ submitting: true, err: '' })
        uploadFile(filePath, 'avatar')
          .then((url) => updateCurrentUser({ avatar: url }))
          .then((user) => {
            setUserInfo(user)
            this.setData({ userAvatarUrl: resolveFileUrl(user.avatar) })
            wx.showToast({ title: '澶村儚宸叉洿鎹?', icon: 'success' })
          })
          .catch((error: Error) => wx.showToast({ title: error.message || '澶村儚涓婁紶澶辫触', icon: 'none' }))
          .finally(() => this.setData({ submitting: false }))
      }
    })
  },

  chooseShopLocation() {
    wx.chooseLocation({
      latitude: Number(this.data.shopLat) || undefined,
      longitude: Number(this.data.shopLng) || undefined,
      success: (result) => {
        this.setData({
          shopAddress: result.address || result.name || this.data.shopAddress,
          shopLat: Number(result.latitude || 0),
          shopLng: Number(result.longitude || 0),
          err: ''
        })
      },
      fail: (error) => {
        if (error && error.errMsg && error.errMsg.indexOf('cancel') >= 0) return
        wx.showModal({
          title: '需要位置权限',
          content: '开启位置权限后，才能在地图上选择店铺取货点。',
          confirmText: '去设置',
          success: (result) => {
            if (result.confirm) wx.openSetting({})
          }
        })
      }
    })
  },

  submit() {
    if (this.data.submitting) return
    const d = this.data
    if (!d.shopName.trim()) {
      this.setData({ err: '请填写店铺名称' })
      return
    }
    if (d.contactPhone.trim() && !/^1\d{10}$|^0\d{2,3}-?\d{7,8}$/.test(d.contactPhone.trim())) {
      this.setData({ err: '请填写正确的联系电话' })
      return
    }
    this.setData({ submitting: true })
    updateMerchantShop({
      shopName: d.shopName.trim(),
      shopLogo: d.shopLogo || undefined,
      contactPhone: d.contactPhone.trim(),
      shopAddress: d.shopAddress.trim(),
      shopLat: d.shopLat || undefined,
      shopLng: d.shopLng || undefined,
      shopNotice: d.shopNotice.trim(),
      openStatus: d.open ? 10 : 20
    })
      .then(() => {
        wx.showToast({ title: '已保存', icon: 'none' })
        setTimeout(() => wx.navigateBack({ fail: () => wx.redirectTo({ url: '/pages/m-me/m-me' }) }), 600)
      })
      .catch((error: Error) => {
        this.setData({ submitting: false })
        wx.showToast({ title: error.message || '保存失败', icon: 'none' })
      })
  },

  goBack() {
    wx.navigateBack({ fail: () => wx.redirectTo({ url: '/pages/m-me/m-me' }) })
  },

  noop() {}
})
