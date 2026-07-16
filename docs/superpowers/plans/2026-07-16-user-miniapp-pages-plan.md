# BiteSmart User Mini Program Pages Implementation Plan

> **For agentic workers:** Execute this plan task-by-task with verification after each task.

**Goal:** Build the user-facing mini-program flow from the product plan, using the supplied health-management UI language while keeping role-based merchant and delivery workspaces isolated.

**Architecture:** Keep `pages/index/index` as the role entry and health dashboard, then add focused user pages for profile, AI, food, cart, checkout, orders, delivery, health records, membership, and review. Put transport calls in `miniprogram/api`, auth and role checks in `utils`, and share the same custom bottom navigation style across user pages.

**Tech Stack:** WeChat Mini Program, TypeScript, WXML, Sass, existing JWT/WeChat auth wrapper, existing backend REST endpoints.

---

### Task 1: User route and shared navigation foundation

**Files:**
- Modify: `BiteSmartMin/miniprogram/app.json`
- Create: `BiteSmartMin/miniprogram/utils/user-route.ts`
- Create: `BiteSmartMin/miniprogram/components/user-nav/user-nav.json`
- Create: `BiteSmartMin/miniprogram/components/user-nav/user-nav.wxml`
- Create: `BiteSmartMin/miniprogram/components/user-nav/user-nav.ts`
- Create: `BiteSmartMin/miniprogram/components/user-nav/user-nav.scss`

- [ ] Register the user routes: `profile`, `ai`, `chat`, `food`, `food-detail`, `cart`, `checkout`, `order`, `order-detail`, `delivery`, `health`, `member`, and `review`.
- [ ] Add a route helper that rejects missing or non-user `roleType` before entering user pages.
- [ ] Build the five-item user navigation matching the supplied reference: records, AI assistant, recipes, fasting, and profile.
- [ ] Verify every registered page has JSON, WXML, TypeScript, and style files.

### Task 2: Health and AI experience

**Files:**
- Create: `BiteSmartMin/miniprogram/pages/profile/*`
- Create: `BiteSmartMin/miniprogram/pages/ai/*`
- Create: `BiteSmartMin/miniprogram/pages/chat/*`
- Create: `BiteSmartMin/miniprogram/pages/health/*`
- Create: `BiteSmartMin/miniprogram/api/user.ts`
- Create: `BiteSmartMin/miniprogram/api/ai.ts`
- Create: `BiteSmartMin/miniprogram/api/health.ts`

- [ ] Implement the health questionnaire and editable profile fields for goal, body data, food preferences, allergies, exercise frequency, and health target.
- [ ] Implement recommendation cards with nutrition explanation and add-to-cart entry.
- [ ] Implement AI chat history, message sending, loading, empty, and error states.
- [ ] Implement diet, exercise, weight, and daily summary records with add/edit/delete entry points.
- [ ] Verify user-only routing and empty/error states without a backend connection.

### Task 3: Food purchase flow

**Files:**
- Create: `BiteSmartMin/miniprogram/pages/food/*`
- Create: `BiteSmartMin/miniprogram/pages/food-detail/*`
- Create: `BiteSmartMin/miniprogram/pages/cart/*`
- Create: `BiteSmartMin/miniprogram/pages/checkout/*`
- Create: `BiteSmartMin/miniprogram/api/catalog.ts`
- Create: `BiteSmartMin/miniprogram/api/cart.ts`
- Create: `BiteSmartMin/miniprogram/api/order.ts`

- [ ] Implement food categories, search, list cards, nutrition facts, suitable groups, and dish detail.
- [ ] Implement combo detail and ingredient replacement with recalculated price and nutrition.
- [ ] Implement cart quantity changes, selection, deletion, replacement entry, delivery fee, and checkout.
- [ ] Implement address selection, delivery method/time, note, amount confirmation, and submit order.
- [ ] Verify cart-to-checkout navigation preserves selected items and rejects empty checkout.

### Task 4: Orders, delivery, membership, and review

**Files:**
- Create: `BiteSmartMin/miniprogram/pages/order/*`
- Create: `BiteSmartMin/miniprogram/pages/order-detail/*`
- Create: `BiteSmartMin/miniprogram/pages/delivery/*`
- Create: `BiteSmartMin/miniprogram/pages/member/*`
- Create: `BiteSmartMin/miniprogram/pages/review/*`
- Modify: `BiteSmartMin/miniprogram/pages/index/index.ts`
- Modify: `BiteSmartMin/miniprogram/pages/index/index.wxml`

- [ ] Implement order filters for all, unpaid, preparing, delivering, completed, and cancelled states.
- [ ] Implement order detail with items, address, note, status timeline, cancel, reorder, pay, and review actions.
- [ ] Implement delivery tracking with driver information, current status, estimated arrival, and refresh state.
- [ ] Implement membership level, benefits, plan purchase, renewal, expiry, and purchase history.
- [ ] Implement dish, merchant, driver, and order reviews with star, text, image placeholder, and complaint entry.
- [ ] Verify user pages never render for merchant or delivery roles and run JSON, TypeScript, WXML, and diff checks.
