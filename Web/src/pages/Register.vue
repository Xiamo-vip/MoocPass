<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Zap, User, Lock, Smile, Mail, ArrowRight, AlertCircle, CheckCircle } from 'lucide-vue-next'
import { registerApi } from '../api/user'

const router = useRouter()
const username = ref('')
const nickname = ref('')
const email = ref('')
const password = ref('')

const isLoading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const handleRegister = async () => {
  if (!username.value || !password.value) return
  isLoading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    await registerApi({
      username: username.value,
      nickname: nickname.value || username.value,
      email: email.value,
      password: password.value
    })
    
    successMessage.value = '注册成功！正在跳转至登录页面...'
    setTimeout(() => {
      router.push('/login')
    }, 1200)
  } catch (err) {
    console.error('Register error:', err)
    errorMessage.value = err.message || '注册失败，请更换用户名重试'
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div class="auth-container">
    <div class="auth-card glass-panel">
      <div class="card-header">
        <div class="brand-badge">
          <Zap class="w-6 h-6 text-green-400" />
        </div>
        <h2 class="text-gradient">创建账号</h2>
        <p>注册以开始使用 MoocPass</p>
      </div>

      <div v-if="errorMessage" class="error-banner">
        <AlertCircle class="w-4 h-4 text-red-400 flex-shrink-0" />
        <span>{{ errorMessage }}</span>
      </div>

      <div v-if="successMessage" class="success-banner">
        <CheckCircle class="w-4 h-4 text-green-400 flex-shrink-0" />
        <span>{{ successMessage }}</span>
      </div>

      <form @submit.prevent="handleRegister" class="form-body">
        <div class="form-group">
          <label>用户名 <span class="required">*</span></label>
          <div class="input-wrap">
            <User class="input-icon w-4 h-4" />
            <input 
              type="text" 
              v-model="username" 
              placeholder="用于登录的账号用户名" 
              class="cyber-input" 
              required 
            />
          </div>
        </div>

        <div class="form-group">
          <label>显示昵称</label>
          <div class="input-wrap">
            <Smile class="input-icon w-4 h-4" />
            <input 
              type="text" 
              v-model="nickname" 
              placeholder="个性化名称（选填）" 
              class="cyber-input" 
            />
          </div>
        </div>

        <div class="form-group">
          <label>电子邮箱</label>
          <div class="input-wrap">
            <Mail class="input-icon w-4 h-4" />
            <input 
              type="email" 
              v-model="email" 
              placeholder="name@example.com（选填）" 
              class="cyber-input" 
            />
          </div>
        </div>

        <div class="form-group">
          <label>登录密码 <span class="required">*</span></label>
          <div class="input-wrap">
            <Lock class="input-icon w-4 h-4" />
            <input 
              type="password" 
              v-model="password" 
              placeholder="设置安全登录密码" 
              class="cyber-input" 
              required 
            />
          </div>
        </div>

        <button type="submit" class="glow-btn w-full justify-center text-base mt-2" :disabled="isLoading">
          <span v-if="isLoading">提交注册中...</span>
          <span v-else>确认注册账号</span>
          <ArrowRight class="w-4 h-4" />
        </button>
      </form>

      <div class="card-footer">
        <span>已有 MoocPass 账号？</span>
        <router-link to="/login" class="link-btn">直接登录</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background-image: 
    radial-gradient(circle at 20% 20%, rgba(34, 197, 94, 0.08) 0%, transparent 40%),
    radial-gradient(circle at 80% 80%, rgba(6, 182, 212, 0.08) 0%, transparent 40%);
}

.auth-card {
  width: 100%;
  max-width: 440px;
  padding: 40px 36px;
  border-color: rgba(255, 255, 255, 0.12);
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.6), 0 0 30px rgba(34, 197, 94, 0.12);
  animation: fadeIn 0.4s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.card-header {
  text-align: center;
  margin-bottom: 24px;
}

.brand-badge {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.2), rgba(6, 182, 212, 0.2));
  border: 1px solid rgba(34, 197, 94, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px auto;
  box-shadow: 0 0 20px rgba(34, 197, 94, 0.25);
}

.card-header h2 {
  font-size: 24px;
  font-weight: 800;
}

.card-header p {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 6px;
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
  margin-bottom: 20px;
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
  margin-bottom: 20px;
}

.form-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-muted);
}

.required {
  color: #ef4444;
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

.cyber-input {
  padding-left: 40px;
}

.card-footer {
  margin-top: 24px;
  text-align: center;
  padding-top: 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  font-size: 13px;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.link-btn {
  color: var(--primary-green);
  text-decoration: none;
  font-weight: 600;
  transition: color 0.2s;
}
.link-btn:hover {
  color: #4ade80;
  text-decoration: underline;
}
</style>
