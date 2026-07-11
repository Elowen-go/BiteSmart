﻿﻿﻿﻿﻿﻿﻿<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDietRecords, addDietRecord, deleteDietRecord, getExerciseRecords, addExerciseRecord, deleteExerciseRecord, getWeightRecords, saveWeightRecord } from '../../../api/user/health'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeTab = ref('diet')
const loading = ref(false)

// 饮食记录
const dietRecords = ref<any[]>([])
const dietDialogVisible = ref(false)
const dietForm = ref({ mealType: '', dishIds: '', notes: '' })
const mealTypeOptions = [
  { label: '早餐', value: 'breakfast' },
  { label: '午餐', value: 'lunch' },
  { label: '晚餐', value: 'dinner' },
  { label: '加餐', value: 'snack' }
]

// 运动记录
const exerciseRecords = ref<any[]>([])
const exerciseDialogVisible = ref(false)
const exerciseForm = ref({ exerciseType: '', duration: 30, notes: '' })

// 体重记录
const weightRecords = ref<any[]>([])
const weightDialogVisible = ref(false)
const weightForm = ref({ weight: 60, recordDate: '' })

const fetchDietRecords = async () => {
  try {
    const res = await getDietRecords()
    dietRecords.value = res.data?.list || res.data || []
  } catch (e) {
    console.error('获取饮食记录失败', e)
  }
}

const fetchExerciseRecords = async () => {
  try {
    const res = await getExerciseRecords()
    exerciseRecords.value = res.data?.list || res.data || []
  } catch (e) {
    console.error('获取运动记录失败', e)
  }
}

const fetchWeightRecords = async () => {
  try {
    const res = await getWeightRecords()
    weightRecords.value = res.data?.list || res.data || []
  } catch (e) {
    console.error('获取体重记录失败', e)
  }
}

const loadData = async () => {
  loading.value = true
  try {
    if (activeTab.value === 'diet') await fetchDietRecords()
    else if (activeTab.value === 'exercise') await fetchExerciseRecords()
    else if (activeTab.value === 'weight') await fetchWeightRecords()
  } finally {
    loading.value = false
  }
}

// 饮食
const handleAddDiet = async () => {
  try {
    await addDietRecord(dietForm.value as any)
    ElMessage.success('添加成功')
    dietDialogVisible.value = false
    dietForm.value = { mealType: '', dishIds: '', notes: '' }
    fetchDietRecords()
  } catch (e) {
    ElMessage.error('添加失败')
  }
}

const handleDeleteDiet = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除该记录吗？', '提示')
    await deleteDietRecord(id)
    ElMessage.success('已删除')
    fetchDietRecords()
  } catch (e) {
    // 取消不做处理
  }
}

// 运动
const handleAddExercise = async () => {
  try {
    await addExerciseRecord(exerciseForm.value as any)
    ElMessage.success('添加成功')
    exerciseDialogVisible.value = false
    exerciseForm.value = { exerciseType: '', duration: 30, notes: '' }
    fetchExerciseRecords()
  } catch (e) {
    ElMessage.error('添加失败')
  }
}

const handleDeleteExercise = async (id: number) => {
  try {
    await ElMessageBox.confirm('确定要删除该记录吗？', '提示')
    await deleteExerciseRecord(id)
    ElMessage.success('已删除')
    fetchExerciseRecords()
  } catch (e) {
    // 取消不做处理
  }
}

// 体重
const handleAddWeight = async () => {
  try {
    await saveWeightRecord(weightForm.value as any)
    ElMessage.success('保存成功')
    weightDialogVisible.value = false
    weightForm.value = { weight: 60, recordDate: '' }
    fetchWeightRecords()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>健康记录</h3>
        <el-button type="primary" size="small" @click="activeTab === 'diet' ? dietDialogVisible = true : activeTab === 'exercise' ? exerciseDialogVisible = true : weightDialogVisible = true">
          添加记录
        </el-button>
      </div>
      <div style="padding-top: 20px;">
        <el-tabs v-model="activeTab" @tab-change="loadData">
          <el-tab-pane label="饮食记录" name="diet">
            <el-table v-loading="loading" :data="dietRecords" border>
              <el-table-column prop="recordDate" label="日期" width="120" />
              <el-table-column prop="mealType" label="餐次" width="100" />
              <el-table-column prop="totalCalories" label="热量(千卡)" width="120" />
              <el-table-column prop="notes" label="备注" min-width="200" show-overflow-tooltip />
              <el-table-column label="操作" width="120" fixed="right">
                <template #default="{ row }">
                  <el-button size="small" type="danger" @click="handleDeleteDiet(row.id)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="运动记录" name="exercise">
            <el-table v-loading="loading" :data="exerciseRecords" border>
              <el-table-column prop="recordDate" label="日期" width="120" />
              <el-table-column prop="exerciseType" label="运动类型" width="120" />
              <el-table-column prop="duration" label="时长(分钟)" width="120" />
              <el-table-column prop="caloriesBurned" label="消耗(千卡)" width="120" />
              <el-table-column prop="notes" label="备注" min-width="200" show-overflow-tooltip />
              <el-table-column label="操作" width="120" fixed="right">
                <template #default="{ row }">
                  <el-button size="small" type="danger" @click="handleDeleteExercise(row.id)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="体重记录" name="weight">
            <el-table v-loading="loading" :data="weightRecords" border>
              <el-table-column prop="recordDate" label="日期" width="120" />
              <el-table-column prop="weight" label="体重(kg)" width="120" />
              <el-table-column prop="createTime" label="记录时间" width="180" />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <!-- 添加饮食记录对话框 -->
    <el-dialog v-model="dietDialogVisible" title="添加饮食记录" width="500px">
      <el-form :model="dietForm" label-width="100px">
        <el-form-item label="餐次">
          <el-select v-model="dietForm.mealType" style="width: 100%;">
            <el-option v-for="opt in mealTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="菜品ID">
          <el-input v-model="dietForm.dishIds" placeholder="多个ID用逗号分隔" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dietForm.notes" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dietDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddDiet">确定</el-button>
      </template>
    </el-dialog>

    <!-- 添加运动记录对话框 -->
    <el-dialog v-model="exerciseDialogVisible" title="添加运动记录" width="500px">
      <el-form :model="exerciseForm" label-width="100px">
        <el-form-item label="运动类型">
          <el-input v-model="exerciseForm.exerciseType" placeholder="如：跑步、游泳" />
        </el-form-item>
        <el-form-item label="时长(分钟)">
          <el-input-number v-model="exerciseForm.duration" :min="1" :max="600" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="exerciseForm.notes" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="exerciseDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddExercise">确定</el-button>
      </template>
    </el-dialog>

    <!-- 添加体重记录对话框 -->
    <el-dialog v-model="weightDialogVisible" title="添加体重记录" width="500px">
      <el-form :model="weightForm" label-width="100px">
        <el-form-item label="体重(kg)">
          <el-input-number v-model="weightForm.weight" :min="20" :max="300" :precision="1" :step="0.1" />
        </el-form-item>
        <el-form-item label="记录日期">
          <el-date-picker v-model="weightForm.recordDate" type="date" placeholder="选择日期" style="width: 100%;" value-format="YYYY-MM-DD" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="weightDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddWeight">确定</el-button>
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
</style>
