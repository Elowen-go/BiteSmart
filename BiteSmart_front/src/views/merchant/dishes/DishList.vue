<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Delete, Plus, Minus, Refresh, Search } from '@element-plus/icons-vue'
import { getDishList, addDish, updateDish, deleteDish, getDishDetail } from '../../../api/merchant/dishes'
import { uploadFile } from '../../../api/merchant/shop'
import { getMerchantIngredientList, getMerchantIngredientCategories } from '../../../api/merchant/ingredients'
import { getCategoryList } from '../../../api/merchant/categories'
import type { Dish } from '../../../api/merchant/dishes'
import type { DishCategory } from '../../../api/merchant/categories'
import { resolveFileUrl } from '../../../utils/fileUrl'

const loading = ref(false)
const dishList = ref<Dish[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()
const editingId = ref<number | null>(null)
const viewDialogVisible = ref(false)
const viewForm = ref<any>({})
const searchKeyword = ref('')
const statusFilter = ref<number | 'all'>('all')
const categoryList = ref<DishCategory[]>([])

const form = ref({
  dishName: '',
  price: 0,
  stock: 0,
  status: 10,
  description: '',
  categoryId: 0,
  dishImage: '',
  calories: 0,
  protein: 0,
  fat: 0,
  carbs: 0
})

const imagePreview = ref('')
const selectedImageFile = ref<File | null>(null)
const uploadInput = ref<HTMLInputElement | null>(null)

// 食材相关
const ingredientList = ref<any[]>([])
const ingredientCategories = ref<string[]>([])
const selectedIngredients = ref<{ ingredientId: number; ingredientName: string; weight: number }[]>([])
const showIngredientSelector = ref(false)
const selectedCategory = ref('')

// 根据分类筛选食材
const filteredIngredients = computed(() => {
  if (!selectedCategory.value) return ingredientList.value
  return ingredientList.value.filter(ing => ing.categoryName === selectedCategory.value)
})

const filteredDishList = computed(() => {
  return dishList.value.filter((item) => {
    const keywordMatched = !searchKeyword.value || item.dishName?.includes(searchKeyword.value)
    const statusMatched = statusFilter.value === 'all' || item.status === statusFilter.value
    return keywordMatched && statusMatched
  })
})

const dishStats = computed(() => {
  const onSale = dishList.value.filter((item) => item.status === 10).length
  const offSale = dishList.value.filter((item) => item.status !== 10).length
  const lowStock = dishList.value.filter((item) => Number(item.stock || 0) <= 5).length
  const noNutrition = dishList.value.filter((item) => !item.calories && !item.protein && !item.fat && !item.carbs).length

  return [
    { label: '上架菜品', value: onSale, hint: '当前可售', className: 'success' },
    { label: '下架菜品', value: offSale, hint: '暂不售卖', className: 'muted' },
    { label: '低库存', value: lowStock, hint: '库存不高于 5', className: 'warning' },
    { label: '待补营养', value: noNutrition, hint: '缺少营养数据', className: 'danger' }
  ]
})

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getDishList({ page: page.value, size: size.value })
    if (res.code === 200) {
      // 后端返回 ResultVO<PageResultVO<Dish>>，需要取 res.data.data
      const pageData = res.data?.data || res.data
      dishList.value = pageData.list || []
      total.value = pageData.total || 0
    }
  } catch (e) {
    console.error('获取菜品列表失败', e)
  } finally {
    loading.value = false
  }
}

const fetchCategories = async () => {
  try {
    const res = await getCategoryList()
    if (res.code === 200) {
      const data = res.data?.list || res.data || []
      categoryList.value = data
    }
  } catch (e) {
    console.error('获取菜品分类失败', e)
  }
}

const handleAdd = () => {
  editingId.value = null
  dialogTitle.value = '新增菜品'
  form.value = { dishName: '', price: 0, stock: 0, status: 10, description: '', categoryId: 0, dishImage: '', calories: 0, protein: 0, fat: 0, carbs: 0 }
  imagePreview.value = ''
  selectedImageFile.value = null
  selectedIngredients.value = []
  dialogVisible.value = true
  fetchIngredients()
}

const handleEdit = async (row: any) => {
  editingId.value = row.id
  dialogTitle.value = '编辑菜品'
  
  // 获取菜品详情（包含关联的食材）
  try {
    const res = await getDishDetail(row.id)
    if (res.code === 200) {
      const detail = res.data
      form.value = {
        dishName: detail.dishName || detail.name,
        price: detail.price,
        stock: detail.stock,
        status: detail.status,
        description: detail.description || '',
        categoryId: detail.categoryId || 0,
        dishImage: detail.dishImage || detail.imageUrl || '',
        calories: detail.calories || 0,
        protein: detail.protein || 0,
        fat: detail.fat || 0,
        carbs: detail.carbs || 0
      }
      if (form.value.dishImage) {
        imagePreview.value = resolveFileUrl(form.value.dishImage)
      } else {
        imagePreview.value = ''
      }
      
      // 加载菜品关联的食材
      if (detail.ingredients && detail.ingredients.length > 0) {
        selectedIngredients.value = detail.ingredients.map((item: any) => ({
          ingredientId: item.ingredientId,
          ingredientName: item.ingredientName || '未知食材',
          weight: item.weight || 100
        }))
      } else {
        selectedIngredients.value = []
      }
    }
  } catch (e) {
    console.error('获取菜品详情失败', e)
    ElMessage.error('获取菜品详情失败')
    return
  }
  
  selectedImageFile.value = null
  dialogVisible.value = true
  fetchIngredients()
}

const handleView = async (row: any) => {
  // 获取菜品详情（包含关联的食材）
  try {
    const res = await getDishDetail(row.id)
    if (res.code === 200) {
      viewForm.value = res.data
      viewDialogVisible.value = true
    }
  } catch (e) {
    console.error('获取菜品详情失败', e)
    ElMessage.error('获取菜品详情失败')
  }
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定要删除菜品「${row.dishName}」吗？`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await deleteDish(row.id)
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

const handleImageSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  selectedImageFile.value = file
  const reader = new FileReader()
  reader.onload = (e) => {
    imagePreview.value = e.target?.result as string
  }
  reader.readAsDataURL(file)
  target.value = ''
}

const handleRemoveImage = () => {
  form.value.dishImage = ''
  imagePreview.value = ''
  selectedImageFile.value = null
}

const triggerImageUpload = () => {
  uploadInput.value?.click()
}

const handleSubmit = async () => {
  loading.value = true
  try {
    if (selectedImageFile.value) {
      const uploadRes = await uploadFile(selectedImageFile.value, 'dish_image')
      if (uploadRes.code === 200) {
        form.value.dishImage = uploadRes.data.url
        imagePreview.value = resolveFileUrl(uploadRes.data.url)
        selectedImageFile.value = null
      } else {
        ElMessage.error(uploadRes.message || '图片上传失败')
        loading.value = false
        return
      }
    }

    // 构建提交数据，包含食材信息
    const submitData = {
      ...form.value,
      calories: selectedIngredients.value.length ? Math.round(calculatedNutrition.value.calories) : form.value.calories,
      protein: selectedIngredients.value.length ? Number(calculatedNutrition.value.protein.toFixed(1)) : form.value.protein,
      fat: selectedIngredients.value.length ? Number(calculatedNutrition.value.fat.toFixed(1)) : form.value.fat,
      carbs: selectedIngredients.value.length ? Number(calculatedNutrition.value.carbs.toFixed(1)) : form.value.carbs,
      ingredients: selectedIngredients.value.map(item => ({
        ingredientId: item.ingredientId,
        weight: item.weight
      }))
    }

    let res: any
    if (editingId.value) {
      res = await updateDish(editingId.value, submitData as any)
    } else {
      res = await addDish(submitData as any)
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

const handleToggleStatus = async (row: any) => {
  try {
    const newStatus = row.status === 10 ? 20 : 10
    const res = await updateDish(row.id, { ...row, status: newStatus } as any)
    if (res.code === 200) {
      ElMessage.success(newStatus === 10 ? '已上架' : '已下架')
      fetchList()
    }
  } catch (e) {
    ElMessage.error('操作失败')
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

const getCategoryName = (categoryId: number) => {
  return categoryList.value.find((item) => item.id === categoryId)?.categoryName || '未分类'
}

const getStockTag = (stock: number) => {
  if (Number(stock || 0) <= 0) return { label: '售罄', type: 'danger' as const }
  if (Number(stock || 0) <= 5) return { label: '低库存', type: 'warning' as const }
  return { label: '充足', type: 'success' as const }
}

const formatAmount = (amount: number | string | undefined) => {
  return Number(amount || 0).toFixed(2)
}

const previewImage = (imageUrl: string) => {
  if (!imageUrl) return
  const fullUrl = resolveFileUrl(imageUrl)
  showImagePreview.value = true
  previewImageUrl.value = fullUrl
}

const showImagePreview = ref(false)
const previewImageUrl = ref('')

const formatDateTime = (dateTime: string) => {
  if (!dateTime) return '-'
  const date = new Date(dateTime)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

// 计算查看详情的营养成分
const calculateViewNutrition = computed(() => {
  let calories = 0, protein = 0, fat = 0, carbs = 0
  if (!viewForm.value.ingredients) return { calories, protein, fat, carbs }
  
  viewForm.value.ingredients.forEach((item: any) => {
    // 从ingredientList查找食材信息
    const ingredient = ingredientList.value.find((ing: any) => ing.id === item.ingredientId)
    if (ingredient) {
      const ratio = (item.weight || 0) / 100
      calories += (ingredient.calories || 0) * ratio
      protein += (ingredient.protein || 0) * ratio
      fat += (ingredient.fat || 0) * ratio
      carbs += (ingredient.carbs || 0) * ratio
    }
  })
  return { calories, protein, fat, carbs }
})

// 获取食材列表和分类
const fetchIngredients = async () => {
  try {
    const [listRes, catRes] = await Promise.all([
      getMerchantIngredientList(),
      getMerchantIngredientCategories()
    ])
    if (listRes.code === 200) {
      ingredientList.value = listRes.data || []
    }
    if (catRes.code === 200) {
      ingredientCategories.value = catRes.data || []
    }
  } catch (e) {
    console.error('获取食材列表失败', e)
  }
}

// 添加食材到菜品
const addIngredientToDish = (ingredient: any) => {
  const exists = selectedIngredients.value.find(item => item.ingredientId === ingredient.id)
  if (exists) {
    ElMessage.warning('该食材已添加')
    return
  }
  selectedIngredients.value.push({
    ingredientId: ingredient.id,
    ingredientName: ingredient.name,
    weight: 100
  })
}

// 移除食材
const removeIngredient = (index: number) => {
  selectedIngredients.value.splice(index, 1)
}

// 计算营养成分
const calculatedNutrition = computed(() => {
  let calories = 0, protein = 0, fat = 0, carbs = 0
  selectedIngredients.value.forEach(item => {
    const ingredient = ingredientList.value.find(ing => ing.id === item.ingredientId)
    if (ingredient) {
      const ratio = item.weight / 100
      calories += (ingredient.calories || 0) * ratio
      protein += (ingredient.protein || 0) * ratio
      fat += (ingredient.fat || 0) * ratio
      carbs += (ingredient.carbs || 0) * ratio
    }
  })
  return { calories, protein, fat, carbs }
})

onMounted(() => {
  fetchList()
  fetchCategories()
})
</script>

<template>
  <div class="page-container">
    <div class="dish-page">
      <div class="page-head">
        <div>
          <h2>菜品管理</h2>
          <p>维护菜品图片、价格、库存、分类和营养信息</p>
        </div>
        <div class="head-actions">
          <el-button :icon="Refresh" :loading="loading" @click="fetchList">刷新</el-button>
          <el-button type="primary" :icon="Plus" @click="handleAdd">添加菜品</el-button>
        </div>
      </div>

      <div class="summary-grid">
        <div v-for="item in dishStats" :key="item.label" class="summary-item" :class="item.className">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <em>{{ item.hint }}</em>
        </div>
      </div>

      <div class="card-panel dish-panel">
        <div class="toolbar">
          <el-input
            v-model="searchKeyword"
            :prefix-icon="Search"
            clearable
            placeholder="搜索菜品名称"
            class="search-input"
          />
          <el-segmented
            v-model="statusFilter"
            :options="[
              { label: '全部', value: 'all' },
              { label: '上架', value: 10 },
              { label: '下架', value: 20 }
            ]"
          />
          <span class="toolbar-count">当前 {{ filteredDishList.length }} 个菜品</span>
        </div>

        <el-table :data="filteredDishList" v-loading="loading" empty-text="暂无符合条件的菜品">
          <el-table-column label="菜品图片" width="100">
            <template #default="{ row }">
              <div class="table-dish-image">
                <img 
                  v-if="row.dishImage" 
                  :src="resolveFileUrl(row.dishImage)"
                  alt="菜品图片"
                  @click="previewImage(row.dishImage)"
                />
                <div v-else class="image-placeholder">{{ (row.dishName || '菜')[0] }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="dishName" label="菜品信息" min-width="220">
            <template #default="{ row }">
              <div class="dish-name">{{ row.dishName }}</div>
              <div class="sub-text">{{ getCategoryName(row.categoryId) }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="price" label="价格" width="120" align="right">
            <template #default="{ row }">
              <span class="amount">¥{{ formatAmount(row.price) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="营养" min-width="180">
            <template #default="{ row }">
              <div class="nutrition-brief">{{ row.calories || 0 }} 千卡</div>
              <div class="sub-text">蛋白 {{ row.protein || 0 }}g / 脂肪 {{ row.fat || 0 }}g / 碳水 {{ row.carbs || 0 }}g</div>
            </template>
          </el-table-column>
          <el-table-column prop="stock" label="库存" width="120">
            <template #default="{ row }">
              <div class="stock-cell">
                <strong>{{ row.stock || 0 }}</strong>
                <el-tag :type="getStockTag(row.stock).type" size="small">{{ getStockTag(row.stock).label }}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusTag(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right" align="right">
            <template #default="{ row }">
              <el-button size="small" @click="handleView(row)">查看</el-button>
              <el-button size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button size="small" @click="handleToggleStatus(row)">
                {{ row.status === 10 ? '下架' : '上架' }}
              </el-button>
              <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-row">
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

    <!-- 图片预览弹窗 -->
    <div class="image-preview-modal" v-if="showImagePreview" @click="showImagePreview = false">
      <div class="preview-content" @click.stop>
        <img :src="previewImageUrl" alt="菜品图片预览" />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="760px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="菜品名称">
          <el-input v-model="form.dishName" placeholder="请输入菜品名称" />
        </el-form-item>
        <el-form-item label="菜品分类">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%;">
            <el-option
              v-for="item in categoryList"
              :key="item.id"
              :label="item.categoryName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="form.stock" :min="0" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="10">上架</el-radio>
            <el-radio :value="20">下架</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜品描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入菜品描述" />
        </el-form-item>
        <el-form-item label="菜品图片">
          <div class="image-preview" @click="triggerImageUpload">
            <img v-if="imagePreview" :src="imagePreview" alt="菜品图片" class="image" />
            <div v-else class="image-placeholder">
              {{ (form.dishName || '菜')[0] }}
            </div>
            <button class="remove-btn" @click.stop="handleRemoveImage" v-if="imagePreview">
              <Delete style="width: 16px; height: 16px;" />
            </button>
            <div class="upload-hint" v-if="!imagePreview">
              <Upload style="width: 20px; height: 20px;" />
              <span>点击上传图片</span>
            </div>
          </div>
          <input
            ref="uploadInput"
            type="file"
            accept="image/jpeg,image/png,image/gif"
            class="hidden-input"
            @change="handleImageSelect"
          />
        </el-form-item>
        
        <!-- 食材选择区域 -->
        <el-form-item label="关联食材">
          <div class="ingredient-section">
            <!-- 已选食材列表 -->
            <div v-if="selectedIngredients.length > 0" class="selected-ingredients">
              <div v-for="(item, index) in selectedIngredients" :key="item.ingredientId" class="ingredient-item">
                <span class="ingredient-name">{{ item.ingredientName }}</span>
                <el-input-number 
                  v-model="item.weight" 
                  :min="1" 
                  :max="1000" 
                  size="small"
                  style="width: 100px;"
                />
                <span class="unit">g</span>
                <el-button type="danger" size="small" circle @click="removeIngredient(index)">
                  <Minus style="width: 12px; height: 12px;" />
                </el-button>
              </div>
            </div>
            <div v-else class="no-ingredients">暂无关联食材</div>
            
            <!-- 添加食材按钮 -->
            <el-button type="primary" size="small" @click="showIngredientSelector = true" style="margin-top: 10px;">
              <Plus style="width: 14px; height: 14px;" />
              添加食材
            </el-button>
            
            <!-- 营养成分计算 -->
            <div v-if="selectedIngredients.length > 0" class="nutrition-info">
              <div class="nutrition-title">营养成分（估算）</div>
              <div class="nutrition-items">
                <span>热量: {{ calculatedNutrition.calories.toFixed(1) }} 大卡</span>
                <span>蛋白质: {{ calculatedNutrition.protein.toFixed(1) }} g</span>
                <span>脂肪: {{ calculatedNutrition.fat.toFixed(1) }} g</span>
                <span>碳水: {{ calculatedNutrition.carbs.toFixed(1) }} g</span>
              </div>
            </div>
            <div v-else class="manual-nutrition">
              <div class="nutrition-title">手动营养信息</div>
              <div class="manual-grid">
                <el-input-number v-model="form.calories" :min="0" :precision="0" placeholder="热量" />
                <el-input-number v-model="form.protein" :min="0" :precision="1" placeholder="蛋白质" />
                <el-input-number v-model="form.fat" :min="0" :precision="1" placeholder="脂肪" />
                <el-input-number v-model="form.carbs" :min="0" :precision="1" placeholder="碳水" />
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="loading">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 食材选择器弹窗 -->
    <el-dialog v-model="showIngredientSelector" title="选择食材" width="600px">
      <div class="ingredient-selector">
        <!-- 分类筛选 -->
        <div class="category-filter">
          <el-radio-group v-model="selectedCategory" size="small">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button v-for="cat in ingredientCategories" :key="cat" :label="cat">{{ cat }}</el-radio-button>
          </el-radio-group>
        </div>
        
        <!-- 食材列表 -->
        <div class="ingredient-grid">
          <div 
            v-for="ingredient in filteredIngredients" 
            :key="ingredient.id"
            class="ingredient-card"
            @click="addIngredientToDish(ingredient)"
          >
            <div class="ingredient-name">{{ ingredient.name }}</div>
            <div class="ingredient-category">{{ ingredient.categoryName }}</div>
            <div class="ingredient-nutrition">
              {{ ingredient.calories }}大卡/100g
            </div>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 查看菜品详情弹窗 -->
    <el-dialog v-model="viewDialogVisible" title="菜品详情" width="500px">
      <el-form :model="viewForm" label-width="100px" class="view-form">
        <el-form-item label="菜品图片">
          <div class="view-image-container">
            <img 
              v-if="viewForm.dishImage" 
              :src="resolveFileUrl(viewForm.dishImage)"
              alt="菜品图片"
              class="view-image"
            />
            <div v-else class="view-image-placeholder">{{ (viewForm.dishName || '菜')[0] }}</div>
          </div>
        </el-form-item>
        <el-form-item label="菜品名称">
          <span class="view-text">{{ viewForm.dishName }}</span>
        </el-form-item>
        <el-form-item label="菜品分类">
          <span class="view-text">{{ getCategoryName(viewForm.categoryId) }}</span>
        </el-form-item>
        <el-form-item label="价格">
          <span class="view-text">¥{{ viewForm.price }}</span>
        </el-form-item>
        <el-form-item label="库存">
          <span class="view-text">{{ viewForm.stock }}</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-tag :type="getStatusTag(viewForm.status)">{{ getStatusLabel(viewForm.status) }}</el-tag>
        </el-form-item>
        <el-form-item label="创建时间">
          <span class="view-text">{{ formatDateTime(viewForm.createTime) }}</span>
        </el-form-item>
        <el-form-item label="菜品描述" v-if="viewForm.description">
          <span class="view-text">{{ viewForm.description }}</span>
        </el-form-item>
        <el-form-item label="基础营养">
          <div class="view-nutrition">
            <div class="nutrition-row">
              <span>热量: {{ viewForm.calories || 0 }} 大卡</span>
              <span>蛋白质: {{ viewForm.protein || 0 }} g</span>
            </div>
            <div class="nutrition-row">
              <span>脂肪: {{ viewForm.fat || 0 }} g</span>
              <span>碳水: {{ viewForm.carbs || 0 }} g</span>
            </div>
          </div>
        </el-form-item>
        
        <!-- 查看关联食材 -->
        <el-form-item label="关联食材" v-if="viewForm.ingredients && viewForm.ingredients.length > 0">
          <div class="view-ingredients">
            <div v-for="item in viewForm.ingredients" :key="item.ingredientId" class="view-ingredient-item">
              <span class="name">{{ item.ingredientName || '未知食材' }}</span>
              <span class="weight">{{ item.weight }}g</span>
            </div>
          </div>
        </el-form-item>
        
        <!-- 查看营养成分 -->
        <el-form-item label="营养成分" v-if="viewForm.ingredients && viewForm.ingredients.length > 0">
          <div class="view-nutrition">
            <div class="nutrition-row">
              <span>热量: {{ calculateViewNutrition.calories.toFixed(1) }} 大卡</span>
              <span>蛋白质: {{ calculateViewNutrition.protein.toFixed(1) }} g</span>
            </div>
            <div class="nutrition-row">
              <span>脂肪: {{ calculateViewNutrition.fat.toFixed(1) }} g</span>
              <span>碳水: {{ calculateViewNutrition.carbs.toFixed(1) }} g</span>
            </div>
          </div>
        </el-form-item>
      </el-form>
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

.dish-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-head h2 {
  margin: 0;
  color: var(--bs-text-title);
  font-size: 20px;
  font-weight: 650;
}

.page-head p {
  margin-top: 4px;
  color: var(--bs-text-muted);
  font-size: 13px;
}

.head-actions {
  display: flex;
  gap: 10px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.summary-item {
  min-height: 104px;
  padding: 16px;
  background: #fff;
  border: 1px solid var(--bs-border-light);
  border-radius: var(--bs-radius-md);
  box-shadow: var(--bs-card-shadow);
}

.summary-item span,
.summary-item em {
  display: block;
  color: var(--bs-text-muted);
  font-size: 13px;
  font-style: normal;
}

.summary-item strong {
  display: block;
  margin: 6px 0 4px;
  color: var(--bs-text-title);
  font-size: 28px;
  line-height: 1.1;
}

.summary-item.success {
  border-left: 3px solid #1b6b4a;
}

.summary-item.warning {
  border-left: 3px solid #b76e2a;
}

.summary-item.danger {
  border-left: 3px solid var(--bs-status-danger);
}

.summary-item.muted {
  border-left: 3px solid #8a9299;
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

.dish-panel {
  padding-top: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.search-input {
  width: 260px;
}

.toolbar-count {
  margin-left: auto;
  color: var(--bs-text-muted);
  font-size: 13px;
  white-space: nowrap;
}

.dish-name,
.amount,
.nutrition-brief {
  color: var(--bs-text-title);
  font-weight: 600;
}

.sub-text {
  margin-top: 4px;
  color: var(--bs-text-muted);
  font-size: 12px;
}

.stock-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stock-cell strong {
  color: var(--bs-text-title);
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
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

.image-preview {
  position: relative;
  width: 120px;
  height: 120px;
  cursor: pointer;
}

.image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
  border: 2px solid var(--bs-border-color);
}

.image-placeholder {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  background: #1B3A2F;
  color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  font-weight: 600;
  border: 2px solid var(--bs-border-color);
}

.remove-btn {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 24px;
  height: 24px;
  background: #D9534F;
  color: #FFFFFF;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
  z-index: 10;
}

.remove-btn:hover {
  background: #c9302c;
}

.upload-hint {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: transparent;
  transition: all 0.2s;
}

.image-preview:hover .upload-hint {
  background: rgba(0, 0, 0, 0.3);
  color: #FFFFFF;
}

.upload-hint span {
  font-size: var(--bs-font-size-sm);
  margin-top: 8px;
}

.hidden-input {
  display: none;
}

.table-dish-image {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
}

.table-dish-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.2s;
}

.table-dish-image img:hover {
  transform: scale(1.05);
}

.table-dish-image .image-placeholder {
  width: 100%;
  height: 100%;
  background: #1B3A2F;
  color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 600;
}

.image-preview-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  animation: fadeIn 0.2s ease;
}

.preview-content {
  max-width: 90%;
  max-height: 90%;
  background: #FFFFFF;
  padding: 20px;
  border-radius: 12px;
  animation: scaleIn 0.2s ease;
}

.preview-content img {
  max-width: 100%;
  max-height: 80vh;
  object-fit: contain;
  border-radius: 8px;
}

@keyframes scaleIn {
  from { transform: scale(0.9); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

.view-form .el-form-item {
  margin-bottom: 16px;
}

.view-image-container {
  width: 120px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
}

.view-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.view-image-placeholder {
  width: 100%;
  height: 100%;
  background: #1B3A2F;
  color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  font-weight: 600;
}

.view-text {
  color: var(--bs-text-primary);
  font-size: var(--bs-font-size-base);
}

/* 食材选择区域样式 */
.ingredient-section {
  border: 1px solid var(--bs-border-color);
  border-radius: 8px;
  padding: 12px;
  background: var(--bs-bg-secondary);
}

.selected-ingredients {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ingredient-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  background: #fff;
  border-radius: 6px;
  border: 1px solid var(--bs-border-color);
}

.ingredient-name {
  flex: 1;
  font-weight: 500;
  color: var(--bs-text-primary);
}

.unit {
  color: var(--bs-text-secondary);
  font-size: 12px;
}

.no-ingredients {
  color: var(--bs-text-secondary);
  font-size: 14px;
  text-align: center;
  padding: 16px;
}

.nutrition-info {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--bs-border-color);
}

.nutrition-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--bs-text-title);
  margin-bottom: 8px;
}

.nutrition-items {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
  color: var(--bs-text-secondary);
}

.manual-nutrition {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--bs-border-light);
}

.manual-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.manual-grid :deep(.el-input-number) {
  width: 100%;
}

/* 食材选择器样式 */
.ingredient-selector {
  max-height: 400px;
  overflow-y: auto;
}

.category-filter {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--bs-border-color);
}

.ingredient-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
}

.ingredient-card {
  padding: 12px;
  border: 1px solid var(--bs-border-color);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
}

.ingredient-card:hover {
  border-color: var(--bs-primary);
  background: rgba(27, 58, 47, 0.05);
}

.ingredient-card .ingredient-name {
  font-weight: 600;
  color: var(--bs-text-primary);
  margin-bottom: 4px;
}

.ingredient-card .ingredient-category {
  font-size: 12px;
  color: var(--bs-text-secondary);
  margin-bottom: 4px;
}

.ingredient-card .ingredient-nutrition {
  font-size: 11px;
  color: var(--bs-primary);
}

/* 查看详情中的食材样式 */
.view-ingredients {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.view-ingredient-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 12px;
  background: var(--bs-bg-secondary);
  border-radius: 6px;
  border: 1px solid var(--bs-border-color);
}

.view-ingredient-item .name {
  font-weight: 500;
  color: var(--bs-text-primary);
}

.view-ingredient-item .weight {
  color: var(--bs-text-secondary);
  font-size: 14px;
}

/* 查看详情中的营养成分样式 */
.view-nutrition {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.view-nutrition .nutrition-row {
  display: flex;
  gap: 24px;
}

.view-nutrition span {
  font-size: 14px;
  color: var(--bs-text-secondary);
}

@media (max-width: 1100px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .manual-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .page-head,
  .toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .head-actions,
  .search-input {
    width: 100%;
  }

  .summary-grid,
  .manual-grid {
    grid-template-columns: 1fr;
  }

  .toolbar-count {
    margin-left: 0;
  }
}
</style>
