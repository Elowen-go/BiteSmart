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
    vehicleType: string
  }
}

export const getDeliveryTracking = (orderId: number): Promise<any> => {
  return request.get(`/delivery/tracking/${orderId}`)
}
