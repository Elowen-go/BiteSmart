# AI Recipe Recommendation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将用户端 AI 食谱推荐改造成基于真实在售菜品的整日三餐计划，支持营养汇总、单菜替换和按商家加入购物车。

**Architecture:** 后端以健康档案和饮食限制筛选带营养数据的在售菜品，向 AI 提供带 ID 的候选池；AI 只返回候选菜品 ID 和餐次，后端重新查询并校验结果后计算权威营养值。前端使用结构化响应渲染三餐，并复用现有菜品详情、购物车 API；跨商家推荐按商家分组加入购物车。

**Tech Stack:** Spring Boot, MyBatis, Java DTO/Map response, Vue 3 Composition API, TypeScript, Element Plus, Axios。

---

## Task 1: 建立结构化推荐请求和响应模型

**Files:**
- Create: `D:\demo\BiteSmart\BiteSmart\src\main\java\com\ws\bitesmart\dto\request\AiRecommendRequest.java`
- Create: `D:\demo\BiteSmart\BiteSmart\src\main\java\com\ws\bitesmart\dto\response\AiRecipeResponse.java`
- Create: `D:\demo\BiteSmart\BiteSmart\src\main\java\com\ws\bitesmart\dto\response\AiRecipeMealResponse.java`
- Create: `D:\demo\BiteSmart\BiteSmart\src\main\java\com\ws\bitesmart\dto\response\AiRecipeItemResponse.java`

- [ ] **Step 1: Define request fields and defaults**

```java
public class AiRecommendRequest {
    private String mealType = "all";
    private String dietaryRestrictions;
}
```

Use Lombok annotations already used in the project. `mealType` accepts `all`, `breakfast`, `lunch`, or `dinner`; invalid values are rejected with a business error.

- [ ] **Step 2: Define response fields**

`AiRecipeResponse` contains `success`, `message`, target nutrition values, `summary`, `meals`, and `advice`. `AiRecipeMealResponse` contains `mealType` and `items`. `AiRecipeItemResponse` contains `dishId`, `dishName`, `merchantId`, `merchantName`, `dishImage`, `quantity`, `price`, `calories`, `protein`, `fat`, `carbs`, and `available`.

- [ ] **Step 3: Add DTO serialization test**

Add a Jackson test that serializes one meal with one item and asserts the JSON contains `dishId`, `mealType`, `summary`, and `available` with the expected values.

- [ ] **Step 4: Run the focused test**

Run from `D:\demo\BiteSmart\BiteSmart`:

```powershell
mvn.cmd test -Dtest=AiRecipeResponseTest
```

Expected: `BUILD SUCCESS`.

## Task 2: Build the database-backed dish candidate pool

**Files:**
- Modify: `D:\demo\BiteSmart\BiteSmart\src\main\java\com\ws\bitesmart\mapper\dish\DishMapper.java`
- Modify: `D:/demo/BiteSmart/BiteSmart/src/main/resources/mapper/dish/DishMapper.xml`
- Modify: `D:\demo\BiteSmart\BiteSmart\src\main\java\com\ws\bitesmart\service\ai\AiRecommendService.java`

- [ ] **Step 1: Add a candidate query**

Add a mapper method that selects dish ID, merchant ID, name, image, price, stock, status, and nutrition values where the dish is on sale, stock is greater than zero, the merchant is active, and nutrition values exist. Reuse the project’s existing dish status and merchant status constants/SQL values instead of introducing new status numbers.

- [ ] **Step 2: Add server-side restriction filtering**

Normalize `dietaryRestrictions` to lower-case text and filter known terms before sending candidates to AI: seafood terms exclude seafood-tagged dishes, low-sugar excludes dishes marked high sugar when that field exists, and allergy terms exclude matching ingredient/dish names. If the schema does not expose a matching field, keep the candidate and return the limitation as a prompt constraint rather than pretending it was verified.

- [ ] **Step 3: Add deterministic fallback selection**

When the AI key is unavailable, the AI response is malformed, or the candidate pool is too small, select candidates deterministically by meal type and lower calorie distance from the meal’s target. Return only database-backed items and a user-readable `advice` explaining that a rule-based recommendation was used.

- [ ] **Step 4: Add candidate-pool service tests**

Mock `DishMapper` and verify that unavailable dishes are not passed to the recommendation selector, and that a fallback result never contains an item without a positive `dishId` or nutrition data.

- [ ] **Step 5: Run the focused service test**

```powershell
mvn.cmd test -Dtest=AiRecommendServiceTest
```

Expected: `BUILD SUCCESS`.

## Task 3: Replace the recommendation endpoint contract

**Files:**
- Modify: `D:\demo\BiteSmart\BiteSmart\src\main\java\com\ws\bitesmart\controller\ai\AiRecommendController.java`
- Modify: `D:\demo\BiteSmart\BiteSmart\src\main\java\com\ws\bitesmart\BiteSmart\src\main\java\com\ws\bitesmart\service\ai\AiRecommendService.java`

- [ ] **Step 1: Change the endpoint to POST JSON**

Use `@PostMapping("/recommend")` and `@RequestBody AiRecommendRequest request`. Preserve the existing login check and pass `userId` plus request values to the service.

- [ ] **Step 2: Make the service validate AI output against the candidate map**

Build a `Map<Long, DishCandidate>` from the database result. Ignore IDs not in that map, duplicate IDs in the same meal, unavailable IDs, and quantities outside `1..3`. Recalculate every returned item’s price and nutrition from the candidate object, then calculate `summary` on the server.

- [ ] **Step 3: Implement meal targeting**

For `all`, create breakfast, lunch, and dinner groups. For a single meal type, return only that group and calculate summary for the returned group. The service must always return an empty `meals` array rather than null when no candidates are available.

- [ ] **Step 4: Add endpoint tests**

Test that `POST /api/ai/recommend` accepts JSON, returns `400` for an invalid meal type, and returns a response whose nutrition values match the mocked database values rather than values invented by the AI response.

- [ ] **Step 5: Run backend verification**

```powershell
mvn.cmd test
```

Expected: all tests pass.

## Task 4: Add replacement and cart-grouping server operations

**Files:**
- Create or modify: `D:\demo\BiteSmart\BiteSmart\src\main\java\com\ws\bitesmart\controller\ai\AiRecipeController.java`
- Create or modify: `D:\demo\BiteSmart\BiteSmart\src\main\java\com\ws\bitesmart\service\ai\AiRecipeCartService.java`
- Modify: `D:\demo\BiteSmart\BiteSmart\src\main\java\com\ws\bitesmart\mapper\dish\DishMapper.java`
- Modify: `D:\demo\BiteSmart\BiteSmart\src\main\java\com\ws\bitesmart\service\order\ShoppingCartService.java`

- [ ] **Step 1: Add replacement validation**

Expose `POST /api/ai/recommend/replace` with the original dish ID, replacement dish ID, meal type, and dietary restrictions. Rebuild the candidate pool, require the replacement to be in the same eligible pool, then return the updated item and server-calculated summary.

- [ ] **Step 2: Add grouped cart insertion**

Expose `POST /api/ai/recommend/cart`. Group recipe items by `merchantId`, recheck status and stock immediately before insertion, and call the existing cart service for each dish. Return per-merchant results with successful item IDs and explicit failure reasons.

- [ ] **Step 3: Add service tests**

Verify that replacement rejects an unavailable dish, duplicate dish selection, and a dish outside restrictions. Verify grouped insertion reports one merchant failure while preserving successful insertions for another merchant.

- [ ] **Step 4: Run backend tests**

```powershell
mvn.cmd test
```

Expected: all tests pass.

## Task 5: Build the structured AI recipe page

**Files:**
- Modify: `D:\demo\BiteSmart\BiteSmart_front\src\api\user\ai.ts`
- Modify: `D:\demo\BiteSmart\BiteSmart_front\src\views\user\ai\RecommendCenter.vue`
- Inspect/reuse: `D:\demo\BiteSmart\BiteSmart_front\src\api\user\dishes.ts`
- Inspect/reuse: `D:\demo\BiteSmart\BiteSmart_front\src\api\user\cart.ts`

- [ ] **Step 1: Align the frontend API methods**

Replace the current nested Axios `params` POST body with JSON request data:

```ts
export const aiRecommend = (data: AiRecommendRequest): Promise<any> =>
  request.post('/ai/recommend', data)

export const replaceAiRecipeDish = (data: ReplaceAiRecipeDishRequest): Promise<any> =>
  request.post('/ai/recommend/replace', data)

export const addAiRecipeToCart = (data: AddAiRecipeCartRequest): Promise<any> =>
  request.post('/ai/recommend/cart', data)
```

- [ ] **Step 2: Replace text-only result rendering**

Render target nutrition cards, summary progress, meal sections, dish rows, replacement select, unavailable state, advice, and per-merchant cart results. Use stable grid/flex dimensions so long dish names and AI advice do not overlap controls.

- [ ] **Step 3: Implement page state transitions**

Keep `mealType`, `dietaryRestrictions`, `result`, `loading`, and `replacingDishId` in component state. On generation, preserve input values on error. On replacement, update only the selected meal item and summary from the server response. Disable duplicate requests while loading.

- [ ] **Step 4: Add empty and failure states**

Show a health-profile completion action when `success === false` and the response identifies a missing profile. Show a candidate shortage message when a meal has no items. Show an actionable stock/unavailable label instead of allowing an unavailable item to enter the cart.

- [ ] **Step 5: Run frontend verification**

Run from `D:\demo\BiteSmart\BiteSmart_front`:

```powershell
npm run build
```

Expected: type-check and Vite build both pass.

## Task 6: Integration verification and documentation

**Files:**
- Modify: `D:\demo\BiteSmart\docs\superpowers\specs\2026-07-13-ai-recipe-design.md` only if implementation decisions differ from the approved design.
- Create: `D:\demo\BiteSmart\BiteSmart\src\test\java\com\ws\bitesmart\ai\AiRecipeIntegrationTest.java` if the existing test setup supports controller integration tests.

- [ ] **Step 1: Verify the complete happy path**

Generate all-day results, replace one dish, confirm summary changes, add to cart, and verify cart items are grouped by merchant.

- [ ] **Step 2: Verify failure paths**

Test missing profile, invalid meal type, empty candidate pool, AI failure, out-of-stock replacement, and partial merchant cart insertion. Each path must expose a specific reason to the user.

- [ ] **Step 3: Run final checks**

```powershell
Set-Location D:\demo\BiteSmart\BiteSmart
mvn.cmd test
Set-Location D:\demo\BiteSmart\BiteSmart_front
npm run build
```

Expected: backend tests and frontend build complete successfully.

- [ ] **Step 4: Review the final diff**

Run `git diff --check` and inspect only AI recipe files plus the design/plan documents. Do not revert unrelated existing shopping-cart, combo, Redis, upload, or generated-file changes in the worktree.
