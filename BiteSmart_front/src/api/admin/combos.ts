import request from '../../utils/request'

export const getAdminComboList = (params?: { page?: number; size?: number }) => request.get('/admin/combos', { params })
export const getAdminComboDetail = (id: number) => request.get(`/admin/combos/${id}`)
export const addAdminCombo = (data: any) => request.post('/admin/combos', data)
export const updateAdminCombo = (id: number, data: any) => request.put(`/admin/combos/${id}`, data)
export const deleteAdminCombo = (id: number) => request.delete(`/admin/combos/${id}`)
