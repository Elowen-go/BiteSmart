# BiteSmart PC 商家端 UI/UX 评审报告

- 评审范围：`BiteSmart_front/src/views/merchant/` 全部 13 个页面 + `layouts/MerchantLayout.vue` + `src/styles/`（tokens.scss / global.scss / element-plus-theme.scss）
- 设计基准：新版 `Dashboard.vue`（牛油果绿 #1E9E62 点缀 + 墨绿 #123326、英文小号大写区块标、1px hairline 卡片、8-10px 微角、tabular-nums、米白底、高信息密度）
- 评审方式：只读源码评审（未运行页面）
- 日期：2026-07

---

## 一、总体评分与印象

**总体评分：6.0 / 10**

| 维度 | 得分 | 说明 |
|---|---|---|
| 视觉一致性 | 4.5 | 全端存在**三套并行色板**（tokens 深绿 #1B3A2F / Dashboard 牛油果绿 / Finance·Apply 散装绿色），新基准只活在 Dashboard 局部作用域里 |
| 信息架构 | 6.5 | 列表页"页头 + 指标卡 + 工具栏 + 表格"的骨架已趋同，但页头有 h2/h3 两种规格，部分页统计卡是装饰大于功能 |
| 数据展示 | 6.0 | 表格用法总体规范、空态基本覆盖；但统计口径混乱（分页页数据冒充全量统计）、Statistics 手搓 CSS 图表与 Dashboard 的 ECharts 并存 |
| 交互质量 | 6.5 | 危险操作基本有确认；按钮层级在订单/菜品页过平（一行 4 个同级按钮），完成按钮用了未收敛的 Element 默认绿 |
| 工程整洁度 | 4.5 | 存在失效 CSS 变量（约 35 处）、压缩成一行的脚手架残留（Finance 全文、OrderList 第二段 style）、重复粘贴的样式块（fadeIn / summary-grid 在 9 个页面各写一份） |

**总体印象**：这是一套"功能先行、样式后补"的后台。列表类页面（订单/配送/库存/评价）已经在同一个模板上收敛，底子不差；但新版 Dashboard 定稿的设计语言**没有沉淀到 tokens 层**，导致它像一块风格孤岛的飞地——其余 12 个页面还停留在旧深绿主题，且各自残留脚手架痕迹（内联色值、失效变量、压缩样式）。当前最划算的投入不是逐页重画，而是先把基准语言下沉到 `tokens.scss` / `element-plus-theme.scss`，再做一轮"对齐式"改造。

---

## 二、共享层评审（tokens / global / element-plus-theme / MerchantLayout）

### 2.1 tokens.scss —— 基准语言未入库（全端性根因）

**现状**：token 体系完整（色板/圆角/阴影/间距/字号/状态色），但内容仍是旧主题：`$primary: #1B3A2F`（深墨绿作主按钮色）、`$bg-page: #F5F7FA`（冷灰而非米白）、卡片用 `$card-shadow` 投影而非 hairline 描边。

**主要问题**

- [高] Dashboard 的整套基准色（#1E9E62 / #123326 / #17251F / #6B7A72 / #E4E8E3 / #E4F3EA / #D97B2B）只定义在 `.db` 局部作用域（Dashboard.vue L574-584），**其他页面无法复用**，新语言天然无法扩散。
- [高] `$primary` 直接沿用侧栏深绿 #1B3A2F，导致全端主按钮是"一大片墨绿块"，与基准"牛油果绿点缀、大面积留白"的克制感相反。
- [中] `$bg-page: #F5F7FA` 是冷灰蓝底，与基准米白不符；`--el-input-bg-color: #{$bg-page}` 还让所有输入框带上灰底，视觉上偏"填表系统"。
- [中] 状态色仅定义 success/warning/secondary/danger 四色且 `$status-danger` 无对应 bg；页面里实际用到的小写硬编码 `#b76e2a`、`#1b6b4a`、`#8a9299`（见 Delivery/Inventory/Orders/Reviews/DishList/ComboList 的 `.summary-item`）与 token 值相同却各写一遍。

**修改建议**

1. 把 Dashboard 的 `.db` 变量组提升为 token：新增 `$accent: #1E9E62`、`$accent-deep: #14704A`、`$ink-green: #123326`、`$hairline: #E4E8E3`、`$bg-warm`（米白）、`$status-orange: #D97B2B`。
2. `$primary` 改为 #1E9E62（按钮/链接），#1B3A2F 或 #123326 仅保留给侧栏；卡片规范改为 `border: 1px solid $hairline; box-shadow: none`。
3. 同步更新 `element-plus-theme.scss`：`--el-color-primary` 指向新 accent，并**补齐未覆盖的 `--el-color-success` / `--el-color-danger` / `--el-color-warning`**（见 2.2）。

### 2.2 element-plus-theme.scss —— Element 主题只收敛了一半

- [高] 只覆盖了 primary 系，**success/danger/warning 全部未覆盖**。实测影响：`OrderList.vue:256` 的"完成"按钮 `type="success"` 渲染为 Element 默认亮绿 #67C23A；全端 `el-tag type="success/danger"`（Delivery.vue:132、OrderList.vue:242、ReviewList.vue:153-155、DishList.vue:506 等十余处）都是默认绿 #67C23A / 红 #F56C6C，与品牌绿和 `$status-danger: #D9534F` 两个体系都不一致——这是全端最显眼的"各写各的"。
- [中] `--el-bg-color-overlay: rgba(0,0,0,0.5)` 用法错误：该变量是**弹层面板背景色**（dropdown/dialog 面板底），被设成了半透明黑；遮罩变量实际是 `--el-overlay-color-lighter`。当前弹窗/下拉面板的底色存在渲染异常风险。
- [低] `--el-radius-lg: #{$radius-md}`（12px 被压回 8px）是有意收敛可以保留，但建议注释说明。

### 2.3 global.scss —— 半成品公共类

- [中] 定义了 `.btn-primary` / `.card-panel` / `.status-badge` 公共类，但只有 CategoryList / Profile / ShopInfo 三个页面在用，其余页面用 el-button + 私有 `.card-panel` 重新定义（同名不同源，Delivery/Inventory/Orders/Reviews/DishList/ComboList 各写一份完全相同的 `.card-panel`）。`.status-badge` **没有任何页面使用**，是死代码。
- [低] `.content-body` 在 global 和 MerchantLayout 各定义一次。

### 2.4 MerchantLayout.vue —— 侧栏/顶栏

**现状**：240px 深绿侧栏 + 64px 白顶栏，分组菜单、店铺切换器、通知角标、折叠 tooltip 齐备，信息架构清晰。

**主要问题**

- [高] **店铺选择器的深色主题写了两遍**：一遍是 29-99 行的 `shopSelectorStyles` 字符串在 `onMounted` 里 `document.head.appendChild` 动态注入全局 `<style>`，另一遍是 403-492 行 scoped `:deep()`，两遍内容大面积重复（.el-select-dropdown 背景、item 颜色等），且注入的全局样式在组件卸载后**不清理**，会泄漏污染其他布局（AdminLayout/UserLayout 若有用到同名 class 即被污染）。这是脚手架期"调不通就加 !important"的典型残留，全组件 `!important` 超过 40 处。
- [中] 侧栏选中态是纯白色 rgba 叠加（`rgba(255,255,255,0.15)`），无品牌绿点缀；按基准应给 active 项左侧 3px #1E9E62 指示条或文字着色。
- [中] `fetchLogo()` 拿到的 `logoUrl` **从未在模板中使用**（模板固定渲染 `<Shop class="logo-icon" />`，L255），属死代码——要么侧栏展示店铺 Logo，要么删掉请求。
- [中] 顶栏 `icon-btn:hover { color: #A8D5BA }`（L723-726）在白底上 hover 变浅绿，对比度不足且 A8D5BA 是侧栏深底色上的点缀色，用在白色顶栏语境错误；通知角标 `#E57373`（L741）是 Material 红，与 $status-danger #D9534F 不一致。
- [低] `menuItems.push({ label: '资金' ... })`（L168-171）在数组定义外追加菜单项，脚手架痕迹；且工作台/销售统计/资金中心三个菜单项都复用 `PieChart` 图标。
- [低] `.collapse-btn:hover` 重复定义两次（L609-617）。
- [低] 侧栏底部"商家"文案 + Shop 图标语义空泛，可放店铺名/版本号。

**修改建议**：删除 JS 注入的 `shopSelectorStyles`，保留 `:deep()` 单源并收敛 `!important`（用 popper-class 挂载点提高优先级即可）；active 菜单加 #1E9E62 左指示条；`fetchLogo` 结果接入侧栏 logo 位或删除；顶栏 hover 色改 `--bs-text-title`；角标统一 `$status-danger`。

---

## 三、逐页评审

### 3.1 Dashboard.vue —— 基准页（9.0/10）

**现状**：新版设计完成度高：hero 营收卡 + 指标带 + 待办/预警/评价三栏 + ECharts 双图，hairline 卡片、英文区块标、tabular-nums 齐备，接单/拒单操作链路完整（带 opLoading 防重复点击、拒单 prompt 给示例占位）。

**主要问题（基准自身的小瑕疵，推广前应先修）**

- [中] 设计变量定义在 `.db` 局部作用域，未沉淀 token（见 2.1），基准无法被复用。
- [低] 环比徽章用 `↑` `↓` 字符（L457）、评价星级用 `★☆` 字符（L245），属"准 emoji"符号，与"无 emoji"规范擦边；建议换 SVG 或纯文字 +/-%。
- [低] `.btn-green` 是 11px 的 span 按钮（L612-623），无键盘可达性（非 `<button>`、无 focus 态），拒单 `.todo-rej` 同理。
- [低] `max-width: 1280px` 但未 `margin: 0 auto`，超宽屏下整页偏左。

### 3.2 orders/OrderList.vue —— 订单处理（6.5/10）

**现状**：页头 + 四张状态统计卡（可点击筛选）+ segmented + 表格 + 分页 + 详情弹窗（descriptions + 商品明细 + 状态时间线），结构是全端列表页里最完整的。

**主要问题**

- [高] **统计口径误导**：`orderStats`（L41-53）基于 `orderList`（当前页，size=10）统计"待接单/备餐中"数量，翻页后数字会变化，商家会误以为只有这几单待处理。金额 hint 写了"本页完成 ¥xx"但数量卡没有任何"本页"限定。应改为后端聚合接口，或至少在卡上标注口径。
- [中] "完成"按钮 `type="success"`（L256）渲染 Element 默认绿 #67C23A，与主题脱节（根因在 2.2，本页最显眼）；"备餐"按钮是无 type 的默认样式，与"接单"(primary solid)、"拒绝"(danger plain) 并排时层级混乱——一行最多 3-4 个按钮（详情/接单/拒绝同时出现），操作列 250px 容易挤压。
- [中] 状态统计卡与 `el-segmented` 是**两套重复的筛选控件**（卡片点击和 segmented 都能切状态），同一功能两个入口。
- [低] 订单详情弹窗底部追加了一段**压缩成一行的第二个 `<style scoped>` 块**（L494-496），内联色值 #f5f8f5/#87928a/#1f2a24/#eef4ef/#5e8069 全是 token 外散装色，明显是后期补丁式脚手架残留。
- [低] `party-strip` 里买家/商家信息用 12px 灰字横排，与 descriptions 的"收货人/联系电话"信息重复。
- [低] 商品图片占位符是一个"餐"字纯色块（`.order-item-placeholder`），与 Dashboard 的字母头像风格不统一。

**修改建议**：操作列收敛为"主操作按钮 + 详情 text 按钮"两个；统计卡改全量口径或移除（待办已由 Dashboard 承担）；segmented 与卡片点击筛选二选一；合并两段 style 并替换散装色。

### 3.3 dishes/DishList.vue —— 菜品管理（5.5/10）

**现状**：搜索 + 状态筛选 + 图文表格 + 分页，编辑弹窗内含食材关联与营养估算，功能是全端最重的页面之一。

**主要问题**

- [高] **约 20 处引用不存在的 CSS 变量**：`--bs-border-color`、`--bs-text-primary`、`--bs-text-secondary`、`--bs-bg-secondary`（L957/971/1122/1128/1131/1147/1153/1157/1162/1171/1186/1214/1225/1239/1245/1265/1267/1272/1276/1294 等），tokens.scss 里根本没有这些定义。后果：`border: 2px solid var(--bs-border-color)` 整条声明失效（图片边框不显示）、`color: var(--bs-text-primary)` 失效后静默继承——页面"看起来没崩"纯属巧合，是典型的从其他项目拷贝样式后的残留。
- [中] 操作列 4 个同级小按钮（查看/编辑/下架/删除，L516-522）+ 260px 列宽，删除用 solid danger 过重；"查看"与"编辑"弹窗信息大量重复，可合并。
- [中] 统计卡同样只统计当前页；且"低库存"阈值本页写死 5（L70），与 Dashboard 的 `LOW_STOCK_THRESHOLD = 10` 不一致——两个页面对同一菜品会给出不同的预警结论。
- [中] 编辑弹窗 label-width 100px + 大量 `style="width: 100%;"` 内联（L553/563/566 等），营养区"热量: xx 大卡"半角冒号与全角混排（L632-635）。
- [低] 自绘 `.image-preview-modal` 全屏预览（L1057-1085）与 el-image-viewer 功能重复，且 z-index 9999 硬编码。
- [低] 占位图是 #1B3A2F 深绿大色块 + 48px 首字符（`.image-placeholder`），表格里视觉过重，与基准的轻量字母徽章不符。

**修改建议**：全局替换失效变量为 `--bs-border-light` / `--bs-text-title` / `--bs-text-body` / `--bs-bg-hover`；统一低库存阈值常量；操作列改"编辑 primary + 更多 dropdown"；预览换 `el-image` 的 preview-src-list。

### 3.4 combos/ComboList.vue —— 套餐管理（5.5/10）

**现状**：与 DishList 同构（统计卡 + 搜索 + 表格 + 分页），编辑弹窗 880px 内含套餐构建器（已选菜品表、可替换池、营养自动同步），交互深度足够。

**主要问题**

- [高] 编辑弹窗信息过载：单弹窗内塞了 12 个表单项 + 菜品构建器 + 可替换池 + 营养同步提示，纵向滚动极长；`label-width: 120px` 配 `el-row/el-col` 两列布局，每个 input-number 下的"已同步: xx kcal"提示（`.nutrition-hint`）造成视觉噪音。建议改分步（基础信息 → 菜品与替换 → 营养确认）或 drawer + 分区锚点。
- [中] 样式区散装色值密集：`.dish-table-header` 背景 #f5f7fa / 文字 #909399、`.view-dish-table` 边框 #e4e7ed、`.combo-image-placeholder` #f0f0f0/#999、`.replaceable-preview-placeholder` #fff4e5/#b76e2a 等 10+ 处（L1323-1498 区间），全是 Element 旧默认灰阶，与 token 体系脱节。
- [中] 表格"换菜"列用 warning/info tag（L580-584）；操作列查看/编辑/删除三按钮中"编辑"用了 primary solid（L598），而 DishList 里编辑却是 default——同角色按钮两个页面样式不一致。
- [低] `comboStats` 与 DishList 的 `dishStats` 同样只统计当前页；"无图"占位（灰底黑字）与 DishList 的深绿首字占位两种风格并存。
- [低] 详情弹窗里 `x{{ item.quantity }}`（L922）半角 x 作乘号，建议 ×。

### 3.5 categories/CategoryList.vue —— 分类管理（5.0/10）

**现状**：单卡片 + 表格 + 增删改弹窗，118 行，功能简单直接。

**主要问题**

- [中] 仍是**第一代页面范式**：`card-header h3`（16px）页头 vs 其他页的 `page-head h2`（20px + 副标题 + 刷新按钮）；按钮用自绘 `.btn.btn-primary` 而非 el-button——两种按钮体系在同一后台并存。
- [中] 表格是**全端唯一**使用 `border stripe`（L76）的：竖向边框 + 斑马纹与其他页面无边框表格风格直接冲突。
- [中] 删除按钮 `type="danger"` solid（L85）视觉过重（确认弹窗有 ElMessageBox.confirm，这点合规），其他页面危险操作多为 plain。
- [低] 排序只能用弹窗里的 input-number 改数字，无拖拽/上下移快捷操作；分类无"菜品数"列，删除前不提示关联菜品数量，有误删风险。
- [低] `style="padding-top: 20px;"` 内联样式（L75）；`ElMessage`/`ElMessageBox` 依赖 auto-import 未显式 import，与其他页面显式引入风格不统一。

### 3.6 delivery/Delivery.vue —— 配送管理（7.0/10）

**现状**：页头 + 四张可点击状态卡 + segmented + 表格，结构干净，状态映射表（statusConfig）集中管理，是列表页里完成度较高的一页。

**主要问题**

- [中] 与 Orders 相同的两处问题：状态卡与 segmented 重复提供筛选；任务一次性全量拉取（无分页），数据量大时有性能风险。
- [中] "已送达"卡片 hint 写"今日完成配送"（L43），但统计的是**全部任务**中 status=50 的数量，文案与数据不符。
- [低] 状态用 `el-tag type="primary"`（待取餐/已取餐）渲染为大块深绿 tag，视觉权重超过真正需要关注的"异常"(danger)；建议异常以外的状态改 plain 描边样式。
- [低] 表格无操作列（只读），对"异常"任务没有任何处理入口（联系骑手/改派/标记处理），异常卡片 hint 说"需要及时处理"却无处可点——信息架构断点。
- [低] 文件开头有一串不可见 BOM 字符（L1），OrderList/ReviewList 同样存在，建议清理。

### 3.7 inventory/Inventory.vue —— 库存管理（7.0/10）

**现状**：统计卡 + tabs（预警/变动日志）双表，预警按缺口排序、正负数红绿着色，数据展示合理；还通过 `setBreadcrumbSubtitle` 联动了顶栏面包屑（全端唯一用到这个机制的页面）。

**主要问题**

- [中] **只读页面**：预警列表没有"去补货/下架"操作入口（Dashboard 库存预警卡都有"去补货 →"链接），看完预警要手动跳菜品页，闭环断裂。
- [中] "库存正常"统计（L31）= 预警列表总数 - 售罄 - 低库存，但预警列表本身就是"需要关注的菜品"，用它的子集算"正常"在语义上是错的（真正正常的菜品不在列表里）。
- [低] 变动日志的变动类型统一 `type="info"` tag（L138），IN/OUT/LOCK/ADJUST 语义不同却同色系，建议入库绿/出库橙/调整灰。
- [低] 两表共用同一 `loading`，切 tab 无独立加载态；无分页。

### 3.8 reviews/ReviewList.vue —— 评价管理（7.0/10）

**现状**：统计卡（均分/未回复/低分/总数）+ 筛选 segmented + 表格 + 回复弹窗（带原文上下文），"待安抚"状态标签有业务洞察，筛选维度设计合理。

**主要问题**

- [中] 统计卡（尤其"平均评分"）基于当前页数据计算（L40），翻页均分会跳变，应后端聚合或标注"本页"。
- [低] 已回复的回复按钮置灰显示"已处理"（L160-162），占着操作列却不能点——不如对已回复行显示"查看回复"text 链接。
- [低] 评分列 `el-rate disabled show-score` 渲染 Element 默认橙黄星，宽度 150px 偏大；"低分"阈值 ≤3 分写死，与 Dashboard 无共享常量。
- [低] 回复弹窗无字数限制/计数。

### 3.9 statistics/Statistics.vue —— 销售统计（5.5/10）

**现状**：四张指标卡 + 近 7 日 CSS 柱状图 + 期间表格 + 热销排行列表，空态用 el-empty 处理了。

**主要问题**

- [高] **手搓 CSS 柱状图**（`.bars`/`.bar-track`/`.bar-fill`，L262-294）与 Dashboard 的 ECharts 方案并存：无 tooltip、无坐标轴、无 hover，柱子是纯深绿大方块（`background: var(--bs-primary)`），与基准图的 2.5px 细线 + 渐变面积 + 圆角细柱风格差距明显。同一份数据两种图表语言。
- [高] **与 Dashboard 功能重复**：Dashboard 已有"经营趋势 PERFORMANCE"和"热销 TOP 5 BEST SELLERS"（同为近 7 日 + top 菜品），本页是低清复刻版。要么做深（时间范围切换、同比环比、分类维度、导出），要么合并入口。
- [中] 柱状图与下方期间表格表达同一份数据（日期/销售额/订单数），图 + 表 + 排行三段式信息冗余；零值柱 `min-height: 8px` 无"无数据"语义。
- [低] 排行名次用深绿实心圆（`.rank`），前三名无差异化；金额无千分位（`toFixed(2)`，Dashboard 已用 toLocaleString）。

### 3.10 finance/Finance.vue —— 资金中心（4.0/10）

**现状**：四张资金卡 + 资金流水/结算记录双表，功能完整且有全端唯一的 error 兜底 alert。

**主要问题**

- [高] **整个文件的 template 和 style 全部压缩成单行**（L34/L44-47/L50），是未经格式化的脚手架直出版，可维护性为零，与全端其他文件风格格格不入。
- [高] 散装色值：卡片左边条 `#9eb8a9`、收入绿 `#2f8f63`、支出红 `#c06b5a`（L50）——第三套绿/红体系（token 是 #1B6B4A/#D9534F，基准是 #1E9E62/#D97B2B）。
- [中] 金额用 `toFixed(2)` 无千分位、无 tabular-nums；资金页是数字密度最高的页面，基准的数字规范恰恰最该用在这里。
- [中] "累计净收入"是前端 `totalIncome - totalCommission` 现算的（L14），口径应以后端为准；卡片 em 文案"管理员可为此余额创建结算"是把后端逻辑说明直接当 UI 文案，商家读不懂。
- [低] 流水类型/结算状态用内联三元嵌套（L44）而非映射表，与 Delivery 的 statusConfig 模式不一致；状态纯文本无 tag 着色。

### 3.11 shop/ShopInfo.vue —— 店铺管理（5.5/10）

**现状**：单卡片大表单（查看/编辑双态切换），含营业开关、Logo 上传、配送范围、工作日/周末营业时间，业务覆盖完整。

**主要问题**

- [高] 营业开关内联色 `style="--el-switch-on-color: #1f6b4a"`（L268）——与 token 的 `#1B6B4A` 差一位（疑似手误），且全端唯一一处内联改 Element 组件色。
- [中] "营业执照"是一个**纯文本输入框**让商家手填图片地址（L314-316），而入驻页（MerchantApply）已有完整的上传组件——同端两种标准，且手填 URL 是普通商家不可能完成的操作。
- [中] 查看态用 disabled 的 el-input 呈现（灰底灰字），信息可读性差；非编辑态应用 descriptions/纯文本展示。
- [低] `var(--bs-border-color)` / `--bs-text-secondary` / `--bs-bg-secondary` 失效变量约 10 处（L486-618，同 DishList 问题）；`.btn-cancel` 因变量失效实际是无边框透明按钮。
- [低] `style="padding-top: 20px;"` 内联（L258）；自绘 `.image-preview-modal` 与 DishList 重复实现。
- [低] 保存无确认、无表单校验（电话格式、时间区间 min<max 合法性未校验）。

### 3.12 profile/Profile.vue —— 个人中心（5.5/10）

**现状**：单卡片表单（头像上传 + 昵称/手机/邮箱），功能闭环完整（上传即传、保存同步 userStore）。

**主要问题**

- [中] 与 ShopInfo 同构问题：`card-header h3` 旧范式页头、失效变量 `--bs-border-color`（L211/225 两处，头像边框实际不显示）。
- [中] 表单零校验：手机号/邮箱格式、昵称长度均无 rules，保存直接提交；空值转 null 的逻辑（L73-78）意味着用户清空字段即清空数据且无确认。
- [低] 移除头像按钮（`.remove-btn` 红色圆点）点击无确认、且**只清前端状态**，保存前刷新页面头像还在——交互语义模糊。
- [低] `style="padding-top: 20px;"` 内联（L110）。

### 3.13 apply/MerchantApply.vue —— 商家入驻（6.5/10）

**现状**：独立全屏页（不在 MerchantLayout 内），状态面板 + 表单双栏，状态机（待审核/通过/驳回/关闭）文案完整，是体验较完整的一页。

**主要问题**

- [中] **第四套色板**：背景 #f5f7f6、文字 #1f2d28、状态色 #2f8f55/#c28223/#c84f45、边框 #d8dfdb/#e3e8e5、阴影 `0 8px 22px rgba(28,45,38,0.05)`（L227-316）——与 token、基准都不沾边，阴影也比全端任何卡片都重。
- [中] 提交只做三个字段的非空检查（L106-109），电话格式、执照编号格式无校验；`deliveryRange`/`businessHours` 以 JSON 字符串硬编码在表单默认值里（L27-28），入驻后无法在表单上编辑这两项（与 ShopInfo 的能力不对等）。
- [低] `.status-icon` 用 34px 大尺寸彩色图标 + `el-tag effect="plain"` 重复表达同一状态。
- [低] 被驳回后重新提交时，无"修改了哪些字段"的提示。

---

## 四、共性问题汇总（全端性）

按修复收益排序：

1. **基准语言未沉淀 token（根因）**：Dashboard 的 #1E9E62/#123326/hairline/10px 角全部锁在 `.db` 局部；tokens.scss 仍是旧深绿主题。→ 改 tokens + element-plus-theme 两个文件，全端自动靠拢基准。
2. **Element 语义色未收敛**：`--el-color-success/danger/warning` 缺失，全端 success 按钮/tag 是 Element 默认亮绿 #67C23A、danger 是 #F56C6C，与 `$status-danger: #D9534F` 双红并存。
3. **失效 CSS 变量约 35 处**：`--bs-border-color`、`--bs-text-primary`、`--bs-text-secondary`、`--bs-bg-secondary` 在 DishList/ShopInfo/Profile 大量使用但从未定义，声明静默失效。
4. **样式块跨页复制粘贴**：`fadeIn` keyframes + `.page-container` 在 11 个页面各定义一遍；`.card-panel` 在 7 个页面重复定义；`.summary-grid/.summary-item`（含 border-left 状态条、28px 数字）在 6 个页面复制且状态色硬编码小写 #b76e2a/#1b6b4a/#8a9299。→ 应提取共享样式或 `<PageHeader>/<StatCard>` 组件。
5. **统计卡口径造假风险**：Orders/Reviews/DishList/ComboList/Statistics 的统计卡全部基于"当前页数据"计算，翻页即变，误导经营判断。
6. **两套页头范式并存**：`page-head h2(20px)+副标题+刷新`（8 页）vs `card-header h3(16px)+按钮`（3 页）；按钮体系三套：el-button / 自绘 .btn / Dashboard .btn-green span。
7. **脚手架残留**：Finance.vue 全文单行压缩；OrderList.vue 第二个压缩 style 块；文件头部 BOM 串（Delivery/OrderList/ReviewList）；`menuItems.push`；`fetchLogo` 死代码；global.scss 里无人使用的 `.status-badge`。
8. **双重筛选控件**：Orders/Delivery 同时提供可点击统计卡和 el-segmented 筛选，功能重复。
9. **操作闭环断裂**：Delivery 异常任务、Inventory 预警菜品均只有展示没有处理入口，和 Dashboard"可操作列表"的设计哲学相反。
10. **数字规范缺失**：仅 Dashboard 使用 toLocaleString 千分位 + tabular-nums；Finance/Statistics 等数字密集页全部 `toFixed(2)` 直出。

---

## 五、改造优先级排序

### P0 — 地基（一次改动，全端受益）
| 项 | 文件 | 工作量 |
|---|---|---|
| 基准色/圆角/hairline/米白底下沉 token；新增 accent 系列变量 | `styles/tokens.scss` | 0.5 天 |
| 补齐 `--el-color-success/danger/warning`，修正 `--el-bg-color-overlay` 误用 | `styles/element-plus-theme.scss` | 0.5 天 |
| 修复约 35 处失效 CSS 变量（全局替换为已定义 token） | DishList / ShopInfo / Profile | 0.5 天 |
| 提取公共 `.page-head` / `.summary-grid` / `.card-panel` / `fadeIn` 到共享样式或组件 | 新增共享样式 + 各页删除重复 | 1 天 |

### P1 — 小改即可对齐（结构已正确，只需换皮 + 局部修补）
| 页面 | 关键动作 | 工作量 |
|---|---|---|
| Delivery.vue | segmented/卡片筛选二选一；异常行加处理入口；修正"今日"文案；清 BOM | 0.5 天 |
| Inventory.vue | 预警表加"去补货"操作列；修正"库存正常"统计语义；变动类型分色 | 0.5 天 |
| ReviewList.vue | 统计口径改全量；已回复行改"查看回复"链接 | 0.5 天 |
| OrderList.vue | 操作列收敛（主按钮 + 详情）；删压缩 style 块换 token 色；统计口径标注或接聚合接口 | 1 天 |
| CategoryList.vue | 切到 page-head 范式；去 `border stripe`；删除改 plain；加关联菜品数提示 | 0.5 天 |
| Statistics.vue | CSS 柱状图换 ECharts（复用 Dashboard 配置风格）；金额千分位 | 1 天 |

### P2 — 需要中度重构（结构或交互有明显缺陷）
| 页面 | 关键动作 | 工作量 |
|---|---|---|
| DishList.vue | 操作列收敛；查看/编辑弹窗合并；统一低库存阈值；预览换 el-image-viewer | 1.5 天 |
| ComboList.vue | 编辑弹窗拆分步/分区；散装灰阶色全部 token 化；与 DishList 统一按钮规范 | 1.5 天 |
| ShopInfo.vue | 查看态改 descriptions；营业执照改上传组件；营业开关色值修正；加表单校验 | 1 天 |
| Profile.vue | 加表单校验；头像移除语义修正（即时生效或随保存生效二选一） | 0.5 天 |
| MerchantLayout.vue | 删 JS 注入样式、收敛 !important；active 态加品牌绿指示；logo 死代码处理 | 1 天 |

### P3 — 建议按 Dashboard 标准重写
| 页面 | 理由 | 工作量 |
|---|---|---|
| Finance.vue | 全文压缩脚手架 + 独立第三套色板 + 数字规范最缺失，修补成本高于重写；资金页值得按基准 hero 卡 + 流水列表重做 | 1.5 天 |
| MerchantApply.vue | 独立第四套色板；作为商家第一眼页面，应对齐基准（米白底 + hairline 卡 + accent 按钮） | 1 天 |

**建议路线**：P0（约 2.5 天）→ P1（约 4 天）→ P2（约 5 天）→ P3（约 2.5 天），总计约 14 个工作日。P0 完成后全端自动获得 60% 的一致性提升，是性价比最高的第一步。

---

*本报告为只读评审，未修改任何源码。所有行号引用基于评审时的工作区版本（master，最近提交 c852544）。*
