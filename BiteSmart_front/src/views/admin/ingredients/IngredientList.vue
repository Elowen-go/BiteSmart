<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getIngredientList, addIngredient, updateIngredient, deleteIngredient, getIngredientCategories } from '../../../api/admin/ingredients'
import ListState from '../../../components/common/ListState.vue'

// 加载状态
const loading = ref(false)

// 食材列表数据
const ingredientList = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loadError = ref('')

// 分类列表
const categoryList = ref<string[]>([])

// 弹窗控制
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()
const editingId = ref<number | null>(null)

// 表单数据
const form = ref({
  name: '',
  categoryName: '',
  calories: 0,
  protein: 0,
  fat: 0,
  carbs: 0,
  status: 10
})

// 获取食材列表
const fetchList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const res = await getIngredientList({ page: page.value, size: size.value })
    if (res.code === 200) {
      // 处理嵌套数据结构 ResultVO<PageResultVO<Ingredient>>
      const pageResultVO = res.data
      const pageData = pageResultVO?.data || pageResultVO
      ingredientList.value = pageData?.list || []
      total.value = pageData?.total || 0
    } else {
      loadError.value = res.message || '食材列表暂时无法获取'
    }
  } catch (e) {
    loadError.value = '请检查网络连接后重试'
    console.error('获取食材列表失败', e)
    ElMessage.error('获取食材列表失败')
  } finally {
    loading.value = false
  }
}

// 获取分类列表
const fetchCategories = async () => {
  try {
    const res = await getIngredientCategories()
    if (res.code === 200) {
      categoryList.value = res.data || []
    }
  } catch (e) {
    console.error('获取分类列表失败', e)
  }
}

// 新增食材
const handleAdd = () => {
  editingId.value = null
  dialogTitle.value = '新增食材'
  form.value = {
    name: '',
    categoryName: '',
    calories: 0,
    protein: 0,
    fat: 0,
    carbs: 0,
    status: 10
  }
  dialogVisible.value = true
}

// 编辑食材
const handleEdit = (row: any) => {
  editingId.value = row.id
  dialogTitle.value = '编辑食材'
  form.value = {
    name: row.name,
    categoryName: row.categoryName,
    calories: row.calories || 0,
    protein: row.protein || 0,
    fat: row.fat || 0,
    carbs: row.carbs || 0,
    status: row.status
  }
  dialogVisible.value = true
}

// 删除食材
const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定要删除食材「${row.name}」吗？`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await deleteIngredient(row.id)
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

// 提交表单
const handleSubmit = async () => {
  loading.value = true
  try {
    let res: any
    if (editingId.value) {
      res = await updateIngredient(editingId.value, form.value)
    } else {
      res = await addIngredient(form.value)
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

// 分页变化
const handlePageChange = (val: number) => {
  page.value = val
  fetchList()
}

const handleSizeChange = (val: number) => {
  size.value = val
  page.value = 1
  fetchList()
}

// 获取状态标签类型
const getStatusTag = (status: number) => {
  return status === 10 ? 'success' : 'danger'
}

// 获取状态文本
const getStatusLabel = (status: number) => {
  return status === 10 ? '启用' : '禁用'
}

onMounted(() => {
  fetchList()
  fetchCategories()
})
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>食材管理</h3>
        <button class="btn btn-primary" @click="handleAdd">新增食材</button>
      </div>
      <div style="padding-top: 20px;">
        <ListState :loading="loading" :error="loadError" :empty="!ingredientList.length" empty-text="暂无食材记录" @retry="fetchList">
        <el-table :data="ingredientList" border>
          <el-table-column prop="name" label="食材名称" min-width="120" />
          <el-table-column prop="categoryName" label="分类" width="100" />
          <el-table-column prop="calories" label="热量(大卡/100g)" width="140">
            <template #default="{ row }">
              {{ row.calories || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="protein" label="蛋白质(g/100g)" width="130">
            <template #default="{ row }">
              {{ row.protein || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="fat" label="脂肪(g/100g)" width="120">
            <template #default="{ row }">
              {{ row.fat || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="carbs" label="碳水(g/100g)" width="120">
            <template #default="{ row }">
              {{ row.carbs || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="getStatusTag(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        </ListState>
        <div v-if="total > 0" style="display: flex; justify-content: flex-end; margin-top: 20px;">
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="size"
            :total="Number(total)"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="食材名称" required>
          <el-input v-model="form.name" placeholder="请输入食材名称" />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="form.categoryName" placeholder="请选择分类" style="width: 100%;">
            <el-option
              v-for="cat in categoryList"
              :key="cat"
              :label="cat"
              :value="cat"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="热量(大卡/100g)">
          <el-input-number v-model="form.calories" :min="0" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="蛋白质(g/100g)">
          <el-input-number v-model="form.protein" :min="0" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="脂肪(g/100g)">
          <el-input-number v-model="form.fat" :min="0" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="碳水(g/100g)">
          <el-input-number v-model="form.carbs" :min="0" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="10">启用</el-radio>
            <el-radio :value="20">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="loading">确定</el-button>
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
</style>
