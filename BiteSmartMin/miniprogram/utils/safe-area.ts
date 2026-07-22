/**
 * 顶部安全区计算：状态栏高度 + 右上角胶囊位置（navigationStyle: custom 必备）
 * - statusBarH：状态栏高度（px）
 * - menuTop / menuH：胶囊 top / height（px），自定义导航栏用它与胶囊对齐
 * - padTop：内容区顶部安全高度 = 胶囊 bottom + 8px
 * 全部用 API 动态计算，兼容开发者工具与真机，不写死常量。
 */

export interface SafeAreaInfo {
  statusBarH: number
  menuTop: number
  menuH: number
  padTop: number
}

export const getSafeArea = (): SafeAreaInfo => {
  let statusBarH = 20
  try {
    const w = wx as unknown as { getWindowInfo?: () => { statusBarHeight?: number } }
    const info = typeof w.getWindowInfo === 'function' ? w.getWindowInfo() : wx.getSystemInfoSync()
    statusBarH = info.statusBarHeight || 20
  } catch (_) { /* ignore */ }

  let menuTop = statusBarH + 6
  let menuH = 32
  let menuBottom = menuTop + menuH
  try {
    const rect = wx.getMenuButtonBoundingClientRect()
    if (rect && rect.height) {
      menuTop = rect.top
      menuH = rect.height
      menuBottom = rect.bottom
    }
  } catch (_) { /* ignore */ }

  return { statusBarH, menuTop, menuH, padTop: menuBottom + 8 }
}

/** 设备像素比（canvas 2d 高清绘制用），失败兜底 2 */
export const getPixelRatio = (): number => {
  try {
    const w = wx as unknown as { getWindowInfo?: () => { pixelRatio?: number } }
    if (typeof w.getWindowInfo === 'function') return w.getWindowInfo().pixelRatio || 2
    return wx.getSystemInfoSync().pixelRatio || 2
  } catch (_) {
    return 2
  }
}

/** 窗口宽度 px（rpx -> px 换算用），失败兜底 375 */
export const getWindowWidth = (): number => {
  try {
    const w = wx as unknown as { getWindowInfo?: () => { windowWidth?: number } }
    if (typeof w.getWindowInfo === 'function') return w.getWindowInfo().windowWidth || 375
    return wx.getSystemInfoSync().windowWidth || 375
  } catch (_) {
    return 375
  }
}

/** rpx 转 px（按 375 设计稿基准） */
export const rpx2px = (rpx: number): number => Math.round((rpx * getWindowWidth()) / 750)
