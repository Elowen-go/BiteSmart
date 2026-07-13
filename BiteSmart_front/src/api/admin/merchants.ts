import request from '../../utils/request'

export interface Merchant {
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

export const getMerchantList = (params?: { pageNum?: number; pageSize?: number; status?: number }): Promise<any> => {
  return request.get('/admin/merchants', { params })
}

export const getMerchantDetail = (id: number): Promise<any> => {
  return request.get(`/admin/merchants/${id}`)
}

export const auditMerchant = (id: number, status: number, auditRemark?: string): Promise<any> => {
  return request.put(`/admin/merchants/${id}/audit`, null, { params: { status, auditRemark } })
}

export const closeMerchant = (id: number, reason: string): Promise<any> => {
  return request.put(`/admin/merchants/${id}/close`, null, { params: { reason } })
}
