<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getDeliveryTracking } from '../../../api/user/delivery'

const route = useRoute()
const loading = ref(false)
const tracking = ref<any>(null)

const fetchTracking = async () => {
  const orderId = Number(route.params.orderId)
  if (!orderId) return
  loading.value = true
  try {
    const res = await getDeliveryTracking(orderId)
    tracking.value = res.data
  } catch (e) {
    console.error('获取配送信息失败', e)
  } finally {
    loading.value = false
  }
}

const taskStatusMap: Record<number, string> = {
  0: '待分配',
  1: '待取餐',
  2: '配送中',
  3: '已送达',
  4: '已完成'
}

onMounted(() => {
  fetchTracking()
})
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>配送追踪</h3>
      </div>
      <div style="padding-top: 20px;">
        <div v-loading="loading" style="min-height: 200px;">
          <template v-if="tracking">
            <div style="text-align: center; padding: 20px;">
              <div style="font-size: 48px; margin-bottom: 16px;">🚚</div>
              <div style="font-size: 18px; font-weight: 600; color: var(--bs-text-title);">
                {{ taskStatusMap[tracking.taskStatus] || '配送中' }}
              </div>
              <div v-if="tracking.estimatedDeliveryTime" style="font-size: 14px; color: var(--bs-text-muted); margin-top: 8px;">
                预计 {{ tracking.estimatedDeliveryTime }} 送达
              </div>
            </div>
            <el-descriptions :column="1" border style="margin-top: 20px;">
              <el-descriptions-item label="取餐码">{{ tracking.pickupCode || '无' }}</el-descriptions-item>
              <el-descriptions-item label="取餐时间">{{ tracking.pickupTime || '尚未取餐' }}</el-descriptions-item>
              <el-descriptions-item label="送达时间">{{ tracking.deliverTime || '尚未送达' }}</el-descriptions-item>
            </el-descriptions>
            <div v-if="tracking.driver" class="card-panel" style="margin-top: 16px;">
              <div class="card-header">
                <h3>配送员信息</h3>
              </div>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="姓名">{{ tracking.driver.realName }}</el-descriptions-item>
                <el-descriptions-item label="电话">{{ tracking.driver.phone }}</el-descriptions-item>
                <el-descriptions-item label="交通工具">{{ tracking.driver.vehicleType || '未知' }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </template>
          <div v-else-if="!loading" style="text-align: center; padding: 40px; color: var(--bs-text-muted);">
            暂无配送信息
          </div>
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
