# 商家资金中心与管理员结算 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build merchant finance viewing and administrator settlement operations on top of the existing fund account, ledger, and settlement tables.

**Architecture:** Keep accounting mutations in `MerchantFinanceService`; expose merchant-scoped and admin-scoped controllers; add thin TypeScript API modules and pages following existing layout/router patterns. Settlement transitions use optimistic status updates and idempotent ledger keys.

**Tech Stack:** Spring Boot, MyBatis, JUnit 5/Mockito, Vue 3, TypeScript, Element Plus.

---

### Task 1: Extend settlement accounting service

**Files:**
- Modify: `BiteSmart/src/main/java/com/ws/bitesmart/service/merchant/MerchantFinanceService.java`
- Modify: `BiteSmart/src/main/java/com/ws/bitesmart/mapper/merchant/MerchantSettlementMapper.java`
- Modify: `BiteSmart/src/main/resources/mapper/merchant/MerchantSettlementMapper.xml`
- Test: `BiteSmart/src/test/java/com/ws/bitesmart/service/merchant/MerchantFinanceServiceTest.java`

- [ ] Write failing tests for account freeze, completion, rejection, amount overflow, and repeated transitions.
- [ ] Run `mvn.cmd -q "-Dtest=MerchantFinanceServiceTest" test` and confirm the new tests fail for missing methods.
- [ ] Implement create, approve, complete, and reject transitions with account row locking, status predicates, and ledger idempotency keys.
- [ ] Run the same test target and confirm all tests pass.

### Task 2: Add merchant finance APIs

**Files:**
- Create: `BiteSmart/src/main/java/com/ws/bitesmart/controller/merchant/MerchantFinanceController.java`
- Create: `BiteSmart/src/test/java/com/ws/bitesmart/controller/merchant/MerchantFinanceControllerTest.java`

- [ ] Test that the controller resolves the merchant ID from the authenticated user and returns overview, ledger, and settlement data.
- [ ] Implement the three GET endpoints with 401 handling and the existing `ResultVO` response format.
- [ ] Run the controller test.

### Task 3: Add admin finance APIs

**Files:**
- Create: `BiteSmart/src/main/java/com/ws/bitesmart/controller/admin/AdminFinanceController.java`
- Create: `BiteSmart/src/test/java/com/ws/bitesmart/controller/admin/AdminFinanceControllerTest.java`

- [ ] Test account/settlement listing and all transition endpoints.
- [ ] Implement admin-only account and settlement queries plus create/approve/complete/reject endpoints.
- [ ] Run admin finance tests.

### Task 4: Add frontend API modules and routes

**Files:**
- Create: `BiteSmart_front/src/api/merchant/finance.ts`
- Create: `BiteSmart_front/src/api/admin/finance.ts`
- Modify: `BiteSmart_front/src/router/index.ts`
- Modify: `BiteSmart_front/src/layouts/MerchantLayout.vue`
- Modify: `BiteSmart_front/src/layouts/AdminLayout.vue`

- [ ] Add typed request functions and route/menu entries.
- [ ] Run `npm.cmd run build` to catch route and type errors.

### Task 5: Build finance pages

**Files:**
- Create: `BiteSmart_front/src/views/merchant/finance/Finance.vue`
- Create: `BiteSmart_front/src/views/admin/finance/Finance.vue`

- [ ] Build merchant overview, ledgers, settlements, loading, empty, and failure states.
- [ ] Build admin account and settlement tables with create and transition dialogs.
- [ ] Run `npm.cmd run build` and verify both routes render.

### Task 6: Integration verification

- [ ] Run `mvn.cmd -q test`.
- [ ] Run `npm.cmd run build`.
- [ ] Verify settlement amount cannot exceed available balance and repeated completion does not create a second ledger.
