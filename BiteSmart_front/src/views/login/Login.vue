<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Key, User, Lock, View, Unlock } from '@element-plus/icons-vue'
import { login } from '../../api/auth'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()

const username = ref('')
const password = ref('')
const roleType = ref(40)
const showPassword = ref(false)
const loading = ref(false)
const error = ref('')

const handleLogin = async () => {
  if (!username.value || !password.value) {
    error.value = '请输入账号和密码'
    return
  }
  
  loading.value = true
  error.value = ''
  
  try {
    const response = await login({
      username: username.value,
      password: password.value
    })
    
    if (response.code === 200) {
      const { token, user } = response.data
      const role = String(user.roleType)
      
      userStore.login(token, role, user)
      
      if (role === '40' || role === 'ADMIN') {
        router.push('/admin/dashboard')
      } else if (role === '20' || role === 'MERCHANT') {
        router.push('/merchant/dashboard')
      } else {
        router.push('/user')
      }
    } else {
      error.value = response.message || '登录失败'
    }
  } catch (err: any) {
    error.value = err.message || '登录失败，请重试'
  } finally {
    loading.value = false
  }
}

const roleOptions = [
  { value: 10, label: '普通用户' },
  { value: 20, label: '商家' },
  { value: 40, label: '管理员' }
]
</script>

<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-header">
        <div class="logo">
          <Key style="font-size: 40px; color: #1B3A2F;" />
          <span>BiteSmart</span>
        </div>
        <h1>智能健康膳食管理平台</h1>
        <p>登录您的账户</p>
      </div>
      
      <form class="login-form" @submit.prevent="handleLogin">
        <div class="form-group">
          <label>角色类型</label>
          <div class="role-selector">
            <button
              v-for="option in roleOptions"
              :key="option.value"
              type="button"
              :class="{ active: roleType === option.value }"
              @click="roleType = option.value"
            >
              {{ option.label }}
            </button>
          </div>
        </div>
        
        <div class="form-group">
          <label>账号</label>
          <div class="input-wrapper">
            <User style="color: var(--bs-text-muted);" />
            <input
              type="text"
              v-model="username"
              placeholder="请输入账号"
              autocomplete="username"
            />
          </div>
        </div>
        
        <div class="form-group">
          <label>密码</label>
          <div class="input-wrapper">
            <Lock style="color: var(--bs-text-muted);" />
            <input
              :type="showPassword ? 'text' : 'password'"
              v-model="password"
              placeholder="请输入密码"
              autocomplete="current-password"
            />
            <button type="button" class="toggle-password" @click="showPassword = !showPassword">
              <View v-if="showPassword" />
              <Unlock v-else />
            </button>
          </div>
        </div>
        
        <div v-if="error" class="error-message">
          {{ error }}
        </div>
        
        <button type="submit" class="login-btn" :disabled="loading">
          <span v-if="loading">登录中...</span>
          <span v-else>登 录</span>
        </button>
      </form>
      
      <div class="login-footer">
        <span>© 2026 BiteSmart. All rights reserved.</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1B3A2F 0%, #2D5A45 50%, #3D7A5A 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--bs-spacing-lg);
}

.login-container {
  background: #FFFFFF;
  border-radius: var(--bs-radius-lg);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  padding: var(--bs-spacing-xl);
  width: 100%;
  max-width: 420px;
}

.login-header {
  text-align: center;
  margin-bottom: var(--bs-spacing-xl);
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  font-size: var(--bs-font-size-2xl);
  font-weight: 700;
  color: var(--bs-primary);
  margin-bottom: var(--bs-spacing-md);
}

.login-header h1 {
  font-size: var(--bs-font-size-xl);
  font-weight: 600;
  color: var(--bs-text-title);
  margin-bottom: var(--bs-spacing-xs);
}

.login-header p {
  font-size: var(--bs-font-size-sm);
  color: var(--bs-text-muted);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: var(--bs-spacing-lg);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--bs-spacing-sm);
}

.form-group label {
  font-size: var(--bs-font-size-sm);
  font-weight: 500;
  color: var(--bs-text-title);
}

.role-selector {
  display: flex;
  gap: var(--bs-spacing-sm);
}

.role-selector button {
  flex: 1;
  padding: 10px;
  border: 1px solid var(--bs-border-light);
  border-radius: var(--bs-radius-md);
  background: transparent;
  color: var(--bs-text-body);
  font-size: var(--bs-font-size-sm);
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}

.role-selector button:hover {
  border-color: var(--bs-primary);
  color: var(--bs-primary);
}

.role-selector button.active {
  background: var(--bs-primary);
  border-color: var(--bs-primary);
  color: #FFFFFF;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  border: 1px solid var(--bs-border-light);
  border-radius: var(--bs-radius-md);
  padding: 0 var(--bs-spacing-md);
  transition: all 0.15s;
}

.input-wrapper:focus-within {
  border-color: var(--bs-primary);
  box-shadow: 0 0 0 3px rgba(27, 58, 47, 0.05);
}

.input-wrapper svg {
  font-size: var(--bs-font-size-lg);
}

.input-wrapper input {
  flex: 1;
  padding: var(--bs-spacing-md);
  border: none;
  outline: none;
  font-size: var(--bs-font-size-base);
  color: var(--bs-text-title);
}

.input-wrapper input::placeholder {
  color: var(--bs-text-muted);
}

.toggle-password {
  background: none;
  border: none;
  color: var(--bs-text-muted);
  cursor: pointer;
  padding: 4px;
  border-radius: var(--bs-radius-sm);
  transition: all 0.15s;
}

.toggle-password:hover {
  background: var(--bs-bg-hover);
  color: var(--bs-text-title);
}

.error-message {
  font-size: var(--bs-font-size-sm);
  color: #D9534F;
  text-align: center;
}

.login-btn {
  background: var(--bs-primary);
  color: #FFFFFF;
  border: none;
  padding: var(--bs-spacing-md);
  border-radius: var(--bs-radius-md);
  font-size: var(--bs-font-size-base);
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}

.login-btn:hover:not(:disabled) {
  background: var(--bs-primary-hover);
}

.login-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.login-footer {
  text-align: center;
  margin-top: var(--bs-spacing-xl);
  padding-top: var(--bs-spacing-lg);
  border-top: 1px solid var(--bs-border-light);
}

.login-footer span {
  font-size: var(--bs-font-size-xs);
  color: var(--bs-text-muted);
}
</style>