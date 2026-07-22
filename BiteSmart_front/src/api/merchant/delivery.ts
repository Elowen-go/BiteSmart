import request from '../../utils/request'

export interface DeliveryTask {
  id: number | string
  orderId: number | string
  orderNo: string
  driverId: number | string
  /** 后端联表 delivery_driver 带出，可能为空，视图需兜底显示 */
  driverName?: string
  driverPhone?: string
  taskStatus: number
  pickupCode: string
  estimatedDeliveryTime: string
  pickupTime: string
  deliverTime: string
  currentLat: number
  currentLng: number
  createTime: string
  updateTime: string
}

export const getDeliveryTasks = (): Promise<any> => {
  return request.get('/merchant/delivery/tasks')
}

export const getDeliveryTaskByOrderId = (orderId: number | string): Promise<any> => {
  return request.get(`/merchant/delivery/tasks/${orderId}`)
}
