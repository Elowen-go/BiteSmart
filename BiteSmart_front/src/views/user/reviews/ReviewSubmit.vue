<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { createReview, getMyReviews } from '../../../api/user/reviews'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const reviews = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const form = ref({
  orderId: 0,
  rating: 0,
  content: ''
})
const submitLoading = ref(false)

const fetchReviews = async () => {
  loading.value = true
  try {
    const res = await getMyReviews({ page: currentPage.value, size: pageSize.value })
    reviews.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (e) {
    console.error('获取评价列表失败', e)
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (!form.value.orderId) {
    ElMessage.warning('请输入订单ID')
    return
  }
  if (form.value.rating === 0) {
    ElMessage.warning('请评分')
    return
  }
  if (!form.value.content.trim()) {
    ElMessage.warning('请输入评价内容')
    return
  }

  submitLoading.value = true
  try {
    await createReview(form.value.orderId, form.value as any)
    ElMessage.success('评价提交成功')
    form.value = { orderId: 0, rating: 0, content: '' }
    fetchReviews()
  } catch (e) {
    ElMessage.error('提交失败')
  } finally {
    submitLoading.value = false
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchReviews()
}

const formatDate = (dateStr: string) => {
  return dateStr ? dateStr.slice(0, 16) : ''
}

onMounted(() => {
  fetchReviews()
})
</script>

<template>
  <div class="page-container">
    <!-- 提交评价 -->
    <div class="card-panel">
      <div class="card-header">
        <h3>提交评价</h3>
      </div>
      <div style="padding-top: 20px;">
        <el-form :model="form" label-width="100px" label-position="right">
          <el-form-item label="订单ID">
            <el-input-number
              v-model="form.orderId"
              :min="1"
              placeholder="请输入订单ID"
              style="width: 200px;"
            />
          </el-form-item>
          <el-form-item label="评分">
            <el-rate v-model="form.rating" :colors="['#F56C6C', '#E6A23C', '#67C23A']" />
          </el-form-item>
          <el-form-item label="评价内容">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="4"
              placeholder="请输入评价内容"
              maxlength="500"
              show-word-limit
              style="max-width: 500px;"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              :loading="submitLoading"
              @click="handleSubmit"
            >提交评价</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 我的评价列表 -->
    <div class="card-panel" style="margin-top: var(--bs-spacing-lg);">
      <div class="card-header">
        <h3>我的评价</h3>
      </div>
      <div v-loading="loading" style="padding-top: 20px;">
        <div v-for="item in reviews" :key="item.id" class="review-card">
          <div class="review-header">
            <div class="review-order">
              <span class="label">订单 #{{ item.orderId }}</span>
            </div>
            <el-rate
              v-model="item.rating"
              disabled
              :colors="['#F56C6C', '#E6A23C', '#67C23A']"
            />
            <span class="review-time">{{ formatDate(item.createTime) }}</span>
          </div>
          <div class="review-content">{{ item.content }}</div>
          <div v-if="item.replyContent" class="review-reply">
            <span class="reply-label">商家回复：</span>
            {{ item.replyContent }}
          </div>
        </div>
        <div v-if="!loading && reviews.length === 0" class="empty-state">
          <el-empty description="暂无评价" />
        </div>
        <div v-if="total > pageSize" class="pagination-wrap">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="handlePageChange"
          />
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

.review-card {
  border: 1px solid var(--bs-border-color);
  border-radius: var(--bs-radius-md);
  padding: 16px;
  margin-bottom: 12px;
  transition: all 0.2s;
}

.review-card:hover {
  border-color: var(--bs-primary);
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.review-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}

.review-order .label {
  font-size: var(--bs-font-size-sm);
  font-weight: 600;
  color: var(--bs-text-title);
}

.review-time {
  font-size: var(--bs-font-size-xs);
  color: var(--bs-text-muted);
  margin-left: auto;
}

.review-content {
  font-size: var(--bs-font-size-base);
  color: var(--bs-text-secondary);
  line-height: 1.6;
}

.review-reply {
  margin-top: 12px;
  padding: 12px;
  background: var(--bs-bg-page);
  border-radius: var(--bs-radius-sm);
  font-size: var(--bs-font-size-sm);
  color: var(--bs-text-secondary);
  line-height: 1.5;
}

.reply-label {
  color: var(--bs-primary);
  font-weight: 500;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
