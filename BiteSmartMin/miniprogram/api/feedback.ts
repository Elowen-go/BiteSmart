import { request } from '../utils/request'

export interface ReviewPayload { ratingFood?: number; ratingDelivery?: number; ratingService?: number; overallRating?: number; content?: string; isAnonymous?: number; images?: string }
export const createReview = (orderId: number, review: ReviewPayload): Promise<number> => request<number>({ url: `/reviews?orderId=${orderId}`, method: 'POST', data: review })
export interface ComplaintPayload { orderId: number; targetType: number; targetId: number; complaintReason: string; complaintDesc?: string; evidenceImages?: string }
export const createComplaint = (payload: ComplaintPayload): Promise<void> => request<void>({ url: '/complaints', method: 'POST', data: payload })
