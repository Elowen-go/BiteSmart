import { getOrderDetail, type Order } from '../../api/order'
import { getSafeArea } from '../../utils/safe-area'
import { requireUser } from '../../utils/user-route'

Page({
  data: { orderId: '' as number | string, orderNo: '', totalAmount: '', eta: '', padTop: 0 },
  onLoad(options: Record<string, string>) {
    if (!requireUser()) return
    const { padTop } = getSafeArea()
    this.setData({ padTop })
    // 订单 id 为雪花字符串，原样透传禁止 Number() 强转
    const id = (options && options.orderId) || ''
    if (!id) return
    this.setData({ orderId: id })
    getOrderDetail(id).then((detail) => {
      const order: Partial<Order> = detail.order || {}
      const amount = order.totalAmount != null ? Number(order.totalAmount).toFixed(2).replace(/\.00$/, '') : ''
      const eta = Number(order.deliveryType) === 20 ? '' : '待骑手接单'
      this.setData({ orderNo: order.orderNo || '', totalAmount: amount, eta })
    }).catch(() => {})
  },
  viewOrder() {
    const id = this.data.orderId
    wx.redirectTo({ url: id ? `/pages/order-detail/order-detail?id=${id}` : '/pages/order/order' })
  },
  backHome() { wx.reLaunch({ url: '/pages/index/index' }) }
})
