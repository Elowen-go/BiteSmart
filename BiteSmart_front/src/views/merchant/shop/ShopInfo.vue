<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getShopInfo, updateShopInfo } from '../../../api/merchant/shop'

const loading = ref(false)
const form = ref({
  shopName: '',
  logoUrl: '',
  phone: '',
  address: '',
  shopDesc: '',
  businessHours: '',
  shopNotice: ''
})

const fetchShopInfo = async () => {
  loading.value = true
  try {
    const res = await getShopInfo()
    if (res.code === 200) {
      const data = res.data
      form.value.shopName = data.shopName || ''
      form.value.logoUrl = data.logoUrl || ''
      form.value.phone = data.phone || ''
      form.value.address = data.address || ''
      form.value.shopDesc = data.shopDesc || ''
      form.value.businessHours = data.businessHours || ''
      form.value.shopNotice = data.shopNotice || ''
    }
  } catch (e) {
    console.error('获取店铺信息失败', e)
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  loading.value = true
  try {
    const res = await updateShopInfo(form.value as any)
    if (res.code === 200) {
      ElMessage.success('保存成功')
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败')
    console.error('保存店铺信息失败', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchShopInfo()
})
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>店铺信息</h3>
        <button class="btn btn-primary" @click="handleSave">保存修改</button>
      </div>
      <div style="padding-top: 20px;">
        <el-form :model="form" label-width="120px" v-loading="loading">
          <el-form-item label="店铺名称">
            <el-input v-model="form.shopName" placeholder="请输入店铺名称" />
          </el-form-item>
          <el-form-item label="店铺Logo">
            <el-input v-model="form.logoUrl" placeholder="请输入Logo URL" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="form.phone" placeholder="请输入联系电话" />
          </el-form-item>
          <el-form-item label="店铺地址">
            <el-input v-model="form.address" placeholder="请输入店铺地址" />
          </el-form-item>
          <el-form-item label="营业时间">
            <el-input v-model="form.businessHours" placeholder="请输入营业时间，如 09:00-22:00" />
          </el-form-item>
          <el-form-item label="店铺公告">
            <el-input v-model="form.shopNotice" type="textarea" :rows="3" placeholder="请输入店铺公告" />
          </el-form-item>
        </el-form>
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

