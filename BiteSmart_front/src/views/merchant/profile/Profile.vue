<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Delete } from '@element-plus/icons-vue'
import { getProfile, updateProfile, uploadFile } from '../../../api/merchant/profile'
import { useUserStore } from '../../../stores/user'
import { resolveFileUrl } from '../../../utils/fileUrl'

const loading = ref(false)
const formRef = ref()
const form = ref({
  nickname: '',
  avatar: '',
  phone: '',
  email: ''
})

const formRules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 20, message: '昵称不超过 20 个字符', trigger: 'blur' }
  ],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email' as const, message: '邮箱格式不正确', trigger: 'blur' }]
}

const imageUrl = ref('')
const userStore = useUserStore()

const syncUserStore = (patch: Record<string, any>) => {
  userStore.setUserInfo({ ...(userStore.userInfo || {}), ...patch })
}

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
        imageUrl.value = resolveFileUrl(form.value.avatar)
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
        const avatar = res.data.url
        const saveRes = await updateProfile({ avatar })
        if (saveRes.code !== 200) throw new Error(saveRes.message || '头像保存失败')
        form.value.avatar = avatar
        syncUserStore({ avatar })
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

// 与“上传即生效”对齐：移除也即时生效
const handleRemoveAvatar = () => {
  ElMessageBox.confirm('确定要移除当前头像吗？移除后立即生效。', '移除头像', {
    confirmButtonText: '移除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await updateProfile({ avatar: null } as any)
      if (res.code === 200) {
        form.value.avatar = ''
        imageUrl.value = ''
        syncUserStore({ avatar: '' })
        ElMessage.success('头像已移除')
      } else {
        ElMessage.error(res.message || '移除失败')
      }
    } catch (e) {
      ElMessage.error('移除失败')
    }
  }).catch(() => {})
}

const handleSave = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
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
      syncUserStore({ nickname: form.value.nickname, avatar: form.value.avatar })
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
    <div class="profile-page">
      <div class="page-head">
        <div>
          <div class="en">PROFILE</div>
          <h2>我的</h2>
          <p>维护商家账号的个人资料与联系方式</p>
        </div>
        <el-button type="primary" :loading="loading" @click="handleSave">保存修改</el-button>
      </div>

      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px" v-loading="loading" class="profile-form">
        <!-- 个人信息 -->
        <div class="card-panel form-card">
          <div class="section-head">
            <div>
              <div class="en">BASIC</div>
              <h3>个人信息</h3>
            </div>
          </div>
          <el-form-item label="头像">
            <div class="logo-preview">
              <el-image
                v-if="imageUrl"
                :src="imageUrl"
                fit="cover"
                class="logo-image"
                :preview-src-list="[imageUrl]"
                preview-teleported
              />
              <div v-else class="avatar-placeholder">
                {{ (form.nickname || '用')[0] }}
              </div>
              <button class="remove-btn" @click="handleRemoveAvatar" v-if="imageUrl">
                <Delete style="width: 16px; height: 16px;" />
              </button>
              <el-upload
                class="avatar-uploader"
                :show-file-list="false"
                :before-upload="handleAvatarUpload"
                accept="image/jpeg,image/png,image/gif"
              >
                <div class="upload-overlay">
                  <Upload style="width: 20px; height: 20px;" />
                </div>
              </el-upload>
            </div>
            <div class="avatar-tip">点击头像上传新图片，即时生效；支持 JPG / PNG / GIF</div>
          </el-form-item>
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="form.nickname" placeholder="请输入昵称" maxlength="20" show-word-limit />
          </el-form-item>
        </div>

        <!-- 账号安全 -->
        <div class="card-panel form-card">
          <div class="section-head">
            <div>
              <div class="en">ACCOUNT</div>
              <h3>账号安全</h3>
            </div>
            <span class="section-tip">手机号与邮箱用于登录验证和找回账号</span>
          </div>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" placeholder="请输入邮箱" />
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

.profile-page {
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

.profile-form {
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
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
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

.section-tip {
  color: var(--bs-text-muted);
  font-size: 12px;
}

.logo-preview {
  position: relative;
  width: 120px;
  height: 120px;
}

.logo-image {
  width: 100%;
  height: 100%;
  display: block;
  border-radius: 50%;
  border: 1px solid var(--bs-border-light);
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: var(--green-soft);
  color: var(--green-deep);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  font-weight: 700;
  border: 1px solid var(--bs-border-light);
}

.avatar-tip {
  margin-top: 8px;
  color: var(--bs-text-muted);
  font-size: 12px;
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

.avatar-uploader {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  cursor: pointer;
}

.upload-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0);
  display: flex;
  align-items: center;
  justify-content: center;
  color: transparent;
  transition: all 0.2s;
}

.avatar-uploader:hover .upload-overlay {
  background: rgba(0, 0, 0, 0.3);
  color: #FFFFFF;
}

@media (max-width: 720px) {
  .page-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
