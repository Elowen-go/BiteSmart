<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload, Delete, Edit } from '@element-plus/icons-vue'
import { getShopInfo, updateShopInfo, uploadFile } from '../../../api/merchant/shop'
import { resolveFileUrl } from '../../../utils/fileUrl'

const loading = ref(false)
const editing = ref(false)
const form = ref({
  shopName: '',
  shopLogo: '',
  contactName: '',
  contactPhone: '',
  shopAddress: '',
  businessLicense: '',
  licenseNumber: '',
  deliveryRange: '',
  businessHours: '',
  shopNotice: ''
})

const businessHoursForm = ref({
  weekdayStart: '09:00',
  weekdayEnd: '22:00',
  weekendStart: '10:00',
  weekendEnd: '22:00'
})

const deliveryRangeForm = ref({
  minDistance: 0,
  maxDistance: 3,
  unit: 'km'
})

const imageUrl = ref('')
const uploadInput = ref<HTMLInputElement | null>(null)
const selectedFile = ref<File | null>(null)
const showPreview = ref(false)

const weekdayTime = computed(() => {
  return `${businessHoursForm.value.weekdayStart}-${businessHoursForm.value.weekdayEnd}`
})

const weekendTime = computed(() => {
  return `${businessHoursForm.value.weekendStart}-${businessHoursForm.value.weekendEnd}`
})

const fetchShopInfo = async () => {
  loading.value = true
  try {
    const res = await getShopInfo()
    if (res.code === 200) {
      const data = res.data
      form.value.shopName = data.shopName || ''
      form.value.shopLogo = data.shopLogo || ''
      form.value.contactName = data.contactName || ''
      form.value.contactPhone = data.contactPhone || ''
      form.value.shopAddress = data.shopAddress || ''
      form.value.businessLicense = data.businessLicense || ''
      form.value.licenseNumber = data.licenseNumber || ''
      form.value.shopNotice = data.shopNotice || ''
      if (form.value.shopLogo) {
        imageUrl.value = resolveFileUrl(form.value.shopLogo)
      }
      parseBusinessHours(data.businessHours)
      parseDeliveryRange(data.deliveryRange)
    }
  } catch (e) {
    console.error('获取店铺信息失败', e)
  } finally {
    loading.value = false
  }
}

const parseBusinessHours = (hours: string) => {
  if (!hours) return
  try {
    const parsed = JSON.parse(hours)
    if (parsed.weekday) {
      const [start, end] = parsed.weekday.split('-')
      businessHoursForm.value.weekdayStart = start || '09:00'
      businessHoursForm.value.weekdayEnd = end || '22:00'
    }
    if (parsed.weekend) {
      const [start, end] = parsed.weekend.split('-')
      businessHoursForm.value.weekendStart = start || '10:00'
      businessHoursForm.value.weekendEnd = end || '22:00'
    }
  } catch {
    const [start, end] = hours.split('-')
    businessHoursForm.value.weekdayStart = start || '09:00'
    businessHoursForm.value.weekdayEnd = end || '22:00'
    businessHoursForm.value.weekendStart = start || '10:00'
    businessHoursForm.value.weekendEnd = end || '22:00'
  }
}

const buildBusinessHours = () => {
  return JSON.stringify({
    weekday: weekdayTime.value,
    weekend: weekendTime.value
  })
}

const parseDeliveryRange = (range: string) => {
  if (!range) return
  try {
    const parsed = JSON.parse(range)
    deliveryRangeForm.value.minDistance = parsed.minDistance || 0
    deliveryRangeForm.value.maxDistance = parsed.maxDistance || 3
    deliveryRangeForm.value.unit = parsed.unit || 'km'
  } catch {
    deliveryRangeForm.value.minDistance = 0
    deliveryRangeForm.value.maxDistance = parseInt(range) || 3
    deliveryRangeForm.value.unit = 'km'
  }
}

const buildDeliveryRange = () => {
  return JSON.stringify({
    minDistance: deliveryRangeForm.value.minDistance,
    maxDistance: deliveryRangeForm.value.maxDistance,
    unit: deliveryRangeForm.value.unit
  })
}

const handleLogoSelect = (event: Event) => {
  if (!editing.value) return
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  selectedFile.value = file
  const reader = new FileReader()
  reader.onload = (e) => {
    imageUrl.value = e.target?.result as string
  }
  reader.readAsDataURL(file)
  target.value = ''
}

const handleRemoveLogo = () => {
  if (!editing.value) return
  form.value.shopLogo = ''
  imageUrl.value = ''
  selectedFile.value = null
}

const handleEdit = () => {
  editing.value = true
}

const handleCancel = () => {
  editing.value = false
  selectedFile.value = null
  fetchShopInfo()
}

const handleSave = async () => {
  loading.value = true
  try {
    if (selectedFile.value) {
      const uploadRes = await uploadFile(selectedFile.value, 'shop_logo')
      if (uploadRes.code === 200) {
        form.value.shopLogo = uploadRes.data.url
        imageUrl.value = `/api/files/download${uploadRes.data.url}`
        selectedFile.value = null
      } else {
        ElMessage.error(uploadRes.message || '图片上传失败')
        loading.value = false
        return
      }
    }

    form.value.businessHours = buildBusinessHours()
    form.value.deliveryRange = buildDeliveryRange()
    const submitData = { ...form.value } as Record<string, any>
    Object.keys(submitData).forEach(key => {
      if (submitData[key] === '') {
        submitData[key] = null
      }
    })
    const res = await updateShopInfo(submitData as any)
    if (res.code === 200) {
      ElMessage.success('保存成功')
      editing.value = false
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

const triggerUpload = () => {
  if (!editing.value) return
  uploadInput.value?.click()
}

const handleLogoClick = () => {
  if (editing.value) {
    triggerUpload()
  } else if (imageUrl.value) {
    showPreview.value = true
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
        <div class="btn-group">
          <template v-if="!editing">
            <button class="btn btn-edit" @click="handleEdit">
              <Edit style="width: 16px; height: 16px;" />
              修改
            </button>
          </template>
          <template v-else>
            <button class="btn btn-cancel" @click="handleCancel">取消</button>
            <button class="btn btn-primary" @click="handleSave">保存修改</button>
          </template>
        </div>
      </div>
      <div style="padding-top: 20px;">
        <el-form :model="form" label-width="120px" v-loading="loading">
          <el-form-item label="店铺名称">
            <el-input v-model="form.shopName" placeholder="请输入店铺名称" :disabled="!editing" />
          </el-form-item>
          <el-form-item label="联系人">
            <el-input v-model="form.contactName" placeholder="请输入联系人姓名" :disabled="!editing" />
          </el-form-item>
          <el-form-item label="店铺Logo">
            <div class="logo-preview" @click="handleLogoClick">
              <img v-if="imageUrl" :src="imageUrl" alt="店铺Logo" class="logo-image" />
              <div v-else class="logo-placeholder">
                {{ (form.shopName || '店')[0] }}
              </div>
              <button class="remove-btn" @click.stop="handleRemoveLogo" v-if="imageUrl && editing">
                <Delete style="width: 16px; height: 16px;" />
              </button>
              <div class="upload-hint" v-if="!imageUrl && editing">
                <Upload style="width: 20px; height: 20px;" />
                <span>点击上传Logo</span>
              </div>
              <div class="view-hint" v-if="imageUrl && !editing">
                <span>点击查看大图</span>
              </div>
            </div>
            <input
              ref="uploadInput"
              type="file"
              accept="image/jpeg,image/png,image/gif"
              class="hidden-input"
              @change="handleLogoSelect"
            />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="form.contactPhone" placeholder="请输入联系电话" :disabled="!editing" />
          </el-form-item>
          <el-form-item label="执照编号">
            <el-input v-model="form.licenseNumber" placeholder="请输入统一社会信用代码" :disabled="!editing" />
          </el-form-item>
          <el-form-item label="营业执照">
            <el-input v-model="form.businessLicense" placeholder="营业执照图片地址" :disabled="!editing" />
          </el-form-item>
          <el-form-item label="店铺地址">
            <el-input v-model="form.shopAddress" placeholder="请输入店铺地址" :disabled="!editing" />
          </el-form-item>
          <el-form-item label="配送范围">
            <div class="delivery-range-container">
              <el-input-number
                v-model="deliveryRangeForm.minDistance"
                :min="0"
                :max="50"
                class="range-input"
                placeholder="最小距离"
                :disabled="!editing"
              />
              <span class="range-separator">-</span>
              <el-input-number
                v-model="deliveryRangeForm.maxDistance"
                :min="1"
                :max="50"
                class="range-input"
                placeholder="最大距离"
                :disabled="!editing"
              />
              <span class="range-unit">{{ deliveryRangeForm.unit }}</span>
            </div>
          </el-form-item>
          <el-form-item label="营业时间">
            <div class="business-hours-container">
              <div class="hours-row">
                <span class="hours-label">工作日（周一至周五）</span>
                <el-time-picker
                  v-model="businessHoursForm.weekdayStart"
                  format="HH:mm"
                  value-format="HH:mm"
                  placeholder="开始时间"
                  class="time-picker"
                  :disabled="!editing"
                  :picker-options="{
                    selectableRange: '00:00 - 23:59',
                    step: '00:30'
                  }"
                />
                <span class="time-separator">-</span>
                <el-time-picker
                  v-model="businessHoursForm.weekdayEnd"
                  format="HH:mm"
                  value-format="HH:mm"
                  placeholder="结束时间"
                  class="time-picker"
                  :disabled="!editing"
                  :picker-options="{
                    selectableRange: '00:00 - 23:59',
                    step: '00:30'
                  }"
                />
              </div>
              <div class="hours-row">
                <span class="hours-label">周末（周六至周日）</span>
                <el-time-picker
                  v-model="businessHoursForm.weekendStart"
                  format="HH:mm"
                  value-format="HH:mm"
                  placeholder="开始时间"
                  class="time-picker"
                  :disabled="!editing"
                  :picker-options="{
                    selectableRange: '00:00 - 23:59',
                    step: '00:30'
                  }"
                />
                <span class="time-separator">-</span>
                <el-time-picker
                  v-model="businessHoursForm.weekendEnd"
                  format="HH:mm"
                  value-format="HH:mm"
                  placeholder="结束时间"
                  class="time-picker"
                  :disabled="!editing"
                  :picker-options="{
                    selectableRange: '00:00 - 23:59',
                    step: '00:30'
                  }"
                />
              </div>
            </div>
          </el-form-item>
          <el-form-item label="店铺公告">
            <el-input v-model="form.shopNotice" type="textarea" :rows="3" placeholder="请输入店铺公告" :disabled="!editing" />
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
  
  <div class="image-preview-modal" v-if="showPreview" @click="showPreview = false">
    <div class="preview-content" @click.stop>
      <img :src="imageUrl" alt="店铺Logo预览" />
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

.btn-group {
  display: flex;
  gap: 10px;
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

.btn-edit {
  background: transparent;
  color: var(--bs-primary);
  border-color: var(--bs-primary);
}

.btn-edit:hover {
  background: rgba(27, 58, 47, 0.1);
}

.btn-cancel {
  background: transparent;
  color: var(--bs-text-secondary);
  border-color: var(--bs-border-color);
}

.btn-cancel:hover {
  background: var(--bs-bg-secondary);
}

.logo-preview {
  position: relative;
  width: 120px;
  height: 120px;
  cursor: pointer;
}

.logo-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
  border: 2px solid var(--bs-border-color);
}

.logo-placeholder {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  background: #1B3A2F;
  color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  font-weight: 600;
  border: 2px solid var(--bs-border-color);
}

.remove-btn {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 24px;
  height: 24px;
  background: #D9534F;
  color: #FFFFFF;
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
  background: #c9302c;
}

.upload-hint {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: transparent;
  transition: all 0.2s;
}

.logo-preview:hover .upload-hint {
  background: rgba(0, 0, 0, 0.3);
  color: #FFFFFF;
}

.upload-hint span {
  font-size: var(--bs-font-size-sm);
  margin-top: 8px;
}

.hidden-input {
  display: none;
}

.business-hours-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hours-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hours-label {
  width: 160px;
  font-size: var(--bs-font-size-base);
  color: var(--bs-text-secondary);
}

.time-picker {
  width: 120px;
}

.time-separator {
  color: var(--bs-text-secondary);
  font-weight: 500;
}

.delivery-range-container {
  display: flex;
  align-items: center;
  gap: 12px;
}

.range-input {
  width: 100px;
}

.range-separator {
  color: var(--bs-text-secondary);
  font-weight: 500;
}

.range-unit {
  color: var(--bs-text-secondary);
  font-size: var(--bs-font-size-base);
}

.view-hint {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: transparent;
  transition: all 0.2s;
}

.logo-preview:hover .view-hint {
  background: rgba(0, 0, 0, 0.3);
  color: #FFFFFF;
}

.view-hint span {
  font-size: var(--bs-font-size-sm);
}

.image-preview-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  animation: fadeIn 0.2s ease;
}

.preview-content {
  max-width: 90%;
  max-height: 90%;
  background: #FFFFFF;
  padding: 20px;
  border-radius: 12px;
  animation: scaleIn 0.2s ease;
}

.preview-content img {
  max-width: 100%;
  max-height: 80vh;
  object-fit: contain;
  border-radius: 8px;
}

@keyframes scaleIn {
  from { transform: scale(0.9); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}
</style>
