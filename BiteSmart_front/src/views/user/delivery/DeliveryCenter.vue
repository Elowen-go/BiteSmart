<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOrderList } from '../../../api/user/orders'
import { ElMessage } from 'element-plus'
const router = useRouter(); const loading = ref(false); const orders = ref<any[]>([])
const orderStatus: Record<number, string> = { 40: '配送中', 50: '已送达' }
const fetchOrders = async () => { loading.value = true; try { const res = await getOrderList(); const list = Array.isArray(res.data) ? res.data : (res.data?.list || []); orders.value = list.filter((o: any) => o.orderStatus === 40 || o.orderStatus === 50) } catch (error) { console.error('获取配送订单失败', error); ElMessage.error('获取配送订单失败，请稍后重试') } finally { loading.value = false } }
onMounted(fetchOrders)
</script>
<template><div class="page-container"><div class="card-panel"><div class="card-header"><div><h3>配送跟踪</h3><p>查看正在配送和最近送达的订单</p></div><el-button @click="fetchOrders">刷新</el-button></div><el-table v-loading="loading" :data="orders" border><el-table-column prop="orderNo" label="订单号" min-width="190" /><el-table-column prop="receiverName" label="收货人" width="100" /><el-table-column prop="deliveryAddress" label="配送地址" min-width="220" show-overflow-tooltip /><el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.orderStatus === 40 ? 'warning' : 'success'">{{ orderStatus[row.orderStatus] }}</el-tag></template></el-table-column><el-table-column prop="updateTime" label="更新时间" width="180" /><el-table-column label="操作" width="110" fixed="right"><template #default="{ row }"><el-button type="primary" link @click="router.push(`/user/delivery/${row.id}`)">查看跟踪</el-button></template></el-table-column></el-table><el-empty v-if="!loading && !orders.length" description="暂无配送中的订单" /></div></div></template>
<style scoped>.page-container{animation:fadeIn .3s ease}.card-panel{background:var(--bs-card-bg);border-radius:var(--bs-radius-md);box-shadow:var(--bs-card-shadow);padding:var(--bs-spacing-lg)}.card-header{display:flex;justify-content:space-between;align-items:center;margin-bottom:20px}.card-header h3{margin:0;color:var(--bs-text-title)}.card-header p{margin:6px 0 0;color:var(--bs-text-muted);font-size:13px}@keyframes fadeIn{from{opacity:0;transform:translateY(10px)}to{opacity:1;transform:translateY(0)}}</style>
