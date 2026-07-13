import request from '../../utils/request'

export const getRefundList = (params?: { pageNum?: number; pageSize?: number; status?: number }): Promise<any> =>
  request.get('/admin/work-orders/refunds', { params })

export const auditRefund = (id: number, status: number, remark?: string): Promise<any> =>
  request.put(`/admin/work-orders/refunds/${id}`, null, { params: { status, remark } })

export const getComplaintList = (params?: { pageNum?: number; pageSize?: number; status?: number }): Promise<any> =>
  request.get('/admin/work-orders/complaints', { params })

export const handleComplaint = (id: number, status: number, remark?: string, result?: string): Promise<any> =>
  request.put(`/admin/work-orders/complaints/${id}`, null, { params: { status, remark, result } })
