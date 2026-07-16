# 商家资金中心与管理员结算设计

## 目标

为商家提供只读资金总览、资金流水和结算记录，为管理员提供商家资金账户查询、结算单创建、审核和完成结算能力。

## 边界

- 商家只能通过当前登录商家 ID 查询自己的账户、流水和结算单。
- 管理员可以按商家和结算状态查询全部账户及结算单。
- 结算金额必须大于 0 且不超过可结算余额。
- 创建结算单时冻结对应可结算余额；完成结算时扣除冻结余额；驳回时解冻回可结算余额。
- 结算操作使用结算单状态条件更新和资金流水幂等键，重复请求不重复扣款。
- 暂不实现支付宝向商家账户自动转账，管理员完成结算表示平台内部结算完成。

## 页面

商家端新增“资金中心”，包含四个金额指标、资金流水表和结算记录表，提供加载、空数据和失败提示。

管理端新增“商家结算”，包含商家账户表、结算单表和创建/审核/完成/驳回操作。完成结算前展示商家、金额和收款方式确认信息。

## 接口

- `GET /api/merchant/finance/overview`
- `GET /api/merchant/finance/ledgers`
- `GET /api/merchant/finance/settlements`
- `GET /api/admin/finance/accounts`
- `GET /api/admin/finance/settlements`
- `POST /api/admin/finance/settlements`
- `PUT /api/admin/finance/settlements/{id}/approve`
- `PUT /api/admin/finance/settlements/{id}/complete`
- `PUT /api/admin/finance/settlements/{id}/reject`

## 测试

服务层覆盖账户查询、金额上限校验、创建结算冻结余额、完成扣除冻结余额、驳回解冻余额和重复操作幂等；控制器覆盖商家身份隔离和管理员接口参数校验。
