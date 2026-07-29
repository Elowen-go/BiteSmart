import { getTracking, type TrackingInfo } from '../../api/delivery'
import { getSafeArea } from '../../utils/safe-area'
import { requireUser } from '../../utils/user-route'

interface TlStep { text: string; time: string; cls: string }

interface MapMarker {
  id: number
  latitude: number
  longitude: number
  iconPath: string
  width: number
  height: number
  callout?: { content: string; color: string; fontSize: number; borderRadius: number; bgColor: string; padding: number; display: string }
}
interface MapPolyline { points: { latitude: number; longitude: number }[]; color: string; width: number }

interface TrackView {
  statusText: string
  driverName: string
  driverInitial: string
  vehicle: string
  phone: string
  hasDriver: boolean
  pickupCode: string
  eta: string
  progress: number
  steps: TlStep[]
  // 真实地图（有骑手坐标时渲染原生 map；无坐标保持 CSS 示意视图）
  hasLocation: boolean
  mapLat: number
  mapLng: number
  markers: MapMarker[]
  polylines: MapPolyline[]
}

/** 后端 delivery_task.task_status 真实枚举：10 待接单 / 20 待取餐 / 30 已取餐 / 40 配送中 / 50 已送达 / 60 异常 / 70 已取消 */
const statusTextOf = (taskStatus?: number): string => {
  const map: Record<number, string> = { 10: '待接单', 20: '骑手待取餐', 30: '已取餐', 40: '配送中', 50: '已送达', 60: '配送异常', 70: '已取消' }
  return (taskStatus != null && map[taskStatus]) || '配送中'
}

const progressOf = (taskStatus?: number): number => {
  if (taskStatus == null) return 50
  if (taskStatus <= 10) return 12
  if (taskStatus === 20) return 30
  if (taskStatus === 30) return 55
  if (taskStatus === 40) return 75
  return 96
}

const buildView = (tracking: TrackingInfo): TrackView => {
  const driver = tracking.driver || {}
  const driverName = driver.realName || ''
  // 后端 delivery_driver.vehicle_type 真实枚举：10 电动车 / 20 自行车 / 30 汽车
  const vehicleMap: Record<number, string> = { 10: '电动车', 20: '自行车', 30: '汽车' }
  const status = tracking.taskStatus || 0
  const steps: TlStep[] = [
    { text: '骑手已接单', time: '', cls: status >= 20 ? 'done' : '' },
    { text: '到店取餐，配送中', time: tracking.pickupTime || '', cls: (tracking.pickupTime || status >= 30) ? 'done' : '' },
    { text: '已送达，请取餐', time: tracking.deliverTime || '', cls: (tracking.deliverTime || status >= 50) ? 'done' : '' }
  ]
  const doing = steps.findIndex((s) => !s.cls)
  if (doing >= 0) steps[doing].cls = 'now'

  // 真实坐标：顶层 riderLat/riderLng（后端最终契约）优先，driver.currentLat/Lng 与任务快照兜底
  const riderLat = Number(tracking.riderLat || (driver.currentLat != null ? driver.currentLat : tracking.currentLat) || 0)
  const riderLng = Number(tracking.riderLng || (driver.currentLng != null ? driver.currentLng : tracking.currentLng) || 0)
  const hasLocation = !!(riderLat && riderLng)
  const markers: MapMarker[] = []
  const polylines: MapPolyline[] = []
  if (hasLocation) {
    const hm = String(tracking.locTime || tracking.locationUpdateTime || '').slice(11, 16)
    markers.push({
      id: 1,
      latitude: riderLat,
      longitude: riderLng,
      iconPath: '/assets/map/rider-dot.png',
      width: 30,
      height: 30,
      callout: {
        content: hm ? `骑手 · 更新于 ${hm}` : '骑手',
        color: '#17251F',
        fontSize: 11,
        borderRadius: 8,
        bgColor: '#FFFFFF',
        padding: 6,
        display: 'ALWAYS'
      }
    })
    // 商家/顾客坐标（后端并行开发中，delivery_task 尚无此列；有值才落 marker）
    if (tracking.merchantLat && tracking.merchantLng) {
      markers.push({ id: 2, latitude: Number(tracking.merchantLat), longitude: Number(tracking.merchantLng), iconPath: '/assets/map/shop-pin.png', width: 26, height: 34 })
    }
    if (tracking.deliveryLat && tracking.deliveryLng) {
      markers.push({ id: 3, latitude: Number(tracking.deliveryLat), longitude: Number(tracking.deliveryLng), iconPath: '/assets/map/home-pin.png', width: 26, height: 34 })
    }
    // 轨迹串线（后端已返回 path：时间升序坐标点）
    if (Array.isArray(tracking.path) && tracking.path.length > 1) {
      polylines.push({ points: tracking.path, color: '#1E9E62', width: 4 })
    }
  }
  return {
    statusText: statusTextOf(tracking.taskStatus),
    driverName,
    driverInitial: driverName ? driverName.slice(0, 1) : '',
    vehicle: (driver.vehicleType != null && vehicleMap[driver.vehicleType]) || '配送车',
    phone: driver.phone || '',
    hasDriver: Boolean(driverName),
    pickupCode: tracking.pickupCode || '',
    eta: tracking.estimatedDeliveryTime || '',
    progress: progressOf(tracking.taskStatus),
    steps,
    hasLocation,
    mapLat: riderLat,
    mapLng: riderLng,
    markers,
    polylines
  }
}

Page({
  data: { tracking: null as TrackingInfo | null, view: null as TrackView | null, orderId: '' as number | string, loading: false, menuTop: 0, menuH: 32 },
  trackingTimer: null as ReturnType<typeof setInterval> | null,

  onLoad(options: Record<string, string>) {
    if (!requireUser()) return
    const { menuTop, menuH } = getSafeArea()
    // 订单 id 为雪花字符串，原样透传禁止 Number() 强转
    const orderId = (options && options.orderId) || ''
    this.setData({ menuTop, menuH, orderId, loading: true }, () => this.startTrackingTimer())
    if (orderId) {
      getTracking(orderId)
        .then((tracking) => this.setData({ tracking, view: buildView(tracking) }))
        .catch((error: Error) => wx.showToast({ title: error.message || '配送信息加载失败', icon: 'none' }))
        .finally(() => this.setData({ loading: false }))
    } else {
      this.setData({ loading: false })
    }
  },
  onShow() {
    this.startTrackingTimer()
  },

  onHide() {
    this.stopTrackingTimer()
  },

  onUnload() {
    this.stopTrackingTimer()
  },

  startTrackingTimer() {
    if (!this.data.orderId || this.trackingTimer) return
    this.trackingTimer = setInterval(() => this.refresh(true), 15000)
  },

  stopTrackingTimer() {
    if (this.trackingTimer) {
      clearInterval(this.trackingTimer)
      this.trackingTimer = null
    }
  },

  back() { wx.navigateBack() },
  refresh(silent = false) {
    if (!this.data.orderId) return
    getTracking(this.data.orderId)
      .then((tracking) => {
        this.setData({ tracking, view: buildView(tracking) })
        if (!silent) wx.showToast({ title: '已刷新', icon: 'none' })
      })
      .catch(() => {})
  },
  callDriver() {
    const phone = this.data.view && this.data.view.phone
    if (phone) wx.makePhoneCall({ phoneNumber: phone }).catch(() => {})
  }
})
