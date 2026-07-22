import { merchantStatusText, type MerchantOrder } from '../api/merchant'
import { fmtDateTime } from './json'

/** 商家端订单卡视图模型（m-home / m-orders 共用，保证两页渲染一致） */
export interface MOrderVM {
  id: number | string
  orderNo: string
  status: number
  deliveryStatus: number
  statusText: string
  statusCls: string // s1 橙（待处理）/ s2 绿（履约中）/ s3 灰（终态）
  itemsText: string
  metaText: string
  amount: string
}

/** 库存预警阈值：stock ≤ 该值时标记「库存紧张」（运营可调整） */
export const LOW_STOCK_THRESHOLD = 10

/** 金额展示：整数不带小数点（对齐原型 money()） */
export const money = (n: number): string => {
  const v = Math.round((Number(n) || 0) * 100) / 100
  return v % 1 === 0 ? String(v) : v.toFixed(2)
}

const statusCls = (o: MerchantOrder): string => {
  const s = o.orderStatus || 0
  if (s === 20) return 's1'
  if (s === 30 || s === 40) return 's2'
  return 's3'
}

export const buildOrderVM = (o: MerchantOrder): MOrderVM => ({
  id: o.id || '',
  orderNo: o.orderNo || '—',
  status: o.orderStatus || 0,
  deliveryStatus: o.deliveryStatus || 0,
  statusText: merchantStatusText(o),
  statusCls: statusCls(o),
  itemsText: (o.items || []).map((it) => `${it.snapshotName || '商品'} ×${it.quantity || 1}`).join('　'),
  metaText: `备注：${o.remark || '无'} · ${fmtDateTime(o.createTime)}`,
  amount: money(Number(o.payAmount != null ? o.payAmount : o.totalAmount) || 0)
})
