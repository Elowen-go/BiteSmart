/**
 * 本地 mock 数据：内容直接抄自高保真原型（outputs/prototype/index.html）
 * 的 DISHES / COMBOS 常量。开发期无后端时保证界面完整可见。
 */

export const uimg = (id: string, w = 600): string =>
  `https://images.unsplash.com/photo-${id}?w=${w}&q=80`

export interface MockDish {
  id: number
  name: string
  price: number
  kcal: number
  protein: number
  carbs: number
  fat: number
  img: string
  cat: 'salad' | 'protein' | 'lowcal'
  tags: string[]
  sold: number
  stock: number
  desc: string
  ai: string
  fit: string[]
  caution: string[]
}

export interface MockCombo {
  id: number
  name: string
  price: number
  org: number
  days: number
  img: string
  en: string
  desc: string
  kcal: number
  items: number[]
  tags: string[]
}

export const MOCK_DISHES: MockDish[] = [
  { id: 1, name: '牛油果鸡胸肉沙拉', price: 29, kcal: 326, protein: 32, carbs: 18, fat: 12, img: '1512621776951-a57141f2eefd', cat: 'salad', tags: ['高蛋白', '减脂'], sold: 824, stock: 36, desc: '低温慢煮鸡胸 · 牛油果 · 混合生菜 · 油醋汁', fit: ['减脂期', '健身增肌', '控糖饮食'], caution: ['坚果过敏慎选', '含少量乳制品'], ai: '蛋白质占全天目标的 36%，牛油果提供优质不饱和脂肪，适合作为减脂期晚餐。搭配一杯无糖酸奶更均衡。' },
  { id: 2, name: '彩虹藜麦能量碗', price: 33, kcal: 412, protein: 18, carbs: 52, fat: 11, img: '1546069901-ba9599a7e63c', cat: 'salad', tags: ['全谷物', '饱腹感强'], sold: 657, stock: 42, desc: '三色藜麦 · 烤时蔬 · 鹰嘴豆 · 芝麻酱', fit: ['素食', '健身人群', '午餐正餐'], caution: ['含芝麻'], ai: '复合碳水释放平稳，下午不容易犯困。藜麦是完全蛋白，素食日也能吃够氨基酸。' },
  { id: 3, name: '香煎三文鱼时蔬', price: 36, kcal: 285, protein: 28, carbs: 12, fat: 14, img: '1540189549336-e6e99c3679fe', cat: 'protein', tags: ['Omega-3', '低卡'], sold: 931, stock: 25, desc: '挪威三文鱼 · 芦笋 · 小番茄 · 柠檬黄油汁', fit: ['减脂期', '控糖饮食', '用脑人群'], caution: ['鱼类过敏慎选'], ai: 'Omega-3 帮助控制炎症，285 kcal 的晚餐优选。蛋白质 28g，正好补上你今天的缺口。' },
  { id: 4, name: '田园时蔬温沙拉', price: 22, kcal: 198, protein: 9, carbs: 24, fat: 8, img: '1498837167922-ddd27525d352', cat: 'salad', tags: ['素食', '轻负担'], sold: 412, stock: 50, desc: '烤南瓜 · 西兰花 · 孢子甘蓝 · 蜂蜜芥末汁', fit: ['素食', '轻断食日'], caution: ['蛋白质偏低，建议搭配'], ai: '热量不到 200 kcal，适合轻断食日晚餐。建议加一份鸡胸或鸡蛋补足蛋白质。' },
  { id: 5, name: '香草烤鲈鱼配时蔬', price: 39, kcal: 310, protein: 34, carbs: 15, fat: 11, img: '1467003909585-2f8a72700288', cat: 'protein', tags: ['高蛋白', '低脂'], sold: 386, stock: 18, desc: '海鲈鱼 · 迷迭香 · 烤根茎蔬菜', fit: ['增肌期', '低碳饮食'], caution: ['鱼类过敏慎选'], ai: '34g 蛋白质 + 低脂，增肌期的理想正餐。鲈鱼刺少，老人孩子也好入口。' },
  { id: 6, name: '照烧鸡胸糙米饭', price: 27, kcal: 445, protein: 35, carbs: 48, fat: 9, img: '1512058564366-18510be2db19', cat: 'protein', tags: ['高蛋白', '主食均衡'], sold: 1102, stock: 60, desc: '照烧鸡腿去皮 · 糙米 · 溏心蛋 · 海苔', fit: ['健身增肌', '午餐正餐'], caution: ['含鸡蛋', '照烧汁含糖'], ai: '练后餐首选：35g 蛋白质 + 48g 缓释碳水，肌肉修复与糖原补充一碗搞定。' },
  { id: 7, name: '鸡丝荞麦凉面', price: 24, kcal: 368, protein: 22, carbs: 52, fat: 7, img: '1476224203421-9ac39bcb3327', cat: 'lowcal', tags: ['低脂', '夏日限定'], sold: 543, stock: 33, desc: '荞麦面 · 手撕鸡胸 · 黄瓜丝 · 麻酱汁', fit: ['夏季食欲差', '控脂期'], caution: ['含麸质', '含芝麻'], ai: '荞麦升糖指数低，麻酱控制在一勺以内，整碗只有 7g 脂肪。' },
  { id: 8, name: '泰式青咖喱鸡肉碗', price: 31, kcal: 398, protein: 26, carbs: 38, fat: 16, img: '1455619452474-d2be8b1e70cd', cat: 'lowcal', tags: ['异域风味'], sold: 298, stock: 22, desc: '青咖喱 · 鸡腿肉 · 泰国香米（半份）· 九层塔', fit: ['想吃重口时', '正常饮食日'], caution: ['微辣', '含椰浆'], ai: '想吃重口的日子选它：椰浆减半、香米半份，风味保留、热量比外卖同款低 40%。' },
  { id: 9, name: '蓝莓燕麦松饼', price: 19, kcal: 268, protein: 8, carbs: 42, fat: 7, img: '1567620905732-2d1ec7ab7445', cat: 'lowcal', tags: ['早餐', '低GI'], sold: 476, stock: 28, desc: '全麦松饼 · 新鲜蓝莓 · 无糖枫糖浆', fit: ['早餐', '加餐'], caution: ['含麸质'], ai: '早餐碳水选低 GI 的全麦，蓝莓补花青素。268 kcal 开启稳定供能的一上午。' },
  { id: 10, name: '希腊酸奶莓果杯', price: 16, kcal: 182, protein: 12, carbs: 22, fat: 5, img: '1490645935967-10de6ba17061', cat: 'lowcal', tags: ['加餐', '高蛋白'], sold: 689, stock: 45, desc: '无糖希腊酸奶 · 混合莓果 · 格兰诺拉', fit: ['加餐', '下午茶替代'], caution: ['含乳制品'], ai: '下午馋甜食时的救星：12g 蛋白质、无添加糖，满足感来自莓果本身的甜。' },
  { id: 11, name: '法式全麦吐司配煎蛋', price: 21, kcal: 295, protein: 14, carbs: 33, fat: 12, img: '1484723091739-30a097e8f929', cat: 'lowcal', tags: ['早餐'], sold: 355, stock: 0, desc: '全麦吐司 · 太阳蛋 · 牛油果泥 · 小番茄', fit: ['早餐', '周末早午餐'], caution: ['含鸡蛋', '含麸质'], ai: '经典早餐组合，295 kcal + 14g 蛋白质，配黑咖啡就是完整的晨间仪式。' },
  { id: 12, name: '莓果思慕雪碗', price: 23, kcal: 236, protein: 9, carbs: 41, fat: 4, img: '1495521821757-a1efb6729352', cat: 'lowcal', tags: ['早餐', '抗氧化'], sold: 264, stock: 31, desc: '冻莓果 · 香蕉 · 燕麦奶 · 奇亚籽', fit: ['早餐', '轻断食日'], caution: ['果糖偏高，控糖者少量'], ai: '花青素 + 膳食纤维双高的一碗。奇亚籽带来 omega-3 与饱腹感。' }
]

export const MOCK_COMBOS: MockCombo[] = [
  { id: 101, name: '7 天轻盈计划', price: 169, org: 217, days: 7, img: '1543353071-873f17a7a088', en: '7-DAY PROGRAM', desc: '21 餐搭配好 · 立省 ¥48', kcal: 1850, items: [1, 2, 3, 4], tags: ['减脂', '全周覆盖'] },
  { id: 102, name: '3 日高蛋白冲刺', price: 109, org: 135, days: 3, img: '1467003909585-2f8a72700288', en: '3-DAY PROTEIN', desc: '健身周加持 · 日均 90g 蛋白', kcal: 1620, items: [5, 6, 3, 1], tags: ['增肌', '高蛋白'] },
  { id: 103, name: '5 日办公轻食', price: 129, org: 156, days: 5, img: '1547592180-85f173990554', en: '5-DAY OFFICE', desc: '工作日午餐 · 免纠结', kcal: 1780, items: [2, 7, 8, 4], tags: ['午餐', '通勤'] },
  { id: 104, name: '周末焕新果蔬计划', price: 79, org: 96, days: 2, img: '1511690743698-d9d85f2fbf38', en: 'WEEKEND FRESH', desc: '轻断食 + 果蔬汁 · 重启状态', kcal: 980, items: [10, 12, 4, 9], tags: ['轻断食', '周末'] }
]

/** 首页「今日热量」mock，抄自原型 state.health */
export const MOCK_HEALTH = {
  target: 2000,
  breakfast: 420,
  lunch: 536,
  snack: 356,
  exerciseBurn: 396, // 186 + 210
  macros: [
    { key: 'protein', label: '蛋白质', en: 'PROTEIN', cur: 62, goal: 90, color: '#1E9E62' },
    { key: 'carbs', label: '碳水', en: 'CARBS', cur: 148, goal: 250, color: '#5BBF8A' },
    { key: 'fat', label: '脂肪', en: 'FAT', cur: 35, goal: 65, color: '#A8D9BF' }
  ]
}

/** 首页「AI 今日餐单」静态三条，抄自原型 pgHome 未定制分支 */
export const MOCK_MEALS = [
  { when: '早', en: 'AM', name: '燕麦酸奶杯 + 水煮蛋', status: '已记录', kcal: 420, hot: false, dishId: 0 },
  { when: '午', en: 'NOON', name: '彩虹藜麦能量碗', status: '已记录', kcal: 536, hot: false, dishId: 2 },
  { when: '晚', en: 'PM', name: '香煎三文鱼时蔬', status: 'AI 推荐 · 补足蛋白质缺口', kcal: 285, hot: true, dishId: 3 }
]
