import { getSafeArea } from '../../utils/safe-area'
import {
  getDriverTasks,
  getPendingTasks,
  acceptDriverTask,
  updateDriverStatus,
  riderTaskStatusText,
  type RiderTask
} from '../../api/delivery'
import { fmtDateTime } from '../../utils/json'

/**
 * 骑手在线状态：后端只有 POST /driver/status（10 在线 / 30 离线），无 GET 查询，
 * 当前状态本地持久化；切换时同步调接口，失败（如未注册为配送员）回滚并 toast。
 */
const RIDER_ONLINE_KEY = 'bitesmart_rider_online'

/** 配送费展示值：delivery_task 无费用字段（结算记录在送达后才生成），按原型展示 ¥5 */
const FEE_TEXT = '5'

export interface RiderTaskVM {
  id: number | string
  orderNo: string
  status: number
  statusText: string
  pickup: string
  deliver: string
  receiver: string
  timeText: string
  fee: string
}

const buildVM = (t: RiderTask): RiderTaskVM => ({
  id: t.id || '',
  orderNo: t.orderNo || '—',
  status: t.taskStatus || 0,
  statusText: riderTaskStatusText(t.taskStatus),
  pickup: t.merchantAddress || '商家门店（取货点）',
  deliver: t.deliveryAddress || '—',
  receiver: t.receiverName || '顾客',
  timeText: fmtDateTime(t.createTime),
  fee: FEE_TEXT
})

Page({
  data: {
    padTop: 44,
    active: 'tasks',
    online: false,
    mine: [] as RiderTaskVM[],
    hall: [] as RiderTaskVM[],
    loadError: ''
  },

  onLoad() {
    this.setData({ padTop: getSafeArea().padTop })
    let online = false
    try {
      online = !!wx.getStorageSync(RIDER_ONLINE_KEY)
    } catch (_) { /* ignore */ }
    this.setData({ online })
    this.loadAll()
  },

  onShow() {
    if (this.data.online) this.loadAll()
  },

  loadAll() {
    getDriverTasks()
      .then((tasks) => {
        const mine = (tasks || [])
          .filter((t) => t.taskStatus === 20 || t.taskStatus === 30 || t.taskStatus === 40)
          .map(buildVM)
        this.setData({ mine, loadError: '' })
      })
      .catch((error: Error) => {
        console.warn('[r-tasks] 我的任务加载失败：', error && error.message)
        this.setData({ loadError: error.message || '任务加载失败' })
      })
    getPendingTasks()
      .then((tasks) => this.setData({ hall: (tasks || []).map(buildVM) }))
      .catch((error: Error) => console.warn('[r-tasks] 任务大厅加载失败：', error && error.message))
  },

  /** 上线/下线：同步 POST /driver/status，失败回滚 */
  toggleOnline() {
    const next = !this.data.online
    updateDriverStatus(next ? 10 : 30)
      .then(() => {
        try {
          wx.setStorageSync(RIDER_ONLINE_KEY, next)
        } catch (_) { /* ignore */ }
        this.setData({ online: next })
        wx.showToast({ title: next ? '已上线，开始接单' : '已下线', icon: 'none' })
        if (next) this.loadAll()
      })
      .catch((error: Error) => wx.showToast({ title: error.message || '状态更新失败', icon: 'none' }))
  },

  /** 抢单：乐观锁，被抢/单满时后端 400，toast message 并刷新大厅 */
  accept(e: WechatMiniprogram.CustomEvent) {
    const id = e.currentTarget.dataset.id as number | string
    acceptDriverTask(id)
      .then(() => {
        wx.showToast({ title: '接单成功，请尽快取餐', icon: 'none' })
        this.loadAll()
      })
      .catch((error: Error) => {
        wx.showToast({ title: error.message || '接单失败', icon: 'none' })
        this.loadAll()
      })
  },

  goTask(e: WechatMiniprogram.CustomEvent) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/r-task/r-task?id=${id}` })
  },

  goTab(e: WechatMiniprogram.CustomEvent) {
    const url = e.currentTarget.dataset.url as string
    const key = e.currentTarget.dataset.key as string
    if (!url || key === this.data.active) return
    wx.redirectTo({ url })
  },

  noop() {}
})
