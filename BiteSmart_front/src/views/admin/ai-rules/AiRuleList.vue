<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listAiRules, addAiRule, updateAiRule, deleteAiRule } from '../../../api/admin/ai-rule'
import type { AiRecommendRule } from '../../../api/admin/ai-rule'

const loading = ref(false)
const ruleList = ref<AiRecommendRule[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('')
const editingId = ref<number | null>(null)

const form = ref({
  ruleName: '',
  description: '',
  ruleType: '',
  ruleConfig: '',
  enabled: true,
  priority: 0
})

const fetchList = async () => {
  loading.value = true
  try {
    const res = await listAiRules()
    if (res.code === 200) {
      ruleList.value = res.data || []
    }
  } catch (e) {
    console.error('获取AI规则列表失败', e)
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  editingId.value = null
  dialogTitle.value = '新增AI规则'
  form.value = { ruleName: '', description: '', ruleType: '', ruleConfig: '', enabled: true, priority: 0 }
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  editingId.value = row.id
  dialogTitle.value = '编辑AI规则'
  form.value = {
    ruleName: row.ruleName,
    description: row.description || '',
    ruleType: row.ruleType || '',
    ruleConfig: row.ruleConfig || '',
    enabled: row.enabled !== false,
    priority: row.priority || 0
  }
  dialogVisible.value = true
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(`确定要删除规则「${row.ruleName}」吗？`, '删除确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await deleteAiRule(row.id)
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
      res = await updateAiRule(editingId.value, form.value as any)
    } else {
      res = await addAiRule(form.value as any)
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

const handleToggleEnabled = async (row: any) => {
  try {
    const res = await updateAiRule(row.id, { enabled: !row.enabled } as any)
    if (res.code === 200) {
      row.enabled = !row.enabled
      ElMessage.success(row.enabled ? '规则已启用' : '规则已禁用')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const ruleTypeMap: Record<string, string> = { RECOMMEND: '推荐', FILTER: '过滤', SORT: '排序' }
const getRuleTypeLabel = (type: string) => ruleTypeMap[type] || type

onMounted(fetchList)
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>AI规则管理</h3>
        <button class="btn btn-primary" @click="handleAdd">新增规则</button>
      </div>
      <div style="padding-top: 20px;">
        <el-table :data="ruleList" border v-loading="loading">
          <el-table-column prop="ruleName" label="规则名称" min-width="160" />
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column label="规则类型" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ getRuleTypeLabel(row.ruleType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="启用" width="80" align="center">
            <template #default="{ row }">
              <el-switch :model-value="row.enabled" @change="handleToggleEnabled(row)" />
            </template>
          </el-table-column>
          <el-table-column prop="priority" label="优先级" width="80" align="center" />
          <el-table-column label="更新时间" width="170">
            <template #default="{ row }">
              {{ row.updateTime?.slice(0, 16) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="规则名称">
          <el-input v-model="form.ruleName" placeholder="请输入规则名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入规则描述" />
        </el-form-item>
        <el-form-item label="规则类型">
          <el-select v-model="form.ruleType" placeholder="请选择规则类型" style="width: 100%;">
            <el-option label="推荐" value="RECOMMEND" />
            <el-option label="过滤" value="FILTER" />
            <el-option label="排序" value="SORT" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则配置">
          <el-input v-model="form.ruleConfig" type="textarea" :rows="3" placeholder="请输入规则配置(JSON)" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="form.priority" :min="0" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
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
