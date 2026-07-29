import { getSafeArea } from '../../utils/safe-area'
import {
  getDriverTasks,
  getPendingTasks,
  pickupDriverTask,
  startDeliveryTask,
  deliverDriverTask,
  rejectDriverTask,
  updateDriverLocation,
  riderTaskStatusText,
  type RiderTask
} from '../../api/delivery'
import { fmtDateTime } from '../../utils/json'

/** 配送费展示值：delivery_task 无费用字段（结算记录在送达后生成），按原型展示 ¥5 */
const FEE_TEXT = '5'

/** 定位上传间隔（毫秒）：配送中每 15 秒上报一次 */
const LOC_INTERVAL = 15000

Page({
  data: {
    menuTop: 26,
    menuH: 32,
    id: '' as number | string,
    found: true,
    orderNo: '',
    pickupCode: '----',
    merchantAddr: '商家门店',
    merchantPhone: '',
    receiver: '',
    receiverInitial: '客',
    receiverPhone: '',
    address: '',
    eta: '',
    remark: '',
    statusText: '',
    fee: FEE_TEXT,
    status: 0,
    // 取货点和收货点坐标（未维护坐标时导航按钮会提示）
    merchantLat: 0,
    merchantLng: 0,
    deliveryLat: 0,
    deliveryLng: 0
  },

  /** 定位上传定时器（仅配送中启动） */
  locTimer: null as ReturnType<typeof setInterval> | null,
  /** 定位授权失败提示只弹一次 */
  locWarned: false,

  onLoad(options: Record<string, string | undefined>) {
    const sa = getSafeArea()
    this.setData({ menuTop: sa.menuTop, menuH: sa.menuH, id: (options && options.id) || '' })
    this.loadTask()
  },

  loadTask() {
    const id = this.data.id
    // 无单任务 GET 端点：先查我的任务，未命中再查大厅（未接单任务从大厅进详情的情况）
    getDriverTasks()
      .then((tasks) => {
        const hit = (tasks || []).find((t) => String(t.id) === String(id))
        if (hit) return hit
        return getPendingTasks().then((pending) =>
          (pending || []).find((t) => String(t.id) === String(id)))
      })
      .then((task: RiderTask | undefined) => {
        if (!task) {
          this.setData({ found: false })
          return
        }
        this.renderTask(task)
      })
      .catch((error: Error) => wx.showToast({ title: error.message || '任务加载失败', icon: 'none' }))
  },

  renderTask(task: RiderTask) {
    this.setData({
      found: true,
      orderNo: task.orderNo || '—',
      pickupCode: task.pickupCode || '----',
      merchantAddr: task.merchantAddress || '商家门店（取货点）',
      merchantPhone: task.merchantPhone || '',
      receiver: task.receiverName || '顾客',
      receiverInitial: (task.receiverName || '客').slice(0, 1),
      receiverPhone: task.receiverPhone || '',
      address: task.deliveryAddress || '—',
      eta: task.estimatedDeliveryTime
        ? fmtDateTime(task.estimatedDeliveryTime)
        : task.taskStatus === 10 ? '待骑手接单' : '—',
      remark: task.orderRemark || '',
      statusText: riderTaskStatusText(task.taskStatus),
      status: task.taskStatus || 0,
      merchantLat: Number(task.merchantLat || 0),
      merchantLng: Number(task.merchantLng || 0),
      deliveryLat: Number(task.deliveryLat || 0),
      deliveryLng: Number(task.deliveryLng || 0)
    })
    this.syncLocationTimer()
  },

  /* ---------- 定位上传（配送中每 15 秒上报；离开页面/送达后停止） ---------- */

  syncLocationTimer() {
    if (this.data.status === 30 || this.data.status === 40) {
      if (this.locTimer) return
      this.uploadLocation()
      this.locTimer = setInterval(() => this.uploadLocation(), LOC_INTERVAL)
    } else {
      this.stopLocationTimer()
    }
  },

  stopLocationTimer() {
    if (this.locTimer) {
      clearInterval(this.locTimer)
      this.locTimer = null
    }
  },

  uploadLocation() {
    const taskId = this.data.id
    if (!taskId) return
    wx.getLocation({
      type: 'gcj02',
      success: (res) => {
        // JSON body { taskId, latitude, longitude }：后端校验任务归属与状态（30/40），失败静默等下一拍
        updateDriverLocation(taskId, res.latitude, res.longitude)
          .catch((error: Error) => console.warn('[r-task] 定位上传失败：', error && error.message))
      },
      fail: () => {
        if (this.locWarned) return
        this.locWarned = true
        wx.showModal({
          title: '需要定位权限',
          content: '配送中需要持续上报位置，请在设置中开启定位权限',
          confirmText: '去开启',
          success: (r) => {
            if (r.confirm) wx.openSetting({})
          }
        })
      }
    })
  },

  onUnload() {
    this.stopLocationTimer()
  },

  onHide() {
    this.stopLocationTimer()
  },

  onShow() {
    if (this.data.found) this.syncLocationTimer()
  },

  /* ---------- 导航（wx.openLocation 调起手机地图 App，无需 key；后续可用 config/amap.ts 的 key 接高德 SDK 做页内路线规划） ---------- */

  navTo(lat: number, lng: number, name: string, address: string) {
    const latitude = Number(lat)
    const longitude = Number(lng)
    if (!Number.isFinite(latitude) || !Number.isFinite(longitude)
      || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180
      || (latitude === 0 && longitude === 0)) {
      wx.showToast({ title: '地址坐标缺失，暂无法导航', icon: 'none' })
      return
    }
    wx.openLocation({ latitude, longitude, name, address, scale: 16 })
  },

  navMerchant() {
    this.navTo(this.data.merchantLat, this.data.merchantLng, '商家门店', this.data.merchantAddr)
  },

  navCustomer() {
    this.navTo(this.data.deliveryLat, this.data.deliveryLng, `${this.data.receiver}（收货地址）`, this.data.address)
  },

  /** 确认取餐：20 → 30（用户端显示“已取餐”） */
  pickup() {
    pickupDriverTask(this.data.id)
      .then(() => {
        wx.showToast({ title: '已取餐，请开始配送', icon: 'none' })
        this.loadTask()
      })
      .catch((error: Error) => wx.showToast({ title: error.message || '操作失败', icon: 'none' }))
  },

  /** 开始配送：30 → 40 */
  startDelivery() {
    startDeliveryTask(this.data.id)
      .then(() => {
        wx.showToast({ title: '已开始配送', icon: 'none' })
        this.loadTask()
      })
      .catch((error: Error) => wx.showToast({ title: error.message || '操作失败', icon: 'none' }))
  },

  /** 确认送达：40 → 50（订单 40→50 完结，商家结算解冻） */
  deliver() {
    deliverDriverTask(this.data.id)
      .then(() => {
        wx.showToast({ title: '已送达，任务完成', icon: 'none' })
        this.loadTask()
      })
      .catch((error: Error) => wx.showToast({ title: error.message || '操作失败', icon: 'none' }))
  },

  callMerchant() {
    if (!this.data.merchantPhone) {
      wx.showToast({ title: '商家电话未登记', icon: 'none' })
      return
    }
    wx.makePhoneCall({ phoneNumber: this.data.merchantPhone })
  },

  callCustomer() {
    if (!this.data.receiverPhone) {
      wx.showToast({ title: '顾客电话未登记', icon: 'none' })
      return
    }
    wx.makePhoneCall({ phoneNumber: this.data.receiverPhone })
  },

  /** 拒单：仅已接单未取餐（status=20）可拒；reason 走 query（后端已实现） */
  reject() {
    wx.showModal({
      title: '拒绝该配送任务？',
      content: '拒单后任务将退回任务大厅',
      confirmText: '确认拒单',
      confirmColor: '#C0563F',
      editable: true,
      placeholderText: '拒单原因（选填）',
      success: (res) => {
        if (!res.confirm) return
        rejectDriverTask(this.data.id, (res.content || '').trim() || '骑手原因无法配送')
          .then(() => {
            wx.showToast({ title: '已拒单', icon: 'none' })
            setTimeout(() => wx.navigateBack({ fail: () => wx.redirectTo({ url: '/pages/r-tasks/r-tasks' }) }), 600)
          })
          .catch((error: Error) => wx.showToast({ title: error.message || '拒单失败', icon: 'none' }))
      }
    })
  },

  goException() {
    wx.navigateTo({ url: `/pages/r-exception/r-exception?id=${this.data.id}` })
  },

  goBack() {
    wx.navigateBack({ fail: () => wx.redirectTo({ url: '/pages/r-tasks/r-tasks' }) })
  },

  noop() {}
})
