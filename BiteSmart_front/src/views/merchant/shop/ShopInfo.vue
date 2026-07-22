<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload, Delete, Edit } from '@element-plus/icons-vue'
import { getShopInfo, updateShopInfo, updateShopOpenStatus, uploadFile } from '../../../api/merchant/shop'
import { resolveFileUrl } from '../../../utils/fileUrl'
import { useUserStore } from '../../../stores/user'

const userStore = useUserStore()
const loading = ref(false)
const editing = ref(false)
/** 营业状态：10-营业中 20-打烊（缺省按营业中处理） */
const openStatus = ref(10)
const openStatusSaving = ref(false)
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
/** 营业执照图片 */
const licenseUrl = ref('')
const licenseInput = ref<HTMLInputElement | null>(null)
const selectedLicenseFile = ref<File | null>(null)

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
      openStatus.value = data.openStatus === 20 ? 20 : 10
      userStore.setShopOpenStatus(openStatus.value)
      if (form.value.shopLogo) {
        imageUrl.value = resolveFileUrl(form.value.shopLogo)
      }
      licenseUrl.value = form.value.businessLicense ? resolveFileUrl(form.value.businessLicense) : ''
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

const handleLicenseSelect = (event: Event) => {
  if (!editing.value) return
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  selectedLicenseFile.value = file
  const reader = new FileReader()
  reader.onload = (e) => {
    licenseUrl.value = e.target?.result as string
  }
  reader.readAsDataURL(file)
  target.value = ''
}

const handleRemoveLicense = () => {
  if (!editing.value) return
  form.value.businessLicense = ''
  licenseUrl.value = ''
  selectedLicenseFile.value = null
}

const triggerLicenseUpload = () => {
  if (!editing.value) return
  licenseInput.value?.click()
}

const handleEdit = () => {
  editing.value = true
}

const handleCancel = () => {
  editing.value = false
  selectedFile.value = null
  selectedLicenseFile.value = null
  fetchShopInfo()
}

// 保存前的基础校验：电话格式、营业时间先后顺序
const validateForm = () => {
  if (form.value.contactPhone && !/^(1[3-9]\d{9}|0\d{2,3}-?\d{7,8})$/.test(form.value.contactPhone)) {
    ElMessage.warning('联系电话格式不正确')
    return false
  }
  if (businessHoursForm.value.weekdayStart >= businessHoursForm.value.weekdayEnd) {
    ElMessage.warning('工作日营业时间：开始时间需早于结束时间')
    return false
  }
  if (businessHoursForm.value.weekendStart >= businessHoursForm.value.weekendEnd) {
    ElMessage.warning('周末营业时间：开始时间需早于结束时间')
    return false
  }
  if (deliveryRangeForm.value.minDistance >= deliveryRangeForm.value.maxDistance) {
    ElMessage.warning('配送范围：最小距离需小于最大距离')
    return false
  }
  return true
}

const handleSave = async () => {
  if (!validateForm()) return
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

    if (selectedLicenseFile.value) {
      const licenseRes = await uploadFile(selectedLicenseFile.value, 'shop_license')
      if (licenseRes.code === 200) {
        form.value.businessLicense = licenseRes.data.url
        selectedLicenseFile.value = null
      } else {
        ElMessage.error(licenseRes.message || '营业执照上传失败')
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

const handleOpenStatusChange = async (value: string | number | boolean) => {
  const next = value ? 10 : 20
  openStatusSaving.value = true
  try {
    const res = await updateShopOpenStatus(next)
    if (res.code === 200) {
      openStatus.value = next
      userStore.setShopOpenStatus(next)
      ElMessage.success(next === 10 ? '已恢复营业' : '店铺已打烊，用户将无法下单')
    } else {
      ElMessage.error(res.message || '营业状态更新失败')
    }
  } catch (e) {
    console.error('更新营业状态失败', e)
    ElMessage.error('营业状态更新失败')
  } finally {
    openStatusSaving.value = false
  }
}

const triggerUpload = () => {
  if (!editing.value) return
  uploadInput.value?.click()
}

onMounted(() => {
  fetchShopInfo()
})
</script>

<template>
  <div class="page-container">
    <div class="shop-page">
      <div class="page-head">
        <div>
          <div class="en">SHOP</div>
          <h2>店铺管理</h2>
          <p>维护店铺资料、营业状态与配送范围</p>
        </div>
        <div class="page-head-actions">
          <el-button v-if="!editing" :icon="Edit" @click="handleEdit">修改资料</el-button>
          <template v-else>
            <el-button @click="handleCancel">取消</el-button>
            <el-button type="primary" :loading="loading" @click="handleSave">保存修改</el-button>
          </template>
        </div>
      </div>

      <el-form :model="form" label-width="100px" v-loading="loading" class="shop-form">
        <!-- 基本信息 -->
        <div class="card-panel form-card">
          <div class="section-head">
            <div>
              <div class="en">BASIC</div>
              <h3>基本信息</h3>
            </div>
          </div>
          <el-form-item label="店铺Logo">
            <div class="logo-preview" @click="editing ? triggerUpload() : undefined">
              <el-image
                v-if="imageUrl"
                :src="imageUrl"
                fit="cover"
                class="logo-image"
                :preview-src-list="editing ? [] : [imageUrl]"
                preview-teleported
              />
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
            </div>
            <input
              ref="uploadInput"
              type="file"
              accept="image/jpeg,image/png,image/gif"
              class="hidden-input"
              @change="handleLogoSelect"
            />
          </el-form-item>
          <el-form-item label="店铺名称">
            <el-input v-if="editing" v-model="form.shopName" placeholder="请输入店铺名称" />
            <span v-else class="field-text" :class="{ empty: !form.shopName }">{{ form.shopName || '未填写' }}</span>
          </el-form-item>
          <el-form-item label="联系人">
            <el-input v-if="editing" v-model="form.contactName" placeholder="请输入联系人姓名" />
            <span v-else class="field-text" :class="{ empty: !form.contactName }">{{ form.contactName || '未填写' }}</span>
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-if="editing" v-model="form.contactPhone" placeholder="请输入联系电话" />
            <span v-else class="field-text" :class="{ empty: !form.contactPhone }">{{ form.contactPhone || '未填写' }}</span>
          </el-form-item>
          <el-form-item label="店铺地址">
            <el-input v-if="editing" v-model="form.shopAddress" placeholder="请输入店铺地址" />
            <span v-else class="field-text" :class="{ empty: !form.shopAddress }">{{ form.shopAddress || '未填写' }}</span>
          </el-form-item>
          <el-form-item label="执照编号">
            <el-input v-if="editing" v-model="form.licenseNumber" placeholder="请输入统一社会信用代码" />
            <span v-else class="field-text" :class="{ empty: !form.licenseNumber }">{{ form.licenseNumber || '未填写' }}</span>
          </el-form-item>
          <el-form-item label="营业执照">
            <div class="logo-preview" @click="editing ? triggerLicenseUpload() : undefined">
              <el-image
                v-if="licenseUrl"
                :src="licenseUrl"
                fit="cover"
                class="logo-image"
                :preview-src-list="editing ? [] : [licenseUrl]"
                preview-teleported
              />
              <div v-else class="license-empty">{{ editing ? '点击上传营业执照' : '未上传' }}</div>
              <button class="remove-btn" @click.stop="handleRemoveLicense" v-if="licenseUrl && editing">
                <Delete style="width: 16px; height: 16px;" />
              </button>
            </div>
            <input
              ref="licenseInput"
              type="file"
              accept="image/jpeg,image/png,image/gif"
              class="hidden-input"
              @change="handleLicenseSelect"
            />
          </el-form-item>
          <el-form-item label="店铺公告">
            <el-input v-if="editing" v-model="form.shopNotice" type="textarea" :rows="3" placeholder="请输入店铺公告" />
            <span v-else class="field-text" :class="{ empty: !form.shopNotice }">{{ form.shopNotice || '未填写' }}</span>
          </el-form-item>
        </div>

        <!-- 营业信息 -->
        <div class="card-panel form-card">
          <div class="section-head">
            <div>
              <div class="en">BUSINESS</div>
              <h3>营业信息</h3>
            </div>
          </div>
          <el-form-item label="营业状态">
            <div class="open-status-row">
              <el-switch
                :model-value="openStatus !== 20"
                :loading="openStatusSaving"
                active-text="营业中"
                inactive-text="打烊中"
                inline-prompt
                @change="handleOpenStatusChange"
              />
              <el-tag :type="openStatus === 20 ? 'warning' : 'success'" effect="plain" size="small">
                {{ openStatus === 20 ? '打烊中' : '营业中' }}
              </el-tag>
              <span class="open-status-hint">打烊后用户将无法下单，可随时切换</span>
            </div>
          </el-form-item>
          <el-form-item label="营业时间">
            <div class="business-hours-container">
              <div class="hours-row">
                <span class="hours-label">工作日（周一至周五）</span>
                <template v-if="editing">
                  <el-time-picker
                    v-model="businessHoursForm.weekdayStart"
                    format="HH:mm"
                    value-format="HH:mm"
                    placeholder="开始时间"
                    class="time-picker"
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
                    :picker-options="{
                      selectableRange: '00:00 - 23:59',
                      step: '00:30'
                    }"
                  />
                </template>
                <span v-else class="field-text">{{ weekdayTime }}</span>
              </div>
              <div class="hours-row">
                <span class="hours-label">周末（周六至周日）</span>
                <template v-if="editing">
                  <el-time-picker
                    v-model="businessHoursForm.weekendStart"
                    format="HH:mm"
                    value-format="HH:mm"
                    placeholder="开始时间"
                    class="time-picker"
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
                    :picker-options="{
                      selectableRange: '00:00 - 23:59',
                      step: '00:30'
                    }"
                  />
                </template>
                <span v-else class="field-text">{{ weekendTime }}</span>
              </div>
            </div>
          </el-form-item>
        </div>

        <!-- 配送信息 -->
        <div class="card-panel form-card">
          <div class="section-head">
            <div>
              <div class="en">DELIVERY</div>
              <h3>配送信息</h3>
            </div>
          </div>
          <el-form-item label="配送范围">
            <div v-if="editing" class="delivery-range-container">
              <el-input-number
                v-model="deliveryRangeForm.minDistance"
                :min="0"
                :max="50"
                class="range-input"
                placeholder="最小距离"
              />
              <span class="range-separator">-</span>
              <el-input-number
                v-model="deliveryRangeForm.maxDistance"
                :min="1"
                :max="50"
                class="range-input"
                placeholder="最大距离"
              />
              <span class="range-unit">{{ deliveryRangeForm.unit }}</span>
            </div>
            <span v-else class="field-text">{{ deliveryRangeForm.minDistance }} - {{ deliveryRangeForm.maxDistance }} {{ deliveryRangeForm.unit }}</span>
          </el-form-item>
        </div>
      </el-form>
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

.shop-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-head h2 {
  margin: 0;
  color: var(--bs-text-title);
  font-size: 20px;
  font-weight: 650;
}

.page-head .en {
  color: var(--faint);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 2px;
}

.page-head p {
  margin-top: 4px;
  color: var(--bs-text-muted);
  font-size: 13px;
}

.page-head-actions {
  display: flex;
  gap: 8px;
}

.shop-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-panel {
  background: var(--bs-card-bg);
  border-radius: var(--bs-radius-md);
  box-shadow: var(--bs-card-shadow);
  padding: var(--bs-spacing-lg);
}

.section-head {
  margin-bottom: 18px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--bs-border-light);
}

.section-head h3 {
  margin: 0;
  font-size: var(--bs-font-size-lg);
  font-weight: 600;
  color: var(--bs-text-title);
}

.section-head .en {
  color: var(--faint);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 2px;
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
  display: block;
  border-radius: 8px;
  border: 1px solid var(--bs-border-light);
}

.logo-placeholder {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  background: var(--green-soft);
  color: var(--green-deep);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  font-weight: 700;
  border: 1px solid var(--bs-border-light);
}

.license-empty {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  border: 1px dashed var(--bs-border-light);
  color: var(--bs-text-muted);
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 8px;
  box-sizing: border-box;
}

.remove-btn {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 24px;
  height: 24px;
  background: var(--bs-status-danger);
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
  background: var(--danger-brand);
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
  color: var(--bs-text-muted);
}

.time-picker {
  width: 120px;
}

.time-separator {
  color: var(--bs-text-muted);
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
  color: var(--bs-text-muted);
  font-weight: 500;
}

.range-unit {
  color: var(--bs-text-muted);
  font-size: var(--bs-font-size-base);
}

.open-status-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.open-status-hint {
  color: var(--bs-text-muted);
  font-size: var(--bs-font-size-sm);
}

.field-text {
  color: var(--bs-text-title);
  font-size: var(--bs-font-size-base);
  line-height: 32px;
  white-space: pre-wrap;
}

.field-text.empty {
  color: var(--bs-text-muted);
}

@media (max-width: 720px) {
  .page-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .hours-row {
    flex-wrap: wrap;
  }

  .hours-label {
    width: 100%;
  }
}
</style>
