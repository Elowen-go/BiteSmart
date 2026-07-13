<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { auditRefund, getRefundList } from '../../api/admin/work-orders'

const loading = ref(false); const rows = ref<any[]>([]); const total = ref(0)
const pageNum = ref(1); const pageSize = ref(10); const status = ref<number | undefined>()
const dialog = ref(false); const current = ref<any>(); const remark = ref('')
const statusMap: Record<number, string> = { 10: '待审核', 20: '审核通过', 30: '已驳回', 40: '已退款' }
const load = async () => { loading.value = true; try { const r = await getRefundList({ pageNum: pageNum.value, pageSize: pageSize.value, status: status.value }); if (r.code === 200) { rows.value = r.data.list || []; total.value = r.data.total || 0 } } finally { loading.value = false } }
const open = (row: any) => { current.value = row; remark.value = ''; dialog.value = true }
const submit = async (next: number) => { const r = await auditRefund(current.value.id, next, remark.value); if (r.code === 200) { ElMessage.success('退款工单已处理'); dialog.value = false; load() } }
onMounted(load)
</script>
<template><div class="page-container"><div class="card-panel"><div class="card-header"><h3>退款工单</h3><el-select v-model="status" clearable placeholder="按状态筛选" size="small" style="width:150px" @change="load"><el-option v-for="(label, value) in statusMap" :key="value" :label="label" :value="Number(value)" /></el-select></div><el-table :data="rows" v-loading="loading" border stripe><el-table-column prop="orderNo" label="订单号" min-width="170"/><el-table-column prop="refundAmount" label="退款金额" width="110"/><el-table-column prop="refundReason" label="退款原因" min-width="180" show-overflow-tooltip/><el-table-column label="状态" width="100"><template #default="{row}">{{ statusMap[row.auditStatus] || '-' }}</template></el-table-column><el-table-column prop="applyTime" label="申请时间" width="170"/><el-table-column label="操作" width="100" fixed="right"><template #default="{row}"><el-button v-if="row.auditStatus === 10" type="primary" link size="small" @click="open(row)">处理</el-button></template></el-table-column></el-table><div class="pager"><el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" layout="total, prev, pager, next" @current-change="load"/></div></div><el-dialog v-model="dialog" title="处理退款工单" width="430px"><el-input v-model="remark" type="textarea" :rows="3" placeholder="处理备注"/><template #footer><el-button @click="dialog=false">关闭</el-button><el-button type="danger" @click="submit(30)">驳回</el-button><el-button type="primary" @click="submit(40)">确认退款</el-button></template></el-dialog></div></template>
<style scoped>.page-container{animation:fadeIn .25s ease}.card-panel{background:#fff;border:1px solid var(--bs-border-light);border-radius:8px;padding:20px;box-shadow:var(--bs-card-shadow)}.card-header{display:flex;align-items:center;justify-content:space-between;margin-bottom:16px}.card-header h3{margin:0;color:var(--bs-text-title)}.pager{display:flex;justify-content:flex-end;margin-top:16px}@keyframes fadeIn{from{opacity:0;transform:translateY(8px)}to{opacity:1;transform:translateY(0)}}</style>
