import request from '../../utils/request'

// 与后端 entity/health/DietRecord 对齐（Long id 序列化为 string）
export interface DietRecord {
  id: number | string
  userId: number | string
  recordDate: string
  recordTime: string
  /** 餐次：10-早餐 20-午餐 30-晚餐 40-加餐 */
  mealType: number
  foodName: string
  quantity: number
  calories: number
  protein: number
  fat: number
  carbs: number
  /** 来源：10-平台订单自动 20-用户手动添加 30-专属计划打卡 */
  sourceType: number
  orderItemId?: number | string
  planMealId?: number | string
  createTime: string
  updateTime?: string
}

// 与后端 entity/health/ExerciseRecord 对齐
export interface ExerciseRecord {
  id: number | string
  userId: number | string
  recordDate: string
  exerciseType: string
  duration: number
  distance: number
  caloriesBurned: number
  remark: string
  createTime: string
  updateTime?: string
}

export interface WeightRecord {
  id: number | string
  userId: number | string
  weight: number
  bodyFatRate?: number
  bmi?: number
  recordDate: string
  createTime: string
}

export const getDietRecords = (date?: string): Promise<any> => {
  return request.get('/health/diet', { params: { date } })
}

export const addDietRecord = (data: DietRecord): Promise<any> => {
  return request.post('/health/diet', data)
}

export const updateDietRecord = (id: number | string, data: Partial<DietRecord>): Promise<any> => {
  return request.put(`/health/diet/${id}`, data)
}

export const deleteDietRecord = (id: number | string): Promise<any> => {
  return request.delete(`/health/diet/${id}`)
}

export const getExerciseRecords = (date?: string): Promise<any> => {
  return request.get('/health/exercise', { params: { date } })
}

export const addExerciseRecord = (data: ExerciseRecord): Promise<any> => {
  return request.post('/health/exercise', data)
}

export const deleteExerciseRecord = (id: number | string): Promise<any> => {
  return request.delete(`/health/exercise/${id}`)
}

export const getWeightRecords = (params?: { page?: number; size?: number }): Promise<any> => {
  return request.get('/health/weight', { params })
}

export const saveWeightRecord = (data: WeightRecord): Promise<any> => {
  return request.post('/health/weight', data)
}
