import { request } from '../utils/request'

/**
 * 健康三记录 API —— 字段对齐后端 bitesmart.sql：
 * - diet_record.mealType 为 int 枚举：10 早餐 / 20 午餐 / 30 晚餐 / 40 加餐；碳水字段名 carbs（非 carbohydrate）
 * - exercise_record 热量字段名 caloriesBurned；exerciseType 为自由文本
 * - weight_record 体脂字段名 bodyFatRate；每日一条，POST 即 upsert
 */
export interface DietRecord {
  id?: number | string
  recordDate?: string
  recordTime?: string
  mealType?: number
  foodName?: string
  quantity?: number
  calories?: number
  protein?: number
  fat?: number
  carbs?: number
  sourceType?: number
  orderItemId?: number | string
}

export interface ExerciseRecord {
  id?: number | string
  recordDate?: string
  exerciseType?: string
  duration?: number
  distance?: number
  caloriesBurned?: number
  remark?: string
}

export interface WeightRecord {
  id?: number | string
  recordDate?: string
  weight?: number
  bodyFatRate?: number
  bmi?: number
}

export const getDietRecords = (date?: string): Promise<DietRecord[]> =>
  request<DietRecord[]>({ url: '/health/diet', data: date ? { date } : {} })

export const addDietRecord = (record: DietRecord): Promise<void> =>
  request<void>({ url: '/health/diet', method: 'POST', data: record })

/** id 为雪花 Long，传输层是字符串，禁止 Number() 强转 */
export const deleteDietRecord = (id: number | string): Promise<void> =>
  request<void>({ url: `/health/diet/${id}`, method: 'DELETE' })

export const getExerciseRecords = (date?: string): Promise<ExerciseRecord[]> =>
  request<ExerciseRecord[]>({ url: '/health/exercise', data: date ? { date } : {} })

export const addExerciseRecord = (record: ExerciseRecord): Promise<void> =>
  request<void>({ url: '/health/exercise', method: 'POST', data: record })

export const deleteExerciseRecord = (id: number | string): Promise<void> =>
  request<void>({ url: `/health/exercise/${id}`, method: 'DELETE' })

/** 不传 page 走后端 List 分支返回数组；传 page 走 PageResultVO 分页分支，列表字段为 list（非 records） */
export const getWeightRecords = (page?: number, size?: number): Promise<WeightRecord[] | { list?: WeightRecord[]; total?: number }> =>
  request<WeightRecord[] | { list?: WeightRecord[]; total?: number }>({ url: '/health/weight', data: page ? { page, size: size || 10 } : {} })

export const saveWeightRecord = (record: WeightRecord): Promise<void> =>
  request<void>({ url: '/health/weight', method: 'POST', data: record })

/* ---------- 运动库（GET /exercise/library，B 类） ---------- */
/** 运动库项目（后端 ExerciseLibrary）：tags 为 JSON 数组字符串；id 传输层为 string（Long 序列化防精度丢失） */
export interface ExerciseLibraryItem {
  id?: number | string
  category?: string // aerobic 有氧 / strength 力量 / shape 塑形 / yoga 瑜伽
  name?: string
  stdText?: string
  kcalPerMin?: number
  defMins?: number
  defDist?: number
  imageUrl?: string
  tags?: string
  hot?: number // 0 / 1
  sort?: number
}

export const getExerciseLibrary = (): Promise<ExerciseLibraryItem[]> =>
  request<ExerciseLibraryItem[]>({ url: '/exercise/library' })

/* ---------- mealType int 枚举映射（页面通用） ---------- */
export const MEAL_TYPES = [10, 20, 30, 40] as const
export const MEAL_NAME: Record<number, string> = { 10: '早餐', 20: '午餐', 30: '晚餐', 40: '加餐' }
export const MEAL_EN: Record<number, string> = { 10: 'BREAKFAST', 20: 'LUNCH', 30: 'DINNER', 40: 'SNACK' }
export const mealTypeOf = (name: string): number => {
  for (const t of MEAL_TYPES) if (MEAL_NAME[t] === name) return t
  return 40
}
