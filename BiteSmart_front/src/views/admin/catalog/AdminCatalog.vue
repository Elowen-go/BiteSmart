<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Refresh } from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'
import { getAdminDishList, addAdminDish, updateAdminDish, deleteAdminDish } from '../../../api/admin/dishes'
import { getAdminComboList, addAdminCombo, updateAdminCombo, deleteAdminCombo } from '../../../api/admin/combos'
import { getMerchantList } from '../../../api/admin/merchants'
import { getDishCategories } from '../../../api/user/dishes'
import { resolveFileUrl } from '../../../utils/fileUrl'
import ListState from '../../../components/common/ListState.vue'

const route = useRoute()
const isCombo = computed(() => route.path.endsWith('/combos'))
const loading = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const merchants = ref<any[]>([])
const categories = ref<any[]>([])
const dialog = ref(false)
const editingId = ref<number | null>(null)
const form = ref<any>({})
const loadError = ref('')
const imageUrl = (value: string) => resolveFileUrl(value) || ''

const load = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const res: any = isCombo.value ? await getAdminComboList({ page: page.value, size: size.value }) : await getAdminDishList({ page: page.value, size: size.value })
    const data = res.data?.data || res.data || {}
    rows.value = (data.list || []).map((item: any) => ({ ...item, dishImage: imageUrl(item.dishImage), comboImage: imageUrl(item.comboImage) }))
    total.value = Number(data.total || 0)
  } catch (error: any) { loadError.value = error?.message || '商品列表暂时无法获取'; ElMessage.error(loadError.value) } finally { loading.value = false }
}
const loadOptions = async () => {
  try {
    const [merchantRes, categoryRes] = await Promise.all([getMerchantList({ pageNum: 1, pageSize: 100 }), getDishCategories()])
    merchants.value = merchantRes.data?.list || merchantRes.data?.data?.list || merchantRes.data || []
    categories.value = categoryRes.data || []
  } catch {}
}
const openAdd = () => { editingId.value = null; form.value = isCombo.value ? { merchantId: merchants.value[0]?.id, comboName: '', price: 0, description: '', comboType: 10, status: 10, dishItems: '' } : { merchantId: merchants.value[0]?.id, dishName: '', categoryId: categories.value[0]?.id, price: 0, stock: 0, status: 10, description: '', calories: 0, protein: 0, fat: 0, carbs: 0 }; dialog.value = true }
const openEdit = (row: any) => { editingId.value = row.id; form.value = { ...row, dishItems: '' }; dialog.value = true }
const save = async () => {
  try {
    let res: any
    if (isCombo.value) {
      const dishItemsText = String(form.value.dishItems || '').trim()
      const { dishItems: _dishItems, ...combo } = form.value
      const data: any = { combo }
      if (dishItemsText) data.dishItems = dishItemsText.split(',').map((id: string) => id.trim()).filter(Boolean).map((dishId: string) => ({ dishId, quantity: 1, isFixed: 1 }))
      res = editingId.value ? await updateAdminCombo(editingId.value, data) : await addAdminCombo(data)
    } else res = editingId.value ? await updateAdminDish(editingId.value, form.value) : await addAdminDish(form.value)
    if (res.code !== 200) throw new Error(res.message)
    ElMessage.success(editingId.value ? '更新成功' : '新增成功'); dialog.value = false; load()
  } catch (e: any) { ElMessage.error(e?.message || '保存失败') }
}
const remove = async (row: any) => { try { await ElMessageBox.confirm(`确定删除「${isCombo.value ? row.comboName : row.dishName}」吗？`, '删除确认', { type: 'warning' }); const res: any = isCombo.value ? await deleteAdminCombo(row.id) : await deleteAdminDish(row.id); if (res.code !== 200) throw new Error(res.message); ElMessage.success('删除成功'); load() } catch (e: any) { if (e?.message) ElMessage.error(e.message) } }
const toggle = async (row: any) => { const data = { ...row, status: Number(row.status) === 10 ? 20 : 10 }; const res: any = isCombo.value ? await updateAdminCombo(row.id, { combo: data }) : await updateAdminDish(row.id, data); if (res.code === 200) { ElMessage.success(data.status === 10 ? '已上架' : '已下架'); load() } }
watch(() => route.path, () => { page.value = 1; load() })
onMounted(() => { loadOptions(); load() })
</script>

<template>
  <div class="catalog-page"><div class="page-head"><div><h2>{{ isCombo ? '套餐管理' : '菜品管理' }}</h2><p>管理员统一维护平台商品，跨商家查看与管理。</p></div><div><el-button :icon="Refresh" @click="load">刷新</el-button><el-button type="primary" :icon="Plus" @click="openAdd">新增{{ isCombo ? '套餐' : '菜品' }}</el-button></div></div>
    <div class="catalog-panel"><ListState :loading="loading" :error="loadError" :empty="!rows.length" empty-text="暂无商品数据" @retry="load"><el-table :data="rows"><el-table-column label="图片" width="82"><template #default="{ row }"><div class="table-image"><img v-if="row.dishImage || row.comboImage" :src="row.dishImage || row.comboImage" alt="商品图片" /><span v-else>{{ (row.dishName || row.comboName || '商')[0] }}</span></div></template></el-table-column><el-table-column v-if="!isCombo" prop="dishName" label="菜品名称" min-width="220" /><el-table-column v-else prop="comboName" label="套餐名称" min-width="220" /><el-table-column prop="merchantId" label="商家 ID" width="170" /><el-table-column v-if="!isCombo" label="营养" min-width="180"><template #default="{ row }">{{ row.calories || 0 }} kcal · 蛋白 {{ row.protein || 0 }}g<br /><small>脂肪 {{ row.fat || 0 }}g · 碳水 {{ row.carbs || 0 }}g</small></template></el-table-column><el-table-column v-else label="营养" min-width="180"><template #default="{ row }">{{ row.totalCalories || 0 }} kcal · 蛋白 {{ row.totalProtein || 0 }}g<br /><small>脂肪 {{ row.totalFat || 0 }}g · 碳水 {{ row.totalCarbs || 0 }}g</small></template></el-table-column><el-table-column label="价格" width="130"><template #default="{ row }">¥{{ row.price || 0 }}</template></el-table-column><el-table-column label="状态" width="110"><template #default="{ row }"><el-tag :type="Number(row.status) === 10 ? 'success' : 'info'">{{ Number(row.status) === 10 ? '上架' : '下架' }}</el-tag></template></el-table-column><el-table-column label="操作" width="270" fixed="right"><template #default="{ row }"><el-button size="small" :icon="Edit" @click="openEdit(row)">编辑</el-button><el-button size="small" @click="toggle(row)">{{ Number(row.status) === 10 ? '下架' : '上架' }}</el-button><el-button size="small" type="danger" :icon="Delete" @click="remove(row)">删除</el-button></template></el-table-column></el-table></ListState><div class="pager"><el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" @current-change="load" @size-change="load" /></div></div>
    <el-dialog v-model="dialog" :title="`${editingId ? '编辑' : '新增'}${isCombo ? '套餐' : '菜品'}`" width="600px"><el-form :model="form" label-width="90px"><el-form-item label="所属商家"><el-select v-model="form.merchantId" style="width:100%"><el-option v-for="item in merchants" :key="item.id" :label="`${item.shopName || '商家'}（${item.id}）`" :value="item.id" /></el-select></el-form-item><template v-if="isCombo"><el-form-item label="套餐名称"><el-input v-model="form.comboName" /></el-form-item><el-form-item label="套餐类型"><el-select v-model="form.comboType"><el-option :value="10" label="减脂"/><el-option :value="20" label="增肌"/><el-option :value="30" label="控糖"/></el-select></el-form-item><el-form-item label="价格"><el-input-number v-model="form.price" :min="0" /></el-form-item><el-form-item label="关联菜品"><el-input v-model="form.dishItems" placeholder="填写菜品 ID，多个用英文逗号分隔" /></el-form-item></template><template v-else><el-form-item label="菜品名称"><el-input v-model="form.dishName" /></el-form-item><el-form-item label="分类"><el-select v-model="form.categoryId"><el-option v-for="item in categories" :key="item.id" :label="item.categoryName" :value="item.id" /></el-select></el-form-item><el-form-item label="价格"><el-input-number v-model="form.price" :min="0" /></el-form-item><el-form-item label="库存"><el-input-number v-model="form.stock" :min="0" /></el-form-item><el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item></template></el-form><template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template></el-dialog>
  </div>
</template>

<style scoped>.catalog-page{padding:4px}.page-head{display:flex;align-items:center;justify-content:space-between;gap:20px;margin-bottom:18px}.page-head h2{margin:0;font-size:22px;color:var(--bs-text-title)}.page-head p{margin:6px 0 0;color:var(--bs-text-muted);font-size:13px}.catalog-panel{padding:18px;background:#fff;border:1px solid var(--bs-border-light);border-radius:8px;box-shadow:var(--bs-card-shadow)}.table-image{display:grid;place-items:center;width:48px;height:48px;background:#eef4ef;color:#5e8069;font-size:16px}.table-image img{width:100%;height:100%;object-fit:cover}.catalog-panel small{color:#87928a;font-size:11px}.pager{display:flex;justify-content:flex-end;margin-top:18px}@media(max-width:700px){.page-head{align-items:flex-start;flex-direction:column}.page-head>div:last-child{width:100%;display:flex}.page-head .el-button{flex:1}}
</style>
