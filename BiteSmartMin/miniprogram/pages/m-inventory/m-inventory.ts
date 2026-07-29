import { getSafeArea } from '../../utils/safe-area'
import {
  getMerchantDishes,
  getMerchantInventoryLogs,
  getMerchantStockWarnings,
  type MerchantInventoryLog
} from '../../api/merchant'
import type { Dish } from '../../api/catalog'
import { fmtDateTime } from '../../utils/json'

interface WarningVM {
  id: number | string
  name: string
  image: string
  stock: number
  threshold: number
  statusText: string
  statusCls: string
}

interface LogVM {
  id: number | string
  name: string
  changeText: string
  quantityText: string
  stockText: string
  time: string
  cls: string
}

const changeTypeText = (type?: number): string => ({
  10: '入库',
  20: '出库',
  30: '下单锁定',
  40: '支付扣减',
  50: '取消释放',
  60: '退款恢复',
  70: '盘盈',
  80: '盘亏'
} as Record<number, string>)[type || 0] || '库存变动'

const imageOf = (dish: Dish): string => dish.dishImage || ''

const buildWarning = (dish: Dish): WarningVM => {
  const stock = Number(dish.stock || 0)
  const threshold = Number(dish.minStockWarning || 0)
  const soldOut = stock <= 0
  return {
    id: dish.id || '',
    name: dish.dishName || '未命名菜品',
    image: imageOf(dish),
    stock,
    threshold,
    statusText: soldOut ? '已售罄' : '库存紧张',
    statusCls: soldOut ? 'red' : 'orange'
  }
}

const buildLog = (log: MerchantInventoryLog, names: Record<string, string>): LogVM => {
  const quantity = Number(log.changeQuantity || 0)
  return {
    id: log.id || `${log.dishId}-${log.createTime}`,
    name: names[String(log.dishId)] || '菜品库存',
    changeText: changeTypeText(log.changeType),
    quantityText: `${quantity > 0 ? '+' : ''}${quantity}`,
    stockText: `${Number(log.beforeStock || 0)} → ${Number(log.afterStock || 0)}`,
    time: fmtDateTime(log.createTime),
    cls: quantity >= 0 ? 'plus' : 'minus'
  }
}

Page({
  data: {
    menuTop: 26,
    menuH: 32,
    totalCount: 0,
    normalCount: 0,
    warningCount: 0,
    warnings: [] as WarningVM[],
    logs: [] as LogVM[],
    loading: true
  },

  onLoad() {
    const sa = getSafeArea()
    this.setData({ menuTop: sa.menuTop, menuH: sa.menuH })
    this.loadAll()
  },

  onShow() {
    if (!this.data.loading) this.loadAll()
  },

  loadAll() {
    this.setData({ loading: true })
    Promise.all([getMerchantDishes(), getMerchantStockWarnings(), getMerchantInventoryLogs()])
      .then(([dishes, warningDishes, logs]) => {
        const all = dishes || []
        const warnings = (warningDishes || []).map(buildWarning)
        const names: Record<string, string> = {}
        all.forEach((dish) => { names[String(dish.id)] = dish.dishName || '未命名菜品' })
        this.setData({
          totalCount: all.length,
          normalCount: Math.max(0, all.length - warnings.length),
          warningCount: warnings.length,
          warnings,
          logs: (logs || []).slice(0, 12).map((log) => buildLog(log, names)),
          loading: false
        })
      })
      .catch((error: Error) => {
        this.setData({ loading: false })
        wx.showToast({ title: error.message || '库存加载失败', icon: 'none' })
      })
  },

  goDishes() {
    wx.redirectTo({ url: '/pages/m-dishes/m-dishes' })
  },

  goBack() {
    wx.navigateBack({ fail: () => wx.redirectTo({ url: '/pages/m-me/m-me' }) })
  },

  noop() {}
})
