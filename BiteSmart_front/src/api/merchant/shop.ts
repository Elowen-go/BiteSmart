import request from '../../utils/request'

export interface MerchantShop {
  id: number
  userId: number
  shopName: string
  shopDesc: string
  logoUrl: string
  phone: string
  address: string
  businessLicense: string
  auditStatus: number
  createTime: string
  updateTime: string
}

export const getShopInfo = (): Promise<any> => {
  return request.get('/merchant/shop')
}

export const updateShopInfo = (data: MerchantShop): Promise<any> => {
  return request.put('/merchant/shop', data)
}
