<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Zap, User, Lock, ArrowRight, AlertCircle } from 'lucide-vue-next'
import { loginApi } from '../api/user'

const router = useRouter()
const username = ref('')
const password = ref('')
const isLoading = ref(false)
const errorMessage = ref('')

const handleLogin = async () => {
  if (!username.value || !password.value) return
  isLoading.value = true
  errorMessage.value = ''

  try {
    const loginVo = await loginApi({
      username: username.value,
      password: password.value
    })
    
    if (loginVo && loginVo.token) {
      localStorage.setItem('moocpass_token', loginVo.token)
      localStorage.setItem('moocpass_user', JSON.stringify(loginVo.user || { username: username.value }))
      router.push('/')
    } else {
      errorMessage.value = '登录返回数据异常'
    }
  } catch (err) {
    console.error('Login error:', err)
    errorMessage.value = err.message || '登录失败，请检查账号密码'
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
        <h2 class="text-gradient">欢迎登录</h2>
        <p>登录账户以继续</p>
      </div>

      <div v-if="errorMessage" class="error-banner">
        <AlertCircle class="w-4 h-4 text-red-400 flex-shrink-0" />
        <span>{{ errorMessage }}</span>
      </div>

      <form @submit.prevent="handleLogin" class="form-body">
        <div class="form-group">
          <label>账号 / 用户名</label>
          <div class="input-wrap">
            <User class="input-icon w-4 h-4" />
            <input 
              type="text" 
              v-model="username" 
              placeholder="请输入用户名" 
              class="cyber-input" 
              required 
            />
          </div>
        </div>

        <div class="form-group">
          <label>登录密码</label>
          <div class="input-wrap">
            <Lock class="input-icon w-4 h-4" />
            <input 
              type="password" 
              v-model="password" 
              placeholder="请输入密码" 
              class="cyber-input" 
              required 
            />
          </div>
        </div>

        <button type="submit" class="glow-btn w-full justify-center text-base mt-2" :disabled="isLoading">
          <span v-if="isLoading">正在登录...</span>
          <span v-else>立即登录</span>
          <ArrowRight class="w-4 h-4" />
        </button>
      </form>

      <div class="card-footer">
        <span>还没有账号？</span>
        <router-link to="/register" class="link-btn">免费注册新账号</router-link>
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
  max-width: 420px;
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
  margin-bottom: 28px;
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

.form-body {
  display: flex;
  flex-direction: column;
  gap: 18px;
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
  margin-top: 28px;
  text-align: center;
  padding-top: 20px;
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
