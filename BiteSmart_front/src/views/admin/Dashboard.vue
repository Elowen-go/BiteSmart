<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  Money,
  ShoppingCart,
  User,
  Shop,
  List,
  Bell,
  ChatDotRound,
  ArrowUp
} from '@element-plus/icons-vue'
import StatCard from '../../components/common/StatCard.vue'
import { getOverview } from '../../api/admin/statistics'

const loading = ref(true)
const stats = ref({
  totalRevenue: 0,
  orderCount: 0,
  userCount: 0,
  merchantCount: 0
})

const recentOrders = ref<any[]>([])
const notifications = ref([
  { icon: Shop, title: '新商家入驻', desc: '"轻食主义"提交资质', time: '10分钟前' },
  { icon: ChatDotRound, title: '系统通知', desc: '平台运营数据已更新', time: '1小时前' },
  { icon: ArrowUp, title: 'AI规则更新', desc: '参数 v2.3 已生效', time: '3小时前' }
])

onMounted(async () => {
  loading.value = true
  try {
    const overviewRes = await getOverview()
    if (overviewRes.code === 200) {
      const d = overviewRes.data
      stats.value.totalRevenue = d.totalRevenue || 0
      stats.value.orderCount = d.orderCount || 0
      stats.value.userCount = d.userCount || 0
      stats.value.merchantCount = d.merchantCount || 0
    }

    const { getOrderList } = await import('../../api/admin/orders')
    const orderRes = await getOrderList({ pageNum: 1, pageSize: 5 })
    if (orderRes.code === 200) {
      const list = orderRes.data?.list || []
      recentOrders.value = list.map((o: any) => ({
        orderNo: o.orderNo,
        customer: o.deliveryAddress?.slice(0, 8) + '…' || '未知',
        amount: o.payAmount || o.totalAmount,
        status: o.orderStatus >= 50 ? 'completed' : o.orderStatus >= 40 ? 'delivering' : 'pending',
        time: o.createTime?.slice(0, 16) || ''
      }))
    }
  } catch (err) {
    console.error('获取Dashboard数据失败', err)
  } finally {
    loading.value = false
  }
})

const getStatusBadge = (status: string) => {
  switch (status) {
    case 'completed':
      return { class: 'success', text: '已完成' }
    case 'delivering':
      return { class: 'warning', text: '配送中' }
    case 'pending':
      return { class: 'secondary', text: '待接单' }
    default:
      return { class: 'secondary', text: status }
  }
}
</script>

<template>
  <div class="dashboard">
    <div v-loading="loading" class="stats-row">
      <StatCard
        :icon="Money"
        label="总销售额"
        :value="'¥' + Number(stats.totalRevenue).toLocaleString()"
      />
      <StatCard
        :icon="ShoppingCart"
        label="订单总数"
        :value="stats.orderCount"
      />
      <StatCard
        :icon="User"
        label="注册用户"
        :value="stats.userCount"
      />
      <StatCard
        :icon="Shop"
        label="商家总数"
        :value="stats.merchantCount"
      />
    </div>
    
    <div class="grid-2col">
      <div class="card-panel">
        <div class="card-header">
          <h3>
            <List style="width: 18px; height: 18px; margin-right: 8px; color: var(--bs-primary);" />
            最近订单
          </h3>
          <button class="btn btn-secondary btn-sm">查看更多</button>
        </div>
        <div class="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>订单号</th>
                <th>客户</th>
                <th>金额</th>
                <th>状态</th>
                <th>时间</th>
              </tr>
            </thead>
            <tbody v-if="recentOrders.length > 0">
              <tr v-for="order in recentOrders" :key="order.orderNo">
                <td>{{ order.orderNo }}</td>
                <td>{{ order.customer }}</td>
                <td>¥{{ Number(order.amount).toFixed(2) }}</td>
                <td>
                  <span :class="['status-badge', getStatusBadge(order.status).class]">
                    {{ getStatusBadge(order.status).text }}
                  </span>
                </td>
                <td>{{ order.time }}</td>
              </tr>
            </tbody>
            <tbody v-else>
              <tr><td colspan="5" style="text-align:center;color:var(--bs-text-muted);padding:32px 0;">暂无订单</td></tr>
            </tbody>
          </table>
        </div>
      </div>
      
      <div class="card-panel">
        <div class="card-header">
          <h3>
            <Bell style="width: 18px; height: 18px; margin-right: 8px; color: var(--bs-primary);" />
            最新通知
          </h3>
          <span style="font-size: 13px; color: var(--bs-text-muted);">3条未读</span>
        </div>
        <div class="notifications-list">
          <div v-for="(item, index) in notifications" :key="index" class="notification-item">
            <component :is="item.icon" style="color: var(--bs-primary); width: 20px;" />
            <div class="notification-content">
              <div style="font-weight: 500; color: var(--bs-text-title);">{{ item.title }}</div>
              <div style="font-size: 13px; color: var(--bs-text-muted);">{{ item.desc }}</div>
            </div>
            <span style="font-size: 12px; color: var(--bs-text-muted);">{{ item.time }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--bs-spacing-lg);
  margin-bottom: var(--bs-spacing-lg);
}

.grid-2col {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: var(--bs-spacing-lg);
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
  text-decoration: none;
}

.btn-primary {
  background: var(--bs-primary);
  color: #FFFFFF;
}

.btn-primary:hover {
  background: var(--bs-primary-hover);
}

.btn-secondary {
  background: transparent;
  border-color: var(--bs-border-light);
  color: var(--bs-text-body);
}

.btn-secondary:hover {
  background: var(--bs-bg-hover);
}

.btn-sm {
  padding: var(--bs-spacing-xs) var(--bs-spacing-md);
  font-size: var(--bs-font-size-sm);
}

.table-wrapper {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--bs-font-size-base);
}

table th {
  text-align: left;
  padding: 12px 12px 12px 0;
  font-weight: 600;
  color: var(--bs-text-muted);
  border-bottom: 1px solid var(--bs-border-light);
}

table td {
  padding: 14px 12px 14px 0;
  border-bottom: 1px solid var(--bs-border-light);
  color: var(--bs-text-body);
}

table tr:last-child td {
  border-bottom: none;
}

table tr:hover td {
  background: var(--bs-bg-hover);
}

.status-badge {
  display: inline-block;
  padding: 2px 12px;
  border-radius: 20px;
  font-size: var(--bs-font-size-xs);
  font-weight: 500;
}

.status-badge.success {
  background: var(--bs-status-success-bg);
  color: var(--bs-status-success);
}

.status-badge.warning {
  background: var(--bs-status-warning-bg);
  color: var(--bs-status-warning);
}

.status-badge.secondary {
  background: var(--bs-status-secondary-bg);
  color: var(--bs-status-secondary);
}

.notifications-list {
  display: flex;
  flex-direction: column;
  gap: var(--bs-spacing-md);
}

.notification-item {
  display: flex;
  gap: var(--bs-spacing-md);
  align-items: flex-start;
  padding-bottom: var(--bs-spacing-sm);
  border-bottom: 1px solid var(--bs-border-light);
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-content {
  flex: 1;
}

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .grid-2col {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
}
</style>
