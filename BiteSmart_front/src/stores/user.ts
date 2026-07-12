import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getToken, setToken, removeToken, getRole, setRole, removeRole, getUserInfo, setUserInfo, removeUserInfo } from '../utils/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken())
  const role = ref(getRole())
  const userInfo = ref<any>(getUserInfo())
  const breadcrumbSubtitle = ref('')
  const currentShopId = ref(Number(localStorage.getItem('currentShopId') || 0))

  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'ADMIN' || role.value === '40')
  const isMerchant = computed(() => role.value === 'MERCHANT' || role.value === '20')
  const isUser = computed(() => role.value === 'USER' || role.value === '10')

  const login = (newToken: string, newRole: string, info?: any) => {
    token.value = newToken
    role.value = newRole
    userInfo.value = info
    setToken(newToken)
    setRole(newRole)
    if (info) {
      setUserInfo(info)
    }
  }

  const logout = () => {
    token.value = null
    role.value = null
    userInfo.value = null
    removeToken()
    removeRole()
    removeUserInfo()
    localStorage.removeItem('currentShopId')
  }

  const setUserInfo = (info: any) => {
    userInfo.value = info
    setUserInfo(info)
  }

  const setBreadcrumbSubtitle = (subtitle: string) => {
    breadcrumbSubtitle.value = subtitle
  }

  const setCurrentShopId = (shopId: number) => {
    currentShopId.value = shopId
    localStorage.setItem('currentShopId', String(shopId))
  }

  return {
    token,
    role,
    userInfo,
    breadcrumbSubtitle,
    currentShopId,
    isAuthenticated,
    isAdmin,
    isMerchant,
    isUser,
    login,
    logout,
    setUserInfo,
    setBreadcrumbSubtitle,
    setCurrentShopId
  }
})