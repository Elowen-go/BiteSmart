<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Plus } from '@element-plus/icons-vue'
import { getCategoryList, addCategory, updateCategory, deleteCategory } from '../../../api/merchant/categories'
import type { DishCategory } from '../../../api/merchant/categories'
import { getDishList } from '../../../api/merchant/dishes'

const loading = ref(false)
const list = ref<DishCategory[]>([])
const dishCountMap = ref<Record<number, number>>({})
const dialogVisible = ref(false)
const dialogTitle = ref('新增分类')
const editingId = ref<number | null>(null)
const form = ref({ categoryName: '', sortOrder: 0 })
const formRules = { categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }] }

const loadData = async () => {
  loading.value = true
  try {
    const [catRes, dishRes] = await Promise.all([
      getCategoryList(),
      getDishList({ page: 1, size: 100 })
    ])
    if (catRes.code === 200) list.value = catRes.data || []
    if (dishRes.code === 200) {
      const dishes = dishRes.data.list || dishRes.data || []
      const map: Record<number, number> = {}
      dishes.forEach((dish: any) => {
        if (dish.categoryId != null) map[dish.categoryId] = (map[dish.categoryId] || 0) + 1
      })
      dishCountMap.value = map
    }
  } catch (e) {
    console.error('获取分类失败', e)
  } finally {
    loading.value = false
  }
}

const dishCount = (row: any) => dishCountMap.value[Number(row.id)] || 0

const handleAdd = () => {
  dialogTitle.value = '新增分类'
  editingId.value = null
  form.value = { categoryName: '', sortOrder: 0 }
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  dialogTitle.value = '编辑分类'
  editingId.value = row.id
  form.value = { categoryName: row.categoryName, sortOrder: row.sortOrder }
  dialogVisible.value = true
}

const handleSave = async () => {
  try {
    if (editingId.value) {
      await updateCategory(editingId.value, form.value as any)
      ElMessage.success('修改成功')
    } else {
      await addCategory(form.value as any)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const handleDelete = async (row: any) => {
  const count = dishCount(row)
  try {
    await ElMessageBox.confirm(
      count > 0
        ? `该分类下有 ${count} 道菜品，删除后这些菜品将失去分类归属，确定删除「${row.categoryName}」吗？`
        : `确定要删除分类「${row.categoryName}」吗？`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteCategory(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) { /* 取消删除 */ }
}

onMounted(loadData)
</script>

<template>
  <div class="page-container">
    <div class="category-page">
      <div class="page-head">
        <div>
          <h2>分类管理</h2>
          <p>维护菜单分类与展示顺序，删除前请确认分类下的菜品数量</p>
        </div>
        <div class="page-head-actions">
          <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增分类</el-button>
        </div>
      </div>

      <div class="card-panel">
        <el-table :data="list" v-loading="loading" empty-text="暂无分类">
          <el-table-column prop="categoryName" label="分类名称" min-width="180">
            <template #default="{ row }"><span class="category-name">{{ row.categoryName }}</span></template>
          </el-table-column>
          <el-table-column label="菜品数" width="100" align="center">
            <template #default="{ row }">{{ dishCount(row) }}</template>
          </el-table-column>
          <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
          <el-table-column prop="createTime" label="创建时间" min-width="170">
            <template #default="{ row }">{{ row.createTime?.slice(0, 16) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right" align="right">
            <template #default="{ row }">
              <el-button size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="form.categoryName" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="排序序号">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
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

.category-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
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

.page-head-actions {
  display: flex;
  gap: 8px;
}

.card-panel {
  background: var(--bs-card-bg);
  border-radius: var(--bs-radius-md);
  box-shadow: var(--bs-card-shadow);
  padding: var(--bs-spacing-lg);
}

.category-name {
  color: var(--bs-text-title);
  font-weight: 600;
}

@media (max-width: 720px) {
  .page-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }
}
</style>
