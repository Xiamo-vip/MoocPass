<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Layers, CheckSquare, Settings, ArrowRight, Activity, Cpu, ShieldCheck } from 'lucide-vue-next'
import { getPlatformListApi } from '../api/platform'
import { getTaskListApi } from '../api/task'

const router = useRouter()
const platforms = ref([])
const runningTaskCount = ref(0)
const totalTaskCount = ref(0)
const isLoading = ref(true)

const loadDashboardData = async () => {
  isLoading.value = true
  try {
    const platformRes = await getPlatformListApi()
    if (Array.isArray(platformRes)) {
      platforms.value = platformRes
    }
  } catch (e) {
    console.warn('Load platforms error:', e)
  }

  try {
    const taskRes = await getTaskListApi()
    if (Array.isArray(taskRes)) {
      totalTaskCount.value = taskRes.length
      runningTaskCount.value = taskRes.filter(t => t.status === 'RUNNING').length
    }
  } catch (e) {
    console.warn('Load tasks error:', e)
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  loadDashboardData()
})
</script>

<template>
  <div class="dashboard-page">
    <!-- Welcome Banner -->
    <div class="welcome-banner glass-panel">
      <div class="banner-content">
        <h1 class="text-gradient">欢迎回来</h1>
        <p>简洁高效的课程自动化管理中心</p>
      </div>
    </div>

    <!-- Quick Stats Grid -->
    <div class="stats-grid">
      <div class="stat-card glass-card">
        <div class="stat-icon green">
          <Activity class="w-5 h-5 text-green-400" />
        </div>
        <div class="stat-info">
          <span class="stat-label">运行中任务</span>
          <span class="stat-value text-green-400">{{ runningTaskCount }} <span class="unit">个</span></span>
        </div>
      </div>

      <div class="stat-card glass-card">
        <div class="stat-icon cyan">
          <CheckSquare class="w-5 h-5 text-cyan-400" />
        </div>
        <div class="stat-info">
          <span class="stat-label">累计处理任务</span>
          <span class="stat-value text-cyan-400">{{ totalTaskCount }} <span class="unit">个</span></span>
        </div>
      </div>

      <div class="stat-card glass-card">
        <div class="stat-icon violet">
          <Cpu class="w-5 h-5 text-violet-400" />
        </div>
        <div class="stat-info">
          <span class="stat-label">接入平台</span>
          <span class="stat-value text-violet-400">{{ platforms.length || 2 }} <span class="unit">个</span></span>
        </div>
      </div>
    </div>

    <!-- Supported Platforms Section -->
    <div class="section-title">
      <h3>支持的网课平台</h3>
      <p>选择平台以进入对应控制台</p>
    </div>

    <div class="platforms-grid">
      <!-- Chaoxing Card -->
      <div class="platform-card glass-card" @click="router.push('/platform/chaoxing')">
        <div class="card-top">
          <div class="platform-logo cx">CX</div>
          <span class="badge-status" :class="platforms.find(p => p.code === 'chaoxing')?.bound ? 'badge-bound' : 'badge-unbound'">
            {{ platforms.find(p => p.code === 'chaoxing')?.bound ? '账号已绑定' : '账号未绑定' }}
          </span>
        </div>
        <div class="card-body">
          <h4>超星学习通</h4>
          <p>支持课程学习与章节答题自动解析。</p>
        </div>
        <div class="card-action">
          <span>进入平台控制台</span>
          <ArrowRight class="w-4 h-4" />
        </div>
      </div>

      <!-- Zhijiaoyun Card -->
      <div class="platform-card glass-card" @click="router.push('/platform/zhy')">
        <div class="card-top">
          <div class="platform-logo zhy">ZHY</div>
          <span class="badge-status" :class="platforms.find(p => p.code === 'zhy')?.bound ? 'badge-bound' : 'badge-unbound'">
            {{ platforms.find(p => p.code === 'zhy')?.bound ? '账号已绑定' : '账号未绑定' }}
          </span>
        </div>
        <div class="card-body">
          <h4>职教云</h4>
          <p>基于 Token / OAuth 原生授权与自动化刷课。</p>
        </div>
        <div class="card-action">
          <span>进入平台控制台</span>
          <ArrowRight class="w-4 h-4" />
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.welcome-banner {
  padding: 36px 40px;
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.9) 0%, rgba(30, 41, 59, 0.7) 100%);
  border-color: rgba(34, 197, 94, 0.25);
  position: relative;
  overflow: hidden;
}

.welcome-banner::after {
  content: '';
  position: absolute;
  right: -50px;
  top: -50px;
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(34, 197, 94, 0.15) 0%, transparent 70%);
  pointer-events: none;
}

.banner-content h1 {
  font-size: 26px;
  font-weight: 800;
  margin-bottom: 8px;
}

.banner-content p {
  color: var(--text-muted);
  font-size: 14px;
  max-width: 600px;
  margin-bottom: 24px;
}

.banner-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
}

.stat-card {
  padding: 20px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon.green { background: rgba(34, 197, 94, 0.15); border: 1px solid rgba(34, 197, 94, 0.3); }
.stat-icon.cyan { background: rgba(6, 182, 212, 0.15); border: 1px solid rgba(6, 182, 212, 0.3); }
.stat-icon.violet { background: rgba(139, 92, 246, 0.15); border: 1px solid rgba(139, 92, 246, 0.3); }

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 13px;
  color: var(--text-muted);
}

.stat-value {
  font-size: 24px;
  font-weight: 800;
  font-family: var(--font-mono);
}
.stat-value .unit {
  font-size: 13px;
  font-weight: 400;
  color: var(--text-dim);
}

.section-title h3 {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-main);
}
.section-title p {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 4px;
}

.platforms-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 20px;
}

.platform-card {
  padding: 24px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 180px;
}

.platform-card.disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.platform-card.disabled:hover {
  transform: none;
  border-color: rgba(255, 255, 255, 0.07);
  box-shadow: none;
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.platform-logo {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 16px;
  color: white;
}
.platform-logo.cx { background: linear-gradient(135deg, #e11d48, #be123c); }
.platform-logo.zhy { background: linear-gradient(135deg, #0284c7, #2563eb); }
.platform-logo.zhs { background: linear-gradient(135deg, #0284c7, #0369a1); }


.card-body h4 {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-main);
  margin-top: 16px;
}
.card-body p {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 6px;
}

.card-action {
  margin-top: 20px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--primary-green);
}
</style>
