<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { 
  Pause, 
  Play, 
  Square, 
  Terminal, 
  CheckCircle2, 
  Sparkles, 
  Clock, 
  Activity,
  ArrowLeft
} from 'lucide-vue-next'
import gsap from 'gsap'
import { getTaskStatusApi, getTaskLogsApi } from '../api/task'

const props = defineProps({
  taskConfig: { type: Object, required: true }
})

const emit = defineEmits(['finishTask', 'backToCourses'])

const isRunning = ref(true)
const progress = ref(25)
const stats = ref({
  finishedEpisodes: 4,
  totalEpisodes: 18,
  aiQuestionsSolved: 12,
  autoSubmitted: 3
})

// Streaming Terminal Logs
const logs = ref([
  { id: 1, time: '18:56:01', level: 'INFO', msg: `[MoocPass] 连接成功。关联任务ID: ${props.taskConfig.taskId || 'TASK-8809'}` },
  { id: 2, time: '18:56:02', level: 'INFO', msg: `课程配置与身份校验通过。` },
  { id: 3, time: '18:56:03', level: 'AI_SOLVER', msg: `初始化题目解析模块` },
  { id: 4, time: '18:56:05', level: 'INFO', msg: `正在播放课程视频: ${props.taskConfig.courseName}` }
])

let timer = null
let logIdCounter = 10

const fetchBackendStatusAndLogs = async () => {
  if (!props.taskConfig.taskId) return
  try {
    const taskStatus = await getTaskStatusApi(props.taskConfig.taskId)
    if (taskStatus && taskStatus.progress !== undefined) {
      progress.value = taskStatus.progress
    }
    const backendLogs = await getTaskLogsApi(props.taskConfig.taskId)
    if (Array.isArray(backendLogs) && backendLogs.length > 0) {
      logs.value = backendLogs.map((msg, idx) => ({
        id: idx + 1,
        time: new Date().toTimeString().split(' ')[0],
        level: msg.includes('AI') ? 'AI_SOLVER' : (msg.includes('成功') || msg.includes('完成') ? 'SUCCESS' : 'INFO'),
        msg
      }))
      return
    }
  } catch (e) {
    // Standard streaming fallback
  }
}

const addMockLog = () => {
  if (!isRunning.value || progress.value >= 100) return
  
  progress.value = Math.min(100, progress.value + Math.floor(Math.random() * 8) + 3)
  
  gsap.to('.progress-bar-inner', {
    width: progress.value + '%',
    duration: 0.5,
    ease: 'power2.out'
  })

  const now = new Date().toTimeString().split(' ')[0]
  const mockMsgs = [
    { level: 'INFO', msg: `保持连接正常。` },
    { level: 'AI_SOLVER', msg: `[AI解析] 正在检索解析题目... 准确率 99.8%` },
    { level: 'SUCCESS', msg: `[自动提交] 章节测试已保存/提交。` },
    { level: 'INFO', msg: `视频播放进度: ${(progress.value).toFixed(0)}% (${props.taskConfig.speed || 1.0}x)` },
    { level: 'SUCCESS', msg: `当前章节播放完毕，自动进入下一章节...` }
  ]

  const randomItem = mockMsgs[Math.floor(Math.random() * mockMsgs.length)]
  logs.value.push({
    id: logIdCounter++,
    time: now,
    level: randomItem.level,
    msg: randomItem.msg
  })

  if (randomItem.level === 'AI_SOLVER') stats.value.aiQuestionsSolved += 1
  if (randomItem.level === 'SUCCESS') stats.value.autoSubmitted += 1

  setTimeout(() => {
    const terminalBody = document.getElementById('terminal-body')
    if (terminalBody) terminalBody.scrollTop = terminalBody.scrollHeight
  }, 50)
}

onMounted(() => {
  fetchBackendStatusAndLogs()
  timer = setInterval(() => {
    fetchBackendStatusAndLogs()
    addMockLog()
  }, 2500)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

const togglePause = () => {
  isRunning.value = !isRunning.value
}

const handleStop = () => {
  isRunning.value = false
  emit('finishTask')
}
</script>

<template>
  <div class="execution-container">
    <!-- Top Nav -->
    <div class="nav-bar">
      <button class="secondary-btn" @click="emit('backToCourses')">
        <ArrowLeft class="w-4 h-4" /> 返回课程列表
      </button>
      <div class="status-indicator">
        <span class="pulse-dot" :class="{ paused: !isRunning }"></span>
        <span>{{ isRunning ? '任务运行中' : '已暂停' }}</span>
      </div>
    </div>

    <!-- Header info -->
    <div class="header-card glass-panel">
      <div class="task-meta">
        <span class="badge-running">
          <Activity class="w-3.5 h-3.5 text-green-400 animate-pulse" /> 正在执行任务
        </span>
        <h2>{{ taskConfig.courseName }}</h2>
        <p>解析模式: <span class="text-purple-400 font-semibold">{{ taskConfig.solverMode === 'ai' ? 'AI 智能解析' : '标准题库' }}</span> | 倍速: {{ taskConfig.speed }}x</p>
      </div>

      <div class="action-controls">
        <button class="secondary-btn" @click="togglePause">
          <Play v-if="!isRunning" class="w-4 h-4 text-green-400 fill-current" />
          <Pause v-else class="w-4 h-4 text-amber-400" />
          <span>{{ isRunning ? '暂停任务' : '恢复任务' }}</span>
        </button>
        <button class="secondary-btn text-red-400 hover:bg-red-500/20" @click="handleStop">
          <Square class="w-4 h-4 fill-current" /> 停止任务
        </button>
      </div>
    </div>

    <!-- Progress & Stats Row -->
    <div class="dashboard-row">
      <!-- Main Progress Card -->
      <div class="progress-card glass-panel">
        <div class="card-title-row">
          <span>总进度完成率</span>
          <span class="percentage-text">{{ progress }}%</span>
        </div>
        <div class="progress-bar-outer">
          <div class="progress-bar-inner" :style="{ width: progress + '%' }"></div>
        </div>
        <div class="progress-sub">
          <span>进行中</span>
          <span class="text-green-400">连接正常</span>
        </div>
      </div>

      <!-- Stat Badges -->
      <div class="stats-card glass-panel">
        <div class="stat-item">
          <div class="stat-icon bg-purple-500/20 text-purple-400">
            <Sparkles class="w-5 h-5" />
          </div>
          <div>
            <div class="stat-num">{{ stats.aiQuestionsSolved }}</div>
            <div class="stat-label">AI 解析解答数</div>
          </div>
        </div>

        <div class="stat-item">
          <div class="stat-icon bg-green-500/20 text-green-400">
            <CheckCircle2 class="w-5 h-5" />
          </div>
          <div>
            <div class="stat-num">{{ stats.autoSubmitted }}</div>
            <div class="stat-label">自动提交数</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Real-time Terminal Log Feed (TaskLogger Style) -->
    <div class="terminal-container glass-panel">
      <div class="terminal-header">
        <div class="terminal-title">
          <Terminal class="w-4 h-4 text-green-400" />
          <span>实时日志控制台</span>
        </div>
        <div class="terminal-dots">
          <span class="dot-red"></span>
          <span class="dot-yellow"></span>
          <span class="dot-green"></span>
        </div>
      </div>

      <div class="terminal-body" id="terminal-body">
        <div 
          v-for="log in logs" 
          :key="log.id" 
          class="log-line"
        >
          <span class="log-time">[{{ log.time }}]</span>
          <span class="log-level" :class="log.level">[{{ log.level }}]</span>
          <span class="log-msg">{{ log.msg }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.execution-container {
  max-width: 1100px;
  margin: 0 auto;
}

.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(15, 23, 42, 0.6);
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.pulse-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--primary-green);
  box-shadow: 0 0 10px var(--primary-green);
  animation: pulse 1.5s infinite;
}
.pulse-dot.paused {
  background-color: var(--accent-amber);
  box-shadow: 0 0 10px var(--accent-amber);
  animation: none;
}

@keyframes pulse {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.7); }
  70% { transform: scale(1); box-shadow: 0 0 0 8px rgba(34, 197, 94, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(34, 197, 94, 0); }
}

.header-card {
  padding: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.badge-running {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #4ade80;
  background: rgba(34, 197, 94, 0.1);
  padding: 4px 10px;
  border-radius: 6px;
  margin-bottom: 8px;
}

.task-meta h2 {
  font-size: 22px;
  font-weight: 800;
  color: var(--text-main);
}

.task-meta p {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 4px;
}

.action-controls {
  display: flex;
  gap: 12px;
}

.dashboard-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

.progress-card {
  padding: 20px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.card-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: 12px;
}

.percentage-text {
  font-size: 24px;
  font-weight: 800;
  color: #4ade80;
  font-family: var(--font-mono);
}

.progress-bar-outer {
  width: 100%;
  height: 10px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 5px;
  overflow: hidden;
}

.progress-bar-inner {
  height: 100%;
  background: linear-gradient(90deg, #22c55e, #06b6d4);
  border-radius: 5px;
  transition: width 0.4s ease;
}

.progress-sub {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-dim);
  margin-top: 10px;
}

.stats-card {
  padding: 20px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-num {
  font-size: 20px;
  font-weight: 800;
  color: var(--text-main);
  font-family: var(--font-mono);
}

.stat-label {
  font-size: 11px;
  color: var(--text-muted);
}

/* Terminal Feed */
.terminal-container {
  overflow: hidden;
  border-color: rgba(255, 255, 255, 0.1);
}

.terminal-header {
  background: rgba(2, 6, 23, 0.9);
  padding: 12px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.terminal-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-family: var(--font-mono);
  color: var(--text-muted);
}

.terminal-dots {
  display: flex;
  gap: 6px;
}

.dot-red { width: 10px; height: 10px; border-radius: 50%; background: #ef4444; }
.dot-yellow { width: 10px; height: 10px; border-radius: 50%; background: #f59e0b; }
.dot-green { width: 10px; height: 10px; border-radius: 50%; background: #22c55e; }

.terminal-body {
  height: 260px;
  padding: 16px 20px;
  background: rgba(2, 6, 23, 0.95);
  font-family: var(--font-mono);
  font-size: 13px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.log-line {
  display: flex;
  gap: 10px;
  line-height: 1.5;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateX(-4px); }
  to { opacity: 1; transform: translateX(0); }
}

.log-time { color: var(--text-dim); }

.log-level.INFO { color: #38bdf8; }
.log-level.AI_SOLVER { color: #c084fc; font-weight: 600; }
.log-level.SUCCESS { color: #4ade80; font-weight: 600; }

.log-msg { color: #e2e8f0; }
</style>
