<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getAdminDeliveryTasks } from '../../api/admin/delivery'
import ListState from '../../components/common/ListState.vue'

const loading = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const taskStatus = ref<number | undefined>()
const loadError = ref('')
const statusMap: Record<number, string> = { 10: '待接单', 20: '待取餐', 30: '已取餐', 40: '配送中', 50: '已送达', 60: '异常', 70: '已取消' }
const load = async () => { loading.value = true; loadError.value = ''; try { const res = await getAdminDeliveryTasks({ pageNum: pageNum.value, pageSize: pageSize.value, taskStatus: taskStatus.value }); if (res.code === 200) { rows.value = res.data.list || []; total.value = res.data.total || 0 } else { loadError.value = res.message || '配送任务暂时无法获取' } } catch (err) { loadError.value = '请检查网络连接后重试'; console.error('获取配送任务失败', err) } finally { loading.value = false } }
onMounted(load)
</script>
<template>
<div class="page-container"><div class="card-panel"><div class="toolbar"><h3>配送任务运营</h3><el-select v-model="taskStatus" clearable placeholder="按配送状态筛选" style="width:180px" @change="pageNum=1;load()"><el-option v-for="(label,value) in statusMap" :key="value" :label="label" :value="Number(value)" /></el-select></div><ListState :loading="loading" :error="loadError" :empty="!rows.length" empty-text="暂无配送任务" @retry="load"><el-table :data="rows" border stripe><el-table-column prop="orderNo" label="订单号" min-width="160"/><el-table-column prop="driverId" label="配送员 ID" width="140"/><el-table-column prop="deliveryAddress" label="配送地址" min-width="220" show-overflow-tooltip/><el-table-column prop="receiverName" label="收货人" width="110"/><el-table-column prop="pickupCode" label="取餐码" width="100"/><el-table-column label="状态" width="100"><template #default="{row}"><el-tag :type="row.taskStatus===60?'danger':row.taskStatus===70?'info':row.taskStatus===50?'success':'warning'">{{ statusMap[row.taskStatus] || '未知' }}</el-tag></template></el-table-column><el-table-column prop="exceptionReason" label="异常原因" min-width="180" show-overflow-tooltip/><el-table-column prop="estimatedDeliveryTime" label="预计送达" width="170"/></el-table></ListState><div class="pager"><el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" layout="total, sizes, prev, pager, next" @current-change="load" @size-change="load" /></div></div></div>
</template>
<style scoped>.card-panel{background:#fff;border:1px solid var(--bs-border-light);border-radius:8px;padding:20px;box-shadow:var(--bs-card-shadow)}.toolbar{display:flex;align-items:center;justify-content:space-between;margin-bottom:18px}.toolbar h3{margin:0;color:var(--bs-text-title)}.pager{display:flex;justify-content:flex-end;margin-top:16px}</style>
