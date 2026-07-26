import { getUserInfo } from '../../utils/auth'
import { getSafeArea } from '../../utils/safe-area'
import { addToCart } from '../../api/cart'
import { getMembershipStatus } from '../../api/user'
import {
  generatePlan,
  getCurrentPlan,
  startPlan as apiStartPlan,
  checkPlanMeal,
  swapPlanMeal,
  advancePlan,
  type PlanDetail,
  type PlanMealItem
} from '../../api/plan'
import { parseJsonList, fmtDateTime } from '../../utils/json'

const GEN_PHASES = ['正在分析身体数据…', '正在匹配营养方案…', '正在生成每日餐单…']

/** mealIndex（0 早 / 1 午 / 2 晚）→ 展示文案 */
const MEAL_META = [
  { when: '早餐', en: 'BREAKFAST' },
  { when: '午餐', en: 'LUNCH' },
  { when: '晚餐', en: 'DINNER' }
]

/** 后端 activityLevel 枚举 → 问卷文案 */
const ACTIVITY_TEXT: Record<number, string> = {
  10: '久坐不动型',
  20: '轻度活跃型',
  30: '中度活跃型',
  40: '重度活跃型'
}

interface MealVM {
  mealId: number | string
  when: string
  en: string
  name: string
  kcal: number
  protein: number
  carbs: number
  fat: number
  tags: string[]
  image: string
  done: boolean
  dishId: number | string
}

Page({
  data: {
    padTop: 44,
    genDeg: 0,
    state: 'empty' as 'gen' | 'result' | 'empty',
    // 生成动画
    pct: 0,
    phase: GEN_PHASES[0],
    // 结果
    nickname: '王硕',
    genTime: '',
    basis: '',
    planDays: 7,
    goalDelta: '',
    targetWeight: 0,
    prefsText: '',
    avoidText: '',
    partsText: '',
    started: false,
    finished: false,
    curDay: 0,
    doneCount: 0,
    progressPct: 0,
    days: [] as number[],
    planDay: 0,
    totalKcal: 0,
    meals: [] as MealVM[],
    allDone: false,
    isMember: false
  },

  genTimer: 0 as unknown as ReturnType<typeof setInterval> | null,
  /** 当前计划的全部餐次（接口原始数据，按 dayIndex 过滤渲染） */
  planMeals: [] as PlanMealItem[],
  /** 生成接口异步结果（动画与接口并行） */
  genDone: false,
  genDetail: null as PlanDetail | null,
  genError: null as Error | null,
  skipFirstShow: true,
  /** 问卷跳转带来的目标时长（会员 21 / 非会员 7），仅 from=gen 生效 */
  genPlanDays: undefined as number | undefined,

  onLoad(options: Record<string, string | undefined>) {
    const user = getUserInfo()
    this.setData({
      padTop: getSafeArea().padTop,
      nickname: (user && (user.nickname || user.username)) || '王硕'
    })
    // 会员状态：控制「会员可解锁 21 天完整计划」提示条
    getMembershipStatus()
      .then((m) => this.setData({ isMember: !!(m && m.status === 10) }))
      .catch((error: Error) => console.warn('[plan] 会员状态查询失败：', error && error.message))
    if (options && options.from === 'gen') {
      const pd = Number(options.planDays)
      this.genPlanDays = pd === 21 ? 21 : 7
      this.setData({ state: 'gen', pct: 0, phase: GEN_PHASES[0] })
    } else {
      this.loadPlan(false)
    }
  },

  onReady() {
    if (this.data.state === 'gen') this.startGen()
  },

  onShow() {
    // 首次 onShow 紧跟 onLoad（已在加载），跳过；之后从其他页返回时刷新打卡态
    if (this.skipFirstShow) {
      this.skipFirstShow = false
      return
    }
    if (this.data.state === 'result') this.loadPlan(true)
  },

  onUnload() {
    this.stopGen()
  },

  /* ---------- 拉取当前计划 ---------- */

  loadPlan(keepDay: boolean) {
    getCurrentPlan()
      .then((detail) => {
        if (!detail || !detail.plan) {
          this.setData({ state: 'empty' })
          return
        }
        this.renderDetail(detail, keepDay)
      })
      .catch((error: Error) => {
        console.warn('[plan] 计划加载失败：', error && error.message)
        wx.showToast({ title: '计划加载失败，请稍后重试', icon: 'none' })
      })
  },

  /* ---------- AI 生成动画（圆环为纯 CSS conic-gradient）；动画播放期间真实调 /plan/generate ---------- */

  startGen() {
    this.stopGen()
    this.genDone = false
    this.genDetail = null
    this.genError = null
    generatePlan(this.genPlanDays)
      .then((detail) => { this.genDetail = detail })
      .catch((error: Error) => { this.genError = error })
      .finally(() => { this.genDone = true })
    this.genTimer = setInterval(() => {
      // 接口未返回时动画停在 95%，返回后再补满 100%
      const cap = this.genDone ? 100 : 95
      const pct = Math.min(cap, this.data.pct + 3 + Math.round(Math.random() * 5))
      const phase = pct < 34 ? GEN_PHASES[0] : pct < 67 ? GEN_PHASES[1] : GEN_PHASES[2]
      this.setData({ pct, phase, genDeg: Math.round(pct * 3.6 * 10) / 10 })
      if (pct >= 100 && this.genDone) {
        this.stopGen()
        if (this.genError || !this.genDetail || !this.genDetail.plan) {
          const message = (this.genError && this.genError.message) || '计划生成失败，请稍后重试'
          wx.showToast({ title: message, icon: 'none' })
          this.setData({ state: 'empty' })
          return
        }
        setTimeout(() => this.renderDetail(this.genDetail as PlanDetail, false), 350)
      }
    }, 160)
  },

  stopGen() {
    if (this.genTimer) {
      clearInterval(this.genTimer)
      this.genTimer = null
    }
  },

  /* ---------- 结果页 ---------- */

  /** 用接口返回的 { plan, meals } 整体重渲染；keepDay=true 时保持当前浏览的天（换菜/刷新用） */
  renderDetail(detail: PlanDetail, keepDay: boolean) {
    const plan = detail.plan || {}
    this.planMeals = detail.meals || []
    const started = plan.status === 20
    const finished = plan.status === 30
    const planDays = plan.planDays || 7
    const curDay = plan.curDay || 0
    const targetWeight = Number(plan.targetWeight || 0)
    const delta = Math.abs(Math.round((targetWeight - Number(plan.startWeight || 0)) * 10) / 10)
    const goal = plan.goal || ''
    const planDay = keepDay
      ? Math.min(this.data.planDay, planDays - 1)
      : (started ? curDay : 0)
    this.setData({
      state: 'result',
      genTime: fmtDateTime(plan.createdTime),
      basis: `${ACTIVITY_TEXT[plan.activityLevel || 0] || '日常活动'} · ${goal || '健康'}`,
      planDays,
      goalDelta: `${goal.indexOf('增') >= 0 ? '+' : '-'}${delta}`,
      targetWeight,
      prefsText: parseJsonList(plan.prefs).join('、') || '无',
      avoidText: parseJsonList(plan.avoid).join('、') || '无',
      partsText: parseJsonList(plan.focusParts).join('、') || '无',
      started,
      finished,
      curDay,
      days: Array.from({ length: planDays }, (_, i) => i),
      planDay
    })
    this.buildMeals()
  },

  buildMeals() {
    const { planDay, curDay, started, planDays } = this.data
    const list = this.planMeals
      .filter((m) => (m.dayIndex || 0) === planDay)
      .sort((a, b) => (a.mealIndex || 0) - (b.mealIndex || 0))
    const doneCount = started
      ? this.planMeals.filter((m) => (m.dayIndex || 0) === curDay && m.checked === 1).length
      : 0
    const meals: MealVM[] = list.map((m) => {
      const meta = MEAL_META[m.mealIndex || 0] || MEAL_META[MEAL_META.length - 1]
      return {
        mealId: m.id || 0,
        when: meta.when,
        en: meta.en,
        name: m.dishName || '健康菜品',
        kcal: Number(m.calories || 0),
        protein: Number(m.protein || 0),
        carbs: Number(m.carbs || 0),
        fat: Number(m.fat || 0),
        tags: parseJsonList(m.tags).slice(0, 3),
        image: m.dishImage || '',
        done: m.checked === 1,
        dishId: m.dishId || 0
      }
    })
    const totalKcal = meals.reduce((s, m) => s + m.kcal, 0)
    this.setData({
      meals,
      totalKcal,
      doneCount,
      progressPct: Math.round(((curDay + doneCount / 3) / planDays) * 100),
      allDone: started && planDay === curDay && doneCount === 3
    })
  },

  pickDay(e: WechatMiniprogram.CustomEvent) {
    this.setData({ planDay: Number(e.currentTarget.dataset.day) })
    this.buildMeals()
  },

  /** 开始执行：POST /plan/start（仅未开始可调用，后端校验） */
  startPlan() {
    if (this.data.started || this.data.finished) return
    apiStartPlan()
      .then((detail) => {
        wx.showToast({ title: '计划已开始 · 今天是第 1 天', icon: 'none' })
        this.renderDetail(detail, false)
      })
      .catch((error: Error) => wx.showToast({ title: error.message || '操作失败', icon: 'none' }))
  },

  /**
   * 打卡 / 取消打卡：POST /plan/check { mealId, checked }。
   * 服务端已同步写/删 diet_record（sourceType=30 计划餐），无需前端再调 /health/diet。
   * 重复打卡 / 未打卡取消 → 409，直接 toast 后端 message。
   */
  checkMeal(e: WechatMiniprogram.CustomEvent) {
    const day = Number(e.currentTarget.dataset.day)
    const mealId = e.currentTarget.dataset.mealId as number | string
    const done = !!e.currentTarget.dataset.done
    if (!this.data.started) return
    if (day !== this.data.curDay) {
      wx.showToast({ title: '只能打卡当天餐食', icon: 'none' })
      return
    }
    checkPlanMeal(mealId, !done)
      .then((detail) => {
        wx.showToast({ title: done ? '已取消打卡' : '已打卡 · 同步到饮食记录', icon: 'none' })
        this.renderDetail(detail, true)
      })
      .catch((error: Error) => wx.showToast({ title: error.message || '操作失败', icon: 'none' }))
  },

  /** 换一道：POST /plan/swap { mealId }（已打卡餐次后端 400 拒绝，toast 提示） */
  swapMeal(e: WechatMiniprogram.CustomEvent) {
    const mealId = e.currentTarget.dataset.mealId as number | string
    swapPlanMeal(mealId)
      .then((detail) => {
        wx.showToast({ title: '已换一道菜', icon: 'none' })
        this.renderDetail(detail, true)
      })
      .catch((error: Error) => wx.showToast({ title: error.message || '换菜失败', icon: 'none' }))
  },

  /** 演示入口：长按「执行进度」行触发 /plan/advance 进入下一天（curDay+1，最后一天自动完成计划）。正式环境由后端按日期推进。 */
  advanceDay() {
    if (!this.data.started) return
    advancePlan()
      .then((detail) => {
        wx.showToast({ title: '已进入下一天（演示）', icon: 'none' })
        this.renderDetail(detail, false)
      })
      .catch((error: Error) => wx.showToast({ title: error.message || '操作失败', icon: 'none' }))
  },

  /** 非当天餐卡的加号：加入购物车。dishId 为雪花 id（字符串），原样透传，禁止 Number() 强转 */
  addDish(e: WechatMiniprogram.CustomEvent) {
    const id = e.currentTarget.dataset.id as number | string
    if (id === undefined || id === null || id === '') return
    addToCart(10, id)
      .then(() => wx.showToast({ title: '已加入购物车', icon: 'none' }))
      .catch(() => wx.showToast({ title: '已加入购物车', icon: 'none' }))
  },

  goMall() {
    wx.switchTab({ url: '/pages/food/food' })
  },

  goHome() {
    wx.switchTab({ url: '/pages/index/index' })
  },

  goMember() {
    wx.navigateTo({ url: '/pages/member/member' })
  },

  reCustomize() {
    wx.redirectTo({ url: '/pages/assessment/assessment' })
  },

  goAssessment() {
    wx.navigateTo({ url: '/pages/assessment/assessment' })
  },

  noop() {}
})
