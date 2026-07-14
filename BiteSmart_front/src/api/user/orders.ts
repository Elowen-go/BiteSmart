import request from '../../utils/request'

export interface Order {
  id: number
  orderNo: string
  merchantId: number
  totalAmount: number
  discountAmount: number
  payAmount: number
  payMethod: number
  orderStatus: number
  deliveryStatus: number
  deliveryAddress: string
  receiverName: string
  receiverPhone: string
  remark: string
  createTime: string
  updateTime: string
}

export interface OrderItem {
  id: number
  orderId: number
  dishId: number
  comboId: number
  name: string
  price: number
  quantity: number
}

export const createOrder = (address: string, receiverName: string, receiverPhone: string, remark?: string): Promise<any> => {
  return request.post('/orders', null, { params: { address, receiverName, receiverPhone, remark } })
}

export interface MerchantOrderRemark {
  merchantId: number
  remark?: string
}

export const createBatchOrder = (data: {
  address: string
  receiverName: string
  receiverPhone: string
  merchantOrders: MerchantOrderRemark[]
}): Promise<any> => {
  return request.post('/orders/batch', data)
}

export const getOrderList = (params?: { page?: number; size?: number }): Promise<any> => {
  return request.get('/orders', { params })
}

export const getOrderDetail = (id: number): Promise<any> => {
  return request.get(`/orders/${id}`)
}

export const cancelOrder = (id: number, reason?: string): Promise<any> => {
  return request.post(`/orders/${id}/cancel`, null, { params: { reason } })
}

export const payOrder = (id: number, payMethod: number): Promise<any> => {
  return request.post(`/orders/${id}/pay`, null, { params: { payMethod } })
}
