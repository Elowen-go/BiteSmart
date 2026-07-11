<script setup lang="ts">
import type { Component } from 'vue'

interface Props {
  icon: Component
  label: string
  value: string | number
  change?: number
  color?: string
}

defineProps<Props>()

const formatValue = (val: string | number): string => {
  if (typeof val === 'number') {
    if (val >= 10000) {
      return (val / 10000).toFixed(1) + '万'
    }
    return val.toLocaleString()
  }
  return val
}
</script>

<template>
  <div class="stat-card">
    <div class="label">
      <component :is="icon" :style="{ color: color || 'var(--bs-primary)' }" />
      <span>{{ label }}</span>
    </div>
    <div class="value">{{ formatValue(value) }}</div>
    <div v-if="change !== undefined" class="change">
      <span :class="change >= 0 ? 'up' : 'down'">
        {{ change >= 0 ? '↑' : '↓' }} {{ Math.abs(change) }}%
      </span>
      <span style="margin-left: 4px;">较上月</span>
    </div>
  </div>
</template>

<style scoped>
.stat-card {
  background: var(--bs-card-bg);
  border-radius: var(--bs-radius-md);
  box-shadow: var(--bs-card-shadow);
  padding: var(--bs-spacing-lg);
  transition: box-shadow 0.2s;
}

.stat-card:hover {
  box-shadow: var(--bs-card-shadow-hover);
}

.label {
  font-size: var(--bs-font-size-sm);
  color: var(--bs-text-muted);
  margin-bottom: var(--bs-spacing-sm);
  display: flex;
  align-items: center;
  gap: 6px;
}

.label :deep(svg) {
  width: 20px;
  height: 20px;
}

.value {
  font-size: var(--bs-font-size-3xl);
  font-weight: 600;
  color: var(--bs-text-title);
}

.change {
  font-size: var(--bs-font-size-xs);
  margin-top: 6px;
  color: var(--bs-text-muted);
}

.up {
  color: #2D8F5C;
}

.down {
  color: #D9534F;
}
</style>