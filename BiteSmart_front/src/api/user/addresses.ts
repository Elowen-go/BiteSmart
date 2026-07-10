import request from '../../utils/request'

export interface UserAddress {
  id: number
  userId: number
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detailAddress: string
  isDefault: number
  createTime: string
  updateTime: string
}

export const getAddressList = (): Promise<any> => {
  return request.get('/user/addresses')
}

export const addAddress = (data: UserAddress): Promise<any> => {
  return request.post('/user/addresses', data)
}

export const updateAddress = (id: number, data: UserAddress): Promise<any> => {
  return request.put(`/user/addresses/${id}`, data)
}

export const deleteAddress = (id: number): Promise<any> => {
  return request.delete(`/user/addresses/${id}`)
}
