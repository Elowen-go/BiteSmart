import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/login/Login.vue'),
      meta: { title: '登录' }
    },
    {
      path: '/admin',
      name: 'Admin',
      component: () => import('../layouts/AdminLayout.vue'),
      meta: { requiresAuth: true, requiresRole: ['ADMIN', '40'] },
      children: [
        {
          path: 'dashboard',
          name: 'AdminDashboard',
          component: () => import('../views/admin/Dashboard.vue'),
          meta: { title: '仪表盘', subtitle: '总览' }
        },
        {
          path: 'orders',
          name: 'AdminOrders',
          component: () => import('../views/admin/orders/OrderList.vue'),
          meta: { title: '订单管理', subtitle: '订单列表' }
        },
        {
          path: 'dishes',
          name: 'AdminDishes',
          component: () => import('../views/admin/dishes/DishList.vue'),
          meta: { title: '菜品管理', subtitle: '菜品列表' }
        },
        {
          path: 'users',
          name: 'AdminUsers',
          component: () => import('../views/admin/users/UserList.vue'),
          meta: { title: '用户管理', subtitle: '用户列表' }
        },
        {
          path: 'merchants',
          name: 'AdminMerchants',
          component: () => import('../views/admin/merchants/MerchantList.vue'),
          meta: { title: '商家管理', subtitle: '商家列表' }
        },
        {
          path: 'drivers',
          name: 'AdminDrivers',
          component: () => import('../views/admin/drivers/DriverList.vue'),
          meta: { title: '配送员管理', subtitle: '配送员列表' }
        },
        {
          path: 'reviews',
          name: 'AdminReviews',
          component: () => import('../views/admin/reviews/ReviewList.vue'),
          meta: { title: '评论管理', subtitle: '评论列表' }
        },
        {
          path: 'system',
          name: 'AdminSystem',
          component: () => import('../views/admin/system/SystemSettings.vue'),
          meta: { title: '系统设置', subtitle: '系统配置' }
        },
        {
          path: 'logs',
          name: 'AdminLogs',
          component: () => import('../views/admin/system/OperationLogs.vue'),
          meta: { title: '日志', subtitle: '操作日志' }
        },
        {
          path: 'ai-rules',
          name: 'AdminAiRules',
          component: () => import('../views/admin/ai-rules/AiRuleList.vue'),
          meta: { title: 'AI规则管理', subtitle: '规则列表' }
        },
        {
          path: 'categories',
          name: 'AdminCategories',
          component: () => import('../views/admin/categories/CategoryList.vue'),
          meta: { title: '分类管理', subtitle: '菜品分类' }
        },
        {
          path: 'nutrition',
          name: 'AdminNutrition',
          component: () => import('../views/admin/nutrition/NutritionList.vue'),
          meta: { title: '营养标准', subtitle: '标准管理' }
        },
        {
          path: 'notices',
          name: 'AdminNotices',
          component: () => import('../views/admin/notices/NoticeList.vue'),
          meta: { title: '公告管理', subtitle: '公告列表' }
        }
      ]
    },
    {
      path: '/merchant',
      name: 'Merchant',
      component: () => import('../layouts/MerchantLayout.vue'),
      meta: { requiresAuth: true, requiresRole: ['MERCHANT', '20'] },
      children: [
        {
          path: 'dashboard',
          name: 'MerchantDashboard',
          component: () => import('../views/merchant/Dashboard.vue'),
          meta: { title: '工作台', subtitle: '总览' }
        },
        {
          path: 'shop',
          name: 'MerchantShop',
          component: () => import('../views/merchant/shop/ShopInfo.vue'),
          meta: { title: '店铺管理', subtitle: '店铺信息' }
        },
        {
          path: 'dishes',
          name: 'MerchantDishes',
          component: () => import('../views/merchant/dishes/DishList.vue'),
          meta: { title: '菜品管理', subtitle: '菜品列表' }
        },
        {
          path: 'combos',
          name: 'MerchantCombos',
          component: () => import('../views/merchant/combos/ComboList.vue'),
          meta: { title: '套餐管理', subtitle: '套餐列表' }
        },
        {
          path: 'orders',
          name: 'MerchantOrders',
          component: () => import('../views/merchant/orders/OrderList.vue'),
          meta: { title: '订单处理', subtitle: '订单列表' }
        },
        {
          path: 'inventory',
          name: 'MerchantInventory',
          component: () => import('../views/merchant/inventory/Inventory.vue'),
          meta: { title: '库存管理', subtitle: '库存预警' }
        },
        {
          path: 'delivery',
          name: 'MerchantDelivery',
          component: () => import('../views/merchant/delivery/Delivery.vue'),
          meta: { title: '配送管理', subtitle: '配送进度' }
        },
        {
          path: 'reviews',
          name: 'MerchantReviews',
          component: () => import('../views/merchant/reviews/ReviewList.vue'),
          meta: { title: '评价管理', subtitle: '评价列表' }
        },
        {
          path: 'categories',
          name: 'MerchantCategories',
          component: () => import('../views/merchant/categories/CategoryList.vue'),
          meta: { title: '分类管理', subtitle: '菜品分类' }
        },
        {
          path: 'statistics',
          name: 'MerchantStatistics',
          component: () => import('../views/merchant/statistics/Statistics.vue'),
          meta: { title: '销售统计', subtitle: '数据报表' }
        },
        {
          path: 'profile',
          name: 'MerchantProfile',
          component: () => import('../views/merchant/profile/Profile.vue'),
          meta: { title: '个人中心', subtitle: '我的信息' }
        }
      ]
    },
    {
      path: '/user',
      name: 'User',
      component: () => import('../layouts/UserLayout.vue'),
      meta: { requiresAuth: true, requiresRole: ['USER', '10'] },
      children: [
        {
          path: '',
          name: 'UserHome',
          component: () => import('../views/user/Home.vue'),
          meta: { title: '首页', subtitle: '推荐' }
        },
        {
          path: 'dishes',
          name: 'UserDishes',
          component: () => import('../views/user/dishes/DishList.vue'),
          meta: { title: '菜品浏览', subtitle: '全部菜品' }
        },
        {
          path: 'cart',
          name: 'UserCart',
          component: () => import('../views/user/cart/Cart.vue'),
          meta: { title: '购物车', subtitle: '已选商品' }
        },
        {
          path: 'orders',
          name: 'UserOrders',
          component: () => import('../views/user/orders/OrderList.vue'),
          meta: { title: '我的订单', subtitle: '订单列表' }
        },
        {
          path: 'delivery',
          name: 'UserDeliveryList',
          component: () => import('../views/user/delivery/DeliveryList.vue'),
          meta: { title: '配送追踪', subtitle: '配送列表' }
        },
        {
          path: 'delivery/:orderId',
          name: 'UserDelivery',
          component: () => import('../views/user/delivery/DeliveryTracking.vue'),
          meta: { title: '配送追踪', subtitle: '实时跟踪' }
        },
        {
          path: 'ai/chat',
          name: 'UserAiChat',
          component: () => import('../views/user/ai/Chat.vue'),
          meta: { title: 'AI对话', subtitle: '智能问答' }
        },
        {
          path: 'ai/recommend',
          name: 'UserAiRecommend',
          component: () => import('../views/user/ai/Recommend.vue'),
          meta: { title: 'AI推荐', subtitle: '个性化食谱' }
        },
        {
          path: 'health',
          name: 'UserHealth',
          component: () => import('../views/user/health/HealthRecord.vue'),
          meta: { title: '健康记录', subtitle: '饮食运动' }
        },
        {
          path: 'profile',
          name: 'UserProfile',
          component: () => import('../views/user/profile/Profile.vue'),
          meta: { title: '个人中心', subtitle: '我的信息' }
        },
        {
          path: 'membership',
          name: 'UserMembership',
          component: () => import('../views/user/membership/Membership.vue'),
          meta: { title: '会员中心', subtitle: '我的会员' }
        },
        {
          path: 'addresses',
          name: 'UserAddresses',
          component: () => import('../views/user/addresses/AddressList.vue'),
          meta: { title: '地址管理', subtitle: '收货地址' }
        },
        {
          path: 'reviews',
          name: 'UserReviews',
          component: () => import('../views/user/reviews/ReviewSubmit.vue'),
          meta: { title: '我的评价', subtitle: '评价管理' }
        },
        {
          path: 'notices',
          name: 'UserNotices',
          component: () => import('../views/user/notices/NoticeList.vue'),
          meta: { title: '系统公告', subtitle: '公告列表' }
        }
      ]
    },
    {
      path: '/',
      redirect: '/login'
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/login'
    }
  ]
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  if (to.meta.title) {
    document.title = `BiteSmart - ${to.meta.title}`
  }
  
  // 需要登录的页面
  if (to.meta.requiresAuth && !userStore.isAuthenticated) {
    next('/login')
    return
  }
  
  // 已登录用户访问登录页，重定向到对应首页
  if (to.path === '/login' && userStore.isAuthenticated) {
    if (userStore.isAdmin) {
      next('/admin/dashboard')
    } else if (userStore.isMerchant) {
      next('/merchant/dashboard')
    } else {
      next('/user')
    }
    return
  }
  
  next()
})

export default router