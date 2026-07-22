const TOKEN_KEY = 'bitesmart_token'
const USER_KEY = 'bitesmart_user'

export interface MiniUserInfo {
  id: number
  username: string
  nickname?: string
  avatar?: string
  roleType: number
}

const normalizeUser = (stored: unknown): MiniUserInfo | null => {
  if (!stored) return null
  if (typeof stored === 'string') {
    try {
      return normalizeUser(JSON.parse(stored) as unknown)
    } catch (_) {
      return null
    }
  }
  if (typeof stored !== 'object') return null
  const value = stored as Record<string, unknown>
  const role = value.role as Record<string, unknown> | undefined
  const roleTypeValue = value.roleType !== undefined && value.roleType !== null ? value.roleType : (role && role.roleType !== undefined && role.roleType !== null ? role.roleType : role && role.type)
  const roleType = Number(roleTypeValue)
  if (![10, 20, 30].includes(roleType)) return null
  return { ...value, roleType } as MiniUserInfo
}

export const getToken = (): string => wx.getStorageSync(TOKEN_KEY) || ''

export const setAuth = (token: string, user: MiniUserInfo): void => {
  const normalizedUser = normalizeUser(user)
  if (!token || !normalizedUser) throw new Error('登录响应缺少有效的用户信息')
  wx.setStorageSync(TOKEN_KEY, token)
  wx.setStorageSync(USER_KEY, normalizedUser)
}

export const getUserInfo = (): MiniUserInfo | null => normalizeUser(wx.getStorageSync(USER_KEY))

export const clearAuth = (): void => {
  wx.removeStorageSync(TOKEN_KEY)
  wx.removeStorageSync(USER_KEY)
}
