import { request } from '../utils/request'

export interface ReviewPayload { ratingFood?: number; ratingDelivery?: number; ratingService?: number; overallRating?: number; content?: string; isAnonymous?: number; images?: string }
/** orderId 为雪花 Long，传输层是字符串，原样透传禁止 Number() 强转 */
export const createReview = (orderId: number | string, review: ReviewPayload): Promise<number> => request<number>({ url: `/reviews?orderId=${orderId}`, method: 'POST', data: review })
export interface ComplaintPayload { orderId: number | string; targetType: number; targetId: number | string; complaintReason: string; complaintDesc?: string; evidenceImages?: string }
export const createComplaint = (payload: ComplaintPayload): Promise<void> => request<void>({ url: '/complaints', method: 'POST', data: payload })
