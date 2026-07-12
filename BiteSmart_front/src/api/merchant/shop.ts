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
