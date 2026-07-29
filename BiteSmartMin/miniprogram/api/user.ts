import { request } from '../utils/request'

/** 健康档案（后端 user_profile 表字段）：dietPreference / allergyInfo / diseaseHistory 均为 JSON 字符串列 */
export interface UserProfile {
  id?: number
  age?: number
  gender?: number // 10 男 / 20 女
  height?: number
  weight?: number
  activityLevel?: number // 10 久坐 / 20 轻度 / 30 中度 / 40 重度
  dietPreference?: string
  allergyInfo?: string
  diseaseHistory?: string
  healthGoal?: string
  dailyCalorieTarget?: number
  targetWeight?: number // 目标体重 kg（B 类）
  exerciseFreq?: number // 每周运动频次，次（B 类）
  focusParts?: string // 重点部位 JSON 数组字符串（B 类）
}

export const getProfile = (): Promise<UserProfile | null> => request<UserProfile | null>({ url: '/user/profile' })
export const saveProfile = (profile: UserProfile): Promise<void> => request<void>({ url: '/user/profile', method: 'PUT', data: profile })

/** 地址 id 为雪花 Long，传输层是字符串，禁止 Number() 强转 */
export interface UserAddress { id?: number | string; receiverName?: string; receiverPhone?: string; province?: string; city?: string; district?: string; detailAddress?: string; locationName?: string; latitude?: number; longitude?: number; addressTag?: string; isDefault?: number }
export const getAddresses = (): Promise<UserAddress[]> => request<UserAddress[]>({ url: '/user/addresses' })
export const addAddress = (address: UserAddress): Promise<void> => request<void>({ url: '/user/addresses', method: 'POST', data: address })
export const updateAddress = (id: number | string, address: UserAddress): Promise<void> => request<void>({ url: `/user/addresses/${id}`, method: 'PUT', data: address })
export const deleteAddress = (id: number | string): Promise<void> => request<void>({ url: `/user/addresses/${id}`, method: 'DELETE' })

export interface MembershipPlan { id: number | string; planName?: string; price?: number; originalPrice?: number; validDays?: number; benefits?: string }
export const getMembershipPlans = (): Promise<MembershipPlan[]> => request<MembershipPlan[]>({ url: '/user/membership/plans' })

/** 会员状态（后端 UserMembership）：membershipType 10月卡/20季卡/30年卡；status 10生效中/20已过期/30已退款；id 为雪花 Long，传输层是字符串 */
export interface UserMembership {
  id?: number | string
  planId?: number | string
  membershipType?: number
  status?: number
  startTime?: string
  endTime?: string
  orderId?: number | string
  payAmount?: number
}
export const getMembershipStatus = (): Promise<UserMembership | null> => request<UserMembership | null>({ url: '/user/membership/status' })
export const buyMembership = (planId: number | string): Promise<Record<string, unknown>> => request<Record<string, unknown>>({ url: `/user/membership/buy/${planId}`, method: 'POST' })
