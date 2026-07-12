import request from '../../utils/request'

/**
 * 商家端食材查询 API
 * 
 * 提供商家查询平台预设食材的功能
 * 商家只能查询，不能修改食材库
 */

export interface Ingredient {
  id: number
  name: string
  categoryName: string
  calories: number
  protein: number
  fat: number
  carbs: number
}

/**
 * 获取所有启用的食材列表
 * @returns 食材列表
 */
export const getMerchantIngredientList = (): Promise<any> => {
  return request.get('/merchant/ingredients')
}

/**
 * 按分类查询食材列表
 * @param category 分类名称，如：肉类、蔬菜、主食等
 * @returns 该分类下的食材列表
 */
export const getMerchantIngredientsByCategory = (category: string): Promise<any> => {
  return request.get('/merchant/ingredients/by-category', { params: { category } })
}

/**
 * 获取所有分类名称
 * @returns 分类名称列表
 */
export const getMerchantIngredientCategories = (): Promise<any> => {
  return request.get('/merchant/ingredients/categories')
}
