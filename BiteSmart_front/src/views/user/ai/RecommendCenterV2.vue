<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { addToCart } from '../../../api/user/cart'
import { aiRecommend } from '../../../api/user/ai'
import { resolveFileUrl } from '../../../utils/fileUrl'
import { merchantLabel } from '../../../utils/display'

const router = useRouter()
const loading = ref(false)
const adding = ref(false)
const result = ref<any>(null)
const mealType = ref<'all' | 'breakfast' | 'lunch' | 'dinner'>('all')
const restrictions = ref('')
const meals = computed((): any[] => result.value?.meals || [])
const summary = computed(() => result.value?.summary || { calories: 0, protein: 0, fat: 0, carbs: 0 })
const candidates = computed(() => result.value?.candidates || [])
const mealLabels: Record<string, string> = { breakfast: '早餐', lunch: '午餐', dinner: '晚餐' }
const candidateOptions = (currentId: number) => candidates.value.filter((item: any) => item.dishId !== currentId)
// 无图菜品用首字徽章占位，不再回退到无关素材图
const imageUrlFor = (item: any) => resolveFileUrl(item?.dishImage) || ''

const generate = async () => {
  loading.value = true
  try {
    const response = await aiRecommend({ mealType: mealType.value, dietaryRestrictions: restrictions.value.trim() || undefined })
    result.value = response.data
    if (result.value?.success === false) ElMessage.warning(result.value.message || '请先完善健康档案')
  } catch (error: any) {
    ElMessage.error(error?.message || '生成食谱失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const replaceDish = (meal: any, item: any, dishId: number) => {
  const next = candidates.value.find((candidate: any) => candidate.dishId === dishId)
  if (!next) return
  const old = { ...item }
  Object.assign(item, { ...next, mealType: meal.mealType })
  result.value.summary.calories = Number(result.value.summary.calories || 0) + Number(next.calories || 0) - Number(old.calories || 0)
  result.value.summary.protein = Number(result.value.summary.protein || 0) + Number(next.protein || 0) - Number(old.protein || 0)
  result.value.summary.fat = Number(result.value.summary.fat || 0) + Number(next.fat || 0) - Number(old.fat || 0)
  result.value.summary.carbs = Number(result.value.summary.carbs || 0) + Number(next.carbs || 0) - Number(old.carbs || 0)
  ElMessage.success(`已将菜品替换为 ${next.dishName}`)
}

const addRecipeToCart = async () => {
  const items = meals.value.flatMap((meal: any) => meal.items || []).filter((item: any) => item.available !== false)
  if (!items.length) return ElMessage.warning('当前没有可加入购物车的菜品')
  adding.value = true
  let success = 0
  const merchants = new Set<string>()
  try {
    for (const item of items) {
      await addToCart(10, item.dishId, undefined, Number(item.quantity || 1))
      merchants.add(String(item.merchantId))
      success += 1
    }
    ElMessage.success(`已加入 ${success} 道菜，来自 ${merchants.size} 家商户；结算时请按商户分别下单`)
  } catch (error: any) {
    ElMessage.error(error?.message || `已加入 ${success} 道菜，部分菜品加入失败`)
  } finally {
    adding.value = false
  }
}
</script>

<template>
  <div class="recipe-page" :class="{ 'is-empty': !result }">
    <section class="recipe-hero"><div><span class="eyebrow">BiteSmart AI</span><h1>把今天吃什么，交给一份靠谱的计划</h1><p>从平台真实在售菜品中，为你的健康目标搭配早餐、午餐和晚餐。</p></div><div class="hero-mark">AI<br><small>MEAL PLAN</small></div></section>
    <section class="control-panel"><div class="control-copy"><strong>生成你的今日食谱</strong><span>菜品、库存和营养数据均来自平台当前数据</span></div><div class="controls"><el-select v-model="mealType" class="meal-select" aria-label="选择餐次"><el-option label="整日三餐" value="all"/><el-option label="只看早餐" value="breakfast"/><el-option label="只看午餐" value="lunch"/><el-option label="只看晚餐" value="dinner"/></el-select><el-input v-model="restrictions" clearable placeholder="饮食限制或忌口，如低糖、不吃海鲜"/><el-button type="primary" :loading="loading" @click="generate">生成食谱</el-button></div></section>
    <el-alert v-if="result?.success === false" type="warning" :closable="true" class="profile-alert"><template #title><span>{{ result.message }}</span><el-button type="warning" size="small" class="profile-action" @click="router.push('/user/profile')">去完善健康档案</el-button></template></el-alert>
    <div v-loading="loading" class="recipe-layout"><main class="meal-column"><div v-if="!result && !loading" class="empty-state"><div class="empty-icon">✦</div><h2>先告诉我你的饮食偏好</h2><p>生成后，你会看到按餐次整理好的真实菜品和营养数据。</p></div><template v-else><section v-for="(meal, index) in meals" :key="meal.mealType" class="meal-section"><div class="meal-heading"><div><span class="meal-index">0{{ index + 1 }}</span><h2>{{ mealLabels[meal.mealType] || meal.mealType }}</h2></div><span>{{ meal.items?.length || 0 }} 道菜</span></div><div v-if="!meal.items?.length" class="meal-empty">当前条件下没有找到合适菜品</div><article v-for="item in meal.items" :key="item.dishId" class="dish-card"><img v-if="imageUrlFor(item)" :src="imageUrlFor(item)" :alt="item.dishName"><span v-else class="img-placeholder">{{ (item.dishName || '菜')[0] }}</span><div class="dish-main"><div class="dish-title"><h3>{{ item.dishName }}</h3><span>¥{{ item.price || 0 }}</span></div><p>{{ merchantLabel(item.shopName, item.merchantId) }} · {{ item.quantity || 1 }} 份 · 库存 {{ item.stock || 0 }}</p><div class="nutrient-line"><span>{{ item.calories || 0 }} kcal</span><span>蛋白质 {{ item.protein || 0 }}g</span><span>脂肪 {{ item.fat || 0 }}g</span><span>碳水 {{ item.carbs || 0 }}g</span></div></div><div class="dish-actions"><el-select :model-value="item.dishId" size="small" @change="(id: number) => replaceDish(meal, item, id)"><el-option :label="item.dishName" :value="item.dishId"/><el-option v-for="candidate in candidateOptions(item.dishId)" :key="candidate.dishId" :label="candidate.dishName" :value="candidate.dishId"/></el-select></div></article></section><div class="advice"><strong>搭配说明</strong><span>{{ result.advice }}</span></div></template></main><aside class="summary-column"><section class="summary-panel"><div class="summary-heading"><div><span>DAILY TARGET</span><h2>每日营养</h2></div><el-button type="primary" :loading="adding" @click="addRecipeToCart">全部加入购物车</el-button></div><div class="summary-total"><strong>{{ summary.calories || 0 }}</strong><span>kcal 推荐热量</span></div><div class="target-list"><div><span>热量目标</span><strong>{{ result?.dailyCalories || '-' }} kcal</strong></div><div><span>蛋白质</span><strong>{{ summary.protein || 0 }} / {{ result?.proteinGrams || '-' }}g</strong></div><div><span>脂肪</span><strong>{{ summary.fat || 0 }} / {{ result?.fatGrams || '-' }}g</strong></div><div><span>碳水</span><strong>{{ summary.carbs || 0 }} / {{ result?.carbsGrams || '-' }}g</strong></div></div></section><section class="cart-note"><span class="note-icon">i</span><p>食谱可能来自多个商家。加入购物车后，结算时会按商家分别填写留言、地址并下单。</p></section></aside></div>
  </div>
</template>

<style scoped>
.recipe-page{max-width:1180px;margin:0 auto;padding:48px 24px 80px;color:#253129}.recipe-hero{display:flex;justify-content:space-between;align-items:flex-end;border-bottom:1px solid #dfe7df;padding:16px 0 34px}.eyebrow{color:var(--orange);font-size:11px;font-weight:800;letter-spacing:.18em}.recipe-hero h1{max-width:640px;margin:13px 0 10px;font-family:"Source Han Serif SC","Songti SC",serif;font-size:38px;line-height:1.2;font-weight:600}.recipe-hero p{margin:0;color:#718078;font-size:14px}.hero-mark{color:var(--green-ink);font-family:Georgia,serif;font-size:52px;line-height:.76;text-align:right}.hero-mark small{font-family:inherit;font-size:8px;letter-spacing:.2em}.control-panel{display:flex;align-items:center;justify-content:space-between;gap:24px;padding:22px 0;border-bottom:1px solid #dfe7df}.control-copy{display:flex;flex-direction:column;gap:6px;white-space:nowrap}.control-copy strong{font-size:15px}.control-copy span{color:#8a968e;font-size:11px}.controls{display:flex;gap:10px;width:min(690px,100%)}.meal-select{width:132px}.controls .el-input{flex:1}.recipe-layout{display:grid;grid-template-columns:minmax(0,1fr) 310px;gap:44px;padding-top:30px}.meal-section{margin-bottom:30px}.meal-heading{display:flex;justify-content:space-between;align-items:end;margin-bottom:10px}.meal-heading>div{display:flex;align-items:baseline;gap:12px}.meal-heading h2{margin:0;font-family:"Source Han Serif SC","Songti SC",serif;font-size:24px}.meal-heading>span{color:#8a968e;font-size:11px}.meal-index{color:var(--orange);font-family:Georgia,serif;font-size:13px}.dish-card{display:grid;grid-template-columns:92px minmax(0,1fr) 132px;align-items:center;gap:16px;padding:12px 0;border-top:1px solid #edf1ec}.dish-card>img{width:92px;height:76px;object-fit:cover;background:#eef2ed}.dish-main{min-width:0}.dish-title{display:flex;justify-content:space-between;gap:12px}.dish-title h3{overflow:hidden;margin:0;font-size:15px;text-overflow:ellipsis;white-space:nowrap}.dish-title span{color:var(--green);font-weight:700}.dish-main p{margin:7px 0;color:#8a968e;font-size:11px}.nutrient-line{display:flex;flex-wrap:wrap;gap:10px;color:#65746b;font-size:11px}.dish-actions .el-select{width:132px}.meal-empty{padding:20px 0;color:#a0aaa3;font-size:12px}.summary-column{position:relative}.summary-panel{position:sticky;top:24px;padding:22px;background:#f4f8f3;border:1px solid #d6e4d8}.summary-heading{display:flex;align-items:start;justify-content:space-between;gap:10px}.summary-heading span{color:var(--orange);font-size:9px;letter-spacing:.14em}.summary-heading h2{margin:6px 0 0;font-family:"Source Han Serif SC","Songti SC",serif;font-size:21px}.summary-heading .el-button{min-width:112px}.summary-total{display:flex;align-items:baseline;gap:8px;margin:30px 0 22px}.summary-total strong{color:var(--green);font-size:36px;font-family:Georgia,serif}.summary-total span{color:#718078;font-size:11px}.target-list{border-top:1px solid #d6e4d8}.target-list div{display:flex;justify-content:space-between;padding:12px 0;border-bottom:1px solid #d6e4d8;color:#718078;font-size:11px}.target-list strong{color:#253129}.cart-note{display:flex;gap:10px;margin-top:16px;color:#8a968e}.cart-note p{margin:0;font-size:11px;line-height:1.7}.note-icon{display:grid;place-items:center;width:18px;height:18px;flex:0 0 18px;border:1px solid #b9cbbb;border-radius:50%;color:var(--green);font-size:11px}.advice{display:flex;gap:14px;padding:16px;background:#fbfaf6;border-left:2px solid var(--orange);color:#718078;font-size:12px;line-height:1.7}.advice strong{color:#253129;white-space:nowrap}.empty-state{text-align:center;padding:110px 20px;color:#718078}.empty-icon{color:var(--orange);font-size:28px}.empty-state h2{margin:15px 0 8px;color:#253129;font-family:"Source Han Serif SC","Songti SC",serif}.empty-state p{margin:0;font-size:13px}.profile-alert{margin-top:18px}.profile-alert .el-button{margin-left:14px}@media(max-width:900px){.recipe-layout{grid-template-columns:1fr}.summary-panel{position:static}.control-panel{align-items:stretch;flex-direction:column}.controls{width:100%}}@media(max-width:600px){.recipe-page{padding:30px 16px 60px}.recipe-hero h1{font-size:30px}.hero-mark{font-size:34px}.controls{flex-wrap:wrap}.meal-select,.controls .el-input{width:100%}.dish-card{grid-template-columns:64px minmax(0,1fr)}.dish-card>img{width:64px;height:64px}.dish-actions{grid-column:2}.dish-actions .el-select{width:100%}.dish-title h3{font-size:13px}.nutrient-line{gap:6px;font-size:10px}}
</style>
<style scoped>
.profile-alert :deep(.el-alert__title){display:flex;align-items:center;justify-content:space-between;gap:18px;width:100%}
.profile-alert .profile-action{flex:0 0 auto;margin-left:auto;padding:8px 14px;border-color:var(--orange);background:var(--orange);color:#fff;font-weight:700}
.profile-alert .profile-action:hover{border-color:var(--orange);background:var(--orange);color:#fff}
@media(max-width:600px){.profile-alert :deep(.el-alert__title){align-items:flex-start;flex-direction:column;gap:10px}.profile-alert .profile-action{margin-left:0}}
</style>
<style scoped>
.summary-total strong,.target-list strong{color:var(--green)!important}
</style>
<style scoped>
.recipe-page{max-width:1280px;padding:38px 28px 72px;background:#f8faf7}
.recipe-hero{padding:14px 0 28px}
.recipe-hero h1{font-size:34px;letter-spacing:0}
.recipe-hero p{font-size:13px}
.hero-mark{font-size:42px;opacity:.72}
.control-panel{margin-top:20px;padding:18px 20px;border:1px solid #e1e9e2;background:#fff}
.control-copy strong{font-size:14px}
.controls{width:min(700px,100%)}
.controls :deep(.el-input__wrapper),.meal-select :deep(.el-select__wrapper){box-shadow:0 0 0 1px #dce6df inset}
.controls .el-button{min-width:104px}
.recipe-layout{gap:28px;padding-top:24px}
.empty-state{min-height:350px;padding:86px 20px;background:#fff;border:1px solid #e1e9e2}
.empty-state h2{font-size:24px}
.empty-state p{color:#8a968e}
.summary-panel{background:#fff;border-color:#dce7de}
.summary-heading .el-button{font-size:12px}


@media(max-width:600px){.recipe-page{padding:24px 16px 56px}.recipe-hero{padding-bottom:20px}.hero-mark{display:none}.control-panel{padding:16px}.empty-state{min-height:280px;padding:70px 16px}.empty-state h2{font-size:21px}}
</style>
<style scoped>
.dish-card>.img-placeholder{display:flex;align-items:center;justify-content:center;width:92px;height:76px;background:var(--green-soft);color:var(--green-deep);font-family:"Source Han Serif SC","Songti SC",serif;font-size:24px;font-weight:600}
</style>
