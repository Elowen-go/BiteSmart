import request from '../utils/request'

export interface LoginRequest {
  username: string
  password: string
  roleType?: number
}

export interface RegisterRequest {
  username: string
  password: string
  nickname?: string
  phone?: string
  roleType?: number
}

export interface LoginResponse {
  code: number
  message: string
  data: {
    token: string
    tokenType: string
    expireTime: string
    user: {
      id: number
      username: string
      nickname: string
      avatar: string
      roleType: number
    }
  }
}

export const login = (data: LoginRequest): Promise<LoginResponse> => {
  return request.post('/auth/login', data)
}

export const register = (data: RegisterRequest): Promise<LoginResponse> => {
  return request.post('/auth/register', data)
}

export const refreshToken = (): Promise<any> => {
  return request.post('/auth/refresh')
}

export const logout = (): Promise<any> => {
  return request.post('/auth/logout')
}
