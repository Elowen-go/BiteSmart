import { addAddress, deleteAddress, getAddresses, updateAddress, type UserAddress } from '../../api/user'
import { getSafeArea } from '../../utils/safe-area'
import { requireUser } from '../../utils/user-route'

Page({
  data: {
    addresses: [] as UserAddress[],
    loading: false,
    selectable: false,
    formVisible: false,
    editingId: '' as number | string,
    form: { receiverName: '', receiverPhone: '', detailAddress: '', locationName: '', latitude: 0, longitude: 0 },
    menuTop: 0,
    menuH: 32
  },
  onLoad(options: Record<string, string>) {
    if (!requireUser()) return
    const { menuTop, menuH } = getSafeArea()
    this.setData({ menuTop, menuH, selectable: options && options.selectable === '1' })
    this.loadAddresses()
  },
  loadAddresses() {
    this.setData({ loading: true })
    getAddresses()
      .then((addresses) => this.setData({ addresses }))
      .catch(() => {})
      .finally(() => this.setData({ loading: false }))
  },
  back() { wx.navigateBack() },
  selectAddress(event: WechatMiniprogram.CustomEvent) {
    if (!this.data.selectable) return
    wx.setStorageSync('selectedAddress', event.currentTarget.dataset.address)
    wx.navigateBack()
  },
  removeAddress(event: WechatMiniprogram.CustomEvent) {
    // 地址 id 为雪花字符串，禁止 Number() 强转
    const id = String(event.currentTarget.dataset.id || '')
    if (!id) return
    deleteAddress(id)
      .then(() => this.setData({ addresses: this.data.addresses.filter((item) => String(item.id) !== id) }))
      .catch((error: Error) => wx.showToast({ title: error.message || '删除失败', icon: 'none' }))
  },
  showForm() { this.setData({ formVisible: true }) },
  hideForm() { this.setData({ formVisible: false }) },
  editAddress(event: WechatMiniprogram.CustomEvent) {
    const id = String(event.currentTarget.dataset.id || '')
    const address = this.data.addresses.find((item) => String(item.id) === id)
    if (address) {
      this.setData({
        formVisible: true,
        editingId: address.id || '',
        form: {
          receiverName: address.receiverName || '',
          receiverPhone: address.receiverPhone || '',
          detailAddress: address.detailAddress || '',
          locationName: address.locationName || '',
          latitude: address.latitude || 0,
          longitude: address.longitude || 0
        }
      })
    }
  },
  onInput(event: WechatMiniprogram.Input) {
    this.setData({ [`form.${event.currentTarget.dataset.field}`]: event.detail.value })
  },
  chooseLocation() {
    const openPicker = () => {
      wx.chooseLocation({
        success: (result) => {
          this.setData({
            'form.locationName': result.name || '已选择位置',
            'form.detailAddress': result.address || result.name || '',
            'form.latitude': result.latitude || 0,
            'form.longitude': result.longitude || 0
          })
        },
        fail: (error) => {
          if (error && error.errMsg && error.errMsg.indexOf('cancel') >= 0) return
          this.showLocationSetting()
        }
      })
    }

    wx.getSetting({
      success: (setting) => {
        const status = setting.authSetting && setting.authSetting['scope.userLocation']
        if (status === true) {
          openPicker()
          return
        }
        if (status === false) {
          this.showLocationSetting()
          return
        }
        wx.authorize({
          scope: 'scope.userLocation',
          success: openPicker,
          fail: () => this.showLocationSetting()
        })
      },
      fail: openPicker
    })
  },
  showLocationSetting() {
    wx.showModal({
      title: '需要位置权限',
      content: '开启位置权限后，才能在地图上选择收货地址。',
      confirmText: '去设置',
      cancelText: '取消',
      success: (result) => {
        if (result.confirm) wx.openSetting({})
      }
    })
  },
  saveAddress() {
    const form = this.data.form
    if (!form.receiverName || !form.receiverPhone || !form.detailAddress) {
      wx.showToast({ title: '请填写完整地址信息', icon: 'none' })
      return
    }
    const request = this.data.editingId
      ? updateAddress(this.data.editingId, form)
      : addAddress({
        receiverName: form.receiverName,
        receiverPhone: form.receiverPhone,
        detailAddress: form.detailAddress,
        locationName: form.locationName,
        latitude: form.latitude,
        longitude: form.longitude,
        isDefault: this.data.addresses.length ? 0 : 1
      })
    request
      .then(() => {
        this.setData({ formVisible: false, editingId: '', form: { receiverName: '', receiverPhone: '', detailAddress: '', locationName: '', latitude: 0, longitude: 0 } })
        this.loadAddresses()
      })
      .catch((error: Error) => wx.showToast({ title: error.message || '保存失败', icon: 'none' }))
  }
})
