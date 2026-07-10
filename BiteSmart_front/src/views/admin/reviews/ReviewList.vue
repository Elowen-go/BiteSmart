<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getReviewList, updateReviewStatus } from '../../../api/admin/reviews'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getReviewList({ pageNum: pageNum.value, pageSize: pageSize.value })
    if (res.code === 200) {
      tableData.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } catch (err) {
    console.error('获取评价列表失败', err)
  } finally {
    loading.value = false
  }
}

const statusMap: Record<number, string> = { 10: '已发布', 20: '已隐藏', 30: '违规删除' }
const statusTypeMap: Record<number, 'success' | 'warning' | 'danger'> = { 10: 'success', 20: 'warning', 30: 'danger' }

const handleToggleHide = async (row: any) => {
  const newStatus = row.status === 10 ? 20 : 10
  try {
    const res = await updateReviewStatus(row.id, newStatus)
    if (res.code === 200) {
      row.status = newStatus
      ElMessage.success(newStatus === 20 ? '评价已隐藏' : '评价已显示')
    }
  } catch (err) {
    console.error('更新评价状态失败', err)
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>评论管理</h3>
      </div>
      <div style="padding-top: 20px;">
        <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
          <el-table-column label="评分" width="80">
            <template #default="{ row }">
              <el-rate v-model="row.rating" disabled size="small" />
            </template>
          </el-table-column>
          <el-table-column prop="content" label="评价内容" min-width="260" show-overflow-tooltip />
          <el-table-column prop="username" label="用户" width="130" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTypeMap[row.status] || 'info'" size="small">
                {{ statusMap[row.status] || '未知' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="170">
            <template #default="{ row }">
              {{ row.createTime?.slice(0, 16) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button
                size="small"
                :type="row.status === 20 ? 'success' : 'warning'"
                link
                @click="handleToggleHide(row)"
              >
                {{ row.status === 20 ? '显示' : '隐藏' }}
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
