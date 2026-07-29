import { request } from '../utils/request'

/** 订单（后端 Orders 字段）：地址字段为 deliveryAddress；id 为雪花 Long，JSON 传输层是字符串，全程禁止 Number() 强转 */
export interface RefundApplication {
  id: number | string
  orderId: number | string
  refundAmount?: number | string
  refundReason?: string
  refundDesc?: string
  auditStatus?: number
  auditRemark?: string
  applyTime?: string
  refundTime?: string
}
export interface Order { id: number | string; orderNo?: string; orderStatus?: number; totalAmount?: number; receiverName?: string; receiverPhone?: string; deliveryAddress?: string; createTime?: string; refundId?: number | string }
export interface OrderDetail { order: Order; items: Record<string, unknown>[]; statusTimeline: Record<string, unknown>[]; refundApplication?: RefundApplication | null }
export const getOrders = (page?: number): Promise<Order[] | { records: Order[] }> => request<Order[] | { records: Order[] }>({ url: '/orders', data: page ? { page, size: 10 } : {} })
export const getOrderDetail = (id: number | string): Promise<OrderDetail> => request<OrderDetail>({ url: `/orders/${id}` })
export const createOrder = (data: { address: string; receiverName: string; receiverPhone: string; remark?: string; latitude?: number; longitude?: number }): Promise<{ orderNo: string }> => request<{ orderNo: string }>({ url: '/orders', method: 'POST', data, contentType: 'form' })
export const cancelOrder = (id: number | string, reason?: string): Promise<void> => request<void>({ url: `/orders/${id}/cancel`, method: 'POST', data: { reason }, contentType: 'form' })
export const applyRefund = (id: number | string, reason?: string, desc?: string): Promise<void> => request<void>({ url: `/orders/${id}/refund`, method: 'POST', data: { reason, desc }, contentType: 'form' })
export const payOrder = (id: number | string, payMethod: number): Promise<Record<string, unknown>> => request<Record<string, unknown>>({ url: `/orders/${id}/pay`, method: 'POST', data: { payMethod }, contentType: 'form' })
