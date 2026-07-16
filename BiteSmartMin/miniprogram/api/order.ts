import { request } from '../utils/request'

export interface Order { id: number; orderNo?: string; orderStatus?: number; totalAmount?: number; receiverName?: string; receiverPhone?: string; address?: string; createTime?: string }
export interface OrderDetail { order: Order; items: Record<string, unknown>[]; statusTimeline: Record<string, unknown>[] }
export const getOrders = (page?: number): Promise<Order[] | { records: Order[] }> => request<Order[] | { records: Order[] }>({ url: '/orders', data: page ? { page, size: 10 } : {} })
export const getOrderDetail = (id: number): Promise<OrderDetail> => request<OrderDetail>({ url: `/orders/${id}` })
export const createOrder = (data: { address: string; receiverName: string; receiverPhone: string; remark?: string }): Promise<{ orderNo: string }> => request<{ orderNo: string }>({ url: '/orders', method: 'POST', data, contentType: 'form' })
export const cancelOrder = (id: number, reason?: string): Promise<void> => request<void>({ url: `/orders/${id}/cancel`, method: 'POST', data: { reason }, contentType: 'form' })
export const payOrder = (id: number, payMethod: number): Promise<Record<string, unknown>> => request<Record<string, unknown>>({ url: `/orders/${id}/pay`, method: 'POST', data: { payMethod }, contentType: 'form' })
