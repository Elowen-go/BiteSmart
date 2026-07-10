import request from '../../utils/request'

export interface MerchantReview {
  id: number
  orderId: number
  userId: number
  username: string
  rating: number
  content: string
  images: string
  status: number
  replyContent: string
  replyTime: string
  createTime: string
}

export const getReviewList = (params?: { page?: number; size?: number }): Promise<any> => {
  return request.get('/merchant/reviews', { params })
}

export const replyReview = (id: number, content: string): Promise<any> => {
  return request.post(`/merchant/reviews/${id}/reply`, { params: { content } })
}
