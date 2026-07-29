import { request } from '../utils/request'

export interface DriverFeedbackPayload {
  orderId: number | string
  category: string
  content: string
}

/** 骑手端独立反馈接口，不复用用户投诉接口。 */
export const submitDriverFeedback = (payload: DriverFeedbackPayload): Promise<void> =>
  request<void>({ url: '/driver/feedback', method: 'POST', data: payload })
