# BiteSmart 前后端联调计划

## 一、当前状态分析

### 1.1 前端状态

| 项目      | 状态                                    |
| ------- | ------------------------------------- |
| 框架      | Vue3 + Vite + Element Plus            |
| 开发服务器   | <http://localhost:5174/> (已启动)        |
| 请求基础路径  | `/api`                                |
| 代理配置    | ❌ 未配置 (需要代理到后端8080)                   |
| API文件数量 | 2个 (`auth.ts`, `admin/statistics.ts`) |
| 页面数量    | 28个 (管理端9个, 商家端9个, 用户端10个)            |
| 数据状态    | 全部使用mock数据                            |

### 1.2 后端状态

| 项目           | 状态                             |
| ------------ | ------------------------------ |
| 框架           | Spring Boot + MyBatis          |
| 服务端口         | 8080                           |
| 数据库          | MySQL localhost:3306/bitesmart |
| Redis        | localhost:6379                 |
| Controller数量 | 38个                            |

### 1.3 待解决问题

1. **Vite代理未配置**：前端请求 `/api/*` 需要代理到 `http://localhost:8080/api/*`
2. **API文件缺失**：仅2个API文件，需要创建约20+个
3. **API路径不匹配**：前端调用 `/admin/statistics/dashboard`，后端实际为 `/admin/statistics/overview`
4. **页面未接入真实数据**：所有页面使用mock数据

***

## 二、联调步骤

### 阶段一：基础配置（第1步）

**目标**：确保前端能正常访问后端API

| 步骤  | 内容         | 文件                            |
| --- | ---------- | ----------------------------- |
| 1.1 | 配置Vite代理   | `vite.config.ts`              |
| 1.2 | 修复API路径不匹配 | `src/api/admin/statistics.ts` |

### 阶段二：认证模块联调（第2步）

**目标**：实现登录、登出功能

| 步骤  | 内容          | 文件                                    |
| --- | ----------- | ------------------------------------- |
| 2.1 | 登录页面接入真实API | `src/views/login/Login.vue`           |
| 2.2 | 验证登录成功后路由跳转 | `src/router/index.ts`                 |
| 2.3 | 验证登出功能      | `src/layouts/AdminLayout.vue` (及其他布局) |

### 阶段三：管理端页面联调（第3-11步）

| 序号 | 页面        | 后端API                                      | 前端API文件                       | 预计工作量 |
| -- | --------- | ------------------------------------------ | ----------------------------- | ----- |
| 3  | Dashboard | `/api/admin/statistics/overview`, `/trend` | `src/api/admin/statistics.ts` | 中     |
| 4  | 用户管理      | `/api/admin/users`                         | `src/api/admin/users.ts`      | 中     |
| 5  | 商家管理      | `/api/admin/merchants`                     | `src/api/admin/merchants.ts`  | 中     |
| 6  | 配送员管理     | `/api/admin/drivers`                       | `src/api/admin/drivers.ts`    | 中     |
| 7  | 订单管理      | `/api/admin/orders`                        | `src/api/admin/orders.ts`     | 中     |
| 8  | 评论管理      | `/api/admin/reviews`                       | `src/api/admin/reviews.ts`    | 中     |
| 9  | 系统设置      | `/api/admin/system/configs`                | `src/api/admin/system.ts`     | 中     |
| 10 | 操作日志      | `/api/admin/system/logs`                   | `src/api/admin/system.ts`     | 低     |
| 11 | 缺失页面开发    | AI规则/分类/营养标准/公告                            | 新建API文件 + 新建页面                | 高     |

### 阶段四：商家端页面联调（第12-20步）

| 序号 | 页面        | 后端API                                       | 前端API文件                          | 预计工作量 |
| -- | --------- | ------------------------------------------- | -------------------------------- | ----- |
| 12 | Dashboard | `/api/merchant/statistics/today`, `/period` | `src/api/merchant/statistics.ts` | 中     |
| 13 | 店铺管理      | `/api/merchant/shop`                        | `src/api/merchant/shop.ts`       | 低     |
| 14 | 菜品管理      | `/api/merchant/dishes`                      | `src/api/merchant/dishes.ts`     | 中     |
| 15 | 套餐管理      | `/api/merchant/combos`                      | `src/api/merchant/combos.ts`     | 中     |
| 16 | 订单处理      | `/api/merchant/orders`                      | `src/api/merchant/orders.ts`     | 中     |
| 17 | 库存管理      | `/api/merchant/inventory`                   | `src/api/merchant/inventory.ts`  | 中     |
| 18 | 配送管理      | `/api/merchant/delivery`                    | `src/api/merchant/delivery.ts`   | 中     |
| 19 | 评价管理      | `/api/merchant/reviews`                     | `src/api/merchant/reviews.ts`    | 低     |
| 20 | 销售统计      | `/api/merchant/statistics/top-dishes`       | `src/api/merchant/statistics.ts` | 低     |

### 阶段五：用户端页面联调（第21-30步）

| 序号 | 页面     | 后端API                                | 前端API文件                                            | 预计工作量 |
| -- | ------ | ------------------------------------ | -------------------------------------------------- | ----- |
| 21 | 首页     | `/api/dishes`, `/api/combos`         | `src/api/user/dishes.ts`, `src/api/user/combos.ts` | 中     |
| 22 | 菜品浏览   | `/api/dishes`                        | `src/api/user/dishes.ts`                           | 中     |
| 23 | 购物车    | `/api/cart`                          | `src/api/user/cart.ts`                             | 中     |
| 24 | 我的订单   | `/api/orders`                        | `src/api/user/orders.ts`                           | 中     |
| 25 | 配送追踪   | `/api/delivery/tracking`             | `src/api/user/delivery.ts`                         | 低     |
| 26 | AI对话   | `/api/ai/chat`                       | `src/api/user/ai.ts`                               | 中     |
| 27 | AI推荐   | `/api/ai/recommend`                  | `src/api/user/ai.ts`                               | 中     |
| 28 | 健康记录   | `/api/health/diet/exercise/weight`   | `src/api/user/health.ts`                           | 中     |
| 29 | 个人中心   | `/api/users/me`, `/api/user/profile` | `src/api/user/profile.ts`                          | 低     |
| 30 | 缺失页面开发 | 会员中心/地址管理/评价提交/公告                    | 新建API文件 + 新建页面                                     | 高     |

***

## 三、联调执行规范

### 3.1 API文件命名规范

```
src/api/
├── auth.ts                    # 认证相关
├── admin/
│   ├── statistics.ts          # 数据统计
│   ├── users.ts               # 用户管理
│   ├── merchants.ts           # 商家管理
│   ├── drivers.ts             # 配送员管理
│   ├── orders.ts              # 订单管理
│   ├── reviews.ts             # 评论管理
│   ├── system.ts              # 系统设置
│   ├── ai-rule.ts             # AI规则管理
│   ├── categories.ts          # 分类管理
│   ├── nutrition.ts           # 营养标准管理
│   └── notices.ts             # 公告管理
├── merchant/
│   ├── shop.ts                # 店铺管理
│   ├── dishes.ts              # 菜品管理
│   ├── combos.ts              # 套餐管理
│   ├── orders.ts              # 订单处理
│   ├── inventory.ts           # 库存管理
│   ├── delivery.ts            # 配送管理
│   ├── reviews.ts             # 评价管理
│   ├── statistics.ts          # 销售统计
│   └── categories.ts          # 分类管理
└── user/
    ├── profile.ts             # 个人信息
    ├── dishes.ts              # 菜品浏览
    ├── combos.ts              # 套餐浏览
    ├── cart.ts                # 购物车
    ├── orders.ts              # 订单管理
    ├── delivery.ts            # 配送追踪
    ├── ai.ts                  # AI服务
    ├── health.ts              # 健康记录
    ├── membership.ts          # 会员中心
    ├── addresses.ts           # 地址管理
    └── reviews.ts             # 评价提交
```

### 3.2 每个页面联调标准流程

1. **创建API文件**：根据后端Controller定义接口函数
2. **替换mock数据**：将页面中的ref数据替换为API调用
3. **添加加载状态**：使用 `loading` 状态显示加载中
4. **添加错误处理**：捕获API错误并显示提示
5. **添加分页逻辑**：如果后端支持分页，实现分页组件
6. **验证功能**：测试增删改查等核心操作

### 3.3 数据库准备

联调前需要确保数据库已初始化：

* 数据库名称：`bitesmart`

* 执行SQL脚本：`bitesmart.sql`

* 建议预先插入测试数据

***

## 四、风险与注意事项

| 风险        | 描述           | 应对措施               |
| --------- | ------------ | ------------------ |
| 后端未启动     | 前端请求无法到达后端   | 联调前确认后端服务运行        |
| 数据库连接失败   | 后端API返回500错误 | 确认MySQL服务和配置正确     |
| Redis连接失败 | 认证和缓存功能异常    | 确认Redis服务和配置正确     |
| API路径不匹配  | 前端404错误      | 对照后端Controller路径修正 |
| 跨域问题      | 浏览器CORS错误    | 使用Vite代理解决         |
| Token过期   | 请求返回401      | 前端已配置自动跳转登录页       |

***

## 五、启动联调前的准备工作

1. ✅ 启动前端开发服务器 (`npm run dev`)
2. ✅ 启动MySQL数据库 (端口3306)
3. ✅ 启动Redis服务 (端口6379)
4. ✅ 启动后端Spring Boot服务 (`mvn spring-boot:run`)
5. ✅ 确认数据库已初始化并包含测试数据

***

## 六、联调顺序建议

**建议按以下顺序逐个页面进行联调**：

1. **基础配置** → 确保代理和路径正确
2. **登录页面** → 确保认证流程通
3. **管理端Dashboard** → 验证统计数据展示
4. **用户管理** → 验证CRUD操作
5. **商家管理** → 验证审核功能
6. **订单管理** → 验证订单列表和详情
7. **依次完成其他管理端页面**
8. **商家端页面**
9. **用户端页面**
10. **缺失页面开发**

