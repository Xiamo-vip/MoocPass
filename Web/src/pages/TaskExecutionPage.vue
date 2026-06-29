<script setup>
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { 
  ArrowLeft, 
  Square, 
  Terminal, 
  CheckCircle2, 
  Activity,
  RefreshCw,
  AlertTriangle,
  Clock
} from 'lucide-vue-next'
import { getTaskStatusApi, getTaskLogsApi, stopTaskApi } from '../api/task'

const route = useRoute()
const router = useRouter()
const taskId = computed(() => route.params.id)

const taskInfo = ref(null)
const logs = ref([])
const isLoading = ref(true)
const isStopping = ref(false)
const errorMessage = ref('')

let pollingTimer = null

const fetchRealData = async () => {
  if (!taskId.value) return
  try {
    const statusRes = await getTaskStatusApi(taskId.value)
    if (statusRes) {
      taskInfo.value = statusRes
    }

    const logsRes = await getTaskLogsApi(taskId.value)
    if (Array.isArray(logsRes)) {
      logs.value = logsRes
    }

    errorMessage.value = ''
  } catch (err) {
    console.error('Fetch task status error:', err)
    if (!taskInfo.value) {
      errorMessage.value = err.message || '与云端控制台通信中断'
    }
  } finally {
    isLoading.value = false
    await nextTick()
    scrollToBottom()
  }
}

const scrollToBottom = () => {
  const terminalBody = document.getElementById('task-terminal-body')
  if (terminalBody) {
    terminalBody.scrollTop = terminalBody.scrollHeight
  }
}

const handleStopTask = async () => {
  if (!confirm('确定要停止当前任务吗？')) return
  isStopping.value = true
  try {
    await stopTaskApi(taskId.value)
    await fetchRealData()
  } catch (err) {
    alert('停止任务失败: ' + (err.message || '网络异常'))
  } finally {
    isStopping.value = false
  }
}

const goBackToTasks = () => {
  router.push('/tasks')
}

const parseLogLevelClass = (logLine) => {
  if (!logLine) return 'log-default'
  if (logLine.includes('[视频]')) return 'log-video'
  if (logLine.includes('[测验]') || logLine.includes('[答题]')) return 'log-quiz'
  if (logLine.includes('[跳过]') || logLine.includes('[完成]')) return 'log-success'
  if (logLine.includes('[异常]') || logLine.includes('失败') || logLine.includes('错误')) return 'log-error'
  return 'log-default'
}

onMounted(() => {
  fetchRealData()
  pollingTimer = setInterval(fetchRealData, 1500)
})

onUnmounted(() => {
  if (pollingTimer) clearInterval(pollingTimer)
})
</script>

<template>
  <div class="minimal-execution-page">
    <!-- Top Nav Bar -->
    <div class="top-nav-bar">
      <button class="nav-back-btn" @click="goBackToTasks">
        <ArrowLeft class="w-4 h-4" /> <span>返回任务列表</span>
      </button>
      
      <div v-if="taskInfo" class="status-pill" :class="taskInfo.status.toLowerCase()">
        <span class="pill-dot"></span>
        <span class="pill-text">{{ taskInfo.status }}</span>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="isLoading" class="state-card glass-panel">
      <RefreshCw class="w-8 h-8 animate-spin text-green-400 mb-3" />
      <p class="text-slate-300 text-sm">正在加载实时状态...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="errorMessage" class="state-card glass-panel">
      <AlertTriangle class="w-10 h-10 text-red-400 mb-3" />
      <h3 class="text-base font-bold text-slate-100 mb-1">通信连接异常</h3>
      <p class="text-xs text-slate-400 mb-4">{{ errorMessage }}</p>
      <button class="secondary-btn text-xs px-4 py-2" @click="fetchRealData">重连</button>
    </div>

    <!-- Main Content Layout -->
    <div v-else-if="taskInfo" class="main-content-layout">
      <!-- 1. Course Info Header Banner (Minimal & Elegant) -->
      <div class="course-header-card glass-panel">
        <div class="course-main-info">
          <div class="cover-box">
            <img :src="taskInfo.coverUrl || 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?q=80&w=600&auto=format&fit=crop'" referrerpolicy="no-referrer" alt="cover" />
          </div>
          <div class="info-meta">
            <div class="tag-row">
              <span class="platform-name">{{ taskInfo.platformCode ? taskInfo.platformCode.toUpperCase() : 'CHAOXING' }}</span>
              <span class="task-id">#{{ taskInfo.id }}</span>
            </div>
            <h2 class="course-name">{{ taskInfo.courseName }}</h2>
            <div class="detail-row">
              <span>教师: <strong>{{ taskInfo.teacher || '官方课程' }}</strong></span>
              <span class="dot">•</span>
              <span>倍速: <strong>{{ taskInfo.speed || 1.0 }}x</strong></span>
            </div>
          </div>
        </div>

        <div class="header-actions">
          <button class="action-btn" @click="fetchRealData" title="刷新状态">
            <RefreshCw class="w-4 h-4 text-cyan-400" /> <span>刷新</span>
          </button>
          <button 
            v-if="taskInfo.status === 'RUNNING'" 
            class="action-btn stop-btn" 
            :disabled="isStopping"
            @click="handleStopTask"
          >
            <Square class="w-4 h-4 fill-current" /> <span>{{ isStopping ? '停止中...' : '停止任务' }}</span>
          </button>
        </div>
      </div>

      <!-- 2. Minimal Overall Progress Card (Fixed Dimensions, Zero Shift) -->
      <div class="progress-section-card glass-panel">
        <div class="progress-header">
          <div class="progress-title">
            <Activity class="w-4 h-4 text-green-400 animate-pulse" />
            <span>课程总体完成进度</span>
          </div>
          <div class="progress-score">{{ taskInfo.progress || 0 }}%</div>
        </div>

        <div class="progress-track">
          <div class="progress-bar" :style="{ width: (taskInfo.progress || 0) + '%' }"></div>
        </div>

        <div class="progress-footer">
          <div class="current-chapter-wrap">
            <span class="lbl">当前章节:</span>
            <span class="val">{{ taskInfo.currentChapter || '准备就绪' }}</span>
          </div>
          <div class="run-state">
            <span v-if="taskInfo.status === 'RUNNING'" class="state-text running">
              <Clock class="w-3.5 h-3.5 inline mr-1" /> 云端运行中
            </span>
            <span v-else-if="taskInfo.status === 'COMPLETED'" class="state-text completed">
              <CheckCircle2 class="w-3.5 h-3.5 inline mr-1" /> 已全完成
            </span>
            <span v-else class="state-text stopped">
              已停止
            </span>
          </div>
        </div>
      </div>

      <!-- 3. Clean Realtime Log Console (Terminal) -->
      <div class="terminal-section-card glass-panel">
        <div class="terminal-bar">
          <div class="terminal-label">
            <Terminal class="w-4 h-4 text-green-400" />
            <span>实时运行控制台日志 (Cloud Logs)</span>
          </div>
          <div class="terminal-mac-dots">
            <span class="mac-dot red"></span>
            <span class="mac-dot yellow"></span>
            <span class="mac-dot green"></span>
          </div>
        </div>

        <div class="terminal-body-view" id="task-terminal-body">
          <div v-if="logs.length === 0" class="log-empty-msg">
            > 正在从云端调取日志...
          </div>
          <div 
            v-for="(line, idx) in logs" 
            :key="idx" 
            class="log-row-item"
            :class="parseLogLevelClass(line)"
          >
            <span class="log-num">{{ idx + 1 }}</span>
            <span class="log-content">{{ line }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.minimal-execution-page {
  max-width: 1040px;
  margin: 0 auto;
  padding-bottom: 40px;
}

/* Top Navigation */
.top-nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.nav-back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #cbd5e1;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.nav-back-btn:hover {
  background: rgba(30, 41, 59, 0.8);
  color: #fff;
  border-color: rgba(255, 255, 255, 0.2);
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-family: var(--font-mono);
  font-weight: 600;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(15, 23, 42, 0.6);
}

.pill-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}

.status-pill.running {
  border-color: rgba(34, 197, 94, 0.3);
  color: #4ade80;
  background: rgba(34, 197, 94, 0.1);
}
.status-pill.running .pill-dot {
  background: #22c55e;
  box-shadow: 0 0 8px #22c55e;
}

.status-pill.completed {
  border-color: rgba(6, 182, 212, 0.3);
  color: #22d3ee;
  background: rgba(6, 182, 212, 0.1);
}
.status-pill.completed .pill-dot { background: #06b6d4; }

.status-pill.stopped, .status-pill.failed {
  border-color: rgba(239, 68, 68, 0.3);
  color: #f87171;
  background: rgba(239, 68, 68, 0.1);
}
.status-pill.stopped .pill-dot, .status-pill.failed .pill-dot { background: #ef4444; }

.state-card {
  padding: 60px 20px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

/* 1. Header Banner (Fixed height 100px, Minimalist layout) */
.course-header-card {
  height: 100px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  box-sizing: border-box;
}

.course-main-info {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
  flex: 1;
}

.cover-box {
  width: 64px;
  height: 64px;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
  flex-shrink: 0;
  background: #090d16;
}
.cover-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.info-meta {
  min-width: 0;
  flex: 1;
}

.tag-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  margin-bottom: 2px;
}
.platform-name {
  color: var(--primary-green);
  font-family: var(--font-mono);
  font-weight: 700;
}
.task-id {
  color: #64748b;
  font-family: var(--font-mono);
}

.course-name {
  font-size: 18px;
  font-weight: 700;
  color: #f8fafc;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
}

.detail-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #94a3b8;
}
.detail-row .dot { color: #475569; }

.header-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: rgba(30, 41, 59, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #e2e8f0;
  padding: 8px 14px;
  border-radius: 8px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.action-btn:hover {
  background: rgba(51, 65, 85, 0.8);
  border-color: rgba(255, 255, 255, 0.2);
}
.stop-btn {
  color: #f87171;
  border-color: rgba(239, 68, 68, 0.3);
}
.stop-btn:hover {
  background: rgba(239, 68, 68, 0.15);
}

/* 2. Overall Progress Card (Strictly Fixed height 130px, Stable Layout) */
.progress-section-card {
  height: 130px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  margin-bottom: 20px;
  box-sizing: border-box;
}

.progress-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.progress-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #94a3b8;
}

.progress-score {
  font-size: 24px;
  font-weight: 800;
  color: #4ade80;
  font-family: var(--font-mono);
  line-height: 1;
}

.progress-track {
  width: 100%;
  height: 8px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 4px;
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #22c55e, #06b6d4);
  border-radius: 4px;
  transition: width 0.4s ease-out;
}

.progress-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  gap: 12px;
}

.current-chapter-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  flex: 1;
}
.current-chapter-wrap .lbl { color: #64748b; flex-shrink: 0; }
.current-chapter-wrap .val {
  color: #cbd5e1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.run-state {
  flex-shrink: 0;
  font-family: var(--font-mono);
}
.state-text.running { color: #4ade80; }
.state-text.completed { color: #22d3ee; }
.state-text.stopped { color: #64748b; }

/* 3. Terminal Log Console Card (Fixed Body height 380px) */
.terminal-section-card {
  overflow: hidden;
  border-color: rgba(255, 255, 255, 0.1);
}

.terminal-bar {
  background: rgba(2, 6, 23, 0.9);
  padding: 12px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.terminal-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-family: var(--font-mono);
  color: #94a3b8;
}

.terminal-mac-dots {
  display: flex;
  gap: 6px;
}
.mac-dot { width: 9px; height: 9px; border-radius: 50%; }
.mac-dot.red { background: #ef4444; }
.mac-dot.yellow { background: #f59e0b; }
.mac-dot.green { background: #22c55e; }

.terminal-body-view {
  height: 380px;
  padding: 16px 20px;
  background: rgba(2, 6, 23, 0.96);
  font-family: var(--font-mono);
  font-size: 13px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
  box-sizing: border-box;
}

.log-empty-msg {
  color: #64748b;
  font-size: 12px;
}

.log-row-item {
  display: flex;
  gap: 14px;
  line-height: 1.5;
}

.log-num {
  width: 30px;
  text-align: right;
  color: #475569;
  flex-shrink: 0;
  font-size: 12px;
}

.log-content {
  word-break: break-all;
  flex: 1;
}

.log-video { color: #38bdf8; }
.log-quiz { color: #c084fc; }
.log-success { color: #4ade80; }
.log-error { color: #f87171; }
.log-default { color: #cbd5e1; }
</style>
