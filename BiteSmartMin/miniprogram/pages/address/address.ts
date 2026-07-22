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
    form: { receiverName: '', receiverPhone: '', detailAddress: '' },
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
        form: { receiverName: address.receiverName || '', receiverPhone: address.receiverPhone || '', detailAddress: address.detailAddress || '' }
      })
    }
  },
  onInput(event: WechatMiniprogram.Input) {
    this.setData({ [`form.${event.currentTarget.dataset.field}`]: event.detail.value })
  },
  saveAddress() {
    const form = this.data.form
    if (!form.receiverName || !form.receiverPhone || !form.detailAddress) {
      wx.showToast({ title: '请填写完整地址信息', icon: 'none' })
      return
    }
    const request = this.data.editingId
      ? updateAddress(this.data.editingId, form)
      : addAddress({ receiverName: form.receiverName, receiverPhone: form.receiverPhone, detailAddress: form.detailAddress, isDefault: this.data.addresses.length ? 0 : 1 })
    request
      .then(() => {
        this.setData({ formVisible: false, editingId: '', form: { receiverName: '', receiverPhone: '', detailAddress: '' } })
        this.loadAddresses()
      })
      .catch((error: Error) => wx.showToast({ title: error.message || '保存失败', icon: 'none' }))
  }
})
