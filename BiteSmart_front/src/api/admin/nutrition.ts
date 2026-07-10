import request from '../../utils/request'

export interface NutritionStandard {
  id: number
  standardName: string
  description: string
  standardType: string
  minValue: number
  maxValue: number
  unit: string
  priority: number
  createTime: string
}

export const listNutrition = (): Promise<any> => {
  return request.get('/api/admin/nutrition')
}

export const addNutrition = (data: Partial<NutritionStandard>): Promise<any> => {
  return request.post('/api/admin/nutrition', data)
}

export const updateNutrition = (id: number, data: Partial<NutritionStandard>): Promise<any> => {
  return request.put(`/api/admin/nutrition/${id}`, data)
}

export const deleteNutrition = (id: number): Promise<any> => {
  return request.delete(`/api/admin/nutrition/${id}`)
}
