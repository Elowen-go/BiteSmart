import request from '../../utils/request'

export interface DashboardStats {
  userCount: number
  merchantCount: number
  orderCount: number
  totalRevenue: number
}

export interface TrendStats {
  type: string
  startTime: string
  endTime: string
  orderCount: number
  revenue: number
}

export const getOverview = (): Promise<{ code: number; data: DashboardStats }> => {
  return request.get('/admin/statistics/overview')
}

export const getTrend = (type?: 'day' | 'week' | 'month'): Promise<{ code: number; data: TrendStats }> => {
  return request.get('/admin/statistics/trend', { params: { type } })
}