<script setup>
import { ref, onMounted } from 'vue'
import { 
  Sparkles, 
  Key, 
  Globe, 
  Cpu, 
  Save, 
  CheckCircle2, 
  AlertCircle, 
  RefreshCw,
  Search
} from 'lucide-vue-next'
import { getQuestionConfigApi, saveQuestionConfigApi } from '../api/task'

const aiConfig = ref({
  baseUrl: 'https://api.openai.com/v1',
  key: '',
  model: 'gpt-4o-mini'
})

const isLoading = ref(false)
const isSaving = ref(false)
const isFetchingModels = ref(false)
const availableModels = ref([])
const modelFetchHint = ref('')
const errorMessage = ref('')
const successMessage = ref('')

const loadSettings = async () => {
  isLoading.value = true
  errorMessage.value = ''
  try {
    const config = await getQuestionConfigApi()
    if (config) {
      if (config.aiBaseUrl) aiConfig.value.baseUrl = config.aiBaseUrl
      if (config.aiKey) aiConfig.value.key = config.aiKey
      if (config.aiModel) aiConfig.value.model = config.aiModel

      if (config.configJson) {
        try {
          const parsed = JSON.parse(config.configJson)
          if (parsed.AI) {
            if (parsed.AI.baseUrl) aiConfig.value.baseUrl = parsed.AI.baseUrl
            else if (parsed.AI.endpoint) aiConfig.value.baseUrl = parsed.AI.endpoint
            if (parsed.AI.key) aiConfig.value.key = parsed.AI.key
            if (parsed.AI.model) aiConfig.value.model = parsed.AI.model
          }
        } catch (e) {}
      }

      if (aiConfig.value.baseUrl && aiConfig.value.key) {
        autoFetchModels()
      }
    }
  } catch (err) {
    console.warn('Load settings warning:', err)
  } finally {
    isLoading.value = false
  }
}

const autoFetchModels = async () => {
  if (!aiConfig.value.baseUrl || !aiConfig.value.key) return
  isFetchingModels.value = true
  modelFetchHint.value = ''

  let cleanUrl = aiConfig.value.baseUrl.trim()
  if (cleanUrl.endsWith('/')) cleanUrl = cleanUrl.slice(0, -1)
  if (cleanUrl.endsWith('/chat/completions')) cleanUrl = cleanUrl.slice(0, -'/chat/completions'.length)

  const candidates = [
    cleanUrl.endsWith('/models') ? cleanUrl : `${cleanUrl}/models`,
    cleanUrl.endsWith('/v1') ? `${cleanUrl}/models` : `${cleanUrl}/v1/models`
  ]

  let fetchedList = []
  for (const url of candidates) {
    try {
      const resp = await fetch(url, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${aiConfig.value.key.trim()}`
        }
      })
      if (resp.ok) {
        const json = await resp.json()
        if (json && Array.isArray(json.data)) {
          fetchedList = json.data.map(item => item.id).filter(Boolean)
          if (fetchedList.length > 0) break
        }
      }
    } catch (e) {
      console.warn('Frontend direct fetch error:', e)
    }
  }

  if (fetchedList.length > 0) {
    availableModels.value = Array.from(new Set(fetchedList))
    modelFetchHint.value = `前端已检测到 ${availableModels.value.length} 个可用模型`
  } else {
    availableModels.value = []
    modelFetchHint.value = '未读取到模型，请手动输入 Model 名称'
  }
  isFetchingModels.value = false
}

const selectModel = (mod) => {
  aiConfig.value.model = mod
}

const handleSave = async () => {
  if (!aiConfig.value.key) {
    errorMessage.value = '请输入有效的 API Key'
    return
  }

  isSaving.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    const jsonPayload = { AI: aiConfig.value }
    await saveQuestionConfigApi({
      provider: 'AI',
      aiBaseUrl: aiConfig.value.baseUrl,
      aiKey: aiConfig.value.key,
      aiModel: aiConfig.value.model,
      configJson: JSON.stringify(jsonPayload)
    })
    successMessage.value = '配置已就绪'
  } catch (err) {
    errorMessage.value = err.message || '保存失败'
  } finally {
    isSaving.value = false
  }
}

onMounted(() => {
  loadSettings()
})
</script>

<template>
  <div class="settings-container">
    <!-- Header -->
    <div class="header">
      <div class="header-title">
        <Sparkles class="icon-sparkle" />
        <h2>AI 模型与解析设置</h2>
      </div>
    </div>

    <!-- Main Form Card -->
    <div class="cyber-card glass-panel">
      <div v-if="errorMessage" class="banner error">
        <AlertCircle class="w-4 h-4 shrink-0" />
        <span>{{ errorMessage }}</span>
      </div>

      <div v-if="successMessage" class="banner success">
        <CheckCircle2 class="w-4 h-4 shrink-0" />
        <span>{{ successMessage }}</span>
      </div>

      <div v-if="isLoading" class="loading-state">
        <RefreshCw class="w-6 h-6 animate-spin text-purple-400" />
      </div>

      <form v-else @submit.prevent="handleSave" class="form-grid">
        <div class="field">
          <label>API Base URL</label>
          <div class="input-box">
            <Globe class="field-icon" />
            <input 
              type="text" 
              v-model="aiConfig.baseUrl" 
              placeholder="https://api.openai.com/v1" 
              @blur="autoFetchModels"
              required 
            />
          </div>
        </div>

        <div class="field">
          <label>API Key</label>
          <div class="input-box">
            <Key class="field-icon" />
            <input 
              type="password" 
              v-model="aiConfig.key" 
              placeholder="sk-..." 
              @blur="autoFetchModels"
              required 
            />
          </div>
        </div>

        <div class="field">
          <div class="field-label-row">
            <label>Model</label>
            <button 
              type="button" 
              class="fetch-btn" 
              @click="autoFetchModels" 
              :disabled="isFetchingModels || !aiConfig.baseUrl || !aiConfig.key"
            >
              <RefreshCw v-if="isFetchingModels" class="w-3 h-3 animate-spin inline mr-1" />
              <Search v-else class="w-3 h-3 inline mr-1" />
              <span>{{ isFetchingModels ? '读取中...' : '前端直连读取模型' }}</span>
            </button>
          </div>
          <div class="input-box">
            <Cpu class="field-icon" />
            <input 
              type="text" 
              v-model="aiConfig.model" 
              placeholder="gpt-4o-mini / deepseek-chat" 
              required 
            />
          </div>

          <!-- Auto detected models list chips -->
          <div v-if="modelFetchHint || availableModels.length > 0" class="models-panel">
            <span class="hint-text">{{ modelFetchHint }}</span>
            <div v-if="availableModels.length > 0" class="model-chips">
              <span 
                v-for="mod in availableModels.slice(0, 12)" 
                :key="mod" 
                class="chip"
                :class="{ active: aiConfig.model === mod }"
                @click="selectModel(mod)"
              >
                {{ mod }}
              </span>
              <span v-if="availableModels.length > 12" class="chip-more">
                +{{ availableModels.length - 12 }} 更多
              </span>
            </div>
          </div>
        </div>

        <div class="action-bar">
          <button type="submit" class="submit-btn" :disabled="isSaving">
            <RefreshCw v-if="isSaving" class="w-4 h-4 animate-spin" />
            <Save v-else class="w-4 h-4" />
            <span>{{ isSaving ? '保存中' : '保存设置' }}</span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.settings-container {
  max-width: 680px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 4px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.icon-sparkle {
  width: 22px;
  height: 22px;
  color: #a855f7;
  filter: drop-shadow(0 0 8px rgba(168, 85, 247, 0.6));
}

.header h2 {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: #f8fafc;
}

.status-badge {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.1em;
  padding: 3px 8px;
  border-radius: 20px;
  background: rgba(34, 197, 94, 0.1);
  color: #4ade80;
  border: 1px solid rgba(34, 197, 94, 0.25);
  box-shadow: 0 0 10px rgba(34, 197, 94, 0.15);
}

.cyber-card {
  padding: 32px;
  border-radius: 18px;
  background: rgba(15, 23, 42, 0.55);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4);
}

.loading-state {
  display: flex;
  justify-content: center;
  padding: 40px 0;
}

.form-grid {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.field label {
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #94a3b8;
}

.fetch-btn {
  background: transparent;
  border: none;
  color: #c084fc;
  font-size: 11px;
  cursor: pointer;
  display: flex;
  align-items: center;
  transition: color 0.2s;
}

.fetch-btn:hover:not(:disabled) {
  color: #e9d5ff;
}

.fetch-btn:disabled {
  color: #64748b;
  cursor: not-allowed;
}

.input-box {
  position: relative;
  display: flex;
  align-items: center;
}

.field-icon {
  position: absolute;
  left: 14px;
  width: 16px;
  height: 16px;
  color: #64748b;
  transition: color 0.2s ease;
}

.input-box input {
  width: 100%;
  padding: 12px 14px 12px 42px;
  border-radius: 10px;
  background: rgba(2, 6, 23, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: #f8fafc;
  font-size: 14px;
  outline: none;
  transition: all 0.2s ease;
}

.input-box input:focus {
  border-color: #a855f7;
  box-shadow: 0 0 16px rgba(168, 85, 247, 0.2);
}

.input-box input:focus + .field-icon,
.input-box:focus-within .field-icon {
  color: #c084fc;
}

.models-panel {
  margin-top: 6px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hint-text {
  font-size: 11px;
  color: #a855f7;
}

.model-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.chip {
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 6px;
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #94a3b8;
  cursor: pointer;
  transition: all 0.15s ease;
}

.chip:hover {
  border-color: #a855f7;
  color: #f8fafc;
}

.chip.active {
  background: rgba(168, 85, 247, 0.2);
  border-color: #a855f7;
  color: #c084fc;
  font-weight: 600;
}

.chip-more {
  font-size: 11px;
  color: #64748b;
  padding: 3px 6px;
}

.action-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.submit-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 28px;
  border-radius: 10px;
  background: linear-gradient(135deg, #a855f7 0%, #7c3aed 100%);
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  box-shadow: 0 4px 20px rgba(168, 85, 247, 0.35);
  transition: all 0.2s ease;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 24px rgba(168, 85, 247, 0.5);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 10px;
  font-size: 13px;
  margin-bottom: 20px;
}

.banner.error {
  background: rgba(239, 68, 68, 0.12);
  border: 1px solid rgba(239, 68, 68, 0.25);
  color: #fca5a5;
}

.banner.success {
  background: rgba(34, 197, 94, 0.12);
  border: 1px solid rgba(34, 197, 94, 0.25);
  color: #86efac;
}
</style>
