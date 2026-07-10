import request from '../../utils/request'

export interface MembershipPlan {
  id: number
  name: string
  description: string
  price: number
  durationDays: number
  status: number
  createTime: string
}

export interface UserMembership {
  id: number
  userId: number
  planId: number
  planName: string
  startTime: string
  endTime: string
  status: number
  createTime: string
}

export const getMembershipPlans = (): Promise<any> => {
  return request.get('/user/membership/plans')
}

export const getMembershipStatus = (): Promise<any> => {
  return request.get('/user/membership/status')
}

export const buyMembership = (planId: number): Promise<any> => {
  return request.post(`/user/membership/buy/${planId}`)
}

export const getMembershipHistory = (): Promise<any> => {
  return request.get('/user/membership/history')
}
