import {
  addDietRecord,
  deleteDietRecord,
  deleteExerciseRecord,
  getDietRecords,
  getExerciseRecords,
  getWeightRecords,
  saveWeightRecord,
  MEAL_NAME,
  MEAL_EN,
  MEAL_TYPES,
  mealTypeOf,
  type DietRecord,
  type ExerciseRecord,
  type WeightRecord
} from '../../api/health'
import { getProfile } from '../../api/user'
import { getSafeArea, rpx2px } from '../../utils/safe-area'

const DEFAULT_GOAL_WEIGHT = 70 // 档案 targetWeight 缺失时的兜底值（user_profile.target_weight 已上线，onLoad 读档案覆盖）
const DEFAULT_TARGET = 2000
const MEAL_IMAGES: Record<number, string> = {
  10: '/assets/home/home-banner-3.jpg',
  20: '/assets/home/home-banner-1.jpg',
  30: '/assets/home/home-banner-2.jpg',
  40: '/assets/home/home-banner-3.jpg'
}

interface WeightPlotPoint {
  label: string
  x: number
  y: number
}

interface WeightPlotSegment {
  key: string
  style: string
}

interface WeightPlot {
  goalY: number
  points: WeightPlotPoint[]
  segments: WeightPlotSegment[]
}

const buildWeightPlot = (records: WeightRecord[], width: number, height: number, goal: number): WeightPlot => {
  const rows = records.slice(-7).map((record) => ({
    label: String(record.recordDate || '').slice(5),
    value: record.weight || 0
  }))
  if (rows.length < 2) return { goalY: 0, points: [], segments: [] }

  const values = rows.map((row) => row.value)
  const min = Math.min(...values) - 0.4
  const max = Math.max(...values) + 0.4
  const left = 14
  const right = Math.max(left + 1, width - 14)
  const top = 12
  const bottom = Math.max(top + 1, height - 30)
  const xOf = (index: number): number => left + (index * (right - left)) / (rows.length - 1)
  const yOf = (value: number): number => top + ((max - value) / (max - min)) * (bottom - top)
  const points = rows.map((row, index) => ({ label: row.label, x: xOf(index), y: yOf(row.value) }))
  const segments = points.slice(1).map((point, index) => {
    const start = points[index]
    const dx = point.x - start.x
    const dy = point.y - start.y
    const length = Math.sqrt(dx * dx + dy * dy)
    const angle = Math.atan2(dy, dx) * (180 / Math.PI)
    return {
      key: `${index}-${point.label}`,
      style: `left: ${start.x}px; top: ${start.y}px; width: ${length}px; transform: rotate(${angle}deg);`
    }
  })
  return {
    goalY: Math.max(top, Math.min(bottom, yOf(goal))),
    points,
    segments
  }
}

const pad = (n: number): string => (n < 10 ? '0' + n : '' + n)

/** dateOffset（0 = 今天）→ yyyy-MM-dd */
const dateStrOf = (offset: number): string => {
  const d = new Date(Date.now() + offset * 86400000)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

const dateOffsetOf = (value: string): number => {
  const parts = value.split('-').map((item) => Number(item))
  if (parts.length !== 3 || parts.some((item) => !Number.isFinite(item))) return 0
  const selected = new Date(parts[0], parts[1] - 1, parts[2])
  const today = new Date()
  const todayStart = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  return Math.round((selected.getTime() - todayStart.getTime()) / 86400000)
}

/** 运动名称 → 单字图标（exerciseType 为自由文本，按关键字推断） */
const exCharOf = (name: string): string => {
  if (/跑/.test(name)) return '跑'
  if (/走|行/.test(name)) return '走'
  if (/骑/.test(name)) return '骑'
  if (/游/.test(name)) return '游'
  if (/力|哑铃|器械/.test(name)) return '力'
  if (/瑜/.test(name)) return '瑜'
  return '动'
}

Page({
  data: {
    statusBarHeight: 20,
    menuTop: 26,
    menuH: 32,
    ringDeg: 0,
    chartW: 300,
    chartH: 120,
    dateOffset: 0,
    dateMain: '今天',
    dateSub: '',
    todayDate: '',
    pickerDate: '',
    // 总览
    eaten: 0,
    target: DEFAULT_TARGET,
    burned: 0,
    net: 0,
    mealCount: 0,
    curWeight: 0,
    breakfastKcal: 0,
    lunchKcal: 0,
    dinnerKcal: 0,
    // tab
    tab: 'diet' as 'diet' | 'exercise' | 'weight',
    // 饮食
    dietGroups: [] as { meal: string; en: string; list: { id: number | string; name: string; kcal: number }[] }[],
    mealSlots: [] as { meal: string; en: string; image: string }[],
    // 运动
    exMins: 0,
    exDist: 0,
    weekTotal: 0,
    weekBars: [] as { label: string; v: number; h: number; today: boolean }[],
    exList: [] as { id: number | string; ch: string; name: string; meta: string; kcal: number }[],
    // 体重
    weightList: [] as { d: string; w: number }[],
    weightPlot: { goalY: 0, points: [] as WeightPlotPoint[], segments: [] as WeightPlotSegment[] },
    goalWeight: DEFAULT_GOAL_WEIGHT,
    // 弹层表单
    sheetShow: false,
    sheetKind: 'diet' as 'diet' | 'weight',
    formMeal: '早餐',
    mealOptions: MEAL_TYPES.map((t) => MEAL_NAME[t]),
    formName: '',
    formKcal: '',
    errName: false,
    errKcal: false
  },

  weightAll: [] as WeightRecord[],

  onLoad() {
    const sa = getSafeArea()
    this.setData({
      statusBarHeight: sa.statusBarH,
      menuTop: sa.menuTop,
      menuH: sa.menuH,
      chartW: rpx2px(606),
      chartH: rpx2px(240),
      todayDate: dateStrOf(0),
      pickerDate: dateStrOf(0)
    })
    // 每日热量目标与目标体重来自健康档案（user_profile.daily_calorie_target / target_weight），失败时保留默认值
    getProfile()
      .then((profile) => {
        if (!profile) return
        const patch: { target?: number; goalWeight?: number } = {}
        if (profile.dailyCalorieTarget) patch.target = profile.dailyCalorieTarget
        if (profile.targetWeight) patch.goalWeight = profile.targetWeight
        if (Object.keys(patch).length) this.setData(patch)
      })
      .catch(() => {})
  },

  onShow() {
    this.refresh()
  },

  /** 拉取当前浏览日期的饮食/运动 + 近 7 天运动 + 体重历史，刷新视图 */
  refresh() {
    const date = dateStrOf(this.data.dateOffset)
    const weekDates: string[] = []
    for (let i = 6; i >= 1; i -= 1) weekDates.push(dateStrOf(this.data.dateOffset - i))

    const weightReq = getWeightRecords().then((result) => {
      const list = Array.isArray(result) ? result : result.list || []
      this.weightAll = (list || []).slice().sort((a, b) => String(a.recordDate || '').localeCompare(String(b.recordDate || '')))
    })

    Promise.all([
      getDietRecords(date),
      getExerciseRecords(date),
      Promise.all(weekDates.map((d) => getExerciseRecords(d).catch(() => [] as ExerciseRecord[]))),
      weightReq
    ])
      .then(([diet, exercise, weekLists]) => {
        this.renderAll(date, diet || [], exercise || [], weekLists.map((l) => l || []))
      })
      .catch(() => {
        wx.showToast({ title: '健康数据加载失败，请稍后重试', icon: 'none' })
      })
  },

  renderAll(date: string, diet: DietRecord[], exercise: ExerciseRecord[], weekLists: ExerciseRecord[][]) {
    const d = new Date(date.replace(/-/g, '/'))
    const dstr = `${pad(d.getMonth() + 1)} 月 ${pad(d.getDate())} 日`

    // 饮食：按 mealType 枚举分组
    const eaten = diet.reduce((s, x) => s + (x.calories || 0), 0)
    const dietGroups = MEAL_TYPES.map((t) => ({
      meal: MEAL_NAME[t],
      en: MEAL_EN[t],
      list: diet
        .filter((r) => r.mealType === t)
        .map((r) => ({ id: r.id || 0, name: r.foodName || '未命名', kcal: r.calories || 0 }))
    })).filter((g) => g.list.length > 0)
    const kcalOf = (mealType: number): number =>
      diet.filter((r) => r.mealType === mealType).reduce((s, r) => s + (r.calories || 0), 0)
    const mealSlots = MEAL_TYPES.map((t) => ({ meal: MEAL_NAME[t], en: MEAL_EN[t], image: MEAL_IMAGES[t] }))

    // 运动
    const burned = exercise.reduce((s, x) => s + (x.caloriesBurned || 0), 0)
    const exMins = exercise.reduce((s, x) => s + (x.duration || 0), 0)
    const exDist = Math.round(exercise.reduce((s, x) => s + (x.distance || 0), 0) * 10) / 10
    const exList = exercise.map((r) => {
      const name = r.exerciseType || '运动'
      return {
        id: r.id || 0,
        ch: exCharOf(name),
        name,
        meta: `${r.duration || 0} 分钟${r.distance ? ' · ' + r.distance + ' km' : ''}${r.remark ? ' · ' + r.remark : ''}`,
        kcal: r.caloriesBurned || 0
      }
    })

    // 周柱状图：前 6 天 + 当天
    const days = weekLists.map((list) => list.reduce((s, x) => s + (x.caloriesBurned || 0), 0)).concat([burned])
    const max = Math.max(...days, 1)
    const weekBars = days.map((v, i) => {
      const dd = new Date(d.getTime() - (6 - i) * 86400000)
      return { label: i === 6 ? '今天' : pad(dd.getDate()), v, h: Math.max(6, Math.round((v / max) * 124)), today: i === 6 }
    })
    const weekTotal = days.reduce((s, x) => s + x, 0)

    // 体重
    const ws = this.weightAll.filter((x) => String(x.recordDate || '') <= date)
    const currentWeightRecord = ws.slice(-1)[0]
    const curWeight = currentWeightRecord ? currentWeightRecord.weight || 0 : 0
    const weightList = ws.slice(-3).reverse().map((x) => ({ d: String(x.recordDate || '').slice(5), w: x.weight || 0 }))
    const weightPlot = buildWeightPlot(ws, this.data.chartW, this.data.chartH, this.data.goalWeight)

    this.setData({
      dateMain: this.data.dateOffset === 0 ? '今天' : dstr,
      dateSub: dstr,
      eaten,
      burned,
      net: eaten - burned,
      ringDeg: Math.round(Math.min(100, Math.round((eaten / this.data.target) * 100)) * 3.6 * 10) / 10,
      mealCount: diet.length,
      curWeight,
      breakfastKcal: kcalOf(10),
      lunchKcal: kcalOf(20),
      dinnerKcal: kcalOf(30),
      dietGroups,
      mealSlots,
      exMins,
      exDist,
      weekBars,
      weekTotal,
      exList,
      weightList,
      weightPlot
    })
  },

  /* ---------- 顶部导航 ---------- */

  goBack() {
    wx.navigateBack({ fail: () => wx.switchTab({ url: '/pages/index/index' }) })
  },

  prevDay() {
    const dateOffset = this.data.dateOffset - 1
    this.setData({ dateOffset, pickerDate: dateStrOf(dateOffset) }, () => this.refresh())
  },

  nextDay() {
    if (this.data.dateOffset >= 0) return
    const dateOffset = this.data.dateOffset + 1
    this.setData({ dateOffset, pickerDate: dateStrOf(dateOffset) }, () => this.refresh())
  },

  onDatePick(e: WechatMiniprogram.CustomEvent) {
    const value = String(e.detail.value || '')
    if (!value) return
    const dateOffset = Math.min(0, dateOffsetOf(value))
    this.setData({ dateOffset, pickerDate: dateStrOf(dateOffset) }, () => this.refresh())
  },

  switchTab(e: WechatMiniprogram.CustomEvent) {
    const tab = e.currentTarget.dataset.tab as 'diet' | 'exercise' | 'weight'
    this.setData({ tab })
  },

  /* ---------- 记录删除（DELETE /health/diet|exercise/{id}） ---------- */

  delDiet(e: WechatMiniprogram.CustomEvent) {
    // 记录 id 为雪花字符串，禁止 Number() 强转；'0' 为 id 缺失的占位
    const id = String(e.currentTarget.dataset.id || '')
    if (!id || id === '0') return
    deleteDietRecord(id)
      .then(() => {
        wx.showToast({ title: '已删除', icon: 'none' })
        this.refresh()
      })
      .catch((error: Error) => wx.showToast({ title: error.message || '删除失败', icon: 'none' }))
  },

  delExercise(e: WechatMiniprogram.CustomEvent) {
    const id = String(e.currentTarget.dataset.id || '')
    if (!id || id === '0') return
    deleteExerciseRecord(id)
      .then(() => {
        wx.showToast({ title: '已删除', icon: 'none' })
        this.refresh()
      })
      .catch((error: Error) => wx.showToast({ title: error.message || '删除失败', icon: 'none' }))
  },

  /* ---------- 添加表单 ---------- */

  healthAdd(e?: WechatMiniprogram.CustomEvent) {
    if (this.data.tab === 'exercise') {
      wx.navigateTo({ url: '/pages/exercise/exercise' })
      return
    }
    const meal = e && e.currentTarget && e.currentTarget.dataset && e.currentTarget.dataset.meal
    const formMeal = typeof meal === 'string' && meal ? meal : '早餐'
    this.setData({
      sheetShow: true,
      sheetKind: this.data.tab === 'weight' ? 'weight' : 'diet',
      formMeal,
      formName: '',
      formKcal: '',
      errName: false,
      errKcal: false
    })
  },

  openWeightForm() {
    this.healthAdd()
  },

  closeSheet() {
    this.setData({ sheetShow: false })
  },

  pickMeal(e: WechatMiniprogram.CustomEvent) {
    this.setData({ formMeal: e.currentTarget.dataset.meal as string })
  },

  onNameInput(e: WechatMiniprogram.CustomEvent) {
    this.setData({ formName: e.detail.value as string, errName: false })
  },

  onKcalInput(e: WechatMiniprogram.CustomEvent) {
    this.setData({ formKcal: e.detail.value as string, errKcal: false })
  },

  saveForm() {
    const { sheetKind, formMeal, formName, formKcal } = this.data
    const v = parseFloat(formKcal)
    if (sheetKind === 'diet') {
      const errName = !formName.trim()
      const errKcal = isNaN(v) || v < 1 || v > 3000
      if (errName || errKcal) {
        this.setData({ errName, errKcal })
        return
      }
      // 饮食记录记到当前浏览日期，sourceType 20 = 手动记录
      addDietRecord({
        recordDate: dateStrOf(this.data.dateOffset),
        mealType: mealTypeOf(formMeal),
        foodName: formName.trim(),
        calories: Math.round(v),
        sourceType: 20
      })
        .then(() => {
          this.setData({ sheetShow: false })
          wx.showToast({ title: '记录成功', icon: 'none' })
          this.refresh()
        })
        .catch((error: Error) => wx.showToast({ title: error.message || '保存失败', icon: 'none' }))
    } else {
      const errKcal = isNaN(v) || v < 30 || v > 200
      if (errKcal) {
        this.setData({ errKcal })
        return
      }
      // 体重为每日一条 upsert，保存到当前浏览日期
      saveWeightRecord({ recordDate: dateStrOf(this.data.dateOffset), weight: Math.round(v * 10) / 10 })
        .then(() => {
          this.setData({ sheetShow: false })
          wx.showToast({ title: '记录成功', icon: 'none' })
          this.refresh()
        })
        .catch((error: Error) => wx.showToast({ title: error.message || '保存失败', icon: 'none' }))
    }
  },

  goExerciseLib() {
    wx.navigateTo({ url: '/pages/exercise/exercise' })
  },

  noop() {}
})
