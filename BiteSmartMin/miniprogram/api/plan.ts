import { request } from '../utils/request'

/**
 * 专属计划 API —— 对齐后端 PlanController（B 类）：
 * - 返回结构统一为 { plan, meals }；meals 联表带出菜品信息（dishName/dishImage/calories/protein/fat/carbs/tags）
 * - 后端 Jackson 全局把 Long 序列化为字符串（防 JS 精度丢失），id / planId / dishId 传输层是 string，
 *   页面层禁止 Number() 强转，原样透传回后端即可（Spring 自动转 Long）
 * - plan.status：10 未开始 / 20 进行中 / 30 已完成 / 40 已取消；curDay 从 0 起（0 = 第 1 天）
 * - meal.mealIndex：0 早餐 / 1 午餐 / 2 晚餐；checked：0 未打卡 / 1 已打卡
 * - prefs / avoid / focusParts / tags 均为 JSON 数组字符串，展示前需 JSON.parse（见 utils/json.ts）
 * - check / swap / start / advance 成功均返回最新 { plan, meals }，前端直接用响应重渲染，无需回查
 * - 幂等冲突（重复打卡 / 未打卡取消）后端返回 409，request 层 reject Error(message)，页面 toast 即可
 */
export interface PlanInfo {
  id?: number | string
  planDays?: number
  goal?: string
  activityLevel?: number // 10 久坐 / 20 轻度 / 30 中度 / 40 重度
  targetWeight?: number
  startWeight?: number
  prefs?: string // JSON 数组字符串
  avoid?: string // JSON 数组字符串
  focusParts?: string // JSON 数组字符串
  status?: number // 10 未开始 / 20 进行中 / 30 已完成 / 40 已取消
  curDay?: number
  createdTime?: string
  startedTime?: string
}

export interface PlanMealItem {
  id?: number | string
  planId?: number | string
  dayIndex?: number
  mealIndex?: number // 0 早 / 1 午 / 2 晚
  dishId?: number | string
  swapCount?: number
  checked?: number // 0 / 1
  checkedTime?: string
  // 联表带出的菜品信息
  dishName?: string
  dishImage?: string
  calories?: number
  protein?: number
  fat?: number
  carbs?: number
  tags?: string // JSON 数组字符串
}

export interface PlanDetail {
  plan?: PlanInfo
  meals?: PlanMealItem[]
}

/**
 * 生成专属计划（基于当前健康档案；旧计划自动取消）。要求档案已存在，否则 400「请先完善健康档案」。
 * planDays 可选 7 / 21（默认 7，会员 21 天计划由后端并行开发支持；
 * 后端未升级时 body 会被忽略、按默认 7 天生效，前端以响应 plan.planDays 为准）
 */
export const generatePlan = (planDays?: number): Promise<PlanDetail> =>
  request<PlanDetail>({
    url: '/plan/generate',
    method: 'POST',
    data: planDays === 21 ? { planDays: 21 } : (planDays === 7 ? { planDays: 7 } : {})
  })

/** 当前最新计划；无计划时后端 data 为 null */
export const getCurrentPlan = ():Promise<PlanDetail | null> =>
  request<PlanDetail | null>({ url: '/plan/current' })

/** 开始执行（仅 status=10 可开始）：status → 20，curDay = 0 */
export const startPlan = (): Promise<PlanDetail> =>
  request<PlanDetail>({ url: '/plan/start', method: 'POST' })

/** 打卡 / 取消打卡（服务端同步写/删 diet_record；仅进行中计划的 curDay 当天可操作，重复操作 409） */
export const checkPlanMeal = (mealId: number | string, checked: boolean): Promise<PlanDetail> =>
  request<PlanDetail>({ url: '/plan/check', method: 'POST', data: { mealId, checked } })

/** 换一道菜（已打卡餐次 400 禁止更换；已结束计划不可换） */
export const swapPlanMeal = (mealId: number | string): Promise<PlanDetail> =>
  request<PlanDetail>({ url: '/plan/swap', method: 'POST', data: { mealId } })

/** 演示用：进入下一天（curDay + 1；最后一天自动标记计划完成） */
export const advancePlan = (): Promise<PlanDetail> =>
  request<PlanDetail>({ url: '/plan/advance', method: 'POST' })
