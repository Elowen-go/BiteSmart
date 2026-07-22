import { clearAuth, getToken } from './auth'

const BASE_URL = 'http://localhost:8080/api'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp?: number
}

interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: Record<string, unknown> | unknown
  needAuth?: boolean
  contentType?: 'json' | 'form'
}

let kickingOff = false

/** 401 统一处理：清登录态并回登录页（防止多请求并发时重复跳转） */
const kickToLogin = (): void => {
  if (kickingOff) return
  kickingOff = true
  clearAuth()
  wx.reLaunch({
    url: '/pages/login/login',
    complete: () => { kickingOff = false }
  })
}

export const request = <T>(options: RequestOptions): Promise<T> => {
  return new Promise((resolve, reject) => {
    const token = getToken()
    if (options.needAuth !== false && !token) {
      reject(new Error('请先登录'))
      return
    }

    wx.request<ApiResponse<T>>({
      url: `${BASE_URL}${options.url}`,
      method: options.method || 'GET',
      timeout: 8000,
      data: options.data as WechatMiniprogram.IAnyObject,
      header: {
        'Content-Type': options.contentType === 'form' ? 'application/x-www-form-urlencoded' : 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      success: (response) => {
        const result = response.data
        if (!result || typeof result.code !== 'number') {
          reject(new Error('服务响应异常，请稍后重试'))
          return
        }
        if (result.code === 401) {
          kickToLogin()
          reject(new Error(result.message || '登录已过期，请重新登录'))
          return
        }
        if (result.code >= 400) {
          reject(new Error(result.message || '请求失败'))
          return
        }
        resolve(result.data)
      },
      fail: reject
    })
  })
}
