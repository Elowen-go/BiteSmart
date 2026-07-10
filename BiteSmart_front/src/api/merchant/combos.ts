import request from '../../utils/request'

export interface Combo {
  id: number
  merchantId: number
  name: string
  description: string
  price: number
  originalPrice: number
  imageUrl: string
  calories: number
  protein: number
  fat: number
  carbs: number
  status: number
  sortOrder: number
  createTime: string
  updateTime: string
}

export const getComboList = (params?: { page?: number; size?: number }): Promise<any> => {
  return request.get('/merchant/combos', { params })
}

export const addCombo = (data: Combo): Promise<any> => {
  return request.post('/merchant/combos', data)
}

export const updateCombo = (id: number, data: Combo): Promise<any> => {
  return request.put(`/merchant/combos/${id}`, data)
}

export const deleteCombo = (id: number): Promise<any> => {
  return request.delete(`/merchant/combos/${id}`)
}
