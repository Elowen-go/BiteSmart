<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getCartList, deleteCartItem, updateCartQuantity, updateCartSelected } from '../../../api/user/cart'
import { getAddressList } from '../../../api/user/addresses'
import { createOrder } from '../../../api/user/orders'
import { getDishList } from '../../../api/user/dishes'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { ArrowRight } from '@element-plus/icons-vue'
import { resolveFileUrl } from '../../../utils/fileUrl'

const router = useRouter()
const loading = ref(false)
const cartItems = ref<any[]>([])
const addresses = ref<any[]>([])
const checkoutVisible = ref(false)
const selectedAddressId = ref<number | undefined>()
const remark = ref('')
const totalAmount = computed(() => cartItems.value.filter(i => i.selected !== 0).reduce((sum, i) => sum + Number(i.price || i.itemPrice || 0) * i.quantity, 0))
const itemName = (item: any) => item.dishName || item.comboName || item.name || '商品'
const itemImage = (item: any) => resolveFileUrl(item.dishImage || item.comboImage) || '/images/home/meal-card.jpg'
const fetchData = async () => {
  loading.value = true
  try {
    const [cartRes, addressRes, dishRes] = await Promise.all([getCartList(), getAddressList(), getDishList({ page: 1, size: 100 })])
    const dishes = dishRes.data?.data?.list || []
    cartItems.value = (cartRes.data || []).map((item: any) => {
      const dish = item.dishId ? dishes.find((entry: any) => String(entry.id) === String(item.dishId)) : undefined
      return dish ? { ...item, dishName: item.dishName || dish.dishName, dishImage: item.dishImage || dish.dishImage, price: item.price ?? dish.price } : item
    })
    addresses.value = addressRes.data || []
  } catch { ElMessage.error('加载购物车失败') } finally { loading.value = false }
}
const changeSelected = async (item: any, value: boolean) => { try { await updateCartSelected(item.id, value ? 1 : 0); item.selected = value ? 1 : 0 } catch { ElMessage.error('更新选择状态失败') } }
const changeQuantity = async (item: any, value: number | undefined) => { if (!value) return; try { await updateCartQuantity(item.id, value); item.quantity = value } catch { ElMessage.error('修改数量失败') } }
const removeItem = async (item: any) => { try { await ElMessageBox.confirm('确定删除该商品吗？', '提示'); await deleteCartItem(item.id); cartItems.value = cartItems.value.filter(i => i.id !== item.id); ElMessage.success('已删除') } catch {} }
const openCheckout = () => { if (!cartItems.value.some(i => i.selected !== 0)) return ElMessage.warning('请先选择要结算的商品'); selectedAddressId.value = addresses.value.find(a => a.isDefault === 1)?.id || addresses.value[0]?.id; checkoutVisible.value = true }
const submitOrder = async () => { const address = addresses.value.find(a => a.id === selectedAddressId.value); if (!address) return ElMessage.warning('请选择收货地址'); const addressText = `${address.province}${address.city}${address.district}${address.detailAddress}`; try { await createOrder(addressText, address.receiverName, address.receiverPhone, remark.value); ElMessage.success('订单创建成功'); checkoutVisible.value = false; await fetchData(); router.push('/user/orders') } catch { ElMessage.error('创建订单失败') } }
onMounted(fetchData)
</script>

<template>
  <div class="cart-page"><header class="cart-intro"><span>你的选择</span><h1>购物车</h1><p>{{ cartItems.length ? `${cartItems.length} 件商品，准备好下一顿了吗？` : '把喜欢的餐食留在这里。' }}</p></header><div v-loading="loading" class="cart-layout"><section class="cart-list"><div v-for="item in cartItems" :key="item.id" class="cart-item"><el-checkbox :model-value="item.selected !== 0" @change="(v: any) => changeSelected(item, Boolean(v))" /><img :src="itemImage(item)" :alt="itemName(item)" /><div class="cart-item-info"><span>{{ item.itemType === 20 ? '健康套餐' : '精选菜品' }}</span><h2>{{ itemName(item) }}</h2><p>¥{{ Number(item.price || item.itemPrice || 0).toFixed(2) }} / 份</p></div><el-input-number :model-value="item.quantity" :min="1" :max="99" size="small" @change="(v: number | undefined) => changeQuantity(item, v)" /><strong>¥{{ (Number(item.price || item.itemPrice || 0) * item.quantity).toFixed(2) }}</strong><button class="remove-item" title="删除商品" @click="removeItem(item)">×</button></div><div v-if="!loading && !cartItems.length" class="cart-empty"><img src="/images/home/salad-card.jpg" alt="健康餐食" /><div><h2>购物车还是空的</h2><p>去挑一份适合今天的餐食。</p><button @click="router.push('/user/dishes')">浏览菜品 <span>→</span></button></div></div></section><aside class="cart-summary"><span>ORDER SUMMARY</span><h2>本次合计</h2><div class="summary-row"><span>已选商品</span><strong>{{ cartItems.filter(i => i.selected !== 0).length }} 件</strong></div><div class="summary-total"><span>应付金额</span><strong>¥{{ totalAmount.toFixed(2) }}</strong></div><button class="checkout-button" :disabled="!cartItems.length" @click="openCheckout">去结算 <ArrowRight /></button></aside></div>
  <el-dialog v-model="checkoutVisible" title="确认订单" width="560px"><el-form label-width="84px"><el-form-item label="收货地址"><el-radio-group v-model="selectedAddressId"><el-radio v-for="address in addresses" :key="address.id" :value="address.id" class="address-option">{{ address.receiverName }} {{ address.receiverPhone }} {{ address.province }}{{ address.city }}{{ address.district }}{{ address.detailAddress }}</el-radio></el-radio-group><el-button link type="primary" @click="router.push('/user/addresses')">管理地址</el-button></el-form-item><el-form-item label="备注"><el-input v-model="remark" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="给商家留言" /></el-form-item><el-form-item label="应付金额"><strong class="amount">¥{{ totalAmount.toFixed(2) }}</strong></el-form-item></el-form><template #footer><el-button @click="checkoutVisible = false">返回</el-button><el-button type="primary" @click="submitOrder">提交订单</el-button></template></el-dialog></div>
</template>
<style scoped>
.cart-page{max-width:1192px;margin:0 auto;padding:66px 24px 90px;color:#1f2a24}.cart-intro{margin-bottom:38px}.cart-intro span,.cart-summary>span{display:block;color:#cf704f;font-size:11px;letter-spacing:.14em;font-weight:650}.cart-intro h1{margin:12px 0 8px;font-family:"Source Han Serif SC","Songti SC",serif;font-size:38px;font-weight:600}.cart-intro p{margin:0;color:#6d7971;font-size:14px}.cart-layout{display:grid;grid-template-columns:1fr 286px;gap:44px;align-items:start}.cart-list{min-height:230px}.cart-item{display:grid;grid-template-columns:26px 94px 1fr 112px 90px 28px;align-items:center;gap:17px;padding:18px 0;border-top:1px solid #dfe7df}.cart-item:last-child{border-bottom:1px solid #dfe7df}.cart-item img{width:94px;height:78px;object-fit:cover;background:#eef2ed}.cart-item-info>span{color:#cf704f;font-size:10px;letter-spacing:.1em}.cart-item-info h2{margin:7px 0 5px;font-size:17px;font-weight:650}.cart-item-info p{margin:0;color:#78857d;font-size:12px}.cart-item>strong{color:#1f4d3a;font-size:16px}.remove-item{border:0;background:none;color:#9a6960;font-size:22px;cursor:pointer}.cart-summary{position:sticky;top:100px;padding:26px 24px;background:#1f4d3a;color:#fff}.cart-summary h2{margin:12px 0 28px;font-family:"Source Han Serif SC","Songti SC",serif;font-size:25px;font-weight:600}.summary-row,.summary-total{display:flex;justify-content:space-between;align-items:center;padding:14px 0;border-top:1px solid rgba(255,255,255,.22);font-size:12px;color:#d3e0d8}.summary-total{margin-top:24px;padding-top:20px}.summary-total strong{font-size:23px;color:#f5c06d}.checkout-button{display:flex;align-items:center;justify-content:space-between;width:100%;margin-top:24px;padding:13px 0;border:0;border-top:1px solid rgba(255,255,255,.4);background:none;color:#fff;font-weight:650;cursor:pointer}.checkout-button span{color:#f5c06d;font-size:20px}.checkout-button:disabled{opacity:.4;cursor:not-allowed}.cart-empty{display:flex;align-items:center;justify-content:center;gap:24px;min-height:260px;background:#eff3ee}.cart-empty img{width:135px;height:135px;object-fit:cover}.cart-empty h2{margin:0 0 8px;font-family:"Source Han Serif SC","Songti SC",serif}.cart-empty p{margin:0;color:#718078;font-size:13px}.cart-empty button{margin-top:17px;border:0;background:none;color:#1f4d3a;font-weight:650;cursor:pointer}.cart-empty button span{color:#cf704f;margin-left:6px}.address-option{display:flex;width:100%;height:auto;line-height:1.6;margin:0 0 10px;white-space:normal}.amount{font-size:20px;color:var(--bs-primary)}@media(max-width:850px){.cart-layout{grid-template-columns:1fr}.cart-summary{position:static;display:grid;grid-template-columns:1fr 1fr;gap:0 20px}.cart-summary h2,.cart-summary>span{grid-column:1/-1}.summary-total,.checkout-button{grid-column:2}.summary-row{grid-column:1;grid-row:3}.checkout-button{margin-top:0;align-self:center}}@media(max-width:600px){.cart-page{padding:45px 18px 68px}.cart-intro h1{font-size:32px}.cart-item{grid-template-columns:22px 72px 1fr 24px;gap:10px}.cart-item img{width:72px;height:66px}.cart-item :deep(.el-input-number){grid-column:3}.cart-item>strong{grid-column:3}.cart-item-info h2{font-size:15px}.remove-item{grid-column:4;grid-row:1}.cart-summary{display:block}.summary-row,.summary-total{margin-top:14px}.checkout-button{margin-top:20px}.cart-empty{padding:24px;justify-content:flex-start}}
.checkout-button{justify-content:center;gap:12px;padding:13px 16px;border:0;background:#f5c06d;color:#1f4d3a;font-size:14px;font-weight:700;transition:background .2s ease,transform .2s ease}.checkout-button:hover:not(:disabled){background:#ffd58f;transform:translateY(-1px)}.checkout-button span{display:none}.checkout-button svg{width:17px;height:17px;color:#1f4d3a}.checkout-button:disabled{background:rgba(245,192,109,.35);color:rgba(255,255,255,.55)}
</style>
