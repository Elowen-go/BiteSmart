import { request } from '../utils/request'

/** 菜品（后端 Dish 字段）：碳水字段名 carbs；suitableFor 为 JSON 字符串；id 为雪花 Long，传输层是字符串 */
export interface Dish {
  id?: number | string
  merchantId?: number | string
  categoryId?: number | string
  dishName?: string
  dishImage?: string
  price?: number
  originalPrice?: number
  stock?: number
  salesCount?: number
  unit?: string
  description?: string
  suitableFor?: string
  tags?: string // JSON 数组字符串（B 类）
  aiComment?: string // AI 点评文案（B 类）
  fitScenes?: string // 适用场景 JSON 数组字符串（B 类）
  cautions?: string // 注意事项 JSON 数组字符串（B 类）
  calories?: number
  protein?: number
  fat?: number
  carbs?: number
  status?: number
}

/** 套餐（后端 Combo 字段）：营养为整套餐汇总 totalCalories/totalProtein/totalFat/totalCarbs；id 为雪花 Long，传输层是字符串 */
export interface Combo {
  id?: number | string
  merchantId?: number | string
  comboName?: string
  comboImage?: string
  price?: number
  originalPrice?: number
  comboType?: number
  suitableFor?: string
  totalCalories?: number
  totalProtein?: number
  totalFat?: number
  totalCarbs?: number
  description?: string
  status?: number
  salesCount?: number
}

export interface ComboDishRel { dishId?: number | string; quantity?: number; isFixed?: number }
export interface ComboDetail { combo?: Combo; dishRels?: ComboDishRel[] }

export const getDishes = (params: Record<string, unknown> = {}): Promise<Dish[] | { records: Dish[] }> => request<Dish[] | { records: Dish[] }>({ url: '/dishes', data: params, needAuth: false })
export const getDish = (id: number | string): Promise<Dish> => request<Dish>({ url: `/dishes/${id}`, needAuth: false })
export const getCombos = (params: Record<string, unknown> = {}): Promise<Combo[]> => request<Combo[]>({ url: '/combos', data: params, needAuth: false })
export const getCombo = (id: number | string): Promise<ComboDetail> => request<ComboDetail>({ url: `/combos/${id}`, needAuth: false })
