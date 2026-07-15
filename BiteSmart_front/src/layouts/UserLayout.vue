<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  House,
  Bowl,
  ShoppingCart,
  List,
  Ship,
  ChatLineSquare,
  MagicStick,
  Star,
  User,
  Bell,
  Help,
  ArrowLeft,
  ArrowRight,
  HelpFilled,
  Location,
  ChatDotRound,
  Shop
} from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { getNoticeList } from '../api/user/notices'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const collapsed = ref(false)
const notificationCount = ref(0)

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

const menuItems = [
  {
    label: '首页',
    items: [
      { path: '/user', icon: House, label: '首页' },
      { path: '/user/dishes', icon: Bowl, label: '菜品浏览' },
      { path: '/user/cart', icon: ShoppingCart, label: '购物车' },
    ]
  },
  {
    label: '订单',
    items: [
      { path: '/user/orders', icon: List, label: '我的订单' },
      { path: '/user/delivery', icon: Ship, label: '配送追踪' },
    ]
  },
  {
    label: 'AI',
    items: [
      { path: '/user/ai/chat', icon: ChatLineSquare, label: 'AI对话' },
      { path: '/user/ai/recommend', icon: MagicStick, label: 'AI推荐' },
    ]
  },
  {
    label: '健康',
    items: [
      { path: '/user/health', icon: Star, label: '健康记录' },
      { path: '/user/profile', icon: User, label: '个人中心' },
    ]
  },
  {
    label: '更多',
    items: [
      { path: '/user/membership', icon: Star, label: '会员中心' },
      { path: '/user/addresses', icon: Location, label: '地址管理' },
      { path: '/user/reviews', icon: ChatDotRound, label: '我的评价' },
      { path: '/user/complaints', icon: Help, label: '提交投诉' },
      { path: '/user/notices', icon: Bell, label: '系统公告' },
      { path: '/merchant/apply', icon: Shop, label: '商家入驻' },
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

onMounted(loadNotificationCount)
</script>

<template>
  <div class="user-layout">
    <aside class="sidebar" :class="{ collapsed }">
      <div class="sidebar-logo">
        <Heart class="logo-icon" />
        <span v-if="!collapsed">BiteSmart</span>
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
          <User />
          <span>{{ userStore.userInfo?.nickname || '用户' }}</span>
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
          <span class="page-title">{{ route.meta.title || '用户中心' }}</span>
          <span class="breadcrumb">
            <span>/</span>
            <span>{{ route.meta.subtitle || '首页' }}</span>
          </span>
        </div>
        
        <div class="header-right">
          <button class="icon-btn notification-btn">
            <Bell />
            <span v-if="notificationCount > 0" class="notification-badge">{{ notificationCount > 99 ? '99+' : notificationCount }}</span>
          </button>
          <button class="icon-btn">
            <HelpFilled />
          </button>
          <div class="user-info">
            <div class="avatar">{{ (userStore.userInfo?.nickname || 'U')[0] }}</div>
            <span v-if="!collapsed">{{ userStore.userInfo?.nickname || '用户' }}</span>
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
.user-layout {
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
  padding: 16px 24px 16px 24px;
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
