<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getComboList, getComboDetail, addCombo, updateCombo, deleteCombo, comboTypeOptions, suitableForOptions, type DishItem, type ComboRequest } from '../../../api/merchant/combos'
import { getDishList } from '../../../api/merchant/dishes'

const loading = ref(false)
const comboList = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const editingId = ref<number | null>(null)
const viewDialogVisible = ref(false)
const viewData = ref<any>(null)

// 菜品列表
const dishList = ref<any[]>([])
const dishListLoading = ref(false)

/** 已选菜品（含 isFixed、quantity） */
const selectedDishItems = ref<DishItem[]>([])

/** 可替换菜品池（选中的菜品ID列表） */
const replaceablePoolIds = ref<number[]>([])

/** 根据dishId获取菜品信息 */
const getDishById = (dishId: number) => {
  return dishList.value.find((d: any) => d.id == dishId)
}

/** 已选菜品的详细信息（用于展示） */
const selectedDishDetails = computed(() => {
  return selectedDishItems.value.map(item => {
    const dish = getDishById(item.dishId)
    return {
      ...item,
      dishName: dish ? dish.dishName : `菜品${item.dishId}`,
      price: dish ? dish.price : 0,
      calories: dish ? dish.calories : 0,
      protein: dish ? dish.protein : 0,
      fat: dish ? dish.fat : 0,
      carbs: dish ? dish.carbs : 0
    }
  })
})

/** 自动计算营养数据 */
const autoCalculatedNutrition = computed(() => {
  return selectedDishDetails.value.reduce((total, item) => {
    const qty = item.quantity || 1
    return {
      calories: total.calories + (item.calories || 0) * qty,
      protein: total.protein + (item.protein || 0) * qty,
      fat: total.fat + (item.fat || 0) * qty,
      carbs: total.carbs + (item.carbs || 0) * qty
    }
  }, { calories: 0, protein: 0, fat: 0, carbs: 0 })
})

/** 可选但未选的菜品 */
const availableDishes = computed(() => {
  const selectedIds = new Set(selectedDishItems.value.map(i => i.dishId))
  return dishList.value.filter((d: any) => !selectedIds.has(d.id))
})

const form = ref({
  comboName: '',
  price: 0,
  originalPrice: 0,
  comboType: 10,
  status: 10,
  description: '',
  comboImage: '',
  totalCalories: 0,
  totalProtein: 0,
  totalFat: 0,
  totalCarbs: 0,
  maxReplaceCount: 0,
  suitableFor: ''
})

// 获取菜品列表
const fetchDishList = async () => {
  if (dishListLoading.value) return
  dishListLoading.value = true
  try {
    const res = await getDishList({ page: 1, size: 1000 })
    if (res.code === 200) {
      // 后端返回 {code, message, data: {list, pageNum, ...}}
      dishList.value = res.data?.data?.list || []
    }
  } catch (e) {
    console.error('获取菜品列表失败', e)
  } finally {
    dishListLoading.value = false
  }
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getComboList({ page: page.value, size: size.value })
    if (res.code === 200) {
      comboList.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    console.error('获取套餐列表失败', e)
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  form.value = {
    comboName: '',
    price: 0,
    originalPrice: 0,
    comboType: 10,
    status: 10,
    description: '',
    comboImage: '',
    totalCalories: 0,
    totalProtein: 0,
    totalFat: 0,
    totalCarbs: 0,
    maxReplaceCount: 0,
    suitableFor: null
  }
  selectedDishItems.value = []
  replaceablePoolIds.value = []
}

const handleAdd = () => {
  editingId.value = null
  dialogTitle.value = '新增套餐'
  resetForm()
  dialogVisible.value = true
}

const dishSelectRef = ref()

const addDish = (dishId: number) => {
  if (!dishId) return
  // 检查是否已存在
  const exists = selectedDishItems.value.find(i => i.dishId == dishId)
  if (exists) {
    ElMessage.warning('该菜品已在套餐中')
    // 清空选择
    if (dishSelectRef.value) {
      dishSelectRef.value.handleClear()
    }
    return
  }
  selectedDishItems.value.push({ dishId, quantity: 1, isFixed: 1 })
  // 清空选择，允许再次选择
  if (dishSelectRef.value) {
    dishSelectRef.value.handleClear()
  }
}

const removeDish = (dishId: number) => {
  selectedDishItems.value = selectedDishItems.value.filter(i => i.dishId !== dishId)
  replaceablePoolIds.value = replaceablePoolIds.value.filter(id => id !== dishId)
}

const toggleFixed = (dishId: number) => {
  const item = selectedDishItems.value.find(i => i.dishId === dishId)
  if (item) {
    item.isFixed = item.isFixed === 1 ? 0 : 1
  }
}

const handleEdit = async (row: any) => {
  editingId.value = row.id
  dialogTitle.value = '编辑套餐'
  resetForm()
  
  // 确保菜品列表已加载（如果正在加载中则等待）
  if (dishList.value.length === 0) {
    await fetchDishList()
  }
  // 如果已经在加载中，等待加载完成
  while (dishListLoading.value) {
    await new Promise(resolve => setTimeout(resolve, 100))
  }
  
  try {
    const res = await getComboDetail(row.id)
    if (res.code === 200) {
      const { combo, dishItems } = res.data
      form.value = {
        comboName: combo.comboName,
        price: combo.price,
        originalPrice: combo.originalPrice || 0,
        comboType: combo.comboType || 10,
        status: combo.status,
        description: combo.description || '',
        comboImage: combo.comboImage || '',
        totalCalories: combo.totalCalories || 0,
        totalProtein: combo.totalProtein || 0,
        totalFat: combo.totalFat || 0,
        totalCarbs: combo.totalCarbs || 0,
        maxReplaceCount: combo.maxReplaceCount || 0,
        suitableFor: combo.suitableFor || null
      }
      selectedDishItems.value = dishItems || []
      // 初始化可替换菜品池
      if (combo.replaceableDishPool) {
        try { replaceablePoolIds.value = JSON.parse(combo.replaceableDishPool) } catch(e) { replaceablePoolIds.value = [] }
      }
    }
  } catch (e) {
    console.error('获取套餐详情失败', e)
    ElMessage.error('获取套餐详情失败')
  }
  
  dialogVisible.value = true
}

const handleView = async (row: any) => {
  try {
    const res = await getComboDetail(row.id)
    if (res.code === 200) {
      viewData.value = res.data
      viewDialogVisible.value = true
    } else {
      ElMessage.error('获取套餐详情失败')
    }
  } catch (e) {
    console.error('获取套餐详情失败', e)
    ElMessage.error('获取套餐详情失败')
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定要删除套餐「${row.comboName}」吗？`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await deleteCombo(row.id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        fetchList()
      } else {
        ElMessage.error(res.message || '删除失败')
      }
    } catch (e) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

const handleSubmit = async () => {
  // 表单验证
  if (!form.value.comboName.trim()) {
    ElMessage.warning('请输入套餐名称')
    return
  }
  if (form.value.price <= 0) {
    ElMessage.warning('价格必须大于0')
    return
  }

  loading.value = true
  try {
    const comboRequest: ComboRequest = {
      combo: { 
        ...form.value,
        suitableFor: form.value.suitableFor || null,
        replaceableDishPool: replaceablePoolIds.value.length > 0 ? JSON.stringify(replaceablePoolIds.value) : null
      },
      dishItems: selectedDishItems.value
    }
    
    let res: any
    if (editingId.value) {
      res = await updateCombo(editingId.value, comboRequest)
    } else {
      res = await addCombo(comboRequest)
    }
    if (res.code === 200) {
      ElMessage.success(editingId.value ? '更新成功' : '新增成功')
      dialogVisible.value = false
      fetchList()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  } finally {
    loading.value = false
  }
}

const handlePageChange = (val: number) => {
  page.value = val
  fetchList()
}

const handleSizeChange = (val: number) => {
  size.value = val
  page.value = 1
  fetchList()
}

const getStatusTag = (status: number) => {
  return status === 10 ? 'success' : 'danger'
}

const getStatusLabel = (status: number) => {
  return status === 10 ? '上架' : '下架'
}

const getComboTypeLabel = (type: number) => {
  const option = comboTypeOptions.find(opt => opt.value === type)
  return option ? option.label : '未知'
}

const getImageUrl = (url: string) => {
  if (!url) return ''
  return url.startsWith('http') ? url : `/api/files/download${url}`
}

const getDishName = (dishId: number) => {
  const dish = dishList.value.find(d => d.id == dishId)
  return dish ? dish.dishName : `菜品${dishId}`
}

onMounted(() => {
  fetchList()
  fetchDishList()
})
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>套餐管理</h3>
        <button class="btn btn-primary" @click="handleAdd">添加套餐</button>
      </div>
      <div style="padding-top: 20px;">
        <el-table :data="comboList" border v-loading="loading">
          <el-table-column label="套餐" min-width="200">
            <template #default="{ row }">
              <div class="combo-info">
                <img v-if="row.comboImage" :src="getImageUrl(row.comboImage)" class="combo-image" alt="套餐图片" />
                <div v-else class="combo-image-placeholder">无图</div>
                <div class="combo-name-wrapper">
                  <span class="combo-name">{{ row.comboName }}</span>
                  <el-tag size="small" type="info" class="combo-type-tag">{{ getComboTypeLabel(row.comboType) }}</el-tag>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="price" label="价格" width="120">
            <template #default="{ row }">
              <div class="price-wrapper">
                <span class="current-price">¥{{ row.price }}</span>
                <span v-if="row.originalPrice && row.originalPrice > row.price" class="original-price">¥{{ row.originalPrice }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="totalCalories" label="热量" width="100">
            <template #default="{ row }">
              {{ row.totalCalories || 0 }} kcal
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusTag(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="handleView(row)">查看</el-button>
              <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div style="display: flex; justify-content: flex-end; margin-top: 20px;">
          <el-pagination
            v-if="total > 0"
            :current-page="page"
            :page-size="size"
            :total="total"
            :page-sizes="[5, 10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" destroy-on-close>
      <el-form :model="form" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="套餐名称" required>
              <el-input v-model="form.comboName" placeholder="请输入套餐名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="套餐类型" required>
              <el-select v-model="form.comboType" placeholder="请选择套餐类型" style="width: 100%;">
                <el-option
                  v-for="item in comboTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="价格" required>
              <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="原价">
              <el-input-number v-model="form.originalPrice" :min="0" :precision="2" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="状态" required>
              <el-radio-group v-model="form.status">
                <el-radio :value="10">上架</el-radio>
                <el-radio :value="20">下架</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大替换数量">
              <el-input-number v-model="form.maxReplaceCount" :min="0" :max="10" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="总热量 (kcal)">
              <el-input-number v-model="form.totalCalories" :min="0" :precision="0" style="width: 100%;" />
              <span v-if="autoCalculatedNutrition.calories > 0" class="nutrition-hint">
                自动计算: {{ Math.round(autoCalculatedNutrition.calories) }} kcal
              </span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="蛋白质 (g)">
              <el-input-number v-model="form.totalProtein" :min="0" :precision="2" style="width: 100%;" />
              <span v-if="autoCalculatedNutrition.protein > 0" class="nutrition-hint">
                自动计算: {{ autoCalculatedNutrition.protein.toFixed(2) }} g
              </span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="脂肪 (g)">
              <el-input-number v-model="form.totalFat" :min="0" :precision="2" style="width: 100%;" />
              <span v-if="autoCalculatedNutrition.fat > 0" class="nutrition-hint">
                自动计算: {{ autoCalculatedNutrition.fat.toFixed(2) }} g
              </span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="碳水 (g)">
              <el-input-number v-model="form.totalCarbs" :min="0" :precision="2" style="width: 100%;" />
              <span v-if="autoCalculatedNutrition.carbs > 0" class="nutrition-hint">
                自动计算: {{ autoCalculatedNutrition.carbs.toFixed(2) }} g
              </span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="适宜人群">
          <el-select v-model="form.suitableFor" placeholder="请选择适宜人群" style="width: 100%;" clearable>
            <el-option
              v-for="item in suitableForOptions"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="图片URL">
          <el-input v-model="form.comboImage" placeholder="请输入图片URL" />
        </el-form-item>

        <el-form-item label="套餐描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入套餐描述" />
        </el-form-item>

        <el-form-item label="关联菜品">
          <div class="dish-select-area">
            <!-- 添加菜品按钮 -->
            <div class="add-dish-row">
              <el-select ref="dishSelectRef" class="dish-add-select" placeholder="添加菜品到套餐" @change="addDish" size="small" clearable>
                <el-option
                  v-for="dish in availableDishes"
                  :key="dish.id"
                  :label="dish.dishName"
                  :value="dish.id"
                >
                  <span>{{ dish.dishName }}</span>
                  <span style="float: right; color: #909399; font-size: 12px;">¥{{ dish.price }}</span>
                </el-option>
              </el-select>
              <el-tag type="info" class="dish-count-tag">已选 {{ selectedDishItems.length }} 个</el-tag>
            </div>
            
            <!-- 已选菜品列表 -->
            <div v-if="selectedDishDetails.length > 0" class="selected-dish-table">
              <div class="dish-table-header">
                <span class="col-name">菜品名称</span>
                <span class="col-qty">份数</span>
                <span class="col-fixed">可替换</span>
                <span class="col-action">操作</span>
              </div>
              <div v-for="(item, index) in selectedDishItems" :key="item.dishId" class="dish-table-row">
                <span class="col-name">{{ getDishName(item.dishId) }}</span>
                <span class="col-qty">
                  <el-input-number v-model="selectedDishItems[index].quantity" :min="1" :max="99" size="small" controls-position="right" style="width: 100px;" />
                </span>
                <span class="col-fixed">
                  <el-switch
                    :model-value="selectedDishItems[index].isFixed === 0"
                    @change="toggleFixed(item.dishId)"
                    active-text="可换"
                    inactive-text="固定"
                    size="small"
                  />
                </span>
                <span class="col-action">
                  <el-button size="small" type="danger" link @click="removeDish(item.dishId)">移除</el-button>
                </span>
              </div>
            </div>
            <p v-else class="view-empty" style="margin-top: 8px;">暂未添加菜品，请从上方下拉框选择</p>
          </div>
          
          <!-- 可替换菜品池 -->
          <div v-if="selectedDishItems.filter(i => i.isFixed === 0).length > 0" class="replaceable-pool-section">
            <el-divider />
            <div class="pool-header">
              <span class="pool-title">可替换菜品池</span>
              <el-tag size="small" type="warning" effect="plain">套餐中 {{ selectedDishItems.filter(i => i.isFixed === 0).length }} 个菜品可替换</el-tag>
            </div>
            <p class="pool-hint">选择允许替换入的菜品（顾客可将套餐中可替换菜品换成以下菜品）：</p>
            <el-select v-model="replaceablePoolIds" multiple placeholder="选择可替换的菜品" style="width: 100%;" collapse-tags collapse-tags-tooltip>
              <el-option
                v-for="dish in dishList"
                :key="dish.id"
                :label="dish.dishName"
                :value="dish.id"
              />
            </el-select>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="loading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查看套餐详情弹窗 -->
    <el-dialog v-model="viewDialogVisible" title="套餐详情" width="600px" destroy-on-close>
      <div v-if="viewData" class="view-content">
        <div class="view-header">
          <img v-if="viewData.combo.comboImage" :src="getImageUrl(viewData.combo.comboImage)" class="view-image" alt="套餐图片" />
          <div v-else class="view-image-placeholder">无图</div>
          <div class="view-basic">
            <h3>{{ viewData.combo.comboName }}</h3>
            <el-tag size="small" type="info">{{ getComboTypeLabel(viewData.combo.comboType) }}</el-tag>
            <el-tag :type="getStatusTag(viewData.combo.status)" size="small" style="margin-left: 8px;">{{ getStatusLabel(viewData.combo.status) }}</el-tag>
          </div>
        </div>
        
        <el-descriptions :column="2" border class="view-descriptions">
          <el-descriptions-item label="价格">¥{{ viewData.combo.price }}</el-descriptions-item>
          <el-descriptions-item label="原价">¥{{ viewData.combo.originalPrice || '-' }}</el-descriptions-item>
          <el-descriptions-item label="总热量">{{ viewData.combo.totalCalories || 0 }} kcal</el-descriptions-item>
          <el-descriptions-item label="蛋白质">{{ viewData.combo.totalProtein || 0 }} g</el-descriptions-item>
          <el-descriptions-item label="脂肪">{{ viewData.combo.totalFat || 0 }} g</el-descriptions-item>
          <el-descriptions-item label="碳水">{{ viewData.combo.totalCarbs || 0 }} g</el-descriptions-item>
          <el-descriptions-item label="最大替换数">{{ viewData.combo.maxReplaceCount || 0 }} 个</el-descriptions-item>
          <el-descriptions-item label="销量">{{ viewData.combo.salesCount || 0 }}</el-descriptions-item>
        </el-descriptions>
        
        <div class="view-section">
          <h4>套餐描述</h4>
          <p class="view-description">{{ viewData.combo.description || '暂无描述' }}</p>
        </div>
        
        <div class="view-section">
          <h4>关联菜品 ({{ viewData.dishItems?.length || 0 }}个)</h4>
          <div v-if="viewData.dishItems && viewData.dishItems.length > 0" class="view-dish-table">
            <div class="view-dish-row" v-for="item in viewData.dishItems" :key="item.dishId">
              <span>{{ getDishName(item.dishId) }}</span>
              <el-tag size="small" :type="item.isFixed === 1 ? '' : 'warning'" effect="plain">
                {{ item.isFixed === 1 ? '固定' : '可替换' }}
              </el-tag>
              <span class="view-dish-qty">x{{ item.quantity || 1 }}</span>
            </div>
          </div>
          <p v-else class="view-empty">暂无关联菜品</p>
        </div>
        
        <div class="view-section" v-if="viewData.combo?.replaceableDishPool">
          <h4>可替换菜品池</h4>
          <div class="dish-tags">
            <el-tag 
              v-for="id in JSON.parse(viewData.combo.replaceableDishPool || '[]')" 
              :key="id" 
              size="small" 
              type="warning"
              class="dish-tag"
            >
              {{ getDishName(id) }}
            </el-tag>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.card-panel {
  background: var(--bs-card-bg);
  border-radius: var(--bs-radius-md);
  box-shadow: var(--bs-card-shadow);
  padding: var(--bs-spacing-lg);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--bs-spacing-lg);
}

.card-header h3 {
  font-size: var(--bs-font-size-lg);
  font-weight: 600;
  color: var(--bs-text-title);
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: var(--bs-spacing-sm) 20px;
  border-radius: var(--bs-radius-md);
  font-size: var(--bs-font-size-base);
  font-weight: 500;
  border: 1px solid transparent;
  cursor: pointer;
  transition: 0.15s;
}

.btn-primary {
  background: var(--bs-primary);
  color: #FFFFFF;
}

.btn-primary:hover {
  background: var(--bs-primary-hover);
}

.combo-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.combo-image {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  object-fit: cover;
}

.combo-image-placeholder {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 12px;
}

.combo-name-wrapper {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.combo-name {
  font-weight: 500;
  color: var(--bs-text-title);
}

.combo-type-tag {
  width: fit-content;
}

.price-wrapper {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.current-price {
  font-weight: 600;
  color: #f56c6c;
}

.original-price {
  font-size: 12px;
  color: #999;
  text-decoration: line-through;
}

/* 菜品选择区域 */
.dish-select-area {
  width: 100%;
}

.add-dish-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.dish-add-select {
  flex: 1;
}

.dish-count-tag {
  flex-shrink: 0;
}

.selected-dish-table {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  overflow: hidden;
}

.dish-table-header {
  display: flex;
  align-items: center;
  background: #f5f7fa;
  padding: 8px 12px;
  font-size: 13px;
  color: #909399;
  font-weight: 500;
}

.dish-table-row {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  border-top: 1px solid #f0f0f0;
  transition: background 0.15s;
}

.dish-table-row:hover {
  background: #fafafa;
}

.col-name { flex: 1; font-weight: 500; }
.col-qty { width: 120px; }
.col-fixed { width: 120px; }
.col-action { width: 60px; text-align: right; }

.replaceable-pool-section {
  margin-top: 12px;
}

.pool-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.pool-title {
  font-weight: 500;
  color: #606266;
}

.pool-hint {
  font-size: 12px;
  color: #909399;
  margin: 0 0 8px 0;
}

/* 查看弹窗 - 菜品列表 */
.view-dish-table {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  overflow: hidden;
}

.view-dish-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-bottom: 1px solid #f0f0f0;
}

.view-dish-row:last-child {
  border-bottom: none;
}

.view-dish-qty {
  margin-left: auto;
  color: #909399;
  font-size: 13px;
}

/* 查看弹窗样式 */
.view-content {
  padding: 10px 0;
}

.view-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #e4e7ed;
}

.view-image {
  width: 100px;
  height: 100px;
  border-radius: 8px;
  object-fit: cover;
}

.view-image-placeholder {
  width: 100px;
  height: 100px;
  border-radius: 8px;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 14px;
}

.view-basic h3 {
  margin: 0 0 10px 0;
  font-size: 20px;
  color: #303133;
}

.view-descriptions {
  margin-bottom: 20px;
}

.view-section {
  margin-bottom: 20px;
}

.view-section h4 {
  margin: 0 0 10px 0;
  font-size: 16px;
  color: #606266;
  font-weight: 600;
}

.view-description {
  margin: 0;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  color: #606266;
  line-height: 1.6;
}

.view-empty {
  margin: 0;
  color: #909399;
  font-style: italic;
}

.dish-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.dish-tag {
  margin: 0;
}

.nutrition-hint {
  font-size: 12px;
  color: #67c23a;
  margin-left: 8px;
}
</style>
