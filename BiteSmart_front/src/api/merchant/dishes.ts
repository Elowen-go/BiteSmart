import request from '../../utils/request'

export interface Dish {
  id: number
  merchantId: number
  categoryId: number
  name: string
  description: string
  price: number
  costPrice: number
  imageUrl: string
  calories: number
  protein: number
  fat: number
  carbs: number
  stock: number
  lockStock: number
  status: number
  sortOrder: number
  createTime: string
  updateTime: string
}

export const getDishList = (params?: { page?: number; size?: number }): Promise<any> => {
  return request.get('/merchant/dishes', { params })
}

export const addDish = (data: Dish): Promise<any> => {
  return request.post('/merchant/dishes', data)
}

export const updateDish = (id: number, data: Dish): Promise<any> => {
  return request.put(`/merchant/dishes/${id}`, data)
}

export const deleteDish = (id: number): Promise<any> => {
  return request.delete(`/merchant/dishes/${id}`)
}

export const getDishDetail = (id: number): Promise<any> => {
  return request.get(`/merchant/dishes/${id}`)
}
