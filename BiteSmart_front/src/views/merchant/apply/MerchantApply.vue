<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Clock, CircleCheck, CircleClose, Upload, Delete } from '@element-plus/icons-vue'
import { applyMerchant, getMerchantApplyStatus, getMerchantAuditLog } from '../../../api/merchant/auth'
import { uploadFile } from '../../../api/merchant/shop'
import { useUserStore } from '../../../stores/user'
import { resolveFileUrl } from '../../../utils/fileUrl'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const licenseUploading = ref(false)
const auditLog = ref<any>(null)
const currentApply = ref<any>(null)
const formRef = ref()
const licenseUploadRef = ref()

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

const formRules = {
  shopName: [{ required: true, message: '请输入店铺名称', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^(1[3-9]\d{9}|0\d{2,3}-?\d{7,8})$/, message: '联系电话格式不正确', trigger: 'blur' }
  ],
  shopAddress: [{ required: true, message: '请输入店铺地址', trigger: 'blur' }]
}

const statusMeta = computed(() => {
  const status = currentApply.value?.status
  if (status === 20) return { label: '审核通过', type: 'success', icon: CircleCheck, text: '店铺资质已通过，可以进入商家后台维护经营内容。' }
  if (status === 30) return { label: '审核驳回', type: 'danger', icon: CircleClose, text: currentApply.value?.auditRemark || auditLog.value?.auditRemark || '资料未通过，请调整后重新提交。' }
  if (status === 40) return { label: '店铺关闭', type: 'info', icon: CircleClose, text: '店铺已关闭，请联系平台管理员处理。' }
  return { label: currentApply.value ? '待审核' : '未提交', type: 'warning', icon: Clock, text: currentApply.value ? '申请已提交，等待管理员审核。' : '填写基础资质后提交申请。' }
})

const canSubmit = computed(() => !currentApply.value || currentApply.value.status === 30)
const canEnterMerchant = computed(() => currentApply.value?.status === 20 && userStore.isMerchant)

const licensePreviewUrl = computed(() => (form.value.businessLicense ? resolveFileUrl(form.value.businessLicense) : ''))

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

const triggerLicenseUpload = () => {
  if (!canSubmit.value || licenseUploading.value) return
  licenseUploadRef.value?.$el?.querySelector('input')?.click()
}

const handleRemoveLicense = () => {
  form.value.businessLicense = ''
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
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
      <div class="header-title">
        <p class="eyebrow">BITESMART 商家入驻</p>
        <h1>提交店铺资质</h1>
      </div>
      <el-button v-if="canEnterMerchant" type="primary" @click="router.push('/merchant/dashboard')">
        进入商家后台
      </el-button>
    </header>

    <main class="apply-main">
      <section class="status-panel card">
        <component :is="statusMeta.icon" class="status-icon" :class="statusMeta.type" />
        <div>
          <div class="status-row">
            <span class="status-label">{{ statusMeta.label }}</span>
            <el-tag :type="statusMeta.type as any" effect="plain" size="small">{{ statusMeta.label }}</el-tag>
          </div>
          <p>{{ statusMeta.text }}</p>
          <p v-if="currentApply?.status === 30" class="status-tip">修改下方资料后可重新提交审核。</p>
        </div>
      </section>

      <el-form
        ref="formRef"
        label-position="top"
        :model="form"
        :rules="formRules"
        class="apply-form"
        :disabled="!canSubmit"
      >
        <section class="card form-card">
          <div class="section-title">
            <div>
              <div class="en">BASIC</div>
              <h2>基本信息</h2>
            </div>
            <span>平台审核会优先核对联系人与经营地址</span>
          </div>
          <div class="form-grid">
            <el-form-item label="店铺名称" prop="shopName">
              <el-input v-model="form.shopName" placeholder="例如：轻食工坊人民路店" />
            </el-form-item>
            <el-form-item label="联系人">
              <el-input v-model="form.contactName" placeholder="负责人姓名" />
            </el-form-item>
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="form.contactPhone" placeholder="用于平台审核联系" maxlength="11" />
            </el-form-item>
            <el-form-item label="店铺地址" prop="shopAddress">
              <el-input v-model="form.shopAddress" placeholder="省市区 + 详细门牌号" />
            </el-form-item>
          </div>
        </section>

        <section class="card form-card">
          <div class="section-title">
            <div>
              <div class="en">LICENSE</div>
              <h2>资质证照</h2>
            </div>
            <span>支持 JPG / PNG / WebP，不超过 5MB</span>
          </div>
          <div class="form-grid">
            <el-form-item label="营业执照编号">
              <el-input v-model="form.licenseNumber" placeholder="统一社会信用代码" />
            </el-form-item>
          </div>
          <el-form-item label="营业执照">
            <div class="license-upload" v-loading="licenseUploading">
              <div class="license-preview" :class="{ readonly: !canSubmit }" @click="triggerLicenseUpload">
                <el-image
                  v-if="licensePreviewUrl"
                  :src="licensePreviewUrl"
                  fit="cover"
                  class="license-image"
                  :preview-src-list="[licensePreviewUrl]"
                  preview-teleported
                />
                <div v-else class="license-empty">
                  <Upload style="width: 20px; height: 20px;" />
                  <span>{{ canSubmit ? '点击上传营业执照' : '未上传' }}</span>
                </div>
                <button
                  v-if="licensePreviewUrl && canSubmit"
                  class="remove-btn"
                  @click.stop="handleRemoveLicense"
                >
                  <Delete style="width: 14px; height: 14px;" />
                </button>
              </div>
              <el-upload
                ref="licenseUploadRef"
                :show-file-list="false"
                :http-request="handleLicenseUpload"
                accept="image/jpeg,image/png,image/webp"
                class="hidden-upload"
              >
                <span />
              </el-upload>
            </div>
          </el-form-item>
        </section>

        <section class="card form-card">
          <div class="section-title">
            <div>
              <div class="en">NOTICE</div>
              <h2>经营说明</h2>
            </div>
          </div>
          <el-form-item label="店铺公告">
            <el-input
              v-model="form.shopNotice"
              type="textarea"
              :rows="3"
              placeholder="可填写试营业说明、配送说明或健康餐定制提示"
            />
          </el-form-item>
          <div class="action-row">
            <el-button @click="goBack">取消</el-button>
            <el-button type="primary" :loading="submitting" :disabled="!canSubmit" @click="handleSubmit">
              {{ currentApply?.status === 30 ? '重新提交审核' : '提交入驻申请' }}
            </el-button>
          </div>
        </section>
      </el-form>
    </main>
  </div>
</template>

<style scoped>
.apply-page {
  min-height: 100vh;
  background: var(--bg);
  color: var(--ink);
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
  border: 1px solid var(--bs-border-light);
  background: #ffffff;
  color: var(--bs-text-body);
  height: 36px;
  padding: 0 12px;
  border-radius: var(--bs-radius-md);
  cursor: pointer;
  transition: border-color 0.15s ease, color 0.15s ease;
}

.back-btn:hover {
  border-color: var(--green);
  color: var(--green-deep);
}

.back-btn svg {
  width: 16px;
  height: 16px;
}

.header-title {
  flex: 1;
}

.eyebrow {
  margin: 0 0 4px;
  color: var(--faint);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 2px;
}

h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 650;
  color: var(--ink);
}

.apply-main {
  max-width: 1120px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.apply-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.card {
  background: var(--bs-card-bg);
  border: 1px solid var(--hair);
  border-radius: var(--bs-radius-md);
  box-shadow: var(--bs-card-shadow);
}

.status-panel {
  padding: 22px;
  display: flex;
  gap: 14px;
  position: sticky;
  top: 24px;
}

.status-icon {
  width: 22px;
  height: 22px;
  flex: 0 0 auto;
  margin-top: 2px;
}

.status-icon.success {
  color: var(--green);
}

.status-icon.warning {
  color: var(--orange);
}

.status-icon.danger {
  color: var(--danger-brand);
}

.status-icon.info {
  color: var(--bs-status-secondary);
}

.status-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.status-label {
  font-size: 17px;
  font-weight: 650;
  color: var(--ink);
}

.status-panel p {
  margin: 0;
  line-height: 1.7;
  color: var(--sub);
  font-size: 13px;
}

.status-tip {
  margin-top: 8px !important;
  color: var(--orange) !important;
}

.form-card {
  padding: 24px;
}

.section-title {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--hair);
}

.section-title h2 {
  margin: 0;
  font-size: 17px;
  font-weight: 650;
  color: var(--ink);
}

.section-title .en {
  color: var(--faint);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 2px;
}

.section-title > span {
  color: var(--bs-text-muted);
  font-size: 12px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 18px;
}

.license-upload {
  display: flex;
  align-items: center;
  gap: 12px;
}

.license-preview {
  position: relative;
  width: 140px;
  height: 100px;
  cursor: pointer;
}

.license-preview.readonly {
  cursor: default;
}

.license-image {
  width: 100%;
  height: 100%;
  display: block;
  border-radius: 8px;
  border: 1px solid var(--bs-border-light);
}

.license-empty {
  width: 100%;
  height: 100%;
  border: 1px dashed var(--bs-border-light);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--bs-text-muted);
  font-size: 13px;
  transition: border-color 0.15s ease, color 0.15s ease;
}

.license-preview:not(.readonly):hover .license-empty {
  border-color: var(--green);
  color: var(--green-deep);
}

.remove-btn {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 24px;
  height: 24px;
  background: var(--bs-status-danger);
  color: #ffffff;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
  z-index: 10;
}

.remove-btn:hover {
  background: var(--danger-brand);
}

.hidden-upload {
  display: none;
}

.action-row {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 4px;
}

@media (max-width: 860px) {
  .apply-header,
  .apply-main {
    display: flex;
    flex-direction: column;
    align-items: stretch;
  }

  .status-panel {
    position: static;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
