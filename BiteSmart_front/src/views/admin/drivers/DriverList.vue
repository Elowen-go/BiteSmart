<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getDriverList, updateDriverStatus } from '../../../api/admin/drivers'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const statusFilter = ref<number | undefined>()
const detailVisible = ref(false)
const detailData = ref<any>(null)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getDriverList({ pageNum: pageNum.value, pageSize: pageSize.value, status: statusFilter.value })
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

const handleDetail = async (row: any) => {
  const { getDriverDetail } = await import('../../../api/admin/drivers')
  const res = await getDriverDetail(row.id)
  if (res.code === 200) {
    detailData.value = res.data
    detailVisible.value = true
  }
}

const handleFilterChange = () => {
  pageNum.value = 1
  loadData()
}

onMounted(loadData)
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <el-select v-model="statusFilter" clearable placeholder="按状态筛选" size="small" style="width: 140px" @change="handleFilterChange">
          <el-option label="在线" :value="10" />
          <el-option label="忙碌" :value="20" />
          <el-option label="离线" :value="30" />
          <el-option label="冻结" :value="40" />
        </el-select>
        <h3>配送员管理</h3>
      </div>
      <div style="padding-top: 20px;">
        <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
          <el-table-column prop="realName" label="姓名" min-width="120" />
          <el-table-column prop="phone" label="手机号" width="140" />
          <el-table-column label="交通工具" width="140"><template #default="{ row }">{{ ({ 10: '电动车', 20: '自行车', 30: '汽车' } as Record<number, string>)[row.vehicleType] || row.vehicleType || '-' }}</template></el-table-column>
          <el-table-column label="配送负载" width="130"><template #default="{ row }">{{ row.currentOrders ?? 0 }} / {{ row.maxOrders ?? 0 }}</template></el-table-column>
          <el-table-column label="评分 / 累计" width="140"><template #default="{ row }">{{ row.avgRating ?? '-' }} · {{ row.totalDeliveries ?? 0 }} 单</template></el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTypeMap[row.status] || 'info'" size="small">
                {{ statusMap[row.status] || '未知' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" link @click="handleDetail(row)">详情</el-button>
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
  <el-dialog v-model="detailVisible" title="配送员详情" width="560px">
    <el-descriptions v-if="detailData" :column="2" border>
      <el-descriptions-item label="姓名">{{ detailData.realName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="电话">{{ detailData.phone || '-' }}</el-descriptions-item>
      <el-descriptions-item label="用户 ID">{{ detailData.userId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="身份证号">{{ detailData.idCard || '-' }}</el-descriptions-item>
      <el-descriptions-item label="交通工具">{{ ({ 10: '电动车', 20: '自行车', 30: '汽车' } as Record<number, string>)[detailData.vehicleType] || detailData.vehicleType || '-' }}</el-descriptions-item>
      <el-descriptions-item label="当前订单">{{ detailData.currentOrders ?? 0 }}</el-descriptions-item>
      <el-descriptions-item label="最大接单">{{ detailData.maxOrders ?? 0 }}</el-descriptions-item>
      <el-descriptions-item label="配送范围" :span="2">{{ detailData.serviceArea || '-' }}</el-descriptions-item>
      <el-descriptions-item label="平均评分">{{ detailData.avgRating ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="累计配送">{{ detailData.totalDeliveries ?? 0 }}</el-descriptions-item>
      <el-descriptions-item label="当前位置" :span="2">{{ detailData.currentLat ?? '-' }}, {{ detailData.currentLng ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="注册时间" :span="2">{{ detailData.createTime || '-' }}</el-descriptions-item>
    </el-descriptions>
  </el-dialog>
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
