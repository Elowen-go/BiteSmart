import request from '../../utils/request'

// 后端 Long id 序列化为 string，id 字段统一 number | string，禁止 Number() 强转
export interface CartItem {
  id: number | string
  userId: number | string
  itemType: number
  dishId: number | string
  comboId: number | string
  quantity: number
  selected: number
  createTime: string
  updateTime: string
}

export const getCartList = (): Promise<any> => {
  return request.get('/cart')
}

export const addToCart = (itemType: number, dishId?: number | string, comboId?: number | string, quantity?: number): Promise<any> => {
  return request.post('/cart', null, { params: { itemType, dishId, comboId, quantity } }).then((res: any) => {
    if (res.code === 200) window.dispatchEvent(new Event('cart-updated'))
    return res
  })
}

export const updateCartQuantity = (id: number | string, quantity: number): Promise<any> => {
  return request.put(`/cart/${id}`, null, { params: { quantity } })
}

export const updateCartSelected = (id: number | string, selected: number): Promise<any> => {
  return request.put(`/cart/${id}/select`, null, { params: { selected } })
}

export const deleteCartItem = (id: number | string): Promise<any> => {
  return request.delete(`/cart/${id}`).then((res: any) => {
    if (res.code === 200) window.dispatchEvent(new Event('cart-updated'))
    return res
  })
}
