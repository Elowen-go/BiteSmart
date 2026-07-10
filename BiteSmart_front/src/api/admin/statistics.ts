import request from '../../utils/request'

export interface DashboardStats {
  totalSales: number
  orderCount: number
  activeUsers: number
  aiCalls: number
  salesChange: number
  orderChange: number
  userChange: number
  aiChange: number
}

export const getDashboardStats = (): Promise<{ code: number; data: DashboardStats }> => {
  return request.get('/admin/statistics/dashboard')
}

export const getRecentOrders = (params?: { page?: number; size?: number }): Promise<any> => {
  return request.get('/admin/statistics/recent-orders', { params })
}