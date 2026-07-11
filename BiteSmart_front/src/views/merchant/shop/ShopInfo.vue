<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload, Delete } from '@element-plus/icons-vue'
import { getShopInfo, updateShopInfo, uploadFile } from '../../../api/merchant/shop'

const loading = ref(false)
const form = ref({
  shopName: '',
  shopLogo: '',
  contactPhone: '',
  shopAddress: '',
  businessHours: '',
  shopNotice: ''
})

const imageUrl = ref('')

const fetchShopInfo = async () => {
  loading.value = true
  try {
    const res = await getShopInfo()
    if (res.code === 200) {
      const data = res.data
      form.value.shopName = data.shopName || ''
      form.value.shopLogo = data.shopLogo || ''
      form.value.contactPhone = data.contactPhone || ''
      form.value.shopAddress = data.shopAddress || ''
      form.value.businessHours = data.businessHours || ''
      form.value.shopNotice = data.shopNotice || ''
      if (form.value.shopLogo) {
        imageUrl.value = form.value.shopLogo.startsWith('http') ? form.value.shopLogo : `${import.meta.env.VITE_APP_BASE_URL}${form.value.shopLogo}`
      }
    }
  } catch (e) {
    console.error('获取店铺信息失败', e)
  } finally {
    loading.value = false
  }
}

const handleLogoUpload = async (file: any) => {
  const reader = new FileReader()
  reader.onload = async (e) => {
    imageUrl.value = e.target?.result as string
    try {
      const res = await uploadFile(file.raw, 'license')
      if (res.code === 200) {
        form.value.shopLogo = res.data.url
        ElMessage.success('Logo上传成功')
      } else {
        ElMessage.error(res.message || '上传失败')
        imageUrl.value = ''
      }
    } catch (e) {
      ElMessage.error('上传失败')
      imageUrl.value = ''
      console.error('上传Logo失败', e)
    }
  }
  reader.readAsDataURL(file.raw)
  return false
}

const handleRemoveLogo = () => {
  form.value.shopLogo = ''
  imageUrl.value = ''
}

const handleSave = async () => {
  loading.value = true
  try {
    const submitData = { ...form.value } as Record<string, any>
    Object.keys(submitData).forEach(key => {
      if (submitData[key] === '') {
        submitData[key] = null
      }
    })
    const res = await updateShopInfo(submitData as any)
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
            <div v-if="imageUrl" class="logo-preview">
              <img :src="imageUrl" alt="店铺Logo" class="logo-image" />
              <button class="remove-btn" @click="handleRemoveLogo">
                <Delete style="width: 16px; height: 16px;" />
              </button>
            </div>
            <div v-else class="logo-upload">
              <el-upload
                class="avatar-uploader"
                :show-file-list="false"
                :before-upload="handleLogoUpload"
                accept="image/jpeg,image/png,image/gif"
              >
                <div class="upload-btn">
                  <Upload style="width: 40px; height: 40px;" />
                  <span>点击上传Logo</span>
                </div>
              </el-upload>
            </div>
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
          </el-form-item>
          <el-form-item label="店铺地址">
            <el-input v-model="form.shopAddress" placeholder="请输入店铺地址" />
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

.logo-preview {
  position: relative;
  width: 120px;
  height: 120px;
}

.logo-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
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
}

.remove-btn:hover {
  background: #c9302c;
}

.logo-upload {
  width: 120px;
  height: 120px;
}

.avatar-uploader {
  width: 100%;
  height: 100%;
}

.upload-btn {
  width: 100%;
  height: 100%;
  border: 2px dashed var(--bs-border-color);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--bs-text-muted);
  transition: all 0.2s;
}

.upload-btn:hover {
  border-color: var(--bs-primary);
  color: var(--bs-primary);
}

.upload-btn span {
  font-size: var(--bs-font-size-sm);
  margin-top: 8px;
}
</style>