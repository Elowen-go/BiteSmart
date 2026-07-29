import request from '../../utils/request'

export const getFundAccounts = (): Promise<any> => request.get('/admin/finance/accounts') as any
export const getSettlements = (params?: { merchantId?: number; status?: number }): Promise<any> => request.get('/admin/finance/settlements', { params }) as any
export const createSettlement = (data: { merchantId: number; amount: number; payoutMethod?: string; payoutAccountMask?: string }): Promise<any> => request.post('/admin/finance/settlements', null, { params: data }) as any
export const approveSettlement = (id: number, remark?: string): Promise<any> => request.put(`/admin/finance/settlements/${id}/approve`, null, { params: { remark } }) as any
export const completeSettlement = (id: number, remark?: string): Promise<any> => request.put(`/admin/finance/settlements/${id}/complete`, null, { params: { remark } }) as any
export const rejectSettlement = (id: number, remark?: string): Promise<any> => request.put(`/admin/finance/settlements/${id}/reject`, null, { params: { remark } }) as any

export const getDriverSettlements = (status?: number): Promise<any> => request.get('/admin/finance/driver-settlements', { params: { status } }) as any
export const completeDriverSettlement = (id: number | string): Promise<any> => request.put(`/admin/finance/driver-settlements/${id}/complete`) as any
