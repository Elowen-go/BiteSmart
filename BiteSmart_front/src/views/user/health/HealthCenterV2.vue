<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, Food, Plus, ScaleToOriginal, TrendCharts } from '@element-plus/icons-vue'
import { addDietRecord, addExerciseRecord, deleteDietRecord, deleteExerciseRecord, getDietRecords, getExerciseRecords, getWeightRecords, saveWeightRecord } from '../../../api/user/health'

const tab = ref('diet')
const loading = ref(false)
const dialog = ref(false)
const diet = ref<any[]>([])
const exercise = ref<any[]>([])
const weight = ref<any[]>([])
const dietForm = ref({ recordDate: '', recordTime: '', mealType: 10, foodName: '', quantity: 1, calories: 0, protein: 0, fat: 0, carbs: 0 })
const exerciseForm = ref({ recordDate: '', exerciseType: '', duration: 30, distance: 0, caloriesBurned: 0, remark: '' })
const weightForm = ref({ recordDate: '', weight: 60 })
const meals = [{ label: '早餐', value: 10 }, { label: '午餐', value: 20 }, { label: '晚餐', value: 30 }, { label: '加餐', value: 40 }]

const totalRecords = computed(() => diet.value.length + exercise.value.length + weight.value.length)
const currentRecords = computed(() => tab.value === 'diet' ? diet.value : tab.value === 'exercise' ? exercise.value : weight.value)
const currentLabel = computed(() => tab.value === 'diet' ? '饮食' : tab.value === 'exercise' ? '运动' : '体重')
const formatDateTime = (value?: string) => value ? value.replace('T', ' ').slice(0, 16) : '--'

const fetchData = async () => {
  loading.value = true
  try {
    if (tab.value === 'diet') {
      const response = await getDietRecords()
      diet.value = response.data || []
    } else if (tab.value === 'exercise') {
      const response = await getExerciseRecords()
      exercise.value = response.data || []
    } else {
      const response = await getWeightRecords()
      weight.value = response.data?.list || response.data || []
    }
  } catch {
    ElMessage.error('加载健康记录失败')
  } finally {
    loading.value = false
  }
}

const loadOverview = async () => {
  await Promise.allSettled([
    getDietRecords().then(response => { diet.value = response.data || [] }),
    getExerciseRecords().then(response => { exercise.value = response.data || [] }),
    getWeightRecords().then(response => { weight.value = response.data?.list || response.data || [] })
  ])
}

const open = () => {
  dialog.value = true
  const today = new Date().toISOString().slice(0, 10)
  if (tab.value === 'diet') { dietForm.value.recordDate = today; dietForm.value.recordTime = new Date().toTimeString().slice(0, 5) }
  if (tab.value === 'exercise') exerciseForm.value.recordDate = today
  if (tab.value === 'weight') weightForm.value.recordDate = today
}

const save = async () => {
  try {
    if (tab.value === 'diet') await addDietRecord(dietForm.value as any)
    else if (tab.value === 'exercise') await addExerciseRecord(exerciseForm.value as any)
    else await saveWeightRecord(weightForm.value as any)
    ElMessage.success('记录已保存')
    dialog.value = false
    await fetchData()
  } catch {
    ElMessage.error('保存失败，请稍后重试')
  }
}

const remove = async (type: string, id: number) => {
  try {
    await ElMessageBox.confirm('确定删除这条记录吗？', '删除记录', { type: 'warning' })
    if (type === 'diet') await deleteDietRecord(id)
    else await deleteExerciseRecord(id)
    ElMessage.success('记录已删除')
    await fetchData()
  } catch { /* 用户取消删除 */ }
}

onMounted(loadOverview)
</script>

<template>
  <div class="health-page">
    <section class="health-hero">
      <div>
        <span class="eyebrow">MY HEALTH JOURNAL</span>
        <h1>健康记录</h1>
        <p>把每天的饮食、运动和体重变化记下来，慢慢看见自己的状态。</p>
      </div>
      <el-button type="primary" class="add-button" @click="open"><Plus :size="17" />添加{{ currentLabel }}记录</el-button>
    </section>

    <section class="overview-grid">
      <div class="overview-item"><span class="overview-icon food"><Food :size="18" /></span><div><strong>{{ diet.length }}</strong><span>饮食记录</span></div></div>
      <div class="overview-item"><span class="overview-icon exercise"><TrendCharts :size="18" /></span><div><strong>{{ exercise.length }}</strong><span>运动记录</span></div></div>
      <div class="overview-item"><span class="overview-icon weight"><ScaleToOriginal :size="18" /></span><div><strong>{{ weight.length }}</strong><span>体重记录</span></div></div>
      <div class="overview-note"><Calendar :size="17" /><span>累计记录 <strong>{{ totalRecords }}</strong> 条</span></div>
    </section>

    <section class="records-panel">
      <div class="records-heading"><div><span class="section-kicker">YOUR JOURNAL</span><h2>近期记录</h2></div><span class="record-count">{{ currentRecords.length }} 条{{ currentLabel }}记录</span></div>
      <el-tabs v-model="tab" @tab-change="fetchData">
        <el-tab-pane label="饮食" name="diet">
          <el-table v-loading="loading" :data="diet" class="records-table" empty-text="暂无饮食记录，点击右上角添加">
            <el-table-column label="时间" width="155"><template #default="{ row }">{{ row.recordDate }} {{ row.recordTime || '' }}</template></el-table-column>
            <el-table-column label="餐次" width="100"><template #default="{ row }">{{ meals.find(item => item.value === row.mealType)?.label || row.mealType }}</template></el-table-column>
            <el-table-column label="食物" min-width="180"><template #default="{ row }">{{ row.foodName }} <small v-if="row.quantity">×{{ row.quantity }}</small></template></el-table-column>
            <el-table-column prop="calories" label="热量" width="100"><template #default="{ row }">{{ row.calories || 0 }} kcal</template></el-table-column>
            <el-table-column label="营养(g)" min-width="220"><template #default="{ row }"><span class="nutrition-text">蛋白质 {{ row.protein || 0 }} · 脂肪 {{ row.fat || 0 }} · 碳水 {{ row.carbs || 0 }}</span></template></el-table-column>
            <el-table-column label="来源" width="110"><template #default="{ row }"><el-tag size="small" :type="row.sourceType === 10 ? 'success' : 'info'">{{ row.sourceType === 10 ? '平台订单' : '手动记录' }}</el-tag></template></el-table-column>
            <el-table-column label="操作" width="80"><template #default="{ row }"><el-button link type="danger" @click="remove('diet', row.id)">删除</el-button></template></el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="运动" name="exercise">
          <el-table v-loading="loading" :data="exercise" class="records-table" empty-text="暂无运动记录，点击右上角添加">
            <el-table-column prop="recordDate" label="日期" width="130" /><el-table-column prop="exerciseType" label="运动类型" min-width="180" /><el-table-column prop="duration" label="时长" width="130"><template #default="{ row }">{{ row.duration }} 分钟</template></el-table-column><el-table-column prop="distance" label="距离" width="120"><template #default="{ row }">{{ row.distance || 0 }} km</template></el-table-column><el-table-column prop="caloriesBurned" label="消耗热量" width="140"><template #default="{ row }">{{ row.caloriesBurned || 0 }} kcal</template></el-table-column><el-table-column label="操作" width="80"><template #default="{ row }"><el-button link type="danger" @click="remove('exercise', row.id)">删除</el-button></template></el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="体重" name="weight">
          <el-table v-loading="loading" :data="weight" class="records-table" empty-text="暂无体重记录，点击右上角添加">
            <el-table-column prop="recordDate" label="日期" width="160" /><el-table-column prop="weight" label="体重" width="160"><template #default="{ row }"><strong>{{ row.weight }} kg</strong></template></el-table-column><el-table-column label="记录时间" min-width="220"><template #default="{ row }">{{ formatDateTime(row.createTime) }}</template></el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog v-model="dialog" :title="`添加${currentLabel}记录`" width="540px">
      <el-form v-if="tab === 'diet'" label-position="top"><div class="form-grid"><el-form-item label="日期"><el-date-picker v-model="dietForm.recordDate" type="date" value-format="YYYY-MM-DD" /></el-form-item><el-form-item label="餐次"><el-select v-model="dietForm.mealType"><el-option v-for="meal in meals" :key="meal.value" :label="meal.label" :value="meal.value" /></el-select></el-form-item></div><el-form-item label="食物名称"><el-input v-model="dietForm.foodName" placeholder="例如：鸡胸肉沙拉" /></el-form-item><el-form-item label="热量"><el-input-number v-model="dietForm.calories" :min="0" /></el-form-item><el-form-item label="蛋白质 / 脂肪 / 碳水"><div class="inline"><el-input-number v-model="dietForm.protein" :min="0" :precision="1" /><el-input-number v-model="dietForm.fat" :min="0" :precision="1" /><el-input-number v-model="dietForm.carbs" :min="0" :precision="1" /></div></el-form-item></el-form>
      <el-form v-else-if="tab === 'exercise'" label-position="top"><div class="form-grid"><el-form-item label="日期"><el-date-picker v-model="exerciseForm.recordDate" type="date" value-format="YYYY-MM-DD" /></el-form-item><el-form-item label="运动类型"><el-input v-model="exerciseForm.exerciseType" placeholder="例如：慢跑" /></el-form-item><el-form-item label="时长（分钟）"><el-input-number v-model="exerciseForm.duration" :min="1" /></el-form-item><el-form-item label="距离（km）"><el-input-number v-model="exerciseForm.distance" :min="0" :precision="1" /></el-form-item></div><el-form-item label="消耗热量"><el-input-number v-model="exerciseForm.caloriesBurned" :min="0" /></el-form-item><el-form-item label="备注"><el-input v-model="exerciseForm.remark" type="textarea" /></el-form-item></el-form>
      <el-form v-else label-position="top"><el-form-item label="日期"><el-date-picker v-model="weightForm.recordDate" type="date" value-format="YYYY-MM-DD" /></el-form-item><el-form-item label="体重（kg）"><el-input-number v-model="weightForm.weight" :min="20" :max="300" :precision="1" :step=".1" /></el-form-item></el-form>
      <template #footer><el-button @click="dialog = false">取消</el-button><el-button type="primary" @click="save">保存记录</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.health-page{max-width:1180px;margin:0 auto;padding:42px 24px 80px;color:#253129}.health-hero{display:flex;align-items:flex-end;justify-content:space-between;gap:28px;padding:8px 0 28px;border-bottom:1px solid #dfe7df}.eyebrow,.section-kicker{color:#cf704f;font-size:10px;font-weight:800;letter-spacing:.16em}.health-hero h1{margin:9px 0 7px;font-family:"Source Han Serif SC","Songti SC",serif;font-size:32px;line-height:1.1}.health-hero p{margin:0;color:#718078;font-size:13px}.add-button{display:flex;align-items:center;gap:7px;background:#1f4d3a;border-color:#1f4d3a}.add-button:hover{background:#2c654e;border-color:#2c654e}.overview-grid{display:grid;grid-template-columns:repeat(3,1fr) 1.3fr;gap:12px;padding:22px 0}.overview-item,.overview-note{display:flex;align-items:center;gap:12px;min-height:68px;padding:0 16px;background:#fff;border:1px solid #e1e9e2}.overview-item>div{display:flex;flex-direction:column;gap:3px}.overview-item strong{color:#1f4d3a;font-family:Georgia,serif;font-size:22px}.overview-item div span{color:#718078;font-size:11px}.overview-note{justify-content:center;color:#718078;font-size:12px}.overview-note svg{color:#cf704f}.overview-note strong{color:#253129;font-size:15px}.overview-icon{display:grid;place-items:center;width:34px;height:34px;border-radius:50%}.overview-icon.food{background:#f9e9df;color:#cf704f}.overview-icon.exercise{background:#e3efe5;color:#1f4d3a}.overview-icon.weight{background:#e7edf2;color:#537084}.records-panel{padding:22px 24px;background:#fff;border:1px solid #e1e9e2}.records-heading{display:flex;align-items:flex-end;justify-content:space-between;gap:18px;margin-bottom:12px}.records-heading h2{margin:6px 0 0;font-family:"Source Han Serif SC","Songti SC",serif;font-size:22px}.record-count{color:#8a968e;font-size:11px}.records-panel :deep(.el-tabs__header){margin:0 0 18px}.records-panel :deep(.el-tabs__nav-wrap::after){background:#dfe7df}.records-panel :deep(.el-tabs__item){color:#718078}.records-panel :deep(.el-tabs__item.is-active){color:#1f4d3a;font-weight:700}.records-panel :deep(.el-tabs__active-bar){background:#cf704f}.records-table{--el-table-border-color:#e5ece6;--el-table-header-bg-color:#f4f8f3;--el-table-row-hover-bg-color:#f8fbf7}.records-table :deep(th.el-table__cell){height:46px;background:#f4f8f3;color:#718078;font-size:12px}.records-table :deep(td.el-table__cell){height:58px;color:#253129;font-size:13px}.records-table :deep(.el-table__empty-block){min-height:150px}.records-table :deep(.el-table__empty-text){color:#9aa69e}.nutrition-text{color:#718078;font-size:12px}.form-grid{display:grid;grid-template-columns:1fr 1fr;gap:0 18px}.inline{display:flex;gap:8px}.inline :deep(.el-input-number){width:100%}.health-page :deep(.el-dialog__header){border-bottom:1px solid #edf1ec}.health-page :deep(.el-dialog__footer){border-top:1px solid #edf1ec}.health-page :deep(.el-button--primary){background:#1f4d3a;border-color:#1f4d3a}@media(max-width:800px){.overview-grid{grid-template-columns:1fr 1fr}.overview-note{grid-column:span 2}.records-panel{padding:18px}.health-hero{align-items:flex-start;flex-direction:column}.add-button{width:100%;justify-content:center}}@media(max-width:600px){.health-page{padding:30px 16px 58px}.overview-grid{grid-template-columns:1fr}.overview-note{grid-column:auto}.records-heading{align-items:flex-start;flex-direction:column;gap:7px}.form-grid{grid-template-columns:1fr}.records-table :deep(th.el-table__cell),.records-table :deep(td.el-table__cell){padding:8px 4px}}
.overview-icon :deep(svg),.overview-note :deep(svg){width:18px;height:18px;flex:0 0 18px}
</style>
