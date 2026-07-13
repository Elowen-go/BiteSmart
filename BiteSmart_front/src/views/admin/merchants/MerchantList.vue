<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMerchantList, auditMerchant, closeMerchant } from '../../../api/admin/merchants'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const statusFilter = ref<number | undefined>()

const auditDialogVisible = ref(false)
const closeDialogVisible = ref(false)
const currentRow = ref<any>(null)
const auditRemark = ref('')
const closeReason = ref('')

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMerchantList({ pageNum: pageNum.value, pageSize: pageSize.value, status: statusFilter.value })
    if (res.code === 200) {
      tableData.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } catch (err) {
    console.error('获取商家列表失败', err)
  } finally {
    loading.value = false
  }
}

const statusMap: Record<number, string> = { 10: '待审核', 20: '审核通过', 30: '审核驳回', 40: '已关闭' }
const statusTypeMap: Record<number, 'success' | 'warning' | 'info' | 'danger'> = { 10: 'warning', 20: 'success', 30: 'danger', 40: 'info' }

const openAuditDialog = (row: any) => {
  currentRow.value = row
  auditRemark.value = ''
  auditDialogVisible.value = true
}

const submitAudit = async (status: number) => {
  if (!currentRow.value) return
  try {
    const res = await auditMerchant(currentRow.value.id, status, auditRemark.value || undefined)
    if (res.code === 200) {
      ElMessage.success(status === 20 ? '商家已审核通过' : '商家已驳回')
      auditDialogVisible.value = false
      loadData()
    }
  } catch (err) {
    console.error('审核失败', err)
  }
}

const openCloseDialog = (row: any) => {
  currentRow.value = row
  closeReason.value = ''
  closeDialogVisible.value = true
}

const submitClose = async () => {
  if (!currentRow.value || !closeReason.value) {
    ElMessage.warning('请输入关闭原因')
    return
  }
  try {
    const res = await closeMerchant(currentRow.value.id, closeReason.value)
    if (res.code === 200) {
      ElMessage.success('商家已关闭')
      closeDialogVisible.value = false
      loadData()
    }
  } catch (err) {
    console.error('关闭商家失败', err)
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
        <el-select v-model="statusFilter" clearable placeholder="按状态筛选" size="small" style="width: 150px" @change="handleFilterChange">
          <el-option label="待审核" :value="10" />
          <el-option label="审核通过" :value="20" />
          <el-option label="审核驳回" :value="30" />
          <el-option label="已关闭" :value="40" />
        </el-select>
        <h3>商家管理</h3>
      </div>
      <div style="padding-top: 20px;">
        <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
          <el-table-column prop="shopName" label="商家名称" min-width="150" />
          <el-table-column prop="contactPhone" label="联系电话" width="130" />
          <el-table-column prop="shopAddress" label="店铺地址" min-width="200" show-overflow-tooltip />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTypeMap[row.status] || 'info'" size="small">
                {{ statusMap[row.status] || '未知' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="入驻时间" width="170">
            <template #default="{ row }">
              {{ row.createTime?.slice(0, 16) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 10"
                size="small"
                type="primary"
                @click="openAuditDialog(row)"
              >
                审核
              </el-button>
              <el-button
                v-if="row.status === 20"
                size="small"
                type="danger"
                @click="openCloseDialog(row)"
              >
                关闭
              </el-button>
              <span v-if="row.status === 40" style="color: #999; font-size: 13px;">已关闭</span>
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

    <el-dialog v-model="auditDialogVisible" title="商家审核" width="450px">
      <div style="margin-bottom: 16px;">
        <p><strong>商家名称：</strong>{{ currentRow?.shopName }}</p>
        <p><strong>联系电话：</strong>{{ currentRow?.contactPhone }}</p>
      </div>
      <el-input
        v-model="auditRemark"
        type="textarea"
        :rows="3"
        placeholder="审核备注（可选）"
        style="margin-bottom: 16px;"
      />
      <template #footer>
        <el-button @click="auditDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="submitAudit(30)">驳回</el-button>
        <el-button type="primary" @click="submitAudit(20)">审核通过</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="closeDialogVisible" title="关闭商家" width="450px">
      <div style="margin-bottom: 16px;">
        <p><strong>商家名称：</strong>{{ currentRow?.shopName }}</p>
      </div>
      <el-input
        v-model="closeReason"
        type="textarea"
        :rows="3"
        placeholder="请输入关闭原因"
        style="margin-bottom: 16px;"
      />
      <template #footer>
        <el-button @click="closeDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="submitClose">确认关闭</el-button>
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
