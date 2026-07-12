import request from '../../utils/request'

/**
 * 食材管理 API
 * 
 * 提供管理员端食材库的增删改查功能
 */

export interface Ingredient {
  id: number
  name: string
  categoryName: string
  calories: number
  protein: number
  fat: number
  carbs: number
  status: number
}

/**
 * 获取食材列表（分页）
 * @param params 分页参数 { page?: number, size?: number }
 * @returns 分页食材列表
 */
export const getIngredientList = (params?: { page?: number; size?: number }): Promise<any> => {
  return request.get('/admin/ingredients', { params })
}

/**
 * 获取所有分类名称
 * @returns 分类名称列表
 */
export const getIngredientCategories = (): Promise<any> => {
  return request.get('/admin/ingredients/categories')
}

/**
 * 获取食材详情
 * @param id 食材ID
 * @returns 食材详情
 */
export const getIngredientDetail = (id: number): Promise<any> => {
  return request.get(`/admin/ingredients/${id}`)
}

/**
 * 新增食材
 * @param data 食材信息
 * @returns 操作结果
 */
export const addIngredient = (data: Partial<Ingredient>): Promise<any> => {
  return request.post('/admin/ingredients', data)
}

/**
 * 更新食材
 * @param id 食材ID
 * @param data 食材信息
 * @returns 操作结果
 */
export const updateIngredient = (id: number, data: Partial<Ingredient>): Promise<any> => {
  return request.put(`/admin/ingredients/${id}`, data)
}

/**
 * 删除食材
 * @param id 食材ID
 * @returns 操作结果
 */
export const deleteIngredient = (id: number): Promise<any> => {
  return request.delete(`/admin/ingredients/${id}`)
}
