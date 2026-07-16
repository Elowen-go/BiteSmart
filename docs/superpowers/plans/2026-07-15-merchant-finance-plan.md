# Merchant Finance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add an auditable internal merchant-funds ledger around Alipay payment and refund flows, with merchant and admin visibility.

**Architecture:** Keep Alipay responsible for customer payment and original-payment refunds. Keep merchant balances and settlement records in BiteSmart tables, updated transactionally and idempotently from payment/refund events. Expose separate merchant and admin finance APIs and add compact pages using existing layouts.

**Tech Stack:** Spring Boot 3, MyBatis XML, MySQL, Vue 3, Element Plus, TypeScript.

---

### Task 1: Add finance schema and entities

**Files:** create migration under `docs/db`, create merchant finance entities, mappers and XML.

- Add account, immutable ledger and settlement tables with unique idempotency keys and decimal amounts.
- Add mapper methods for account creation/locking, ledger insertion, settlement queries and settlement status updates.
- Verify the migration applies on the live `bitesmart` database before using the new services.

### Task 2: Add payment/refund accounting service

**Files:** create `MerchantFinanceService`, modify `PaymentService`, `AlipayPaymentService`, and admin refund handling.

- Payment success credits merchant pending balance and records commission and income entries once.
- Completion releases pending balance to available balance.
- Alipay refund uses `alipay.trade.refund`; successful refund creates reversal entries and updates payment/refund records.
- Add service tests for idempotency, amount validation and refund rollback behavior.

### Task 3: Add merchant finance APIs

**Files:** create merchant finance controller/service API and frontend API module.

- Return overview totals, paginated ledgers and settlement records for the authenticated merchant.
- Resolve merchant ID from login user ID before every query.
- Add controller tests for ownership and response shape.

### Task 4: Add admin finance APIs

**Files:** create admin finance controller/service API and frontend API module.

- List merchant accounts, inspect a merchant ledger, create settlement batches and mark a batch paid.
- Reject settlement amounts greater than available balance and make status transitions idempotent.
- Add controller/service tests for over-settlement and repeated payment.

### Task 5: Add merchant and admin views

**Files:** add merchant finance page, admin finance page, route/menu entries and styles.

- Show balance cards, income/refund/commission totals, ledger table and settlement table.
- Add loading, empty and failure states using existing project components.
- Build the frontend and verify the pages at desktop and narrow widths.

### Task 6: End-to-end verification

- Run focused Maven tests and frontend build.
- Start the backend with the local profile.
- Verify one paid order creates merchant pending income, one refund creates a negative ledger, and one admin settlement moves available balance to settled.
