import request from '../../utils/request'

export interface DishCategory {
  id: number
  categoryName: string
  categoryIcon: string
  sortOrder: number
  parentId: number
  createTime: string
}

export const listCategories = (): Promise<any> => {
  return request.get('/api/admin/categories')
}

export const addCategory = (data: Partial<DishCategory>): Promise<any> => {
  return request.post('/api/admin/categories', data)
}

export const updateCategory = (id: number, data: Partial<DishCategory>): Promise<any> => {
  return request.put(`/api/admin/categories/${id}`, data)
}

export const deleteCategory = (id: number): Promise<any> => {
  return request.delete(`/api/admin/categories/${id}`)
}
