/**
 * 高德地图 Web 端配置（配送跟踪页使用）
 *
 * TODO: 填入高德 Web 端 JS API key。
 * 申请地址：https://lbs.amap.com/ —— 控制台「应用管理」创建应用，
 * 添加 key 时服务平台选择「Web端(JS API)」。
 * key 为空时，TrackingView 显示占位卡（状态时间线 + 配置提示），不加载地图脚本。
 *
 * 后端轨迹坐标为 GCJ-02（高德系），无需转换直接渲染。
 */
export const AMAP_JS_KEY = 'bc3954f0e03f08b6b638165c501a026e'

/** 高德 JS API 脚本地址（2.0 版本） */
export const AMAP_SCRIPT_URL = `https://webapi.amap.com/maps?v=2.0&key=${AMAP_JS_KEY}`
