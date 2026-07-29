/**
 * 高德地图配置
 *
 * 申请地址：https://lbs.amap.com/ （控制台 → 应用管理 → 创建新应用 → 添加 Key，
 * 服务平台选「微信小程序」），把 key 填入下方 AMAP_KEY。
 *
 * 当前方案（不内置 amap-wx SDK，公网无法直接下载到官方 SDK 文件）：
 * - 骑手导航：wx.openLocation 调起手机地图 App（无需 key）
 * - 用户端轨迹：原生 <map> 组件 + markers/polyline 渲染（无需 key）
 *
 * AMAP_KEY 预留用途：后续从 https://lbs.amap.com/api/wx/download 下载 amap-wx.js
 * 放入 miniprogram/libs/ 后，可在此封装 getRegeo（逆地理编码）/ getDrivingRoute
 * （驾车路线规划），用于骑手端页内路线规划与配送路径 polyline 生成。
 */
export const AMAP_KEY = 'bc3954f0e03f08b6b638165c501a026e' // TODO: 在此填入高德小程序 SDK key

export const hasAmapKey = (): boolean => !!AMAP_KEY
