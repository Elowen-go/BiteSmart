const fs = require('fs');
const path = require('path');
const ts = require(path.join(__dirname, '..', 'BiteSmart_front', 'node_modules', 'typescript'));

const ROOT = 'D:/demo/BiteSmart/BiteSmartMin/miniprogram';
const FILES = [
  'utils/safe-area.ts',
  'utils/request.ts',
  'utils/json.ts',
  'utils/merchant-vm.ts',
  'api/health.ts',
  'api/user.ts',
  'api/catalog.ts',
  'api/order.ts',
  'api/cart.ts',
  'api/feedback.ts',
  'api/plan.ts',
  'api/merchant.ts',
  'mock/health.ts',
  'pages/index/index.ts',
  'pages/food/food.ts',
  'pages/profile/profile.ts',
  'pages/health/health.ts',
  'pages/assessment/assessment.ts',
  'pages/plan/plan.ts',
  'pages/exercise/exercise.ts',
  'mock/catalog.ts',
  'pages/combo/combo.ts',
  'pages/combo-detail/combo-detail.ts',
  'pages/food-detail/food-detail.ts',
  'pages/cart/cart.ts',
  'pages/checkout/checkout.ts',
  'pages/pay-result/pay-result.ts',
  'pages/order/order.ts',
  'pages/order-detail/order-detail.ts',
  'pages/delivery/delivery.ts',
  'pages/review/review.ts',
  'pages/login/login.ts',
  'pages/account-setup/account-setup.ts',
  'pages/ai/ai.ts',
  'pages/chat/chat.ts',
  'pages/member/member.ts',
  'pages/address/address.ts',
  'pages/notice/notice.ts',
  'pages/settings/settings.ts',
  'pages/complaint/complaint.ts',
  'pages/m-home/m-home.ts',
  'pages/m-orders/m-orders.ts',
  'pages/m-dishes/m-dishes.ts',
  'pages/m-me/m-me.ts',
  'pages/m-stats/m-stats.ts',
  'pages/m-reviews/m-reviews.ts',
  'pages/m-dish-edit/m-dish-edit.ts',
  'pages/m-shop-edit/m-shop-edit.ts',
  'pages/m-combos/m-combos.ts',
  'pages/m-combo-edit/m-combo-edit.ts',
  'pages/r-profile/r-profile.ts',
  'pages/checkout/checkout.ts',
  'pages/combo-detail/combo-detail.ts',
  'pages/r-me/r-me.ts',
  'config/amap.ts',
  'api/plan.ts',
  'pages/assessment/assessment.ts',
  'pages/plan/plan.ts',
  'pages/m-home/m-home.ts',
  'pages/m-dishes/m-dishes.ts',
  'pages/r-tasks/r-tasks.ts',
  'api/delivery.ts',
  'api/driver-feedback.ts',
  'pages/r-task/r-task.ts',
  'pages/r-exception/r-exception.ts',
  'pages/r-stats/r-stats.ts',
  'pages/r-me/r-me.ts',
  'pages/r-reviews/r-reviews.ts',
  'pages/r-feedback/r-feedback.ts'
];

let fail = 0;
for (const f of FILES) {
  const p = path.join(ROOT, f);
  const src = fs.readFileSync(p, 'utf8');
  const out = ts.transpileModule(src, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2017,
      strict: true
    },
    reportDiagnostics: true,
    fileName: f
  });
  const errs = (out.diagnostics || []).filter(d => d.category === ts.DiagnosticCategory.Error);
  if (errs.length) {
    fail += errs.length;
    for (const d of errs) {
      console.log('ERR', f, ts.flattenDiagnosticMessageText(d.messageText, ' '));
    }
  } else {
    console.log('TS OK ', f);
  }
}
console.log('FAILURES:', fail);
process.exit(fail ? 1 : 0);
