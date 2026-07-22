/**
 * 健康模块本地常量（内容抄自高保真原型）。
 * 仅保留仍被引用的 C 类常量：
 * - EX_LIB：运动库兜底（exercise 页在 GET /exercise/library 失败时回退）
 * - ASM_* / PART_OVERLAYS：问卷选项常量（assessment 页）
 * 计划档案（bitesmart_plan_profile_v1）与健康记录 mock 已随 B 类接口上线删除，
 * 计划执行状态以后端 user_plan / user_plan_meal 为准。
 */

/* ---------------- 类型 ---------------- */

export interface ExLibItem {
  id: number
  cat: 'aerobic' | 'strength' | 'shape' | 'yoga'
  hot?: number
  name: string
  std: string
  per: number // kcal / 分钟
  defMins: number
  defDist: number
  img: string
  tags: string[]
}

/* ---------------- 常量（抄自原型） ---------------- */

export const EX_LIB: ExLibItem[] = [
  { id: 1, cat: 'aerobic', hot: 1, name: '户外跑步', std: '331kcal/5km', per: 11, defMins: 30, defDist: 5, img: '1552674605-db6ffd4facb5', tags: ['全身', '有氧', '户外'] },
  { id: 2, cat: 'aerobic', hot: 1, name: '跑步机跑步', std: '248kcal/30分钟', per: 8, defMins: 30, defDist: 0, img: '1576678927484-cc907957088c', tags: ['全身', '有氧', '室内'] },
  { id: 3, cat: 'aerobic', hot: 1, name: '户外行走', std: '162kcal/4km', per: 5, defMins: 40, defDist: 4, img: '1476480862126-209bfaa8edc8', tags: ['低冲击', '户外'] },
  { id: 4, cat: 'aerobic', name: '户外骑行', std: '258kcal/10km', per: 8, defMins: 40, defDist: 10, img: '1485965120184-e220f721d03e', tags: ['下肢', '有氧', '户外'] },
  { id: 5, cat: 'aerobic', name: '游泳', std: '330kcal/30分钟', per: 11, defMins: 30, defDist: 0, img: '1530549387789-4c1017266635', tags: ['全身', '有氧', '低冲击'] },
  { id: 6, cat: 'strength', hot: 1, name: '力量训练', std: '210kcal/30分钟', per: 7, defMins: 30, defDist: 0, img: '1517836357463-d25dfeac3438', tags: ['力量', '室内'] },
  { id: 7, cat: 'shape', name: '哑铃塑形', std: '180kcal/30分钟', per: 6, defMins: 30, defDist: 0, img: '1581009146145-b5ef050c2e30', tags: ['塑形', '上肢'] },
  { id: 8, cat: 'yoga', name: '瑜伽', std: '120kcal/30分钟', per: 4, defMins: 30, defDist: 0, img: '1544367567-0f2fcb009e0b', tags: ['柔韧', '舒缓'] }
]

export const ASM_GOALS = [
  { n: '减重', d: '温和热量缺口，健康掉秤不掉营养', img: '1512621776951-a57141f2eefd' },
  { n: '增肌增重', d: '高蛋白盈余饮食，配合力量训练', img: '1517836357463-d25dfeac3438' },
  { n: '提升健康水平', d: '均衡膳食结构，改善代谢与精力', img: '1498837167922-ddd27525d352' },
  { n: '缓解压力', d: '抗炎舒缓食材，照顾情绪与睡眠', img: '1506126613408-eca07ce68773' }
]

export const ASM_ACTS = [
  { n: '久坐不动型', d: '极少运动，日常以坐姿为主', img: '1497032628192-86f99bcd76bc' },
  { n: '轻度活跃型', d: '偶尔运动，少量步行与家务为主', img: '1476480862126-209bfaa8edc8' },
  { n: '中度活跃型', d: '每日 5000-10000 步，每周 2-8h 运动', img: '1552674605-db6ffd4facb5' },
  { n: '重度活跃型', d: '每日 10000 步以上，每周 8-14h 运动', img: '1581009146145-b5ef050c2e30' }
]

export const ASM_FREQS = ['0-1 次', '2-3 次', '4-5 次', '6 次以上']
export const ASM_PARTS = ['全身', '肩部', '胸部', '背部', '手臂', '腰腹部', '臀部', '腿部']
export const ASM_PREFS = ['少油少盐', '高蛋白', '低糖', '素食', '地中海', '川湘重口']
export const ASM_AVOID = ['坚果', '海鲜', '乳制品', '香菜', '辣', '麸质']

/** 问卷部位 -> 人体模型高亮图（front/back 两个视角） */
export const PART_OVERLAYS: Record<string, { f?: string; b?: string }> = {
  全身: { f: 'f-full', b: 'b-full' },
  肩部: { f: 'f-shoulder' },
  胸部: { f: 'f-chest' },
  背部: { b: 'b-back' },
  手臂: { f: 'f-arm', b: 'b-arm' },
  腰腹部: { f: 'f-waist', b: 'b-waist' },
  臀部: { f: 'f-hip', b: 'b-hip' },
  腿部: { f: 'f-leg', b: 'b-leg' }
}
