import { request } from '../utils/request'

export interface UserProfile { id?: number; height?: number; weight?: number; targetWeight?: number; healthGoal?: string; dietPreference?: string; allergies?: string; exerciseFrequency?: string }
export const getProfile = (): Promise<UserProfile | null> => request<UserProfile | null>({ url: '/user/profile' })
export const saveProfile = (profile: UserProfile): Promise<void> => request<void>({ url: '/user/profile', method: 'PUT', data: profile })

export interface UserAddress { id?: number; receiverName?: string; receiverPhone?: string; province?: string; city?: string; district?: string; detailAddress?: string; addressTag?: string; isDefault?: number }
export const getAddresses = (): Promise<UserAddress[]> => request<UserAddress[]>({ url: '/user/addresses' })
export const addAddress = (address: UserAddress): Promise<void> => request<void>({ url: '/user/addresses', method: 'POST', data: address })

export interface MembershipPlan { id: number; planName?: string; price?: number; originalPrice?: number; validDays?: number; benefits?: string }
export const getMembershipPlans = (): Promise<MembershipPlan[]> => request<MembershipPlan[]>({ url: '/user/membership/plans' })
export const getMembershipStatus = (): Promise<Record<string, unknown> | null> => request<Record<string, unknown> | null>({ url: '/user/membership/status' })
export const buyMembership = (planId: number): Promise<Record<string, unknown>> => request<Record<string, unknown>>({ url: `/user/membership/buy/${planId}`, method: 'POST' })
