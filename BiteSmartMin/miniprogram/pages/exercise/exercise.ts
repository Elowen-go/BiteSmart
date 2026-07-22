import { EX_LIB, type ExLibItem } from '../../mock/health'
import {
  addExerciseRecord,
  getWeightRecords,
  getExerciseLibrary,
  type WeightRecord,
  type ExerciseLibraryItem
} from '../../api/health'
import { uimg } from '../../mock/catalog'
import { getSafeArea } from '../../utils/safe-area'
import { parseJsonList } from '../../utils/json'

const CATS = [
  { k: 'hot', n: '常用' },
  { k: 'aerobic', n: '有氧' },
  { k: 'strength', n: '力量' },
  { k: 'shape', n: '塑形' },
  { k: 'yoga', n: '瑜伽' }
]

/** 运动库视图模型：id 允许 string（后端雪花 id 以字符串传输，禁止 Number() 强转） */
interface ExVM {
  id: number | string
  cat: string
  hot?: number
  name: string
  std: string
  per: number // kcal / 分钟
  defMins: number
  defDist: number
  image: string
  tags: string[]
}

/** 本地常量兜底（C 类）：接口失败时保证页面可用 */
const fromMock = (e: ExLibItem): ExVM => ({
  id: e.id,
  cat: e.cat,
  hot: e.hot,
  name: e.name,
  std: e.std,
  per: e.per,
  defMins: e.defMins,
  defDist: e.defDist,
  image: uimg(e.img, 200),
  tags: e.tags
})

/** 后端 ExerciseLibrary 字段（stdText/kcalPerMin/defMins/defDist/imageUrl，tags 为 JSON 字符串）→ 视图模型 */
const fromRemote = (e: ExerciseLibraryItem): ExVM => ({
  id: e.id || 0,
  cat: e.category || 'aerobic',
  hot: e.hot,
  name: e.name || '运动',
  std: e.stdText || '',
  per: Number(e.kcalPerMin || 0),
  defMins: Number(e.defMins || 30),
  defDist: Number(e.defDist || 0),
  image: e.imageUrl ? (e.imageUrl.indexOf('http') === 0 ? e.imageUrl : uimg(e.imageUrl, 200)) : '',
  tags: parseJsonList(e.tags)
})

const pad = (n: number): string => (n < 10 ? '0' + n : '' + n)
const todayStr = (): string => {
  const d = new Date()
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

Page({
  data: {
    statusBarHeight: 20,
    menuTop: 26,
    menuH: 32,
    curWeight: 0,
    cats: CATS,
    cat: 'hot',
    list: [] as ExVM[],
    // 参数弹层
    sheetShow: false,
    cur: null as ExVM | null,
    mins: 0,
    dist: 0,
    kcal: 0,
    // 自定义弹层
    customShow: false,
    cName: '',
    cMins: '',
    cKcal: '',
    errName: false,
    errMins: false,
    errKcal: false,
    saving: false
  },

  lib: [] as ExVM[],

  onLoad() {
    const sa = getSafeArea()
    this.setData({ statusBarHeight: sa.statusBarH, menuTop: sa.menuTop, menuH: sa.menuH })
    // 先用本地常量渲染，再拉真实运动库（GET /exercise/library），失败保留常量兜底
    this.lib = EX_LIB.map(fromMock)
    this.buildList()
    getExerciseLibrary()
      .then((list) => {
        if (list && list.length) {
          this.lib = list.map(fromRemote)
          this.buildList()
        }
      })
      .catch((error: Error) => {
        console.warn('[exercise] 运动库接口失败，使用本地常量：', error && error.message)
      })
  },

  onShow() {
    // 当前体重取最新一条真实体重记录
    getWeightRecords()
      .then((result) => {
        const list: WeightRecord[] = Array.isArray(result) ? result : result.list || []
        if (list && list.length) {
          const sorted = list.slice().sort((a, b) => String(a.recordDate || '').localeCompare(String(b.recordDate || '')))
          this.setData({ curWeight: sorted[sorted.length - 1].weight || 0 })
        }
      })
      .catch(() => {})
  },

  buildList() {
    const cat = this.data.cat
    const list = this.lib.filter((e) => (cat === 'hot' ? e.hot : e.cat === cat))
    this.setData({ list })
  },

  goBack() {
    wx.navigateBack({ fail: () => wx.switchTab({ url: '/pages/index/index' }) })
  },

  pickCat(e: WechatMiniprogram.CustomEvent) {
    this.setData({ cat: e.currentTarget.dataset.k as string })
    this.buildList()
  },

  /* ---------- 参数弹层 ---------- */

  openSheet(e: WechatMiniprogram.CustomEvent) {
    const id = e.currentTarget.dataset.id as number | string
    const ex = this.lib.find((x) => String(x.id) === String(id))
    if (!ex) return
    this.setData({
      sheetShow: true,
      cur: ex,
      mins: ex.defMins,
      dist: ex.defDist,
      kcal: ex.defMins * ex.per
    })
  },

  closeSheet() {
    this.setData({ sheetShow: false })
  },

  exStep(e: WechatMiniprogram.CustomEvent) {
    const field = e.currentTarget.dataset.field as 'mins' | 'dist'
    const delta = Number(e.currentTarget.dataset.delta)
    const cur = this.data.cur
    if (!cur) return
    let { mins, dist } = this.data
    if (field === 'mins') mins = Math.max(0, Math.min(600, mins + delta))
    else dist = Math.max(0, Math.min(100, Math.round((dist + delta) * 10) / 10))
    this.setData({ mins, dist, kcal: mins * cur.per })
  },

  /** 提交 POST /health/exercise（exerciseType 自由文本 / caloriesBurned） */
  saveRecord(name: string, mins: number, dist: number, kcal: number, remark: string) {
    if (this.data.saving) return
    this.setData({ saving: true })
    addExerciseRecord({
      recordDate: todayStr(),
      exerciseType: name,
      duration: mins,
      distance: dist,
      caloriesBurned: kcal,
      remark
    })
      .then(() => {
        this.setData({ sheetShow: false, customShow: false })
        wx.showToast({ title: '已添加运动记录', icon: 'none' })
      })
      .catch((error: Error) => wx.showToast({ title: error.message || '保存失败', icon: 'none' }))
      .finally(() => this.setData({ saving: false }))
  },

  confirmAdd() {
    const cur = this.data.cur
    if (!cur) return
    if (this.data.mins < 1) {
      wx.showToast({ title: '请填写运动时长', icon: 'none' })
      return
    }
    this.saveRecord(cur.name, this.data.mins, this.data.dist, this.data.mins * cur.per, '')
  },

  /* ---------- 自定义运动 ---------- */

  openCustom() {
    this.setData({ customShow: true, cName: '', cMins: '', cKcal: '', errName: false, errMins: false, errKcal: false })
  },

  closeCustom() {
    this.setData({ customShow: false })
  },

  onCName(e: WechatMiniprogram.CustomEvent) {
    this.setData({ cName: e.detail.value as string, errName: false })
  },

  onCMins(e: WechatMiniprogram.CustomEvent) {
    this.setData({ cMins: e.detail.value as string, errMins: false })
  },

  onCKcal(e: WechatMiniprogram.CustomEvent) {
    this.setData({ cKcal: e.detail.value as string, errKcal: false })
  },

  saveCustom() {
    const { cName, cMins, cKcal } = this.data
    const m = parseFloat(cMins)
    const v = parseFloat(cKcal)
    const errName = !cName.trim()
    const errMins = isNaN(m) || m < 1 || m > 600
    const errKcal = isNaN(v) || v < 1 || v > 2000
    if (errName || errMins || errKcal) {
      this.setData({ errName, errMins, errKcal })
      return
    }
    this.saveRecord(cName.trim(), Math.round(m), 0, Math.round(v), '自定义')
  },

  noop() {}
})
