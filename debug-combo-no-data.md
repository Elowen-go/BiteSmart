# Debug Session: combo-no-data

## Status
[OPEN]

## Symptoms
- 套餐管理界面显示 "No Data"
- 数据库中确认有套餐数据
- 前端表格没有渲染数据

## Hypotheses
1. **H1**: 前端请求参数错误（page/size 传递不正确）
2. **H2**: 后端返回的数据格式与前端期望不匹配
3. **H3**: merchant_id 不匹配（当前店铺ID与数据中的merchant_id不一致）
4. **H4**: 数据库中套餐数据的 merchant_id 与当前店铺ID不一致
5. **H5**: SQL查询条件问题（deleted=0 或其他条件过滤掉了数据）

## Evidence Collection Plan
1. 检查数据库中 combo 表的数据和 merchant_id
2. 检查前端实际发送的请求和接收的响应
3. 检查后端查询逻辑和返回的数据

## Progress
- [ ] Step 1: Database verification
- [ ] Step 2: Frontend request/response inspection
- [ ] Step 3: Backend query verification
- [ ] Step 4: Fix implementation
- [ ] Step 5: Verification
