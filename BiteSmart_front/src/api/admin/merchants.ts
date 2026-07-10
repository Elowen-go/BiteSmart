import request from '../../utils/request'

export interface Merchant {
  id: number
  userId: number
  shopName: string
  shopDesc: string
  logoUrl: string
  phone: string
  address: string
  businessLicense: string
  auditStatus: number
  auditTime: string
  auditRemark: string
  createTime: string
  updateTime: string
}

export const getMerchantList = (params?: { pageNum?: number; pageSize?: number }): Promise<any> => {
  return request.get('/admin/merchants', { params })
}

export const getMerchantDetail = (id: number): Promise<any> => {
  return request.get(`/admin/merchants/${id}`)
}

export const auditMerchant = (id: number, auditStatus: number, remark?: string): Promise<any> => {
  return request.put(`/admin/merchants/${id}/audit`, { params: { auditStatus, remark } })
}

export const closeMerchant = (id: number): Promise<any> => {
  return request.put(`/admin/merchants/${id}/close`)
}
