import { getToken } from '../utils/auth'
import { API_ORIGIN } from '../utils/request'

interface UploadResponse {
  code: number
  message?: string
  data?: { url?: string }
}

export const resolveFileUrl = (value?: string | null): string => {
  if (!value) return ''
  const url = String(value).trim()
  if (!url) return ''
  if (/^(https?:)?\/\//i.test(url)) return url
  if (url.startsWith('/api/files/download')) return `${API_ORIGIN}${url}`
  if (url.startsWith('/uploads/')) return `${API_ORIGIN}/api/files/download${url}`
  if (url.startsWith('uploads/')) return `${API_ORIGIN}/api/files/download/${url}`
  return url
}

export const uploadFile = (filePath: string, bizType: string): Promise<string> => {
  return new Promise((resolve, reject) => {
    const token = getToken()
    if (!token) {
      reject(new Error('请先登录'))
      return
    }

    wx.uploadFile({
      url: `${API_ORIGIN}/api/files/upload`,
      filePath,
      name: 'file',
      formData: { bizType },
      header: { Authorization: `Bearer ${token}` },
      success: (response) => {
        try {
          const result = JSON.parse(response.data) as UploadResponse
          if (response.statusCode === 401 || result.code === 401) {
            reject(new Error('登录已过期，请重新登录'))
            return
          }
          const data = result.data
          if (result.code >= 400 || !data || !data.url) {
            reject(new Error(result.message || '图片上传失败'))
            return
          }
          resolve(data.url)
        } catch (_) {
          reject(new Error('上传响应异常，请稍后重试'))
        }
      },
      fail: reject
    })
  })
}
