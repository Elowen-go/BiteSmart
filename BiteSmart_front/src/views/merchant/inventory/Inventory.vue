<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getInventoryWarnings, getInventoryLogs } from '../../../api/merchant/inventory'
import type { InventoryWarning, InventoryLog } from '../../../api/merchant/inventory'

const loading = ref(false)
const warnings = ref<InventoryWarning[]>([])
const logs = ref<InventoryLog[]>([])
const activeTab = ref('warnings')

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

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>库存管理</h3>
      </div>
      <el-tabs v-model="activeTab" style="padding-top: 10px;">
        <el-tab-pane label="库存预警" name="warnings">
          <el-table :data="warnings" border v-loading="loading">
            <el-table-column prop="dishName" label="菜品名称" />
            <el-table-column prop="currentStock" label="当前库存" width="120" />
            <el-table-column prop="minStock" label="预警值" width="120" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.currentStock <= row.minStock" type="danger">库存不足</el-tag>
                <el-tag v-else type="success">正常</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="库存变动日志" name="logs">
          <el-table :data="logs" border v-loading="loading">
            <el-table-column prop="dishName" label="菜品名称" />
            <el-table-column prop="changeType" label="变动类型" width="120" />
            <el-table-column prop="changeAmount" label="变动数量" width="120">
              <template #default="{ row }">
                <span :style="{ color: row.changeAmount > 0 ? '#2D8F5C' : '#D9534F' }">
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

