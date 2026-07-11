# 商家端工作台界面优化计划

## 需求分析

当前商家工作台存在以下问题：
1. **数据显示 `undefined`**：后端返回字段名与前端期望不匹配
2. **数据维度不足**：仅显示4个基础指标，缺少商家关心的核心数据
3. **界面单调**：缺少图标化展示和可视化图表

目标：打造一个数据丰富、视觉专业的商家工作台，包含多维度统计卡片和可视化图表。

## 当前问题分析

### 字段名不匹配
| 后端返回字段 | 前端期望字段 | 问题 |
|-------------|-------------|------|
| `todayOrders` | `orderCount` | 不匹配导致显示 undefined |
| `todaySales` | `revenue` | 不匹配导致显示 undefined |
| `totalQuantity` | `soldCount` | 热销菜品排行字段不匹配 |
| - | `newUserCount` | 后端未提供 |
| - | `avgOrderAmount` | 后端未提供 |

## 优化方案

### 1. 后端接口增强

**修改文件**: `StatisticsService.java`

**新增统计指标**:
| 指标 | 字段名 | 说明 |
|------|--------|------|
| `orderCount` | 今日订单数 | 替换 `todayOrders` |
| `revenue` | 今日营收 | 替换 `todaySales` |
| `newUserCount` | 新用户数 | 新增 |
| `avgOrderAmount` | 平均订单金额 | 新增 |
| `pendingOrderCount` | 待处理订单数 | 新增 |
| `stockAlertCount` | 库存预警数 | 新增 |
| `reviewCount` | 今日评价数 | 新增 |

### 2. 前端界面优化

**修改文件**: `Dashboard.vue`

**新增数据卡片**（共7个）:
| 卡片 | 图标 | 数据来源 |
|------|------|----------|
| 今日营收 | Money | revenue |
| 今日订单 | ShoppingCart | orderCount |
| 新用户数 | UserFilled | newUserCount |
| 平均订单金额 | Coin | avgOrderAmount |
| 待处理订单 | Clock | pendingOrderCount |
| 库存预警 | AlertTriangle | stockAlertCount |
| 今日评价 | Message | reviewCount |

**新增图表区域**:
- 营收趋势图（近7天）
- 订单状态分布图

### 3. 前端API更新

**修改文件**: `statistics.ts`

更新接口类型定义，匹配后端返回的新字段名。

## 实施步骤

### 步骤一：修改后端统计服务
- 更新 `getTodayStats()` 返回字段名
- 添加新统计指标（待处理订单、库存预警、评价数）

### 步骤二：修改前端API定义
- 更新 `TodayStats` 接口定义
- 添加新字段类型

### 步骤三：优化前端Dashboard界面
- 添加更多数据卡片（7个）
- 添加营收趋势折线图
- 添加订单状态分布饼图

### 步骤四：构建验证
- 前端构建验证
- 后端重启验证

## 风险与注意事项

1. **字段名变更兼容性**：修改后端返回字段名可能影响其他调用方，需同步更新所有前端代码
2. **数据库查询性能**：新增统计指标可能增加数据库查询压力，需优化SQL
3. **图标资源**：确保使用的Element Plus图标已正确导入

## 验证方案

1. 检查今日营收和订单数是否正常显示（非undefined）
2. 检查新增的待处理订单、库存预警、评价数卡片是否显示
3. 检查热销菜品排行是否正常显示
4. 检查营收趋势图和订单状态分布图是否渲染