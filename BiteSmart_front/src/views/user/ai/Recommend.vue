<script setup lang="ts">
import { ref } from 'vue'
import { aiRecommend } from '../../../api/user/ai'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const recommendations = ref<any>(null)
const mealType = ref('')
const dietaryRestrictions = ref('')

const mealTypeOptions = [
  { label: '早餐', value: 'breakfast' },
  { label: '午餐', value: 'lunch' },
  { label: '晚餐', value: 'dinner' },
  { label: '加餐', value: 'snack' }
]

const handleGenerate = async () => {
  loading.value = true
  try {
    const params: any = {}
    if (mealType.value) params.mealType = mealType.value
    if (dietaryRestrictions.value) params.dietaryRestrictions = dietaryRestrictions.value
    const res = await aiRecommend(params)
    recommendations.value = res.data
  } catch (e) {
    ElMessage.error('获取推荐失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>AI推荐</h3>
        <el-button type="primary" :loading="loading" @click="handleGenerate">生成推荐</el-button>
      </div>
      <div style="padding-top: 20px;">
        <div style="display: flex; gap: 16px; margin-bottom: 20px;">
          <el-select v-model="mealType" placeholder="选择餐次" clearable style="width: 160px;">
            <el-option v-for="opt in mealTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-input v-model="dietaryRestrictions" placeholder="饮食限制（可选）" clearable style="width: 240px;" />
        </div>

        <div v-loading="loading" style="min-height: 200px;">
          <template v-if="recommendations">
            <div v-if="recommendations.dishes && recommendations.dishes.length" style="margin-bottom: 20px;">
              <h4 style="margin-bottom: 12px; color: var(--bs-text-title);">推荐菜品</h4>
              <el-table :data="recommendations.dishes" border>
                <el-table-column prop="name" label="菜品名称" />
                <el-table-column prop="calories" label="热量(千卡)" width="120" />
                <el-table-column prop="protein" label="蛋白质(g)" width="120" />
                <el-table-column prop="fat" label="脂肪(g)" width="120" />
                <el-table-column prop="carbs" label="碳水(g)" width="120" />
              </el-table>
            </div>
            <div v-if="recommendations.suggestion" class="card-panel" style="margin-top: 16px;">
              <div class="card-header">
                <h3>营养建议</h3>
              </div>
              <p style="line-height: 1.8; color: var(--bs-text-muted);">{{ recommendations.suggestion }}</p>
            </div>
          </template>
          <div v-else-if="!loading" style="text-align: center; padding: 60px 20px;">
            <div style="font-size: 48px; margin-bottom: 16px;">✨</div>
            <div style="font-size: 18px; font-weight: 600; color: var(--bs-text-title);">个性化食谱推荐</div>
            <div style="font-size: 14px; color: var(--bs-text-muted); margin-top: 8px;">基于您的健康档案，点击上方按钮生成推荐</div>
          </div>
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
