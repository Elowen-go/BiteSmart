<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Clock, CircleCheck, CircleClose, UploadFilled } from '@element-plus/icons-vue'
import { applyMerchant, getMerchantApplyStatus, getMerchantAuditLog } from '../../../api/merchant/auth'
import { uploadFile } from '../../../api/merchant/shop'
import { useUserStore } from '../../../stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const licenseUploading = ref(false)
const auditLog = ref<any>(null)
const currentApply = ref<any>(null)

const form = ref({
  shopName: '',
  contactName: '',
  contactPhone: '',
  shopAddress: '',
  licenseNumber: '',
  businessLicense: '',
  shopNotice: '',
  deliveryRange: JSON.stringify({ minDistance: 0, maxDistance: 3, unit: 'km' }),
  businessHours: JSON.stringify({ weekday: '09:00-22:00', weekend: '10:00-22:00' })
})

const statusMeta = computed(() => {
  const status = currentApply.value?.status
  if (status === 20) return { label: '审核通过', type: 'success', icon: CircleCheck, text: '店铺资质已通过，可以进入商家后台维护经营内容。' }
  if (status === 30) return { label: '审核驳回', type: 'danger', icon: CircleClose, text: currentApply.value?.auditRemark || auditLog.value?.auditRemark || '资料未通过，请调整后重新提交。' }
  if (status === 40) return { label: '店铺关闭', type: 'info', icon: CircleClose, text: '店铺已关闭，请联系平台管理员处理。' }
  return { label: currentApply.value ? '待审核' : '未提交', type: 'warning', icon: Clock, text: currentApply.value ? '申请已提交，等待管理员审核。' : '填写基础资质后提交申请。' }
})

const canSubmit = computed(() => !currentApply.value || currentApply.value.status === 30)
const canEnterMerchant = computed(() => currentApply.value?.status === 20 && userStore.isMerchant)

const fillForm = (data: any) => {
  form.value.shopName = data?.shopName || ''
  form.value.contactName = data?.contactName || ''
  form.value.contactPhone = data?.contactPhone || ''
  form.value.shopAddress = data?.shopAddress || ''
  form.value.licenseNumber = data?.licenseNumber || ''
  form.value.businessLicense = data?.businessLicense || ''
  form.value.shopNotice = data?.shopNotice || ''
  form.value.deliveryRange = data?.deliveryRange || form.value.deliveryRange
  form.value.businessHours = data?.businessHours || form.value.businessHours
}

const fetchStatus = async () => {
  loading.value = true
  try {
    const res = await getMerchantApplyStatus()
    if (res.code === 200 && res.data) {
      currentApply.value = res.data
      fillForm(res.data)
    }
  } catch (e) {
    currentApply.value = null
  } finally {
    loading.value = false
  }

  try {
    const logRes = await getMerchantAuditLog()
    if (logRes.code === 200) {
      auditLog.value = logRes.data
    }
  } catch (e) {
    auditLog.value = null
  }
}

const beforeLicenseUpload = (file: File) => {
  const validType = ['image/jpeg', 'image/png', 'image/webp'].includes(file.type)
  const validSize = file.size / 1024 / 1024 < 5
  if (!validType) ElMessage.error('请上传 JPG、PNG 或 WebP 图片')
  if (!validSize) ElMessage.error('图片不能超过 5MB')
  return validType && validSize
}

const handleLicenseUpload = async (options: any) => {
  const file = options.file as File
  if (!beforeLicenseUpload(file)) return
  licenseUploading.value = true
  try {
    const res = await uploadFile(file, 'license')
    if (res.code === 200) {
      form.value.businessLicense = res.data.url
      ElMessage.success('营业执照已上传')
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch (e) {
    ElMessage.error('上传失败')
  } finally {
    licenseUploading.value = false
  }
}

const handleSubmit = async () => {
  if (!form.value.shopName || !form.value.contactPhone || !form.value.shopAddress) {
    ElMessage.warning('请填写店铺名称、联系电话和店铺地址')
    return
  }
  submitting.value = true
  try {
    const res = await applyMerchant({ ...form.value })
    if (res.code === 200) {
      ElMessage.success(res.message || '入驻申请已提交')
      await fetchStatus()
    } else {
      ElMessage.error(res.message || '提交失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '提交失败，请确认已登录账号')
  } finally {
    submitting.value = false
  }
}

const goBack = () => {
  if (userStore.isMerchant) {
    router.push('/merchant/dashboard')
  } else if (userStore.isAdmin) {
    router.push('/admin/dashboard')
  } else {
    router.push('/user')
  }
}

onMounted(fetchStatus)
</script>

<template>
  <div class="apply-page" v-loading="loading">
    <header class="apply-header">
      <button class="back-btn" @click="goBack">
        <ArrowLeft />
        返回
      </button>
      <div>
        <p class="eyebrow">BiteSmart 商家入驻</p>
        <h1>提交店铺资质</h1>
      </div>
      <el-button v-if="canEnterMerchant" type="primary" @click="router.push('/merchant/dashboard')">
        进入商家后台
      </el-button>
    </header>

    <main class="apply-main">
      <section class="status-panel">
        <component :is="statusMeta.icon" class="status-icon" :class="statusMeta.type" />
        <div>
          <div class="status-row">
            <span class="status-label">{{ statusMeta.label }}</span>
            <el-tag :type="statusMeta.type as any" effect="plain">{{ statusMeta.label }}</el-tag>
          </div>
          <p>{{ statusMeta.text }}</p>
        </div>
      </section>

      <section class="form-panel">
        <div class="section-title">
          <h2>基础信息</h2>
          <span>平台审核会优先核对联系人、执照与经营地址</span>
        </div>

        <el-form label-position="top" :model="form" class="apply-form" :disabled="!canSubmit">
          <div class="form-grid">
            <el-form-item label="店铺名称" required>
              <el-input v-model="form.shopName" placeholder="例如：轻食工坊人民路店" />
            </el-form-item>
            <el-form-item label="联系人">
              <el-input v-model="form.contactName" placeholder="负责人姓名" />
            </el-form-item>
            <el-form-item label="联系电话" required>
              <el-input v-model="form.contactPhone" placeholder="用于平台审核联系" />
            </el-form-item>
            <el-form-item label="营业执照编号">
              <el-input v-model="form.licenseNumber" placeholder="统一社会信用代码" />
            </el-form-item>
          </div>

          <el-form-item label="店铺地址" required>
            <el-input v-model="form.shopAddress" placeholder="省市区 + 详细门牌号" />
          </el-form-item>

          <el-form-item label="营业执照">
            <div class="license-row">
              <el-upload
                :show-file-list="false"
                :http-request="handleLicenseUpload"
                accept="image/jpeg,image/png,image/webp"
              >
                <el-button :loading="licenseUploading">
                  <UploadFilled />
                  上传执照
                </el-button>
              </el-upload>
              <span v-if="form.businessLicense" class="license-path">{{ form.businessLicense }}</span>
              <span v-else class="muted">支持 JPG / PNG / WebP，建议上传清晰原图</span>
            </div>
          </el-form-item>

          <el-form-item label="店铺公告">
            <el-input v-model="form.shopNotice" type="textarea" :rows="3" placeholder="可填写试营业说明、配送说明或健康餐定制提示" />
          </el-form-item>

          <div class="action-row">
            <el-button @click="goBack">取消</el-button>
            <el-button type="primary" :loading="submitting" :disabled="!canSubmit" @click="handleSubmit">
              {{ currentApply?.status === 30 ? '重新提交审核' : '提交入驻申请' }}
            </el-button>
          </div>
        </el-form>
      </section>
    </main>
  </div>
</template>

<style scoped>
.apply-page {
  min-height: 100vh;
  background: #f5f7f6;
  color: #1f2d28;
  padding: 28px clamp(20px, 4vw, 56px);
}

.apply-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  max-width: 1120px;
  margin: 0 auto 24px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid #d8dfdb;
  background: #ffffff;
  color: #35463f;
  height: 36px;
  padding: 0 12px;
  border-radius: 6px;
  cursor: pointer;
}

.back-btn svg {
  width: 16px;
  height: 16px;
}

.eyebrow {
  margin: 0 0 4px;
  color: #607168;
  font-size: 13px;
}

h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 650;
}

.apply-main {
  max-width: 1120px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 20px;
}

.status-panel,
.form-panel {
  background: #ffffff;
  border: 1px solid #e3e8e5;
  border-radius: 8px;
  box-shadow: 0 8px 22px rgba(28, 45, 38, 0.05);
}

.status-panel {
  padding: 22px;
  height: max-content;
  display: flex;
  gap: 14px;
}

.status-icon {
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
}

.status-icon.success {
  color: #2f8f55;
}

.status-icon.warning {
  color: #c28223;
}

.status-icon.danger {
  color: #c84f45;
}

.status-icon.info {
  color: #7d8790;
}

.status-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.status-label {
  font-size: 18px;
  font-weight: 650;
}

.status-panel p {
  margin: 0;
  line-height: 1.7;
  color: #607168;
  font-size: 14px;
}

.form-panel {
  padding: 24px;
}

.section-title {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.section-title h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 650;
}

.section-title span,
.muted {
  color: #7b8982;
  font-size: 13px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 18px;
}

.license-row {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 34px;
}

.license-path {
  max-width: 520px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #52635b;
  font-size: 13px;
}

.action-row {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 8px;
}

@media (max-width: 860px) {
  .apply-header,
  .apply-main {
    display: flex;
    flex-direction: column;
    align-items: stretch;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
