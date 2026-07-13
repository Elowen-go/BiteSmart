import request from '../../utils/request'

export interface Combo {
  id: number
  merchantId: number
  comboName: string
  description: string
  price: number
  originalPrice: number
  comboImage: string
  calories: number
  protein: number
  fat: number
  carbs: number
  status: number
  createTime: string
}

export interface ComboDishRel {
  id: number
  comboId: number
  dishId: number
  dishName: string
  quantity: number
  isFixed: number
}

export const getComboList = (params?: { page?: number; size?: number }): Promise<any> => {
  return request.get('/combos', { params })
}

export const getComboDetail = (id: number): Promise<any> => {
  return request.get(`/combos/${id}`)
}

export const replaceComboDish = (comboId: number, oldDishId: number, newDishId: number): Promise<any> => {
  return request.post(`/combos/${comboId}/replace`, { params: { oldDishId, newDishId } })
}

export const replaceCartComboDish = (cartId: number, oldDishId: number, newDishId: number): Promise<any> => {
  return request.put(`/cart/${cartId}/replace`, { params: { oldDishId, newDishId } })
}
