import request from '../../utils/request'

export interface Order {
  id: number
  orderNo: string
  userId: number
  merchantId: number
  deliveryDriverId: number
  totalAmount: number
  discountAmount: number
  payAmount: number
  couponId?: number
  couponDiscount?: number
  platformSubsidy?: number
  payMethod: number
  payTime?: string
  orderStatus: number
  deliveryStatus: number
  refundId?: number
  lockStockTime?: string
  autoCancelTime?: string
  channel?: string
  deliveryAddress: string
  createTime: string
  updateTime: string
  cancelTime?: string
  cancelReason?: string
  finishTime?: string
}

export const getOrderList = (params?: {
  pageNum?: number
  pageSize?: number
  orderStatus?: number
  userId?: number
  merchantId?: number
  paymentStatus?: number
  deliveryStatus?: number
}): Promise<any> => {
  return request.get('/admin/orders', { params })
}

export const getOrderDetail = (id: number): Promise<any> => {
  return request.get(`/admin/orders/${id}`)
}

export const cancelOrder = (id: number, reason?: string): Promise<any> => {
  return request.put(`/admin/orders/${id}/cancel`, null, { params: { reason } })
}
