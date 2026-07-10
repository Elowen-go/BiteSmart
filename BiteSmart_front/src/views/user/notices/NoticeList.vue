<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getNoticeList, getNoticeDetail } from '../../../api/user/notices'
import type { Notice } from '../../../api/user/notices'
import { ArrowRight } from '@element-plus/icons-vue'

const loading = ref(false)
const notices = ref<Notice[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const detailVisible = ref(false)
const currentNotice = ref<Notice | null>(null)
const detailLoading = ref(false)

const fetchNotices = async () => {
  loading.value = true
  try {
    const res = await getNoticeList()
    notices.value = Array.isArray(res.data) ? res.data : (res.data?.list || [])
    total.value = notices.value.length
  } catch (e) {
    console.error('获取公告列表失败', e)
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchNotices()
}

const handleViewDetail = async (row: Notice) => {
  detailLoading.value = true
  detailVisible.value = true
  try {
    const res = await getNoticeDetail(row.id)
    currentNotice.value = res.data || row
  } catch (e) {
    currentNotice.value = row
  } finally {
    detailLoading.value = false
  }
}

const getTypeTag = (noticeType: number) => {
  const map: Record<number, { type: string; label: string }> = {
    10: { type: 'info', label: '系统公告' },
    20: { type: 'success', label: '健康知识' },
    30: { type: 'warning', label: '活动信息' },
    40: { type: 'primary', label: '升级通知' }
  }
  return map[noticeType] || { type: 'info', label: '公告' }
}

const formatDate = (dateStr: string) => {
  return dateStr ? dateStr.slice(0, 10) : ''
}

onMounted(() => {
  fetchNotices()
})
</script>

<template>
  <div class="page-container">
    <div class="card-panel">
      <div class="card-header">
        <h3>系统公告</h3>
      </div>
      <div v-loading="loading" style="padding-top: 20px;">
        <div v-for="item in notices" :key="item.id" class="notice-item" @click="handleViewDetail(item)">
          <div class="notice-left">
            <el-tag :type="getTypeTag(item.noticeType).type as any" size="small">
              {{ getTypeTag(item.noticeType).label }}
            </el-tag>
          </div>
          <div class="notice-center">
            <span class="notice-title">{{ item.title }}</span>
          </div>
          <div class="notice-right">
            <span class="notice-time">{{ formatDate(item.createTime) }}</span>
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
        <div v-if="!loading && notices.length === 0" class="empty-state">
          <el-empty description="暂无公告" />
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

    <el-dialog
      v-model="detailVisible"
      title="公告详情"
      width="700px"
      top="5vh"
    >
      <div v-loading="detailLoading" class="notice-detail">
        <template v-if="currentNotice">
          <div class="detail-header">
            <h2>{{ currentNotice.title }}</h2>
            <div class="detail-meta">
              <el-tag :type="getTypeTag(currentNotice.noticeType).type as any" size="small">
                {{ getTypeTag(currentNotice.noticeType).label }}
              </el-tag>
              <span class="detail-time">{{ currentNotice.createTime }}</span>
            </div>
          </div>
          <div class="detail-content">{{ currentNotice.content }}</div>
        </template>
      </div>
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

.notice-item {
  display: flex;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid var(--bs-border-color);
  cursor: pointer;
  transition: all 0.2s;
}

.notice-item:last-child {
  border-bottom: none;
}

.notice-item:hover {
  background: var(--bs-bg-hover);
  border-radius: var(--bs-radius-sm);
}

.notice-left {
  flex-shrink: 0;
  margin-right: 16px;
}

.notice-center {
  flex: 1;
  min-width: 0;
}

.notice-title {
  font-size: var(--bs-font-size-base);
  color: var(--bs-text-title);
  font-weight: 500;
}

.notice-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  margin-left: 16px;
}

.notice-time {
  font-size: var(--bs-font-size-xs);
  color: var(--bs-text-muted);
  white-space: nowrap;
}

.notice-right .el-icon {
  color: var(--bs-text-muted);
  font-size: 14px;
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

.notice-detail {
  min-height: 200px;
}

.detail-header {
  margin-bottom: 24px;
}

.detail-header h2 {
  font-size: 22px;
  font-weight: 600;
  color: var(--bs-text-title);
  margin-bottom: 12px;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.detail-time {
  font-size: var(--bs-font-size-sm);
  color: var(--bs-text-muted);
}

.detail-content {
  font-size: var(--bs-font-size-base);
  color: var(--bs-text-secondary);
  line-height: 1.8;
  white-space: pre-wrap;
}
</style>
