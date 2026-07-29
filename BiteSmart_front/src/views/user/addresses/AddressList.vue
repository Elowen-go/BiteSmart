<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getAddressList, addAddress, updateAddress, deleteAddress } from '../../../api/user/addresses'
import type { UserAddress } from '../../../api/user/addresses'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { AMAP_JS_KEY, AMAP_SCRIPT_URL } from '../../../config/amap'

const loading = ref(false)
const addressList = ref<UserAddress[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const locating = ref(false)
let amapLoading: Promise<any> | null = null

interface AddressForm {
  id: number
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detailAddress: string
  latitude?: number
  longitude?: number
  isDefault: number
}

const form = ref<AddressForm>({
  id: 0,
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  latitude: undefined as number | undefined,
  longitude: undefined as number | undefined,
  isDefault: 0
})
const route = useRoute()
const router = useRouter()
const fromCart = computed(() => route.query.from === 'cart')

const rules = {
  receiverName: [{ required: true, message: '请输入收件人姓名', trigger: 'blur' }],
  receiverPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  district: [{ required: true, message: '请输入区县', trigger: 'blur' }],
  detailAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

const fetchAddresses = async () => {
  loading.value = true
  try {
    const res = await getAddressList()
    addressList.value = res.data || []
  } catch (e) {
    console.error('获取地址列表失败', e)
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  form.value = { id: 0, receiverName: '', receiverPhone: '', province: '', city: '', district: '', detailAddress: '', latitude: undefined, longitude: undefined, isDefault: 0 }
  dialogVisible.value = true
}

const handleEdit = (row: UserAddress) => {
  isEdit.value = true
  form.value = { ...row }
  dialogVisible.value = true
}

const handleDelete = async (row: UserAddress) => {
  try {
    await ElMessageBox.confirm('确定要删除该地址吗？', '提示')
    await deleteAddress(row.id)
    ElMessage.success('删除成功')
    fetchAddresses()
  } catch (e) {
    // 取消不做处理
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (isEdit.value) {
      await updateAddress(form.value.id, form.value as any)
      ElMessage.success('更新成功')
    } else {
      await addAddress(form.value as any)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    fetchAddresses()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const loadAmap = (): Promise<any> => {
  const current = (window as any).AMap
  if (current) return Promise.resolve(current)
  if (!AMAP_JS_KEY) return Promise.reject(new Error('未配置高德地图 Key'))
  if (!amapLoading) {
    amapLoading = new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = `${AMAP_SCRIPT_URL}&plugin=AMap.Geocoder`
      script.onload = () => resolve((window as any).AMap)
      script.onerror = () => { amapLoading = null; reject(new Error('地图服务加载失败')) }
      document.head.appendChild(script)
    })
  }
  return amapLoading
}

const locateAddress = async () => {
  const text = [form.value.province, form.value.city, form.value.district, form.value.detailAddress]
    .filter(Boolean).join('')
  if (!text) return ElMessage.warning('请先填写完整地址')
  locating.value = true
  try {
    const AMap = await loadAmap()
    const result = await new Promise<any>((resolve, reject) => {
      const run = () => {
        const geocoder = new AMap.Geocoder({ city: form.value.city || undefined })
        geocoder.getLocation(text, (status: string, response: any) => {
          if (status === 'complete' && response?.geocodes?.length) resolve(response.geocodes[0])
          else reject(new Error('地址未解析到地图位置'))
        })
      }
      if (AMap.Geocoder) run()
      else AMap.plugin('AMap.Geocoder', run)
    })
    const location = result.location
    form.value.latitude = Number(location.lat)
    form.value.longitude = Number(location.lng)
    ElMessage.success('地图位置已获取')
  } catch (error: any) {
    ElMessage.error(error?.message || '地址解析失败，请检查地址')
  } finally {
    locating.value = false
  }
}

const setDefault = (row: UserAddress) => {
  form.value = { ...row, isDefault: 1 }
  handleSubmit()
}

onMounted(() => {
  fetchAddresses()
})

const backToCart = () => router.push('/user/cart')
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>收货地址</h3>
        <div class="header-actions">
          <el-button v-if="fromCart" @click="backToCart">返回购物车</el-button>
          <el-button type="primary" @click="handleAdd">新增地址</el-button>
        </div>
      </div>
      <div v-loading="loading" style="padding-top: 20px;">
        <div v-for="item in addressList" :key="item.id" class="address-card">
          <div class="address-info">
            <div class="address-top">
              <span class="receiver-name">{{ item.receiverName }}</span>
              <span class="receiver-phone">{{ item.receiverPhone }}</span>
              <el-tag v-if="item.isDefault === 1" type="success" size="small">默认</el-tag>
            </div>
            <div class="address-detail">
              {{ item.province }}{{ item.city }}{{ item.district }}{{ item.detailAddress }}
            </div>
          </div>
          <div class="address-actions">
            <el-button size="small" @click="handleEdit(item)">编辑</el-button>
            <el-button
              size="small"
              type="danger"
              @click="handleDelete(item)"
            >删除</el-button>
          </div>
        </div>
        <div v-if="!loading && addressList.length === 0" class="empty-state">
          <el-empty description="暂无收货地址，点击上方按钮添加" />
        </div>
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑地址' : '新增地址'"
      width="600px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        label-position="right"
      >
        <el-form-item label="收件人" prop="receiverName">
          <el-input v-model="form.receiverName" placeholder="请输入收件人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="receiverPhone">
          <el-input v-model="form.receiverPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="省份" prop="province">
              <el-input v-model="form.province" placeholder="省份" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="城市" prop="city">
              <el-input v-model="form.city" placeholder="城市" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="区县" prop="district">
              <el-input v-model="form.district" placeholder="区县" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="详细地址" prop="detailAddress">
          <el-input
            v-model="form.detailAddress"
            type="textarea"
            :rows="2"
            placeholder="请输入详细地址"
          />
        </el-form-item>
        <el-form-item label="地图位置">
          <div class="location-row">
            <el-button type="primary" link :loading="locating" @click="locateAddress">解析地图位置</el-button>
            <span v-if="form.latitude && form.longitude" class="location-ok">已设置，可用于骑手导航</span>
            <span v-else class="location-missing">未设置坐标</span>
          </div>
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch
            v-model="form.isDefault"
            :active-value="1"
            :inactive-value="0"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
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

.header-actions {
  display: flex;
  gap: 10px;
}

.location-row {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 32px;
}

.location-ok,
.location-missing {
  font-size: 12px;
}

.location-ok { color: var(--bs-green, #1e9e62); }
.location-missing { color: var(--bs-text-muted, #8b958f); }

.address-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border: 1px solid var(--bs-border-color);
  border-radius: var(--bs-radius-md);
  margin-bottom: 12px;
  transition: all 0.2s;
}

.address-card:hover {
  border-color: var(--green);
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.address-info {
  flex: 1;
}

.address-top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.receiver-name {
  font-size: var(--bs-font-size-base);
  font-weight: 600;
  color: var(--bs-text-title);
}

.receiver-phone {
  font-size: var(--bs-font-size-sm);
  color: var(--bs-text-muted);
}

.address-detail {
  font-size: var(--bs-font-size-sm);
  color: var(--bs-text-secondary);
  line-height: 1.5;
}

.address-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
  margin-left: 16px;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
}

@media (max-width: 768px) {
  .address-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .address-actions {
    margin-left: 0;
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
