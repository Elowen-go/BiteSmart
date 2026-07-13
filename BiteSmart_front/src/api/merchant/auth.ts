import request from '../../utils/request'

export interface MerchantApplyForm {
  shopName: string
  shopLogo?: string
  businessLicense?: string
  licenseNumber?: string
  contactName?: string
  contactPhone: string
  shopAddress: string
  deliveryRange?: string
  businessHours?: string
  shopNotice?: string
}

export interface MerchantAuditLog {
  id: number
  merchantId: number
  auditStatus: number
  auditRemark?: string
  submitTime?: string
  auditTime?: string
}

export const applyMerchant = (data: MerchantApplyForm): Promise<any> => {
  return request.post('/merchant/auth/apply', data)
}

export const getMerchantApplyStatus = (): Promise<any> => {
  return request.get('/merchant/auth/status')
}

export const getMerchantAuditLog = (): Promise<any> => {
  return request.get('/merchant/auth/audit-log')
}
