<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  PieChart,
  ShoppingBag,
  User,
  Shop,
  Ship,
  Star,
  Setting,
  Document,
  Bell,
  Help,
  ArrowLeft,
  ArrowRight,
  HelpFilled,
  Collection,
  List,
  DataBoard,
  Memo,
  Food
} from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const collapsed = ref(false)

const toggleSidebar = () => {
  collapsed.value = !collapsed.value
}

const menuItems = [
  {
    label: '概览',
    items: [
      { path: '/admin/dashboard', icon: PieChart, label: '仪表盘' },
      { path: '/admin/orders', icon: ShoppingBag, label: '订单管理' },
      { path: '/admin/users', icon: User, label: '用户管理' },
    ]
  },
  {
    label: '运营',
    items: [
      { path: '/admin/merchants', icon: Shop, label: '商家管理' },
      { path: '/admin/drivers', icon: Ship, label: '配送员管理' },
      { path: '/admin/reviews', icon: Star, label: '评论管理' },
    ]
  },
  {
    label: '系统',
    items: [
      { path: '/admin/system', icon: Setting, label: '系统设置' },
      { path: '/admin/logs', icon: Document, label: '日志' },
      { path: '/admin/ai-rules', icon: DataBoard, label: 'AI规则管理' },
      { path: '/admin/categories', icon: Collection, label: '分类管理' },
      { path: '/admin/ingredients', icon: Food, label: '食材管理' },
      { path: '/admin/nutrition', icon: List, label: '营养标准' },
      { path: '/admin/notices', icon: Memo, label: '公告管理' },
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
</script>

<template>
  <div class="admin-layout">
    <aside class="sidebar" :class="{ collapsed }">
      <div class="sidebar-logo">
        <PieChart class="logo-icon" />
        <span v-if="!collapsed">轻食·AI</span>
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
          <HelpFilled />
          <span>超级管理员</span>
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
          <span class="page-title">{{ route.meta.title || '管理后台' }}</span>
          <span class="breadcrumb">
            <span>/</span>
            <span>{{ route.meta.subtitle || '总览' }}</span>
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
            <div class="avatar">A</div>
            <span v-if="!collapsed">超级管理员</span>
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
.admin-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  width: var(--bs-sidebar-width);
  background: var(--bs-sidebar-bg);
  color: var(--bs-sidebar-text);
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
  color: var(--bs-sidebar-text-muted);
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
  background: var(--bs-sidebar-hover);
  color: #FFFFFF;
}

.sidebar-menu a.active {
  background: var(--bs-sidebar-active);
  color: #FFFFFF;
}

.menu-label {
  font-size: var(--bs-font-size-xs);
  text-transform: uppercase;
  color: var(--bs-sidebar-text-label);
  padding: 16px 16px 8px 16px;
  letter-spacing: 0.5px;
  font-weight: 600;
}

.sidebar-footer {
  padding: 16px 24px 20px 24px;
  border-top: 1px solid var(--bs-sidebar-border);
  margin-top: 16px;
  font-size: var(--bs-font-size-sm);
  color: var(--bs-sidebar-text-label);
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

.icon-btn:hover {
  background: var(--bs-bg-hover);
  color: #A8D5BA;
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
  background: var(--bs-primary);
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