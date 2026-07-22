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
import { getNoticeList } from '../api/user/notices'
import { resolveFileUrl } from '../utils/fileUrl'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const collapsed = ref(false)
const logoUrl = ref('')
const shopName = ref('')
const shopList = ref<any[]>([])
const notificationCount = ref(0)
/** 营业状态：10-营业中 20-打烊（缺省按营业中处理），store 共享给 ShopInfo 开关实时同步 */
const openStatus = computed(() => userStore.shopOpenStatus ?? 10)

const loadNotificationCount = async () => {
  try {
    const res = await getNoticeList({ pageNum: 1, pageSize: 1 })
    if (res.code === 200) notificationCount.value = Number(res.data?.total || res.data?.list?.length || 0)
  } catch (err) {
    console.error('获取通知数量失败', err)
  }
}

const toggleSidebar = () => {
  collapsed.value = !collapsed.value
}

const currentShopId = computed(() => userStore.currentShopId)

const switchShop = (shopId: number | string) => {
  userStore.setCurrentShopId(shopId)
  const shop = shopList.value.find(s => String(s.id) === String(shopId))
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
  },
  {
    label: '资金',
    items: [{ path: '/merchant/finance', icon: PieChart, label: '资金中心' }]
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
        logoUrl.value = resolveFileUrl(res.data.shopLogo)
      }
      shopName.value = res.data.shopName || ''
      userStore.setShopOpenStatus(res.data.openStatus === 20 ? 20 : 10)
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
          // 如果保存的店铺ID不在列表中，重置为第一个店铺
          const firstShop = shopList.value[0]
          userStore.setCurrentShopId(firstShop.id)
          shopName.value = firstShop.shopName
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
  loadNotificationCount()
})
</script>

<template>
  <div class="merchant-layout">
    <aside class="sidebar" :class="{ collapsed }">
      <div class="sidebar-logo">
        <img v-if="logoUrl" :src="logoUrl" class="logo-img" alt="店铺Logo" />
        <Shop v-else class="logo-icon" />
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
          <span class="footer-shop-name">{{ shopName || '商家后台' }}</span>
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
          <span class="open-pill" :class="{ closed: openStatus === 20 }">
            <i class="open-dot"></i>{{ openStatus === 20 ? '已打烊' : '营业中' }}
          </span>
          <button class="icon-btn notification-btn">
            <Bell />
            <span v-if="notificationCount > 0" class="notification-badge">{{ notificationCount > 99 ? '99+' : notificationCount }}</span>
          </button>
          <button class="icon-btn">
            <HelpFilled />
          </button>
          <div class="user-info">
            <img v-if="userStore.userInfo?.avatar" :src="resolveFileUrl(userStore.userInfo.avatar)" class="avatar" alt="用户头像" />
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

.logo-img {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  object-fit: cover;
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

/* 店铺选择器输入框 - 深色主题（Element 2.x 结构为 .el-select__wrapper） */
.shop-selector :deep(.el-select__wrapper) {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: none;
  border-radius: 6px;
  min-height: 32px;
}

.shop-selector :deep(.el-select__wrapper:hover) {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.3);
}

.shop-selector :deep(.el-select__wrapper.is-focused) {
  background: rgba(255, 255, 255, 0.15);
  border-color: #A8D5BA;
  box-shadow: 0 0 0 2px rgba(168, 213, 186, 0.2);
}

.shop-selector :deep(.el-select__selected-item),
.shop-selector :deep(.el-select__placeholder) {
  color: #FFFFFF;
}

.shop-selector :deep(.el-select__suffix),
.shop-selector :deep(.el-select__caret),
.shop-selector :deep(.el-icon) {
  color: rgba(255, 255, 255, 0.7);
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
  border-left: 2px solid transparent;
  border-radius: 0 var(--bs-radius-md) var(--bs-radius-md) 0;
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
  background: rgba(255,255,255,0.06);
  color: #FFFFFF;
}

/* 对齐 Dashboard 预览版：激活项品牌绿左边条 + 白字 */
.sidebar-menu a.active {
  background: rgba(255,255,255,0.06);
  border-left-color: var(--green);
  color: #FFFFFF;
}

.menu-label {
  padding: 16px 16px 8px 16px;
  color: #5E7A6C;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 2px;
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
  flex-shrink: 0;
}

.footer-shop-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  color: var(--bs-text-title);
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
  background: var(--bs-status-danger);
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

.open-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 999px;
  background: var(--green-soft);
  color: var(--green-deep);
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.open-pill .open-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--green);
}

.open-pill.closed {
  background: var(--orange-soft);
  color: var(--orange);
}

.open-pill.closed .open-dot {
  background: var(--orange);
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

<!-- 店铺选择器下拉面板（popper teleport 到 body，scoped 无法触达，必须全局；
     用 popper-class="shop-select-dropdown" 限定作用范围，不会污染其他布局） -->
<style>
.shop-select-dropdown.el-select__popper,
.shop-select-dropdown.el-popper {
  background: #1B3A2F;
  border: 1px solid rgba(255, 255, 255, 0.15);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
}

.shop-select-dropdown .el-select-dropdown__list,
.shop-select-dropdown .el-scrollbar,
.shop-select-dropdown .el-scrollbar__view {
  background: #1B3A2F;
}

.shop-select-dropdown .el-select-dropdown__list {
  padding: 4px 0;
}

.shop-select-dropdown .el-select-dropdown__item {
  color: rgba(255, 255, 255, 0.85);
  background: transparent;
  padding: 10px 16px;
}

.shop-select-dropdown .el-select-dropdown__item:hover,
.shop-select-dropdown .el-select-dropdown__item.hover {
  background: rgba(255, 255, 255, 0.12) !important;
  color: #FFFFFF;
}

.shop-select-dropdown .el-select-dropdown__item.selected {
  color: #A8D5BA !important;
  font-weight: 600;
  background: rgba(168, 213, 186, 0.15) !important;
}

.shop-select-dropdown .el-popper__arrow::before {
  background: #1B3A2F !important;
  border-color: rgba(255, 255, 255, 0.15) !important;
}
</style>
