import { getUserInfo } from './auth'

export const requireUser = (): boolean => {
  const user = getUserInfo()
  if (!user || Number(user.roleType) !== 10) {
    wx.reLaunch({ url: '/pages/login/login' })
    return false
  }
  return true
}

export const goUserPage = (url: string): void => {
  if (requireUser()) wx.navigateTo({ url })
}
