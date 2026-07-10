<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDriverList, updateDriverStatus } from '../../../api/admin/drivers'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getDriverList({ pageNum: pageNum.value, pageSize: pageSize.value })
    if (res.code === 200) {
      tableData.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } catch (err) {
    console.error('获取配送员列表失败', err)
  } finally {
    loading.value = false
  }
}

const statusMap: Record<number, string> = { 10: '在线', 20: '忙碌', 30: '离线', 40: '冻结' }
const statusTypeMap: Record<number, 'success' | 'warning' | 'info' | 'danger'> = { 10: 'success', 20: 'warning', 30: 'info', 40: 'danger' }

const handleToggleFreeze = async (row: any) => {
  const newStatus = row.status === 40 ? 10 : 40
  try {
    const res = await updateDriverStatus(row.id, newStatus)
    if (res.code === 200) {
      row.status = newStatus
      ElMessage.success(newStatus === 40 ? '配送员已冻结' : '配送员已解冻')
    }
  } catch (err) {
    console.error('更新配送员状态失败', err)
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>配送员管理</h3>
      </div>
      <div style="padding-top: 20px;">
        <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
          <el-table-column prop="realName" label="姓名" min-width="120" />
          <el-table-column prop="phone" label="手机号" width="140" />
          <el-table-column prop="vehicleType" label="交通工具" width="120" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTypeMap[row.status] || 'info'" size="small">
                {{ statusMap[row.status] || '未知' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button
                size="small"
                :type="row.status === 40 ? 'success' : 'warning'"
                link
                @click="handleToggleFreeze(row)"
              >
                {{ row.status === 40 ? '解冻' : '冻结' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
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
