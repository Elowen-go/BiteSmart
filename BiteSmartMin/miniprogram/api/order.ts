import { request } from '../utils/request'

/** 订单（后端 Orders 字段）：地址字段为 deliveryAddress；id 为雪花 Long，JSON 传输层是字符串，全程禁止 Number() 强转 */
export interface Order { id: number | string; orderNo?: string; orderStatus?: number; totalAmount?: number; receiverName?: string; receiverPhone?: string; deliveryAddress?: string; createTime?: string }
export interface OrderDetail { order: Order; items: Record<string, unknown>[]; statusTimeline: Record<string, unknown>[] }
export const getOrders = (page?: number): Promise<Order[] | { records: Order[] }> => request<Order[] | { records: Order[] }>({ url: '/orders', data: page ? { page, size: 10 } : {} })
export const getOrderDetail = (id: number | string): Promise<OrderDetail> => request<OrderDetail>({ url: `/orders/${id}` })
export const createOrder = (data: { address: string; receiverName: string; receiverPhone: string; remark?: string }): Promise<{ orderNo: string }> => request<{ orderNo: string }>({ url: '/orders', method: 'POST', data, contentType: 'form' })
export const cancelOrder = (id: number | string, reason?: string): Promise<void> => request<void>({ url: `/orders/${id}/cancel`, method: 'POST', data: { reason }, contentType: 'form' })
export const payOrder = (id: number | string, payMethod: number): Promise<Record<string, unknown>> => request<Record<string, unknown>>({ url: `/orders/${id}/pay`, method: 'POST', data: { payMethod }, contentType: 'form' })
