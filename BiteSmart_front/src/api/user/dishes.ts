import request from '../../utils/request'

export interface Dish {
  id: number
  merchantId: number
  categoryId: number
  dishName: string
  description: string
  price: number
  dishImage: string
  calories: number
  protein: number
  fat: number
  carbs: number
  stock: number
  status: number
  createTime: string
}

export const getDishList = (params?: { categoryId?: number; page?: number; size?: number }): Promise<any> => {
  return request.get('/dishes', { params })
}

export const getDishDetail = (id: number): Promise<any> => {
  return request.get(`/dishes/${id}`)
}
