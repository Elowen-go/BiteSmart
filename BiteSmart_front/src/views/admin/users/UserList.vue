<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getUserList, updateUserStatus } from '../../../api/admin/users'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getUserList({ pageNum: pageNum.value, pageSize: pageSize.value })
    if (res.code === 200) {
      tableData.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } catch (err) {
    console.error('获取用户列表失败', err)
  } finally {
    loading.value = false
  }
}

const handleStatusChange = async (row: any) => {
  const newStatus = row.status === 10 ? 20 : 10
  try {
    const res = await updateUserStatus(row.id, newStatus)
    if (res.code === 200) {
      row.status = newStatus
      ElMessage.success(newStatus === 10 ? '用户已启用' : '用户已冻结')
    }
  } catch (err) {
    console.error('更新用户状态失败', err)
  }
}

const roleMap: Record<number, string> = { 10: '普通用户', 20: '商家', 30: '配送员', 40: '管理员' }
const statusMap: Record<number, string> = { 10: '正常', 20: '冻结', 30: '注销' }

onMounted(loadData)
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>用户管理</h3>
        <button class="btn btn-primary">新增用户</button>
      </div>
      <div style="padding-top: 20px;">
        <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="username" label="账号" min-width="140" />
          <el-table-column prop="nickname" label="昵称" min-width="120" />
          <el-table-column prop="phone" label="手机号" width="140" />
          <el-table-column label="角色" width="100">
            <template #default="{ row }">
              {{ roleMap[row.roleType] || row.roleType }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 10 ? 'success' : 'danger'" size="small">
                {{ statusMap[row.status] || '未知' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" min-width="170">
            <template #default="{ row }">
              {{ row.createTime?.slice(0, 16) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" link>详情</el-button>
              <el-button
                size="small"
                :type="row.status === 10 ? 'warning' : 'success'"
                link
                @click="handleStatusChange(row)"
              >
                {{ row.status === 10 ? '冻结' : '启用' }}
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
