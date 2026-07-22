import request from '../../utils/request'

export interface UserInfo {
  id: number | string
  username: string
  nickname: string
  avatar: string
  phone: string
  email: string
  roleType: number
  status: number
}

// 与后端 entity/user/UserProfile 对齐（含 20260718 迁移新增列）
export interface UserProfile {
  id: number | string
  userId: number | string
  age: number
  /** 性别：10-男 20-女 */
  gender: number
  height: number
  weight: number
  /** 运动量等级：10-久坐 20-轻度 30-中度 40-重度 */
  activityLevel: number
  /** 饮食偏好 JSON 字符串，如 ["少盐","少油"] */
  dietPreference: string
  /** 过敏史 JSON 字符串 */
  allergyInfo: string
  /** 疾病史 JSON 字符串 */
  diseaseHistory: string
  /** 健康目标：减肥/增肌/维持/控糖/其他 */
  healthGoal: string
  dailyCalorieTarget?: number
  targetWeight?: number
  exerciseFreq?: number
  /** 重点锻炼部位 JSON 数组字符串 */
  focusParts?: string
  createTime: string
  updateTime: string
}

export const getCurrentUser = (): Promise<any> => {
  return request.get('/users/me')
}

export const updateCurrentUser = (data: { username?: string; nickname?: string; avatar?: string }): Promise<any> => {
  return request.put('/users/me', data)
}

export const uploadFile = (file: File, bizType: string): Promise<any> => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('bizType', bizType)
  return request.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const getProfile = (): Promise<any> => {
  return request.get('/user/profile')
}

export const saveProfile = (data: Partial<UserProfile>): Promise<any> => {
  return request.put('/user/profile', data)
}
