﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
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
    <div class="card-panel">
      <div class="card-header">
        <h3>评价管理</h3>
      </div>
      <div style="padding-top: 20px;">
        <el-table :data="reviewList" border v-loading="loading">
          <el-table-column prop="username" label="用户" width="120" />
          <el-table-column prop="rating" label="评分" width="100">
            <template #default="{ row }">
              <el-rate :model-value="row.rating" disabled show-score score-template="{value}分" />
            </template>
          </el-table-column>
          <el-table-column prop="content" label="评价内容" min-width="200" show-overflow-tooltip />
          <el-table-column prop="createTime" label="评价时间" width="180" />
          <el-table-column prop="replyContent" label="回复内容" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.replyContent">{{ row.replyContent }}</span>
              <span v-else style="color: #999;">未回复</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="handleReply(row)" :disabled="!!row.replyContent">
                {{ row.replyContent ? '已回复' : '回复' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <div style="display: flex; justify-content: flex-end; margin-top: 20px;">
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

