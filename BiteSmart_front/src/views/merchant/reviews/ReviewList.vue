﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getReviewList, replyReview } from '../../../api/merchant/reviews'
import type { MerchantReview } from '../../../api/merchant/reviews'

const loading = ref(false)
const reviewList = ref<MerchantReview[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const replyDialogVisible = ref(false)
const currentReview = ref<MerchantReview | null>(null)
const replyContent = ref('')
const activeFilter = ref<'all' | 'unreplied' | 'low' | 'replied'>('all')

const filterOptions = [
  { label: '全部', value: 'all' },
  { label: '未回复', value: 'unreplied' },
  { label: '低分评价', value: 'low' },
  { label: '已回复', value: 'replied' }
]

const filteredReviewList = computed(() => {
  if (activeFilter.value === 'unreplied') return reviewList.value.filter((item) => !item.replyContent)
  if (activeFilter.value === 'low') return reviewList.value.filter((item) => Number(item.rating || 0) <= 3)
  if (activeFilter.value === 'replied') return reviewList.value.filter((item) => !!item.replyContent)
  return reviewList.value
})

const reviewStats = computed(() => {
  const unreplied = reviewList.value.filter((item) => !item.replyContent).length
  const low = reviewList.value.filter((item) => Number(item.rating || 0) <= 3).length
  const avg = reviewList.value.length
    ? reviewList.value.reduce((sum, item) => sum + Number(item.rating || 0), 0) / reviewList.value.length
    : 0

  return [
    { label: '平均评分', value: avg.toFixed(1), hint: '当前页评价均分', className: 'score' },
    { label: '未回复', value: unreplied, hint: '建议优先处理', className: 'warning' },
    { label: '低分评价', value: low, hint: '3 分及以下', className: 'danger' },
    { label: '评价总数', value: reviewList.value.length, hint: '当前页记录', className: 'muted' }
  ]
})

const fetchList = async () => {
  loading.value = true
  try {
    const res = await getReviewList({ page: page.value, size: size.value })
    if (res.code === 200) {
      reviewList.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    console.error('获取评价列表失败', e)
  } finally {
    loading.value = false
  }
}

const handleReply = (row: any) => {
  currentReview.value = row
  replyContent.value = row.replyContent || ''
  replyDialogVisible.value = true
}

const submitReply = async () => {
  if (!currentReview.value || !replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  loading.value = true
  try {
    const res = await replyReview(currentReview.value.id, replyContent.value)
    if (res.code === 200) {
      ElMessage.success('回复成功')
      replyDialogVisible.value = false
      fetchList()
    } else {
      ElMessage.error(res.message || '回复失败')
    }
  } catch (e) {
    ElMessage.error('回复失败')
  } finally {
    loading.value = false
  }
}

const handlePageChange = (val: number) => {
  page.value = val
  fetchList()
}

const handleSizeChange = (val: number) => {
  size.value = val
  page.value = 1
  fetchList()
}

onMounted(() => {
  fetchList()
})
</script>

<template>
  <div class="page-container">
    <div class="review-page">
      <div class="page-head">
        <div>
          <h2>评价管理</h2>
          <p>优先处理低分和未回复评价，维护门店反馈闭环</p>
        </div>
        <el-button :icon="Refresh" :loading="loading" @click="fetchList">刷新</el-button>
      </div>

      <div class="summary-grid">
        <div v-for="item in reviewStats" :key="item.label" class="summary-item" :class="item.className">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <em>{{ item.hint }}</em>
        </div>
      </div>

      <div class="card-panel review-panel">
        <div class="toolbar">
          <el-segmented v-model="activeFilter" :options="filterOptions" />
          <span class="toolbar-count">当前 {{ filteredReviewList.length }} 条评价</span>
        </div>

        <el-table :data="filteredReviewList" v-loading="loading" empty-text="暂无符合条件的评价">
          <el-table-column prop="username" label="用户" width="140">
            <template #default="{ row }">
              <div class="user-name">{{ row.username || '匿名用户' }}</div>
              <div class="sub-text">订单 {{ row.orderId }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="rating" label="评分" width="150">
            <template #default="{ row }">
              <el-rate :model-value="row.rating" disabled show-score score-template="{value}分" />
            </template>
          </el-table-column>
          <el-table-column prop="content" label="评价内容" min-width="240" show-overflow-tooltip />
          <el-table-column prop="createTime" label="评价时间" width="180" />
          <el-table-column prop="replyContent" label="回复内容" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.replyContent">{{ row.replyContent }}</span>
              <span v-else class="unreplied">未回复</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.replyContent" type="success">已回复</el-tag>
              <el-tag v-else-if="Number(row.rating || 0) <= 3" type="danger">待安抚</el-tag>
              <el-tag v-else type="warning">待回复</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="110" fixed="right" align="right">
            <template #default="{ row }">
              <el-button size="small" :type="row.replyContent ? 'info' : 'primary'" @click="handleReply(row)" :disabled="!!row.replyContent">
                {{ row.replyContent ? '已处理' : '回复' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-row">
          <el-pagination
            v-if="total > 0"
            :current-page="page"
            :page-size="size"
            :total="total"
            :page-sizes="[5, 10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>
      </div>
    </div>

    <el-dialog v-model="replyDialogVisible" title="回复评价" width="500px">
      <div v-if="currentReview" class="reply-context">
        <div class="user-name">{{ currentReview.username || '匿名用户' }}</div>
        <div class="sub-text">{{ currentReview.content }}</div>
      </div>
      <el-input
        v-model="replyContent"
        type="textarea"
        :rows="4"
        placeholder="请输入回复内容"
      />
      <template #footer>
        <el-button @click="replyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReply" :loading="loading">确定回复</el-button>
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

.review-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-head h2 {
  margin: 0;
  color: var(--bs-text-title);
  font-size: 20px;
  font-weight: 650;
}

.page-head p {
  margin-top: 4px;
  color: var(--bs-text-muted);
  font-size: 13px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.summary-item {
  min-height: 104px;
  padding: 16px;
  background: #fff;
  border: 1px solid var(--bs-border-light);
  border-radius: var(--bs-radius-md);
  box-shadow: var(--bs-card-shadow);
}

.summary-item span,
.summary-item em {
  display: block;
  color: var(--bs-text-muted);
  font-size: 13px;
  font-style: normal;
}

.summary-item strong {
  display: block;
  margin: 6px 0 4px;
  color: var(--bs-text-title);
  font-size: 28px;
  line-height: 1.1;
}

.summary-item.score,
.summary-item.muted {
  border-left: 3px solid #8a9299;
}

.summary-item.warning {
  border-left: 3px solid #b76e2a;
}

.summary-item.danger {
  border-left: 3px solid var(--bs-status-danger);
}

.card-panel {
  background: var(--bs-card-bg);
  border-radius: var(--bs-radius-md);
  box-shadow: var(--bs-card-shadow);
  padding: var(--bs-spacing-lg);
}

.review-panel {
  padding-top: 16px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.toolbar-count {
  color: var(--bs-text-muted);
  font-size: 13px;
  white-space: nowrap;
}

.user-name {
  color: var(--bs-text-title);
  font-weight: 600;
}

.sub-text {
  margin-top: 4px;
  color: var(--bs-text-muted);
  font-size: 12px;
}

.unreplied {
  color: var(--bs-text-muted);
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.reply-context {
  padding: 12px;
  margin-bottom: 14px;
  background: var(--bs-bg-hover);
  border-radius: var(--bs-radius-md);
}

@media (max-width: 1100px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .page-head,
  .toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
