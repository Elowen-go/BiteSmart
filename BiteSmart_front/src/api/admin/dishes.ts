import request from '../../utils/request'

export const getAdminDishList = (params?: { page?: number; size?: number }) => request.get('/admin/dishes', { params })
export const getAdminDishDetail = (id: number) => request.get(`/admin/dishes/${id}`)
export const addAdminDish = (data: any) => request.post('/admin/dishes', data)
export const updateAdminDish = (id: number, data: any) => request.put(`/admin/dishes/${id}`, data)
export const deleteAdminDish = (id: number) => request.delete(`/admin/dishes/${id}`)
