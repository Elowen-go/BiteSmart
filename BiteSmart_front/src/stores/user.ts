import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getToken, setToken, removeToken, getRole, setRole, removeRole, getUserInfo, setUserInfo as persistUserInfo, removeUserInfo } from '../utils/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken())
  const role = ref(getRole())
  const userInfo = ref<any>(getUserInfo())
  const breadcrumbSubtitle = ref('')
  /** 店铺营业状态：10-营业中 20-打烊，null 表示尚未拉取（MerchantLayout pill / ShopInfo 开关共享） */
  const shopOpenStatus = ref<number | null>(null)
  // 店铺 id 是雪花 ID，必须保持字符串，Number() 强转会丢精度
  const currentShopId = ref<string>(localStorage.getItem('currentShopId') || '')

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
    persistUserInfo(info)
  }

  const setBreadcrumbSubtitle = (subtitle: string) => {
    breadcrumbSubtitle.value = subtitle
  }

  const setShopOpenStatus = (status: number | null) => {
    shopOpenStatus.value = status
  }

  const setCurrentShopId = (shopId: number | string) => {
    currentShopId.value = String(shopId)
    localStorage.setItem('currentShopId', String(shopId))
  }

  return {
    token,
    role,
    userInfo,
    breadcrumbSubtitle,
    currentShopId,
    shopOpenStatus,
    isAuthenticated,
    isAdmin,
    isMerchant,
    isUser,
    login,
    logout,
    setUserInfo,
    setBreadcrumbSubtitle,
    setShopOpenStatus,
    setCurrentShopId
  }
})
