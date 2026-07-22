import { getCart, type CartItem } from '../../api/cart'
import { getAddresses, type UserAddress } from '../../api/user'
import { createOrder, getOrders, payOrder } from '../../api/order'
import { getSafeArea } from '../../utils/safe-area'
import { requireUser } from '../../utils/user-route'

/** 套餐换菜备注 key（与 combo-detail.ts 保持一致；不 import 页面模块以免重复执行 Page 注册） */
const COMBO_SWAP_NOTE_KEY = 'bitesmart_combo_swap_note'

const round2 = (n: number): number => Math.round(n * 100) / 100

Page({
  data: {
    loading: false,
    items: [] as CartItem[],
    address: null as UserAddress | null,
    remark: '',
    delivery: 0, // 0 外卖配送 / 1 到店自取（展示层选项，金额以后端为准）
    subtotal: 0,
    fee: 5,
    pay: 0,
    menuTop: 0,
    menuH: 32
  },
  onLoad() {
    if (!requireUser()) return
    const { menuTop, menuH } = getSafeArea()
    this.setData({ menuTop, menuH })
  },
  onShow() {
    getCart().then((items) => {
      const selected = items.filter((item) => item.selected === 1)
      const subtotal = round2(selected.reduce((sum, item) => sum + Number(item.price || 0) * item.quantity, 0))
      this.setData({ items: selected, subtotal })
      this.refreshPay()
    }).catch(() => {})
    getAddresses().then((addresses) => {
      const selected = wx.getStorageSync('selectedAddress') as UserAddress | undefined
      this.setData({ address: selected || addresses.find((item) => item.isDefault === 1) || addresses[0] || null })
    }).catch(() => {})
    // 套餐换菜记录（combo-detail 加购时写入）：附到订单备注，仅预填一次，用户可改
    try {
      const note = wx.getStorageSync(COMBO_SWAP_NOTE_KEY) as string
      if (note && this.data.remark.indexOf(note) < 0) {
        this.setData({ remark: this.data.remark ? `${this.data.remark}；${note}` : note })
      }
    } catch (_) { /* ignore */ }
  },
  refreshPay() {
    const fee = this.data.delivery === 0 ? 5 : 0
    this.setData({ fee, pay: round2(this.data.subtotal + fee) })
  },
  back() { wx.navigateBack() },
  chooseAddress() { wx.navigateTo({ url: '/pages/address/address?selectable=1' }) },
  setDelivery(event: WechatMiniprogram.CustomEvent) {
    this.setData({ delivery: Number(event.currentTarget.dataset.v) })
    this.refreshPay()
  },
  onRemarkInput(event: WechatMiniprogram.Input) { this.setData({ remark: event.detail.value }) },
  submit() {
    if (this.data.loading) return
    if (this.data.delivery === 0 && !this.data.address) {
      wx.showToast({ title: '请先选择收货地址', icon: 'none' })
      return
    }
    if (!this.data.items.length) {
      wx.showToast({ title: '没有待结算的商品', icon: 'none' })
      return
    }
    this.setData({ loading: true })
    const address = this.data.address
    const addressText = this.data.delivery === 1
      ? '门店自取'
      : `${(address && address.province) || ''}${(address && address.city) || ''}${(address && address.district) || ''}${(address && address.detailAddress) || ''}`
    createOrder({
      address: addressText,
      receiverName: (address && address.receiverName) || '',
      receiverPhone: (address && address.receiverPhone) || '',
      remark: this.data.remark
    })
      .then((created) => getOrders().then((result) => ({ created, orders: Array.isArray(result) ? result : result.records })))
      .then(({ created, orders }) => {
        const order = orders.find((item) => item.orderNo === created.orderNo)
        if (!order || !order.id) throw new Error('订单已创建，但未找到订单编号')
        return payOrder(order.id, 20).then(() => order.id)
      })
      .then((orderId) => {
        // 订单已创建，清除换菜备注，避免污染下一单
        try { wx.removeStorageSync(COMBO_SWAP_NOTE_KEY) } catch (_) { /* ignore */ }
        wx.redirectTo({ url: `/pages/pay-result/pay-result?orderId=${orderId}` })
      })
      .catch((error: Error) => wx.showToast({ title: error.message || '订单提交失败', icon: 'none' }))
      .finally(() => this.setData({ loading: false }))
  }
})
