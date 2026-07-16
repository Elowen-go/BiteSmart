<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getLogList } from '../../../api/admin/system'
import ListState from '../../../components/common/ListState.vue'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loadError = ref('')

const loadData = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const res = await getLogList({ page: pageNum.value, size: pageSize.value })
    if (res.code === 200) {
      tableData.value = res.data.list || []
      total.value = res.data.total || 0
    } else {
      loadError.value = res.message || '操作日志暂时无法获取'
    }
  } catch (err) {
    loadError.value = '请检查网络连接后重试'
    console.error('获取操作日志失败', err)
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>操作日志</h3>
      </div>
      <div style="padding-top: 20px;">
        <ListState :loading="loading" :error="loadError" :empty="!tableData.length" empty-text="暂无操作日志" @retry="loadData">
        <el-table :data="tableData" border stripe style="width: 100%">
          <el-table-column prop="username" label="操作人" width="130" />
          <el-table-column prop="operation" label="操作" min-width="200" show-overflow-tooltip />
          <el-table-column prop="module" label="模块" width="120" />
          <el-table-column prop="ip" label="IP地址" width="140" />
          <el-table-column label="操作时间" width="170">
            <template #default="{ row }">
              {{ row.createTime?.slice(0, 16) }}
            </template>
          </el-table-column>
        </el-table>
        </ListState>
        <div style="display:flex;justify-content:flex-end;padding-top:16px;">
          <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="loadData"
            @size-change="loadData"
          />
        </div>
      </div>
    </div>
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
