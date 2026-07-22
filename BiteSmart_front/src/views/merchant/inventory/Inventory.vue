<script setup lang="ts">
import { computed, ref, onMounted, watch } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getInventoryWarnings, getInventoryLogs } from '../../../api/merchant/inventory'
import type { InventoryWarning, InventoryLog } from '../../../api/merchant/inventory'
import { updateDish } from '../../../api/merchant/dishes'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../../stores/user'

const userStore = useUserStore()
const loading = ref(false)
const warnings = ref<InventoryWarning[]>([])
const logs = ref<InventoryLog[]>([])
const activeTab = ref('warnings')

const sortedWarnings = computed(() => {
  return [...warnings.value].sort((a, b) => {
    const aGap = Number(a.currentStock || 0) - Number(a.minStock || 0)
    const bGap = Number(b.currentStock || 0) - Number(b.minStock || 0)
    return aGap - bGap
  })
})

const inventoryStats = computed(() => {
  const soldOut = warnings.value.filter((item) => Number(item.currentStock || 0) <= 0).length
  const lowStock = warnings.value.filter((item) => Number(item.currentStock || 0) > 0 && Number(item.currentStock || 0) <= Number(item.minStock || 0)).length
  const recentChanges = logs.value.length

  return [
    { label: '售罄菜品', value: soldOut, hint: '建议立即下架或补货', className: 'danger' },
    { label: '库存预警', value: lowStock, hint: '低于安全库存', className: 'warning' },
    { label: '预警菜品', value: warnings.value.length, hint: '需关注的菜品', className: 'brand' },
    { label: '变动记录', value: recentChanges, hint: '最近库存流水', className: 'muted' }
  ]
})

const updateBreadcrumb = () => {
  const subtitle = activeTab.value === 'warnings' ? '库存预警' : '库存变动日志'
  userStore.setBreadcrumbSubtitle(subtitle)
}

watch(activeTab, () => {
  updateBreadcrumb()
})

const fetchData = async () => {
  loading.value = true
  try {
    const [warnRes, logRes] = await Promise.all([
      getInventoryWarnings(),
      getInventoryLogs()
    ])
    if (warnRes.code === 200) {
      warnings.value = warnRes.data.list || warnRes.data || []
    }
    if (logRes.code === 200) {
      logs.value = logRes.data.list || logRes.data || []
    }
  } catch (e) {
    console.error('获取库存数据失败', e)
  } finally {
    loading.value = false
  }
}

const getStockStatus = (row: any) => {
  if (Number(row.currentStock || 0) <= 0) return { label: '已售罄', type: 'danger' as const }
  if (Number(row.currentStock || 0) <= Number(row.minStock || 0)) return { label: '库存不足', type: 'warning' as const }
  return { label: '正常', type: 'success' as const }
}

const getChangeTypeText = (type: string) => {
  const map: Record<string, string> = {
    IN: '入库',
    OUT: '出库',
    LOCK: '锁定',
    UNLOCK: '释放',
    ADJUST: '调整'
  }
  return map[type] || type || '-'
}

const getChangeTypeTag = (type: string): 'success' | 'warning' | 'info' | 'danger' => {
  const map: Record<string, 'success' | 'warning' | 'info' | 'danger'> = {
    IN: 'success',
    OUT: 'warning',
    LOCK: 'info',
    UNLOCK: 'info',
    ADJUST: 'danger'
  }
  return map[type] || 'info'
}

const replenishVisible = ref(false)
const replenishTarget = ref<InventoryWarning | null>(null)
const replenishStock = ref(0)
const replenishSaving = ref(false)

const openReplenish = (row: any) => {
  const target = row as InventoryWarning
  replenishTarget.value = target
  // 默认补到预警值的 2 倍，至少 50，商家可直接改
  replenishStock.value = Math.max(Number(target.minStock || 0) * 2, 50)
  replenishVisible.value = true
}

const submitReplenish = async () => {
  if (!replenishTarget.value) return
  replenishSaving.value = true
  try {
    // 后端无专用补货接口，复用菜品更新接口：DishMapper 为动态 update，仅提交 stock 字段即可
    const res = await updateDish(replenishTarget.value.dishId, { stock: replenishStock.value } as any)
    if (res.code === 200) {
      ElMessage.success('补货成功')
      replenishVisible.value = false
      fetchData()
    } else {
      ElMessage.error(res.message || '补货失败')
    }
  } catch (e) {
    console.error('补货失败', e)
    ElMessage.error('补货失败，请稍后重试')
  } finally {
    replenishSaving.value = false
  }
}

onMounted(() => {
  fetchData()
  updateBreadcrumb()
})
</script>

<template>
  <div class="page-container">
    <div class="inventory-page">
      <div class="page-head">
        <div>
          <h2>库存管理</h2>
          <p>跟踪菜品库存预警和库存变动记录</p>
        </div>
        <el-button :icon="Refresh" :loading="loading" @click="fetchData">刷新</el-button>
      </div>

      <div class="summary-grid">
        <div v-for="item in inventoryStats" :key="item.label" class="summary-item" :class="item.className">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <em>{{ item.hint }}</em>
        </div>
      </div>

      <div class="card-panel inventory-panel">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="库存预警" name="warnings">
          <el-table :data="sortedWarnings" v-loading="loading" empty-text="暂无库存预警">
            <el-table-column prop="dishName" label="菜品名称" min-width="180">
              <template #default="{ row }">
                <div class="dish-name">{{ row.dishName }}</div>
                <div class="sub-text">菜品 ID：{{ row.dishId }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="currentStock" label="当前库存" width="120" />
            <el-table-column prop="minStock" label="预警值" width="120" />
            <el-table-column label="库存差额" width="120">
              <template #default="{ row }">
                <span :class="Number(row.currentStock || 0) - Number(row.minStock || 0) <= 0 ? 'negative' : 'positive'">
                  {{ Number(row.currentStock || 0) - Number(row.minStock || 0) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="getStockStatus(row).type" effect="plain">{{ getStockStatus(row).label }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" size="small" @click="openReplenish(row)">去补货</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="库存变动日志" name="logs">
          <el-table :data="logs" v-loading="loading" empty-text="暂无库存变动记录">
            <el-table-column prop="dishName" label="菜品名称" min-width="180" />
            <el-table-column prop="changeType" label="变动类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getChangeTypeTag(row.changeType)" effect="plain">{{ getChangeTypeText(row.changeType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="changeAmount" label="变动数量" width="120">
              <template #default="{ row }">
                <span :class="row.changeAmount > 0 ? 'positive' : 'negative'">
                  {{ row.changeAmount > 0 ? '+' : '' }}{{ row.changeAmount }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="beforeStock" label="变动前" width="100" />
            <el-table-column prop="afterStock" label="变动后" width="100" />
            <el-table-column prop="operator" label="操作人" width="120" />
            <el-table-column prop="createTime" label="变动时间" width="180" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
      </div>

      <el-dialog v-model="replenishVisible" title="菜品补货" width="420px">
        <div v-if="replenishTarget" class="replenish-body">
          <p class="replenish-dish">{{ replenishTarget.dishName }}</p>
          <p class="replenish-meta">当前库存 {{ replenishTarget.currentStock }} · 预警值 {{ replenishTarget.minStock }}</p>
          <div class="replenish-field">
            <span>补货后库存</span>
            <el-input-number v-model="replenishStock" :min="0" :step="10" controls-position="right" />
          </div>
        </div>
        <template #footer>
          <el-button @click="replenishVisible = false">取消</el-button>
          <el-button type="primary" :loading="replenishSaving" @click="submitReplenish">确认补货</el-button>
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

.inventory-page {
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
  background: #fff;
  border: 1px solid var(--bs-border-light);
  border-radius: var(--bs-radius-md);
  box-shadow: var(--bs-card-shadow);
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

.summary-item.danger {
  border-left: 3px solid var(--bs-status-danger);
}

.summary-item.warning {
  border-left: 3px solid var(--bs-status-warning);
}

.summary-item.brand {
  border-left: 3px solid var(--green);
}

.summary-item.muted {
  border-left: 3px solid var(--bs-status-secondary);
}

.card-panel {
  background: var(--bs-card-bg);
  border-radius: var(--bs-radius-md);
  box-shadow: var(--bs-card-shadow);
  padding: var(--bs-spacing-lg);
}

.inventory-panel {
  padding-top: 12px;
}

.dish-name {
  color: var(--bs-text-title);
  font-weight: 600;
}

.sub-text {
  margin-top: 4px;
  color: var(--bs-text-muted);
  font-size: 12px;
}

.positive {
  color: var(--bs-status-success);
  font-weight: 600;
}

.negative {
  color: var(--bs-status-danger);
  font-weight: 600;
}

.replenish-dish {
  margin: 0;
  color: var(--bs-text-title);
  font-size: 15px;
  font-weight: 600;
}

.replenish-meta {
  margin: 6px 0 16px;
  color: var(--bs-text-muted);
  font-size: 12px;
}

.replenish-field {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  color: var(--bs-text-title);
  font-size: 13px;
}

@media (max-width: 1100px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .page-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
