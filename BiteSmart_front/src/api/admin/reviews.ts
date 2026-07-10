import request from '../../utils/request'

export interface Review {
  id: number
  orderId: number
  userId: number
  merchantId: number
  rating: number
  content: string
  images: string
  status: number
  replyContent: string
  replyTime: string
  createTime: string
}

export const getReviewList = (params?: { pageNum?: number; pageSize?: number }): Promise<any> => {
  return request.get('/admin/reviews', { params })
}

export const updateReviewStatus = (id: number, status: number): Promise<any> => {
  return request.put(`/admin/reviews/${id}/status`, { params: { status } })
}
