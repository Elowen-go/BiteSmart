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
  payMethod: number
  orderStatus: number
  deliveryStatus: number
  deliveryAddress: string
  createTime: string
  updateTime: string
}

export const getOrderList = (params?: { pageNum?: number; pageSize?: number }): Promise<any> => {
  return request.get('/admin/orders', { params })
}

export const getOrderDetail = (id: number): Promise<any> => {
  return request.get(`/admin/orders/${id}`)
}
