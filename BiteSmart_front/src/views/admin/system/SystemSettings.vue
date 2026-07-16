<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getConfigList, addConfig, updateConfig, deleteConfig } from '../../../api/admin/system'
import type { SysConfig } from '../../../api/admin/system'
import ListState from '../../../components/common/ListState.vue'

const loading = ref(false)
const tableData = ref<any[]>([])
const loadError = ref('')

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formData = ref<Partial<SysConfig>>({
  configKey: '',
  configValue: '',
  description: '',
  group: '',
  isSensitive: 0
})
const isEdit = ref(false)

const loadData = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const res = await getConfigList()
    if (res.code === 200) {
      tableData.value = res.data.list || res.data || []
    } else {
      loadError.value = res.message || '系统配置暂时无法获取'
    }
  } catch (err) {
    loadError.value = '请检查网络连接后重试'
    console.error('获取配置列表失败', err)
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增配置'
  formData.value = { configKey: '', configValue: '', description: '', group: '', isSensitive: 0 }
  dialogVisible.value = true
}

const handleEdit = (row: any) => {
  isEdit.value = true
  dialogTitle.value = '编辑配置'
  formData.value = { ...row }
  dialogVisible.value = true
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm('确定要删除该配置吗？', '确认删除', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      const res = await deleteConfig(row.id)
      if (res.code === 200) {
        ElMessage.success('配置已删除')
        loadData()
      }
    } catch (err) {
      console.error('删除配置失败', err)
    }
  }).catch(() => {})
}

const handleSave = async () => {
  if (!formData.value.configKey || !formData.value.configValue) {
    ElMessage.warning('请填写配置键和配置值')
    return
  }
  try {
    let res
    if (isEdit.value) {
      res = await updateConfig(
        formData.value.configKey!,
        formData.value.configValue!,
        formData.value.description || undefined,
        formData.value.isSensitive
      )
    } else {
      res = await addConfig(formData.value as SysConfig)
    }
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '配置已更新' : '配置已新增')
      dialogVisible.value = false
      loadData()
    }
  } catch (err) {
    console.error('保存配置失败', err)
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>系统设置</h3>
        <el-button type="primary" @click="handleAdd">新增配置</el-button>
      </div>
      <div style="padding-top: 20px;">
        <ListState :loading="loading" :error="loadError" :empty="!tableData.length" empty-text="暂无系统配置" @retry="loadData">
        <el-table :data="tableData" border stripe style="width: 100%">
          <el-table-column prop="configKey" label="配置键" min-width="160" />
          <el-table-column prop="configValue" label="配置值" min-width="200" show-overflow-tooltip />
          <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
          <el-table-column prop="group" label="分组" width="120" />
          <el-table-column label="敏感" width="80">
            <template #default="{ row }">
              <el-tag :type="row.isSensitive === 1 ? 'danger' : 'info'" size="small">
                {{ row.isSensitive === 1 ? '是' : '否' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="更新时间" width="170">
            <template #default="{ row }">
              {{ row.updateTime?.slice(0, 16) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" link @click="handleEdit(row)">编辑</el-button>
              <el-button size="small" type="danger" link @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        </ListState>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="550px">
      <el-form :model="formData" label-width="100px">
        <el-form-item label="配置键" required>
          <el-input
            v-model="formData.configKey"
            placeholder="请输入配置键"
            :disabled="isEdit"
          />
        </el-form-item>
        <el-form-item label="配置值" required>
          <el-input
            v-model="formData.configValue"
            type="textarea"
            :rows="2"
            placeholder="请输入配置值"
          />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="formData.description"
            placeholder="请输入配置描述"
          />
        </el-form-item>
        <el-form-item label="分组">
          <el-input
            v-model="formData.group"
            placeholder="请输入分组名称"
          />
        </el-form-item>
        <el-form-item label="敏感配置">
          <el-switch
            v-model="formData.isSensitive"
            :active-value="1"
            :inactive-value="0"
            active-text="是"
            inactive-text="否"
          />
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
</style>
