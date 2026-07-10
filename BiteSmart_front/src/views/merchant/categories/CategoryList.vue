<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getCategoryList, addCategory, updateCategory, deleteCategory } from '../../../api/merchant/categories'
import type { DishCategory } from '../../../api/merchant/categories'

const loading = ref(false)
const list = ref<DishCategory[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增分类')
const editingId = ref<number | null>(null)
const form = ref({ categoryName: '', sortOrder: 0 })
const formRules = { categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }] }

const loadData = async () => {
  loading.value = true
  try {
    const res = await getCategoryList()
    if (res.code === 200) list.value = res.data || []
  } catch (e) {
    console.error('获取分类失败', e)
  } finally {
    loading.value = false
  }
}

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

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除该分类吗？', '提示')
    await deleteCategory(id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) { /* 取消删除 */ }
}

onMounted(loadData)
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>分类管理</h3>
        <button class="btn btn-primary" @click="handleAdd">新增分类</button>
      </div>
      <div style="padding-top: 20px;">
        <el-table :data="list" v-loading="loading" border stripe>
          <el-table-column prop="categoryName" label="分类名称" min-width="180" />
          <el-table-column prop="sortOrder" label="排序" width="80" />
          <el-table-column prop="createTime" label="创建时间" min-width="170">
            <template #default="{ row }">{{ row.createTime?.slice(0, 16) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
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
.page-container { animation: fadeIn 0.3s ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
.card-panel { background: var(--bs-card-bg); border-radius: var(--bs-radius-md); box-shadow: var(--bs-card-shadow); padding: var(--bs-spacing-lg); }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--bs-spacing-lg); }
.card-header h3 { font-size: var(--bs-font-size-lg); font-weight: 600; color: var(--bs-text-title); }
.btn { display: inline-flex; align-items: center; gap: 6px; padding: var(--bs-spacing-sm) 20px; border-radius: var(--bs-radius-md); font-size: var(--bs-font-size-base); font-weight: 500; border: 1px solid transparent; cursor: pointer; transition: 0.15s; }
.btn-primary { background: var(--bs-primary); color: #FFFFFF; }
.btn-primary:hover { background: var(--bs-primary-hover); }
</style>
