import { setAuth, type MiniUserInfo } from '../utils/auth'
import { request } from '../utils/request'

interface LoginResponse {
  token: string
  tokenType: string
  expireTime: number
  user: MiniUserInfo
  accountSetupRequired?: boolean
}

export const loginWithWechat = (code: string, roleType: number): Promise<LoginResponse> => {
  return request<LoginResponse>({
    url: '/auth/wechat-login',
    method: 'POST',
    data: { code, roleType },
    needAuth: false
  }).then((result) => {
    setAuth(result.token, result.user)
    return result
  })
}

export const loginWithAccount = (username: string, password: string, roleType: number): Promise<LoginResponse> => {
  return request<LoginResponse>({
    url: '/auth/login',
    method: 'POST',
    data: { username, password, roleType },
    needAuth: false
  }).then((result) => {
    setAuth(result.token, result.user)
    return result
  })
}

export const setupCredentials = (username: string, phone: string, password: string): Promise<void> => {
  return request<void>({
    url: '/auth/credentials',
    method: 'PUT',
    data: { username, phone, password }
  })
}

export const bindWechat = (code: string): Promise<void> => {
  return request<void>({
    url: '/auth/wechat-bind',
    method: 'POST',
    data: { code }
  })
}
