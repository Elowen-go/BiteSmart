# BiteSmart 小程序 ↔ 后端接口对接摸底调查报告

调查日期：2026-07-16（只读，未改任何代码）
范围：`BiteSmart/`（Spring Boot 后端）、`BiteSmartMin/miniprogram`（微信小程序）、`bitesmart.sql`

---

## 0. TL;DR

- **健康三表（饮食/运动/体重）后端 CRUD 齐全**：`/api/health/diet|exercise|weight`，小程序 `api/health.ts` 也已封装，但**没有任何页面引用它** —— health/exercise/plan/index 页全部走本地 mock（storage 键 `bitesmart_health_v1`）。
- **健康档案有后端**：`/api/user/profile` GET/PUT（表 `user_profile`），但小程序 assessment/plan 页用的是本地 mock 档案（storage 键 `bitesmart_plan_profile_v1`），字段结构差异较大。
- **"专属计划/食谱"后端完全没有**：没有 plan 表、没有计划接口。AI 相关只有 `/api/ai/recommend`（单次推荐 Map）和 `/api/ai/chat`。plan 页的 7 天计划、打卡、换菜全部本地 mock，属于 **B 类（缺接口）或 C 类（可保留本地）**。
- **运动库后端没有**：没有 exercise_library 表/接口，EX_LIB 只有 mock。属 B 类（也可接受 C 类：前端常量）。
- **菜品/套餐接口字段基本够用**（kcal/protein/fat/carbs/img 都有，名为 calories/dishImage），但**没有 tags、ai 点评、fit、caution** 四个字段，mock 里 UI 重度使用。
- 字段命名有系统性不一致：小程序接口层用 `carbohydrate/bodyFat/mealType:string`，后端是 `carbs/bodyFatRate/mealType:int`。

---

## 1. 后端接口清单

统一约定：路径前缀 `/api`，返回 `ResultVO { code, message, data }`；分页用 PageHelper，分页返回 `{records,...}`。认证 JWT Bearer，`LoginUser` 注入。

### 1.1 用户端（小程序直接相关）

| Controller | 端点 | 说明 |
|---|---|---|
| AuthController `/api/auth` | POST `/register` `/login` `/wechat-login` `/wechat-bind` `/logout`；PUT `/credentials` | 返回 `{token, tokenType, expireTime, user, accountSetupRequired?}` |
| UserController `/api/users` | GET/PUT `/me` | 当前用户资料 |
| UserProfileController `/api/user/profile` | GET、PUT | **健康档案**，一人一栏 |
| UserAddressController `/api/user/addresses` | GET/POST（基础路径）、PUT `/{id}`、DELETE `/{id}` | 收货地址 CRUD |
| MembershipController `/api/user/membership` | GET `/plans` `/status` `/history`；POST `/buy/{planId}` | status 返回 `UserMembership` 或 null |
| DishController `/api/dishes` | GET（keyword/categoryId/sort/page/size）、GET `/categories`、GET `/{id}` | 免登录 |
| ComboController `/api/combos` | GET、GET `/{id}`（返回 `{combo, dishRels}`）、POST `/{id}/replace?oldDishId&newDishId` | 换菜自动重算营养 |
| ShoppingCartController `/api/cart` | GET/POST、PUT `/{id}`、PUT `/{id}/select`、PUT `/{id}/replace`、DELETE `/{id}` | |
| UserOrderController `/api/orders` | POST（表单：address/receiverName/receiverPhone/remark）、POST `/batch`、GET、GET `/{id}`（`{order, items, statusTimeline}`）、POST `/{id}/cancel`、POST `/{id}/pay`（payMethod 10 支付宝沙箱 / 其他 mock 支付） | |
| HealthRecordController `/api/health` | 见 1.3 | |
| AiRecommendController `/api/ai` | POST `/recommend`（body `AiRecommendRequest`，返回 `Map<String,Object>`） | 需健康档案 |
| AiChatController `/api/ai` | POST `/chat`（表单 question/sessionId）、GET `/chat/history?sessionId` | |
| DeliveryTrackingController `/api/delivery/tracking` | GET `/{orderId}` | 含 taskStatus、骑手位置 |
| UserReviewController `/api/reviews` | POST（?orderId=）、GET `/my` | |
| ComplaintController `/api/complaints` | POST | |
| NoticeController `/api/notices` | GET | 免登录 |
| FileController `/api/files` | POST `/upload`、GET `/download/**` | |

### 1.2 管理端/商家端/骑手端（小程序骑手、商家页理论上可用）

- `/api/merchant/**`：auth(apply/status)、orders(accept/reject/prepare/done)、dishes、combos、categories、ingredients、shop、statistics(today/period/top-dishes/daily/category-revenue)、delivery/tasks、finance、inventory/warnings、reviews
- `/api/driver/**`：register、status、location、tasks(accept/pickup/deliver/exception)、settlements、reviews
- `/api/admin/**`：users、merchants、orders、drivers、dishes、combos、categories、ingredients、nutrition、ai-rules、reviews、notices、work-orders(refunds/complaints)、statistics、finance、system
- `/api/payment/alipay/notify` 回调

### 1.3 健康域重点结论

**饮食/运动/体重 CRUD —— 全部已有：**

| 端点 | 方法 | 入参 | 返回 |
|---|---|---|---|
| `/api/health/diet` | GET | `?date=yyyy-MM-dd`（可选） | `List<DietRecord>` |
| `/api/health/diet` | POST | DietRecord JSON | — |
| `/api/health/diet/{id}` | PUT / DELETE | | |
| `/api/health/exercise` | GET / POST | 同上 | `List<ExerciseRecord>` |
| `/api/health/exercise/{id}` | DELETE | | （无 PUT） |
| `/api/health/weight` | GET | `?page&size`（可选） | List 或分页 |
| `/api/health/weight` | POST | WeightRecord JSON | 每日唯一（upsert 语义，"保存"） |

字段（与 `bitesmart.sql` 一致）：
- `diet_record`：recordDate, recordTime, **mealType int（10 早/20 午/30 晚/40 加餐）**, foodName, quantity, **calories, protein, fat, carbs**, sourceType(10 订单导入/20 手动), orderItemId
- `exercise_record`：recordDate, **exerciseType string（自由文本）**, duration(分钟), distance(km), **caloriesBurned**, remark
- `weight_record`：recordDate（user+date 唯一）, weight, **bodyFatRate**, bmi

**健康档案 —— 已有**：`/api/user/profile`，表 `user_profile`：age, gender(10/20), height, weight, activityLevel(10-40), dietPreference(JSON), allergyInfo(JSON), diseaseHistory(JSON), healthGoal, dailyCalorieTarget(AI 算)。
**没有**：targetWeight、运动频次 freq、计划部位 parts、忌口 avoid（可用 allergyInfo 凑）、planDays、curDay、checks 等"计划"字段。

**专属计划/食谱 —— 完全没有**：无 plan 表、无 plan controller。AI 只有单次 `/api/ai/recommend`（不持久化计划）和 `/api/ai/chat`（会话存 `ai_conversation` 表）。

**运动库 —— 完全没有**：无 exercise_library 表/接口；`exercise_record.exerciseType` 只是自由文本。

### 1.4 菜品/套餐返回字段

`Dish`：id, merchantId, categoryId, **dishName, dishImage**, price, originalPrice, stock, lockStock, salesCount, salesReal, unit, description, **suitableFor(JSON 字符串)**, **calories, protein, fat, carbs**, status, ingredients（非表字段）。
`Combo`：id, merchantId, **comboName, comboImage**, price, originalPrice, comboType(10 减脂/20 增肌/30 控糖/40 会员), suitableFor, **totalCalories, totalProtein, totalFat, totalCarbs**, description, replaceableDishPool, maxReplaceCount, status, salesCount。详情额外返回 `dishRels`（combo_dish_rel：dishId, quantity, isFixed）。

**缺失（mock 里有、后端没有）**：`tags`（可用 suitableFor 部分替代）、`ai`（AI 点评文案）、`fit[]`（适用场景）、`caution[]`（忌口提醒）、英文副标题 `en`、套餐 `days`。

### 1.5 订单/配送状态枚举（SQL 注释真实定义）

- `orders.order_status`：**10 待支付 / 20 待接单 / 30 备餐中 / 40 配送中 / 50 已完成 / 60 已取消 / 70 退款中 / 80 已退款**
- `orders.delivery_status`：0 未配送 / 10 待取餐 / 20 已取餐 / 30 配送中 / 40 已送达
- `delivery_task.task_status`：10 待接单 / 20 待取餐 / 30 已取餐 / 40 配送中 / 50 已送达 / 60 异常 / 70 已取消
- `pay_method`：10 支付宝 / 20 微信

### 1.6 会员状态接口

`GET /api/user/membership/status` → `UserMembership | null`：id, planId, **membershipType(10 月卡/20 季卡/30 年卡)**, **status(10 生效中/20 已过期/30 已退款)**, startTime, endTime, orderId, payAmount。小程序 `member.ts` 已对接。

---

## 2. 小程序 API 层现状（`api/*.ts`）

BASE_URL 硬编码 `http://localhost:8080/api`（`utils/request.ts`），统一解包 `ResultVO.data`，`code>=400` reject。

| 文件 | 封装方法 → 路径 | 页面使用情况 |
|---|---|---|
| auth.ts | loginWithWechat `/auth/wechat-login`、loginWithAccount `/auth/login`、setupCredentials PUT `/auth/credentials`、bindWechat `/auth/wechat-bind` | login、account-setup 使用；**bindWechat 无页面用** |
| catalog.ts | getDishes `/dishes`、getDish `/dishes/{id}`、getCombos `/combos`、getCombo `/combos/{id}` | food、food-detail、combo、combo-detail 使用（均带 mock fallback） |
| cart.ts | getCart、addToCart、updateCartQuantity、selectCartItem、deleteCartItem → `/cart*` | cart/checkout/food-detail/combo-detail/food/index/plan 使用 |
| order.ts | getOrders、getOrderDetail、createOrder(表单)、cancelOrder、payOrder → `/orders*` | order、order-detail、checkout、pay-result 使用 |
| **health.ts** | getDietRecords/addDietRecord `/health/diet`、getExerciseRecords/addExerciseRecord `/health/exercise`、getWeightRecords/saveWeightRecord `/health/weight` | **⚠️ 零页面引用**（后端还有 diet PUT、diet/exercise DELETE 未封装） |
| ai.ts | recommend POST `/ai/recommend`、chat POST `/ai/chat`、getChatHistory `/ai/chat/history` | ai、chat 使用 |
| user.ts | getProfile/saveProfile `/user/profile`、地址 CRUD、getMembershipPlans/getMembershipStatus/buyMembership | address/checkout/member 使用；**getProfile/saveProfile（健康档案）无页面用** |
| delivery.ts | getTracking `/delivery/tracking/{orderId}`；骑手组 getDriverTasks/accept/pickup/deliver/status/settlements | delivery 页用 getTracking；**骑手组全部无页面用** |
| feedback.ts | createReview `/reviews?orderId=`、createComplaint `/complaints` | review、complaint 使用 |
| notice.ts | getNotices `/notices` | notice 使用 |
| merchant.ts | getMerchantTodayStats、getMerchantOrders、accept/prepare/finish、getMerchantDishes | **全部无页面用**（小程序暂无商家端页面） |

**接口层与后端的字段错位（对接时必踩）**：
- health.ts：`carbohydrate` vs 后端 `carbs`；`bodyFat` vs `bodyFatRate`；`mealType: string` vs 后端 int 枚举；exercise 用 `calories` vs 后端 `caloriesBurned`
- catalog.ts：`name/image/carbs`
- order.ts：创建订单用 `contentType:'form'` + data 对象，与后端 `@RequestParam` 匹配 ✓

---

## 3. Mock 使用点清单

**storage 键**：`bitesmart_health_v1`（饮食/运动/体重记录）、`bitesmart_plan_profile_v1`（计划档案）、`bitesmart_asm_draft`（问卷草稿，assessment→plan 传递）。另 `aiSessionId`（chat）、`selectedAddress`（checkout）是会话级缓存，可保留。

| 页面 | mock 内容 | 对应真实接口 |
|---|---|---|
| **index** | MOCK_HEALTH（今日热量环/三大营养素/三餐已摄入）、MOCK_MEALS（AI 今日餐单）、MOCK_COMBOS[0]（推荐套餐卡）、MOCK_DISHES（餐单图片） | 热量环=GET `/health/diet?date=today`+`/health/exercise?date=today`+`user_profile.dailyCalorieTarget`；AI 餐单=POST `/ai/recommend`；推荐套餐=GET `/combos` |
| **food** | MOCK_DISHES/MOCK_COMBOS 作 fallback + 分类 | GET `/dishes`、`/combos`、`/dishes/categories`（已接，fallback 可去） |
| **food-detail** | MOCK_DISHES 兜底 + `ai/fit/caution` 展示 | GET `/dishes/{id}`（**ai/fit/caution 后端无字段**） |
| **combo / combo-detail** | MOCK_COMBOS 兜底、套餐天数 dayMeals、换菜候选=全量 MOCK_DISHES | GET `/combos`、`/combos/{id}`、POST `/combos/{id}/replace`（购物车侧另有 PUT `/cart/{id}/replace`） |
| **cart** | 用 MOCK_DISHES/MOCK_COMBOS 按 id 补名称/图片 | GET `/cart` 返回项已带 dishName/comboName/dishImage/comboImage，**mock 补全可直接删** |
| **health** | mock/health 的饮食/运动/体重全部增删查 + 周消耗柱状 | GET/POST/DELETE `/health/diet` `/health/exercise` `/health/weight`（字段需映射） |
| **exercise** | EX_LIB 运动库 + addExerciseRecord | **运动库无后端**；记录=POST `/health/exercise` |
| **assessment** | ASM_GOALS/ACTS/FREQS/PARTS/PREFS/AVOID 问卷选项 + 草稿键 | 选项可前端常量；结果落库=PUT `/user/profile`（字段需映射/扩展） |
| **plan** | planMeals（7 天三餐）、applyAssessment、打卡 checks、换菜 swaps | **计划整体无后端**；单餐"加入购物车"已用 addToCart |
| **ai** | MOCK_DISHES[0] 兜底推荐卡 | POST `/ai/recommend`（已接） |
| **profile** | 静态 stats（连续记录天数/当前体重/本月变化）、goal 文案 | 体重=GET `/health/weight`；连续天数无接口（可前端算） |
| chat/checkout/order/order-detail/pay-result/delivery/notice/member/address/review/complaint/login/account-setup/settings | 无 mock（settings 纯 UI） | — |

---

## 4. 差距清单（页面 ↔ 后端）

### A 类：后端已有，直接对接（工作量在字段映射）

| 页面/模块 | 对接点 | 注意 |
|---|---|---|
| health 页-饮食记录 | `/health/diet` CRUD | mealType 字符串↔int 枚举；carbs≠carbohydrate；DELETE 需在 api 层补封装 |
| health 页-体重 | `/health/weight` GET/POST | bodyFat→bodyFatRate；每日一条 upsert |
| health 页-运动记录 | `/health/exercise` GET/POST/DELETE | calories→caloriesBurned；无 PUT |
| index 页-今日热量环 | `/health/diet?date` + `/health/exercise?date` + `/user/profile` 的 dailyCalorieTarget | 营养素 goal 后端没有，需前端按比例算或扩展 profile |
| food/food-detail/combo/combo-detail | 去 mock fallback，纯走 `/dishes`、`/combos` | 字段改名 dishName/dishImage/carbs |
| cart | 删除 MOCK 补全逻辑 | 后端 CartItem 已带名称/图片字段 |
| assessment→档案保存 | PUT `/user/profile` | 映射：goal→healthGoal、activity→activityLevel、prefs→dietPreference(JSON)、avoid→allergyInfo(JSON)；freq/parts/targetWeight 无处可放（见 B） |
| profile 页-当前体重/本月变化 | GET `/health/weight` | 直接算 |
| index-AI 餐单、ai 页 | POST `/ai/recommend` | 已封装，需按真实返回 Map 结构调整渲染 |

### B 类：后端缺接口/表，需要新增

| 需求 | 建议新增 |
|---|---|
| **专属计划**（7 天计划生成/查看/进度） | 表 `user_plan`（id, user_id, goal, plan_days, cur_day, start_date, status, gen_time）+ `user_plan_meal`（plan_id, day_index, meal_type, dish_id/combo_id, kcal 快照, swapped_from）；接口 `GET/POST /api/plan`、`POST /api/plan/{id}/start`、`POST /api/plan/meal/{id}/check`、`POST /api/plan/meal/{id}/swap`（可复用 AI 生成） |
| **问卷扩展字段** | `user_profile` 加列：target_weight、exercise_freq、focus_parts(JSON)；或由 plan 表承载 |
| **运动库** | 表 `exercise_library`（name, category, kcal_per_min, std_desc, image, tags JSON）+ `GET /api/health/exercise/library`（或接受前端常量，降为 C 类） |
| **菜品 AI 点评/适用场景/忌口提醒** | `dish` 加列 `ai_comment`、`fit_scenes`(JSON)、`cautions`(JSON)；`dish_nutrition` 表已存在但用户端无查询接口，可挂到 `/dishes/{id}` 返回 |
| **菜品 tags** | 可用现有 `suitable_for` JSON 顶上，或加 `tags` 列 |
| **首页营养素目标** | profile 加 protein/fat/carbs target 列，或 `/ai/recommend` 返回里带 |
| **连续记录天数** | `GET /api/health/stats`（可选，前端也能算） |
| **订单→饮食记录自动导入** | 表结构已留 sourceType=10/orderItemId，需核查 OrderService 完成订单时是否实际写入（本次未验证） |

### C 类：纯前端，保留本地即可

- 计划打卡 UI 态（checks/swaps）——若不做 B 类计划表，继续用 `bitesmart_plan_profile_v1`
- 问卷选项常量（ASM_GOALS/ACTS/FREQS/PARTS/PREFS/AVOID）、人体部位高亮图 PART_OVERLAYS
- 运动库 EX_LIB（若产品接受静态库）
- `bitesmart_asm_draft` 问卷草稿、`aiSessionId`、`selectedAddress`
- settings 页全部、profile 页静态菜单

---

## 5. 后端配置与本地启动

- **端口 8080**，无 context-path（接口即 `/api/**`）；小程序 BASE_URL 硬编码 `http://localhost:8080/api` 匹配 ✓
- **MySQL**：`localhost:3306/bitesmart`，root/123456（环境变量可覆盖）；**Redis**：localhost:6379，密码 123456，db0（根目录有 `redis-dev.conf`、`dump.rdb`、`appendonlydir/` 和 redis 日志，本地确实起过）
- **启动方式**：Maven —— 日志显示用 `spring-boot-maven-plugin:3.2.5:run`（即 `mvnw spring-boot:run`）；`backend-local.log` 记录一次 51 分钟后 BUILD FAILURE（进程被杀），`backend-statistics.log`/`backend-finance.log` 有成功启动记录（Tomcat initialized with port 8080）。`BiteSmart/target/` 存在，也可 package 后跑 jar
- **其他依赖**：DeepSeek API（`AI_API_KEY` 环境变量，未配则 AI 接口不可用）、微信 appid/secret（application-local.yml 已配真实值）、支付宝沙箱（local profile 启用）。**注意：`application-local.yml` 含支付宝私钥与微信 secret，不应提交入库**
- 静态资源：`./uploads` 目录映射 + FileController 上传

## 6. 对接建议优先级

1. **P0（纯映射，收益最大）**：health 页三记录 → `/api/health/*`；food/combo 系去 mock fallback；cart 去 mock 补全。顺修 api 层字段名（carbs/bodyFatRate/caloriesBurned/mealType 枚举）。
2. **P1**：assessment → PUT `/user/profile`（先映射已有字段）；index 热量环接真实数据。
3. **P2（需后端开发）**：计划表 + `/api/plan/*`；菜品 ai/fit/caution/tags 扩展；运动库（或定调为前端常量）。
