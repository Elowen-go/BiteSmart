import { request } from '../utils/request'

export interface DietRecord { id?: number; recordDate?: string; mealType?: string; foodName?: string; calories?: number; protein?: number; fat?: number; carbohydrate?: number }
export interface ExerciseRecord { id?: number; recordDate?: string; exerciseType?: string; duration?: number; distance?: number; calories?: number }
export interface WeightRecord { id?: number; recordDate?: string; weight?: number; bodyFat?: number }
export const getDietRecords = (date?: string): Promise<DietRecord[]> => request<DietRecord[]>({ url: '/health/diet', data: date ? { date } : {} })
export const addDietRecord = (record: DietRecord): Promise<void> => request<void>({ url: '/health/diet', method: 'POST', data: record })
export const getExerciseRecords = (date?: string): Promise<ExerciseRecord[]> => request<ExerciseRecord[]>({ url: '/health/exercise', data: date ? { date } : {} })
export const addExerciseRecord = (record: ExerciseRecord): Promise<void> => request<void>({ url: '/health/exercise', method: 'POST', data: record })
export const getWeightRecords = (): Promise<WeightRecord[] | { records: WeightRecord[] }> => request<WeightRecord[] | { records: WeightRecord[] }>({ url: '/health/weight' })
export const saveWeightRecord = (record: WeightRecord): Promise<void> => request<void>({ url: '/health/weight', method: 'POST', data: record })
