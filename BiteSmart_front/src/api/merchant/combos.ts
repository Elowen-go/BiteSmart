import request from '../../utils/request'

export interface Combo {
  id: number
  merchantId: number
  comboName: string
  description: string
  price: number
  originalPrice: number
  comboImage: string
  comboType: number
  suitableFor: string
  totalCalories: number
  totalProtein: number
  totalFat: number
  totalCarbs: number
  replaceableDishPool: string
  maxReplaceCount: number
  status: number
  salesCount: number
  createTime: string
  updateTime: string
}

/** 套餐关联菜品项 */
export interface DishItem {
  dishId: number
  quantity: number
  /** 是否固定不可替换：1-固定 0-可替换 */
  isFixed: number
}

export interface ComboRequest {
  combo: Partial<Combo>
  dishItems: DishItem[]
}

export interface ComboDetailVO {
  combo: Combo
  dishItems: DishItem[]
}

export const getComboList = (params?: { page?: number; size?: number }): Promise<any> => {
  return request.get('/merchant/combos', { params })
}

export const getComboDetail = (id: number): Promise<any> => {
  return request.get(`/merchant/combos/${id}`)
}

export const addCombo = (data: ComboRequest): Promise<any> => {
  return request.post('/merchant/combos', data)
}

export const updateCombo = (id: number, data: ComboRequest): Promise<any> => {
  return request.put(`/merchant/combos/${id}`, data)
}

export const deleteCombo = (id: number): Promise<any> => {
  return request.delete(`/merchant/combos/${id}`)
}

// 套餐类型选项
export const comboTypeOptions = [
  { value: 10, label: '减脂' },
  { value: 20, label: '增肌' },
  { value: 30, label: '控糖' },
  { value: 40, label: '会员专属' }
]

// 适宜人群选项
export const suitableForOptions = [
  '减脂人群',
  '增肌人群',
  '控糖人群',
  '健身爱好者',
  '上班族',
  '学生党',
  '老年人',
  '孕妇'
]
