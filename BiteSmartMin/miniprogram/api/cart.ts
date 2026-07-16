import { request } from '../utils/request'

export interface CartItem { id: number; itemType: number; dishId?: number; comboId?: number; quantity: number; selected: number; dishName?: string; comboName?: string; price?: number; merchantId?: number }
export const getCart = (): Promise<CartItem[]> => request<CartItem[]>({ url: '/cart' })
export const addToCart = (itemType: number, id: number, quantity = 1): Promise<void> => request<void>({ url: '/cart', method: 'POST', data: { itemType, ...(itemType === 10 ? { dishId: id } : { comboId: id }), quantity }, contentType: 'form' })
export const updateCartQuantity = (id: number, quantity: number): Promise<void> => request<void>({ url: `/cart/${id}`, method: 'PUT', data: { quantity }, contentType: 'form' })
export const selectCartItem = (id: number, selected: number): Promise<void> => request<void>({ url: `/cart/${id}/select`, method: 'PUT', data: { selected }, contentType: 'form' })
export const deleteCartItem = (id: number): Promise<void> => request<void>({ url: `/cart/${id}`, method: 'DELETE' })
