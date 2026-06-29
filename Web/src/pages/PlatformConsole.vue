<script setup>
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { 
  Globe, 
  Cloud,
  ShieldCheck, 
  Key, 
  User, 
  Lock, 
  BookOpen, 
  Play, 
  RefreshCw, 
  AlertCircle,
  Unlink,
  CheckCircle2,
  ExternalLink
} from 'lucide-vue-next'
import { getPlatformConfigApi, savePlatformConfigApi, deletePlatformConfigApi, getCourseListApi } from '../api/platform'
import { getQuestionConfigApi } from '../api/task'
import TaskConfigModal from '../components/TaskConfigModal.vue'

const route = useRoute()
const router = useRouter()

const platformCode = computed(() => route.params.code || 'chaoxing')
const platformName = computed(() => {
  if (platformCode.value === 'zhy') return '职教云'
  if (platformCode.value === 'chaoxing') return '超星学习通'
  return '网课平台'
})

const isTokenPlatform = computed(() => platformCode.value === 'zhy')
const iframeSrc = computed(() => {
  const callbackUrl = window.location.origin + '/oauth-callback.html'
  return `https://sso.icve.com.cn/sso/auth?mode=simple&source=2&redirect=${encodeURIComponent(callbackUrl)}`
})

// Binding & Form State
const isBound = ref(false)
const boundUsername = ref('')
const formUsername = ref('')
const formPassword = ref('')
const formToken = ref('')
const isBindingLoading = ref(false)
const bindError = ref('')
const bindSuccess = ref('')
const showEditForm = ref(false)

// Courses State
const courses = ref([])
const isCoursesLoading = ref(false)
const courseError = ref('')

// Task Launch State
const selectedCourse = ref(null)
const showTaskModal = ref(false)
const isAiConfigured = ref(false)

const checkPlatformStatusAndCourses = async () => {
  bindError.value = ''
  courseError.value = ''
  
  try {
    const qConf = await getQuestionConfigApi()
    if (qConf && qConf.aiKey) isAiConfigured.value = true
  } catch (e) {}

  try {
    const config = await getPlatformConfigApi(platformCode.value)
    if (config && config.status === 1 && (config.username || config.token)) {
      isBound.value = true
      boundUsername.value = config.username || 'Token已授权用户'
      formUsername.value = config.username || ''
      formToken.value = config.token || ''
      showEditForm.value = false
      loadCourses()
    } else {
      isBound.value = false
      showEditForm.value = true
    }
  } catch (e) {
    isBound.value = false
    showEditForm.value = true
  }
}

watch(platformCode, () => {
  checkPlatformStatusAndCourses()
})

const loadCourses = async () => {
  isCoursesLoading.value = true
  courseError.value = ''
  try {
    const res = await getCourseListApi(platformCode.value)
    if (Array.isArray(res)) {
      courses.value = res
    } else {
      courses.value = []
    }
  } catch (err) {
    console.error('Load courses failed:', err)
    courseError.value = err.message || '获取课程列表失败，请重试'
  } finally {
    isCoursesLoading.value = false
  }
}

const DEFAULT_COVER = 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?q=80&w=600&auto=format&fit=crop'

const formatCoverUrl = (url) => {
  if (!url) return DEFAULT_COVER
  if (url.startsWith('http://')) {
    return url.replace('http://', 'https://')
  }
  return url
}

const handleCoverError = (e) => {
  if (e && e.target && e.target.src !== DEFAULT_COVER) {
    e.target.src = DEFAULT_COVER
  }
}

const handleSaveBinding = async () => {
  isBindingLoading.value = true
  bindError.value = ''
  bindSuccess.value = ''

  try {
    if (isTokenPlatform.value) {
      if (!formToken.value) {
        throw new Error('请先在上方页面中登录以获取 Token，或手动粘贴 Token')
      }
      await savePlatformConfigApi({
        platformCode: platformCode.value,
        token: formToken.value
      })
    } else {
      if (!formUsername.value || !formPassword.value) {
        throw new Error('请输入平台账号和密码')
      }
      await savePlatformConfigApi({
        platformCode: platformCode.value,
        username: formUsername.value,
        password: formPassword.value
      })
    }
    
    bindSuccess.value = '账号验证并绑定成功！'
    isBound.value = true
    boundUsername.value = formUsername.value || 'Token已授权用户'
    showEditForm.value = false
    loadCourses()
  } catch (err) {
    bindError.value = err.message || '绑定失败，请稍后重试'
  } finally {
    isBindingLoading.value = false
  }
}

const handleOAuthMessage = async (event) => {
  if (event.data && event.data.type === 'ICVE_OAUTH_SUCCESS' && event.data.token) {
    const capturedToken = event.data.token
    formToken.value = capturedToken
    bindSuccess.value = '成功捕获到登录 Token，正在自动提交绑定...'
    
    try {
      await savePlatformConfigApi({
        platformCode: platformCode.value,
        token: capturedToken
      })
      bindSuccess.value = '授权 Token 绑定成功！'
      isBound.value = true
      showEditForm.value = false
      loadCourses()
    } catch (err) {
      bindError.value = err.message || 'Token 自动绑定失败，请手动点击保存'
    }
  }
}

const handleUnbind = async () => {
  if (!confirm('确认解除绑定该平台的账号/凭证吗？')) return
  try {
    await deletePlatformConfigApi(platformCode.value)
    isBound.value = false
    boundUsername.value = ''
    formUsername.value = ''
    formPassword.value = ''
    formToken.value = ''
    courses.value = []
    showEditForm.value = true
    bindSuccess.value = '解绑成功'
  } catch (err) {
    alert(err.message || '解绑失败')
  }
}

const handleOpenTask = (course) => {
  selectedCourse.value = course
  showTaskModal.value = true
}

const handleStartTask = (config) => {
  showTaskModal.value = false
  if (config && config.taskId) {
    router.push('/task/execution/' + config.taskId)
  } else {
    router.push('/tasks')
  }
}

const handleGoToAiSettings = () => {
  showTaskModal.value = false
  router.push('/settings')
}

onMounted(() => {
  window.addEventListener('message', handleOAuthMessage)
  checkPlatformStatusAndCourses()
})

onUnmounted(() => {
  window.removeEventListener('message', handleOAuthMessage)
})
</script>

<template>
  <div class="platform-console-page">
    <!-- Header Banner -->
    <div class="console-header glass-panel">
      <div class="header-info">
        <div class="platform-badge" :class="platformCode">
          <Cloud v-if="platformCode === 'zhy'" class="w-6 h-6 text-sky-400" />
          <Globe v-else class="w-6 h-6 text-emerald-400" />
        </div>
        <div>
          <h2>{{ platformName }} 控制台</h2>
          <p>管理在{{ platformName }}的绑定状态，同步并查看课程列表与创建自动化任务。</p>
        </div>
      </div>
    </div>

    <!-- Section 1: Platform Binding -->
    <div class="binding-section glass-panel">
      <div class="binding-header">
        <div class="section-title">
          <Key class="w-5 h-5 text-emerald-400" />
          <h3>{{ platformName }} 账号绑定管理</h3>
        </div>
        <span class="badge-status" :class="isBound ? 'badge-bound' : 'badge-unbound'">
          {{ isBound ? '账号已成功绑定' : (isTokenPlatform ? '等待 Token 授权绑定' : '尚未绑定账号密码') }}
        </span>
      </div>

      <!-- State A: Bound Summary View -->
      <div v-if="isBound && !showEditForm" class="bound-summary">
        <div class="user-chip">
          <User class="w-4 h-4 text-emerald-400" />
          <span>当前绑定凭证: <strong>{{ boundUsername }}</strong></span>
        </div>
        <div class="summary-actions">
          <button class="secondary-btn text-xs" @click="showEditForm = true">
            重新授权/绑定
          </button>
          <button class="secondary-btn text-xs text-rose-400 hover:bg-rose-500/20" @click="handleUnbind">
            <Unlink class="w-3.5 h-3.5" /> 解除绑定
          </button>
        </div>
      </div>

      <!-- State B: Binding Container -->
      <div v-if="showEditForm" class="binding-form-wrap">
        <div v-if="bindError" class="error-banner">
          <AlertCircle class="w-4 h-4 text-rose-400 flex-shrink-0" />
          <span>{{ bindError }}</span>
        </div>
        <div v-if="bindSuccess" class="success-banner">
          <CheckCircle2 class="w-4 h-4 text-emerald-400 flex-shrink-0" />
          <span>{{ bindSuccess }}</span>
        </div>

        <!-- Mode 1: Token / OAuth Iframe Mode (ZhiJiaoYun) -->
        <div v-if="isTokenPlatform" class="token-oauth-container">
          <div class="oauth-tip">
            <ShieldCheck class="w-5 h-5 text-sky-400 flex-shrink-0" />
            <div>
              <strong>OAuth 托管安全登录：</strong>
              <span>请在下方窗口中完成{{ platformName }}登录。登录成功后系统将自动捕获凭证并同步绑定。</span>
            </div>
          </div>

          <div class="iframe-wrapper">
            <iframe :src="iframeSrc" id="oauthFrame"></iframe>
          </div>

          <form @submit.prevent="handleSaveBinding" class="binding-form mt-4">
            <div class="form-group">
              <label>手动填入 / 捕获到的 Token</label>
              <div class="input-wrap">
                <Key class="input-icon w-4 h-4 text-slate-400" />
                <input 
                  type="text" 
                  v-model="formToken" 
                  placeholder="登录成功后自动填充，也可手动粘贴 Token" 
                  class="cyber-input font-mono" 
                  required 
                />
              </div>
            </div>

            <div class="form-actions">
              <button v-if="isBound" type="button" class="secondary-btn" @click="showEditForm = false">取消</button>
              <button type="submit" class="glow-btn" :disabled="isBindingLoading">
                <RefreshCw v-if="isBindingLoading" class="w-4 h-4 animate-spin" />
                <span>{{ isBindingLoading ? '正在验证 Token...' : '立即保存并绑定 Token' }}</span>
              </button>
            </div>
          </form>
        </div>

        <!-- Mode 2: Username / Password Mode (ChaoXing) -->
        <form v-else @submit.prevent="handleSaveBinding" class="binding-form">
          <div class="form-row">
            <div class="form-group flex-1">
              <label>{{ platformName }} 登录账号 (手机号/学号/邮箱)</label>
              <div class="input-wrap">
                <User class="input-icon w-4 h-4" />
                <input 
                  type="text" 
                  v-model="formUsername" 
                  placeholder="请输入平台账号" 
                  class="cyber-input" 
                  required 
                />
              </div>
            </div>

            <div class="form-group flex-1">
              <label>{{ platformName }} 登录密码</label>
              <div class="input-wrap">
                <Lock class="input-icon w-4 h-4" />
                <input 
                  type="password" 
                  v-model="formPassword" 
                  placeholder="请输入平台密码" 
                  class="cyber-input" 
                  required 
                />
              </div>
            </div>
          </div>

          <div class="form-actions">
            <button v-if="isBound" type="button" class="secondary-btn" @click="showEditForm = false">取消</button>
            <button type="submit" class="glow-btn" :disabled="isBindingLoading">
              <RefreshCw v-if="isBindingLoading" class="w-4 h-4 animate-spin" />
              <span>{{ isBindingLoading ? '正在验证并绑定...' : (isBound ? '保存更新账号' : '立即验证并绑定账号') }}</span>
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Section 2: Course List Grid -->
    <div class="courses-section">
      <div class="courses-header">
        <div>
          <h3>同步的课程列表</h3>
          <p>鼠标悬浮至课程卡片上可立即创建刷课任务</p>
        </div>
        <button v-if="isBound" class="secondary-btn text-xs" @click="loadCourses" :disabled="isCoursesLoading">
          <RefreshCw class="w-3.5 h-3.5" :class="{ 'animate-spin': isCoursesLoading }" />
          <span>刷新课程列表</span>
        </button>
      </div>

      <!-- Case 1: Unbound state prompt -->
      <div v-if="!isBound" class="empty-box glass-panel">
        <BookOpen class="w-12 h-12 text-slate-600 mb-2" />
        <h4>暂无法加载课程</h4>
        <p>请先在上方完成 {{ platformName }} 平台账号凭证的绑定。</p>
      </div>

      <!-- Case 2: Loading -->
      <div v-else-if="isCoursesLoading" class="empty-box glass-panel">
        <RefreshCw class="w-8 h-8 text-emerald-400 animate-spin mb-2" />
        <p>正在同步 {{ platformName }} 课程数据...</p>
      </div>

      <!-- Case 3: Error -->
      <div v-else-if="courseError" class="empty-box glass-panel">
        <AlertCircle class="w-10 h-10 text-rose-400 mb-2" />
        <h4>加载课程列表失败</h4>
        <p>{{ courseError }}</p>
        <button class="secondary-btn text-xs mt-3" @click="loadCourses">重试获取</button>
      </div>

      <!-- Case 4: Course Cards Grid -->
      <div v-else-if="courses.length > 0" class="course-cards-grid">
        <div v-for="course in courses" :key="course.courseId || course.id" class="course-card glass-card">
          <div class="cover-wrap">
            <img 
              :src="formatCoverUrl(course.coverUrl || course.cover)" 
              alt="course cover" 
              class="cover-img" 
              referrerpolicy="no-referrer"
              @error="handleCoverError" 
            />
            <div class="cover-overlay">
              <button class="glow-btn text-xs hover:scale-105 transition-transform" @click="handleOpenTask(course)">
                <Play class="w-3.5 h-3.5 fill-current" /> 创建刷课任务
              </button>
            </div>
          </div>
          <div class="card-info">
            <h4 class="course-name">{{ course.name }}</h4>
            <div class="course-meta">
              <span class="teacher">教师: {{ course.teacher || '未知教师' }}</span>
              <span class="class-id" v-if="course.classId">班级: {{ course.classId }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Case 5: Empty Array -->
      <div v-else class="empty-box glass-panel">
        <BookOpen class="w-10 h-10 text-slate-500 mb-2" />
        <h4>未检测到进行中的课程</h4>
        <p>请确认该账号下是否有正在进行的网络课程。</p>
      </div>
    </div>

    <!-- Task Launch Modal -->
    <TaskConfigModal 
      v-if="showTaskModal"
      :course="selectedCourse"
      :isAiConfigured="isAiConfigured"
      :platformCode="platformCode"
      @close="showTaskModal = false"
      @startTask="handleStartTask"
      @goToAiSettings="handleGoToAiSettings"
    />
  </div>
</template>

<style scoped>
.platform-console-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.console-header {
  padding: 28px 32px;
}

.header-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.platform-badge {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.platform-badge.chaoxing { background: rgba(225, 29, 72, 0.15); border: 1px solid rgba(225, 29, 72, 0.3); }
.platform-badge.zhy { background: rgba(14, 165, 233, 0.15); border: 1px solid rgba(14, 165, 233, 0.3); }

.header-info h2 {
  font-size: 22px;
  font-weight: 800;
  color: var(--text-main);
}
.header-info p {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 4px;
}

.binding-section {
  padding: 24px 28px;
  border-color: rgba(34, 197, 94, 0.2);
}

.binding-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
}
.section-title h3 {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-main);
}

.bound-summary {
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(2, 6, 23, 0.5);
  padding: 14px 20px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: var(--text-main);
}

.summary-actions {
  display: flex;
  gap: 10px;
}

.binding-form-wrap {
  margin-top: 16px;
}

.token-oauth-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.oauth-tip {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  background: rgba(14, 165, 233, 0.1);
  border: 1px solid rgba(14, 165, 233, 0.25);
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 13px;
  color: #7dd3fc;
}

.iframe-wrapper {
  width: 100%;
  height: 480px;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.15);
  background: #ffffff;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1);
}

iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.binding-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-row {
  display: flex;
  gap: 16px;
}
.flex-1 { flex: 1; }

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.form-group label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
}

.input-wrap {
  position: relative;
}
.input-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-dim);
}
.cyber-input { padding-left: 40px; }

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}

.error-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: rgba(239, 68, 68, 0.15);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 10px;
  color: #fca5a5;
  font-size: 13px;
  margin-bottom: 16px;
}

.success-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: rgba(34, 197, 94, 0.15);
  border: 1px solid rgba(34, 197, 94, 0.3);
  border-radius: 10px;
  color: #86efac;
  font-size: 13px;
  margin-bottom: 16px;
}

.courses-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.courses-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.courses-header h3 {
  font-size: 18px;
  font-weight: 700;
}
.courses-header p {
  font-size: 13px;
  color: var(--text-muted);
}

.empty-box {
  padding: 50px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.empty-box h4 {
  font-size: 16px;
  font-weight: 700;
  margin-top: 4px;
}
.empty-box p {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 4px;
}

.course-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 20px;
}

.course-card {
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.cover-wrap {
  height: 140px;
  position: relative;
  overflow: hidden;
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.course-card:hover .cover-img {
  transform: scale(1.05);
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background: rgba(2, 6, 23, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.25s ease;
}

.course-card:hover .cover-overlay {
  opacity: 1;
}

.card-info {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.course-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-main);
  line-height: 1.4;
  height: 42px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.course-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-muted);
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  padding-top: 8px;
}
</style>
