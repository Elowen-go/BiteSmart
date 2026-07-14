<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDishCategories, getDishList, getDishDetail } from '../../../api/user/dishes'
import { addToCart } from '../../../api/user/cart'
import { ElMessage } from 'element-plus'
import { Plus, Check } from '@element-plus/icons-vue'
import { resolveFileUrl } from '../../../utils/fileUrl'

const loading = ref(false)
const dishes = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const categoryId = ref<number | undefined>()
const sort = ref('latest')
const categories = ref<any[]>([])
const detailVisible = ref(false)
const detailData = ref<any>(null)
const addingDishId = ref<number | null>(null)
const addedDishId = ref<number | null>(null)
const fallbackImages = ['/images/home/salad-card.jpg', '/images/home/meal-card.jpg', '/images/home/hero-fresh-bowl.jpg']
const imageFor = (dish: any, index: number) => resolveFileUrl(dish.dishImage) || fallbackImages[index % fallbackImages.length]

const fetchDishes = async () => {
  loading.value = true
  try {
    const res = await getDishList({ keyword: keyword.value || undefined, categoryId: categoryId.value, sort: sort.value, page: currentPage.value, size: pageSize.value })
    dishes.value = res.data?.data?.list || []
    total.value = res.data?.data?.total || 0
  } catch { ElMessage.error('获取菜品列表失败') } finally { loading.value = false }
}
const handleViewDetail = async (row: any) => {
  try { const res = await getDishDetail(row.id); detailData.value = res.data || row; detailVisible.value = true } catch { ElMessage.error('获取菜品详情失败') }
}
const handleAddToCart = async (row: any) => {
  if (addingDishId.value === row.id) return
  addingDishId.value = row.id
  try {
    await addToCart(10, row.id, undefined, 1)
    addedDishId.value = row.id
    ElMessage.success('已加入购物车')
    window.setTimeout(() => { if (addedDishId.value === row.id) addedDishId.value = null }, 1400)
  } catch { ElMessage.error('加入购物车失败') } finally { addingDishId.value = null }
}
const applyFilters = () => { currentPage.value = 1; fetchDishes() }
onMounted(async () => { try { const res = await getDishCategories(); categories.value = res.data || [] } catch {} await fetchDishes() })
</script>

<template>
  <div class="dish-page">
    <header class="browse-intro"><span>菜品</span><h1>今天想吃点什么？</h1><p>新鲜搭配，按你的节奏吃好每一餐。</p></header>
    <div class="browse-filters"><el-input v-model="keyword" class="filter-search" clearable placeholder="搜索菜品" @keyup.enter="applyFilters" @clear="applyFilters" /><el-select v-model="categoryId" clearable placeholder="全部分类" @change="applyFilters"><el-option v-for="category in categories" :key="category.id" :label="category.categoryName" :value="category.id" /></el-select><el-select v-model="sort" @change="applyFilters"><el-option label="最新上架" value="latest" /><el-option label="价格从低到高" value="priceAsc" /><el-option label="价格从高到低" value="priceDesc" /><el-option label="热量最低" value="calories" /><el-option label="蛋白质最高" value="protein" /><el-option label="销量优先" value="sales" /></el-select><el-button type="primary" @click="applyFilters">搜索</el-button></div>
    <div v-loading="loading" class="dish-grid">
      <article v-for="(dish, index) in dishes" :key="dish.id" class="dish-card">
        <button class="dish-image" @click="handleViewDetail(dish)"><img :src="imageFor(dish, index)" :alt="dish.dishName" /><span>{{ dish.calories || 0 }} kcal</span></button>
        <div class="dish-copy"><div class="dish-top"><h2>{{ dish.dishName }}</h2><strong>¥{{ dish.price }}</strong></div><p>{{ dish.description || '新鲜食材搭配，轻松吃得更均衡。' }}</p><div class="dish-bottom"><small>蛋白质 {{ dish.protein || 0 }}g</small><button class="add-button" :class="{ added: addedDishId === dish.id }" :disabled="addingDishId === dish.id" :aria-label="addedDishId === dish.id ? '已加入购物车' : '加入购物车'" @click.stop="handleAddToCart(dish)"><Check v-if="addedDishId === dish.id" /><Plus v-else /><span>{{ addedDishId === dish.id ? '已加入' : '加入' }}</span></button></div></div>
      </article>
      <div v-if="!loading && !dishes.length" class="dish-empty"><img src="/images/home/salad-card.jpg" alt="健康餐食" /><div><h2>菜品正在准备</h2><p>稍后再来看看今日的新鲜搭配。</p></div></div>
    </div>
    <div class="pager"><el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="prev, pager, next" @current-change="fetchDishes" /></div>
  <el-dialog v-model="detailVisible" class="dish-detail-dialog" width="680px" :show-close="true">
    <template v-if="detailData">
      <div class="dish-detail-hero">
        <el-image v-if="detailData.dishImage" :src="resolveFileUrl(detailData.dishImage)" fit="cover" class="dish-detail-cover" />
        <div class="dish-detail-info">
          <span class="detail-eyebrow">精选菜品</span>
          <h2>{{ detailData.dishName }}</h2>
          <p>{{ detailData.description || '新鲜食材搭配，轻松吃得更均衡。' }}</p>
          <strong class="detail-price">¥{{ detailData.price }}</strong>
        </div>
      </div>
      <div class="nutrition-heading"><span>营养信息</span><small>每份估算值</small></div>
      <div class="nutrition-grid">
        <div><span>热量</span><strong>{{ detailData.calories || 0 }}<small>kcal</small></strong></div>
        <div><span>蛋白质</span><strong>{{ detailData.protein || 0 }}<small>g</small></strong></div>
        <div><span>脂肪</span><strong>{{ detailData.fat || 0 }}<small>g</small></strong></div>
        <div><span>碳水</span><strong>{{ detailData.carbs || 0 }}<small>g</small></strong></div>
      </div>
    </template>
    <template #footer>
      <div class="detail-footer"><span>喜欢这道菜？加入购物车随时享用。</span><div><el-button @click="detailVisible = false">稍后再看</el-button><el-button type="primary" @click="handleAddToCart(detailData)">加入购物车</el-button></div></div>
    </template>
  </el-dialog>
  </div>
</template>

<style scoped>
.dish-page{max-width:1192px;margin:0 auto;padding:66px 24px 90px;color:#1f2a24}.browse-intro{margin-bottom:38px}.browse-intro span{display:block;color:#cf704f;font-size:12px;font-weight:650;letter-spacing:.14em}.browse-intro h1{margin:12px 0 8px;font-family:"Source Han Serif SC","Noto Serif CJK SC","Songti SC",serif;font-size:38px;font-weight:600}.browse-intro p{margin:0;color:#6c786f;font-size:14px}.dish-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:30px 22px;min-height:300px}.dish-card{background:#fff;border-bottom:1px solid #dfe7df}.dish-image{display:block;position:relative;width:100%;height:230px;padding:0;border:0;background:#eef2ed;overflow:hidden;cursor:pointer}.dish-image img{width:100%;height:100%;object-fit:cover;transition:transform .35s ease}.dish-card:hover .dish-image img{transform:scale(1.04)}.dish-image span{position:absolute;left:13px;bottom:12px;padding:5px 8px;background:#fff;color:#1f4d3a;font-size:11px}.dish-copy{padding:16px 2px 15px}.dish-top{display:flex;justify-content:space-between;gap:12px}.dish-top h2{margin:0;font-size:18px;font-weight:650}.dish-top strong{color:#1f4d3a;font-size:17px}.dish-copy p{min-height:38px;margin:9px 0;color:#718078;font-size:12px;line-height:1.6}.dish-bottom{display:flex;align-items:center;justify-content:space-between;padding-top:12px;border-top:1px solid #edf1ec}.dish-bottom small{color:#78857d;font-size:11px}.dish-bottom button{border:0;background:none;color:#1f4d3a;font-weight:650;cursor:pointer}.dish-bottom button span{margin-left:5px;color:#cf704f;font-size:18px;vertical-align:-1px}.dish-empty{grid-column:1/-1;display:flex;align-items:center;justify-content:center;gap:26px;min-height:280px;background:#eff3ee}.dish-empty img{width:145px;height:145px;object-fit:cover}.dish-empty h2{margin:0 0 8px;font-family:"Source Han Serif SC","Songti SC",serif}.dish-empty p{margin:0;color:#718078;font-size:13px}.pager{display:flex;justify-content:center;margin-top:46px}.dish-detail{display:flex;gap:18px}.dish-detail .dish-image{width:150px;height:120px;flex-shrink:0;cursor:default}.dish-detail h3{margin:0 0 10px}.dish-detail p{color:#6c786f;line-height:1.6}.dish-detail strong{font-size:20px;color:#1f4d3a}@media(max-width:800px){.dish-page{padding:45px 18px 68px}.dish-grid{grid-template-columns:repeat(2,1fr)}.dish-image{height:210px}}@media(max-width:540px){.browse-intro h1{font-size:31px}.dish-grid{grid-template-columns:1fr}.dish-image{height:250px}}
.add-button{display:inline-flex;align-items:center;justify-content:center;gap:6px;min-width:76px;height:32px;padding:0 12px;border:1px solid #c9dbce;border-radius:4px;background:#f5f8f4;color:#1f4d3a;font-size:12px;font-weight:700;line-height:1;cursor:pointer;transition:background .2s ease,border-color .2s ease,color .2s ease,transform .2s ease}.add-button svg{width:15px;height:15px}.add-button span{margin-left:0;color:inherit;font-size:inherit;vertical-align:initial}.add-button:hover:not(:disabled){border-color:#1f4d3a;background:#1f4d3a;color:#fff;transform:translateY(-1px)}.add-button:active:not(:disabled){transform:translateY(0)}.add-button:disabled{cursor:wait;opacity:.65}.add-button.added{border-color:#1f4d3a;background:#1f4d3a;color:#fff}
.dish-detail-dialog :deep(.el-dialog__header){margin:0;padding:24px 28px 16px;border-bottom:1px solid #edf1ec}.dish-detail-dialog :deep(.el-dialog__title){color:#1f2a24;font-size:18px;font-weight:700}.dish-detail-dialog :deep(.el-dialog__headerbtn){top:20px;right:22px}.dish-detail-dialog :deep(.el-dialog__body){padding:24px 28px 20px}.dish-detail-dialog :deep(.el-dialog__footer){padding:16px 28px 22px;border-top:1px solid #edf1ec}.dish-detail-hero{display:grid;grid-template-columns:230px 1fr;gap:26px;align-items:center}.dish-detail-cover{width:230px;height:190px;background:#eef2ed}.dish-detail-info{min-width:0}.detail-eyebrow{display:block;margin-bottom:10px;color:#cf704f;font-size:11px;font-weight:700;letter-spacing:.12em}.dish-detail-info h2{margin:0;color:#1f2a24;font-family:"Source Han Serif SC","Noto Serif CJK SC","Songti SC",serif;font-size:26px;line-height:1.25}.dish-detail-info p{margin:13px 0 18px;color:#718078;font-size:13px;line-height:1.8}.detail-price{color:#1f4d3a;font-size:27px;letter-spacing:.02em}.nutrition-heading{display:flex;align-items:baseline;justify-content:space-between;margin:26px 0 10px}.nutrition-heading span{color:#1f2a24;font-size:14px;font-weight:700}.nutrition-heading small{color:#8a968e;font-size:11px}.nutrition-grid{display:grid;grid-template-columns:repeat(4,1fr);border:1px solid #e1e9e2;background:#f8faf8}.nutrition-grid>div{display:flex;flex-direction:column;gap:8px;padding:15px 14px;border-right:1px solid #e1e9e2}.nutrition-grid>div:last-child{border-right:0}.nutrition-grid span{color:#78857d;font-size:11px}.nutrition-grid strong{color:#1f4d3a;font-size:18px}.nutrition-grid strong small{margin-left:3px;color:#8a968e;font-size:10px;font-weight:500}.detail-footer{display:flex;align-items:center;justify-content:space-between;gap:20px}.detail-footer>span{color:#78857d;font-size:12px}.detail-footer .el-button--primary{min-width:116px;background:#1f4d3a;border-color:#1f4d3a}.detail-footer .el-button--primary:hover{background:#2c654e;border-color:#2c654e}@media(max-width:600px){.dish-detail-dialog{width:calc(100% - 28px)!important;margin-top:8vh!important}.dish-detail-dialog :deep(.el-dialog__header){padding:20px 20px 14px}.dish-detail-dialog :deep(.el-dialog__body){padding:18px 20px}.dish-detail-dialog :deep(.el-dialog__footer){padding:14px 20px 18px}.dish-detail-hero{grid-template-columns:1fr;gap:18px}.dish-detail-cover{width:100%;height:190px}.dish-detail-info h2{font-size:23px}.nutrition-grid{grid-template-columns:repeat(2,1fr)}.nutrition-grid>div:nth-child(2){border-right:0}.nutrition-grid>div:nth-child(-n+2){border-bottom:1px solid #e1e9e2}.detail-footer{align-items:stretch;flex-direction:column;gap:12px}.detail-footer>span{display:none}.detail-footer div{display:flex;justify-content:flex-end}.detail-footer .el-button--primary{flex:1}}
.browse-filters{display:flex;align-items:center;gap:10px;margin:-12px 0 30px}.browse-filters .filter-search{max-width:330px}.browse-filters :deep(.el-select){width:150px}.browse-filters .el-button{background:#1f4d3a;border-color:#1f4d3a}@media(max-width:600px){.browse-filters{align-items:stretch;flex-wrap:wrap;margin:-8px 0 24px}.browse-filters .filter-search{max-width:none;width:100%}.browse-filters :deep(.el-select){flex:1;width:auto}.browse-filters .el-button{width:80px}}
</style>
