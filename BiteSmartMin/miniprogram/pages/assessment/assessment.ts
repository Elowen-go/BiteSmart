import {
  ASM_GOALS,
  ASM_ACTS,
  ASM_FREQS,
  ASM_PARTS,
  ASM_PREFS,
  ASM_AVOID,
  PART_OVERLAYS
} from '../../mock/health'
import { uimg } from '../../mock/catalog'
import { saveProfile, getMembershipStatus } from '../../api/user'
import { getSafeArea } from '../../utils/safe-area'

export const ASM_DRAFT_KEY = 'bitesmart_asm_draft'

const STEP_LABELS = ['目标设定', '改善部位', '运动量', '身体档案']

/** 问卷活动量文案 → 后端 activityLevel 枚举（10 久坐 / 20 轻度 / 30 中度 / 40 重度） */
const ACTIVITY_LEVEL: Record<string, number> = {
  久坐不动型: 10,
  轻度活跃型: 20,
  中度活跃型: 30,
  重度活跃型: 40
}
const ACTIVITY_FACTOR: Record<number, number> = { 10: 1.2, 20: 1.375, 30: 1.55, 40: 1.725 }

/** 问卷运动频次要案 → 后端 exerciseFreq 整数（每周次数，取区间代表值） */
const FREQ_VALUE: Record<string, number> = { '0-1 次': 1, '2-3 次': 3, '4-5 次': 5, '6 次以上': 6 }

/** 问卷目标文案 → DB user_profile.health_goal 枚举口径（减肥/增肌/维持/控糖）；问卷展示文案保持不变 */
const GOAL_VALUE: Record<string, string> = { 减重: '减肥', 增肌增重: '增肌', 提升健康水平: '维持', 缓解压力: '维持' }

/** Mifflin-St Jeor 估算每日热量目标（后端 dailyCalorieTarget 为 AI 计算值，这里先给基础估算） */
const calcCalorieTarget = (gender: string, age: number, height: number, weight: number, level: number, goal: string): number => {
  const bmr = 10 * weight + 6.25 * height - 5 * age + (gender === '女' ? -161 : 5)
  let target = bmr * (ACTIVITY_FACTOR[level] || 1.375)
  if (goal === '减肥') target -= 300
  else if (goal === '增肌') target += 300
  return Math.max(1200, Math.round(target))
}

Page({
  data: {
    statusBarHeight: 20,
    menuTop: 26,
    menuH: 32,
    step: 0,
    stepLabels: STEP_LABELS,
    // 问卷数据（默认值抄自原型 asm）
    goal: '减重',
    parts: ['全身'] as string[],
    activity: '中度活跃型',
    freq: '2-3 次',
    gender: '男',
    age: '24',
    height: '178',
    weight: '72.4',
    targetWeight: '70',
    prefs: ['少油少盐'] as string[],
    avoid: ['香菜'] as string[],
    // 选项常量
    goals: ASM_GOALS.map((g) => ({ ...g, image: uimg(g.img, 200) })),
    acts: ASM_ACTS.map((a) => ({ ...a, image: uimg(a.img, 200) })),
    freqs: ASM_FREQS,
    partList: ASM_PARTS.map((n) => ({ n, selected: n === '全身' })),
    prefList: ASM_PREFS.map((n) => ({ n, selected: n === '少油少盐' })),
    avoidList: ASM_AVOID.map((n) => ({ n, selected: n === '香菜' })),
    // 人体模型高亮层
    frontOverlays: [] as string[],
    backOverlays: [] as string[],
    err: ''
  },

  onLoad() {
    const sa = getSafeArea()
    this.setData({ statusBarHeight: sa.statusBarH, menuTop: sa.menuTop, menuH: sa.menuH })
    this.updateOverlays()
  },

  goBack() {
    if (this.data.step > 0) {
      this.setData({ step: this.data.step - 1, err: '' })
    } else {
      wx.navigateBack({ fail: () => wx.switchTab({ url: '/pages/index/index' }) })
    }
  },

  /* ---------- 选项交互 ---------- */

  pickGoal(e: WechatMiniprogram.CustomEvent) {
    this.setData({ goal: e.currentTarget.dataset.v as string })
  },

  pickActivity(e: WechatMiniprogram.CustomEvent) {
    this.setData({ activity: e.currentTarget.dataset.v as string })
  },

  pickFreq(e: WechatMiniprogram.CustomEvent) {
    this.setData({ freq: e.currentTarget.dataset.v as string })
  },

  pickGender(e: WechatMiniprogram.CustomEvent) {
    this.setData({ gender: e.currentTarget.dataset.v as string })
  },

  /** 部位多选（复刻原型 togglePart：全身与其他互斥） */
  togglePart(e: WechatMiniprogram.CustomEvent) {
    const p = e.currentTarget.dataset.v as string
    let parts: string[]
    if (p === '全身') {
      parts = ['全身']
    } else {
      parts = this.data.parts.filter((x) => x !== '全身')
      const i = parts.indexOf(p)
      if (i >= 0) parts.splice(i, 1)
      else parts.push(p)
      if (!parts.length) parts = ['全身']
    }
    const partList = this.data.partList.map((item) => ({ ...item, selected: parts.indexOf(item.n) >= 0 }))
    this.setData({ parts, partList })
    this.updateOverlays()
  },

  updateOverlays() {
    const front: string[] = []
    const back: string[] = []
    this.data.parts.forEach((p) => {
      const o = PART_OVERLAYS[p]
      if (!o) return
      if (o.f) front.push(`/assets/body/${o.f}.png`)
      if (o.b) back.push(`/assets/body/${o.b}.png`)
    })
    this.setData({ frontOverlays: front, backOverlays: back })
  },

  togglePref(e: WechatMiniprogram.CustomEvent) {
    const v = e.currentTarget.dataset.v as string
    const arr = this.data.prefs.slice()
    const i = arr.indexOf(v)
    if (i >= 0) arr.splice(i, 1)
    else arr.push(v)
    const prefList = this.data.prefList.map((item) => ({ ...item, selected: arr.indexOf(item.n) >= 0 }))
    this.setData({ prefs: arr, prefList })
  },

  toggleAvoid(e: WechatMiniprogram.CustomEvent) {
    const v = e.currentTarget.dataset.v as string
    const arr = this.data.avoid.slice()
    const i = arr.indexOf(v)
    if (i >= 0) arr.splice(i, 1)
    else arr.push(v)
    const avoidList = this.data.avoidList.map((item) => ({ ...item, selected: arr.indexOf(item.n) >= 0 }))
    this.setData({ avoid: arr, avoidList })
  },

  onFieldInput(e: WechatMiniprogram.CustomEvent) {
    const field = e.currentTarget.dataset.field as string
    this.setData({ [field]: e.detail.value, err: '' } as Record<string, string>)
  },

  /* ---------- 分步 ---------- */

  next() {
    const { step } = this.data
    if (step < 3) {
      this.setData({ step: step + 1, err: '' })
      return
    }
    // 第 4 步：校验身体数据
    const age = parseFloat(this.data.age)
    const h = parseFloat(this.data.height)
    const w = parseFloat(this.data.weight)
    const tw = parseFloat(this.data.targetWeight)
    if (isNaN(age) || age < 10 || age > 100) {
      this.setData({ err: '请填写正确的年龄（10 - 100）' })
      return
    }
    if (isNaN(h) || h < 100 || h > 250) {
      this.setData({ err: '请填写正确的身高（100 - 250 cm）' })
      return
    }
    if (isNaN(w) || w < 30 || w > 200) {
      this.setData({ err: '请填写正确的体重（30 - 200 kg）' })
      return
    }
    if (isNaN(tw) || tw < 30 || tw > 200) {
      this.setData({ err: '请填写正确的目标体重（30 - 200 kg）' })
      return
    }
    // 存草稿（问卷回显用；计划执行状态以后端 user_plan 为准）
    try {
      wx.setStorageSync(ASM_DRAFT_KEY, {
        goal: this.data.goal,
        parts: this.data.parts,
        activity: this.data.activity,
        freq: this.data.freq,
        gender: this.data.gender,
        age,
        height: h,
        weight: w,
        targetWeight: tw,
        prefs: this.data.prefs,
        avoid: this.data.avoid
      })
    } catch (_) { /* ignore */ }

    // 健康档案落库：PUT /user/profile（含 B 类新字段 targetWeight/exerciseFreq/focusParts）。
    // 必须等落库完成再跳生成页 —— plan?from=gen 会立即调 /plan/generate，后端按档案生成计划。
    // 同时查会员状态：生效会员（status=10）生成 21 天计划，非会员 7 天。
    const level = ACTIVITY_LEVEL[this.data.activity] || 30
    // healthGoal 落库统一为 DB 枚举口径（减肥/增肌/维持/控糖），问卷展示文案保持不变
    const healthGoal = GOAL_VALUE[this.data.goal] || '维持'
    const saveP = saveProfile({
      age,
      gender: this.data.gender === '女' ? 20 : 10,
      height: h,
      weight: w,
      activityLevel: level,
      healthGoal,
      dietPreference: JSON.stringify(this.data.prefs),
      allergyInfo: JSON.stringify(this.data.avoid),
      dailyCalorieTarget: calcCalorieTarget(this.data.gender, age, h, w, level, healthGoal),
      targetWeight: tw,
      exerciseFreq: FREQ_VALUE[this.data.freq] || 3,
      focusParts: JSON.stringify(this.data.parts)
    })
      .catch((error: Error) => {
        console.warn('[assessment] 健康档案云端保存失败，已保留本地草稿：', error && error.message)
        wx.showToast({ title: '档案云端保存失败，已本地保留', icon: 'none' })
      })
    const memberP = getMembershipStatus().catch((error: Error) => {
      console.warn('[assessment] 会员状态查询失败，按非会员处理：', error && error.message)
      return null
    })
    Promise.all([saveP, memberP]).then(([, membership]) => {
      const planDays = membership && membership.status === 10 ? 21 : 7
      wx.navigateTo({ url: `/pages/plan/plan?from=gen&planDays=${planDays}` })
    })
  }
})
