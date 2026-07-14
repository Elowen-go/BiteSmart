import request from '../../utils/request'

export interface DietRecord {
  id: number
  userId: number
  recordTime: string
  mealType: string
  dishIds: string
  quantity: number
  totalCalories: number
  totalProtein: number
  totalFat: number
  totalCarbs: number
  recordDate: string
  notes: string
  createTime: string
}

export interface ExerciseRecord {
  id: number
  userId: number
  exerciseType: string
  duration: number
  caloriesBurned: number
  recordDate: string
  notes: string
  createTime: string
}

export interface WeightRecord {
  id: number
  userId: number
  weight: number
  recordDate: string
  createTime: string
}

export const getDietRecords = (date?: string): Promise<any> => {
  return request.get('/health/diet', { params: { date } })
}

export const addDietRecord = (data: DietRecord): Promise<any> => {
  return request.post('/health/diet', data)
}

export const updateDietRecord = (id: number, data: DietRecord): Promise<any> => {
  return request.put(`/health/diet/${id}`, data)
}

export const deleteDietRecord = (id: number): Promise<any> => {
  return request.delete(`/health/diet/${id}`)
}

export const getExerciseRecords = (date?: string): Promise<any> => {
  return request.get('/health/exercise', { params: { date } })
}

export const addExerciseRecord = (data: ExerciseRecord): Promise<any> => {
  return request.post('/health/exercise', data)
}

export const deleteExerciseRecord = (id: number): Promise<any> => {
  return request.delete(`/health/exercise/${id}`)
}

export const getWeightRecords = (params?: { page?: number; size?: number }): Promise<any> => {
  return request.get('/health/weight', { params })
}

export const saveWeightRecord = (data: WeightRecord): Promise<any> => {
  return request.post('/health/weight', data)
}
