<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createComplaint } from '../../api/user/complaints'
const form = ref({ orderId: undefined as number | undefined, targetType: 10, targetId: undefined as number | undefined, complaintReason: '', complaintDesc: '' })
const submit = async () => { if (!form.value.orderId || !form.value.targetId || !form.value.complaintReason) { ElMessage.warning('请填写订单、投诉对象和投诉原因'); return }; const res:any = await createComplaint(form.value as any); if (res.code === 200) { ElMessage.success('投诉已提交'); form.value = { orderId: undefined, targetType: 10, targetId: undefined, complaintReason: '', complaintDesc: '' } } }
</script>
<template><div class="page-container"><div class="panel"><h3>提交投诉</h3><el-form label-width="100px"><el-form-item label="订单ID"><el-input-number v-model="form.orderId" :min="1" /></el-form-item><el-form-item label="投诉对象"><el-radio-group v-model="form.targetType"><el-radio :label="10">商家</el-radio><el-radio :label="20">配送员</el-radio></el-radio-group></el-form-item><el-form-item label="对象ID"><el-input-number v-model="form.targetId" :min="1" /></el-form-item><el-form-item label="投诉原因"><el-input v-model="form.complaintReason" /></el-form-item><el-form-item label="详细说明"><el-input v-model="form.complaintDesc" type="textarea" :rows="4" /></el-form-item><el-form-item><el-button type="primary" @click="submit">提交投诉</el-button></el-form-item></el-form></div></div></template>
<style scoped>.page-container{max-width:760px}.panel{background:#fff;border:1px solid var(--bs-border-light);border-radius:8px;padding:24px}.panel h3{margin:0 0 24px;color:var(--bs-text-title)}</style>
