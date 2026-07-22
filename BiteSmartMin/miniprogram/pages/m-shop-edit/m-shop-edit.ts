import { getSafeArea } from '../../utils/safe-area'
import { getMerchantShop, updateMerchantShop } from '../../api/merchant'

Page({
  data: {
    menuTop: 26,
    menuH: 32,
    shopName: '',
    contactPhone: '',
    shopAddress: '',
    shopNotice: '',
    open: true,
    loaded: false,
    err: '',
    submitting: false
  },

  onLoad() {
    const sa = getSafeArea()
    this.setData({ menuTop: sa.menuTop, menuH: sa.menuH })
    // 预填：以 GET /merchant/shop 实际返回字段为准
    getMerchantShop()
      .then((shop) => {
        if (!shop) return
        this.setData({
          shopName: shop.shopName || '',
          contactPhone: shop.contactPhone || '',
          shopAddress: shop.shopAddress || '',
          shopNotice: shop.shopNotice || '',
          // openStatus 后端并行开发中：缺省时按营业中兜底
          open: shop.openStatus !== 20,
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
    this.setData({ open: !this.data.open })
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
      contactPhone: d.contactPhone.trim(),
      shopAddress: d.shopAddress.trim(),
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
