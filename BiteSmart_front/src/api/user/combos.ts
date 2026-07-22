import request from '../../utils/request'

export interface Combo {
  id: number | string
  merchantId: number | string
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
  id: number | string
  comboId: number | string
  dishId: number | string
  dishName: string
  quantity: number
  isFixed: number
}

export const getComboList = (params?: { keyword?: string; comboType?: number; sort?: string; page?: number; size?: number }): Promise<any> => {
  return request.get('/combos', { params })
}

export const getComboDetail = (id: number | string): Promise<any> => {
  return request.get(`/combos/${id}`)
}

export const replaceComboDish = (comboId: number | string, oldDishId: number | string, newDishId: number | string): Promise<any> => {
  return request.post(`/combos/${comboId}/replace`, null, { params: { oldDishId, newDishId } })
}

export const replaceCartComboDish = (cartId: number | string, oldDishId: number | string, newDishId: number | string): Promise<any> => {
  return request.put(`/cart/${cartId}/replace`, null, { params: { oldDishId, newDishId } })
}
