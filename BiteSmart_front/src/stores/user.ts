import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getToken, setToken, removeToken, getRole, setRole, removeRole } from '../utils/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken())
  const role = ref(getRole())
  const userInfo = ref<any>(null)
  const breadcrumbSubtitle = ref('')

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
  }

  const logout = () => {
    token.value = null
    role.value = null
    userInfo.value = null
    removeToken()
    removeRole()
  }

  const setUserInfo = (info: any) => {
    userInfo.value = info
  }

  const setBreadcrumbSubtitle = (subtitle: string) => {
    breadcrumbSubtitle.value = subtitle
  }

  return {
    token,
    role,
    userInfo,
    breadcrumbSubtitle,
    isAuthenticated,
    isAdmin,
    isMerchant,
    isUser,
    login,
    logout,
    setUserInfo,
    setBreadcrumbSubtitle
  }
})