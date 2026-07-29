import { request } from '../utils/request'
import type { Dish, Combo } from './catalog'

/**
 * 商家端 API —— 对齐后端 controller/merchant/*：
 * - 订单：GET /merchant/orders（列表带 items 明细）；接单/拒单/备餐/出餐为 PUT
 *   状态机：10 待支付 → 20 待接单 →(accept) 30 备餐中 →(done) 40 配送中（自动创建配送任务，
 *   deliveryStatus 10 待取餐）；reject 需 query 传 reason → 60 已取消
 * - 统计：today 返回 orderCount/revenue/pendingOrderCount/avgOrderAmount/reviewCount 等；
 *   daily 返回 [{date, orderCount, revenue}]；top-dishes 返回 [{dishId, dishName, totalQuantity}]
 * - 菜品：PUT /merchant/dishes/{id} 为动态更新（只传要改的字段，如 {status} / {stock}）；
 *   Dish.status：10 上架 / 20 下架 / 30 售罄
 * - 店铺：Merchant.status 是审核状态（10 待审核 / 20 通过 / 30 驳回 / 40 关闭）；
 *   openStatus 为营业状态（10 营业中 / 20 打烊，后端并行开发中——联调前 GET 可能缺省、PUT 会被 Jackson 忽略，前端需兜底）
 * - 所有 Long id 传输层为 string，页面禁止 Number() 强转
 */

/* ---------- 订单 ---------- */

export interface MerchantOrderItem {
  id?: number | string
  itemType?: number // 10 菜品 / 20 套餐
  dishId?: number | string
  comboId?: number | string
  snapshotName?: string
  snapshotImage?: string
  snapshotPrice?: number
  quantity?: number
  subTotal?: number
}

export interface MerchantOrder {
  id?: number | string
  orderNo?: string
  orderStatus?: number // 10 待支付 / 20 待接单 / 30 备餐中 / 40 配送中 / 50 已完成 / 60 已取消 / 70 退款中 / 80 已退款
  deliveryStatus?: number // 0 未配送 / 10 待取餐 / 20 已取餐 / 30 配送中 / 40 已送达
  totalAmount?: number
  payAmount?: number
  deliveryAddress?: string
  receiverName?: string
  receiverPhone?: string
  remark?: string
  createTime?: string
  items?: MerchantOrderItem[]
}

/** 商家视角状态文案（出餐后统一为 40 配送中，按 deliveryStatus 细分待取餐/履约中） */
export const merchantStatusText = (o: MerchantOrder): string => {
  const s = o.orderStatus || 0
  if (s === 20) return '待接单'
  if (s === 30) return '备餐中'
  if (s === 40) return (o.deliveryStatus || 0) <= 10 ? '待取餐' : '配送中'
  if (s === 50) return '已完成'
  if (s === 10) return '待支付'
  if (s === 70) return '退款中'
  if (s === 80) return '已退款'
  return '已取消'
}

export interface MerchantOrderDetail {
  order?: MerchantOrder
  items?: MerchantOrderItem[]
  buyer?: { nickname?: string; username?: string; phone?: string }
  statusTimeline?: { status?: number; remark?: string; createTime?: string }[]
}

export const getMerchantOrders = (): Promise<MerchantOrder[]> =>
  request<MerchantOrder[]>({ url: '/merchant/orders' })

export const getMerchantOrderDetail = (id: number | string): Promise<MerchantOrderDetail> =>
  request<MerchantOrderDetail>({ url: `/merchant/orders/${id}` })

/** 接单：20 待接单 → 30 备餐中（乐观锁，状态冲突 400） */
export const acceptMerchantOrder = (id: number | string): Promise<void> =>
  request<void>({ url: `/merchant/orders/${id}/accept`, method: 'PUT' })

/** 拒单：20 → 60 已取消；后端 @RequestParam 要求 reason 走 query */
export const rejectMerchantOrder = (id: number | string, reason: string): Promise<void> =>
  request<void>({ url: `/merchant/orders/${id}/reject?reason=${encodeURIComponent(reason || '商家原因无法接单')}`, method: 'PUT' })

/** 出餐完成：30 备餐中 → 40 配送中（自动创建配送任务，骑手大厅可见） */
export const finishMerchantOrder = (id: number | string): Promise<void> =>
  request<void>({ url: `/merchant/orders/${id}/done`, method: 'PUT' })

/* ---------- 统计 ---------- */

export interface MerchantTodayStats {
  orderCount?: number
  revenue?: number
  newUserCount?: number
  avgOrderAmount?: number
  pendingOrderCount?: number
  stockAlertCount?: number
  reviewCount?: number
}

export interface MerchantDailyStat { date?: string; orderCount?: number; revenue?: number }
export interface MerchantTopDish { dishId?: number | string; dishName?: string; totalQuantity?: number }

export const getMerchantTodayStats = (): Promise<MerchantTodayStats> =>
  request<MerchantTodayStats>({ url: '/merchant/statistics/today' })

export const getMerchantDailyStats = (startDate: string, endDate: string): Promise<MerchantDailyStat[]> =>
  request<MerchantDailyStat[]>({ url: '/merchant/statistics/daily', data: { startDate, endDate } })

export const getMerchantTopDishes = (limit = 5): Promise<MerchantTopDish[]> =>
  request<MerchantTopDish[]>({ url: '/merchant/statistics/top-dishes', data: { limit } })

/* ---------- 菜品 ---------- */

export const getMerchantDishes = (): Promise<Dish[]> =>
  request<Dish[]>({ url: '/merchant/dishes' })

/** 新增菜品：POST /merchant/dishes（@RequestBody Dish；后端默认 status=10 上架、salesCount=0） */
export const createMerchantDish = (dish: Partial<Dish>): Promise<void> =>
  request<void>({ url: '/merchant/dishes', method: 'POST', data: dish as Record<string, unknown> })

/** 菜品详情（编辑预填用） */
export const getMerchantDish = (id: number | string): Promise<Dish> =>
  request<Dish>({ url: `/merchant/dishes/${id}` })

/** 动态更新：上下架传 { status }，改库存传 { stock }，只更新传入字段 */
export const updateMerchantDish = (id: number | string, patch: Partial<Dish>): Promise<void> =>
  request<void>({ url: `/merchant/dishes/${id}`, method: 'PUT', data: patch as Record<string, unknown> })

/* ---------- 库存 ---------- */

export interface MerchantInventoryLog {
  id?: number | string
  dishId?: number | string
  changeType?: number
  changeQuantity?: number
  beforeStock?: number
  afterStock?: number
  remark?: string
  createTime?: string
}

export const getMerchantStockWarnings = (): Promise<Dish[]> =>
  request<Dish[]>({ url: '/merchant/inventory/warnings' })

export const getMerchantInventoryLogs = (): Promise<MerchantInventoryLog[]> =>
  request<MerchantInventoryLog[]>({ url: '/merchant/inventory/logs' })

/* ---------- 菜品分类 ---------- */

export interface MerchantCategory {
  id?: number | string
  categoryName?: string
  sortOrder?: number
}

export const getMerchantCategories = (): Promise<MerchantCategory[]> =>
  request<MerchantCategory[]>({ url: '/merchant/categories' })

/* ---------- 套餐 ---------- */

/** 套餐关联菜品项（对齐后端 ComboRequest.DishItem；isFixed：1 固定不可替换 / 0 可替换） */
export interface MerchantComboDishItem {
  dishId?: number | string
  quantity?: number
  isFixed?: number
}

export interface MerchantComboPayload {
  combo: Partial<Combo>
  dishItems?: MerchantComboDishItem[]
}

interface ComboPage { list?: Combo[]; total?: number }

/** 套餐列表：后端不分页时也返回 PageResultVO（data.list） */
export const getMerchantCombos = (): Promise<Combo[]> =>
  request<ComboPage>({ url: '/merchant/combos' }).then((p) => (p && p.list) || [])

export const getMerchantComboDetail = (id: number | string): Promise<{ combo?: Combo; dishItems?: MerchantComboDishItem[] }> =>
  request<{ combo?: Combo; dishItems?: MerchantComboDishItem[] }>({ url: `/merchant/combos/${id}` })

export const createMerchantCombo = (payload: MerchantComboPayload): Promise<void> =>
  request<void>({ url: '/merchant/combos', method: 'POST', data: payload as unknown as Record<string, unknown> })

/**
 * 更新套餐。dishItems 省略（undefined）时后端保留原有关联菜品（ComboService.update 仅在其非 null 时重建关联），
 * 因此上下架切换可以只传 { combo: { status } }
 */
export const updateMerchantCombo = (id: number | string, payload: MerchantComboPayload): Promise<void> =>
  request<void>({ url: `/merchant/combos/${id}`, method: 'PUT', data: payload as unknown as Record<string, unknown> })

/* ---------- 店铺 ---------- */

export interface MerchantShop {
  id?: number | string
  shopName?: string
  shopLogo?: string
  contactName?: string
  contactPhone?: string
  shopAddress?: string
  shopLat?: number
  shopLng?: number
  businessHours?: string
  shopNotice?: string
  openStatus?: number // 营业状态：10 营业中 / 20 打烊（后端并行开发中，联调前可能缺省）
  status?: number // 审核状态：10 待审核 / 20 通过 / 30 驳回 / 40 关闭
  avgRating?: number
}

export const getMerchantShop = (): Promise<MerchantShop | null> =>
  request<MerchantShop | null>({ url: '/merchant/shop' })

export const updateMerchantShop = (patch: Partial<MerchantShop>): Promise<void> =>
  request<void>({ url: '/merchant/shop', method: 'PUT', data: patch as Record<string, unknown> })

/* ---------- 评价 ---------- */

export interface MerchantReview {
  id?: number | string
  orderId?: number | string
  ratingFood?: number
  ratingDelivery?: number
  ratingService?: number
  overallRating?: number
  content?: string
  isAnonymous?: number // 1 匿名
  merchantReply?: string
  createTime?: string
  username?: string
  userNickname?: string
  dishName?: string
}

export const getMerchantReviews = (): Promise<MerchantReview[]> =>
  request<MerchantReview[]>({ url: '/merchant/reviews' })

/** 商家回复评价：content 后端用 @RequestParam 接收，走 query 传参 */
export const replyMerchantReview = (id: number | string, content: string): Promise<void> =>
  request<void>({ url: `/merchant/reviews/${id}/reply?content=${encodeURIComponent(content)}`, method: 'POST' })
