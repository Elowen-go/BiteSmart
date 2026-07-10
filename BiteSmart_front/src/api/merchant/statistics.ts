import request from '../../utils/request'

export interface TodayStats {
  orderCount: number
  revenue: number
  newUserCount: number
  avgOrderAmount: number
}

export interface PeriodStats {
  date: string
  orderCount: number
  revenue: number
}

export interface TopDish {
  dishId: number
  dishName: string
  soldCount: number
  revenue: number
}

export const getTodayStats = (): Promise<any> => {
  return request.get('/merchant/statistics/today')
}

export const getPeriodStats = (params?: { startDate?: string; endDate?: string }): Promise<any> => {
  return request.get('/merchant/statistics/period', { params })
}

export const getTopDishes = (params?: { limit?: number }): Promise<any> => {
  return request.get('/merchant/statistics/top-dishes', { params })
}
