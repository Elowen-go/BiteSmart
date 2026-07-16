import { clearAuth, getUserInfo, type MiniUserInfo } from '../../utils/auth'
import { getRoleConfig, type RoleConfig } from '../../utils/role'

const foodImages = {
  bowl: 'https://images.unsplash.com/photo-1547592180-85f173990554?w=900&q=80',
  salad: 'https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=600&q=80',
  salmon: 'https://images.unsplash.com/photo-1467003909585-2f8a72700288?w=600&q=80',
  grain: 'https://images.unsplash.com/photo-1516684669134-de6f7c473a2a?w=600&q=80'
}

Component({
  data: {
    user: null as MiniUserInfo | null,
    initials: '',
    role: null as RoleConfig | null,
    currentTab: 0,
    mealTab: 1,
    hasWeightHistory: false,
    topbarStyle: '',
    images: foodImages,
    notice: '今日低卡菜单已更新，午餐别忘了留一点时间给自己。',
    menuItems: [
      { name: '青柠香煎鸡胸', desc: '高蛋白 · 低脂 · 现做', price: '29', kcal: '426 kcal', image: foodImages.salad },
      { name: '照烧三文鱼谷物碗', desc: '优质脂肪 · 饱腹组合', price: '36', kcal: '508 kcal', image: foodImages.salmon },
      { name: '南瓜藜麦暖沙拉', desc: '膳食纤维 · 当季蔬菜', price: '25', kcal: '318 kcal', image: foodImages.grain }
    ],
    merchantStats: [
      { label: '待处理订单', value: '12', tone: 'hot' }, { label: '今日营业额', value: '¥2,486', tone: 'warm' }, { label: '出餐准时率', value: '96%', tone: 'cool' }
    ],
    deliveryTasks: [
      { id: 'BS26071608', shop: '禾下厨房', address: '滨江创意园 A 座', distance: '1.8 km', state: '待取餐', time: '预计 18:42 前送达' },
      { id: 'BS26071603', shop: '轻食研究所', address: '青禾里 2 号楼', distance: '3.2 km', state: '配送中', time: '还有 16 分钟' }
    ]
  },
  lifetimes: {
    attached() {
      const windowInfo = wx.getSystemInfoSync()
      const menuButton = wx.getMenuButtonBoundingClientRect()
      const topHeight = Math.max(menuButton.bottom + 12, windowInfo.statusBarHeight + 56)
      const user = getUserInfo()
      const role = getRoleConfig(user?.roleType)
      if (!user || !role) {
        wx.reLaunch({ url: '/pages/login/login' })
        return
      }
      this.setData({ user, role, initials: (user.nickname || user.username || 'B').slice(0, 1), topbarStyle: `height:${topHeight}px;padding-top:${windowInfo.statusBarHeight}px;` })
    }
  },
  methods: {
    selectTab(event: WechatMiniprogram.CustomEvent) {
      const index = Number(event.currentTarget.dataset.index)
      if (this.data.role?.type === 10 && index === 2) {
        wx.navigateTo({ url: '/pages/food/food' })
        return
      }
      this.setData({ currentTab: index })
    },
    selectMeal(event: WechatMiniprogram.CustomEvent) {
      this.setData({ mealTab: Number(event.currentTarget.dataset.index) })
    },
    openLogin() { wx.reLaunch({ url: '/pages/login/login' }) },
    logout() {
      clearAuth()
      wx.reLaunch({ url: '/pages/login/login' })
    },
    addDish(event: WechatMiniprogram.CustomEvent) {
      wx.showToast({ title: `${event.currentTarget.dataset.name} 已加入`, icon: 'none' })
    },
    action(event: WechatMiniprogram.CustomEvent) {
      const label = String(event.currentTarget.dataset.label || '')
      const routes: Record<string, string> = {
        '打开 AI 对话': '/pages/chat/chat',
        '打开全部菜单': '/pages/food/food',
        '查看全部菜单': '/pages/food/food',
        '健康套餐': '/pages/combo/combo',
        '设置我的食谱计划': '/pages/food/food',
        '新建记录': '/pages/health/health',
        '会员权益': '/pages/member/member',
        '查看配送进度': '/pages/delivery/delivery'
      }
      if (routes[label]) {
        wx.navigateTo({ url: routes[label] })
        return
      }
      wx.showToast({ title: label, icon: 'none' })
    }
  }
})
