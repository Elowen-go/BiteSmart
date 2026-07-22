# BiteSmart PC 用户端 UI/UX 评审报告

- 评审范围：`BiteSmart_front/src/views/user/` 全部 16 个页面 + `layouts/UserBrandLayout.vue`（线上布局）+ `layouts/UserLayout.vue`（死代码）+ 路由配置
- 设计基准：牛油果绿 #1E9E62 + 墨绿 #123326、英文小标、1px hairline、8-10px 微角、tabular-nums、米白底、无 emoji（参照 `views/merchant/Dashboard.vue`）；P0 全局令牌已入库（tokens.scss L66-170），Element 主题已全局收敛
- 评审方式：只读源码评审（未运行页面）
- 日期：2026-07

---

## 一、总体评分与印象

**总体评分：5.5 / 10**

| 维度 | 得分 | 说明 |
|---|---|---|
| 视觉一致性 | 4.0 | 用户端内部就存在**两套视觉体系**（editorial 杂志风 vs 后台卡片风），且 editorial 色板（#1f4d3a + 陶土橙）与基准牛油果绿完全不同，多个页面还用局部覆盖把 Element 主色改回去，与 P0 全局主题对打 |
| 信息架构 | 5.5 | 逛-选-买-跟踪主链路完整；但导航入口残缺（投诉/地址/评价无入口）、部分页面功能不可用（投诉手填雪花 ID）或半残（公告分页失效、跟踪无地图） |
| 数据展示 | 5.0 | 卡片节奏和图片运用在 C 端达标；但后端新字段（tags/ai_comment/fit_scenes/cautions/targetWeight）零展示，raw 雪花 ID 直接暴露给用户 |
| 交互质量 | 6.5 | 加购反馈、购物车跨店结算、套餐换菜、订单支付链路都有完整状态处理，是亮点；空态/骨架屏在首页和菜品页做得认真 |
| 工程整洁度 | 4.0 | 补丁堆叠式 CSS（同一选择器在一个文件里重写 5-7 遍、RecommendCenterV2 有 4 个 style 块）、整页压缩单行、死布局文件、路由注释垃圾、emoji 残留 |

**总体印象**：用户端是"一半是精心设计、一半是脚手架填坑"的产品。首页/菜品/购物车/订单这条主链路有一套自己的 editorial 杂志语言（衬线标题、#1f4d3a 深绿、陶土橙点缀、大留白），单看不乏设计感；但它既不与商家端基准同源，也没在用户端内部贯彻到底——地址/公告/评价/投诉/AI对话/配送 6 个页面直接套用商家端那套 h3 卡片后台样式，C 端用户会在一次会话里经历两种产品。更棘手的是工程债：HomeEditorial 的 CSS 是十几次迭代的覆盖堆叠，样式 correctness 完全依赖源码顺序；后端为 C 端准备的内容字段（AI 点评、适宜场景、慎食提示、目标体重）一个都没落地到界面。当前最需要的是一次"定调"决策：editorial 语言是作为 C 端正典入库，还是全面向基准靠拢——然后据此收敛。

---

## 二、共享层评审（布局 / 路由 / 主题）

### 2.1 UserBrandLayout.vue —— 线上 C 端外壳（6.5/10）

**现状**：sticky 顶栏（品牌字标 + 5 个导航 + 搜索/公告/购物车/账户下拉）+ router-view + 页脚，购物车角标通过 `cart-updated` 事件与 api 层联动（cart.ts L22/37 派发），机制是通的。

**主要问题**

- [高] **导航信息架构残缺**：顶栏只有 首页/菜品/健康套餐/AI食谱/我的订单 5 项 + 下拉 3 项（个人中心/健康记录/会员中心）。**地址管理、我的评价、提交投诉没有任何入口**（商家入驻入口随死掉的 UserLayout 一起丢失）；系统公告只靠一个小铃铛图标。用户无法发现近一半功能页面。
- [中] 自有第三套色板：品牌绿 `#1f4d3a`、强调橙 `#e87955`、底 `#f7f8f5`、hairline `#e5ebe4`（L32），与基准 #1E9E62/#123326 和 editorial 页内的 #cf704f 都不一致——同一个橙，布局用 #e87955、首页用 #cf704f、AI 推荐页用 #c66e4e。
- [中] 整个 template/style 压缩成单行（L30-34），账户下拉按钮样式一串 `!important`（L32 截断处），与商家端 MerchantLayout 的店铺选择器是同款"调不通就 !important"手法。
- [低] 自绘 `button[title]::after` tooltip（L33）与 Element tooltip 体系并存；导航 active 指示条用 #e87955 橙，hover 变绿，色彩语义随意。
- [低] 品牌字标 `.brand-mark` 用 Georgia 衬线 "B"，与页面内 "Source Han Serif SC" 标题字体不是一套。

### 2.2 UserLayout.vue —— 死代码（含未编译缺陷）

- [高] 路由 `/user` 实际使用 UserBrandLayout（router/index.ts L230），**UserLayout.vue 没有任何引用**，是商家端侧栏布局的拷贝残留。且它内部 `<Heart class="logo-icon" />`（L107）的 `Heart` 未在 import 列表中（L4-22），若被启用会直接渲染失败——典型的改到一半被替换。
- [中] 路由文件残留两段注释掉的垃圾块（router/index.ts L250-259，`/* meta: ... }, meta: ... } */` 无对应的 route 对象），是脚手架编辑痕迹；`/user/dishes` 路由缺 `meta.title`（L240-243，其他页面都有）。

### 2.3 P0 全局主题与用户端的"对打"

- [高] P0 已把 `--el-color-primary` 收敛为 #1E9E62，但 editorial 各页用局部覆盖把按钮改回深绿：`.hero-actions :deep(.el-button){background:#1f4d3a}`（HomeEditorial L95）、`.browse-filters .el-button{background:#1f4d3a}`（DishBrowse L92）、`.detail-footer .el-button--primary{background:#1f4d3a}`（DishBrowse L91）、`.summary-heading .el-button{background:#1f4d3a}`（RecommendCenterV2 L107）、`.add-button{background:#1f4d3a}`（HealthCenterV2 L140）等。**全局主题在用户端事实上不生效**，每个页面各自为战。
- [中] 与此同时，后台风页面（Chat L79 等）用 `var(--bs-primary)` 引用的是**旧深绿 #1B3A2F**（tokens.scss L16 未随 P0 更新，`$primary` 与 `$accent` 并存）——同一个"主色"在用户端实际渲染出 #1E9E62（Element 组件）、#1f4d3a（editorial 覆盖）、#1B3A2F（--bs-primary 引用）三种绿。

---

## 三、逐页评审

### 3.1 HomeEditorial.vue —— 首页（6.0/10）

**现状**：hero（大衬线标题 + 图片色块拼贴）+ 墨绿箭头横幅 + 精选菜单（加载骨架屏/空态插画齐备）+ 套餐专区，视觉完成度是 C 端最高的，空态和加载态做得认真。

**主要问题**

- [高] **CSS 是覆盖堆叠的考古现场**：`.plans-section` 在 L103/104/106/107/108 被连续重定义 5 次，`.menu-layout.is-empty` 在 L96/99/100/101/102 重定义 4 次，`.daily-line` 在 L95/96 两个版本并存（白底版 + 墨绿 clip-path 箭头版，后者靠前序覆盖生效），最后还有一个独立的第二 `<style scoped>` 块（L111-121）。最终样式完全依赖源码顺序，改任何一处都可能连锁塌陷——这是全端最脆弱的文件。
- [高] **硬编码假数据**：`12+ 营养搭配方案`、`30 min 新鲜配送准备`（L47）是写死的营销数字，与平台真实数据无关，C 端门面上属于误导性内容。
- [中] 色板不在基准内：#1f4d3a（主绿）、#cf704f（陶土橙）、#e7a15e（hero 色块）、#f5c06d（暗底金）、#fbfcf9（米白），与基准 #1E9E62/#D97B2B 无映射关系。
- [中] 菜品图 fallback 是 3 张固定素材图轮换（L15），**无图菜品会展示与其完全无关的沙拉照片**，C 端"逛"场景下这是内容真实性硬伤（同样问题在 DishBrowse L22、ComboBrowseV2 L10）。
- [低] hero 主按钮"生成今日食谱"是页面上唯一行动点，但用 `:deep` 覆盖成 #1f4d3a 与全局 accent 对打（见 2.3）；"完善健康档案"入口指向 `/user/profile` 正确，值得保留。

### 3.2 dishes/DishBrowse.vue —— 菜品浏览（6.5/10）

**现状**：搜索 + 分类 + 排序筛选，3 列图文卡片（hover 放大、kcal 角标、加购按钮有"已加入"成功态 1.4s 回退），详情弹窗（大图 + 营养四格）。卡片节奏和加购微交互是 C 端应有的水准。

**主要问题**

- [高] **后端新字段零展示**：Dish 实体已有 `tags`（L50）、`aiComment`（L53）、`fitScenes`（L56）、`cautions`（L59），但详情弹窗只渲染名称/描述/价格/营养四格（L63-79）。"AI 点评、适宜场景、慎食提示"正是健康餐产品的差异化内容，界面完全没接。
- [中] 无图菜品 fallback 到无关素材图（L22-23），同 3.1；详情弹窗在无图时连 fallback 都没有（`v-if="detailData.dishImage"`，L65），信息区会孤单悬空。
- [中] 详情弹窗营养四格只到"热量/蛋白/脂肪/碳水"，没有重量基准（每份？每 100g？），`每份估算值`（L73）文案与商家端"按食材重量估算"的口径没有对客说明。
- [低] 排序选项 6 个（最新/价格×2/热量/蛋白/销量）但无"按我的健康目标推荐"排序——有健康档案却不用于逛菜排序，个性化断点。
- [低] 卡片价格 `¥{{ dish.price }}` 未格式化（可能出现 ¥15.5 / ¥15.90 混排）；路由缺 meta.title（见 2.2）。

### 3.3 combos/ComboBrowseV2.vue —— 套餐浏览（6.0/10）

**现状**：与 DishBrowse 同构的浏览页 + 详情弹窗内**套餐换菜**（固定/可换菜品下拉替换、防重复选择）+ 加入购物车后同步替换购物车快照，业务逻辑是全端最复杂的 C 链路之一，且雪花 ID 全程字符串透传（注释明确），工程质量不差。

**主要问题**

- [高] 整个文件压缩成 28 行（script 每条语句分号连写、template/style 单行），与 HomeEditorial 并列为最难维护文件；`V2` 命名暗示存在过 V1，重构后未清理格式。
- [中] 换菜下拉把所有菜品（`getDishList({page:1,size:100})` 全量）都列为候选，未按"可替换菜品池"过滤——商家端 ComboList 有 replaceableDishPool 概念，C 端换菜候选应与之一致，否则用户能换成商家未授权的菜品（当前靠后端报错"该菜品暂不可替换"兜底，体验是试错式的）。
- [中] 套餐卡片描述 `min-height:38px` + fallback 文案"为日常目标准备的均衡搭配"，无 tags/适用人群展示（suitableFor 在商家端有录入）。
- [低] `imageFor` fallback 同样是 3 张素材图轮换；价格 `¥{{ combo.price }}` 未格式化；类型标签"减脂/增肌/控糖/会员专属"无色彩区分。

### 3.4 cart/CartCheckout.vue —— 购物车与结算（6.5/10）

**现状**：跨商家分组结算（merchantGroups）、每商家独立留言、地址选择弹窗、sessionStorage 保存结算现场（去地址页后返回可恢复，L71-78/L91-94），购物车链路完整度是全端最高的页面之一。

**主要问题**

- [高] **结算弹窗把雪花 ID 直接展示给 C 端用户**：`商家 #{{ group.merchantId }}`（L164）——用户看到的是"商家 #1834567890123456789"。同问题在 RecommendCenterV2 菜品卡（`商家 #{{ item.merchantId }}`）。购物车 api 已关联了菜品/套餐详情，应一并带出 shopName。
- [中] 地址单选项把 省/市/区/详细地址 无分隔直接拼接（L157），长地址换行后难以阅读；`省市区` 在 AddressList 里本来就是三个手填输入框（见 3.13），数据质量无保障。
- [中] 结算汇总卡 `.cart-summary` 是整面 #1f4d3a 深绿 + #f5c06d 金按钮（L177-178），视觉很重但不在基准色板内；且 L177 与 L178 两个 `.checkout-button` 版本并存（又一个"后者覆盖前者"的补丁痕迹）。
- [低] 数量修改/勾选每个操作都单独发请求且无防抖，快速连点会产生并发写；失败仅 toast 不回滚本地态。
- [低] 合计金额 `toFixed(2)` 无千分位、无 tabular-nums。

### 3.5 orders/OrderHistory.vue —— 我的订单（6.5/10）

**现状**：状态点 + 订单卡片列表（摘要/收货人/金额/操作），详情弹窗含商品明细、配送信息、状态时间线，支付走支付宝沙箱 form 提交（新窗口 document.write，L62-83，弹窗拦截有提示），链路完整。

**主要问题**

- [中] 状态色彩体系自造：`.status-dot.pending #cf704f / .processing #568b70 / .shipping #d49b4e / .completed #1f4d3a`（L107）——第四套绿/橙变体（#568b70、#d49b4e 在其他任何页面都不存在），且"备餐中"与"待接单"同色无法区分。
- [中] 订单卡片操作区按钮全为 `text` 类型（去支付/查看详情/配送跟踪/取消订单 4 个 text 按钮并排，L94），主行动（待支付时的"去支付"）没有视觉权重；取消订单用 text danger 反而最显眼。
- [低] 详情弹窗金额 `¥{{ order.payAmount || 0 }}` 未格式化；时间线复用 el-timeline 默认样式，与卡片风混搭。
- [低] 两个 `<style scoped>` 块（L106/L109），第二块只有两条 svg 尺寸规则，是补丁残留。

### 3.6 delivery/DeliveryCenter.vue —— 配送列表（5.0/10）

**现状**：一页压缩文件，`h3 卡片 + el-table(border)` 的后台风列表，筛选出配送中/已送达订单，跳 TrackingView。

**主要问题**

- [中] 视觉上是商家端后台页（h3 卡片、带竖边框表格），嵌在 editorial C 端外壳里风格断裂；`border` 表格是 C 端独一份。
- [中] 只展示"配送中/已送达"两态，无"备餐中"订单（用户最关心的"还要多久"阶段看不到）；`getOrderList()` 不传分页参数全量拉取后前端过滤。
- [低] 无空态之外的引导（"暂无配送中的订单"后没有去逛逛的链接，OrderHistory 的空态反而有）。

### 3.7 delivery/TrackingView.vue —— 配送跟踪（4.0/10）

**现状**：el-steps 四步条 + 状态 alert + descriptions（取餐码/预计送达/各时间点）+ 骑手卡片（姓名/车型/电话），10s 轮询自动刷新，雪花 ID 字符串透传（注释到位），上轮修的车辆枚举（10电动车/20自行车/30汽车）在。

**主要问题**

- [高] **没有地图**：`当前位置` 直接把经纬度原文展示（`12.34, 56.78`，L14 descriptions 最后一行）。package.json 无任何地图依赖（leaflet/amap/mapbox 均无），后端也只回 currentLat/currentLng 单点（无轨迹数组）——"配送跟踪"页的核心心智（骑手在哪、离我多远）完全没有兑现，本质是个"配送状态详情页"。
- [中] el-steps 把 4 个状态硬映射为 3 段进度（`>=50 ? 3 : >=40 ? 2 : >=30 ? 1 : 0`），"待分配(10)/待取餐(20)"都显示第 0 步，异常(60)/取消(70) 状态在 steps 上无任何表达，只靠 alert 变色。
- [低] 骑手卡片电话 `tel:` 链接在 PC 端点击无意义；无"联系商家/客服"备选；轮询期间手动刷新按钮与自动刷新并存且各自 loading 表现不一致。
- [低] `.driver` 卡片背景 #f7f8fa 是 Element 默认灰，不在 editorial 也不在 token 色板。


### 3.8 ai/Chat.vue —— AI 对话（4.0/10）

**现状**：最简聊天窗（消息气泡 + 输入框 + Enter 发送），会话 sessionId 用 crypto.randomUUID 本地生成。

**主要问题**

- [高] **违反"无 emoji"基准**：空态放了一个 48px 的 🤖（L69）；且整个聊天区布局/气泡样式全部是 template 内联 style（L62-95 十余处），scoped style 里只有卡片壳——"先跑起来再说"的半成品。
- [中] 用户气泡背景 `var(--bs-primary)`（L79）渲染为旧深绿 #1B3A2F（见 2.3），发送按钮却是 P0 后的牛油果绿 el-button——同一屏两种品牌绿。
- [中] `getChatHistory` 被 import 但从未调用（L3）——历史会话功能接了一半；"新对话"按钮清空消息无确认。
- [低] 气泡 `whiteSpace: pre-wrap` 但没有 markdown/结构化渲染，AI 长回答是一堵文字墙；"正在输入..."是纯文本无动画；文件尾部混入 `\r` 孤立回车。

### 3.9 ai/RecommendCenterV2.vue —— AI 食谱（6.0/10）

**现状**：餐次选择 + 忌口输入 → 生成三餐计划（真实在售菜品 + 营养汇总 + 换菜候选 + 全部加入购物车），"从平台真实菜品搭配"的产品逻辑清晰，跨商家加购有明确提示（L60）。

**主要问题**

- [高] **4 个 `<style scoped>` 块**（L78/81/87/90）层层覆盖：`.recipe-page` 在 L79 和 L91 两个版本（max-width 1180→1280）、`.summary-total strong` 被第三块用 `!important` 单独改色（L88）——与 HomeEditorial 同款补丁堆叠。
- [中] 菜品卡展示 `商家 #{{ item.merchantId }}`（raw 雪花 ID，同 3.4）；`库存 {{ item.stock || 0 }}` 把内部库存数字透给 C 端用户。
- [中] 换菜是**纯前端替换**：`replaceDish`（L36-46）直接在前端重算 summary，不调后端校验（新组合是否满足目标/忌口无人把关）；候选池只排除当前菜品本身，同一餐内可换出重复菜品。
- [低] 空态图标用 `✦` 字符（L74）与"无 emoji"擦边；eyebrow 橙 #c66e4e 与全站 #cf704f 不统一（第三种橙）；档案缺失引导 alert 的按钮直接涂成 #cf704f 橙底（L83-84），与 alert 自身 warning 配色叠穿。

### 3.10 health/HealthCenterV2.vue —— 健康记录（6.0/10）

**现状**：三类记录（饮食/运动/体重）tabs + 统计条 + 按 tab 切换的添加弹窗，来源标记（平台订单/手动记录）有业务意识，删除有确认。

**主要问题**

- [高] **与数据库新字段脱节**：`UserProfile.targetWeight`（后端 entity L51、前端 api 类型 profile.ts L34 都有），但本页体重 tab 只是裸记录列表——没有体重趋势图、没有目标体重线、没有"距目标还差 x kg"。echarts 已在依赖且商家端 Dashboard 在用，体重趋势是本页最该有的一张图。
- [中] 饮食记录无一日合计视图（当天已摄入 vs 目标热量），记了数据却没有"今天还能吃多少"的闭环——而这恰是 AI 食谱页的输入。
- [中] 平台订单来源（sourceType=10）的饮食记录也允许删除（L113 操作列无区分），会把订单同步来的营养数据删掉，口径破坏。
- [低] 统计卡图标底色 `#f9e9df/#e3efe5/#e7edf2`（L140）散装色；数字用 Georgia 衬线，与基准 tabular-nums 规范不符。

### 3.11 profile/ProfileCenterV2.vue —— 个人中心（6.5/10）

**现状**：头像上传（类型/大小校验 + 即时生效）、账户信息、健康档案（查看卡片/编辑表单双态）、档案完成度进度条、侧栏"目标聚焦 + 下一步引导"，信息架构是用户端最清晰的页面。

**主要问题**

- [高] **健康档案缺 `targetWeight` 字段**：form 只含 age/gender/height/weight/activityLevel/dietPreference/allergyInfo/diseaseHistory/healthGoal（L17），后端和 api 类型都已支持目标体重——减脂产品的核心指标无法录入。
- [中] 档案完成度只统计 5 个字段（age/gender/height/weight/healthGoal，L102-103），过敏/疾病等对 AI 推荐安全性更关键的字段不计入，100% 完成度会给用户"档案已足够"的错误暗示。
- [中] 进度条颜色硬编码 `color="#1f4d3a"`（L157），未走 token；`profileFields` 在模板里直接当数字做 `5 - profileFields` 运算（L163），可读性差。
- [低] 头像 `.hero-avatar` 用 Georgia 衬线首字母、上传 overlay 文字 10px，两处风格细节与基准的字母徽章规范不一致但可接受；账户状态"正常"tag 是写死的假状态（L160）。

### 3.12 membership/MembershipCenterV2.vue —— 会员中心（6.0/10）

**现状**：当前会员状态条 + 三档方案卡（权益解析 discount→"xx 折"有细节）+ 购买记录表，结构完整。

**主要问题**

- [高] **购买无支付链路**：`buy()` 是 confirm 弹窗后直接调 `buyMembership`（L53-60）即"购买成功"——订单页有完整支付宝沙箱支付，会员购买却绕过了支付，要么接支付要么明确标注"后端代扣/测试态"。
- [中] "更受欢迎"角标写死在 `index === 1`（L79-80）——方案顺序变就更受欢迎的就变，应以后端字段为准；已有有效会员时三个方案的按钮全部置灰但文案相同，未提供"续费/升级"路径。
- [低] 方案价格 Georgia 衬线 38px（`.plan-price`），与基准 tabular-nums 不符；`featured` 边框 2px + padding 补偿（L105）造成对齐抖动；权益 icon 缺失（纯文字列表）。

### 3.13 addresses/AddressList.vue —— 地址管理（5.5/10）

**现状**：地址卡片列表 + 增删改弹窗（有表单 rules 校验），从购物车进入时有"返回购物车"衔接，功能闭环完整。

**主要问题**

- [中] 视觉是后台卡片风（`h3 card-header` + `var(--bs-*)`），与 editorial 主链路断裂（见共性问题 1）；`--bs-border-color` 等别名变量经 P0 兼容映射后已不失效，但新代码应直接用真实变量。
- [中] 省/市/区是三个**手填文本框**（L159-169），无级联选择也无格式校验，直接决定配送地址质量；电话只校验必填不校验格式。
- [低] `setDefault` 复用编辑表单整体提交（L90-93），会把陈旧字段一并写回；删除用 solid danger 按钮（L126-130），在 C 端列表里偏重。

### 3.14 reviews/ReviewCenter.vue —— 我的评价（5.5/10）

**现状**：可评价订单下拉（已完成且未评价的 computed 过滤正确）+ 三项评分 + 匿名开关 + 评价列表，"选订单→评分→提交"链路可用，是后台风页面里交互最完整的。

**主要问题**

- [中] 评价列表展示 `订单 #{{ review.orderId }}`（raw 雪花 ID，同 3.4）；商家回复字段取 `review.merchantReply`，而商家端回复接口写的是 `replyContent`——字段名不一致可能导致商家回复永不显示（需核对接口实际返回）。
- [低] 后台卡片风（同 3.13）；`--bs-border-color`/`--bs-text-secondary` 别名引用（L15 style）；评分用 el-rate 默认橙黄星。
- [低] 整文件压缩单行，与 ComboBrowseV2 同款维护性问题。

### 3.15 notices/NoticeList.vue —— 系统公告（5.0/10）

**现状**：类型 tag + 标题列表 + 详情弹窗（pre-wrap 保留换行），公告类型四分（系统/健康知识/活动/升级）有设计。

**主要问题**

- [高] **分页完全失效**：`fetchNotices` 不传分页参数拿全量（L20），`total = notices.length`，翻页时 `handlePageChange` 重新拉取同一全量列表但**从不切片**——用户看到的永远是全部公告，翻页控件是个摆设。
- [低] 后台卡片风（同 3.13）；类型 tag 的 `primary` 类型（L53）在 P0 后渲染为牛油果绿，与"升级通知"语义尚可，但 info/success/warning/primary 四色 tag 混排较花。

### 3.16 Complaint.vue —— 提交投诉（3.0/10）

**现状**：9 行压缩文件，一个表单：手填订单 ID + 投诉对象类型 + **手填对象 ID** + 原因 + 说明。

**主要问题**

- [高] **功能上不可用**：要求 C 端用户手动输入订单 ID 和投诉对象（商家/骑手）的**雪花 ID**——用户没有任何途径知道这些数字。应像 ReviewCenter 一样提供订单下拉选择，targetId 从订单带出。
- [高] **页面是孤儿**：线上布局 UserBrandLayout 的导航和下拉均无投诉入口（见 2.1），用户即使想用也到不了这个页面。
- [低] 无投诉记录列表（提交后石沉大海，无"我的投诉"状态跟踪）；整文件单行压缩。

---

## 四、共性问题汇总（全端性）

1. **两套视觉体系并存**：editorial 杂志风（Home/DishBrowse/Combos/Cart/Orders/AI食谱/健康/会员/个人中心，衬线标题 + #1f4d3a + #cf704f）vs 后台卡片风（地址/公告/评价/投诉/AI对话/配送×2，`h3 card-header` + bs token）。用户在一次会话里穿越两个产品。
2. **editorial 色板未入库且与基准冲突**：主绿 #1f4d3a（及其 hover #2c654e）vs 基准 #1E9E62/#123326；橙红出现 8 种变体（#cf704f/#c66e4e/#e87955/#e7a15e/#f5c06d/#d49b4e/#e6b08e/#b95e40）。同时各页用局部覆盖把 Element 主色改回 #1f4d3a，与 P0 全局主题对打；`$primary: #1B3A2F` 旧值仍在 tokens.scss L16 被后台风页面引用。需要先做一次"C 端色板定调"决策。
3. **补丁堆叠式 CSS**：HomeEditorial 同一选择器重定义最多 5 次 + 2 个 style 块；RecommendCenterV2 4 个 style 块；OrderHistory/ProfileCenterV2/CartCheckout 各 2 个。样式正确性依赖源码顺序，是全端最大的维护地雷。
4. **整页压缩单行文件**：ComboBrowseV2（28 行）、TrackingView（15 行）、DeliveryCenter（12 行）、ReviewCenter（15 行）、Complaint（9 行）——功能再对也无法安全修改。
5. **raw 雪花 ID 暴露给 C 端**：结算弹窗"商家 #id"、AI 食谱"商家 #id"、评价列表"订单 #id"、投诉页要求手填 ID。API 已有关联数据，应统一带出 shopName/orderNo。
6. **后端新字段零落地**：Dish.tags/aiComment/fitScenes/cautions（entity L50-59）全端无引用；UserProfile.targetWeight（entity L51 + api 类型）无录入入口也无展示。
7. **死代码与半成品**：UserLayout.vue 整文件未引用（内含未 import 的 `<Heart>`）；router/index.ts L250-259 注释垃圾 + `/user/dishes` 缺 meta.title；Chat.vue 的 getChatHistory 未使用；`$primary`/`$accent` 双主色并存。
8. **emoji/符号残留**：Chat.vue 🤖（48px）、RecommendCenterV2 ✦。
9. **功能半残**：NoticeList 分页失效；TrackingView 无地图（无依赖、后端无轨迹数据）只显示裸经纬度；Membership 购买无支付；AI 食谱换菜纯前端无后端校验。
10. **图片真实性**：无图菜品/套餐统一 fallback 到 3 张无关素材沙拉图轮换（Home/DishBrowse/Combos/Cart/AI食谱），C 端"逛"场景下属于误导性内容。
11. **导航入口残缺**：地址/评价/投诉/商家入驻无入口；公告只有铃铛图标。

---

## 五、改造优先级排序

### P0 — 定调 + 修硬伤（决策先行，修复随后）
| 项 | 说明 | 工作量 |
|---|---|---|
| C 端色板定调 | 决策：editorial（#1f4d3a + 陶土橙）作为 C 端正典沉淀为 token，还是全面向基准（#1E9E62/#123326）靠拢。建议：C 端保留 editorial 但**入库**（新增 `$brand-green/$clay` token 并统一 8 种橙为 1 种），同时停止各页对 el-button 的局部覆盖，Element 主题映射到定调色 | 0.5 天（决策）+ 1 天（收敛） |
| Complaint 重构 | 改订单下拉 + 自动带出投诉对象；加"我的投诉"列表；接入导航 | 1 天 |
| NoticeList 分页修复 | 前端切片或改走后端分页 | 0.5 天 |
| 清死代码 | 删 UserLayout.vue、router 注释块、Chat 未用 import、补 `/user/dishes` meta.title | 0.5 天 |
| 去 emoji/符号 | Chat 🤖 改 SVG 插画、RecommendCenterV2 ✦ 改文字标 | 0.5 天 |
| 商家名/订单号替代 raw ID | api 层带出 shopName，替换 Cart/Recommend/ReviewCenter 三处 `#雪花ID` | 1 天（含后端确认） |

### P1 — 小改即可见效（结构已正确，补内容/补入口）
| 页面 | 关键动作 | 工作量 |
|---|---|---|
| 导航（UserBrandLayout） | 补地址/评价/投诉入口（下拉或"我的"聚合页）；解压单行样式 | 1 天 |
| DishBrowse | 详情弹窗接入 tags/aiComment/fitScenes/cautions（这是健康餐的核心卖点，优先级最高的内容改造）；价格格式化 | 1 天 |
| ProfileCenterV2 | 表单加 targetWeight；完成度口径纳入过敏/疾病字段 | 0.5 天 |
| HealthCenterV2 | 体重 tab 加 ECharts 趋势图 + 目标体重线（echarts 已在依赖）；平台订单来源记录禁删 | 1.5 天 |
| MembershipCenterV2 | 接支付或标注代扣；"更受欢迎"改后端字段；加续费路径 | 1 天 |
| DeliveryCenter | 去 border 表格改订单卡片（复用 OrderHistory 卡片）；补"备餐中"状态 | 1 天 |

### P2 — 中度重构（结构或工程债需拆解）
| 页面 | 关键动作 | 工作量 |
|---|---|---|
| TrackingView | 接地图（评估引入 leaflet/高德 lite）或至少改为时间线 + 距离进度条；后端补轨迹点数组；异常/取消状态可视化 | 2 天（含后端） |
| Chat.vue | 内联样式全部落 class；气泡色走定调 token；接 getChatHistory 或删入口；AI 回复 markdown 渲染 | 1 天 |
| RecommendCenterV2 | 合并 4 个 style 块；换菜走后端校验接口；库存数字改"有货/紧张"语义化 | 1.5 天 |
| CartCheckout | 合并重复 .checkout-button；地址选项加分隔与默认标识；数量修改防抖 | 1 天 |
| 后台风四页（Address/Notice/Review/Delivery） | 统一改 editorial 页头（eyebrow + 衬线 h1）+ hairline 卡片，消灭双体系 | 2 天 |

### P3 — 建议按定调语言重写
| 页面 | 理由 | 工作量 |
|---|---|---|
| HomeEditorial.vue | CSS 考古现场（同选择器 5 次重定义）+ 硬编码假数据，修补风险高于重写；按定调 token 重做并接真实统计数字 | 2 天 |
| ComboBrowseV2.vue | 28 行压缩文件 + 换菜候选逻辑要与商家端 replaceableDishPool 对齐，一并重写并格式化 | 1.5 天 |
| 小程序功能补齐（计划/问卷/运动库） | 后端已有 UserPlan 实体（含 targetWeight），PC 端按功能清单主打"AI 智能食谱生成"，差距可接受；若要对齐，属新产品需求而非 UI 改造，建议单独立项评估 | 另议 |

**建议路线**：先做 P0 的"色板定调"决策（它决定所有后续页面的改法），P0 修复约 4.5 天；P1 约 6 天；P2 约 7.5 天；P3 约 3.5 天。**P0 + P1（约 10 个工作日）完成后，C 端主链路的可用性和一致性即达标**；P2/P3 解决的是维护性与精致度。

---

*本报告为只读评审，未修改任何源码。行号引用基于评审时工作区版本（master，最近提交 c852544，P0 令牌已入库）。*
