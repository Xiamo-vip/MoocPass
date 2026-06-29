<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { 
  CheckSquare, 
  RefreshCw, 
  Play, 
  Square, 
  Trash2, 
  Terminal, 
  Activity, 
  AlertCircle,
  Clock,
  BookOpen
} from 'lucide-vue-next'
import { getTaskListApi, getTaskLogsApi, stopTaskApi, deleteTaskApi } from '../api/task'

const router = useRouter()
const tasks = ref([])
const isLoading = ref(true)
const errorMessage = ref('')

const navigateToExecution = (taskId) => {
  router.push('/task/execution/' + taskId)
}

// Selected Log Modal State
const activeLogTask = ref(null)
const taskLogs = ref([])
const isLogsLoading = ref(false)
const showLogModal = ref(false)

const loadTasks = async () => {
  isLoading.value = true
  errorMessage.value = ''
  try {
    const res = await getTaskListApi()
    if (Array.isArray(res)) {
      tasks.value = res
    } else {
      tasks.value = []
    }
  } catch (err) {
    console.error('Load tasks failed:', err)
    errorMessage.value = err.message || '加载刷课任务列表失败'
  } finally {
    isLoading.value = false
  }
}

const handleStopTask = async (taskId) => {
  if (!confirm('确定要停止该刷课任务吗？')) return
  try {
    await stopTaskApi(taskId)
    loadTasks()
  } catch (err) {
    alert(err.message || '停止任务失败')
  }
}

const handleDeleteTask = async (taskId) => {
  if (!confirm('确定要删除这条任务记录吗？')) return
  try {
    await deleteTaskApi(taskId)
    loadTasks()
  } catch (err) {
    alert(err.message || '删除任务记录失败')
  }
}

const handleViewLogs = async (task) => {
  activeLogTask.value = task
  showLogModal.value = true
  isLogsLoading.value = true
  try {
    const logs = await getTaskLogsApi(task.id)
    if (Array.isArray(logs)) {
      taskLogs.value = logs
    } else {
      taskLogs.value = []
    }
  } catch (err) {
    taskLogs.value = ['无法加载该任务的日志: ' + err.message]
  } finally {
    isLogsLoading.value = false
  }
}

onMounted(() => {
  loadTasks()
})
</script>

<template>
  <div class="tasks-page">
    <div class="tasks-header-card glass-panel">
      <div class="header-left">
        <div class="header-icon">
          <CheckSquare class="w-6 h-6 text-green-400" />
        </div>
        <div>
          <h2>任务管理</h2>
          <p>实时监控与管理任务状态与运行日志。</p>
        </div>
      </div>
      <button class="secondary-btn text-xs" @click="loadTasks" :disabled="isLoading">
        <RefreshCw class="w-3.5 h-3.5" :class="{ 'animate-spin': isLoading }" />
        <span>刷新列表</span>
      </button>
    </div>

    <!-- Error State -->
    <div v-if="errorMessage" class="error-banner">
      <AlertCircle class="w-4 h-4 text-red-400 flex-shrink-0" />
      <span>{{ errorMessage }}</span>
    </div>

    <!-- Loading State -->
    <div v-if="isLoading" class="empty-box glass-panel">
      <RefreshCw class="w-8 h-8 text-green-400 animate-spin mb-2" />
      <p>正在加载任务列表...</p>
    </div>

    <!-- Tasks List Table / Cards -->
    <div v-else-if="tasks.length > 0" class="tasks-list">
      <div v-for="task in tasks" :key="task.id" class="task-item-card glass-card">
        <div class="task-top-row">
          <div class="task-title-wrap">
            <span class="platform-chip">{{ task.platformCode === 'chaoxing' ? '超星学习通' : task.platformCode }}</span>
            <h4>{{ task.courseName }}</h4>
          </div>

          <div class="task-status-wrap">
            <span v-if="task.status === 'RUNNING'" class="status-badge running">
              <Activity class="w-3.5 h-3.5 animate-pulse" /> 进行中
            </span>
            <span v-else-if="task.status === 'COMPLETED'" class="status-badge completed">
              已完成
            </span>
            <span v-else-if="task.status === 'STOPPED'" class="status-badge stopped">
              已停止
            </span>
            <span v-else class="status-badge failed">
              已失败
            </span>
          </div>
        </div>

        <div class="task-detail-row">
          <div class="meta-col">
            <span class="meta-item"><Clock class="w-3.5 h-3.5 text-slate-400" /> 创建时间: {{ task.createTime }}</span>
            <span class="meta-item"><BookOpen class="w-3.5 h-3.5 text-slate-400" /> 课程ID: {{ task.courseId }}</span>
            <span class="meta-item">播放倍速: {{ task.speed }}x</span>
          </div>

          <div class="progress-col">
            <div class="progress-bar-wrap">
              <div class="progress-fill" :style="{ width: (task.progress || 0) + '%' }"></div>
            </div>
            <span class="progress-num">{{ task.progress || 0 }}%</span>
          </div>
        </div>

        <div class="task-actions-row">
          <button class="glow-btn text-xs cursor-pointer flex items-center gap-1.5" @click="navigateToExecution(task.id)">
            <Terminal class="w-3.5 h-3.5" /> 查看实时状态
          </button>

          <div class="right-actions">
            <button v-if="task.status === 'RUNNING'" class="secondary-btn text-xs text-amber-400 cursor-pointer" @click="handleStopTask(task.id)">
              <Square class="w-3.5 h-3.5 fill-current" /> 停止任务
            </button>
            <button class="secondary-btn text-xs text-red-400 hover:bg-red-500/20 cursor-pointer" @click="handleDeleteTask(task.id)">
              <Trash2 class="w-3.5 h-3.5" /> 删除记录
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="empty-box glass-panel">
      <CheckSquare class="w-12 h-12 text-slate-600 mb-2" />
      <h4>暂无任务记录</h4>
      <p>前往平台控制台选择课程创建任务。</p>
    </div>

    <!-- Logs Modal -->
    <div v-if="showLogModal" class="modal-overlay" @click.self="showLogModal = false">
      <div class="log-modal-card glass-panel">
        <div class="log-modal-header">
          <div>
            <h3>任务实时通信日志</h3>
            <p v-if="activeLogTask" class="text-xs text-green-400 mt-1">{{ activeLogTask.courseName }} (ID: {{ activeLogTask.id }})</p>
          </div>
          <button class="secondary-btn text-xs" @click="showLogModal = false">关闭</button>
        </div>

        <div class="log-modal-body">
          <div v-if="isLogsLoading" class="text-center py-8 text-slate-400">
            <RefreshCw class="w-6 h-6 animate-spin inline mr-2 text-purple-400" /> 日志拉取中...
          </div>
          <div v-else-if="taskLogs.length === 0" class="text-center py-8 text-slate-500">
            暂无日志记录
          </div>
          <div v-else class="log-list">
            <div v-for="(log, idx) in taskLogs" :key="idx" class="log-item">
              <span class="log-index">#{{ idx + 1 }}</span>
              <span class="log-content">{{ log }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.tasks-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.tasks-header-card {
  padding: 24px 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: rgba(34, 197, 94, 0.15);
  border: 1px solid rgba(34, 197, 94, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-left h2 {
  font-size: 22px;
  font-weight: 800;
  color: var(--text-main);
}
.header-left p {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 4px;
}

.empty-box {
  padding: 60px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.empty-box h4 { font-size: 16px; font-weight: 700; margin-top: 6px; }
.empty-box p { font-size: 13px; color: var(--text-muted); margin-top: 4px; }

.tasks-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.task-item-card {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.task-top-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.task-title-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
}

.platform-chip {
  font-size: 11px;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 6px;
  background: rgba(225, 29, 72, 0.15);
  color: #fb7185;
  border: 1px solid rgba(225, 29, 72, 0.3);
}

.task-title-wrap h4 {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-main);
}

.status-badge {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 20px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.status-badge.running { background: rgba(34, 197, 94, 0.15); color: #4ade80; border: 1px solid rgba(34, 197, 94, 0.3); }
.status-badge.completed { background: rgba(6, 182, 212, 0.15); color: #22d3ee; border: 1px solid rgba(6, 182, 212, 0.3); }
.status-badge.stopped { background: rgba(245, 158, 11, 0.15); color: #fbbf24; border: 1px solid rgba(245, 158, 11, 0.3); }
.status-badge.failed { background: rgba(239, 68, 68, 0.15); color: #fca5a5; border: 1px solid rgba(239, 68, 68, 0.3); }

.task-detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(2, 6, 23, 0.4);
  padding: 12px 16px;
  border-radius: 10px;
}

.meta-col {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 12px;
  color: var(--text-muted);
}
.meta-item { display: inline-flex; align-items: center; gap: 4px; }

.progress-col {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 200px;
}

.progress-bar-wrap {
  flex: 1;
  height: 8px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #22c55e, #06b6d4);
}

.progress-num {
  font-size: 12px;
  font-weight: 700;
  font-family: var(--font-mono);
  color: #4ade80;
  width: 36px;
}

.task-actions-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.right-actions {
  display: flex;
  gap: 10px;
}

/* Modal */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(2, 6, 23, 0.85);
  backdrop-filter: blur(10px);
  z-index: 300;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.log-modal-card {
  width: 100%;
  max-width: 680px;
  padding: 24px;
}

.log-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.log-modal-header h3 { font-size: 18px; font-weight: 700; }

.log-modal-body {
  margin-top: 16px;
  max-height: 400px;
  overflow-y: auto;
  background: rgba(2, 6, 23, 0.9);
  padding: 16px;
  border-radius: 10px;
  font-family: var(--font-mono);
  font-size: 13px;
}

.log-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.log-item {
  display: flex;
  gap: 10px;
  line-height: 1.5;
  color: #cbd5e1;
}

.log-index { color: var(--text-dim); }
.log-content { word-break: break-all; }
</style>
