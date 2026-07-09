# BiteSmart 智能健康膳食管理平台

## 项目简介

BiteSmart 是一个基于人工智能的智能健康膳食管理平台，通过 AI 技术为用户提供个性化的饮食推荐、健康管理和在线订餐服务。平台涵盖 PC 端管理后台、微信小程序用户端，支持用户、商家、配送员、管理员四种角色协同运作。

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 4.0.8-SNAPSHOT |
| 编程语言 | Java | 17 |
| 数据库 | MySQL | 8.0+ |
| 缓存 | Redis | 7.0+ |
| ORM | MyBatis | 4.0.1 |
| 认证授权 | Spring Security + JWT | - |
| 实时通信 | WebSocket | - |
| PC端前端 | Vue 3 + TypeScript | Vue 3.5.38 |
| 小程序端 | 微信小程序原生 + TypeScript | - |
| 构建工具 | Maven / Vite | Maven 3.9+ / Vite 8.0+ |

## 项目结构

```
d:\demo\BiteSmart/
├── BiteSmart/               # Spring Boot 后端
│   ├── src/main/java/       # Java 源代码
│   ├── src/main/resources/  # 配置文件
│   └── pom.xml             # Maven 依赖管理
├── BiteSmartMin/            # 微信小程序
│   └── miniprogram/         # 小程序源码
├── BiteSmart_front/         # Vue 3 前端管理后台
│   └── src/                # 前端源代码
├── bitesmart.sql           # 数据库设计脚本（43张表）
├── 开发流程与阶段.md        # 开发流程与阶段规划文档
├── 详细开发文档.md          # 详细技术开发文档
└── README.md               # 本文件
```

## 功能概览（70项）

### PC端（37项）

| 角色 | 功能数 | 核心功能 |
|------|--------|----------|
| 用户端 | 15项 | 注册登录、AI食谱生成、AI智能问答、套餐浏览与购买、在线下单支付、订单管理、配送跟踪、饮食/运动/体重记录、会员中心 |
| 管理员端 | 12项 | 后台登录、用户管理、商家管理、配送员管理、菜品分类、营养标准、AI规则、订单管理、评论管理、公告管理、数据统计、系统管理 |
| 商家端 | 10项 | 商家入驻、店铺维护、菜品管理、套餐管理、订单处理、库存管理、营养维护、销售统计、评价管理、配送管理 |

### 微信小程序端（33项）

| 角色 | 功能数 | 核心功能 |
|------|--------|----------|
| 用户端 | 16项 | 微信登录、AI推荐、AI对话、菜品浏览、套餐购买、购物车、在线下单支付、订单管理、配送追踪、健康记录、评论反馈、会员中心 |
| 商家端 | 8项 | 移动端登录、店铺维护、菜品/套餐管理、订单处理、库存预警、销售统计、评价管理 |
| 配送员端 | 9项 | 配送员登录、接单管理、配送状态更新、实时定位上传、路线导航、收入统计、异常上报、评价查看 |

## 数据库（43张表）

| 模块 | 表数 | 说明 |
|------|------|------|
| 用户与权限域 | 10张 | 用户、档案、会员、商家、配送员、地址、审计 |
| 健康记录域 | 3张 | 饮食、运动、体重 |
| 商品域 | 4张 | 分类、菜品、套餐、关联关系 |
| 订单与支付域 | 6张 | 购物车、订单、明细、支付、退款、库存流水 |
| 配送与物流域 | 1张 | 配送任务 |
| 评价与AI交互域 | 2张 | 评价、AI对话 |
| 系统运维与配置域 | 10张 | 配置、公告、消息、日志、备份、字典、文件 |
| 企业级扩展表 | 7张 | 报表统计、优惠券、投诉、营养详情、资金流水 |

## 快速开始

### 环境要求

- JDK 17+
- MySQL 8.0+
- Redis 7.0+
- Node.js 22+
- Maven 3.9+
- 微信开发者工具

### 本地运行

```bash
# 1. 创建数据库并导入脚本
mysql -u root -p -e "CREATE DATABASE bitesmart CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p bitesmart < bitesmart.sql

# 2. 启动后端
cd BiteSmart
mvn spring-boot:run

# 3. 启动前端管理后台
cd BiteSmart_front
npm install
npm run dev

# 4. 使用微信开发者工具打开 BiteSmartMin 目录
```

## 远程仓库

- Gitee: https://gitee.com/vibe-wave/bite-smart.git
- GitHub: https://github.com/Elowen-go/BiteSmart.git

## 许可证

本项目仅用于学习交流。
