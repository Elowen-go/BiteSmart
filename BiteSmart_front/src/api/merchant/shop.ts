import request from '../../utils/request'

export const uploadFile = (file: File, bizType: string): Promise<any> => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('bizType', bizType)
  return request.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export interface MerchantShop {
  id: number
  userId: number
  shopName: string
  shopLogo?: string
  businessLicense?: string
  licenseNumber?: string
  contactName?: string
  contactPhone?: string
  shopAddress?: string
  deliveryRange?: string
  businessHours?: string
  shopNotice?: string
  status?: number
  auditRemark?: string
  avgRating?: number
  createTime: string
  updateTime: string
}

export const getShopInfo = (): Promise<any> => {
  return request.get('/merchant/shop')
}

export const updateShopInfo = (data: MerchantShop): Promise<any> => {
  return request.put('/merchant/shop', data)
}

/**
 * 获取当前用户的所有店铺列表（支持多店铺）
 */
export const getShopList = (): Promise<any> => {
  return request.get('/merchant/shop/list')
}
