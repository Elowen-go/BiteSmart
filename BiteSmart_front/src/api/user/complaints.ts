import request from '../../utils/request'

export const createComplaint = (data: { orderId: number; targetType: number; targetId: number; complaintReason: string; complaintDesc?: string }) =>
  request.post('/complaints', data)
