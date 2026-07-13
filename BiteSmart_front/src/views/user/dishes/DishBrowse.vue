<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDishList, getDishDetail } from '../../../api/user/dishes'
import { addToCart } from '../../../api/user/cart'
import { ElMessage } from 'element-plus'
import { resolveFileUrl } from '../../../utils/fileUrl'

const loading = ref(false)
const dishes = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const detailVisible = ref(false)
const detailData = ref<any>(null)
const fallbackImages = ['/images/home/salad-card.jpg', '/images/home/meal-card.jpg', '/images/home/hero-fresh-bowl.jpg']
const imageFor = (dish: any, index: number) => resolveFileUrl(dish.dishImage) || fallbackImages[index % fallbackImages.length]

const fetchDishes = async () => {
  loading.value = true
  try {
    const res = await getDishList({ page: currentPage.value, size: pageSize.value })
    dishes.value = res.data?.data?.list || []
    total.value = res.data?.data?.total || 0
  } catch { ElMessage.error('获取菜品列表失败') } finally { loading.value = false }
}
const handleViewDetail = async (row: any) => {
  try { const res = await getDishDetail(row.id); detailData.value = res.data || row; detailVisible.value = true } catch { ElMessage.error('获取菜品详情失败') }
}
const handleAddToCart = async (row: any) => {
  try { await addToCart(10, row.id, undefined, 1); ElMessage.success('已加入购物车') } catch { ElMessage.error('加入购物车失败') }
}
onMounted(fetchDishes)
</script>

<template>
  <div class="dish-page">
    <header class="browse-intro"><span>菜品</span><h1>今天想吃点什么？</h1><p>新鲜搭配，按你的节奏吃好每一餐。</p></header>
    <div v-loading="loading" class="dish-grid">
      <article v-for="(dish, index) in dishes" :key="dish.id" class="dish-card">
        <button class="dish-image" @click="handleViewDetail(dish)"><img :src="imageFor(dish, index)" :alt="dish.dishName" /><span>{{ dish.calories || 0 }} kcal</span></button>
        <div class="dish-copy"><div class="dish-top"><h2>{{ dish.dishName }}</h2><strong>¥{{ dish.price }}</strong></div><p>{{ dish.description || '新鲜食材搭配，轻松吃得更均衡。' }}</p><div class="dish-bottom"><small>蛋白质 {{ dish.protein || 0 }}g</small><button @click="handleAddToCart(dish)">加入 <span>+</span></button></div></div>
      </article>
      <div v-if="!loading && !dishes.length" class="dish-empty"><img src="/images/home/salad-card.jpg" alt="健康餐食" /><div><h2>菜品正在准备</h2><p>稍后再来看看今日的新鲜搭配。</p></div></div>
    </div>
    <div class="pager"><el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="prev, pager, next" @current-change="fetchDishes" /></div>
  <el-dialog v-model="detailVisible" title="菜品详情" width="560px"><template v-if="detailData"><div class="dish-detail"><el-image v-if="detailData.dishImage" :src="resolveFileUrl(detailData.dishImage)" fit="cover" class="dish-image" /><div><h3>{{ detailData.dishName }}</h3><p>{{ detailData.description || '暂无描述' }}</p><strong>¥{{ detailData.price }}</strong></div></div><el-descriptions :column="2" border style="margin-top:18px"><el-descriptions-item label="热量">{{ detailData.calories || 0 }} kcal</el-descriptions-item><el-descriptions-item label="蛋白质">{{ detailData.protein || 0 }} g</el-descriptions-item><el-descriptions-item label="脂肪">{{ detailData.fat || 0 }} g</el-descriptions-item><el-descriptions-item label="碳水">{{ detailData.carbs || 0 }} g</el-descriptions-item></el-descriptions></template><template #footer><el-button @click="detailVisible = false">关闭</el-button><el-button type="primary" @click="handleAddToCart(detailData)">加入购物车</el-button></template></el-dialog>
  </div>
</template>

<style scoped>
.dish-page{max-width:1192px;margin:0 auto;padding:66px 24px 90px;color:#1f2a24}.browse-intro{margin-bottom:38px}.browse-intro span{display:block;color:#cf704f;font-size:12px;font-weight:650;letter-spacing:.14em}.browse-intro h1{margin:12px 0 8px;font-family:"Source Han Serif SC","Noto Serif CJK SC","Songti SC",serif;font-size:38px;font-weight:600}.browse-intro p{margin:0;color:#6c786f;font-size:14px}.dish-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:30px 22px;min-height:300px}.dish-card{background:#fff;border-bottom:1px solid #dfe7df}.dish-image{display:block;position:relative;width:100%;height:230px;padding:0;border:0;background:#eef2ed;overflow:hidden;cursor:pointer}.dish-image img{width:100%;height:100%;object-fit:cover;transition:transform .35s ease}.dish-card:hover .dish-image img{transform:scale(1.04)}.dish-image span{position:absolute;left:13px;bottom:12px;padding:5px 8px;background:#fff;color:#1f4d3a;font-size:11px}.dish-copy{padding:16px 2px 15px}.dish-top{display:flex;justify-content:space-between;gap:12px}.dish-top h2{margin:0;font-size:18px;font-weight:650}.dish-top strong{color:#1f4d3a;font-size:17px}.dish-copy p{min-height:38px;margin:9px 0;color:#718078;font-size:12px;line-height:1.6}.dish-bottom{display:flex;align-items:center;justify-content:space-between;padding-top:12px;border-top:1px solid #edf1ec}.dish-bottom small{color:#78857d;font-size:11px}.dish-bottom button{border:0;background:none;color:#1f4d3a;font-weight:650;cursor:pointer}.dish-bottom button span{margin-left:5px;color:#cf704f;font-size:18px;vertical-align:-1px}.dish-empty{grid-column:1/-1;display:flex;align-items:center;justify-content:center;gap:26px;min-height:280px;background:#eff3ee}.dish-empty img{width:145px;height:145px;object-fit:cover}.dish-empty h2{margin:0 0 8px;font-family:"Source Han Serif SC","Songti SC",serif}.dish-empty p{margin:0;color:#718078;font-size:13px}.pager{display:flex;justify-content:center;margin-top:46px}.dish-detail{display:flex;gap:18px}.dish-detail .dish-image{width:150px;height:120px;flex-shrink:0;cursor:default}.dish-detail h3{margin:0 0 10px}.dish-detail p{color:#6c786f;line-height:1.6}.dish-detail strong{font-size:20px;color:#1f4d3a}@media(max-width:800px){.dish-page{padding:45px 18px 68px}.dish-grid{grid-template-columns:repeat(2,1fr)}.dish-image{height:210px}}@media(max-width:540px){.browse-intro h1{font-size:31px}.dish-grid{grid-template-columns:1fr}.dish-image{height:250px}}
</style>
