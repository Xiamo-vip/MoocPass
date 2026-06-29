<script setup>
import { ref } from 'vue'
import { 
  X, 
  Sparkles, 
  Gauge, 
  CheckSquare, 
  Play, 
  AlertCircle, 
  Settings,
  Loader2,
  FileCheck,
  Percent
} from 'lucide-vue-next'
import { startTaskApi } from '../api/task'

const props = defineProps({
  course: { type: Object, required: true },
  isAiConfigured: { type: Boolean, required: true },
  platformCode: { type: String, default: 'chaoxing' }
})

const emit = defineEmits(['close', 'startTask', 'goToAiSettings'])

const solverMode = ref('ai')
const playbackSpeed = ref(2.0)
const autoAnswer = ref(true)
const submitAnswer = ref(true)
const coverRate = ref(0.8)

const showAiConfigWarning = ref(false)
const isLoading = ref(false)

const handleStart = async () => {
  if (solverMode.value === 'ai' && !props.isAiConfigured) {
    showAiConfigWarning.value = true
    return
  }
  
  isLoading.value = true
  let taskResult = null
  try {
    taskResult = await startTaskApi({
      platformCode: props.platformCode,
      courseId: props.course.courseId || props.course.id,
      classId: props.course.classId || 'CLASS-101',
      courseName: props.course.name,
      coverUrl: props.course.coverUrl || props.course.cover,
      teacher: props.course.teacher,
      speed: playbackSpeed.value,
      questionProvider: solverMode.value.toUpperCase(),
      autoAnswer: autoAnswer.value,
      submitAnswer: submitAnswer.value,
      coverRate: coverRate.value
    })
  } catch (e) {
    console.warn('Backend start task warning, launching task execution stream:', e)
  } finally {
    isLoading.value = false
  }

  emit('startTask', {
    taskId: taskResult ? taskResult.id : Date.now(),
    courseId: props.course.courseId || props.course.id,
    courseName: props.course.name,
    solverMode: solverMode.value,
    speed: playbackSpeed.value,
    autoAnswer: autoAnswer.value,
    submitAnswer: submitAnswer.value,
    coverRate: coverRate.value
  })
}
</script>

<template>
  <div class="modal-overlay">
    <div class="modal-card glass-panel">
      <!-- Modal Header -->
      <div class="modal-header">
        <div>
          <h3>任务参数与自动化配置</h3>
          <p class="course-title">{{ course.name }}</p>
        </div>
        <button class="close-btn" @click="emit('close')">
          <X class="w-5 h-5" />
        </button>
      </div>

      <!-- Warning view if AI not configured -->
      <div v-if="showAiConfigWarning" class="warning-box">
        <div class="warning-icon">
          <AlertCircle class="w-6 h-6 text-amber-400" />
        </div>
        <div class="warning-content">
          <h4>未检测到 AI 题库配置信息！</h4>
          <p>您开启了 AI 答题模式，但尚未在系统设置中配置 API Key 与 Base URL。</p>
        </div>
        <div class="warning-actions">
          <button class="secondary-btn text-xs" @click="showAiConfigWarning = false">返回修改</button>
          <button class="glow-btn text-xs" @click="emit('goToAiSettings')">
            <Settings class="w-3.5 h-3.5" /> 前往配置 AI 密钥
          </button>
        </div>
      </div>

      <!-- Normal form -->
      <div v-else class="modal-body">
        <!-- Execution options -->
        <div class="section">
          <label class="section-title">任务执行配置</label>
          <div class="options-group">
            <!-- 播放倍速 -->
            <div class="option-row">
              <div class="option-info">
                <Gauge class="w-4 h-4 text-green-400" />
                <span>视频播放倍速</span>
              </div>
              <select v-model="playbackSpeed" class="cyber-input select-mini">
                <option :value="1.0">1.0x</option>
                <option :value="1.5">1.5x</option>
                <option :value="2.0">2.0x</option>
              </select>
            </div>

            <!-- 是否答题 -->
            <div class="option-row">
              <div class="option-info">
                <CheckSquare class="w-4 h-4 text-purple-400" />
                <span>自动答题</span>
              </div>
              <input type="checkbox" v-model="autoAnswer" class="toggle-checkbox" />
            </div>

            <!-- 答题模式：保存 vs 提交 -->
            <div v-if="autoAnswer" class="option-row">
              <div class="option-info">
                <FileCheck class="w-4 h-4 text-cyan-400" />
                <span>答题处理动作</span>
              </div>
              <div class="radio-group">
                <label :class="{ active: submitAnswer }" @click="submitAnswer = true">
                  自动提交
                </label>
                <label :class="{ active: !submitAnswer }" @click="submitAnswer = false">
                  仅保存不提交
                </label>
              </div>
            </div>

            <!-- 最小答题覆盖率 -->
            <div v-if="autoAnswer && submitAnswer" class="option-row cover-rate-row">
              <div class="option-info">
                <Percent class="w-4 h-4 text-amber-400" />
                <span>最小答题覆盖率阈值</span>
              </div>
              <div class="input-rate-wrap">
                <input 
                  type="number" 
                  step="0.05" 
                  min="0.1" 
                  max="1.0" 
                  v-model.number="coverRate" 
                  class="cyber-input rate-input"
                />
                <span class="rate-percentage">({{ Math.round(coverRate * 100) }}%)</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Submit CTA -->
        <div class="modal-footer">
          <button class="secondary-btn" @click="emit('close')">取消</button>
          <button class="glow-btn" @click="handleStart" :disabled="isLoading">
            <Loader2 v-if="isLoading" class="w-4 h-4 animate-spin" />
            <Play v-else class="w-4 h-4 fill-current" /> 
            <span>{{ isLoading ? '正在启动...' : '启动任务' }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(2, 6, 23, 0.85);
  backdrop-filter: blur(10px);
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.modal-card {
  width: 100%;
  max-width: 540px;
  padding: 28px;
  border-color: rgba(255, 255, 255, 0.12);
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.7);
  animation: modalIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes modalIn {
  from { opacity: 0; transform: scale(0.95) translateY(10px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  margin-bottom: 20px;
}

.modal-header h3 {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-main);
}

.course-title {
  font-size: 12px;
  color: var(--primary-green);
  margin-top: 2px;
}

.close-btn {
  background: transparent;
  border: none;
  color: var(--text-dim);
  cursor: pointer;
}
.close-btn:hover { color: var(--text-main); }

.section {
  margin-bottom: 20px;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-muted);
  margin-bottom: 10px;
  display: block;
}

.solver-card {
  padding: 16px;
  border-radius: 12px;
  background: rgba(139, 92, 246, 0.12);
  border: 1px solid #8b5cf6;
  box-shadow: 0 0 15px rgba(139, 92, 246, 0.25);
}

.solver-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.badge-recommended {
  font-size: 10px;
  background: rgba(139, 92, 246, 0.2);
  color: #c084fc;
  padding: 2px 6px;
  border-radius: 4px;
}

.solver-card h4 {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-main);
}

.solver-card p {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 4px;
  line-height: 1.4;
}

.options-group {
  display: flex;
  flex-direction: column;
  gap: 14px;
  background: rgba(2, 6, 23, 0.5);
  padding: 18px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.option-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.option-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-main);
}

.select-mini {
  width: 130px;
  padding: 6px 12px;
  font-size: 12px;
}

.toggle-checkbox {
  width: 18px;
  height: 18px;
  accent-color: var(--primary-green);
  cursor: pointer;
}

.radio-group {
  display: flex;
  gap: 6px;
  background: rgba(15, 23, 42, 0.6);
  padding: 3px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.radio-group label {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 6px;
  cursor: pointer;
  color: var(--text-muted);
  transition: all 0.2s ease;
}

.radio-group label.active {
  background: #8b5cf6;
  color: #ffffff;
  font-weight: 600;
}

.input-rate-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rate-input {
  width: 70px;
  padding: 4px 8px;
  font-size: 12px;
  text-align: center;
}

.rate-percentage {
  font-size: 12px;
  color: var(--text-dim);
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.warning-box {
  background: rgba(245, 158, 11, 0.1);
  border: 1px solid rgba(245, 158, 11, 0.3);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.warning-icon {
  display: flex;
  align-items: center;
  gap: 8px;
}

.warning-content h4 {
  font-size: 15px;
  font-weight: 700;
  color: #fbbf24;
}

.warning-content p {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 4px;
}

.warning-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
