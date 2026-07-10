import request from '../../utils/request'

export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar: string
  phone: string
  email: string
  roleType: number
  status: number
}

export interface UserProfile {
  id: number
  userId: number
  age: number
  gender: number
  height: number
  weight: number
  activityLevel: number
  dietaryRestrictions: string
  healthGoals: string
  createTime: string
  updateTime: string
}

export const getCurrentUser = (): Promise<any> => {
  return request.get('/users/me')
}

export const getProfile = (): Promise<any> => {
  return request.get('/user/profile')
}

export const saveProfile = (data: UserProfile): Promise<any> => {
  return request.put('/user/profile', data)
}
