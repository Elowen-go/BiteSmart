import request from '../../utils/request'

export const getAdminDeliveryTasks = (params?: { pageNum?: number; pageSize?: number; taskStatus?: number }): Promise<any> =>
  request.get('/admin/orders/delivery-tasks', { params })
