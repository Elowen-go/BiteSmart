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
  id: number | string
  userId: number | string
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
  /** 营业状态：10-营业中 20-打烊 */
  openStatus?: number
  auditRemark?: string
  avgRating?: number
  createTime: string
  updateTime: string
}

export const getShopInfo = (): Promise<any> => {
  return request.get('/merchant/shop')
}

export const updateShopInfo = (data: Partial<MerchantShop>): Promise<any> => {
  return request.put('/merchant/shop', data)
}

/**
 * 切换营业状态（10-营业中 20-打烊）
 * 后端动态 update，只提交 openStatus 不会清掉其他字段
 */
export const updateShopOpenStatus = (openStatus: number): Promise<any> => {
  return request.put('/merchant/shop', { openStatus })
}

/**
 * 获取当前用户的所有店铺列表（支持多店铺）
 */
export const getShopList = (): Promise<any> => {
  return request.get('/merchant/shop/list')
}
