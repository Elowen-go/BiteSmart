import request from '../../utils/request'

// 与后端 entity/user/MembershipPlan 对齐（Long id 序列化为 string）
export interface MembershipPlan {
  id: number | string
  /** 套餐名称（月卡/季卡/年卡） */
  planName: string
  /** 套餐类型：10-月卡 20-季卡 30-年卡 */
  planType: number
  price: number
  originalPrice?: number
  /** 有效天数（月卡30天） */
  validDays: number
  /** 权益描述 JSON 字符串，如 {"ai_advanced":true,"discount":0.8} */
  benefits: string
  /** 状态：10-上架 20-下架 */
  status: number
  sortOrder?: number
  createTime: string
}

// 与后端 entity/user/UserMembership 对齐（实体无 planName，视图按 membershipType 映射）
export interface UserMembership {
  id: number | string
  userId: number | string
  planId: number | string
  /** 会员类型：10-月卡 20-季卡 30-年卡 */
  membershipType: number
  /** 状态：10-生效中 20-已过期 30-已退款 */
  status: number
  startTime: string
  endTime: string
  orderId?: number | string
  payAmount?: number
  createTime: string
}

export const getMembershipPlans = (): Promise<any> => {
  return request.get('/user/membership/plans')
}

export const getMembershipStatus = (): Promise<any> => {
  return request.get('/user/membership/status')
}

export const buyMembership = (planId: number | string): Promise<any> => {
  return request.post(`/user/membership/buy/${planId}`)
}

export const getMembershipHistory = (): Promise<any> => {
  return request.get('/user/membership/history')
}
