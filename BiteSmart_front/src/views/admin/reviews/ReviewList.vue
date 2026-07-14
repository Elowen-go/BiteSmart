<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getReviewList, updateReviewStatus } from '../../../api/admin/reviews'
import { resolveFileUrl } from '../../../utils/fileUrl'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const detailVisible = ref(false)
const detailData = ref<any>(null)

const imageList = (row: any): string[] => {
  if (!row?.images) return []
  try {
    const value = typeof row.images === 'string' ? JSON.parse(row.images) : row.images
    return Array.isArray(value) ? value : []
  } catch { return [] }
}
const displayUser = (row: any) => row.isAnonymous === 1 ? '匿名用户' : (row.userNickname || row.username || `用户 #${row.userId || '-'}`)
const openDetail = (row: any) => { detailData.value = row; detailVisible.value = true }

const loadData = async () => {
  loading.value = true
  try {
    const res = await getReviewList({ pageNum: pageNum.value, pageSize: pageSize.value })
    if (res.code === 200) {
      tableData.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } catch (err) {
    console.error('获取评价列表失败', err)
  } finally {
    loading.value = false
  }
}

const statusMap: Record<number, string> = { 10: '已发布', 20: '已隐藏', 30: '违规删除' }
const statusTypeMap: Record<number, 'success' | 'warning' | 'danger'> = { 10: 'success', 20: 'warning', 30: 'danger' }

const handleToggleHide = async (row: any) => {
  const newStatus = row.status === 10 ? 20 : 10
  try {
    const res = await updateReviewStatus(row.id, newStatus)
    if (res.code === 200) {
      row.status = newStatus
      ElMessage.success(newStatus === 20 ? '评价已隐藏' : '评价已显示')
    }
  } catch (err) {
    console.error('更新评价状态失败', err)
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>评论管理</h3>
      </div>
      <div style="padding-top: 20px;">
        <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
          <el-table-column label="评分" width="190">
            <template #default="{ row }">
              <div class="rating-cell">
                <el-rate :model-value="Number(row.overallRating || 0)" disabled size="small" />
                <small>餐品 {{ row.ratingFood ?? '-' }} · 配送 {{ row.ratingDelivery ?? '-' }} · 服务 {{ row.ratingService ?? '-' }}</small>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="content" label="评价内容" min-width="260" show-overflow-tooltip />
          <el-table-column label="用户" min-width="240">
            <template #default="{ row }"><div class="user-cell"><span>{{ displayUser(row) }}</span><small v-if="row.isAnonymous !== 1" class="muted">ID {{ row.userId || '-' }}</small></div></template>
          </el-table-column>
          <el-table-column label="商家 / 菜品" min-width="220">
            <template #default="{ row }"><strong>{{ row.shopName || `商家 #${row.merchantId || '-'}` }}</strong><small class="muted">{{ row.dishName || '整单评价' }}</small></template>
          </el-table-column>
          <el-table-column label="图片" width="120">
            <template #default="{ row }"><div class="thumbs"><el-image v-for="(src, i) in imageList(row).slice(0, 3)" :key="i" :src="resolveFileUrl(src)" :preview-src-list="imageList(row).map(resolveFileUrl)" fit="cover" /><span v-if="!imageList(row).length" class="muted">无</span></div></template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTypeMap[row.status] || 'info'" size="small">
                {{ statusMap[row.status] || '未知' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="170">
            <template #default="{ row }">
              {{ row.createTime?.slice(0, 16) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" link @click="openDetail(row)">详情</el-button>
              <el-button
                size="small"
                :type="row.status === 20 ? 'success' : 'warning'"
                link
                @click="handleToggleHide(row)"
              >
                {{ row.status === 20 ? '显示' : '隐藏' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <div style="display:flex;justify-content:flex-end;padding-top:16px;">
          <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="loadData"
            @size-change="loadData"
          />
        </div>
      </div>
    </div>
    <el-dialog v-model="detailVisible" title="评价详情" width="680px">
      <template v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户">{{ displayUser(detailData) }}</el-descriptions-item>
          <el-descriptions-item label="商家">{{ detailData.shopName || `商家 #${detailData.merchantId || '-'}` }}</el-descriptions-item>
          <el-descriptions-item label="菜品">{{ detailData.dishName || '整单评价' }}</el-descriptions-item>
          <el-descriptions-item label="综合评分">{{ detailData.overallRating ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="评价内容" :span="2">{{ detailData.content || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="商家回复" :span="2">{{ detailData.merchantReply || '暂无回复' }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="imageList(detailData).length" class="detail-images"><el-image v-for="(src, i) in imageList(detailData)" :key="i" :src="resolveFileUrl(src)" :preview-src-list="imageList(detailData).map(resolveFileUrl)" fit="cover" /></div>
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
.rating-cell { display: flex; flex-direction: column; gap: 4px; }
.rating-cell small, .muted { color: #718078; font-size: 11px; }
.muted { display: block; }
.user-cell { min-width: 210px; white-space: nowrap; }
.user-cell span, .user-cell small { overflow: hidden; text-overflow: ellipsis; }
.thumbs { display: flex; gap: 6px; align-items: center; }
.thumbs .el-image { width: 34px; height: 34px; border-radius: 4px; }
</style>
