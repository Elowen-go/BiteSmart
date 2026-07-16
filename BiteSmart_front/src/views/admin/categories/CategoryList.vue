<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listCategories, addCategory, updateCategory, deleteCategory } from '../../../api/admin/categories'
import type { DishCategory } from '../../../api/admin/categories'
import ListState from '../../../components/common/ListState.vue'

const loading = ref(false)
const categoryList = ref<DishCategory[]>([])
const loadError = ref('')
const dialogVisible = ref(false)
const dialogTitle = ref('')
const editingId = ref<number | null>(null)
const keyword = ref('')
const filteredCategories = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  return query ? categoryList.value.filter(item => item.categoryName?.toLowerCase().includes(query)) : categoryList.value
})

const form = ref({
  categoryName: '',
  categoryIcon: '',
  sortOrder: 0,
  parentId: 0
})

const fetchList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const res = await listCategories()
    if (res.code === 200) {
      categoryList.value = res.data || []
    } else {
      loadError.value = res.message || '分类列表暂时无法获取'
    }
  } catch (e) {
    loadError.value = '请检查网络连接后重试'
    console.error('获取分类列表失败', e)
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  editingId.value = null
  dialogTitle.value = '新增分类'
  form.value = { categoryName: '', categoryIcon: '', sortOrder: 0, parentId: 0 }
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  editingId.value = row.id
  dialogTitle.value = '编辑分类'
  form.value = {
    categoryName: row.categoryName,
    categoryIcon: row.categoryIcon || '',
    sortOrder: row.sortOrder || 0,
    parentId: row.parentId || 0
  }
  dialogVisible.value = true
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定要删除分类「${row.categoryName}」吗？`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await deleteCategory(row.id)
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
      res = await updateCategory(editingId.value, form.value as any)
    } else {
      res = await addCategory(form.value as any)
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

const getParentName = (parentId: number) => {
  if (!parentId || parentId === 0) return '-'
  const parent = categoryList.value.find(c => c.id === parentId)
  return parent ? parent.categoryName : '-'
}

onMounted(fetchList)
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>分类管理</h3>
        <div class="toolbar">
          <el-input v-model="keyword" clearable placeholder="搜索分类名称" style="width: 220px" />
          <button class="btn btn-primary" @click="handleAdd">新增分类</button>
        </div>
      </div>
      <div style="padding-top: 20px;">
        <ListState :loading="loading" :error="loadError" :empty="!filteredCategories.length" empty-text="暂无分类数据" @retry="fetchList">
        <el-table :data="filteredCategories" border stripe>
          <el-table-column prop="categoryName" label="分类名称" min-width="160" />
          <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
          <el-table-column label="父分类" width="140">
            <template #default="{ row }">
              {{ getParentName(row.parentId) }}
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="170">
            <template #default="{ row }">
              {{ row.createTime?.slice(0, 16) }}
            </template>
          </el-table-column>
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
        <el-form-item label="分类名称">
          <el-input v-model="form.categoryName" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.categoryIcon" placeholder="请输入图标名称或URL" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="父分类">
          <el-select v-model="form.parentId" placeholder="请选择父分类" style="width: 100%;" clearable>
            <el-option label="无（顶级分类）" :value="0" />
            <el-option v-for="c in categoryList" :key="c.id" :label="c.categoryName" :value="c.id" />
          </el-select>
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

.toolbar { display: flex; align-items: center; gap: 12px; }

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
