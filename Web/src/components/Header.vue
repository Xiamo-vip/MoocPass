<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Bell } from 'lucide-vue-next'

const route = useRoute()

const pageTitle = computed(() => {
  if (route.path === '/') return '仪表盘'
  if (route.path.startsWith('/platform')) return '超星学习通'
  if (route.path === '/tasks') return '任务管理'
  if (route.path === '/settings') return '系统设置'
  return '系统页面'
})
</script>

<template>
  <header class="header-container glass-panel">
    <div class="breadcrumb">
      <span class="root-title">MoocPass</span>
      <span class="slash">/</span>
      <span class="active-title">{{ pageTitle }}</span>
    </div>

    <div class="header-status-group">
      <button class="icon-btn" title="消息通知">
        <Bell class="w-4 h-4" />
      </button>
    </div>
  </header>
</template>

<style scoped>
.header-container {
  height: 64px;
  margin: 16px 16px 16px 292px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
}

.root-title {
  color: var(--text-dim);
  font-weight: 500;
}

.slash {
  color: var(--text-dim);
  opacity: 0.5;
}

.active-title {
  color: var(--text-main);
  font-weight: 700;
}

.header-status-group {
  display: flex;
  align-items: center;
  gap: 16px;
}

.icon-btn {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: var(--text-muted);
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}
.icon-btn:hover { color: var(--text-main); background: rgba(255, 255, 255, 0.1); }
</style>
