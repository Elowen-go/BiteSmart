<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload, Delete } from '@element-plus/icons-vue'
import { getProfile, updateProfile, uploadFile } from '../../../api/merchant/profile'

const loading = ref(false)
const form = ref({
  nickname: '',
  avatar: '',
  phone: '',
  email: ''
})

const imageUrl = ref('')

const fetchProfile = async () => {
  loading.value = true
  try {
    const res = await getProfile()
    if (res.code === 200) {
      const data = res.data
      form.value.nickname = data.nickname || ''
      form.value.avatar = data.avatar || ''
      form.value.phone = data.phone || ''
      form.value.email = data.email || ''
      if (form.value.avatar) {
        imageUrl.value = form.value.avatar.startsWith('http') ? form.value.avatar : `${import.meta.env.VITE_APP_BASE_URL}${form.value.avatar}`
      }
    }
  } catch (e) {
    console.error('获取个人信息失败', e)
  } finally {
    loading.value = false
  }
}

const handleAvatarUpload = async (file: any) => {
  const reader = new FileReader()
  reader.onload = async (e) => {
    imageUrl.value = e.target?.result as string
    try {
      const res = await uploadFile(file.raw, 'avatar')
      if (res.code === 200) {
        form.value.avatar = res.data.url
        ElMessage.success('头像上传成功')
      } else {
        ElMessage.error(res.message || '上传失败')
        imageUrl.value = ''
      }
    } catch (e) {
      ElMessage.error('上传失败')
      imageUrl.value = ''
      console.error('上传头像失败', e)
    }
  }
  reader.readAsDataURL(file.raw)
  return false
}

const handleRemoveAvatar = () => {
  form.value.avatar = ''
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
    const res = await updateProfile(submitData as any)
    if (res.code === 200) {
      ElMessage.success('保存成功')
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败')
    console.error('保存个人信息失败', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchProfile()
})
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>个人信息</h3>
        <button class="btn btn-primary" @click="handleSave">保存修改</button>
      </div>
      <div style="padding-top: 20px;">
        <el-form :model="form" label-width="120px" v-loading="loading">
          <el-form-item label="头像">
            <div v-if="imageUrl" class="logo-preview">
              <img :src="imageUrl" alt="头像" class="logo-image" />
              <button class="remove-btn" @click="handleRemoveAvatar">
                <Delete style="width: 16px; height: 16px;" />
              </button>
            </div>
            <div v-else class="logo-upload">
              <el-upload
                class="avatar-uploader"
                :show-file-list="false"
                :before-upload="handleAvatarUpload"
                accept="image/jpeg,image/png,image/gif"
              >
                <div class="upload-btn">
                  <Upload style="width: 40px; height: 40px;" />
                  <span>点击上传头像</span>
                </div>
              </el-upload>
            </div>
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="form.nickname" placeholder="请输入昵称" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="form.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="form.email" placeholder="请输入邮箱" />
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
  border-radius: 50%;
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
  border-radius: 50%;
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