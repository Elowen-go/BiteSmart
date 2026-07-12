<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  PieChart,
  ShoppingBag,
  Bowl,
  Box,
  List,
  OfficeBuilding,
  Star,
  Ship,
  Setting,
  Bell,
  Help,
  ArrowLeft,
  ArrowRight,
  HelpFilled,
  Shop,
  User
} from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { getShopInfo, getShopList } from '../api/merchant/shop'
import { getProfile } from '../api/merchant/profile'

// 店铺选择器样式 - 全局样式
const shopSelectorStyles = `
/* 店铺选择器输入框 - 深色主题 */
.shop-select-input .el-select__wrapper {
  background: rgba(255,255,255,0.08) !important;
  border: 1px solid rgba(255,255,255,0.2) !important;
  box-shadow: none !important;
  border-radius: 6px !important;
}
.shop-select-input .el-select__wrapper:hover {
  background: rgba(255,255,255,0.12) !important;
  border-color: rgba(255,255,255,0.3) !important;
}
.shop-select-input .el-select__wrapper.is-focused {
  background: rgba(255,255,255,0.15) !important;
  border-color: #A8D5BA !important;
  box-shadow: 0 0 0 2px rgba(168, 213, 186, 0.2) !important;
}
.shop-select-input .el-select__selection {
  color: #FFFFFF !important;
}
.shop-select-input .el-select__placeholder {
  color: #FFFFFF !important;
}
.shop-select-input .el-select__selected-item {
  color: #FFFFFF !important;
}
.shop-select-input .el-select__suffix {
  color: rgba(255,255,255,0.7) !important;
}
.shop-select-input .el-select__caret {
  color: rgba(255,255,255,0.7) !important;
}
.shop-select-input .el-icon {
  color: rgba(255,255,255,0.7) !important;
}

/* 店铺选择器下拉框 - 深色主题 */
.shop-select-dropdown {
  background: #1B3A2F !important;
  border: 1px solid rgba(255,255,255,0.15) !important;
  box-shadow: 0 4px 12px rgba(0,0,0,0.4) !important;
}
.shop-select-dropdown .el-select-dropdown__list {
  background: #1B3A2F !important;
  padding: 4px 0;
}
.shop-select-dropdown .el-select-dropdown__item {
  color: rgba(255,255,255,0.85) !important;
  background: transparent !important;
}
.shop-select-dropdown .el-select-dropdown__item:hover,
.shop-select-dropdown .el-select-dropdown__item.hover {
  background: rgba(255,255,255,0.12) !important;
  color: #FFFFFF !important;
}
.shop-select-dropdown .el-select-dropdown__item.selected {
  color: #A8D5BA !important;
  font-weight: 600;
  background: rgba(168, 213, 186, 0.15) !important;
}
.shop-select-dropdown .el-popper__arrow::before {
  background: #1B3A2F !important;
  border-color: rgba(255,255,255,0.15) !important;
}
.shop-select-dropdown .el-scrollbar {
  background: #1B3A2F !important;
}
.shop-select-dropdown .el-scrollbar__view {
  background: #1B3A2F !important;
}
`

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const collapsed = ref(false)
const logoUrl = ref('')
const shopName = ref('')
const shopList = ref<any[]>([])

const toggleSidebar = () => {
  collapsed.value = !collapsed.value
}

const currentShopId = computed(() => userStore.currentShopId)

const switchShop = (shopId: number) => {
  userStore.setCurrentShopId(shopId)
  const shop = shopList.value.find(s => s.id === shopId)
  if (shop) {
    shopName.value = shop.shopName
  }
}

const menuItems = [
  {
    label: '概览',
    items: [
      { path: '/merchant/dashboard', icon: PieChart, label: '工作台' },
      { path: '/merchant/orders', icon: ShoppingBag, label: '订单处理' },
    ]
  },
  {
    label: '管理',
    items: [
      { path: '/merchant/shop', icon: Setting, label: '店铺管理' },
      { path: '/merchant/dishes', icon: Bowl, label: '菜品管理' },
      { path: '/merchant/combos', icon: Box, label: '套餐管理' },
      { path: '/merchant/categories', icon: List, label: '分类管理' },
    ]
  },
  {
    label: '运营',
    items: [
      { path: '/merchant/inventory', icon: OfficeBuilding, label: '库存管理' },
      { path: '/merchant/delivery', icon: Ship, label: '配送管理' },
      { path: '/merchant/reviews', icon: Star, label: '评价管理' },
      { path: '/merchant/statistics', icon: PieChart, label: '销售统计' },
    ]
  },
  {
    label: '商家',
    items: [
      { path: '/merchant/profile', icon: User, label: '个人中心' },
    ]
  }
]

const currentPath = computed(() => route.path)

const isActive = (path: string) => {
  return currentPath.value === path
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

const fetchLogo = async () => {
  try {
    const res = await getShopInfo()
    if (res.code === 200) {
      if (res.data.shopLogo) {
        logoUrl.value = res.data.shopLogo.startsWith('http') 
          ? res.data.shopLogo 
          : `/api/files/download${res.data.shopLogo}`
      }
      shopName.value = res.data.shopName || ''
    }
  } catch (e) {
    console.error('获取店铺Logo失败', e)
  }
}

const fetchShopList = async () => {
  try {
    const res = await getShopList()
    if (res.code === 200 && res.data) {
      shopList.value = res.data
      // 如果有店铺列表，自动选中第一个（或已存储的当前店铺）
      if (shopList.value.length > 0) {
        const savedShopId = userStore.currentShopId
        const existShop = shopList.value.find((s: any) => s.id === savedShopId)
        if (existShop) {
          shopName.value = existShop.shopName
        } else {
          userStore.setCurrentShopId(shopList.value[0].id)
          shopName.value = shopList.value[0].shopName
        }
      }
    }
  } catch (e) {
    console.error('获取店铺列表失败', e)
  }
}

const fetchUserInfo = async () => {
  try {
    const res = await getProfile()
    if (res.code === 200) {
      if (userStore.userInfo) {
        userStore.userInfo.nickname = res.data.nickname
        userStore.userInfo.avatar = res.data.avatar
      }
    }
  } catch (e) {
    console.error('获取用户信息失败', e)
  }
}

onMounted(() => {
  userStore.setBreadcrumbSubtitle('')
  fetchLogo()
  fetchShopList()
  fetchUserInfo()
  
  // 注入全局样式
  const styleEl = document.createElement('style')
  styleEl.id = 'shop-selector-global-styles'
  styleEl.textContent = shopSelectorStyles
  document.head.appendChild(styleEl)
})
</script>

<template>
  <div class="merchant-layout">
    <aside class="sidebar" :class="{ collapsed }">
      <div class="sidebar-logo">
        <Shop class="logo-icon" />
        <span v-if="!collapsed">商家后台</span>
      </div>
      
      <!-- 店铺选择器 -->
      <div class="shop-selector" v-if="!collapsed && shopList.length > 0">
        <el-select 
          v-model="currentShopId" 
          @change="switchShop" 
          size="small" 
          placeholder="选择店铺"
          popper-class="shop-select-dropdown"
          class="shop-select-input"
        >
          <el-option
            v-for="shop in shopList"
            :key="shop.id"
            :label="shop.shopName"
            :value="shop.id"
          >
            <span>{{ shop.shopName }}</span>
          </el-option>
        </el-select>
      </div>
      
      <ul class="sidebar-menu">
        <template v-for="group in menuItems" :key="group.label">
          <li class="menu-label" v-if="!collapsed">{{ group.label }}</li>
          <li v-for="item in group.items" :key="item.path">
            <router-link
              :to="item.path"
              :class="{ active: isActive(item.path) }"
            >
              <component :is="item.icon" />
              <span v-if="!collapsed">{{ item.label }}</span>
            </router-link>
            <span class="tooltip">{{ item.label }}</span>
          </li>
        </template>
      </ul>
      
      <div class="sidebar-footer">
        <template v-if="!collapsed">
          <Shop />
          <span>商家</span>
        </template>
        <button class="collapse-btn" @click="collapsed = !collapsed" :title="collapsed ? '展开侧边栏' : '收起侧边栏'">
          <ArrowLeft v-if="!collapsed" />
          <ArrowRight v-else />
        </button>
      </div>
    </aside>
    
    <div class="main-content">
      <header class="header">
        <div class="header-left">
          <button class="menu-toggle" @click="toggleSidebar">
            <ArrowLeft v-if="!collapsed" />
            <ArrowRight v-else />
          </button>
          <span class="page-title">{{ route.meta.title || '商家后台' }}</span>
          <span class="breadcrumb">
            <span>/</span>
            <span>{{ userStore.breadcrumbSubtitle || route.meta.subtitle || '总览' }}</span>
          </span>
        </div>
        
        <div class="header-right">
          <button class="icon-btn notification-btn">
            <Bell />
            <span class="notification-badge">3</span>
          </button>
          <button class="icon-btn">
            <HelpFilled />
          </button>
          <div class="user-info">
            <img v-if="userStore.userInfo?.avatar" :src="userStore.userInfo.avatar.startsWith('http') ? userStore.userInfo.avatar : `/api/files/download${userStore.userInfo.avatar}`" class="avatar" alt="用户头像" />
            <div v-else class="avatar">{{ (userStore.userInfo?.nickname || userStore.userInfo?.username || '商')[0] }}</div>
            <span v-if="!collapsed">{{ userStore.userInfo?.nickname || userStore.userInfo?.username || '商家' }}</span>
            <button class="logout-btn" @click="handleLogout">退出</button>
          </div>
        </div>
      </header>
      
      <div class="content-body">
        <router-view />
      </div>
    </div>
  </div>
</template>

<style scoped>
.merchant-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  width: var(--bs-sidebar-width);
  background: #1B3A2F;
  color: #FFFFFF;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  height: 100vh;
  overflow: hidden;
  padding: 24px 0 0 0;
  position: relative;
  transition: width 0.2s;
}



.sidebar.collapsed {
  width: 72px;
  padding: 16px 0;
}

.sidebar-logo {
  padding: 0 24px 32px 24px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: var(--bs-font-size-2xl);
  font-weight: 600;
  color: #FFFFFF;
}

.logo-icon {
  width: 24px;
  height: 24px;
  color: #A8D5BA;
}

.sidebar.collapsed .sidebar-logo {
  padding: 0 0 24px 0;
  justify-content: center;
}

.shop-selector {
  padding: 0 16px 16px 16px;
}

.shop-selector .el-select {
  width: 100%;
}

.shop-selector :deep(.el-select .el-input__wrapper) {
  background: rgba(255,255,255,0.08) !important;
  border: 1px solid rgba(255,255,255,0.2) !important;
  box-shadow: none !important;
  border-radius: 6px;
}

.shop-selector :deep(.el-select .el-input__wrapper:hover) {
  background: rgba(255,255,255,0.12) !important;
  border-color: rgba(255,255,255,0.3) !important;
}

.shop-selector :deep(.el-select .el-input__wrapper.is-focus) {
  background: rgba(255,255,255,0.15) !important;
  border-color: #A8D5BA !important;
  box-shadow: 0 0 0 2px rgba(168, 213, 186, 0.2) !important;
}

.shop-selector :deep(.el-select .el-input__inner) {
  color: #FFFFFF !important;
  font-size: 14px;
}

.shop-selector :deep(.el-select .el-input__inner::placeholder) {
  color: rgba(255,255,255,0.5) !important;
}

.shop-selector :deep(.el-select .el-input__suffix) {
  color: rgba(255,255,255,0.7) !important;
}

.shop-selector :deep(.el-select .el-input__suffix-inner) {
  color: rgba(255,255,255,0.7) !important;
}

.shop-selector :deep(.el-select .el-icon) {
  color: rgba(255,255,255,0.7) !important;
}

/* 下拉弹出框容器 - 覆盖所有可能的类名 */
.shop-selector :deep(.el-popper),
.shop-selector :deep(.el-select__popper),
.shop-selector :deep(.el-select .el-select__popper) {
  background: #1B3A2F !important;
  border: 1px solid rgba(255,255,255,0.15) !important;
  box-shadow: 0 4px 12px rgba(0,0,0,0.4) !important;
}

/* 弹窗箭头 */
.shop-selector :deep(.el-popper__arrow::before),
.shop-selector :deep(.el-select .el-select__popper .el-popper__arrow::before) {
  background: #1B3A2F !important;
  border-color: rgba(255,255,255,0.15) !important;
}

/* el-scrollbar 背景 */
.shop-selector :deep(.el-select-dropdown),
.shop-selector :deep(.el-select-dropdown__wrap),
.shop-selector :deep(.el-select-dropdown__list),
.shop-selector :deep(.el-scrollbar),
.shop-selector :deep(.el-scrollbar__view),
.shop-selector :deep(.el-select-dropdown .el-scrollbar),
.shop-selector :deep(.el-select-dropdown .el-scrollbar__view) {
  background: #1B3A2F !important;
}

.shop-selector :deep(.el-select-dropdown__list) {
  padding: 4px 0;
}

/* 选项项 */
.shop-selector :deep(.el-select-dropdown__item) {
  color: rgba(255,255,255,0.85);
  background: transparent !important;
  padding: 10px 16px;
}

/* 悬停 */
.shop-selector :deep(.el-select-dropdown__item:hover),
.shop-selector :deep(.el-select-dropdown__item.hover) {
  background: rgba(255,255,255,0.12) !important;
  color: #FFFFFF;
}

/* 选中 */
.shop-selector :deep(.el-select-dropdown__item.selected) {
  color: #A8D5BA !important;
  font-weight: 600;
  background: rgba(168, 213, 186, 0.15) !important;
}

.sidebar-menu {
  flex: 1;
  list-style: none;
  padding: 0 12px;
  overflow-y: auto;
  overflow-x: hidden;
}

.sidebar-menu::-webkit-scrollbar {
  width: 4px;
}

.sidebar-menu::-webkit-scrollbar-track {
  background: transparent;
}

.sidebar-menu::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 2px;
}

.sidebar-menu::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.25);
}

.sidebar-menu li {
  margin-bottom: 4px;
}

.sidebar-menu a {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 16px;
  border-radius: var(--bs-radius-md);
  color: rgba(255,255,255,0.7);
  text-decoration: none;
  font-size: var(--bs-font-size-md);
  font-weight: 500;
  transition: 0.15s;
}

.sidebar-menu a svg {
  width: 20px;
  height: 20px;
}

.sidebar.collapsed .sidebar-menu a {
  justify-content: center;
  padding: 12px;
}

.sidebar-menu a:hover {
  background: rgba(255,255,255,0.08);
  color: #FFFFFF;
}

.sidebar-menu a.active {
  background: rgba(255,255,255,0.15);
  color: #FFFFFF;
}

.menu-label {
  font-size: var(--bs-font-size-xs);
  text-transform: uppercase;
  color: rgba(255,255,255,0.35);
  padding: 16px 16px 8px 16px;
  letter-spacing: 0.5px;
  font-weight: 600;
}

.sidebar-footer {
  padding: 16px 24px 20px 24px;
  border-top: 1px solid rgba(255,255,255,0.06);
  margin-top: 16px;
  font-size: var(--bs-font-size-sm);
  color: rgba(255,255,255,0.4);
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.sidebar-footer svg {
  width: 20px;
  height: 20px;
  gap: 8px;
}

.sidebar.collapsed .sidebar-footer {
  padding: 16px 0 0 0;
  justify-content: center;
}

.sidebar.collapsed .collapse-btn {
  margin-left: 0;
  margin-top: 0;
}

.collapse-btn {
  background: rgba(255, 255, 255, 0.1);
  border: none;
  color: #FFFFFF;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  margin-left: auto;
  margin-top: 8px;
}

.collapse-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  color: #A8D5BA;
}

.collapse-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  color: #A8D5BA;
}

.collapse-btn svg {
  width: 16px;
  height: 16px;
}

.sidebar-menu li {
  position: relative;
}

.sidebar-menu li .tooltip {
  position: absolute;
  left: 100%;
  top: 50%;
  transform: translateY(-50%);
  background: var(--bs-text-title);
  color: #FFFFFF;
  padding: 6px 12px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
  opacity: 0;
  visibility: hidden;
  pointer-events: none;
  transition: all 0.2s;
  margin-left: 8px;
  z-index: 100;
}

.sidebar.collapsed .sidebar-menu li:hover .tooltip {
  opacity: 1;
  visibility: visible;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

.header {
  background: var(--bs-header-bg);
  border-bottom: 1px solid var(--bs-header-border);
  padding: 0 32px;
  height: var(--bs-header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: var(--bs-font-size-xl);
  font-weight: 500;
  color: var(--bs-text-title);
}

.menu-toggle {
  background: none;
  border: none;
  color: var(--bs-text-muted);
  font-size: var(--bs-font-size-lg);
  cursor: pointer;
  padding: 4px;
  border-radius: var(--bs-radius-sm);
  transition: all 0.15s;
}

.menu-toggle:hover {
  background: var(--bs-bg-hover);
  color: var(--bs-text-title);
}

.breadcrumb {
  font-size: var(--bs-font-size-sm);
  font-weight: 400;
  color: var(--bs-text-muted);
}

.breadcrumb span {
  margin: 0 6px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.icon-btn {
  background: none;
  border: none;
  color: var(--bs-text-muted);
  font-size: var(--bs-font-size-xl);
  cursor: pointer;
  padding: 4px;
  border-radius: var(--bs-radius-sm);
  transition: all 0.15s;
}

.icon-btn:hover {
  background: var(--bs-bg-hover);
  color: #A8D5BA;
}

.icon-btn svg {
  width: 20px;
  height: 20px;
}

.notification-btn {
  position: relative;
}

.notification-badge {
  position: absolute;
  top: -2px;
  right: -4px;
  background: #E57373;
  color: #FFFFFF;
  font-size: 10px;
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #1B3A2F;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: var(--bs-font-size-sm);
  object-fit: cover;
}

.logout-btn {
  background: none;
  border: none;
  color: var(--bs-text-muted);
  font-size: var(--bs-font-size-sm);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--bs-radius-sm);
  transition: all 0.15s;
}

.logout-btn:hover {
  background: var(--bs-bg-hover);
  color: var(--bs-text-title);
}

.content-body {
  flex: 1;
  overflow-y: auto;
  padding: var(--bs-spacing-lg) var(--bs-spacing-xl);
  background: var(--bs-bg-page);
}
</style>