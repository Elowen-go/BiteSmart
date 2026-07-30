<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Hide, Key, Lock, User, View } from '@element-plus/icons-vue'
import { login, register } from '../../api/auth'
import { useUserStore } from '../../stores/user'

type AuthMode = 'login' | 'register'

const router = useRouter()
const userStore = useUserStore()

const mode = ref<AuthMode>('login')
const username = ref('')
const password = ref('')
const nickname = ref('')
const phone = ref('')
const confirmPassword = ref('')
const roleType = ref(10)
const registerRoleType = ref(10)
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const loading = ref(false)
const error = ref('')

const roleOptions = [
  { value: 10, label: '普通用户', hint: '健康饮食与订单' },
  { value: 20, label: '商家', hint: '门店经营管理' },
  { value: 40, label: '管理员', hint: '平台运营管理' }
]

const registerRoleOptions = roleOptions.filter((option) => option.value !== 40)

const storeSession = (token: string, user: { roleType: number; [key: string]: unknown }) => {
  userStore.login(token, String(user.roleType), user)
}

const redirectByRole = (token: string, user: { roleType: number; [key: string]: unknown }) => {
  const role = String(user.roleType)
  storeSession(token, user)

  if (role === '40' || role === 'ADMIN') {
    router.push('/admin/dashboard')
  } else if (role === '20' || role === 'MERCHANT') {
    router.push('/merchant/dashboard')
  } else {
    router.push('/user')
  }
}

const getErrorMessage = (err: any, fallback: string) => {
  return err?.response?.data?.message || err?.message || fallback
}

const handleLogin = async () => {
  if (!username.value.trim() || !password.value) {
    error.value = '请输入账号和密码'
    return
  }

  loading.value = true
  error.value = ''

  try {
    const response = await login({
      username: username.value.trim(),
      password: password.value,
      roleType: roleType.value
    })

    if (response.code === 200) {
      redirectByRole(response.data.token, response.data.user)
    } else {
      error.value = response.message || '登录失败'
    }
  } catch (err: any) {
    error.value = getErrorMessage(err, '登录失败，请检查账号和密码')
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  if (!username.value.trim() || !password.value) {
    error.value = '请填写账号和密码'
    return
  }
  if (password.value.length < 6) {
    error.value = '密码至少需要 6 位字符'
    return
  }
  if (password.value !== confirmPassword.value) {
    error.value = '两次输入的密码不一致'
    return
  }
  if (phone.value && !/^1[3-9]\d{9}$/.test(phone.value)) {
    error.value = '请输入正确的手机号码'
    return
  }

  loading.value = true
  error.value = ''

  try {
    const response = await register({
      username: username.value.trim(),
      password: password.value,
      nickname: nickname.value.trim() || undefined,
      phone: phone.value.trim() || undefined,
      roleType: registerRoleType.value
    })

    if (response.code === 200) {
      storeSession(response.data.token, response.data.user)
      if (registerRoleType.value === 20) {
        router.push('/merchant/apply')
      } else {
        router.push('/user')
      }
    } else {
      error.value = response.message || '注册失败'
    }
  } catch (err: any) {
    error.value = getErrorMessage(err, '注册失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const handleSubmit = () => {
  if (mode.value === 'login') {
    handleLogin()
  } else {
    handleRegister()
  }
}

const switchMode = (nextMode: AuthMode) => {
  mode.value = nextMode
  error.value = ''
  password.value = ''
  confirmPassword.value = ''
  showPassword.value = false
  showConfirmPassword.value = false
}
</script>

<template>
  <main class="auth-page">
    <div class="auth-shell">
      <section class="brand-panel" aria-label="BiteSmart">
        <div class="brand-lockup">
          <span class="brand-mark"><Key /></span>
          <span class="brand-name">BiteSmart</span>
        </div>

        <div class="brand-copy">
          <p class="eyebrow">SMART NUTRITION PLATFORM</p>
          <h1>让每一餐，<br />都更懂你的健康。</h1>
          <p class="brand-description">
            从健康记录到餐品选择，把饮食管理放回真实、轻松的日常里。
          </p>
        </div>

        <div class="brand-footer">
          <span>健康管理</span>
          <span>智能膳食</span>
          <span>全程服务</span>
        </div>
      </section>

      <section class="auth-card">
        <div class="auth-heading">
          <p class="auth-kicker">WELCOME BACK</p>
          <h2>{{ mode === 'login' ? '登录 BiteSmart' : '创建你的账号' }}</h2>
          <p>
            {{ mode === 'login'
              ? '登录后继续管理你的健康饮食'
              : (registerRoleType === 20 ? '注册商家账号，提交店铺入驻资料' : '注册普通用户账号，开启健康饮食记录') }}
          </p>
        </div>

        <div class="auth-tabs" role="tablist" aria-label="登录或注册">
          <button
            type="button"
            role="tab"
            :aria-selected="mode === 'login'"
            :class="{ active: mode === 'login' }"
            @click="switchMode('login')"
          >
            登录
          </button>
          <button
            type="button"
            role="tab"
            :aria-selected="mode === 'register'"
            :class="{ active: mode === 'register' }"
            @click="switchMode('register')"
          >
            注册
          </button>
        </div>

        <form class="auth-form" @submit.prevent="handleSubmit">
          <template v-if="mode === 'login'">
            <div class="form-group">
              <div class="field-heading">
                <label>登录身份</label>
                <span>请选择对应入口</span>
              </div>
              <div class="role-selector">
                <button
                  v-for="option in roleOptions"
                  :key="option.value"
                  type="button"
                  :class="{ active: roleType === option.value }"
                  :aria-pressed="roleType === option.value"
                  @click="roleType = option.value"
                >
                  <strong>{{ option.label }}</strong>
                  <small>{{ option.hint }}</small>
                </button>
              </div>
            </div>
          </template>

          <template v-else>
            <div class="form-group">
              <div class="field-heading">
                <label>注册身份</label>
                <span>{{ registerRoleType === 20 ? '提交资料后等待审核' : '注册后即可使用' }}</span>
              </div>
              <div class="role-selector register-role-selector">
                <button
                  v-for="option in registerRoleOptions"
                  :key="option.value"
                  type="button"
                  :class="{ active: registerRoleType === option.value }"
                  :aria-pressed="registerRoleType === option.value"
                  @click="registerRoleType = option.value"
                >
                  <strong>{{ option.label }}</strong>
                  <small>{{ option.hint }}</small>
                </button>
              </div>
            </div>
          </template>

          <div class="form-group">
            <label for="username">账号</label>
            <div class="input-wrapper">
              <User class="field-icon" />
              <input
                id="username"
                v-model="username"
                type="text"
                placeholder="请输入账号"
                autocomplete="username"
              />
            </div>
          </div>

          <template v-if="mode === 'register'">
            <div class="field-row">
              <div class="form-group">
                <label for="nickname">昵称 <span class="optional">选填</span></label>
                <div class="input-wrapper">
                  <User class="field-icon" />
                  <input id="nickname" v-model="nickname" type="text" placeholder="怎么称呼你" />
                </div>
              </div>
              <div class="form-group">
                <label for="phone">手机号 <span class="optional">选填</span></label>
                <div class="input-wrapper">
                  <input id="phone" v-model="phone" type="tel" placeholder="用于账号找回" autocomplete="tel" />
                </div>
              </div>
            </div>
          </template>

          <div class="form-group">
            <label for="password">密码</label>
            <div class="input-wrapper">
              <Lock class="field-icon" />
              <input
                id="password"
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                :placeholder="mode === 'register' ? '至少 6 位字符' : '请输入密码'"
                :autocomplete="mode === 'register' ? 'new-password' : 'current-password'"
              />
              <button type="button" class="icon-button" :aria-label="showPassword ? '隐藏密码' : '显示密码'" @click="showPassword = !showPassword">
                <View v-if="showPassword" />
                <Hide v-else />
              </button>
            </div>
          </div>

          <div v-if="mode === 'register'" class="form-group">
            <label for="confirm-password">确认密码</label>
            <div class="input-wrapper">
              <Lock class="field-icon" />
              <input
                id="confirm-password"
                v-model="confirmPassword"
                :type="showConfirmPassword ? 'text' : 'password'"
                placeholder="请再次输入密码"
                autocomplete="new-password"
              />
              <button type="button" class="icon-button" :aria-label="showConfirmPassword ? '隐藏密码' : '显示密码'" @click="showConfirmPassword = !showConfirmPassword">
                <View v-if="showConfirmPassword" />
                <Hide v-else />
              </button>
            </div>
          </div>

          <p v-if="mode === 'register'" class="form-note">
            {{ registerRoleType === 20 ? '商家注册后需要提交店铺资料，并等待管理员审核通过。' : '普通用户注册成功后可直接使用健康记录、点餐和订单功能。' }}
          </p>

          <div v-if="error" class="error-message" role="alert">
            {{ error }}
          </div>

          <button type="submit" class="submit-button" :disabled="loading">
            <span>{{ loading ? (mode === 'login' ? '登录中...' : '注册中...') : (mode === 'login' ? '登录' : '创建账号') }}</span>
            <ArrowRight />
          </button>
        </form>

        <p class="mode-hint">
          <template v-if="mode === 'login'">
            还没有账号？
            <button type="button" @click="switchMode('register')">立即注册</button>
          </template>
          <template v-else>
            已有账号？
            <button type="button" @click="switchMode('login')">返回登录</button>
          </template>
        </p>

        <footer class="auth-footer">© 2026 BiteSmart · Smart nutrition for everyday life</footer>
      </section>
    </div>
  </main>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  box-sizing: border-box;
  padding: 32px;
  display: grid;
  place-items: center;
  background: #eef2ee;
}

.auth-shell {
  width: min(1080px, 100%);
  min-height: 680px;
  display: grid;
  grid-template-columns: minmax(360px, 0.88fr) minmax(480px, 1.12fr);
  overflow: hidden;
  border: 1px solid #dbe5de;
  border-radius: 24px;
  background: #ffffff;
  box-shadow: 0 24px 60px rgba(24, 55, 43, 0.12);
}

.brand-panel {
  min-height: 100%;
  box-sizing: border-box;
  padding: 52px 48px 42px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  color: #ffffff;
  background: #173c32;
}

.brand-lockup,
.brand-footer,
.field-heading,
.submit-button,
.auth-footer {
  display: flex;
  align-items: center;
}

.brand-lockup {
  gap: 12px;
}

.brand-mark {
  width: 38px;
  height: 38px;
  display: inline-grid;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.32);
  border-radius: 12px;
  color: #d7efcf;
}

.brand-mark svg {
  width: 21px;
  height: 21px;
}

.brand-name {
  font-size: 22px;
  font-weight: 750;
  letter-spacing: 0;
}

.brand-copy {
  max-width: 340px;
  margin-top: 80px;
}

.eyebrow,
.auth-kicker {
  margin: 0 0 18px;
  color: #a8d59d;
  font-size: 11px;
  font-weight: 750;
  letter-spacing: 1.8px;
}

.brand-copy h1 {
  margin: 0;
  color: #ffffff;
  font-size: 40px;
  line-height: 1.22;
  letter-spacing: 0;
}

.brand-description {
  margin: 24px 0 0;
  color: #c6d9d0;
  font-size: 15px;
  line-height: 1.8;
}

.brand-footer {
  gap: 18px;
  flex-wrap: wrap;
  color: #a9c4b9;
  font-size: 12px;
}

.brand-footer span + span::before {
  content: '/';
  margin-right: 18px;
  color: #557a6c;
}

.auth-card {
  box-sizing: border-box;
  padding: 54px 72px 34px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.auth-heading {
  margin-bottom: 25px;
}

.auth-kicker {
  margin-bottom: 10px;
  color: #4f8b62;
}

.auth-heading h2 {
  margin: 0;
  color: #1c3028;
  font-size: 30px;
  line-height: 1.25;
  letter-spacing: 0;
}

.auth-heading p:last-child {
  margin: 10px 0 0;
  color: #75857d;
  font-size: 14px;
}

.auth-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
  padding: 4px;
  margin-bottom: 25px;
  border: 1px solid #dfe8e1;
  border-radius: 10px;
  background: #f4f7f4;
}

.auth-tabs button {
  min-height: 38px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: #6d7c74;
  font-size: 14px;
  font-weight: 650;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease, box-shadow 0.18s ease;
}

.auth-tabs button:hover {
  color: #1f493a;
}

.auth-tabs button.active {
  background: #ffffff;
  color: #1f493a;
  box-shadow: 0 2px 8px rgba(27, 60, 50, 0.08);
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.form-group label,
.field-heading label {
  color: #30443a;
  font-size: 13px;
  font-weight: 700;
}

.field-heading {
  justify-content: space-between;
  gap: 12px;
}

.field-heading span,
.optional {
  color: #9aa8a0;
  font-size: 11px;
  font-weight: 500;
}

.role-selector {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.role-selector button {
  min-height: 58px;
  padding: 10px 8px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  border: 1px solid #dfe8e1;
  border-radius: 9px;
  background: #ffffff;
  color: #3a4e44;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.18s ease, background 0.18s ease, color 0.18s ease;
}

.role-selector button:hover {
  border-color: #7dab82;
}

.role-selector button.active {
  border-color: #2c634b;
  background: #edf6ed;
  color: #1f493a;
  box-shadow: inset 0 0 0 1px #2c634b;
}

.role-selector strong {
  font-size: 12px;
}

.role-selector small {
  color: #89988f;
  font-size: 10px;
  white-space: nowrap;
}

.input-wrapper {
  min-height: 46px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 13px;
  border: 1px solid #dbe5de;
  border-radius: 9px;
  background: #ffffff;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.input-wrapper:focus-within {
  border-color: #4f8b62;
  box-shadow: 0 0 0 3px rgba(79, 139, 98, 0.12);
}

.field-icon {
  flex: 0 0 auto;
  width: 17px;
  height: 17px;
  color: #8b9a92;
}

.input-wrapper input {
  min-width: 0;
  flex: 1;
  height: 44px;
  padding: 0;
  border: 0;
  outline: 0;
  color: #24392f;
  background: transparent;
  font: inherit;
  font-size: 14px;
}

.input-wrapper input::placeholder {
  color: #a9b5ae;
}

.icon-button {
  width: 28px;
  height: 28px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #8b9a92;
  cursor: pointer;
}

.icon-button:hover {
  background: #edf3ee;
  color: #2c634b;
}

.icon-button svg {
  width: 16px;
  height: 16px;
}

.form-note {
  margin: -4px 0 0;
  color: #829088;
  font-size: 12px;
  line-height: 1.6;
}

.error-message {
  padding: 10px 12px;
  border: 1px solid #f2caca;
  border-radius: 8px;
  background: #fff6f6;
  color: #c44d4d;
  font-size: 12px;
  line-height: 1.5;
}

.submit-button {
  min-height: 48px;
  justify-content: center;
  gap: 10px;
  border: 0;
  border-radius: 9px;
  background: #1f493a;
  color: #ffffff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.18s ease, transform 0.18s ease;
}

.submit-button:hover:not(:disabled) {
  background: #28634c;
  transform: translateY(-1px);
}

.submit-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.submit-button svg {
  width: 16px;
  height: 16px;
}

.mode-hint {
  margin: 22px 0 0;
  color: #7b8981;
  font-size: 13px;
  text-align: center;
}

.mode-hint button {
  padding: 0;
  border: 0;
  background: transparent;
  color: #2c704c;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.mode-hint button:hover {
  text-decoration: underline;
}

.auth-footer {
  justify-content: center;
  margin-top: 30px;
  padding-top: 18px;
  border-top: 1px solid #edf1ed;
  color: #a0aaa4;
  font-size: 11px;
  text-align: center;
}

@media (max-width: 820px) {
  .auth-page {
    padding: 18px;
  }

  .auth-shell {
    min-height: auto;
    grid-template-columns: 1fr;
  }

  .brand-panel {
    min-height: 240px;
    padding: 28px 30px;
  }

  .brand-copy {
    margin-top: 34px;
  }

  .brand-copy h1 {
    font-size: 30px;
  }

  .brand-description {
    display: none;
  }

  .auth-card {
    padding: 34px 30px 28px;
  }
}

@media (max-width: 520px) {
  .auth-page {
    padding: 0;
    display: block;
  }

  .auth-shell {
    min-height: 100vh;
    border: 0;
    border-radius: 0;
  }

  .brand-panel {
    min-height: 190px;
  }

  .brand-footer {
    gap: 12px;
  }

  .brand-footer span + span::before {
    margin-right: 12px;
  }

  .auth-card {
    padding: 30px 20px 24px;
  }

  .auth-heading h2 {
    font-size: 26px;
  }

  .field-row {
    grid-template-columns: 1fr;
    gap: 18px;
  }

  .role-selector {
    grid-template-columns: 1fr;
  }

  .register-role-selector {
    grid-template-columns: 1fr 1fr;
  }

  .role-selector button {
    min-height: 46px;
  }

  .role-selector small {
    display: none;
  }
}
</style>
