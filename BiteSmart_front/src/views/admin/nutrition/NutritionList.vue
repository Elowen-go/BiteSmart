<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listNutrition, addNutrition, updateNutrition, deleteNutrition } from '../../../api/admin/nutrition'
import type { NutritionStandard } from '../../../api/admin/nutrition'
import ListState from '../../../components/common/ListState.vue'

const loading = ref(false)
const nutritionList = ref<NutritionStandard[]>([])
const loadError = ref('')
const dialogVisible = ref(false)
const dialogTitle = ref('')
const editingId = ref<number | null>(null)

const form = ref({
  standardName: '',
  description: '',
  standardType: '',
  minValue: 0,
  maxValue: 0,
  unit: '',
  priority: 0
})

const fetchList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const res = await listNutrition()
    if (res.code === 200) {
      nutritionList.value = res.data || []
    } else {
      loadError.value = res.message || '营养标准列表暂时无法获取'
    }
  } catch (e) {
    loadError.value = '请检查网络连接后重试'
    console.error('获取营养标准列表失败', e)
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  editingId.value = null
  dialogTitle.value = '新增营养标准'
  form.value = { standardName: '', description: '', standardType: '', minValue: 0, maxValue: 0, unit: '', priority: 0 }
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  editingId.value = row.id
  dialogTitle.value = '编辑营养标准'
  form.value = {
    standardName: row.standardName,
    description: row.description || '',
    standardType: row.standardType || '',
    minValue: row.minValue || 0,
    maxValue: row.maxValue || 0,
    unit: row.unit || '',
    priority: row.priority || 0
  }
  dialogVisible.value = true
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定要删除营养标准「${row.standardName}」吗？`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await deleteNutrition(row.id)
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
  loading.value = true
  try {
    let res: any
    if (editingId.value) {
      res = await updateNutrition(editingId.value, form.value as any)
    } else {
      res = await addNutrition(form.value as any)
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

const standardTypeMap: Record<string, string> = { ENERGY: '能量', PROTEIN: '蛋白质', FAT: '脂肪', CARB: '碳水', FIBER: '膳食纤维', VITAMIN: '维生素', MINERAL: '矿物质' }
const getStandardTypeLabel = (type: string) => standardTypeMap[type] || type

onMounted(fetchList)
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>营养标准管理</h3>
        <button class="btn btn-primary" @click="handleAdd">新增标准</button>
      </div>
      <div style="padding-top: 20px;">
        <ListState :loading="loading" :error="loadError" :empty="!nutritionList.length" empty-text="暂无营养标准" @retry="fetchList">
        <el-table :data="nutritionList" border>
          <el-table-column prop="standardName" label="标准名称" min-width="160" />
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column label="类型" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ getStandardTypeLabel(row.standardType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="范围" width="160">
            <template #default="{ row }">
              {{ row.minValue }} ~ {{ row.maxValue }} {{ row.unit }}
            </template>
          </el-table-column>
          <el-table-column prop="unit" label="单位" width="80" align="center" />
          <el-table-column prop="priority" label="优先级" width="80" align="center" />
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        </ListState>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="标准名称">
          <el-input v-model="form.standardName" placeholder="请输入标准名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入标准描述" />
        </el-form-item>
        <el-form-item label="标准类型">
          <el-select v-model="form.standardType" placeholder="请选择标准类型" style="width: 100%;">
            <el-option label="能量" value="ENERGY" />
            <el-option label="蛋白质" value="PROTEIN" />
            <el-option label="脂肪" value="FAT" />
            <el-option label="碳水" value="CARB" />
            <el-option label="膳食纤维" value="FIBER" />
            <el-option label="维生素" value="VITAMIN" />
            <el-option label="矿物质" value="MINERAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="最小值">
          <el-input-number v-model="form.minValue" :min="0" :precision="1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="最大值">
          <el-input-number v-model="form.maxValue" :min="0" :precision="1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="form.unit" placeholder="如：kcal, g" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="form.priority" :min="0" style="width: 100%;" />
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
