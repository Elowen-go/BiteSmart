<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Refresh, Search } from '@element-plus/icons-vue'
import { getComboList, getComboDetail, addCombo, updateCombo, deleteCombo, comboTypeOptions, suitableForOptions, type DishItem, type ComboRequest } from '../../../api/merchant/combos'
import { getDishList } from '../../../api/merchant/dishes'
import { uploadFile } from '../../../api/merchant/profile'
import { resolveFileUrl } from '../../../utils/fileUrl'

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
const searchKeyword = ref('')
const statusFilter = ref<number | 'all'>('all')

// 菜品列表
const dishList = ref<any[]>([])
const dishListLoading = ref(false)

/** 已选菜品（含 isFixed、quantity） */
const selectedDishItems = ref<DishItem[]>([])

/** 可替换菜品池（选中的菜品ID列表） */
const replaceablePoolIds = ref<number[]>([])

/** 套餐图片预览 */
const comboImagePreview = ref('')

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
      dishImage: dish ? dish.dishImage : '',
      price: dish ? dish.price : 0,
      calories: dish ? dish.calories : 0,
      protein: dish ? dish.protein : 0,
      fat: dish ? dish.fat : 0,
      carbs: dish ? dish.carbs : 0
    }
  })
})

const selectedDishAmount = computed(() => {
  return selectedDishDetails.value.reduce((sum, item) => {
    return sum + Number(item.price || 0) * Number(item.quantity || 1)
  }, 0)
})

const replaceableDishCount = computed(() => {
  return selectedDishItems.value.filter((item) => item.isFixed === 0).length
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

const filteredComboList = computed(() => {
  return comboList.value.filter((item) => {
    const keywordMatched = !searchKeyword.value || item.comboName?.includes(searchKeyword.value)
    const statusMatched = statusFilter.value === 'all' || item.status === statusFilter.value
    return keywordMatched && statusMatched
  })
})

const comboStats = computed(() => {
  const onSale = comboList.value.filter((item) => item.status === 10).length
  const offSale = comboList.value.filter((item) => item.status !== 10).length
  const replaceable = comboList.value.filter((item) => Number(item.maxReplaceCount || 0) > 0).length
  const noNutrition = comboList.value.filter((item) => !item.totalCalories && !item.totalProtein && !item.totalFat && !item.totalCarbs).length

  return [
    { label: '上架套餐', value: onSale, hint: '用户可购买', className: 'success' },
    { label: '下架套餐', value: offSale, hint: '暂不展示', className: 'muted' },
    { label: '支持换菜', value: replaceable, hint: '可自定义套餐', className: 'warning' },
    { label: '待补营养', value: noNutrition, hint: '缺少营养汇总', className: 'danger' }
  ]
})

const form = ref<{
  comboName: string
  price: number
  originalPrice: number
  comboType: number
  status: number
  description: string
  comboImage: string
  totalCalories: number
  totalProtein: number
  totalFat: number
  totalCarbs: number
  maxReplaceCount: number
  suitableFor: string | null
}>({
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
})

const syncCalculatedFields = (syncPrice = false) => {
  const amount = Number(selectedDishAmount.value.toFixed(2))
  const nutrition = autoCalculatedNutrition.value

  form.value.originalPrice = amount
  form.value.totalCalories = Math.round(nutrition.calories || 0)
  form.value.totalProtein = Number((nutrition.protein || 0).toFixed(2))
  form.value.totalFat = Number((nutrition.fat || 0).toFixed(2))
  form.value.totalCarbs = Number((nutrition.carbs || 0).toFixed(2))
  form.value.maxReplaceCount = replaceableDishCount.value

  if (replaceableDishCount.value === 0) {
    replaceablePoolIds.value = []
  }

  if (syncPrice && (!form.value.price || form.value.price <= 0)) {
    form.value.price = amount
  }
}

watch(
  () => [
    selectedDishAmount.value,
    autoCalculatedNutrition.value.calories,
    autoCalculatedNutrition.value.protein,
    autoCalculatedNutrition.value.fat,
    autoCalculatedNutrition.value.carbs,
    replaceableDishCount.value
  ],
  () => {
    if (!dialogVisible.value || selectedDishItems.value.length === 0) return
    syncCalculatedFields(!editingId.value)
  }
)

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
  comboImagePreview.value = ''
}

const handleComboImageUpload = async (file: any) => {
  const rawFile = file.raw || file
  const reader = new FileReader()
  reader.onload = async (e) => {
    comboImagePreview.value = e.target?.result as string
    try {
      const res = await uploadFile(rawFile, 'combo')
      if (res.code === 200) {
        form.value.comboImage = res.data.url
        ElMessage.success('图片上传成功')
      } else {
        ElMessage.error(res.message || '上传失败')
        comboImagePreview.value = ''
      }
    } catch (e) {
      ElMessage.error('上传失败')
      comboImagePreview.value = ''
      console.error('上传套餐图片失败', e)
    }
  }
  reader.readAsDataURL(rawFile)
  return false
}

const handleRemoveComboImage = () => {
  form.value.comboImage = ''
  comboImagePreview.value = ''
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

const setDishReplaceable = (dishId: number, replaceable: boolean) => {
  const item = selectedDishItems.value.find(i => i.dishId === dishId)
  if (item) {
    item.isFixed = replaceable ? 0 : 1
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
        suitableFor: normalizeSuitableFor(combo.suitableFor)
      }
      comboImagePreview.value = resolveFileUrl(combo.comboImage)
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
  syncCalculatedFields(false)
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
  if (selectedDishItems.value.length === 0) {
    ElMessage.warning('请至少添加一个菜品')
    return
  }

  loading.value = true
  try {
    syncCalculatedFields(!editingId.value)
    const comboRequest: ComboRequest = {
      combo: { 
        ...form.value,
        suitableFor: normalizeSuitableFor(form.value.suitableFor) || undefined,
        replaceableDishPool: replaceablePoolIds.value.length > 0 ? JSON.stringify(replaceablePoolIds.value) : undefined
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
  return resolveFileUrl(url)
}

const getDishName = (dishId: number) => {
  const dish = dishList.value.find(d => d.id == dishId)
  return dish ? dish.dishName : `菜品${dishId}`
}

const getDishImage = (dishId: number) => {
  const dish = dishList.value.find(d => d.id == dishId)
  return dish?.dishImage || ''
}

const getDishInitial = (dish: any) => {
  return (dish?.dishName || '菜').slice(0, 1)
}

const formatAmount = (amount: number | string | undefined) => {
  return Number(amount || 0).toFixed(2)
}

const formatDateTime = (dateTime: string) => {
  if (!dateTime) return '-'
  return dateTime.replace('T', ' ').slice(0, 16)
}

const normalizeSuitableFor = (value?: string | null): string | null => {
  if (!value) return null

  let current: any = value
  for (let i = 0; i < 5; i += 1) {
    if (typeof current !== 'string') break
    try {
      const parsed = JSON.parse(current)
      if (parsed === current) break
      current = parsed
    } catch (e) {
      break
    }
  }

  if (Array.isArray(current)) return current.join('、')
  return typeof current === 'string' ? current : String(current)
}

const formatSuitableFor = (value?: string) => {
  return normalizeSuitableFor(value) || '未设置'
}

onMounted(() => {
  fetchList()
  fetchDishList()
})
</script>

<template>
  <div class="page-container">
    <div class="combo-page">
      <div class="page-head">
        <div>
          <h2>套餐管理</h2>
          <p>组合菜品，发布减脂、增肌、控糖等健康套餐</p>
        </div>
        <div class="head-actions">
          <el-button :icon="Refresh" :loading="loading" @click="fetchList">刷新</el-button>
          <el-button type="primary" :icon="Plus" @click="handleAdd">添加套餐</el-button>
        </div>
      </div>

      <div class="summary-grid">
        <div v-for="item in comboStats" :key="item.label" class="summary-item" :class="item.className">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <em>{{ item.hint }}</em>
        </div>
      </div>

      <div class="card-panel combo-panel">
        <div class="toolbar">
          <el-input
            v-model="searchKeyword"
            :prefix-icon="Search"
            clearable
            placeholder="搜索套餐名称"
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
          <span class="toolbar-count">当前 {{ filteredComboList.length }} 个套餐</span>
        </div>

        <el-table :data="filteredComboList" v-loading="loading" empty-text="暂无符合条件的套餐">
          <el-table-column label="套餐" min-width="200">
            <template #default="{ row }">
              <div class="combo-info">
                <img v-if="row.comboImage" :src="getImageUrl(row.comboImage)" class="combo-image" alt="套餐图片" />
                <div v-else class="combo-image-placeholder">无图</div>
                <div class="combo-name-wrapper">
                  <span class="combo-name">{{ row.comboName }}</span>
                  <el-tag size="small" type="info" class="combo-type-tag">{{ getComboTypeLabel(row.comboType) }}</el-tag>
                  <span class="sub-text">{{ formatSuitableFor(row.suitableFor) }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="price" label="价格" width="130" align="right">
            <template #default="{ row }">
              <div class="price-wrapper">
                <span class="current-price">¥{{ formatAmount(row.price) }}</span>
                <span v-if="row.originalPrice && row.originalPrice > row.price" class="original-price">¥{{ row.originalPrice }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="totalCalories" label="营养汇总" min-width="190">
            <template #default="{ row }">
              <div class="nutrition-brief">{{ row.totalCalories || 0 }} 千卡</div>
              <div class="sub-text">蛋白 {{ row.totalProtein || 0 }}g / 脂肪 {{ row.totalFat || 0 }}g / 碳水 {{ row.totalCarbs || 0 }}g</div>
            </template>
          </el-table-column>
          <el-table-column prop="maxReplaceCount" label="换菜" width="110">
            <template #default="{ row }">
              <el-tag v-if="Number(row.maxReplaceCount || 0) > 0" type="warning" effect="plain">
                可换 {{ row.maxReplaceCount }}
              </el-tag>
              <el-tag v-else type="info" effect="plain">固定</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusTag(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="salesCount" label="销量" width="90" />
          <el-table-column prop="createTime" label="创建时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right" align="right">
            <template #default="{ row }">
              <el-button size="small" @click="handleView(row)">查看</el-button>
              <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="880px" destroy-on-close>
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
              <span v-if="selectedDishAmount > 0" class="nutrition-hint">
                已同步菜品原价: ¥{{ formatAmount(selectedDishAmount) }}
              </span>
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
              <el-input-number v-model="form.maxReplaceCount" :min="0" :max="10" disabled style="width: 100%;" />
              <span class="nutrition-hint">
                已按可替换菜品数同步：{{ replaceableDishCount }} 个
              </span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="总热量 (kcal)">
              <el-input-number v-model="form.totalCalories" :min="0" :precision="0" style="width: 100%;" />
              <span v-if="autoCalculatedNutrition.calories > 0" class="nutrition-hint">
                已同步: {{ Math.round(autoCalculatedNutrition.calories) }} kcal
              </span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="蛋白质 (g)">
              <el-input-number v-model="form.totalProtein" :min="0" :precision="2" style="width: 100%;" />
              <span v-if="autoCalculatedNutrition.protein > 0" class="nutrition-hint">
                已同步: {{ autoCalculatedNutrition.protein.toFixed(2) }} g
              </span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="脂肪 (g)">
              <el-input-number v-model="form.totalFat" :min="0" :precision="2" style="width: 100%;" />
              <span v-if="autoCalculatedNutrition.fat > 0" class="nutrition-hint">
                已同步: {{ autoCalculatedNutrition.fat.toFixed(2) }} g
              </span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="碳水 (g)">
              <el-input-number v-model="form.totalCarbs" :min="0" :precision="2" style="width: 100%;" />
              <span v-if="autoCalculatedNutrition.carbs > 0" class="nutrition-hint">
                已同步: {{ autoCalculatedNutrition.carbs.toFixed(2) }} g
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

        <el-form-item label="套餐图片">
          <div class="combo-image-upload">
            <img v-if="comboImagePreview" :src="comboImagePreview" alt="套餐图片" class="combo-image-preview" />
            <el-upload
              v-else
              class="combo-image-uploader"
              :before-upload="handleComboImageUpload"
              accept="image/*"
            >
              <div class="upload-hint">
                <el-icon class="el-icon--plus"><Plus /></el-icon>
                <span>点击上传图片</span>
              </div>
            </el-upload>
            <button v-if="comboImagePreview" class="remove-image-btn" @click="handleRemoveComboImage">
              <Delete style="width: 16px; height: 16px;" />
            </button>
          </div>
        </el-form-item>

        <el-form-item label="套餐描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入套餐描述" />
        </el-form-item>

        <el-form-item label="关联菜品">
          <div class="dish-select-area">
            <div class="combo-builder-summary">
              <div>
                <span>菜品原价</span>
                <strong>¥{{ formatAmount(selectedDishAmount) }}</strong>
              </div>
              <div>
                <span>套餐售价</span>
                <strong>¥{{ formatAmount(form.price) }}</strong>
              </div>
              <div>
                <span>总热量</span>
                <strong>{{ Math.round(autoCalculatedNutrition.calories || 0) }} 千卡</strong>
              </div>
              <div>
                <span>营养</span>
                <strong>{{ autoCalculatedNutrition.protein.toFixed(1) }} / {{ autoCalculatedNutrition.fat.toFixed(1) }} / {{ autoCalculatedNutrition.carbs.toFixed(1) }}g</strong>
              </div>
            </div>

            <!-- 添加菜品按钮 -->
            <div class="add-dish-row">
              <el-select ref="dishSelectRef" class="dish-add-select" popper-class="dish-option-popper" placeholder="添加菜品到套餐" @change="addDish" size="small" clearable>
                <el-option
                  v-for="dish in availableDishes"
                  :key="dish.id"
                  :label="dish.dishName"
                  :value="dish.id"
                >
                  <div class="dish-option">
                    <img v-if="dish.dishImage" :src="getImageUrl(dish.dishImage)" class="dish-option-image" alt="菜品图片" />
                    <div v-else class="dish-option-placeholder">{{ getDishInitial(dish) }}</div>
                    <div class="dish-option-main">
                      <span class="dish-option-name">{{ dish.dishName }}</span>
                      <span class="dish-option-meta">{{ dish.calories || 0 }} 千卡 · 蛋白 {{ dish.protein || 0 }}g</span>
                    </div>
                    <span class="dish-option-price">¥{{ formatAmount(dish.price) }}</span>
                  </div>
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
              <div v-for="item in selectedDishDetails" :key="item.dishId" class="dish-table-row">
                <span class="col-name">
                  <span class="dish-mini-card">
                    <img v-if="item.dishImage" :src="getImageUrl(item.dishImage)" class="dish-mini-image" alt="菜品图片" />
                    <span v-else class="dish-mini-placeholder">{{ getDishInitial(item) }}</span>
                    <span class="dish-mini-main">
                      <span class="dish-mini-name">{{ item.dishName }}</span>
                      <span class="dish-mini-meta">¥{{ formatAmount(item.price) }} · {{ item.calories || 0 }} 千卡</span>
                    </span>
                  </span>
                </span>
                <span class="col-qty">
                  <el-input-number v-model="item.quantity" :min="1" :max="99" size="small" controls-position="right" style="width: 100px;" />
                </span>
                <span class="col-fixed">
                  <el-segmented
                    :model-value="item.isFixed === 0 ? 'replaceable' : 'fixed'"
                    :options="[
                      { label: '固定', value: 'fixed' },
                      { label: '可换', value: 'replaceable' }
                    ]"
                    size="small"
                    @update:model-value="(value: string) => setDishReplaceable(item.dishId, value === 'replaceable')"
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
            <el-select v-model="replaceablePoolIds" multiple popper-class="dish-option-popper" placeholder="选择可替换的菜品" style="width: 100%;" collapse-tags collapse-tags-tooltip>
              <el-option
                v-for="dish in dishList"
                :key="dish.id"
                :label="dish.dishName"
                :value="dish.id"
              >
                <div class="dish-option">
                  <img v-if="dish.dishImage" :src="getImageUrl(dish.dishImage)" class="dish-option-image" alt="菜品图片" />
                  <div v-else class="dish-option-placeholder">{{ getDishInitial(dish) }}</div>
                  <div class="dish-option-main">
                    <span class="dish-option-name">{{ dish.dishName }}</span>
                    <span class="dish-option-meta">{{ dish.calories || 0 }} 千卡 · 脂肪 {{ dish.fat || 0 }}g · 碳水 {{ dish.carbs || 0 }}g</span>
                  </div>
                  <span class="dish-option-price">¥{{ formatAmount(dish.price) }}</span>
                </div>
              </el-option>
            </el-select>
            <div v-if="replaceablePoolIds.length > 0" class="replaceable-preview-grid">
              <div v-for="id in replaceablePoolIds" :key="id" class="replaceable-preview-card">
                <img v-if="getDishImage(id)" :src="getImageUrl(getDishImage(id))" class="replaceable-preview-image" alt="菜品图片" />
                <div v-else class="replaceable-preview-placeholder">{{ getDishName(id).slice(0, 1) }}</div>
                <div class="replaceable-preview-main">
                  <span>{{ getDishName(id) }}</span>
                  <small>可替换入套餐</small>
                </div>
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
              <span class="dish-mini-card">
                <img v-if="getDishImage(item.dishId)" :src="getImageUrl(getDishImage(item.dishId))" class="dish-mini-image" alt="菜品图片" />
                <span v-else class="dish-mini-placeholder">{{ getDishName(item.dishId).slice(0, 1) }}</span>
                <span class="dish-mini-main">
                  <span class="dish-mini-name">{{ getDishName(item.dishId) }}</span>
                  <span class="dish-mini-meta">{{ getDishById(item.dishId)?.calories || 0 }} 千卡 · ¥{{ formatAmount(getDishById(item.dishId)?.price) }}</span>
                </span>
              </span>
              <el-tag size="small" :type="item.isFixed === 1 ? undefined : 'warning'" effect="plain">
                {{ item.isFixed === 1 ? '固定' : '可替换' }}
              </el-tag>
              <span class="view-dish-qty">x{{ item.quantity || 1 }}</span>
            </div>
          </div>
          <p v-else class="view-empty">暂无关联菜品</p>
        </div>
        
        <div class="view-section" v-if="viewData.combo?.replaceableDishPool">
          <h4>可替换菜品池</h4>
          <div class="replaceable-preview-grid">
            <div v-for="id in JSON.parse(viewData.combo.replaceableDishPool || '[]')" :key="id" class="replaceable-preview-card">
              <img v-if="getDishImage(id)" :src="getImageUrl(getDishImage(id))" class="replaceable-preview-image" alt="菜品图片" />
              <div v-else class="replaceable-preview-placeholder">{{ getDishName(id).slice(0, 1) }}</div>
              <div class="replaceable-preview-main">
                <span>{{ getDishName(id) }}</span>
                <small>{{ getDishById(id)?.calories || 0 }} 千卡 · ¥{{ formatAmount(getDishById(id)?.price) }}</small>
              </div>
            </div>
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

.combo-page {
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

.combo-panel {
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

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
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

.sub-text {
  color: var(--bs-text-muted);
  font-size: 12px;
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
  color: var(--bs-text-title);
}

.original-price {
  font-size: 12px;
  color: #999;
  text-decoration: line-through;
}

.nutrition-brief {
  color: var(--bs-text-title);
  font-weight: 600;
}

/* 菜品选择区域 */
.dish-select-area {
  width: 100%;
}

.combo-builder-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.combo-builder-summary > div {
  padding: 12px;
  background: var(--bs-bg-hover);
  border: 1px solid var(--bs-border-light);
  border-radius: var(--bs-radius-md);
}

.combo-builder-summary span,
.combo-builder-summary strong {
  display: block;
}

.combo-builder-summary span {
  color: var(--bs-text-muted);
  font-size: 12px;
}

.combo-builder-summary strong {
  margin-top: 4px;
  color: var(--bs-text-title);
  font-size: 15px;
  font-weight: 600;
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

.dish-option {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  width: 100%;
  height: 48px;
  min-height: 48px;
  padding: 0 26px 0 0;
  box-sizing: border-box;
}

/* 下拉项默认高度只有 34px，无法容纳图片和两行营养信息 */
:global(.dish-option-popper .el-select-dropdown__item) {
  height: 64px !important;
  min-height: 64px !important;
  line-height: normal !important;
  padding: 5px 12px;
  box-sizing: border-box;
  overflow: hidden !important;
}

:global(.dish-option-popper .el-select-dropdown__item.hover),
:global(.dish-option-popper .el-select-dropdown__item:hover) {
  background: #f4f7f6;
}

/* 菜品信息较多，给价格和右侧选中状态留出足够空间 */
:global(.dish-option-popper) {
  min-width: 620px !important;
}

:global(.dish-option-popper .dish-option) {
  height: 52px;
  min-height: 52px;
}

:global(.dish-option-popper .dish-option-image),
:global(.dish-option-popper .dish-option-placeholder) {
  width: 48px;
  height: 48px;
  flex-basis: 48px;
}

.dish-option-image,
.dish-option-placeholder {
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  border-radius: 6px;
}

.dish-option-image {
  object-fit: cover;
  border: 1px solid var(--bs-border-light);
}

.dish-option-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eef3f0;
  color: var(--bs-primary);
  font-weight: 600;
}

.dish-option-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.dish-option-name {
  color: var(--bs-text-title);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dish-option-meta {
  color: var(--bs-text-muted);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dish-option-price {
  align-self: center;
  white-space: nowrap;
  color: var(--bs-text-title);
  font-weight: 600;
  font-size: 13px;
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

.col-name { flex: 1; min-width: 0; font-weight: 500; }
.col-qty { width: 120px; }
.col-fixed { width: 150px; }
.col-action { width: 60px; text-align: right; }

.dish-mini-card {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  max-width: 100%;
}

.dish-mini-image,
.dish-mini-placeholder {
  width: 46px;
  height: 46px;
  flex: 0 0 46px;
  border-radius: 6px;
}

.dish-mini-image {
  object-fit: cover;
  border: 1px solid var(--bs-border-light);
}

.dish-mini-placeholder {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #eef3f0;
  color: var(--bs-primary);
  font-weight: 600;
}

.dish-mini-main {
  min-width: 0;
  display: inline-flex;
  flex-direction: column;
  gap: 3px;
}

.dish-mini-name,
.dish-mini-meta {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dish-mini-name {
  color: var(--bs-text-title);
  font-weight: 600;
}

.dish-mini-meta {
  color: var(--bs-text-muted);
  font-size: 12px;
  font-weight: 400;
}

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

.replaceable-preview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 10px;
}

.replaceable-preview-card {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  padding: 10px;
  background: #fff;
  border: 1px solid var(--bs-border-light);
  border-radius: 8px;
}

.replaceable-preview-image,
.replaceable-preview-placeholder {
  width: 44px;
  height: 44px;
  flex: 0 0 44px;
  border-radius: 6px;
}

.replaceable-preview-image {
  object-fit: cover;
}

.replaceable-preview-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff4e5;
  color: #b76e2a;
  font-weight: 600;
}

.replaceable-preview-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.replaceable-preview-main span,
.replaceable-preview-main small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.replaceable-preview-main span {
  color: var(--bs-text-title);
  font-weight: 600;
}

.replaceable-preview-main small {
  color: var(--bs-text-muted);
  font-size: 12px;
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

.combo-image-upload {
  position: relative;
  width: 180px;
  height: 180px;
}

.combo-image-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
}

.combo-image-uploader {
  width: 100%;
  height: 100%;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color 0.2s;
}

.combo-image-uploader:hover {
  border-color: var(--bs-primary);
}

.upload-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #909399;
  font-size: 14px;
}

.remove-image-btn {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #f56c6c;
  color: #fff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.remove-image-btn:hover {
  background: #f78989;
}

@media (max-width: 1100px) {
  .summary-grid,
  .combo-builder-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .page-head,
  .toolbar,
  .add-dish-row {
    align-items: flex-start;
    flex-direction: column;
  }

  .head-actions,
  .search-input,
  .dish-add-select {
    width: 100%;
  }

  .summary-grid,
  .combo-builder-summary {
    grid-template-columns: 1fr;
  }

  .toolbar-count {
    margin-left: 0;
  }
}
</style>
