# BiteSmart 数据库 ↔ 后端 ↔ 前端字段一致性全量审计报告

- 审计时间：2026-07-19（只读审计，未改动任何代码）
- 审计范围：`bitesmart.sql` + `docs/sql/` 三份增量迁移 / `BiteSmart/src/main`（entity、controller、DTO、Jackson/MyBatis 配置）/ `BiteSmartMin`（小程序 api + 重点页面）/ `BiteSmart_front`（PC 端 api + 重点视图）
- 全局事实：
  - Jackson 全局配置 `Long → String` 序列化（`JacksonConfig.java`），所有雪花 ID 在 JSON 传输层是 **字符串**；MyBatis `map-underscore-to-camel-case: true`，entity 无 `@TableField` 时列名自动转驼峰。
  - 雪花 ID（`SnowflakeUtil`，EPOCH=2023-11-15）当前约 18 位数字，**超过 JS `Number.MAX_SAFE_INTEGER`（2^53≈16 位）**，前端任何 `Number()` 强转都会丢精度。
  - 统一返回结构 `ResultVO{code,message,data}`；分页为 `PageResultVO{code,message,data:{list,total,pageNum,pageSize,pages}}`。

---

## 一、不匹配清单（按严重程度排序）

### P0 — 会导致运行报错或数据错误

| # | 层级链路 | 错位点 | 影响 | 建议修法 |
|---|----------|--------|------|----------|
| P0-1 | 小程序页面 `Number()` ↔ 后端 Long ↔ DB bigint | 后端已按约定把 Long 序列化为 String，但以下页面把雪花 ID 字符串 `Number()` 强转，精度丢失后得到**另一个 Long**：`pages/order-detail/order-detail.ts:85` `Number(options.id)`；`pages/delivery/delivery.ts:126` `Number(options.orderId)`；`pages/review/review.ts:22`、`pages/complaint/complaint.ts:28` `Number(options.orderId)`；`pages/cart/cart.ts:50/61/80` `Number(dataset.id)`；`pages/health/health.ts:309/321` `Number(dataset.id)`；`pages/food-detail/food-detail.ts:90` `Number(options.id)` | 真实创建的订单/购物车/健康记录（雪花 ID）会出现：订单详情"订单不存在"、配送跟踪 404、评价/投诉绑错订单、购物车改数量/删除打错记录、删除饮食/运动记录删错或 404。种子数据（菜 id 1-12、test_data.sql 小 id）能跑，所以平时不易暴露 | 改前端：api 类型中所有 id 统一 `number \| string`，页面/路由参数全程字符串透传，删除全部 `Number()` 强转（plan.ts 注释里已有此规约，需推广到全部页面） |
| P0-2 | 小程序 `order-detail.ts:52-55` ↔ 后端 `OrderItem` ↔ DB `order_item` | 前端读 `dishName/comboName/name/price/dishImage/comboImage`，后端/DB 实际字段是 `snapshotName/snapshotPrice/snapshotImage/subTotal` | 用户订单详情页所有商品名显示占位"健康餐"、单价显示 `--"、无图片 | 改前端 `buildView` 的 `readStr` 候选键为 `snapshotName/snapshotPrice/snapshotImage` |
| P0-3 | 小程序 `order-detail.ts:73` + `api/order.ts Order.address` ↔ 后端 `Orders.deliveryAddress` ↔ DB `orders.delivery_address` | 前端类型和页面都用 `order.address`，后端字段是 `deliveryAddress` | 订单详情收货地址恒为空白 | 改前端：`api/order.ts` 的 `address` → `deliveryAddress`，页面同步 |
| P0-4 | 小程序 `order-detail.ts:59` ↔ 后端 `OrderStatusLog` ↔ DB `order_status_log` | 前端时间线读 `text/title/status/description/name`，后端实体字段是 `fromStatus/toStatus/reason/operatorType/createTime` | 状态时间线每条都显示占位文案"状态更新"（时间列 createTime 能读到最后一个兜底键，正常） | 改前端：按 `toStatus` 映射状态文案、`reason` 作备注；或后端组装 `text/time` 视图字段 |

### P1 — 显示异常或 undefined

| # | 层级链路 | 错位点 | 影响 | 建议修法 |
|---|----------|--------|------|----------|
| P1-1 | 小程序 `delivery.ts:55` `vehicleMap={1:'电动车',2:'摩托车'}` ↔ 后端 `DeliveryDriver.vehicleType` ↔ DB `delivery_driver.vehicle_type`（10 电动车 / 20 自行车 / 30 汽车） | 枚举值口径完全不同（1/2 vs 10/20/30，且"摩托车"不在后端枚举里） | 用户端配送跟踪页车辆信息永远 fallback 显示"配送车" | 改前端 map 为 `{10:'电动车',20:'自行车',30:'汽车'}` |
| P1-2 | PC `views/merchant/delivery/Delivery.vue:124-127` ↔ 后端 `GET /merchant/delivery/tasks` 返回 `DeliveryTask` 实体 ↔ DB `delivery_task` | 前端读 `row.driverName/driverPhone`，后端实体只有 `driverId`，无联表 | 商家端配送管理页"配送员"列恒为 `-` | 改后端：任务列表联 `delivery_driver` 带出 realName/phone（或实体加非表字段由 service 填充） |
| P1-3 | 小程序 `r-reviews` 页 ↔ 后端 `ReviewMapper.findByDriverId` ↔ DB `review` | 该查询只 `SELECT BaseColumns FROM review`，不联 `sys_user`（对比：`findByMerchantId` 联了 username/nickname/avatar）；前端读 `userNickname/username` | 骑手端"我收到的评价"里，非匿名评价也显示兜底"用户"，看不到昵称 | 改后端：`findByDriverId` 加 `LEFT JOIN sys_user` 补 `username/user_nickname` |
| P1-4 | 后端 `MerchantOrderController.detail` `buyer = sysUserMapper.findById(...)` ↔ 商家端 | `SysUser` 实体的 `password` 字段**无 @JsonIgnore**，整实体直接序列化 | 商家端订单详情响应携带买家**密码哈希**及 email/roleType 等无关字段（信息泄露，前端虽只读 nickname/phone，但响应报文里全量暴露） | 改后端：buyer 改 Map 只挑 `nickname/username/phone`，或 `SysUser.password` 加 `@JsonIgnore` |
| P1-5 | 小程序 `health.ts:21` `GOAL_WEIGHT = 70` 写死 ↔ 后端 `UserProfile.targetWeight` ↔ DB `user_profile.target_weight` | 页面 TODO 注释还写着"user_profile 暂无 targetWeight 字段"，但该列已在 20260718 迁移上线、entity/接口均已返回 | 体重页目标虚线恒为 70kg，与用户问卷填的目标体重无关 | 改前端：`getProfile()` 已调，顺手取 `targetWeight` 替换常量；清理过期 TODO |
| P1-6 | 小程序 delivery 页 / r-task 页 ↔ 后端 tracking / `DeliveryTask` ↔ DB `delivery_task` | 前端读取 `merchantLat/merchantLng/deliveryLat/deliveryLng`，DB 无此四列、后端不返回 | 用户端地图永远不渲染商家/顾客 marker；骑手端"导航到商家/顾客"永远 toast"地址坐标缺失"。前端注释已标注"后端并行开发中"，属**已知数据缺口**而非笔误 | 决策项：要么 DB 加列（建单时地理编码快照），要么前端下线导航/marker 入口 |

### P2 — 口径不一致但能跑（类型定义过期 / 功能缺口 / 文案口径）

| # | 层级链路 | 错位点 | 影响 | 建议修法 |
|---|----------|--------|------|----------|
| P2-1 | PC `api/user/health.ts` 类型 ↔ 后端 `DietRecord/ExerciseRecord` 实体 | `DietRecord` 类型还写着 `mealType: string、dishIds、totalCalories、totalProtein/Fat/Carbs、notes`；`ExerciseRecord` 写 `notes`。后端实际是 `mealType: number、foodName、calories、protein、fat、carbs、sourceType` 和 `remark` | 运行时不报错（接口返回 `Promise<any>`，且 HealthCenterV2 视图用的恰好是正确字段名），但类型定义完全误导，后续开发照类型写字段会得到 undefined | 改 PC 类型定义对齐实体 |
| P2-2 | PC `api/user/profile.ts` `UserProfile{dietaryRestrictions, healthGoals}` ↔ 后端 `UserProfile{dietPreference, allergyInfo, diseaseHistory, healthGoal}` | 类型字段名后端不存在（ProfileCenterV2 运行时用的正确字段，未踩坑） | 同 P2-1，纯类型误导 | 改 PC 类型定义 |
| P2-3 | PC `api/user/membership.ts` 类型 ↔ 后端 `MembershipPlan/UserMembership` 实体 | 类型写 `name/description/durationDays`，后端是 `planName/planType/validDays/benefits/originalPrice`；`UserMembership.planName` 后端实体没有（视图已用 membershipType 映射兜底，运行时无异常） | 类型误导 | 改 PC 类型定义 |
| P2-4 | PC `api/merchant/shop.ts` `MerchantShop` ↔ 后端 `Merchant.openStatus` ↔ DB `merchant.open_status` | PC 类型缺 `openStatus`，且整个 PC 商家端无任何打烊 UI（grep 全站无引用）；打烊开关只在小程序 m-home/m-shop-edit | 功能缺口：PC 商家无法切换营业状态；后端动态更新（`<if openStatus != null>`）保证 PC 全量 PUT 不会误清该字段，无数据风险 | 产品决策：PC 端补开关，或明确"仅小程序可操作" |
| P2-5 | PC `views/user/delivery/TrackingView.vue` ↔ 后端 tracking `driver.vehicleType` | 直接 `{{ tracking.driver.vehicleType }}` 显示原始数字 10/20/30；`api/user/delivery.ts` 类型还写成 `string` | 用户看到"10"而非"电动车" | 前端加枚举映射；类型改 number |
| P2-6 | 小程序 `api/health.ts` `getWeightRecords` 分页分支类型 `{ records: WeightRecord[] }` ↔ 后端 `PageResultVO{data:{list,...}}` 且被 `ResultVO` 再包一层 | 类型写 `records`，后端分页响应是 `list`（且是 ResultVO 套 PageResultVO 的双层结构） | 当前小程序所有页面都不传 page（走后端 List 分支），未触发；一旦有人传 page 会拿不到数据 | 对齐类型为 `{ list, total }`，或后端统一单层分页返回 |
| P2-7 | 小程序问卷 `assessment.ts` 写 `healthGoal: '减重'/'增肌增重'` ↔ DB `user_profile.health_goal` 注释口径"减肥/增肌/维持/控糖" | 自由文本列，不报错；plan 页用 `indexOf('增')` 判断正负号，当前兼容 | 若后续 AI 推荐/统计按枚举精确匹配会漏 | 统一文案枚举（建议以问卷文案为准改 DB 注释，或反向收敛） |
| P2-8 | 前端注释过期 | `api/merchant.ts:14`、`m-home.ts:50`、`m-shop-edit.ts:30`、`api/delivery.ts:38` 仍写"openStatus / 取送坐标 后端并行开发中"——openStatus、reject、driver profile 均已上线 | 注释误导维护者 | 清理注释 |

### 后端返回了、前端类型未声明（不报错，仅记录）

- `GET /delivery/tracking/{orderId}` 顶层同时返回 `currentLat/currentLng` 和 `riderLat/riderLng`（同值冗余），另有 `locTime`；小程序类型已覆盖。
- `GET /merchant/orders/{id}` 响应含 `merchant`（整店铺实体），小程序 `MerchantOrderDetail` 类型未声明（未使用）。
- `GET /driver/reviews/stats` 返回 `rating1Count~rating5Count`，小程序 `DriverReviewStats` 只声明了 `averageRating/totalReviews`（未使用分布数据，可后续做星级分布图）。
- 小程序 `Catalog.Dish` 类型缺 `lockStock/minStockWarning/salesReal`；`MerchantShop` 类型缺 `deliveryRange/businessLicense/licenseNumber` 等——未使用，无影响。
- `PageResultVO` 的 `total/pageNum/pages` 在小程序各处类型里普遍未声明（只用列表本体）。

---

## 二、逐模块一致性结论（完全一致的部分合并简述）

| 模块 | 链路 | 结论 |
|------|------|------|
| plan 模块 | `user_plan/user_plan_meal` ↔ `UserPlan/UserPlanMeal` ↔ `PlanController`（统一 `{plan, meals}`，service `buildPlanDetail` 一致）↔ `api/plan.ts` + `pages/plan` | ✅ 一致。status 10/20/30/40、curDay 0 起、mealIndex 0/1/2、checked 0/1、prefs/avoid/focusParts/tags JSON 字符串口径全部对齐；打卡写 `diet_record(source_type=30, plan_meal_id)` 与迁移脚本一致；mealId 字符串透传正确 |
| 运动库 | `exercise_library` ↔ `ExerciseLibrary` ↔ `GET /api/exercise/library` ↔ `health.ts ExerciseLibraryItem` + `pages/exercise` | ✅ 一致。stdText/kcalPerMin/defMins/defDist/imageUrl/category(aerobic/strength/shape/yoga)/hot 全对齐，页面还有本地常量兜底 |
| 健康三记录 | `diet_record/exercise_record/weight_record` ↔ 三实体 ↔ `HealthRecordController` ↔ `api/health.ts` + `pages/health/exercise` | ✅ 字段一致（mealType 10/20/30/40、carbs、caloriesBurned、bodyFatRate、recordDate/recordTime）。例外：删除操作的 Number() 强转见 P0-1；目标体重写死见 P1-5；分页类型见 P2-6 |
| 健康档案 | `user_profile`（含 20260718 新列）↔ `UserProfile` ↔ `UserProfileController`（GET/PUT）↔ `api/user.ts` + `pages/assessment/profile` | ✅ 一致。targetWeight/exerciseFreq/focusParts 落库（mapper XML 动态 update 含三列）、回读、问卷提交（JSON.stringify）全链路对齐；activityLevel 10-40、gender 10/20 一致 |
| merchant.openStatus | `merchant.open_status` ↔ `Merchant.openStatus` ↔ `MerchantShopController`（动态 update 含 openStatus）↔ `api/merchant.ts` + `m-home/m-shop-edit` | ✅ 小程序链路一致（10 营业中/20 打烊，缺省兜底营业中）。PC 端缺口见 P2-4 |
| delivery_task 新列 | `reject_reason/order_remark` ↔ `DeliveryTask` ↔ 骑手 reject（`@RequestParam reason`）/建任务快照 `setOrderRemark(order.getRemark())` ↔ `api/delivery.ts` + `r-task` | ✅ 一致。备注快照在出餐建任务时拷贝，拒单 reason 走 query，两端一致 |
| rider_location ↔ tracking | `rider_location` ↔ `RiderLocation` ↔ `POST /driver/location`（body `{taskId, latitude, longitude}`，GCJ-02，校验任务归属+状态 30/40）↔ `GET /delivery/tracking/{orderId}` 的 `path[{latitude, longitude, time}]` + `riderLat/riderLng/locTime` ↔ delivery 页 polyline | ✅ 一致。polyline 直接用 path 点、时间升序，坐标系约定两端一致（gcj02） |
| dish 新列 | `dish.tags/ai_comment/fit_scenes/cautions` ↔ `Dish` ↔ 菜品接口 ↔ `catalog.ts` + `food-detail` + `m-dish-edit` | ✅ 一致。JSON 数组字符串两端 parse/stringify 对齐，food-detail 还有 mock 兜底 |
| 订单/会员枚举 | `orders.order_status` 10-80 ↔ 小程序 order/order-detail/merchant.ts 状态文案 ↔ PC 订单页 | ✅ 枚举口径一致（10 待支付/20 待接单/30 备餐中/40 配送中/50 已完成/60 已取消/70 退款中/80 已退款）；`delivery_status` 0/10/20/30/40 一致；会员 membershipType 10/20/30、status 10/20/30 两端一致。商家订单详情结构 `{order, items, buyer, statusTimeline}` 与小程序类型一致（buyer 安全问题见 P1-4） |
| 骑手 profile/结算/统计 | `delivery_driver` ↔ `DeliveryDriver` ↔ `/driver/profile`（GET/PUT）、`/driver/status`、`/driver/settlements(+stats)`、`/driver/reviews(+stats)` ↔ `api/delivery.ts` + `r-profile/r-me/r-stats` | ✅ 一致。统计键名（totalDeliveryFee/pendingAmount/averageRating/totalReviews 等）与前端类型逐项对齐 |
| 商家统计 | `GET /merchant/statistics/today|daily|top-dishes` ↔ `api/merchant.ts` + `m-home/m-stats` | ✅ 键名一致（orderCount/revenue/pendingOrderCount/date/totalQuantity） |

---

## 三、修复优先级建议

1. **先修 P0-1**（Number 强转雪花 ID）：影响面最大且随真实数据增长必然爆发；改动集中在小程序 6 个页面 + 若干部 api 类型，属于机械替换。
2. **P0-2/3/4**（订单详情三处字段名）：用户下单后必看的页面，建议与 P0-1 同批修。
3. **P1-4**（buyer 泄露密码哈希）：安全项，后端一行 `@JsonIgnore` 或改 Map 即可，建议立即修。
4. P1-1/2/3/5 为体验修复，可排一个迭代；P1-6 需要产品决策（加列 or 下线入口）。
5. P2 类型定义批量对齐可放技术债清单，不阻塞功能。
