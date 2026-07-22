<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getDeliveryTasks } from '../../../api/merchant/delivery'
import type { DeliveryTask } from '../../../api/merchant/delivery'

const loading = ref(false)
const taskList = ref<DeliveryTask[]>([])
const activeStatus = ref<number | 'all'>('all')

const statusConfig: Record<number, { label: string; tag: 'success' | 'warning' | 'primary' | 'info' | 'danger'; className: string }> = {
  10: { label: '待接单', tag: 'info', className: 'muted' },
  20: { label: '待取餐', tag: 'primary', className: 'pickup' },
  30: { label: '已取餐', tag: 'primary', className: 'pickup' },
  40: { label: '配送中', tag: 'warning', className: 'delivery' },
  50: { label: '已送达', tag: 'success', className: 'done' },
  60: { label: '异常', tag: 'danger', className: 'danger' },
  70: { label: '已取消', tag: 'info', className: 'muted' }
}

const filteredTaskList = computed(() => {
  if (activeStatus.value === 'all') return taskList.value
  return taskList.value.filter((item) => item.taskStatus === activeStatus.value)
})

const taskStats = computed(() => {
  const countByStatus = (status: number) => taskList.value.filter((item) => item.taskStatus === status).length
  return [
    { label: '待取餐', value: countByStatus(20), status: 20, hint: '出餐后等待骑手' },
    { label: '配送中', value: countByStatus(40), status: 40, hint: '关注送达进度' },
    { label: '异常', value: countByStatus(60), status: 60, hint: '需要及时处理' },
    { label: '已送达', value: countByStatus(50), status: 50, hint: '累计完成配送' }
  ]
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getDeliveryTasks()
    if (res.code === 200) {
      taskList.value = res.data.list || res.data || []
    }
  } catch (e) {
    console.error('获取配送任务失败', e)
  } finally {
    loading.value = false
  }
}

const getTaskStatusTag = (status: number): 'success' | 'warning' | 'primary' | 'info' | 'danger' => {
  return statusConfig[status]?.tag || 'info'
}

const getTaskStatusLabel = (status: number) => {
  return statusConfig[status]?.label || '未知'
}

const getStatusClass = (status: number) => {
  return statusConfig[status]?.className || 'muted'
}

// 再次点击当前状态卡恢复“全部”
const handleStatusChange = (status: number | 'all') => {
  activeStatus.value = activeStatus.value === status ? 'all' : status
}

const detailVisible = ref(false)
const currentTask = ref<DeliveryTask | null>(null)

const openDetail = (task: any) => {
  currentTask.value = task as DeliveryTask
  detailVisible.value = true
}

const formatText = (value?: string) => {
  return value || '-'
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="page-container">
    <div class="delivery-page">
      <div class="page-head">
        <div>
          <h2>配送管理</h2>
          <p>查看出餐后的取餐、配送、送达和异常状态</p>
        </div>
        <el-button :icon="Refresh" :loading="loading" @click="fetchData">刷新</el-button>
      </div>

      <div class="summary-grid">
        <button
          v-for="item in taskStats"
          :key="item.label"
          class="summary-item"
          :class="[getStatusClass(item.status), { active: activeStatus === item.status }]"
          @click="handleStatusChange(item.status)"
        >
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <em>{{ item.hint }}</em>
        </button>
      </div>

      <div class="card-panel delivery-panel">
        <div class="toolbar">
          <span class="toolbar-hint">点击上方状态卡筛选，再次点击恢复全部</span>
          <span class="toolbar-count">当前 {{ filteredTaskList.length }} 条配送任务</span>
        </div>

        <el-table :data="filteredTaskList" v-loading="loading" class="delivery-table" empty-text="暂无符合条件的配送任务">
          <el-table-column prop="orderNo" label="订单信息" min-width="220">
            <template #default="{ row }">
              <div class="order-no">{{ row.orderNo }}</div>
              <div class="sub-text">创建 {{ formatText(row.createTime) }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="driverName" label="配送员" min-width="150">
            <template #default="{ row }">
              <div class="strong-text">{{ formatText(row.driverName) }}</div>
              <div class="sub-text">{{ formatText(row.driverPhone) }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="taskStatus" label="配送状态" width="120">
            <template #default="{ row }">
              <el-tag :type="getTaskStatusTag(row.taskStatus)" :effect="row.taskStatus === 60 ? 'dark' : 'plain'">{{ getTaskStatusLabel(row.taskStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="pickupCode" label="取餐码" width="120" />
          <el-table-column prop="estimatedDeliveryTime" label="预计送达" min-width="170">
            <template #default="{ row }">{{ formatText(row.estimatedDeliveryTime) }}</template>
          </el-table-column>
          <el-table-column prop="pickupTime" label="取餐时间" min-width="170">
            <template #default="{ row }">{{ formatText(row.pickupTime) }}</template>
          </el-table-column>
          <el-table-column prop="deliverTime" label="送达时间" min-width="170">
            <template #default="{ row }">{{ formatText(row.deliverTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="80" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" size="small" @click="openDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-dialog v-model="detailVisible" title="配送任务详情" width="480px">
        <div v-if="currentTask" class="task-detail">
          <div class="detail-row">
            <span>订单编号</span>
            <strong>{{ currentTask.orderNo }}</strong>
          </div>
          <div class="detail-row">
            <span>配送状态</span>
            <el-tag :type="getTaskStatusTag(currentTask.taskStatus)" :effect="currentTask.taskStatus === 60 ? 'dark' : 'plain'" size="small">
              {{ getTaskStatusLabel(currentTask.taskStatus) }}
            </el-tag>
          </div>
          <div class="detail-row">
            <span>取餐码</span>
            <strong>{{ formatText(currentTask.pickupCode) }}</strong>
          </div>
          <div class="detail-row">
            <span>配送员</span>
            <strong>{{ formatText(currentTask.driverName) }}</strong>
          </div>
          <div class="detail-row">
            <span>联系电话</span>
            <a v-if="currentTask.driverPhone" class="phone-link" :href="`tel:${currentTask.driverPhone}`">{{ currentTask.driverPhone }}</a>
            <strong v-else>-</strong>
          </div>
          <div class="detail-row">
            <span>预计送达</span>
            <strong>{{ formatText(currentTask.estimatedDeliveryTime) }}</strong>
          </div>
          <div class="detail-row">
            <span>取餐时间</span>
            <strong>{{ formatText(currentTask.pickupTime) }}</strong>
          </div>
          <div class="detail-row">
            <span>送达时间</span>
            <strong>{{ formatText(currentTask.deliverTime) }}</strong>
          </div>
          <div class="detail-row">
            <span>创建时间</span>
            <strong>{{ formatText(currentTask.createTime) }}</strong>
          </div>
        </div>
        <p v-if="currentTask?.taskStatus === 60" class="detail-tip">异常任务暂无线上处理入口，请电话联系骑手核实情况后协调处理。</p>
        <template #footer>
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button v-if="currentTask?.driverPhone" type="primary" tag="a" :href="`tel:${currentTask.driverPhone}`">联系骑手</el-button>
        </template>
      </el-dialog>
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

.delivery-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-head h2 {
  margin: 0;
  color: var(--bs-text-title);
  font-size: 20px;
  font-weight: 650;
}

.page-head p {
  margin-top: 4px;
  color: var(--bs-text-muted);
  font-size: 13px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.summary-item {
  min-height: 104px;
  padding: 16px;
  text-align: left;
  background: #fff;
  border: 1px solid var(--bs-border-light);
  border-radius: var(--bs-radius-md);
  box-shadow: var(--bs-card-shadow);
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, transform 0.15s ease;
}

.summary-item:hover,
.summary-item.active {
  border-color: rgba(27, 58, 47, 0.35);
  box-shadow: var(--bs-card-shadow-hover);
  transform: translateY(-1px);
}

.summary-item span,
.summary-item em {
  display: block;
  color: var(--bs-text-muted);
  font-size: 13px;
  font-style: normal;
}

.summary-item strong {
  display: block;
  margin: 6px 0 4px;
  color: var(--bs-text-title);
  font-size: 28px;
  line-height: 1.1;
}

.summary-item.pickup {
  border-left: 3px solid var(--bs-primary);
}

.summary-item.delivery {
  border-left: 3px solid var(--bs-status-warning);
}

.summary-item.danger {
  border-left: 3px solid var(--bs-status-danger);
}

.summary-item.done {
  border-left: 3px solid var(--bs-status-success);
}

.card-panel {
  background: var(--bs-card-bg);
  border-radius: var(--bs-radius-md);
  box-shadow: var(--bs-card-shadow);
  padding: var(--bs-spacing-lg);
}

.delivery-panel {
  padding-top: 16px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.toolbar-count {
  color: var(--bs-text-muted);
  font-size: 13px;
  white-space: nowrap;
}

.toolbar-hint {
  color: var(--bs-text-muted);
  font-size: 12px;
}

.task-detail {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  font-size: 13px;
}

.detail-row span {
  flex-shrink: 0;
  color: var(--bs-text-muted);
}

.detail-row strong {
  color: var(--bs-text-title);
  font-weight: 600;
  text-align: right;
}

.phone-link {
  color: var(--green);
  font-weight: 600;
  text-decoration: none;
}

.phone-link:hover {
  text-decoration: underline;
}

.detail-tip {
  margin: 14px 0 0;
  padding: 8px 10px;
  background: var(--orange-soft);
  border-radius: var(--bs-radius-sm);
  color: var(--danger-brand);
  font-size: 12px;
  line-height: 1.6;
}

.delivery-table {
  width: 100%;
}

.order-no,
.strong-text {
  color: var(--bs-text-title);
  font-weight: 600;
}

.sub-text {
  margin-top: 4px;
  color: var(--bs-text-muted);
  font-size: 12px;
}

@media (max-width: 1100px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .page-head,
  .toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
