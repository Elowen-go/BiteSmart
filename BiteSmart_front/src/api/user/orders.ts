import request from '../../utils/request'

// 后端 Long id 序列化为 string，id 字段统一 number | string
export interface Order {
  id: number | string
  orderNo: string
  merchantId: number | string
  totalAmount: number
  discountAmount: number
  payAmount: number
  deliveryType?: number
  deliveryFee?: number
  couponId?: number | string
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
  refundId?: number | string
  lockStockTime?: string
  autoCancelTime?: string
  channel?: string
  cancelTime?: string
  cancelReason?: string
  finishTime?: string
  createTime: string
  updateTime: string
}

export interface OrderItem {
  id: number | string
  orderId: number | string
  dishId: number | string
  comboId: number | string
  name: string
  price: number
  quantity: number
}

export const createOrder = (address: string, receiverName: string, receiverPhone: string, remark?: string): Promise<any> => {
  return request.post('/orders', null, { params: { address, receiverName, receiverPhone, remark } })
}

export interface MerchantOrderRemark {
  merchantId: number | string
  remark?: string
}

export const createBatchOrder = (data: {
  address: string
  receiverName: string
  receiverPhone: string
  deliveryType?: number
  latitude?: number
  longitude?: number
  merchantOrders: MerchantOrderRemark[]
}): Promise<any> => {
  return request.post('/orders/batch', data)
}

export const getOrderList = (params?: { page?: number; size?: number }): Promise<any> => {
  return request.get('/orders', { params })
}

export const getOrderDetail = (id: number | string): Promise<any> => {
  return request.get(`/orders/${id}`)
}

export const cancelOrder = (id: number | string, reason?: string): Promise<any> => {
  return request.post(`/orders/${id}/cancel`, null, { params: { reason } })
}

export const payOrder = (id: number | string, payMethod: number): Promise<any> => {
  return request.post(`/orders/${id}/pay`, null, { params: { payMethod } })
}
