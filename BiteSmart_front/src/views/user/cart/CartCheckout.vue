<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getCartList, deleteCartItem, updateCartQuantity, updateCartSelected } from '../../../api/user/cart'
import { getAddressList } from '../../../api/user/addresses'
import { createBatchOrder } from '../../../api/user/orders'
import { getDishList } from '../../../api/user/dishes'
import { getComboDetail } from '../../../api/user/combos'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { ArrowRight } from '@element-plus/icons-vue'
import { resolveFileUrl } from '../../../utils/fileUrl'
import { merchantLabel } from '../../../utils/display'

const router = useRouter()
const loading = ref(false)
const cartItems = ref<any[]>([])
const addresses = ref<any[]>([])
const checkoutVisible = ref(false)
const selectedAddressId = ref<number | string | undefined>()
const merchantRemarks = ref<Record<string, string>>({})
const checkoutStateKey = 'bitesmart-cart-checkout-state'
const selectedItems = computed(() => cartItems.value.filter(i => i.selected !== 0))
const selectedQuantity = computed(() => selectedItems.value.reduce((sum, item) => sum + Number(item.quantity || 0), 0))
const totalAmount = computed(() => selectedItems.value.reduce((sum, i) => sum + Number(i.price || i.itemPrice || 0) * i.quantity, 0))
const merchantGroups = computed(() => {
  // merchantId 是雪花 ID（字符串），不可 Number() 强转，否则丢精度后传给后端会下错商家
  const groups = new Map<string, { merchantId: string, shopName: string, items: any[], total: number }>()
  selectedItems.value.forEach(item => {
    const merchantId = String(item.merchantId || '')
    if (!merchantId) return
    const group = groups.get(merchantId) || { merchantId, shopName: '', items: [], total: 0 }
    group.items.push(item)
    if (!group.shopName && item.shopName) group.shopName = item.shopName
    group.total += Number(item.price || item.itemPrice || 0) * Number(item.quantity || 0)
    groups.set(merchantId, group)
  })
  return [...groups.values()]
})
const allSelected = computed(() => cartItems.value.length > 0 && cartItems.value.every(i => i.selected !== 0))
const someSelected = computed(() => selectedItems.value.length > 0 && !allSelected.value)
const itemName = (item: any) => item.dishName || item.comboName || item.name || '商品'
// 无图商品用首字徽章占位，不再回退到无关素材图
const itemImage = (item: any) => resolveFileUrl(item.dishImage || item.comboImage) || ''
const comboDishes = (item: any) => {
  if (item.itemType !== 20) return []
  try {
    const snapshot = typeof item.customizationJson === 'string' ? JSON.parse(item.customizationJson) : item.customizationJson
    return snapshot?.items || item.comboDishes || []
  } catch {
    return []
  }
}
const errorMessage = (error: any, fallback: string) => error?.message || error?.response?.data?.message || fallback
const fetchData = async () => {
  loading.value = true
  try {
    const [cartRes, addressRes, dishRes] = await Promise.all([getCartList(), getAddressList(), getDishList({ page: 1, size: 100 })])
    const dishes = dishRes.data?.data?.list || []
    // comboId 是雪花 ID（字符串），保持字符串透传，Number() 会丢精度导致详情 404
    const comboIds: string[] = [...new Set<string>((cartRes.data || []).filter((item: any) => item.itemType === 20 && item.comboId).map((item: any) => String(item.comboId)))]
    const comboDetails = await Promise.all(comboIds.map(async (id) => {
      try { return await getComboDetail(id) } catch { return null }
    }))
    const comboMap = new Map(comboDetails.filter(Boolean).map((res: any) => [res.data?.combo?.id, res.data]))
    cartItems.value = (cartRes.data || []).map((item: any) => {
      const dish = item.dishId ? dishes.find((entry: any) => String(entry.id) === String(item.dishId)) : undefined
      const combo = item.comboId ? comboMap.get(item.comboId) : undefined
      const comboDishes = combo?.dishRels?.map((rel: any) => ({ ...rel, dishName: dishes.find((dish: any) => dish.id === rel.dishId)?.dishName }))
      return dish
        ? { ...item, merchantId: item.merchantId ?? dish.merchantId, dishName: item.dishName || dish.dishName, dishImage: item.dishImage || dish.dishImage, price: item.price ?? dish.price }
        : { ...item, merchantId: item.merchantId ?? combo?.combo?.merchantId, comboDishes: comboDishes || [] }
    })
    addresses.value = addressRes.data || []
    const saved = sessionStorage.getItem(checkoutStateKey)
    if (saved) {
      const state = JSON.parse(saved)
      selectedAddressId.value = state.selectedAddressId
      merchantRemarks.value = state.merchantRemarks || {}
      if (state.open && selectedItems.value.length) checkoutVisible.value = true
      sessionStorage.removeItem(checkoutStateKey)
    }
  } catch { ElMessage.error('加载购物车失败') } finally { loading.value = false }
}
const changeSelected = async (item: any, value: boolean) => { try { await updateCartSelected(item.id, value ? 1 : 0); item.selected = value ? 1 : 0 } catch { ElMessage.error('更新选择状态失败') } }
const toggleAll = async (value: boolean) => {
  try {
    await Promise.all(cartItems.value.map(item => updateCartSelected(item.id, value ? 1 : 0)))
    cartItems.value.forEach(item => { item.selected = value ? 1 : 0 })
  } catch { ElMessage.error('批量更新选择状态失败'); await fetchData() }
}
const changeQuantity = async (item: any, value: number | undefined) => { if (!value) return; try { await updateCartQuantity(item.id, value); item.quantity = value } catch (error) { ElMessage.error(errorMessage(error, '修改数量失败')) } }
const removeItem = async (item: any) => { try { await ElMessageBox.confirm('确定删除该商品吗？', '提示'); await deleteCartItem(item.id); cartItems.value = cartItems.value.filter(i => i.id !== item.id); ElMessage.success('已删除') } catch {} }
const openCheckout = () => { if (!cartItems.value.some(i => i.selected !== 0)) return ElMessage.warning('请先选择要结算的商品'); selectedAddressId.value = addresses.value.find(a => a.isDefault === 1)?.id || addresses.value[0]?.id; checkoutVisible.value = true }
const goToAddresses = () => {
  sessionStorage.setItem(checkoutStateKey, JSON.stringify({ open: true, selectedAddressId: selectedAddressId.value, merchantRemarks: merchantRemarks.value }))
  router.push({ path: '/user/addresses', query: { from: 'cart' } })
}
const submitOrder = async () => {
  const address = addresses.value.find(a => a.id === selectedAddressId.value)
  if (!address) return ElMessage.warning('请选择收货地址')
  const addressText = `${address.province}${address.city}${address.district}${address.detailAddress}`
  try {
    const res = await createBatchOrder({
      address: addressText,
      receiverName: address.receiverName,
      receiverPhone: address.receiverPhone,
      merchantOrders: merchantGroups.value.map(group => ({ merchantId: group.merchantId, remark: merchantRemarks.value[String(group.merchantId)] || '' }))
    })
    if (res.code !== 200) throw new Error(res.message || '创建订单失败')
    ElMessage.success('订单创建成功'); checkoutVisible.value = false; await fetchData(); router.push('/user/orders')
  } catch (error) { ElMessage.error(errorMessage(error, '创建订单失败')) }
}
onMounted(fetchData)
</script>

<template>
  <div class="cart-page">
    <header class="cart-intro">
      <span>你的选择</span>
      <h1>购物车</h1>
      <p>{{ cartItems.length ? `${selectedQuantity} 件商品，准备好下一顿了吗？` : '把喜欢的餐食留在这里。' }}</p>
    </header>
    <div v-loading="loading" class="cart-layout">
      <section class="cart-list">
        <div v-if="cartItems.length" class="cart-toolbar">
          <el-checkbox :model-value="allSelected" :indeterminate="someSelected" @change="() => toggleAll(!allSelected)">全选</el-checkbox>
          <span>已选 {{ selectedQuantity }} 件</span>
        </div>
        <div v-for="item in cartItems" :key="item.id" class="cart-item">
          <el-checkbox :model-value="item.selected !== 0" @change="(v: any) => changeSelected(item, Boolean(v))" />
          <img v-if="itemImage(item)" :src="itemImage(item)" :alt="itemName(item)" /><span v-else class="img-placeholder">{{ itemName(item)[0] }}</span>
          <div class="cart-item-info">
            <span>{{ item.itemType === 20 ? '健康套餐' : '精选菜品' }}</span>
            <h2>{{ itemName(item) }}</h2>
            <p>¥{{ Number(item.price || item.itemPrice || 0).toFixed(2) }} / 份</p>
            <ul v-if="comboDishes(item).length" class="combo-dishes">
              <li v-for="dish in comboDishes(item)" :key="`${item.id}-${dish.dishId}`">{{ dish.dishName || `菜品 #${dish.dishId}` }} × {{ dish.quantity || 1 }}</li>
            </ul>
          </div>
          <el-input-number :model-value="item.quantity" :min="1" :max="99" size="small" @change="(v: number | undefined) => changeQuantity(item, v)" />
          <strong>¥{{ (Number(item.price || item.itemPrice || 0) * item.quantity).toFixed(2) }}</strong>
          <button class="remove-item" title="删除商品" @click="removeItem(item)">×</button>
        </div>
        <div v-if="!loading && !cartItems.length" class="cart-empty">
          <img src="/images/home/salad-card.jpg" alt="健康餐食" />
          <div><h2>购物车还是空的</h2><p>去挑一份适合今天的餐食。</p><button @click="router.push('/user/dishes')">浏览菜品 <span>→</span></button></div>
        </div>
      </section>
      <aside class="cart-summary">
        <span>ORDER SUMMARY</span><h2>本次合计</h2>
        <div class="summary-row"><span>已选商品</span><strong>{{ selectedQuantity }} 件</strong></div>
        <div class="summary-total"><span>应付金额</span><strong>¥{{ totalAmount.toFixed(2) }}</strong></div>
        <button class="checkout-button" :disabled="!cartItems.length" @click="openCheckout">去结算 <ArrowRight /></button>
      </aside>
    </div>
    <el-dialog v-model="checkoutVisible" title="确认订单" width="560px">
      <el-form label-width="84px">
        <el-form-item label="收货地址">
          <el-radio-group v-model="selectedAddressId">
            <el-radio v-for="address in addresses" :key="address.id" :value="address.id" class="address-option">{{ address.receiverName }} {{ address.receiverPhone }} {{ address.province }}{{ address.city }}{{ address.district }}{{ address.detailAddress }}</el-radio>
          </el-radio-group>
          <el-button link type="primary" @click="goToAddresses">管理地址</el-button>
          <el-empty v-if="!addresses.length" description="暂无收货地址，请先添加" />
        </el-form-item>
        <div class="merchant-checkout-list">
          <section v-for="group in merchantGroups" :key="group.merchantId" class="merchant-checkout-group">
            <div class="merchant-checkout-header"><strong>{{ merchantLabel(group.shopName, group.merchantId) }}</strong><span>¥{{ group.total.toFixed(2) }}</span></div>
            <ul><li v-for="item in group.items" :key="item.id">{{ itemName(item) }} × {{ item.quantity }}</li></ul>
            <el-input v-model="merchantRemarks[String(group.merchantId)]" type="textarea" :rows="2" maxlength="200" show-word-limit placeholder="给该商家留言" />
          </section>
        </div>
        <el-form-item label="应付金额"><strong class="amount">¥{{ totalAmount.toFixed(2) }}</strong></el-form-item>
      </el-form>
      <template #footer><el-button @click="checkoutVisible = false">返回</el-button><el-button type="primary" @click="submitOrder">提交订单</el-button></template>
    </el-dialog>
  </div>
</template>
<style scoped>
.cart-toolbar{display:flex;align-items:center;justify-content:space-between;padding:0 0 12px;color:#718078;font-size:12px}.combo-dishes{margin:9px 0 0;padding:8px 0 0 16px;border-top:1px solid #edf0ed;color:#718078;font-size:11px;line-height:1.7}.combo-dishes li{margin:0}.merchant-checkout-list{display:grid;gap:16px;width:100%}.merchant-checkout-group{padding:14px 16px;border:1px solid #e1e9e2;background:#f8faf8}.merchant-checkout-header{display:flex;justify-content:space-between;color:var(--green)}.merchant-checkout-group ul{margin:8px 0 12px;padding-left:18px;color:#718078;font-size:12px;line-height:1.7}
.cart-page{max-width:1192px;margin:0 auto;padding:66px 24px 90px;color:#1f2a24}.cart-intro{margin-bottom:38px}.cart-intro span,.cart-summary>span{display:block;color:var(--orange);font-size:11px;letter-spacing:.14em;font-weight:650}.cart-intro h1{margin:12px 0 8px;font-family:"Source Han Serif SC","Songti SC",serif;font-size:38px;font-weight:600}.cart-intro p{margin:0;color:#6d7971;font-size:14px}.cart-layout{display:grid;grid-template-columns:1fr 286px;gap:44px;align-items:start}.cart-list{min-height:230px}.cart-item{display:grid;grid-template-columns:26px 94px 1fr 112px 90px 28px;align-items:center;gap:17px;padding:18px 0;border-top:1px solid #dfe7df}.cart-item:last-child{border-bottom:1px solid #dfe7df}.cart-item img{width:94px;height:78px;object-fit:cover;background:#eef2ed}.cart-item-info>span{color:var(--orange);font-size:10px;letter-spacing:.1em}.cart-item-info h2{margin:7px 0 5px;font-size:17px;font-weight:650}.cart-item-info p{margin:0;color:#78857d;font-size:12px}.cart-item>strong{color:var(--green);font-size:16px}.remove-item{border:0;background:none;color:var(--danger-brand);font-size:22px;cursor:pointer}.cart-summary{position:sticky;top:100px;padding:26px 24px;background:var(--green-ink);color:#fff}.cart-summary h2{margin:12px 0 28px;font-family:"Source Han Serif SC","Songti SC",serif;font-size:25px;font-weight:600}.summary-row,.summary-total{display:flex;justify-content:space-between;align-items:center;padding:14px 0;border-top:1px solid rgba(255,255,255,.22);font-size:12px;color:#d3e0d8}.summary-total{margin-top:24px;padding-top:20px}.summary-total strong{font-size:23px;color:var(--orange)}.checkout-button{display:flex;align-items:center;justify-content:space-between;width:100%;margin-top:24px;padding:13px 0;border:0;border-top:1px solid rgba(255,255,255,.4);background:none;color:#fff;font-weight:650;cursor:pointer}.checkout-button span{color:var(--orange);font-size:20px}.checkout-button:disabled{opacity:.4;cursor:not-allowed}.cart-empty{display:flex;align-items:center;justify-content:center;gap:24px;min-height:260px;background:#eff3ee}.cart-empty img{width:135px;height:135px;object-fit:cover}.cart-empty h2{margin:0 0 8px;font-family:"Source Han Serif SC","Songti SC",serif}.cart-empty p{margin:0;color:#718078;font-size:13px}.cart-empty button{margin-top:17px;border:0;background:none;color:var(--green);font-weight:650;cursor:pointer}.cart-empty button span{color:var(--orange);margin-left:6px}.address-option{display:flex;width:100%;height:auto;line-height:1.6;margin:0 0 10px;white-space:normal}.amount{font-size:20px;color:var(--green)}@media(max-width:850px){.cart-layout{grid-template-columns:1fr}.cart-summary{position:static;display:grid;grid-template-columns:1fr 1fr;gap:0 20px}.cart-summary h2,.cart-summary>span{grid-column:1/-1}.summary-total,.checkout-button{grid-column:2}.summary-row{grid-column:1;grid-row:3}.checkout-button{margin-top:0;align-self:center}}@media(max-width:600px){.cart-page{padding:45px 18px 68px}.cart-intro h1{font-size:32px}.cart-item{grid-template-columns:22px 72px 1fr 24px;gap:10px}.cart-item img{width:72px;height:66px}.cart-item :deep(.el-input-number){grid-column:3}.cart-item>strong{grid-column:3}.cart-item-info h2{font-size:15px}.remove-item{grid-column:4;grid-row:1}.cart-summary{display:block}.summary-row,.summary-total{margin-top:14px}.checkout-button{margin-top:20px}.cart-empty{padding:24px;justify-content:flex-start}}
.checkout-button{justify-content:center;gap:12px;padding:13px 16px;border:0;background:var(--orange);color:#fff;font-size:14px;font-weight:700;transition:background .2s ease,transform .2s ease}.checkout-button:hover:not(:disabled){filter:brightness(1.08);transform:translateY(-1px)}.checkout-button span{display:none}.checkout-button svg{width:17px;height:17px;color:#fff}.checkout-button:disabled{background:rgba(217,123,43,.35);color:rgba(255,255,255,.55)}
</style>
<style scoped>
.cart-item .img-placeholder{display:flex;align-items:center;justify-content:center;width:94px;height:78px;background:var(--green-soft);color:var(--green-deep);font-family:"Source Han Serif SC","Songti SC",serif;font-size:26px;font-weight:600}
</style>
