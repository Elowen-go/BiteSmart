import request from '../../utils/request'

export interface Driver {
  id: number
  userId: number
  realName: string
  phone: string
  idCard: string
  driverLicense: string
  vehicleType: string
  vehiclePlate: string
  status: number
  createTime: string
  updateTime: string
}

export const getDriverList = (params?: { pageNum?: number; pageSize?: number; status?: number }): Promise<any> => {
  return request.get('/admin/drivers', { params })
}

export const getDriverDetail = (id: number): Promise<any> => {
  return request.get(`/admin/drivers/${id}`)
}

export const updateDriverStatus = (id: number, status: number): Promise<any> => {
  return request.put(`/admin/drivers/${id}/status`, { params: { status } })
}
