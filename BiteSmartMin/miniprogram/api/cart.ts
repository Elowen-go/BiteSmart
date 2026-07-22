import { request } from '../utils/request'

/** 购物车条目：id/dishId/comboId 均为雪花 Long，传输层是字符串，禁止 Number() 强转 */
export interface CartItem { id: number | string; itemType: number; dishId?: number | string; comboId?: number | string; quantity: number; selected: number; dishName?: string; comboName?: string; dishImage?: string; comboImage?: string; price?: number; merchantId?: number | string }
export const getCart = (): Promise<CartItem[]> => request<CartItem[]>({ url: '/cart' })
/** id 允许 string：计划餐的 dishId 是雪花 id，后端 Long 序列化为字符串，必须原样透传防精度丢失 */
export const addToCart = (itemType: number, id: number | string, quantity = 1): Promise<void> => request<void>({ url: '/cart', method: 'POST', data: { itemType, ...(itemType === 10 ? { dishId: id } : { comboId: id }), quantity }, contentType: 'form' })
export const updateCartQuantity = (id: number | string, quantity: number): Promise<void> => request<void>({ url: `/cart/${id}`, method: 'PUT', data: { quantity }, contentType: 'form' })
export const selectCartItem = (id: number | string, selected: number): Promise<void> => request<void>({ url: `/cart/${id}/select`, method: 'PUT', data: { selected }, contentType: 'form' })
export const deleteCartItem = (id: number | string): Promise<void> => request<void>({ url: `/cart/${id}`, method: 'DELETE' })
