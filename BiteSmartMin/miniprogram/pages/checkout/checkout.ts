import { createOrder } from '../../api/order'
import { requireUser } from '../../utils/user-route'

Page({
  data: { loading: false },
  onLoad() { requireUser() },
  back() { wx.navigateBack() },
  submit() {
    if (this.data.loading) return
    this.setData({ loading: true })
    createOrder({ address: '滨江创意园 A 座 3 楼', receiverName: '安之', receiverPhone: '13800002026', remark: '' }).then(() => wx.redirectTo({ url: '/pages/pay-result/pay-result' })).catch((error: Error) => wx.showToast({ title: error.message || '订单提交失败', icon: 'none' })).finally(() => this.setData({ loading: false }))
  }
})
