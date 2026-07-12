﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getCurrentUser, getProfile, saveProfile } from '../../../api/user/profile'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const editing = ref(false)
const userInfo = ref<any>({})
const profile = ref<any>({})

const form = ref({
  nickname: '',
  phone: '',
  email: '',
  age: 0,
  gender: 0,
  height: 0,
  weight: 0,
  activityLevel: 0,
  dietaryRestrictions: '',
  healthGoals: ''
})

const genderOptions = [
  { label: '保密', value: 0 },
  { label: '男', value: 1 },
  { label: '女', value: 2 }
]

const activityLevelOptions = [
  { label: '久坐', value: 1 },
  { label: '轻度活动', value: 2 },
  { label: '中度活动', value: 3 },
  { label: '高度活动', value: 4 },
  { label: '极度活动', value: 5 }
]

const fetchData = async () => {
  loading.value = true
  try {
    const [userRes, profileRes] = await Promise.all([
      getCurrentUser(),
      getProfile()
    ])
    userInfo.value = userRes.data || {}
    profile.value = profileRes.data || {}

    form.value = {
      nickname: userInfo.value.nickname || '',
      phone: userInfo.value.phone || '',
      email: userInfo.value.email || '',
      age: profile.value.age || 0,
      gender: profile.value.gender || 0,
      height: profile.value.height || 0,
      weight: profile.value.weight || 0,
      activityLevel: profile.value.activityLevel || 0,
      dietaryRestrictions: profile.value.dietaryRestrictions || '',
      healthGoals: profile.value.healthGoals || ''
    }
  } catch (e) {
    console.error('获取个人信息失败', e)
  } finally {
    loading.value = false
  }
}

const toggleEdit = () => {
  if (editing.value) {
    handleSave()
  } else {
    editing.value = true
  }
}

const handleSave = async () => {
  loading.value = true
  try {
    await saveProfile({
      age: form.value.age,
      gender: form.value.gender,
      height: form.value.height,
      weight: form.value.weight,
      activityLevel: form.value.activityLevel,
      dietaryRestrictions: form.value.dietaryRestrictions,
      healthGoals: form.value.healthGoals
    } as any)
    ElMessage.success('保存成功')
    editing.value = false
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    loading.value = false
  }
}

const handleCancel = () => {
  editing.value = false
  fetchData()
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>个人中心</h3>
        <div style="display: flex; gap: 8px;">
          <el-button v-if="editing" size="small" @click="handleCancel">取消</el-button>
          <el-button type="primary" size="small" :loading="loading" @click="toggleEdit">
            {{ editing ? '保存' : '编辑信息' }}
          </el-button>
        </div>
      </div>
      <div style="padding-top: 20px;">
        <el-form v-loading="loading" :model="form" label-width="120px">
          <el-form-item label="用户名">
            <el-input :model-value="userInfo.username" disabled />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-if="editing" v-model="form.nickname" placeholder="请输入昵称" />
            <span v-else>{{ form.nickname || '未设置' }}</span>
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-if="editing" v-model="form.phone" placeholder="请输入手机号" />
            <span v-else>{{ form.phone || '未设置' }}</span>
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-if="editing" v-model="form.email" placeholder="请输入邮箱" />
            <span v-else>{{ form.email || '未设置' }}</span>
          </el-form-item>
          <el-divider>健康档案</el-divider>
          <el-form-item label="年龄">
            <el-input-number v-if="editing" v-model="form.age" :min="0" :max="150" />
            <span v-else>{{ form.age || '未设置' }}</span>
          </el-form-item>
          <el-form-item label="性别">
            <el-select v-if="editing" v-model="form.gender">
              <el-option v-for="opt in genderOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
            <span v-else>{{ genderOptions.find(g => g.value === form.gender)?.label || '未设置' }}</span>
          </el-form-item>
          <el-form-item label="身高(cm)">
            <el-input-number v-if="editing" v-model="form.height" :min="0" :max="250" />
            <span v-else>{{ form.height || '未设置' }}</span>
          </el-form-item>
          <el-form-item label="体重(kg)">
            <el-input-number v-if="editing" v-model="form.weight" :min="0" :max="300" :precision="1" />
            <span v-else>{{ form.weight || '未设置' }}</span>
          </el-form-item>
          <el-form-item label="活动水平">
            <el-select v-if="editing" v-model="form.activityLevel">
              <el-option v-for="opt in activityLevelOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
            <span v-else>{{ activityLevelOptions.find(a => a.value === form.activityLevel)?.label || '未设置' }}</span>
          </el-form-item>
          <el-form-item label="饮食限制">
            <el-input v-if="editing" v-model="form.dietaryRestrictions" type="textarea" :rows="2" />
            <span v-else>{{ form.dietaryRestrictions || '无' }}</span>
          </el-form-item>
          <el-form-item label="健康目标">
            <el-input v-if="editing" v-model="form.healthGoals" type="textarea" :rows="2" />
            <span v-else>{{ form.healthGoals || '无' }}</span>
          </el-form-item>
          <el-form-item label="会员中心">
            <el-button size="small">查看会员权益</el-button>
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
