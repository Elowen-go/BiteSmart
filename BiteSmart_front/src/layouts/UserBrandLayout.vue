<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, ShoppingBag, Bell, User, ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { getCartList } from '../api/user/cart'
import { getCurrentUser } from '../api/user/profile'
import { resolveFileUrl } from '../utils/fileUrl'

const route = useRoute(), router = useRouter(), userStore = useUserStore()
const userName = computed(() => userStore.userInfo?.nickname || '我的账户')

const cartCount = ref(0)
const loadCartCount = async () => {
  try {
    const res = await getCartList()
    cartCount.value = (res.data || []).reduce((sum: number, item: any) => sum + Number(item.quantity || 0), 0)
  } catch {
    cartCount.value = 0
  }
}
const handleCartUpdated = () => { loadCartCount() }
const refreshUser = async () => {
  try {
    const res = await getCurrentUser()
    if (res.code === 200 && res.data) userStore.setUserInfo(res.data)
  } catch (_) {
    // Keep the cached profile available when the API is temporarily unavailable.
  }
}
watch(() => route.path, loadCartCount)
onMounted(() => {
  loadCartCount()
  refreshUser()
  window.addEventListener('cart-updated', handleCartUpdated)
})
onUnmounted(() => window.removeEventListener('cart-updated', handleCartUpdated))

const nav = [
  { label: '首页', path: '/user' },
  { label: '菜品', path: '/user/dishes' },
  { label: '健康套餐', path: '/user/combos' },
  { label: 'AI 食谱', path: '/user/ai/recommend' },
  { label: '我的订单', path: '/user/orders' }
]
const active = (path: string) => path === '/user' ? route.path === '/user' : route.path.startsWith(path)
const openSearch = () => router.push('/user/dishes')
const openNotices = () => router.push('/user/notices')
const logout = () => { userStore.logout(); router.push('/login') }
</script>

<template>
  <div class="site-shell">
    <header class="site-header">
      <div class="header-inner">
        <router-link to="/user" class="brand">
          <span class="brand-mark">B</span>
          <span>BiteSmart</span>
        </router-link>
        <nav>
          <router-link v-for="item in nav" :key="item.path" :to="item.path" :class="{ active: active(item.path) }">
            {{ item.label }}
          </router-link>
        </nav>
        <div class="header-actions">
          <button title="搜索菜品" @click="openSearch"><Search /></button>
          <button title="系统公告" @click="openNotices"><Bell /></button>
          <button class="cart" title="购物车" @click="router.push('/user/cart')">
            <ShoppingBag />
            <i v-if="cartCount > 0">{{ cartCount > 99 ? '99+' : cartCount }}</i>
          </button>
          <el-dropdown>
            <button class="account">
              <span class="avatar">
                <img v-if="userStore.userInfo?.avatar" :src="resolveFileUrl(userStore.userInfo.avatar)" alt="用户头像" @error="$event.currentTarget.style.display = 'none'" />
                <span v-else>{{ userName[0] }}</span>
              </span>
              <span class="account-name">{{ userName }}</span>
              <ArrowDown />
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/user/profile')">个人中心</el-dropdown-item>
                <el-dropdown-item @click="router.push('/user/health')">健康记录</el-dropdown-item>
                <el-dropdown-item @click="router.push('/user/membership')">会员中心</el-dropdown-item>
                <el-dropdown-item divided @click="router.push('/user/addresses')">地址管理</el-dropdown-item>
                <el-dropdown-item @click="router.push('/user/reviews')">我的评价</el-dropdown-item>
                <el-dropdown-item @click="router.push('/user/complaints')">提交投诉</el-dropdown-item>
                <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>
    <main>
      <router-view />
    </main>
    <footer class="site-footer">
      <div>
        <strong>BiteSmart</strong>
        <span>为每一个健康目标，准备更适合的一餐。</span>
      </div>
      <div>健康饮食 · 智能推荐 · 配送到家</div>
    </footer>
  </div>
</template>

<style scoped>
.site-shell {
  min-height: 100vh;
  background: #f7f8f5;
  color: #1f2a24;
  font-family: "Microsoft YaHei UI", "Microsoft YaHei", "Noto Sans SC", sans-serif;
  -webkit-font-smoothing: antialiased;
}

.site-header {
  height: 76px;
  background: rgba(255, 255, 255, .96);
  border-bottom: 1px solid #e5ebe4;
  position: sticky;
  top: 0;
  z-index: 10;
}

.header-inner {
  max-width: 1240px;
  height: 100%;
  margin: auto;
  padding: 0 24px;
  display: grid;
  grid-template-columns: 220px 1fr 220px;
  align-items: center;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--green);
  text-decoration: none;
  font-size: 20px;
  font-weight: 700;
  white-space: nowrap;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 31px;
  height: 31px;
  border-radius: 50%;
  background: var(--green-ink);
  color: #fff;
  font-size: 15px;
  font-family: Georgia, serif;
}

.header-inner nav {
  display: flex;
  justify-content: center;
  align-self: stretch;
  gap: 4px;
}

.header-inner nav a {
  display: flex;
  align-items: center;
  color: #64716a;
  text-decoration: none;
  font-size: 13px;
  padding: 0 14px;
  position: relative;
  white-space: nowrap;
}

.header-inner nav a.active,
.header-inner nav a:hover {
  color: var(--green);
}

.header-inner nav a.active:after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 17px;
  width: 18px;
  height: 2px;
  transform: translateX(-50%);
  background: var(--orange);
}

.header-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 5px;
}

.header-actions>button {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 0;
  background: none;
  color: #64716a;
  cursor: pointer;
  padding: 0;
  position: relative;
  border-radius: 50%;
}

.header-actions>button:hover {
  background: #f0f4ef;
  color: var(--green);
}

.header-actions svg {
  width: 18px;
  height: 18px;
}

.cart i {
  position: absolute;
  right: 1px;
  top: 1px;
  background: var(--orange);
  color: #fff;
  border-radius: 9px;
  font-size: 9px;
  font-style: normal;
  min-width: 14px;
  height: 14px;
  line-height: 14px;
  text-align: center;
}

.header-actions :deep(.el-dropdown) {
  margin-left: 8px;
  padding-left: 12px;
  border-left: 1px solid #e1e7e1;
}

.account {
  display: flex !important;
  align-items: center;
  gap: 8px !important;
  border: 0 !important;
  background: transparent !important;
  color: #1f2a24 !important;
  box-shadow: none !important;
  padding: 0 !important;
  cursor: pointer;
}

.avatar {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--green-ink);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}

.avatar img,
.avatar > span {
  width: 100%;
  height: 100%;
  border-radius: 50%;
}

.avatar img {
  display: block;
  object-fit: cover;
}

.avatar > span {
  display: grid;
  place-items: center;
}

.account-name {
  max-width: 72px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.account svg {
  width: 13px !important;
  color: #829087;
}

.header-actions>button[title]::after {
  content: attr(title);
  position: absolute;
  left: 50%;
  bottom: -31px;
  z-index: 30;
  pointer-events: none;
  transform: translate(-50%, 4px);
  padding: 5px 8px;
  border-radius: 4px;
  background: #1f2f28;
  color: #fff;
  font-size: 11px;
  font-weight: 500;
  line-height: 1;
  white-space: nowrap;
  opacity: 0;
  visibility: hidden;
  transition: opacity .18s ease, transform .18s ease;
}

.header-actions>button[title]:hover::after {
  opacity: 1;
  visibility: visible;
  transform: translate(-50%, 0);
}

main {
  min-height: calc(100vh - 140px);
}

.site-footer {
  min-height: 180px;
  margin: 0;
  padding: 50px max(24px, calc((100% - 1192px)/2));
  background: #edf1eb;
  border-top: 1px solid #dce4dc;
  color: #7a857e;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.site-footer strong {
  color: var(--green);
  margin-right: 14px;
  font-size: 14px;
}

@media (max-width: 800px) {
  .site-header {
    height: 64px;
  }

  .header-inner {
    grid-template-columns: auto 1fr auto;
    gap: 12px;
    padding: 0 16px;
  }

  .header-inner nav {
    justify-content: flex-start;
    overflow: auto;
  }

  .header-inner nav a {
    padding: 0 10px;
    font-size: 12px;
  }

  .account-name,
  .header-actions>button:not(.cart) {
    display: none;
  }

  .header-actions :deep(.el-dropdown) {
    margin-left: 4px;
    padding-left: 8px;
  }

  .site-footer {
    min-height: 150px;
    display: block;
    padding: 38px 18px;
  }

  .site-footer div+div {
    margin-top: 12px;
  }
}
</style>
