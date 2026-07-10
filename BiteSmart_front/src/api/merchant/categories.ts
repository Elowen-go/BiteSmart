import request from '../../utils/request'

export interface DishCategory {
  id: number
  categoryName: string
  categoryIcon: string
  sortOrder: number
  parentId: number
  createTime: string
}

export const getCategoryList = (): Promise<any> => {
  return request.get('/merchant/categories')
}

export const addCategory = (data: DishCategory): Promise<any> => {
  return request.post('/merchant/categories', data)
}

export const updateCategory = (id: number, data: DishCategory): Promise<any> => {
  return request.put(`/merchant/categories/${id}`, data)
}

export const deleteCategory = (id: number): Promise<any> => {
  return request.delete(`/merchant/categories/${id}`)
}
