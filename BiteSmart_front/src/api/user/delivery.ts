import request from '../../utils/request'

export interface DeliveryTracking {
  taskStatus: number
  pickupCode: string
  estimatedDeliveryTime: string
  pickupTime: string
  deliverTime: string
  currentLat: number
  currentLng: number
  driver?: {
    realName: string
    phone: string
    currentLat: number
    currentLng: number
    /** 车辆类型：10-电动车 20-自行车 30-汽车 */
    vehicleType: number
  }
}

export const getDeliveryTracking = (orderId: number | string): Promise<any> => {
  return request.get(`/delivery/tracking/${orderId}`)
}
