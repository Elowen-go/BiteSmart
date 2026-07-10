import request from '../../utils/request'

export interface SysConfig {
  id: number
  configKey: string
  configValue: string
  description: string
  group: string
  isSensitive: number
  createTime: string
  updateTime: string
}

export interface OperationLog {
  id: number
  userId: number
  username: string
  operation: string
  module: string
  ip: string
  createTime: string
}

export const getConfigList = (group?: string): Promise<any> => {
  return request.get('/admin/system/configs', { params: { group } })
}

export const addConfig = (data: SysConfig): Promise<any> => {
  return request.post('/admin/system/configs', data)
}

export const updateConfig = (configKey: string, configValue: string, description?: string, isSensitive?: number): Promise<any> => {
  return request.put(`/admin/system/configs/${configKey}`, { params: { configValue, description, isSensitive } })
}

export const deleteConfig = (id: number): Promise<any> => {
  return request.delete(`/admin/system/configs/${id}`)
}

export const getLogList = (params?: { page?: number; size?: number }): Promise<any> => {
  return request.get('/admin/system/logs', { params })
}
