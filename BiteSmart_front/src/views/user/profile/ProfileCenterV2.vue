<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import { getCurrentUser, getProfile, saveProfile, updateCurrentUser, uploadFile } from '../../../api/user/profile'
import { useUserStore } from '../../../stores/user'
import { extractProfile } from '../../../utils/profileData'
import { resolveFileUrl } from '../../../utils/fileUrl'

const loading = ref(false)
const avatarUploading = ref(false)
const editing = ref(false)
const accountEditing = ref(false)
const userStore = useUserStore()
const user = ref<any>({ ...(userStore.userInfo || {}) })
const accountForm = ref({ username: '', nickname: '' })
const form = ref<any>({ age: null, gender: null, height: null, weight: null, activityLevel: 10, dietPreference: '', allergyInfo: '', diseaseHistory: '', healthGoal: '' })
const goals = ['减脂', '增肌', '维持', '控糖', '其他']
const activities = [{ label: '久坐', value: 10 }, { label: '轻度运动', value: 20 }, { label: '中度运动', value: 30 }, { label: '高强度运动', value: 40 }]
const formatJsonList = (value: any) => {
  if (!value) return ''
  if (Array.isArray(value)) return value.join('、')
  if (typeof value === 'string') {
    try {
      const parsed = JSON.parse(value)
      if (Array.isArray(parsed)) return parsed.join('、')
    } catch { /* 普通文本直接展示 */ }
  }
  return String(value)
}

const load = async () => {
  loading.value = true
  try {
    const cachedUser = userStore.userInfo || {}
    user.value = { ...cachedUser }
    const [userResponse, profileResponse] = await Promise.allSettled([getCurrentUser(), getProfile()])
    if (userResponse.status === 'fulfilled') {
      const serverUser = userResponse.value.data?.user || userResponse.value.data
      if (serverUser) user.value = {
        ...cachedUser,
        ...serverUser,
        username: serverUser.username || cachedUser.username || '',
        nickname: serverUser.nickname || cachedUser.nickname || ''
      }
    }
    if (profileResponse.status === 'fulfilled') {
      form.value = { ...form.value, ...extractProfile(profileResponse.value) }
      form.value.dietPreference = formatJsonList(form.value.dietPreference)
      form.value.allergyInfo = formatJsonList(form.value.allergyInfo)
      form.value.diseaseHistory = formatJsonList(form.value.diseaseHistory)
    }
    accountForm.value = { username: user.value.username || '', nickname: user.value.nickname || '' }
    if (userResponse.status === 'rejected' && profileResponse.status === 'rejected') throw userResponse.reason
  } catch {
    ElMessage.error('加载个人档案失败')
  } finally {
    loading.value = false
  }
}

const saveAccount = async () => {
  if (!accountForm.value.username.trim()) return ElMessage.warning('用户名不能为空')
  loading.value = true
  try {
    const response = await updateCurrentUser({ nickname: accountForm.value.nickname })
    user.value = response.data?.user || response.data || { ...user.value, ...accountForm.value }
    userStore.setUserInfo(user.value)
    accountEditing.value = false
    ElMessage.success('账户信息已更新')
  } catch {
    ElMessage.error('账户信息保存失败')
  } finally {
    loading.value = false
  }
}

const toggleAccountEditing = () => {
  if (!accountEditing.value) {
    accountForm.value = {
      username: user.value.username || userStore.userInfo?.username || '',
      nickname: user.value.nickname || userStore.userInfo?.nickname || ''
    }
  }
  accountEditing.value = !accountEditing.value
}

const save = async () => {
  loading.value = true
  try {
    await saveProfile(form.value)
    ElMessage.success('健康档案已保存')
    editing.value = false
    await load()
  } catch {
    ElMessage.error('保存失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const profileFields = computed(() => [form.value.age, form.value.gender, form.value.height, form.value.weight, form.value.healthGoal].filter(value => value !== null && value !== undefined && value !== '').length)
const completion = computed(() => Math.round(profileFields.value / 5 * 100))
const genderLabel = computed(() => form.value.gender === 10 ? '男' : form.value.gender === 20 ? '女' : '未设置')
const activityLabel = computed(() => activities.find(item => item.value === form.value.activityLevel)?.label || '未设置')
const display = (value: any, suffix = '') => value === null || value === undefined || value === '' || value === 0 ? '未设置' : `${value}${suffix}`
const avatarUrl = computed(() => resolveFileUrl(user.value.avatar))

const handleAvatarUpload = async (file: File) => {
  const allowedTypes = ['image/jpeg', 'image/png', 'image/gif']
  if (!allowedTypes.includes(file.type)) {
    ElMessage.warning('请上传 JPG、PNG 或 GIF 图片')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('头像大小不能超过 5MB')
    return false
  }

  avatarUploading.value = true
  try {
    const uploadResponse = await uploadFile(file, 'avatar')
    const avatar = uploadResponse.data?.url
    if (!avatar) throw new Error('头像上传接口未返回文件地址')

    const userResponse = await updateCurrentUser({ avatar })
    const serverUser = userResponse.data?.user || userResponse.data
    user.value = { ...user.value, ...(serverUser || {}), avatar }
    userStore.setUserInfo(user.value)
    ElMessage.success('头像已更换')
  } catch (error) {
    console.error('头像更换失败', error)
    ElMessage.error('头像更换失败')
  } finally {
    avatarUploading.value = false
  }
  return false
}

watch(() => userStore.userInfo, value => {
  if (!value) return
  user.value = { ...value, ...user.value, username: user.value.username || value.username || '', nickname: user.value.nickname || value.nickname || '' }
  accountForm.value = { username: user.value.username || '', nickname: user.value.nickname || '' }
}, { deep: true })
watch(accountEditing, value => {
  if (value) accountForm.value = {
    username: user.value.username || userStore.userInfo?.username || '',
    nickname: user.value.nickname || userStore.userInfo?.nickname || ''
  }
})

onMounted(load)
</script>

<template>
  <div class="profile-page">
    <section class="profile-hero"><div class="identity"><el-upload class="avatar-uploader" :show-file-list="false" :before-upload="handleAvatarUpload" accept="image/jpeg,image/png,image/gif" :disabled="avatarUploading"><div class="hero-avatar" :class="{ 'is-uploading': avatarUploading }"><img v-if="avatarUrl" :src="avatarUrl" alt="头像"/><span v-else>{{ (user.nickname || user.username || '我').slice(0, 1) }}</span><span class="avatar-overlay"><Upload :size="16"/>更换头像</span></div></el-upload><div><span class="eyebrow">MY BITESMART</span><h1>{{ user.nickname || user.username || '我的账户' }}</h1><p>把健康信息交给我，之后的每一餐都会更适合你。</p></div></div><div class="hero-status"><span>档案完成度</span><strong>{{ completion }}%</strong><el-progress :percentage="completion" :show-text="false" :stroke-width="5" color="#1f4d3a"/></div></section>

    <div v-loading="loading" class="profile-grid">
      <main class="main-column"><section class="section-panel account-panel"><div class="section-heading"><div><span class="section-kicker">ACCOUNT</span><h2>账户信息</h2></div><div class="heading-actions"><span class="muted">登录账户的基本信息</span><el-button size="small" @click="accountEditing = !accountEditing">{{ accountEditing ? '收起' : '编辑账户' }}</el-button></div></div><div v-if="!accountEditing"><div class="account-row"><span>用户名</span><strong>{{ user.username || '未设置' }}</strong></div><div class="account-row"><span>昵称</span><strong>{{ user.nickname || '未设置' }}</strong></div><div class="account-row"><span>账户状态</span><el-tag type="success" effect="plain">正常</el-tag></div></div><el-form v-else label-position="top" class="account-form"><div class="form-grid"><el-form-item label="用户名"><el-input v-model="accountForm.username"/></el-form-item><el-form-item label="昵称"><el-input v-model="accountForm.nickname" placeholder="请输入昵称"/></el-form-item></div><div class="form-actions"><el-button @click="accountEditing = false">取消</el-button><el-button type="primary" :loading="loading" @click="saveAccount">保存账户信息</el-button></div></el-form></section>
        <section class="section-panel health-panel"><div class="section-heading"><div><span class="section-kicker">HEALTH PROFILE</span><h2>健康档案</h2></div><el-button type="primary" @click="editing = !editing">{{ editing ? '收起编辑' : '编辑档案' }}</el-button></div><div v-if="!editing" class="health-cards"><div><span>年龄</span><strong>{{ display(form.age, ' 岁') }}</strong></div><div><span>性别</span><strong>{{ genderLabel }}</strong></div><div><span>身高</span><strong>{{ display(form.height, ' cm') }}</strong></div><div><span>体重</span><strong>{{ display(form.weight, ' kg') }}</strong></div><div><span>活动水平</span><strong>{{ activityLabel }}</strong></div><div><span>健康目标</span><strong>{{ form.healthGoal || '未设置' }}</strong></div></div><div v-else class="edit-form"><el-form label-position="top"><div class="form-grid"><el-form-item label="年龄"><el-input-number v-model="form.age" :min="1" :max="150" controls-position="right"/></el-form-item><el-form-item label="性别"><el-select v-model="form.gender"><el-option label="男" :value="10"/><el-option label="女" :value="20"/></el-select></el-form-item><el-form-item label="身高（cm）"><el-input-number v-model="form.height" :min="50" :max="250" :precision="1" controls-position="right"/></el-form-item><el-form-item label="体重（kg）"><el-input-number v-model="form.weight" :min="20" :max="300" :precision="1" controls-position="right"/></el-form-item><el-form-item label="活动水平"><el-select v-model="form.activityLevel"><el-option v-for="item in activities" :key="item.value" :label="item.label" :value="item.value"/></el-select></el-form-item><el-form-item label="健康目标"><el-select v-model="form.healthGoal"><el-option v-for="goal in goals" :key="goal" :label="goal" :value="goal"/></el-select></el-form-item></div><el-form-item label="饮食偏好"><el-input v-model="form.dietPreference" placeholder="例如：少盐、少油、素食"/></el-form-item><div class="form-grid"><el-form-item label="过敏信息"><el-input v-model="form.allergyInfo" placeholder="没有可留空"/></el-form-item><el-form-item label="疾病史"><el-input v-model="form.diseaseHistory" placeholder="没有可留空"/></el-form-item></div><div class="form-actions"><el-button @click="editing = false">取消</el-button><el-button type="primary" :loading="loading" @click="save">保存健康档案</el-button></div></el-form></div></section>
      </main>
      <aside class="side-column"><section class="focus-panel"><span class="section-kicker">YOUR FOCUS</span><h2>{{ form.healthGoal || '开始设定健康目标' }}</h2><p>{{ form.healthGoal ? '你的推荐和营养目标会围绕这个方向生成。' : '先完善档案，选择一个你想坚持的方向。' }}</p><div class="focus-line"><span>饮食偏好</span><strong>{{ form.dietPreference || '暂无' }}</strong></div><div class="focus-line"><span>过敏信息</span><strong>{{ form.allergyInfo || '暂无' }}</strong></div></section><section class="next-panel"><span class="section-kicker">NEXT STEP</span><strong>{{ completion === 100 ? '档案已完整' : '完善健康档案' }}</strong><p>{{ completion === 100 ? '现在可以去生成你的个性化食谱。' : `还差 ${5 - profileFields} 项基础信息` }}</p></section></aside>
    </div>
  </div>
</template>

<style scoped>
.profile-page{max-width:1180px;margin:0 auto;padding:48px 24px 80px;color:#253129}.profile-hero{display:flex;align-items:flex-end;justify-content:space-between;gap:30px;padding:8px 0 30px;border-bottom:1px solid #dfe7df}.identity{display:flex;align-items:center;gap:18px}.avatar{display:grid;place-items:center;width:68px;height:68px;border-radius:50%;background:#1f4d3a;color:#fff;font-family:Georgia,serif;font-size:28px}.eyebrow,.section-kicker{color:#cf704f;font-size:10px;font-weight:800;letter-spacing:.16em}.profile-hero h1{margin:9px 0 7px;font-family:"Source Han Serif SC","Songti SC",serif;font-size:32px;line-height:1.1}.profile-hero p{margin:0;color:#718078;font-size:13px}.hero-status{width:180px}.hero-status span{color:#8a968e;font-size:11px}.hero-status strong{display:block;margin:5px 0 8px;color:#1f4d3a;font-family:Georgia,serif;font-size:26px}.profile-grid{display:grid;grid-template-columns:minmax(0,1fr) 290px;gap:24px;padding-top:24px}.main-column{display:flex;flex-direction:column;gap:18px}.section-panel{padding:22px 24px;background:#fff;border:1px solid #e1e9e2}.section-heading{display:flex;align-items:flex-start;justify-content:space-between;gap:18px;margin-bottom:20px}.section-heading h2{margin:6px 0 0;font-family:"Source Han Serif SC","Songti SC",serif;font-size:22px}.muted{color:#8a968e;font-size:11px}.account-row{display:flex;align-items:center;justify-content:space-between;padding:13px 0;border-top:1px solid #edf1ec;font-size:12px}.account-row span{color:#8a968e}.account-row strong{font-weight:600}.health-cards{display:grid;grid-template-columns:repeat(3,1fr);border-top:1px solid #edf1ec}.health-cards>div{min-height:86px;padding:16px 14px;border-right:1px solid #edf1ec;border-bottom:1px solid #edf1ec}.health-cards>div:nth-child(3n){border-right:0}.health-cards span{display:block;margin-bottom:10px;color:#8a968e;font-size:11px}.health-cards strong{color:#253129;font-size:15px}.edit-form{padding-top:2px}.form-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:0 18px}.edit-form :deep(.el-form-item){margin-bottom:16px}.edit-form :deep(.el-input-number),.edit-form :deep(.el-select){width:100%}.form-actions{display:flex;justify-content:flex-end;gap:10px;margin-top:4px;padding-top:16px;border-top:1px solid #edf1ec}.form-actions .el-button--primary,.section-heading .el-button--primary{background:#1f4d3a;border-color:#1f4d3a}.preference-strip{display:flex;align-items:center;justify-content:space-between;gap:18px;padding:17px 20px;background:#f4f8f3;border:1px solid #d6e4d8}.preference-strip>div{display:flex;align-items:center;gap:12px}.strip-icon{display:grid;place-items:center;width:28px;height:28px;color:#cf704f;font-size:18px}.preference-strip strong{font-size:13px}.preference-strip p{margin:5px 0 0;color:#718078;font-size:11px;line-height:1.5}.preference-strip .el-button{color:#1f4d3a}.side-column{display:flex;flex-direction:column;gap:18px}.focus-panel{padding:23px 22px;background:#1f4d3a;color:#fff}.focus-panel h2{margin:16px 0 8px;font-family:"Source Han Serif SC","Songti SC",serif;font-size:26px}.focus-panel p{margin:0 0 24px;color:#d7e6d9;font-size:12px;line-height:1.7}.focus-panel .section-kicker{color:#e6b08e}.focus-line{display:flex;justify-content:space-between;gap:12px;padding:12px 0;border-top:1px solid rgba(255,255,255,.18);font-size:11px}.focus-line span{color:#c6d8c8}.focus-line strong{max-width:130px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-weight:500}.next-panel{padding:19px 22px;background:#fff;border:1px solid #e1e9e2}.next-panel .section-kicker{display:block;margin-bottom:12px}.next-panel strong{display:block;font-size:14px}.next-panel p{margin:8px 0 0;color:#718078;font-size:11px}.profile-page :deep(.el-button--primary:hover){background:#2c654e;border-color:#2c654e}@media(max-width:850px){.profile-grid{grid-template-columns:1fr}.side-column{display:grid;grid-template-columns:1fr 1fr}.hero-status{width:150px}}@media(max-width:600px){.profile-page{padding:30px 16px 58px}.profile-hero{align-items:flex-start;flex-direction:column;gap:20px}.profile-hero h1{font-size:28px}.hero-status{width:100%}.profile-grid{padding-top:18px}.section-panel{padding:18px}.health-cards{grid-template-columns:repeat(2,1fr)}.health-cards>div:nth-child(3n){border-right:1px solid #edf1ec}.health-cards>div:nth-child(2n){border-right:0}.form-grid{grid-template-columns:1fr}.side-column{display:flex}.preference-strip{align-items:flex-start;flex-direction:column}.preference-strip .el-button{padding-left:0}}
</style>
<style scoped>
.heading-actions{display:flex;align-items:center;gap:14px}.heading-actions .el-button{color:#1f4d3a;border-color:#c9dbce;background:#f5f8f4}.heading-actions .el-button:hover{color:#fff;border-color:#1f4d3a;background:#1f4d3a}.account-form{padding-top:2px}.account-form :deep(.el-form-item){margin-bottom:16px}
.account-form :deep(.form-grid > .el-form-item:first-child .el-input){pointer-events:none;opacity:.72}
@media(max-width:600px){.heading-actions{align-items:flex-end;flex-direction:column;gap:8px}.heading-actions .muted{display:none}}
.account-panel{max-width:820px}
@media(max-width:850px){.account-panel{max-width:none}}
.avatar-uploader{display:block;width:68px;height:68px;flex:0 0 68px;cursor:pointer}
.avatar-uploader :deep(.el-upload){display:block;width:68px;height:68px;border:0}
.hero-avatar{position:relative;display:grid;place-items:center;width:68px;height:68px;overflow:hidden;border-radius:50%;background:#1f4d3a;color:#fff;font-family:Georgia,serif;font-size:28px}
.hero-avatar img{display:block;width:100%;height:100%;object-fit:cover}
.avatar-overlay{position:absolute;inset:auto 0 0;display:flex;align-items:center;justify-content:center;gap:4px;padding:5px 2px;background:rgba(20,48,37,.82);color:#fff;font-family:inherit;font-size:10px;line-height:1;opacity:0;transition:opacity .2s ease}
.hero-avatar:hover .avatar-overlay{opacity:1}
.hero-avatar.is-uploading{cursor:wait;opacity:.62}
</style>
