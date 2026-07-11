# 登录角色验证功能实现计划

## 需求分析

当前登录页面的角色选择器（普通用户/商家/管理员）没有实际作用，用户希望实现：
- 用户选择某个角色后，只有该角色类型的账号才能登录成功
- 如果账号的实际角色与选择的角色不匹配，应返回错误提示

## 技术架构

### 当前角色类型定义
| 角色 | roleType |
|------|----------|
| 用户 | 10 |
| 商家 | 20 |
| 配送员 | 30 |
| 管理员 | 40 |

### 当前登录流程
```
前端提交 username + password → 后端查询用户 → 校验密码 → 返回token
```

### 修改后登录流程
```
前端提交 username + password + roleType → 后端查询用户 → 校验密码 → 校验角色匹配 → 返回token
```

## 修改文件清单

### 1. 后端 - LoginRequestDTO.java
**路径**: `BiteSmart/src/main/java/com/ws/bitesmart/dto/request/LoginRequestDTO.java`
- 添加 `roleType` 字段

### 2. 后端 - AuthService.java
**路径**: `BiteSmart/src/main/java/com/ws/bitesmart/service/user/AuthService.java`
- 在 `login()` 方法中增加角色校验逻辑
- 如果用户实际角色与请求角色不匹配，抛出业务异常

### 3. 后端 - ResultCodeEnum.java
**路径**: `BiteSmart/src/main/java/com/ws/bitesmart/common/enums/ResultCodeEnum.java`
- 添加新的错误码 `ROLE_NOT_MATCH`

### 4. 前端 - Login.vue
**路径**: `BiteSmart_front/src/views/login/Login.vue`
- 保留角色选择器（之前误删了，需要恢复）
- 将 `roleType` 字段传递给后端 API

### 5. 前端 - auth.ts
**路径**: `BiteSmart_front/src/api/auth.ts`
- 更新登录接口类型定义，添加 `roleType` 参数

## 实施步骤

### 步骤一：修改后端请求DTO
添加 `roleType` 字段到 `LoginRequestDTO`

### 步骤二：添加错误码枚举
在 `ResultCodeEnum` 中添加 `ROLE_NOT_MATCH("角色不匹配，请选择正确的角色类型")`

### 步骤三：修改登录服务逻辑
在 `AuthService.login()` 方法中，校验用户角色是否与请求的角色类型匹配

### 步骤四：修改前端登录页面
恢复角色选择器，更新登录请求参数

### 步骤五：修改前端API类型定义
更新 `auth.ts` 中的类型定义

## 风险与注意事项

1. **兼容性**: 修改后，未传递 `roleType` 的旧请求将无法登录，需确保前端同步修改
2. **角色类型映射**: 前端显示的角色名称需与后端的 `roleType` 值正确映射
3. **错误提示**: 需要提供清晰的错误提示信息给用户

## 验证方案

1. 使用管理员账号选择"普通用户"角色登录 → 应返回"角色不匹配"错误
2. 使用管理员账号选择"管理员"角色登录 → 应登录成功
3. 使用普通用户账号选择"商家"角色登录 → 应返回"角色不匹配"错误
4. 使用商家账号选择"商家"角色登录 → 应登录成功