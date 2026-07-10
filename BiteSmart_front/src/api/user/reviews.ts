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

export const createReview = (orderId: number, data: Review): Promise<any> => {
  return request.post('/reviews', { params: { orderId }, data })
}

export const getMyReviews = (params?: { page?: number; size?: number }): Promise<any> => {
  return request.get('/reviews/my', { params })
}
