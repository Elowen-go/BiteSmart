<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { getCartList, deleteCartItem, updateCartQuantity } from '../../../api/user/cart'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const cartItems = ref<any[]>([])

const totalAmount = computed(() => {
  return cartItems.value
    .filter(item => item.selected !== 0)
    .reduce((sum, item) => sum + (item.price || 0) * item.quantity, 0)
})

const fetchCart = async () => {
  loading.value = true
  try {
    const res = await getCartList()
    cartItems.value = res.data || []
  } catch (e) {
    console.error('获取购物车失败', e)
  } finally {
    loading.value = false
  }
}

const handleQuantityChange = async (item: any, newQuantity: number | undefined) => {
  if (!newQuantity || newQuantity < 1) return
  try {
    await updateCartQuantity(item.id, newQuantity)
    item.quantity = newQuantity
  } catch (e) {
    ElMessage.error('修改数量失败')
  }
}

const handleDelete = async (item: any) => {
  try {
    await ElMessageBox.confirm('确定要删除该商品吗？', '提示')
    await deleteCartItem(item.id)
    cartItems.value = cartItems.value.filter(i => i.id !== item.id)
    ElMessage.success('已删除')
  } catch (e) {
    // 取消删除不做处理
  }
}

const handleCheckout = () => {
  ElMessage.info('结算功能开发中')
}

onMounted(() => {
  fetchCart()
})
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>购物车</h3>
        <button class="btn btn-primary" @click="handleCheckout">去结算 (¥{{ totalAmount }})</button>
      </div>
      <div style="padding-top: 20px;">
        <el-table v-loading="loading" :data="cartItems" border>
          <el-table-column prop="dishName" label="菜品名称" min-width="140" />
          <el-table-column label="数量" width="180">
            <template #default="{ row }">
              <el-input-number
                v-model="row.quantity"
                :min="1"
                :max="99"
                size="small"
                @change="(val: number | undefined) => handleQuantityChange(row, val ?? 1)"
              />
            </template>
          </el-table-column>
          <el-table-column prop="price" label="单价" width="120">
            <template #default="{ row }">
              <span>¥{{ row.price }}</span>
            </template>
          </el-table-column>
          <el-table-column label="小计" width="120">
            <template #default="{ row }">
              <span style="font-weight: 600;">¥{{ (row.price * row.quantity).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="!loading && cartItems.length === 0" style="text-align: center; padding: 40px; color: var(--bs-text-muted);">
          购物车是空的，快去选购美食吧！
        </div>
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
