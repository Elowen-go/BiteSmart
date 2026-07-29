import { request } from '../utils/request'

/**
 * 配送 API —— 用户端物流跟踪 + 骑手端任务/结算/评价：
 * - taskStatus 真实枚举（与用户端 delivery 页一致）：
 *   10 待接单 →(accept 抢单，乐观锁)→ 20 待取餐 →(pickup)→ 30 已取餐 →(start)→ 40 配送中 →(deliver)→ 50 已送达；60 异常 / 70 已取消
 * - 商家出餐（订单 30→40）时后端自动创建 task_status=10 的任务；送达后订单 40→50 完结
 * - exception / status 的 reason、status 参数后端用 @RequestParam 接收，走 query 传参
 * - 所有 Long id 传输层为 string，页面禁止 Number() 强转
 */

/* ---------- 用户端：物流跟踪 ---------- */

export interface TrackingInfo { taskStatus?: number; pickupCode?: string; estimatedDeliveryTime?: string; pickupTime?: string; deliverTime?: string; currentLat?: number; currentLng?: number; riderLat?: number; riderLng?: number; locTime?: string; locationUpdateTime?: string; merchantLat?: number; merchantLng?: number; deliveryLat?: number; deliveryLng?: number; path?: { latitude: number; longitude: number; time?: string }[]; orderRemark?: string; driver?: { realName?: string; phone?: string; currentLat?: number; currentLng?: number; vehicleType?: number } }
export const getTracking = (orderId: number | string): Promise<TrackingInfo> => request<TrackingInfo>({ url: `/delivery/tracking/${orderId}` })

/* ---------- 骑手端：配送任务 ---------- */

export interface RiderTask {
  id?: number | string
  orderId?: number | string
  orderNo?: string
  driverId?: number | string
  merchantId?: number | string
  merchantAddress?: string
  merchantPhone?: string
  deliveryAddress?: string
  receiverName?: string
  receiverPhone?: string
  pickupCode?: string
  taskStatus?: number
  pickupTime?: string
  deliverTime?: string
  estimatedDeliveryTime?: string
  exceptionReason?: string
  orderRemark?: string // 订单备注快照（商家出餐建任务时从订单带出）
  createTime?: string
  // 取货点和收货点坐标（GCJ-02；未维护坐标时导航按钮会提示）
  merchantLat?: number
  merchantLng?: number
  deliveryLat?: number
  deliveryLng?: number
}

/** 骑手端状态文案（口径同用户端 delivery 页） */
export const riderTaskStatusText = (taskStatus?: number): string => {
  const map: Record<number, string> = { 10: '待接单', 20: '待取餐', 30: '已取餐', 40: '配送中', 50: '已送达', 60: '配送异常', 70: '已取消' }
  return (taskStatus != null && map[taskStatus]) || '未知'
}

/** 任务大厅：待接单任务（task_status=10，抢单制） */
export const getPendingTasks = (): Promise<RiderTask[]> =>
  request<RiderTask[]>({ url: '/driver/tasks/pending' })

/** 我的配送任务（全部状态，按创建时间倒序） */
export const getDriverTasks = (): Promise<RiderTask[]> =>
  request<RiderTask[]>({ url: '/driver/tasks' })

/** 抢单：10 → 20（乐观锁；被抢/单满/未注册均 400，toast 后端 message） */
export const acceptDriverTask = (id: number | string): Promise<void> =>
  request<void>({ url: `/driver/tasks/${id}/accept`, method: 'POST' })

/** 确认取餐：20 → 30（同步订单 deliveryStatus=20 已取餐） */
export const pickupDriverTask = (id: number | string): Promise<void> =>
  request<void>({ url: `/driver/tasks/${id}/pickup`, method: 'POST' })

/** 开始配送：30 → 40 */
export const startDeliveryTask = (id: number | string): Promise<void> =>
  request<void>({ url: `/driver/tasks/${id}/start`, method: 'POST' })

/** 确认送达：30/40 → 50（同步订单 40→50 已完成，商家结算解冻） */
export const deliverDriverTask = (id: number | string): Promise<void> =>
  request<void>({ url: `/driver/tasks/${id}/deliver`, method: 'POST' })

/** 异常上报：reason 走 query；任务 → 60 异常 */
export const reportDriverException = (id: number | string, reason: string): Promise<void> =>
  request<void>({ url: `/driver/tasks/${id}/exception?reason=${encodeURIComponent(reason)}`, method: 'POST' })

/** 拒单：reason 走 query（后端已实现并冒烟验证；校验任务属于当前骑手） */
export const rejectDriverTask = (id: number | string, reason: string): Promise<void> =>
  request<void>({ url: `/driver/tasks/${id}/reject?reason=${encodeURIComponent(reason || '骑手原因无法配送')}`, method: 'POST' })

/** 定位上传：JSON body { taskId, latitude, longitude }（GCJ-02；taskId 必填，后端校验任务属于当前骑手且 taskStatus ∈ {30,40}） */
export const updateDriverLocation = (taskId: number | string, latitude: number, longitude: number): Promise<void> =>
  request<void>({ url: '/driver/location', method: 'POST', data: { taskId, latitude, longitude } })

/* ---------- 骑手端：个人资料（后端已实现并冒烟验证） ---------- */

export interface DriverProfile {
  id?: number | string
  realName?: string
  phone?: string
  vehicleType?: number
  serviceArea?: string
  status?: number
}

export const getDriverProfile = (): Promise<DriverProfile> =>
  request<DriverProfile>({ url: '/driver/profile' })

export const updateDriverProfile = (patch: Partial<DriverProfile>): Promise<void> =>
  request<void>({ url: '/driver/profile', method: 'PUT', data: patch as Record<string, unknown> })

/** 在线状态：10 在线 / 20 忙碌 / 30 离线（后端无 GET，当前状态由页面本地持久化） */
export const updateDriverStatus = (status: number): Promise<void> =>
  request<void>({ url: `/driver/status?status=${status}`, method: 'POST' })

/* ---------- 骑手端：结算与评价 ---------- */

export interface DriverSettlement {
  id?: number | string
  deliveryTaskId?: number | string
  orderId?: number | string
  deliveryFee?: number
  bonus?: number
  penalty?: number
  settlementAmount?: number
  settlementStatus?: number // 10 待结算 / 20 已结算
  settlementTime?: string
  createTime?: string
}

export interface SettlementStats {
  totalDeliveryFee?: number
  totalBonus?: number
  totalPenalty?: number
  totalAmount?: number
  pendingAmount?: number
  settledAmount?: number
}

export const getDriverSettlements = (): Promise<DriverSettlement[]> =>
  request<DriverSettlement[]>({ url: '/driver/settlements' })

export const getSettlementStats = (): Promise<SettlementStats> =>
  request<SettlementStats>({ url: '/driver/settlements/stats' })

export interface DriverReview {
  id?: number | string
  ratingDelivery?: number
  content?: string
  isAnonymous?: number
  createTime?: string
  userNickname?: string
  username?: string
}

export interface DriverReviewStats { averageRating?: number; totalReviews?: number }

export const getDriverReviews = (): Promise<DriverReview[]> =>
  request<DriverReview[]>({ url: '/driver/reviews' })

export const getDriverReviewStats = (): Promise<DriverReviewStats> =>
  request<DriverReviewStats>({ url: '/driver/reviews/stats' })
