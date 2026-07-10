import request from '../../utils/request'

export interface InventoryWarning {
  dishId: number
  dishName: string
  currentStock: number
  minStock: number
  imageUrl: string
}

export interface InventoryLog {
  id: number
  dishId: number
  dishName: string
  changeType: string
  changeAmount: number
  beforeStock: number
  afterStock: number
  operator: string
  createTime: string
}

export const getInventoryWarnings = (): Promise<any> => {
  return request.get('/merchant/inventory/warnings')
}

export const getInventoryLogs = (): Promise<any> => {
  return request.get('/merchant/inventory/logs')
}
