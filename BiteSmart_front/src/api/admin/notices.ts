import request from '../../utils/request'

export interface Notice {
  id: number
  title: string
  content: string
  noticeType: number
  targetRole: number
  priority: number
  status: number
  publishTime: string
  expireTime: string
  createTime: string
  updateTime: string
}

export const listNotices = (pageNum: number, pageSize: number): Promise<any> => {
  return request.get('/admin/notices', { params: { pageNum, pageSize } })
}

export const getNoticeDetail = (id: number): Promise<any> => {
  return request.get(`/admin/notices/${id}`)
}

export const addNotice = (data: Partial<Notice>): Promise<any> => {
  return request.post('/admin/notices', data)
}

export const updateNotice = (id: number, data: Partial<Notice>): Promise<any> => {
  return request.put(`/admin/notices/${id}`, data)
}

export const deleteNotice = (id: number): Promise<any> => {
  return request.delete(`/admin/notices/${id}`)
}