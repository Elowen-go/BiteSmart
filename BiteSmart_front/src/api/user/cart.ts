import request from '../../utils/request'

export interface CartItem {
  id: number
  userId: number
  itemType: number
  dishId: number
  comboId: number
  quantity: number
  selected: number
  createTime: string
  updateTime: string
}

export const getCartList = (): Promise<any> => {
  return request.get('/cart')
}

export const addToCart = (itemType: number, dishId?: number, comboId?: number, quantity?: number): Promise<any> => {
  return request.post('/cart', { params: { itemType, dishId, comboId, quantity } })
}

export const updateCartQuantity = (id: number, quantity: number): Promise<any> => {
  return request.put(`/cart/${id}`, { params: { quantity } })
}

export const updateCartSelected = (id: number, selected: number): Promise<any> => {
  return request.put(`/cart/${id}/select`, { params: { selected } })
}

export const deleteCartItem = (id: number): Promise<any> => {
  return request.delete(`/cart/${id}`)
}
