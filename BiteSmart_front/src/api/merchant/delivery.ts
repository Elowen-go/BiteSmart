import request from '../../utils/request'

export interface DeliveryTask {
  id: number
  orderId: number
  orderNo: string
  driverId: number
  driverName: string
  driverPhone: string
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

export const getDeliveryTaskByOrderId = (orderId: number): Promise<any> => {
  return request.get(`/merchant/delivery/tasks/${orderId}`)
}
