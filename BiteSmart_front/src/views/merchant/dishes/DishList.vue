<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDishList, addDish, updateDish, deleteDish } from '../../../api/merchant/dishes'
import type { Dish } from '../../../api/merchant/dishes'

const loading = ref(false)
const dishList = ref<Dish[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()
const editingId = ref<number | null>(null)

const form = ref({
  name: '',
  price: 0,
  stock: 0,
  status: 1,
  description: '',
  categoryId: 0,
  imageUrl: '',
  calories: 0
})

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getDishList({ page: page.value, size: size.value })
    if (res.code === 200) {
      dishList.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    console.error('获取菜品列表失败', e)
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  editingId.value = null
  dialogTitle.value = '新增菜品'
  form.value = { name: '', price: 0, stock: 0, status: 1, description: '', categoryId: 0, imageUrl: '', calories: 0 }
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  editingId.value = row.id
  dialogTitle.value = '编辑菜品'
  form.value = {
    name: row.name,
    price: row.price,
    stock: row.stock,
    status: row.status,
    description: row.description || '',
    categoryId: row.categoryId || 0,
    imageUrl: row.imageUrl || '',
    calories: row.calories || 0
  }
  dialogVisible.value = true
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定要删除菜品「${row.name}」吗？`, '删除确认', {
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

const handleSubmit = async () => {
  loading.value = true
  try {
    let res: any
    if (editingId.value) {
      res = await updateDish(editingId.value, form.value as any)
    } else {
      res = await addDish(form.value as any)
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
    const newStatus = row.status === 1 ? 0 : 1
    const res = await updateDish(row.id, { ...row, status: newStatus } as any)
    if (res.code === 200) {
      ElMessage.success(newStatus === 1 ? '已上架' : '已下架')
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
  return status === 1 ? 'success' : 'danger'
}

const getStatusLabel = (status: number) => {
  return status === 1 ? '上架' : '下架'
}

onMounted(() => {
  fetchList()
})
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>菜品管理</h3>
        <button class="btn btn-primary" @click="handleAdd">添加菜品</button>
      </div>
      <div style="padding-top: 20px;">
        <el-table :data="dishList" border v-loading="loading">
          <el-table-column prop="name" label="菜品名称" />
          <el-table-column prop="price" label="价格" width="120">
            <template #default="{ row }">
              ¥{{ row.price }}
            </template>
          </el-table-column>
          <el-table-column prop="stock" label="库存" width="100" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusTag(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button size="small" @click="handleToggleStatus(row)">
                {{ row.status === 1 ? '下架' : '上架' }}
              </el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="菜品名称">
          <el-input v-model="form.name" placeholder="请输入菜品名称" />
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="form.stock" :min="0" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">上架</el-radio>
            <el-radio :value="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜品描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入菜品描述" />
        </el-form-item>
        <el-form-item label="图片URL">
          <el-input v-model="form.imageUrl" placeholder="请输入图片URL" />
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

