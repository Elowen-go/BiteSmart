import request from '../../utils/request'

export interface Notice {
  id: number
  title: string
  content: string
  type: string
  status: number
  priority: number
  createTime: string
  updateTime: string
}

export const listNotices = (pageNum: number, pageSize: number): Promise<any> => {
  return request.get('/api/admin/notices', { params: { pageNum, pageSize } })
}

export const getNoticeDetail = (id: number): Promise<any> => {
  return request.get(`/api/admin/notices/${id}`)
}

export const addNotice = (data: Partial<Notice>): Promise<any> => {
  return request.post('/api/admin/notices', data)
}

export const updateNotice = (id: number, data: Partial<Notice>): Promise<any> => {
  return request.put(`/api/admin/notices/${id}`, data)
}

export const deleteNotice = (id: number): Promise<any> => {
  return request.delete(`/api/admin/notices/${id}`)
}
