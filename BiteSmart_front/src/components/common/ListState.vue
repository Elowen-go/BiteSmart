<script setup lang="ts">
import { Loading } from '@element-plus/icons-vue'

interface Props {
  loading?: boolean
  error?: string
  empty?: boolean
  emptyText?: string
}

withDefaults(defineProps<Props>(), {
  loading: false,
  error: '',
  empty: false,
  emptyText: '暂无数据'
})

defineEmits<{ retry: [] }>()
</script>

<template>
  <div class="list-state">
    <el-alert v-if="error" title="数据加载失败" :description="error" type="error" show-icon :closable="false">
      <template #default>
        <div class="error-content">
          <span>{{ error }}</span>
          <el-button size="small" type="danger" plain @click="$emit('retry')">重新加载</el-button>
        </div>
      </template>
    </el-alert>
    <div v-else-if="loading" class="loading-state">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>正在加载...</span>
    </div>
    <el-empty v-else-if="empty" :description="emptyText" :image-size="80">
      <el-button type="primary" plain @click="$emit('retry')">重新加载</el-button>
    </el-empty>
    <slot v-else />
  </div>
</template>

<style scoped>
.list-state { min-height: 180px; }
.loading-state { display: flex; align-items: center; justify-content: center; gap: 8px; min-height: 180px; color: var(--bs-text-muted); }
.error-content { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
</style>
