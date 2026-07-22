/**
 * C 端 ID 展示工具：雪花 ID / 订单号只露尾部，绝不向用户展示完整长数字。
 * 注意：仅用于展示层；传给后端的 id 仍保持 string 原样透传。
 */

/** 取 id 尾部 keep 位（默认 6 位），不足则全量返回 */
export const tail = (id: number | string | null | undefined, keep = 6): string => {
  const s = String(id ?? '').replace(/\s/g, '')
  if (!s) return ''
  return s.length <= keep ? s : s.slice(-keep)
}

/** 订单展示：订单 ···903216 */
export const orderLabel = (orderNoOrId: number | string | null | undefined): string => {
  const t = tail(orderNoOrId)
  return t ? `订单 ···${t}` : '订单'
}

/**
 * 商家展示：有名称显示名称，绝不显示雪花 ID。
 * 当前用户端接口不带 shopName，无名称时退化为尾部掩码（已知限制，待接口带出 shopName 后自动生效）。
 */
export const merchantLabel = (shopName?: string | null, merchantId?: number | string | null): string => {
  if (shopName) return shopName
  const t = tail(merchantId)
  return t ? `商家 ···${t}` : '商家'
}

/** 价格展示：统一两位小数，避免 ¥15.5 / ¥15.90 混排 */
export const formatPrice = (value: number | string | null | undefined): string => {
  const n = Number(value)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}
