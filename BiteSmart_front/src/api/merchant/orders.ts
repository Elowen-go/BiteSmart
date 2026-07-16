import request from '../../utils/request'

export interface MerchantOrder {
  id: number
  orderNo: string
  userId: number
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
  deliveryAddress: string
  receiverName: string
  receiverPhone: string
  remark: string
  refundId?: number
  lockStockTime?: string
  autoCancelTime?: string
  channel?: string
  cancelTime?: string
  cancelReason?: string
  finishTime?: string
  createTime: string
  updateTime: string
}

export const getOrderList = (params?: { page?: number; size?: number }): Promise<any> => {
  return request.get('/merchant/orders', { params })
}

export const getOrderDetail = (id: number): Promise<any> => {
  return request.get(`/merchant/orders/${id}`)
}

export const acceptOrder = (id: number): Promise<any> => {
  return request.put(`/merchant/orders/${id}/accept`)
}

export const rejectOrder = (id: number, reason?: string): Promise<any> => {
  return request.put(`/merchant/orders/${id}/reject`, null, { params: { reason } })
}

export const prepareOrder = (id: number): Promise<any> => {
  return request.put(`/merchant/orders/${id}/prepare`)
}

export const doneOrder = (id: number): Promise<any> => {
  return request.put(`/merchant/orders/${id}/done`)
}
