import { getUserInfo } from '../../utils/auth'
import { getSafeArea } from '../../utils/safe-area'
import { addToCart } from '../../api/cart'
import { getDietRecords, getExerciseRecords } from '../../api/health'
import { getProfile } from '../../api/user'
import { getCurrentPlan } from '../../api/plan'
import { getNotices } from '../../api/notice'
import { MOCK_COMBOS, MOCK_DISHES, MOCK_HEALTH, MOCK_MEALS, uimg } from '../../mock/catalog'

const WEEK_EN = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY']
const DEFAULT_TARGET = 2000
const NOTICE_SEEN_KEY = 'bitesmart.notice.seenAt'

/** 计划餐 mealIndex（0 早 / 1 午 / 2 晚）→ 餐单卡文案 */
const PLAN_WHEN = ['早餐', '午餐', '晚餐']
const PLAN_EN = ['BREAKFAST', 'LUNCH', 'DINNER']
const PLAN_IMAGES = [
  '/assets/home/home-banner-3.jpg',
  '/assets/home/home-banner-1.jpg',
  '/assets/home/home-banner-2.jpg'
]

const pad = (n: number): string => (n < 10 ? '0' + n : '' + n)
const todayStr = (): string => {
  const d = new Date()
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

const parseNoticeTime = (value?: string): number => {
  if (!value) return 0
  const normalized = value.includes('T') ? value : value.replace(' ', 'T')
  const time = Date.parse(normalized)
  return Number.isFinite(time) ? time : 0
}

interface RecItem {
  id: number | string
  name: string
  kcal: number
  tags: string
  price: number
  image: string
}

interface PlanMealView {
  when: string
  en: string
  name: string
  status: string
  kcal: number
  hot: boolean
  done: boolean
  hasNext: boolean
  dishId: number | string
  image: string
}

Page({
  data: {
    padTop: 44,
    hasUnreadNotice: false,
    latestNoticeAt: 0,
    heroBanners: [
      '/assets/home/home-banner-1.jpg',
      '/assets/home/home-banner-2.jpg',
      '/assets/home/home-banner-3.jpg'
    ],
    ringDeg: 0,
    dateLine: '',
    greeting: '你好',
    nickname: '王硕',
    initial: '王',
    // 今日热量卡（初始为 mock，真实数据到达后覆盖）
    eaten: 0,
    target: MOCK_HEALTH.target,
    rest: MOCK_HEALTH.target,
    macros: MOCK_HEALTH.macros.map((m) => ({ ...m, pct: Math.round((m.cur / m.goal) * 100) })),
    breakfast: MOCK_HEALTH.breakfast,
    lunch: MOCK_HEALTH.lunch,
    snack: MOCK_HEALTH.snack,
    exerciseBurn: MOCK_HEALTH.exerciseBurn,
    // AI 今日餐单
    planSub: '基于「减脂」目标生成 · 今日餐单',
    planAction: '定制',
    planDayLabel: '第 1 / 7 天',
    planDoneCount: 2,
    planTotal: 3,
    planProgressPct: 67,
    planKcalTotal: MOCK_MEALS.reduce((sum, meal) => sum + meal.kcal, 0),
    meals: MOCK_MEALS.map((meal, index) => ({
      ...meal,
      done: meal.status === '已记录',
      hasNext: index < MOCK_MEALS.length - 1,
      image: PLAN_IMAGES[index % PLAN_IMAGES.length]
    })) as PlanMealView[],
    // 为你推荐
    recs: [] as RecItem[],
    // 健康套餐 hero
    combo: {
      id: MOCK_COMBOS[0].id,
      name: MOCK_COMBOS[0].name,
      en: MOCK_COMBOS[0].en,
      desc: MOCK_COMBOS[0].desc,
      price: MOCK_COMBOS[0].price,
      image: uimg(MOCK_COMBOS[0].img, 800)
    }
  },

  onLoad() {
    const user = getUserInfo()
    if (!user) {
      wx.reLaunch({ url: '/pages/login/login' })
      return
    }
    const now = new Date()
    const hour = now.getHours()
    const greeting = hour < 6 ? '夜深了' : hour < 11 ? '早上好' : hour < 14 ? '中午好' : hour < 18 ? '下午好' : '晚上好'
    this.setData({
      padTop: getSafeArea().padTop,
      dateLine: `${WEEK_EN[now.getDay()]} · ${pad(now.getMonth() + 1)} / ${pad(now.getDate())}`,
      greeting,
      nickname: user.nickname || user.username || '王硕',
      initial: (user.nickname || user.username || '王硕').slice(0, 1),
      recs: [0, 1, 2, 9, 11].map((i) => {
        const d = MOCK_DISHES[i]
        return { id: d.id, name: d.name, kcal: d.kcal, tags: d.tags.join(' · '), price: d.price, image: uimg(d.img, 400) }
      })
    })
    this.loadToday()
    this.loadPlanCard()
    this.loadNoticeBadge()
  },

  onShow() {
    this.loadToday()
    this.loadPlanCard()
    this.loadNoticeBadge()
  },

  loadNoticeBadge() {
    getNotices()
      .then((notices) => {
        const latestAt = notices.reduce((max, item) => {
          const at = parseNoticeTime(String(item.publishTime || item.createTime || ''))
          return at > max ? at : max
        }, 0)
        const seenAt = Number(wx.getStorageSync(NOTICE_SEEN_KEY) || 0)
        this.setData({
          latestNoticeAt: latestAt,
          hasUnreadNotice: latestAt > seenAt
        })
      })
      .catch(() => {
        this.setData({ hasUnreadNotice: false })
      })
  },

  /**
   * AI 今日餐单卡：读 GET /plan/current 真实计划状态。
   * - 进行中（status=20）：显示 curDay 当天三餐 + 打卡态，副标题「第 X / N 天 · 已打卡 Y/3」
   * - 未开始（status=10）：显示第 1 天餐单预览，副标题「专属食谱已生成 · 待开始」
   * - 无计划 / 已结束 / 接口失败：保持静态推荐样式 + 「定制」引导
   */
  loadPlanCard() {
    getCurrentPlan()
      .then((detail) => {
        if (!detail || !detail.plan) return
        const plan = detail.plan
        if (plan.status === 30 || plan.status === 40) {
          this.setData({
            planSub: '计划已完成 · 开启下一份健康餐单',
            planAction: '新计划',
            planDayLabel: `第 ${plan.planDays || 7} / ${plan.planDays || 7} 天`,
            planDoneCount: 3,
            planTotal: 3,
            planProgressPct: 100,
            planKcalTotal: this.data.meals.reduce((sum, meal) => sum + Number(meal.kcal || 0), 0),
            meals: this.data.meals.map((meal) => ({ ...meal, done: true, status: '已记录' }))
          })
          return
        }
        const meals = detail.meals || []
        const running = plan.status === 20
        const day = running ? (plan.curDay || 0) : 0
        const today = meals
          .filter((m) => (m.dayIndex || 0) === day)
          .sort((a, b) => (a.mealIndex || 0) - (b.mealIndex || 0))
        if (!today.length) return
        const doneCount = today.filter((m) => m.checked === 1).length
        const mealViews = today.map((m, index) => ({
          when: PLAN_WHEN[m.mealIndex || 0] || '加餐',
          en: PLAN_EN[m.mealIndex || 0] || 'SNACK',
          name: m.dishName || '健康菜品',
          status: running ? (m.checked === 1 ? '已记录' : '待记录') : '计划餐',
          kcal: Number(m.calories || 0),
          hot: false,
          done: m.checked === 1,
          hasNext: index < today.length - 1,
          dishId: m.dishId || '',
          image: PLAN_IMAGES[index % PLAN_IMAGES.length]
        })) as PlanMealView[]
        this.setData({
          planSub: running
            ? '专属食谱 · 今日餐单'
            : '专属食谱已生成 · 待开始',
          planAction: running ? '查看计划' : '去定制',
          planDayLabel: `第 ${day + 1} / ${plan.planDays || 7} 天`,
          planDoneCount: doneCount,
          planTotal: today.length,
          planProgressPct: Math.round((doneCount / Math.max(today.length, 1)) * 100),
          planKcalTotal: today.reduce((sum, meal) => sum + Number(meal.calories || 0), 0),
          meals: mealViews
        })
      })
      .catch(() => { /* 接口失败保持静态卡 */ })
  },

  /**
   * 今日热量环：真实饮食/运动记录 + 档案每日目标。
   * TODO(B 类)：三大营养素「目标值」后端暂无字段，现按每日热量目标的比例折算
   * （蛋白质 25% / 碳水 50% / 脂肪 25% 热量占比），等后端补 target 列后改读接口。
   */
  loadToday() {
    const date = todayStr()
    Promise.all([getDietRecords(date), getExerciseRecords(date), getProfile().catch(() => null)])
      .then(([diet, exercise, profile]) => {
        const target = (profile && profile.dailyCalorieTarget) || DEFAULT_TARGET
        const sumKcal = (mealType: number) => (diet || []).filter((r) => r.mealType === mealType).reduce((s, r) => s + (r.calories || 0), 0)
        const breakfast = sumKcal(10)
        const lunch = sumKcal(20) + sumKcal(30) // 午餐行合并午/晚两餐展示
        const snack = sumKcal(40)
        const eaten = (diet || []).reduce((s, r) => s + (r.calories || 0), 0)
        const exerciseBurn = (exercise || []).reduce((s, r) => s + (r.caloriesBurned || 0), 0)

        const protein = Math.round((diet || []).reduce((s, r) => s + (r.protein || 0), 0))
        const carbs = Math.round((diet || []).reduce((s, r) => s + (r.carbs || 0), 0))
        const fat = Math.round((diet || []).reduce((s, r) => s + (r.fat || 0), 0))
        const goals = {
          protein: Math.round((target * 0.25) / 4),
          carbs: Math.round((target * 0.5) / 4),
          fat: Math.round((target * 0.25) / 9)
        }
        const macros = this.data.macros.map((m) => {
          const cur = m.key === 'protein' ? protein : m.key === 'carbs' ? carbs : fat
          const goal = m.key === 'protein' ? goals.protein : m.key === 'carbs' ? goals.carbs : goals.fat
          return { ...m, cur, goal, pct: Math.min(100, Math.round((cur / Math.max(goal, 1)) * 100)) }
        })

        // AI 餐单卡由 loadPlanCard() 独立维护（真实计划状态），这里不再混入实际摄入热量

        const ringPct = Math.min(100, Math.round((eaten / target) * 100))
        this.setData({
          target,
          eaten,
          breakfast,
          lunch,
          snack,
          exerciseBurn,
          macros,
          rest: Math.max(0, target - eaten),
          ringDeg: Math.round(ringPct * 3.6 * 10) / 10
        })
      })
      .catch((error: Error) => {
        console.warn('[index] 今日健康数据加载失败，保留初始展示：', error && error.message)
      })
  },

  /* 热量环形图为纯 CSS conic-gradient 实现（见 wxml/scss），无 canvas */

  goNotice() {
    if (this.data.latestNoticeAt) {
      wx.setStorageSync(NOTICE_SEEN_KEY, this.data.latestNoticeAt)
      this.setData({ hasUnreadNotice: false })
    }
    wx.navigateTo({ url: '/pages/notice/notice' })
  },

  goHealth() {
    wx.navigateTo({ url: '/pages/health/health' })
  },

  goAi() {
    wx.switchTab({ url: '/pages/ai/ai' })
  },

  goAssessment() {
    wx.navigateTo({ url: '/pages/assessment/assessment' })
  },

  /** 餐单卡点击进专属计划页（有计划看当天餐单/打卡，无计划显示引导态） */
  goPlan() {
    wx.navigateTo({ url: '/pages/plan/plan' })
  },

  goMall() {
    wx.switchTab({ url: '/pages/food/food' })
  },

  openDish(event: WechatMiniprogram.CustomEvent) {
    const id = String(event.currentTarget.dataset.id || '')
    if (!id) return
    wx.navigateTo({ url: `/pages/food-detail/food-detail?id=${id}` })
  },

  openCombo() {
    wx.navigateTo({ url: `/pages/combo-detail/combo-detail?id=${this.data.combo.id}` })
  },

  addDish(event: WechatMiniprogram.CustomEvent) {
    const id = String(event.currentTarget.dataset.id || '')
    if (!id) return
    addToCart(10, id)
      .then(() => wx.showToast({ title: '已加入购物车', icon: 'none' }))
      .catch(() => wx.showToast({ title: '已加入购物车', icon: 'none' }))
  },

  noop() {}
})
