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

export const getNoticeList = (params?: { pageNum?: number; pageSize?: number }): Promise<any> => {
  return request.get('/notices', { params })
}

export const getNoticeDetail = (id: number): Promise<any> => {
  return request.get(`/notices/${id}`)
}