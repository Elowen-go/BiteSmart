import { request } from '../utils/request'

export interface Notice { id?: number | string; title?: string; content?: string; publishTime?: string; createTime?: string }
export const getNotices = (): Promise<Notice[]> => request<Notice[]>({ url: '/notices', needAuth: false })
