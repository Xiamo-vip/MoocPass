<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { 
  LayoutDashboard, 
  Layers, 
  CheckSquare, 
  Settings, 
  Zap, 
  LogOut,
  User,
  Cloud
} from 'lucide-vue-next'
import { getUserInfoApi } from '../api/user'

const router = useRouter()
const route = useRoute()

const user = ref({ username: '已登录用户' })

const fetchUser = async () => {
  const localUser = localStorage.getItem('moocpass_user')
  if (localUser) {
    try { user.value = JSON.parse(localUser) } catch (e) {}
  }
  try {
    const remoteUser = await getUserInfoApi()
    if (remoteUser && remoteUser.username) {
      user.value = remoteUser
      localStorage.setItem('moocpass_user', JSON.stringify(remoteUser))
    }
  } catch (e) {}
}

onMounted(() => {
  fetchUser()
})

const handleLogout = () => {
  localStorage.removeItem('moocpass_token')
  localStorage.removeItem('moocpass_user')
  router.push('/login')
}
</script>

<template>
  <aside class="sidebar-container glass-panel">
    <!-- Brand Logo Header -->
    <div class="brand-header">
      <div class="logo-icon">
        <Zap class="w-6 h-6 text-green-400" />
      </div>
      <div class="brand-text">
        <h2>MoocPass</h2>
        <p>课程自动化管理平台</p>
      </div>
    </div>

    <!-- Navigation List -->
    <nav class="nav-menu">
      <router-link 
        to="/" 
        class="nav-item" 
        :class="{ active: route.path === '/' }"
      >
        <LayoutDashboard class="nav-icon" />
        <span>仪表盘</span>
      </router-link>

      <div class="nav-section-label">平台管理</div>
      <router-link 
        to="/platform/chaoxing" 
        class="nav-item" 
        :class="{ active: route.path === '/platform/chaoxing' }"
      >
        <Layers class="nav-icon" />
        <span>超星学习通</span>
      </router-link>

      <router-link 
        to="/platform/zhy" 
        class="nav-item" 
        :class="{ active: route.path === '/platform/zhy' }"
      >
        <Cloud class="nav-icon" />
        <span>职教云</span>
      </router-link>

      <router-link 
        to="/tasks" 
        class="nav-item" 
        :class="{ active: route.path === '/tasks' }"
      >
        <CheckSquare class="nav-icon" />
        <span>任务管理</span>
      </router-link>

      <router-link 
        to="/settings" 
        class="nav-item" 
        :class="{ active: route.path === '/settings' }"
      >
        <Settings class="nav-icon" />
        <span>系统设置</span>
      </router-link>
    </nav>

    <!-- User Profile Footer -->
    <div class="user-footer">
      <div class="user-info">
        <div class="avatar">
          {{ user.username ? user.username.charAt(0).toUpperCase() : 'U' }}
        </div>
        <div class="user-details">
          <span class="username">{{ user.nickname || user.username || '普通用户' }}</span>
        </div>
        <button class="logout-btn" title="退出登录" @click="handleLogout">
          <LogOut class="w-4 h-4" />
        </button>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.sidebar-container {
  width: 260px;
  height: calc(100vh - 32px);
  margin: 16px 0 16px 16px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 24px 16px;
  position: fixed;
  top: 0;
  left: 0;
  z-index: 100;
}

.brand-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.2), rgba(6, 182, 212, 0.2));
  border: 1px solid rgba(34, 197, 94, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 15px rgba(34, 197, 94, 0.2);
}

.brand-text h2 {
  font-size: 18px;
  font-weight: 800;
  letter-spacing: 0.5px;
  color: var(--text-main);
}

.brand-text p {
  font-size: 11px;
  color: var(--text-muted);
  margin-top: 2px;
}

.nav-menu {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 24px;
  flex: 1;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 10px;
  color: var(--text-muted);
  background: transparent;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
  position: relative;
  width: 100%;
}

.nav-item:hover {
  color: var(--text-main);
  background: rgba(255, 255, 255, 0.05);
}

.nav-item.active {
  color: #ffffff;
  background: linear-gradient(90deg, rgba(34, 197, 94, 0.15) 0%, rgba(34, 197, 94, 0.03) 100%);
  border-left: 3px solid var(--primary-green);
  font-weight: 600;
}

.nav-icon {
  width: 18px;
  height: 18px;
}

.nav-section-label {
  font-size: 10px;
  font-weight: 700;
  color: var(--text-dim);
  text-transform: uppercase;
  letter-spacing: 1.5px;
  padding: 16px 16px 6px;
}

.user-footer {
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: linear-gradient(135deg, #22c55e, #06b6d4);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  color: #020617;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.3);
}

.user-details {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.username {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-sub {
  font-size: 11px;
  color: var(--text-muted);
}

.logout-btn {
  background: transparent;
  border: none;
  color: var(--text-dim);
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.logout-btn:hover {
  color: #ef4444;
  background: rgba(239, 68, 68, 0.1);
}
</style>
