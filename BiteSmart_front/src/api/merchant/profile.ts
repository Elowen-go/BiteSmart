import request from '../../utils/request'

export interface MerchantProfile {
  id: number
  username: string
  nickname: string
  avatar: string
  phone: string
  email: string
  roleType: number
}

export const getProfile = (): Promise<any> => {
  return request.get('/users/me')
}

export const updateProfile = (data: Partial<MerchantProfile>): Promise<any> => {
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