import { merchantStatusText, type MerchantOrder, type MerchantOrderItem } from '../api/merchant'
import { fmtDateTime } from './json'
import { API_ORIGIN } from './request'

/** 商家端订单卡视图模型（m-home / m-orders 共用，保证两页渲染一致） */
export interface MOrderVM {
  id: number | string
  orderNo: string
  status: number
  deliveryStatus: number
  statusText: string
  statusCls: string // s1 橙（待处理）/ s2 绿（履约中）/ s3 灰（终态）
  items: MOrderItemVM[]
  itemsExpanded: boolean
  itemsText: string
  metaText: string
  amount: string
}

export interface MOrderItemVM {
  name: string
  quantity: number
  image: string
  price: string
}

export type DishPriceMap = Record<string, number>

type MerchantOrderItemCompat = MerchantOrderItem & {
  qty?: number | string
  num?: number | string
  price?: number | string
  unitPrice?: number | string
  snapshot_price?: number | string
  unit_price?: number | string
  subtotal?: number | string
  sub_total?: number | string
  amount?: number | string
  totalPrice?: number | string
  total_price?: number | string
}

const positiveAmount = (value: unknown): number | null => {
  if (value == null || value === '') return null
  const amount = Number(value)
  return Number.isFinite(amount) && amount > 0 ? amount : null
}

const normalizeImage = (value?: string): string => {
  const image = String(value || '').trim()
  if (!image) return ''
  if (/^(https?:\/\/|data:image\/|wxfile:)/i.test(image)) return image
  if (image.startsWith('/uploads/')) return `${API_ORIGIN}/api/files/download/${image.slice('/uploads/'.length)}`
  if (image.startsWith('/')) return `${API_ORIGIN}${image}`
  return `${API_ORIGIN}/${image}`
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

export const buildOrderVM = (o: MerchantOrder, dishPrices: DishPriceMap = {}): MOrderVM => ({
  id: o.id || '',
  orderNo: o.orderNo || '—',
  status: o.orderStatus || 0,
  deliveryStatus: o.deliveryStatus || 0,
  statusText: merchantStatusText(o),
  statusCls: statusCls(o),
  itemsExpanded: false,
  items: (o.items || []).map((it) => {
    const runtimeItem = it as MerchantOrderItemCompat
    const quantity = Number(runtimeItem.quantity ?? runtimeItem.qty ?? runtimeItem.num ?? 1) || 1
    const listedPrice = positiveAmount(runtimeItem.snapshotPrice)
      ?? positiveAmount(runtimeItem.snapshot_price)
      ?? positiveAmount(runtimeItem.price)
      ?? positiveAmount(runtimeItem.unitPrice)
      ?? positiveAmount(runtimeItem.unit_price)
    const subtotal = positiveAmount(runtimeItem.subTotal)
      ?? positiveAmount(runtimeItem.subtotal)
      ?? positiveAmount(runtimeItem.sub_total)
      ?? positiveAmount(runtimeItem.amount)
      ?? positiveAmount(runtimeItem.totalPrice)
      ?? positiveAmount(runtimeItem.total_price)
    const dishPrice = positiveAmount(dishPrices[String(runtimeItem.dishId || '')])
    const unitPrice = listedPrice ?? (subtotal != null ? subtotal / quantity : (dishPrice || 0))
    return {
      name: it.snapshotName || '商品',
      quantity,
      image: normalizeImage(it.snapshotImage),
      price: Number.isFinite(unitPrice) && unitPrice > 0 ? money(unitPrice) : ''
    }
  }),
  itemsText: (o.items || []).map((it) => `${it.snapshotName || '商品'} ×${it.quantity || 1}`).join('　'),
  metaText: `备注：${o.remark || '无'} · ${fmtDateTime(o.createTime)}`,
  amount: money(Number(o.payAmount != null ? o.payAmount : o.totalAmount) || 0)
})
