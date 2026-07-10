import request from '../../utils/request'

export interface User {
  id: number
  username: string
  nickname: string
  phone: string
  email: string
  roleType: number
  status: number
  createTime: string
  updateTime: string
}

export const getUserList = (params?: { pageNum?: number; pageSize?: number }): Promise<any> => {
  return request.get('/admin/users', { params })
}

export const getUserDetail = (id: number): Promise<any> => {
  return request.get(`/admin/users/${id}`)
}

export const updateUserStatus = (id: number, status: number): Promise<any> => {
  return request.put(`/admin/users/${id}/status`, { params: { status } })
}
