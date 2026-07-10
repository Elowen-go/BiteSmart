<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  PieChart,
  ShoppingBag,
  Bowl,
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
  HelpFilled
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
      { path: '/admin/dishes', icon: Bowl, label: '菜品管理' },
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
        <PieChart style="font-size: 28px; color: #A8D5BA;" />
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
          </li>
        </template>
      </ul>
      
      <div class="sidebar-footer">
        <HelpFilled />
        <span v-if="!collapsed">超级管理员</span>
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
          <button class="icon-btn">
            <Bell />
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
  overflow-y: auto;
  padding: 24px 0 20px 0;
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

.sidebar.collapsed .sidebar-logo {
  padding: 0 0 24px 0;
  justify-content: center;
}

.sidebar-menu {
  flex: 1;
  list-style: none;
  padding: 0 12px;
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
  padding: 16px 24px 0 24px;
  border-top: 1px solid var(--bs-sidebar-border);
  margin-top: 16px;
  font-size: var(--bs-font-size-sm);
  color: var(--bs-sidebar-text-label);
  display: flex;
  align-items: center;
  gap: 8px;
}

.sidebar.collapsed .sidebar-footer {
  padding: 16px 0 0 0;
  justify-content: center;
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